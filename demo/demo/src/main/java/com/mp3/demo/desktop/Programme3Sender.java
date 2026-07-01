package com.mp3.demo.desktop;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Programme 3 — Envoi vers l'API backend (insertion en base)
 *
 * - Écoute : queue.mp3.to.db
 * - Reçoit : JSON avec chemin + métadonnées d'un MP3 ayant passé blacklist et durée max
 * - Upload  : POST multipart vers /mp3/upload
 * - Déclenche la suppression (Queue 4 → Programme 4) UNIQUEMENT
 *   après confirmation du succès HTTP 200 de l'insertion en base.
 *   → garantit l'ordre : insertion en base AVANT suppression du fichier.
 *
 * Profil Spring : sender
 * Lancement    : java -jar demo.jar --spring.profiles.active=sender
 */
@Component
@Profile("sender")
public class Programme3Sender {

    private static final String LOG_FILE = "logs/programme3.log";

    @Value("${app.api.upload.url:http://localhost:8080/mp3/upload}")
    private String apiUploadUrl;

    private final RabbitTemplate rabbitTemplate;

    public Programme3Sender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_MP3_TO_DB)
    public void traiter(@Payload Map<String, Object> data) {
        log("Nouveau message reçu depuis Programme 2");

        String chemin  = (String) data.get("chemin");
        String titre   = (String) data.get("titre");
        String artiste = (String) data.get("artiste");
        String album   = (String) data.get("album");
        String genre   = (String) data.get("genre");

        File fichier = new File(chemin);

        if (!fichier.exists()) {
            log("Fichier introuvable, on ignore : " + chemin);
            return;
        }

        log("Envoi vers API : " + fichier.getName()
                + " | artiste=" + artiste
                + " | titre="   + titre);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            String url = apiUploadUrl
                    + "?titre="   + encode(titre)
                    + "&artiste=" + encode(artiste)
                    + "&album="   + encode(album)
                    + "&genre="   + encode(genre);

            HttpPost post = new HttpPost(url);

            var entity = MultipartEntityBuilder.create()
                    .addBinaryBody("file", fichier, ContentType.create("audio/mpeg"), fichier.getName())
                    .build();

            post.setEntity(entity);

            int statusCode = httpClient.execute(post, response -> response.getCode());

            if (statusCode == 200) {
                log("✅ Insertion en base réussie : " + fichier.getName() + " (HTTP " + statusCode + ")");

                // Suppression déclenchée uniquement après succès confirmé de l'insertion
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_MP3,
                        RabbitMQConfig.ROUTING_MP3_TO_DELETE,
                        chemin
                );
                log("Message envoyé à Programme 4 pour suppression : " + fichier.getName());
            } else {
                log("⚠️ Échec insertion (HTTP " + statusCode + ") : "
                        + fichier.getName() + " → fichier conservé pour retraitement");
            }

        } catch (Exception e) {
            log("Erreur lors de l'envoi de " + fichier.getName()
                    + " : " + e.getMessage()
                    + " → fichier conservé pour retraitement");
        }
    }

    private String encode(String valeur) {
        if (valeur == null) return "";
        return URLEncoder.encode(valeur, StandardCharsets.UTF_8);
    }

    private void log(String message) {
        String ligne = "[" + LocalDateTime.now() + "] [SENDER] " + message;
        System.out.println(ligne);
        try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
            fw.write(ligne + "\n");
        } catch (IOException e) {
            System.err.println("Erreur écriture log : " + e.getMessage());
        }
    }
}