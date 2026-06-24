package com.mp3.demo.desktop;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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
 * Programme 3 — Envoi vers l'API backend
 *
 * - Écoute la queue RabbitMQ : queue.metadata.to.sender
 * - Reçoit un objet JSON avec le chemin du fichier + métadonnées
 * - Upload le fichier MP3 vers l'API backend via HTTP POST multipart
 * - Si succès (HTTP 200) : supprime le fichier du répertoire source
 * - Si erreur : conserve le fichier pour retraitement ultérieur
 *
 * Profil Spring : sender
 * Lancement     : java -jar demo.jar --spring.profiles.active=sender
 */
@Component
@Profile("sender")
public class Programme3Sender {

    private static final String LOG_FILE = "logs/programme3.log";

    // URL de l'API backend — définie dans application-sender.properties
    @Value("${app.api.upload.url:http://localhost:8080/mp3/upload}")
    private String apiUploadUrl;

    /**
     * Méthode déclenchée automatiquement dès qu'un message arrive dans la queue.
     * Spring désérialise automatiquement le JSON en Map grâce au Jackson2JsonMessageConverter.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_METADATA_TO_SENDER)
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

            // Construction de l'URL avec les paramètres métadonnées
            String url = apiUploadUrl
                    + "?titre="   + encode(titre)
                    + "&artiste=" + encode(artiste)
                    + "&album="   + encode(album)
                    + "&genre="   + encode(genre);

            HttpPost post = new HttpPost(url);

            // Corps multipart avec le fichier MP3
            var entity = MultipartEntityBuilder.create()
                    .addBinaryBody(
                            "file",
                            fichier,
                            ContentType.create("audio/mpeg"),
                            fichier.getName()
                    )
                    .build();

            post.setEntity(entity);

            // Exécution de la requête
            int statusCode = httpClient.execute(post, response -> response.getCode());

            if (statusCode == 200) {
                log("Envoi réussi : " + fichier.getName() + " (HTTP " + statusCode + ")");

                // Supprimer le fichier original après envoi réussi
                if (fichier.delete()) {
                    log("Fichier supprimé du répertoire source : " + fichier.getName());
                } else {
                    log("Avertissement : impossible de supprimer " + fichier.getName());
                }

            } else {
                log("Échec envoi (HTTP " + statusCode + ") : "
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