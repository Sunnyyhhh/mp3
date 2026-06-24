package com.mp3.demo.desktop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Programme 2 — Extraction de métadonnées
 *
 * - Écoute la queue RabbitMQ : queue.scanner.to.metadata
 * - Reçoit le chemin d'un fichier MP3
 * - Extrait les métadonnées (titre, artiste, album, genre, durée)
 * - Envoie le résultat JSON dans la queue : queue.metadata.to.sender
 *
 * Profil Spring : metadata
 * Lancement     : java -jar demo.jar --spring.profiles.active=metadata
 */
@Component
@Profile("metadata")
public class Programme2Metadata {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String LOG_FILE = "logs/programme2.log";

    public Programme2Metadata(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Méthode déclenchée automatiquement dès qu'un message arrive dans la queue.
     * Le paramètre "chemin" est le chemin absolu du fichier MP3.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_SCANNER_TO_METADATA)
    public void traiter(String chemin) {
        log("Reçu fichier à traiter : " + chemin);
        long debut = System.currentTimeMillis();

        try {
            File fichier = new File(chemin);

            if (!fichier.exists()) {
                log("Fichier introuvable, on ignore : " + chemin);
                return;
            }

            // Valeurs par défaut
            String artiste = "";
            String titre   = "";
            String album   = "";
            String genre   = "";
            int    duree   = 0;

            // Extraction des tags MP3
            Mp3File mp3File = new Mp3File(fichier);
            duree = (int) mp3File.getLengthInSeconds();

            if (mp3File.hasId3v2Tag()) {
                ID3v2 tag = mp3File.getId3v2Tag();
                titre   = tag.getTitle()            != null ? tag.getTitle()            : "";
                artiste = tag.getArtist()           != null ? tag.getArtist()           : "";
                album   = tag.getAlbum()            != null ? tag.getAlbum()            : "";
                genre   = tag.getGenreDescription() != null ? tag.getGenreDescription() : "";

            } else if (mp3File.hasId3v1Tag()) {
                ID3v1 tag = mp3File.getId3v1Tag();
                titre   = tag.getTitle()            != null ? tag.getTitle()            : "";
                artiste = tag.getArtist()           != null ? tag.getArtist()           : "";
                album   = tag.getAlbum()            != null ? tag.getAlbum()            : "";
                genre   = tag.getGenreDescription() != null ? tag.getGenreDescription() : "";
            }

            // Fallback : extraire artiste/titre depuis le nom du fichier si les tags sont vides
            // Format attendu : "Artiste_Titre.mp3"
            if (titre.isEmpty() || artiste.isEmpty()) {
                String nom = fichier.getName()
                        .replace(".mp3", "")
                        .replace(".MP3", "");
                String[] parts = nom.split("_", 2);
                if (parts.length == 2) {
                    if (artiste.isEmpty()) artiste = parts[0].trim();
                    if (titre.isEmpty())   titre   = parts[1].trim();
                } else {
                    if (titre.isEmpty()) titre = nom.trim();
                }
            }

            // Construire le message JSON à envoyer à Programme 3
            Map<String, Object> message = new HashMap<>();
            message.put("chemin",  chemin);
            message.put("titre",   titre);
            message.put("artiste", artiste);
            message.put("album",   album);
            message.put("genre",   genre);
            message.put("duree",   duree);

            // Envoyer vers l'exchange MP3, routing key → Programme 3
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_MP3,
                    RabbitMQConfig.ROUTING_METADATA_TO_SENDER,
                    message  // Jackson2JsonMessageConverter sérialise la Map en JSON automatiquement
            );

            long fin = System.currentTimeMillis();
            log("Extraction terminée : " + fichier.getName()
                    + " | artiste=" + artiste
                    + " | titre="   + titre
                    + " | durée="   + duree + "s"
                    + " | temps="   + (fin - debut) + "ms");

        } catch (Exception e) {
            log("Erreur extraction pour " + chemin + " : " + e.getMessage()
                    + " → fichier conservé pour retraitement");
        }
    }

    private void log(String message) {
        String ligne = "[" + LocalDateTime.now() + "] [METADATA] " + message;
        System.out.println(ligne);
        try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
            fw.write(ligne + "\n");
        } catch (IOException e) {
            System.err.println("Erreur écriture log : " + e.getMessage());
        }
    }
}