package com.mp3.demo.desktop;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Programme 1 — Scanner de répertoire
 *
 * - Surveille un dossier toutes les 10 secondes
 * - Détecte les nouveaux fichiers MP3
 * - Envoie le chemin de chaque nouveau MP3 dans RabbitMQ (queue scanner→metadata)
 *
 * Profil Spring : scanner
 * Lancement     : java -jar demo.jar --spring.profiles.active=scanner
 */
@Component
@Profile("scanner")
public class Programme1Scanner implements CommandLineRunner {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.desktop.watch.path}")
    private String watchPath;

    private static final String LOG_FILE = "logs/programme1.log";

    // Mémorise les fichiers déjà envoyés pour ne pas les renvoyer
    private final Set<String> fichiersDejaScan = new HashSet<>();

    public Programme1Scanner(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        new File("logs").mkdirs();
        log("=== Programme 1 (Scanner) démarré ===");
        log("Surveillance du répertoire : " + watchPath);

        Path dossier = Paths.get(watchPath);
        if (!Files.exists(dossier)) {
            Files.createDirectories(dossier);
            log("Répertoire créé : " + watchPath);
        }

        while (true) {
            try {
                File[] fichiers = dossier.toFile().listFiles(
                        f -> f.isFile() && f.getName().toLowerCase().endsWith(".mp3")
                );

                if (fichiers != null) {
                    for (File fichier : fichiers) {
                        String chemin = fichier.getAbsolutePath();

                        if (!fichiersDejaScan.contains(chemin)) {
                            fichiersDejaScan.add(chemin);
                            log("Nouveau MP3 détecté : " + fichier.getName());

                            // Envoie le chemin vers l'exchange MP3, routing key → Programme 2
                            rabbitTemplate.convertAndSend(
                                    RabbitMQConfig.EXCHANGE_MP3,
                                    RabbitMQConfig.ROUTING_SCANNER_TO_METADATA,
                                    chemin
                            );

                            log("Message envoyé à Programme 2 pour : " + fichier.getName());
                        }
                    }
                }

            } catch (Exception e) {
                log("Erreur pendant le scan : " + e.getMessage());
            }

            Thread.sleep(10_000); // attendre 10 secondes avant le prochain scan
        }
    }

    private void log(String message) {
        String ligne = "[" + LocalDateTime.now() + "] [SCANNER] " + message;
        System.out.println(ligne);
        try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
            fw.write(ligne + "\n");
        } catch (IOException e) {
            System.err.println("Erreur écriture log : " + e.getMessage());
        }
    }
}