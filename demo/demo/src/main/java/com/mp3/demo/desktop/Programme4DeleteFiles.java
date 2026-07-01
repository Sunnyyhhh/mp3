package com.mp3.demo.desktop;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalDateTime;

/**
 * Programme 4 — Suppression des fichiers MP3 du dossier musique
 *
 * - Écoute : queue.mp3.to.delete
 * - Reçoit : chemin absolu du fichier MP3 à supprimer
 * - Supprime le fichier du répertoire musique source
 * - Logge dans logs/programme4.log
 *
 * Déclenché uniquement pour les MP3 qui ont passé blacklist + durée max
 * (et donc déjà envoyés à Programme 3 pour insertion en base).
 *
 * Profil Spring : deleter
 * Lancement    : java -jar demo.jar --spring.profiles.active=deleter
 */
@Component
@Profile("deleter")
public class Programme4DeleteFiles {

    private static final String LOG_FILE = "logs/programme4.log";

    @RabbitListener(queues = RabbitMQConfig.QUEUE_MP3_TO_DELETE)
    public void traiter(String chemin) {
        log("Reçu demande de suppression : " + chemin);

        File fichier = new File(chemin);

        if (!fichier.exists()) {
            log("Fichier déjà absent (ignoré) : " + chemin);
            return;
        }

        String nomFichier = fichier.getName();

        if (fichier.delete()) {
            log("✅ Fichier supprimé du répertoire musique : " + nomFichier);
        } else {
            log("⚠️ Impossible de supprimer : " + nomFichier + " (vérifier les permissions)");
        }
    }

    private void log(String message) {
        String ligne = "[" + LocalDateTime.now() + "] [DELETER] " + message;
        System.out.println(ligne);
        try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
            fw.write(ligne + "\n");
        } catch (IOException e) {
            System.err.println("Erreur écriture log : " + e.getMessage());
        }
    }
}