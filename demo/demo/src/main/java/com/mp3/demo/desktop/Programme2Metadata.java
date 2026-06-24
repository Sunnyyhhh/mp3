package com.mp3.demo.desktop;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Programme 2 — Extraction de métadonnées + filtre blacklist
 *
 * - Écoute la queue RabbitMQ : queue.scanner.to.metadata
 * - Reçoit le chemin d'un fichier MP3
 * - Extrait les métadonnées (titre, artiste, album, genre, durée)
 * - Vérifie si l'artiste ou le genre est blacklisté (lecture de fichiers texte)
 *   → Si blacklisté : supprime le fichier du répertoire, stop
 *   → Sinon         : envoie le résultat JSON dans queue.metadata.to.sender
 *
 * Fichiers blacklist (un nom/genre par ligne, insensible à la casse) :
 *   - blacklist-artistes.txt
 *   - blacklist-genres.txt
 *
 * Profil Spring : metadata
 * Lancement     : java -jar demo.jar --spring.profiles.active=metadata
 */
@Component
@Profile("metadata")
public class Programme2Metadata {

    private final RabbitTemplate rabbitTemplate;

    private static final String LOG_FILE              = "logs/programme2.log";
    private static final String BLACKLIST_ARTISTES    = "blacklist-artistes.txt";
    private static final String BLACKLIST_GENRES      = "blacklist-genres.txt";

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

            // ── 1. Extraction des métadonnées ─────────────────────────────────────
            String artiste = "";
            String titre   = "";
            String album   = "";
            String genre   = "";
            int    duree   = 0;

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

            // Fallback : extraire artiste/titre depuis le nom du fichier si tags vides
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

            log("Métadonnées extraites : artiste=\"" + artiste + "\" | genre=\"" + genre + "\"");

            // ── 2. Chargement des blacklists ──────────────────────────────────────
            Set<String> artistesBlacklistes = chargerBlacklist(BLACKLIST_ARTISTES);
            Set<String> genresBlacklistes   = chargerBlacklist(BLACKLIST_GENRES);

            // ── 3. Vérification blacklist artiste ─────────────────────────────────
            if (!artiste.isEmpty() && artistesBlacklistes.contains(artiste.toLowerCase().trim())) {
                log("⛔ Artiste blacklisté : \"" + artiste + "\" → suppression du fichier");
                supprimerFichier(fichier);
                return;
            }

            // ── 4. Vérification blacklist genre ───────────────────────────────────
            if (!genre.isEmpty() && genresBlacklistes.contains(genre.toLowerCase().trim())) {
                log("⛔ Genre blacklisté : \"" + genre + "\" → suppression du fichier");
                supprimerFichier(fichier);
                return;
            }

            // ── 5. Pas blacklisté → on envoie à Programme 3 ──────────────────────
            Map<String, Object> message = new HashMap<>();
            message.put("chemin",  chemin);
            message.put("titre",   titre);
            message.put("artiste", artiste);
            message.put("album",   album);
            message.put("genre",   genre);
            message.put("duree",   duree);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_MP3,
                    RabbitMQConfig.ROUTING_METADATA_TO_SENDER,
                    message
            );

            long fin = System.currentTimeMillis();
            log("✅ Envoyé à Programme 3 : " + fichier.getName()
                    + " | artiste=" + artiste
                    + " | titre="   + titre
                    + " | durée="   + duree + "s"
                    + " | temps="   + (fin - debut) + "ms");

        } catch (Exception e) {
            log("Erreur extraction pour " + chemin + " : " + e.getMessage()
                    + " → fichier conservé pour retraitement");
        }
    }

    /**
     * Lit un fichier texte de blacklist et retourne un Set en minuscules.
     * Chaque ligne = un artiste ou un genre.
     * Les lignes vides et commentaires (#) sont ignorés.
     * Si le fichier n'existe pas, retourne un Set vide.
     */
    private Set<String> chargerBlacklist(String nomFichier) {
        File fichier = new File(nomFichier);

        if (!fichier.exists()) {
            log("Fichier blacklist introuvable (ignoré) : " + nomFichier);
            return Collections.emptySet();
        }

        try {
            return Files.lines(fichier.toPath())
                    .map(String::trim)
                    .filter(ligne -> !ligne.isEmpty() && !ligne.startsWith("#"))
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            log("Erreur lecture blacklist \"" + nomFichier + "\" : " + e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * Supprime physiquement le fichier du répertoire source.
     */
    private void supprimerFichier(File fichier) {
        if (fichier.delete()) {
            log("🗑 Fichier supprimé : " + fichier.getName());
        } else {
            log("⚠️ Impossible de supprimer : " + fichier.getName());
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
