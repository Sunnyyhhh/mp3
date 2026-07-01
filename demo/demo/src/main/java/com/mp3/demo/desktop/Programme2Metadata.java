package com.mp3.demo.desktop;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Programme 2 — Extraction de métadonnées + filtres blacklist + durée max
 *
 * - Écoute  : queue.scanner.to.metadata
 * - Reçoit  : chemin absolu d'un fichier MP3
 * - Extrait : titre, artiste, album, genre, durée
 *
 * Filtres (les deux NE suppriment PAS le fichier du répertoire musique) :
 *   → Artiste/genre blacklisté (blacklist.txt)  → ignoré, fichier conservé
 *   → Durée > dureeMax (duree-max.txt)          → ignoré, fichier conservé
 *
 * Si la chanson passe les deux filtres :
 *   → Queue 3 (queue.mp3.to.db) : métadonnées JSON pour insertion en base
 *   (c'est Programme 3 qui déclenchera la suppression via Queue 4,
 *    UNIQUEMENT après confirmation du succès de l'insertion en base)
 *
 * Profil Spring : metadata
 * Lancement    : java -jar demo.jar --spring.profiles.active=metadata
 */
@Component
@Profile("metadata")
public class Programme2Metadata {

    private final RabbitTemplate rabbitTemplate;

    private static final String LOG_FILE          = "logs/programme2.log";
    private static final String BLACKLIST_FILE     = "blacklist.txt";
    private static final String DUREE_MAX_FILE     = "duree-max.txt";
    private static final String SECTION_ARTISTES   = "[ARTISTES]";
    private static final String SECTION_GENRES     = "[GENRES]";

    public Programme2Metadata(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

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

            // ── 1. Extraction des métadonnées ─────────────────────────────────
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

            // Fallback nom de fichier : "Artiste_Titre.mp3"
            if (titre.isEmpty() || artiste.isEmpty()) {
                String nom = fichier.getName().replaceAll("(?i)\\.mp3$", "");
                String[] parts = nom.split("_", 2);
                if (parts.length == 2) {
                    if (artiste.isEmpty()) artiste = parts[0].trim();
                    if (titre.isEmpty())   titre   = parts[1].trim();
                } else {
                    if (titre.isEmpty()) titre = nom.trim();
                }
            }

            log("Métadonnées extraites : artiste=\"" + artiste + "\" | genre=\"" + genre + "\" | durée=" + duree + "s");

            // ── 2. Chargement de la blacklist ──────────────────────────────────
            Map<String, Set<String>> blacklists    = chargerBlacklist(BLACKLIST_FILE);
            Set<String> artistesBlacklistes = blacklists.get(SECTION_ARTISTES);
            Set<String> genresBlacklistes   = blacklists.get(SECTION_GENRES);

            // ── 3. Filtre blacklist artiste (pas de suppression) ───────────────
            if (!artiste.isEmpty() && artistesBlacklistes.contains(artiste.toLowerCase().trim())) {
                log("⛔ Artiste blacklisté : \"" + artiste + "\" → fichier conservé dans le répertoire musique");
                return;
            }

            // ── 4. Filtre blacklist genre (pas de suppression) ─────────────────
            if (!genre.isEmpty() && genresBlacklistes.contains(genre.toLowerCase().trim())) {
                log("⛔ Genre blacklisté : \"" + genre + "\" → fichier conservé dans le répertoire musique");
                return;
            }

            // ── 5. Filtre durée max (pas de suppression) ───────────────────────
            int dureeMax = chargerDureeMax(DUREE_MAX_FILE);
            if (dureeMax > 0 && duree > dureeMax) {
                log("⛔ Durée dépassée : " + duree + "s > max " + dureeMax + "s pour \"" + titre
                        + "\" → fichier conservé dans le répertoire musique");
                return;
            }

            // ── 6. Chanson OK → Queue 3 (DB) ───────────────────────────────────
            // La suppression (Queue 4) sera déclenchée par Programme 3
            // uniquement après confirmation du succès de l'insertion.
            Map<String, Object> message = new HashMap<>();
            message.put("chemin",  chemin);
            message.put("titre",   titre);
            message.put("artiste", artiste);
            message.put("album",   album);
            message.put("genre",   genre);
            message.put("duree",   duree);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_MP3,
                    RabbitMQConfig.ROUTING_MP3_TO_DB,
                    message
            );

            long fin = System.currentTimeMillis();
            log("✅ Envoyé en Queue 3 (DB) : " + fichier.getName()
                    + " | artiste=" + artiste
                    + " | titre="   + titre
                    + " | durée="   + duree + "s"
                    + " | temps="   + (fin - debut) + "ms");

        } catch (Exception e) {
            log("Erreur extraction pour " + chemin + " : " + e.getMessage()
                    + " → fichier conservé pour retraitement");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Chargement blacklist (sections [ARTISTES] / [GENRES])
    // ──────────────────────────────────────────────────────────────────────────

    private Map<String, Set<String>> chargerBlacklist(String nomFichier) {
        Map<String, Set<String>> resultat = new HashMap<>();
        resultat.put(SECTION_ARTISTES, new HashSet<>());
        resultat.put(SECTION_GENRES, new HashSet<>());

        File fichier = new File(nomFichier);
        if (!fichier.exists()) {
            log("Fichier blacklist introuvable (ignoré) : " + nomFichier);
            return resultat;
        }

        try {
            String sectionCourante = null;
            for (String ligneBrute : Files.readAllLines(fichier.toPath())) {
                String ligne = ligneBrute.trim();
                if (ligne.isEmpty() || ligne.startsWith("#")) continue;

                if (ligne.equalsIgnoreCase(SECTION_ARTISTES)) { sectionCourante = SECTION_ARTISTES; continue; }
                if (ligne.equalsIgnoreCase(SECTION_GENRES))   { sectionCourante = SECTION_GENRES;   continue; }

                if (sectionCourante != null) {
                    resultat.get(sectionCourante).add(ligne.toLowerCase());
                }
            }
        } catch (IOException e) {
            log("Erreur lecture blacklist : " + e.getMessage());
        }
        return resultat;
    }

    private int chargerDureeMax(String nomFichier) {
        File fichier = new File(nomFichier);
        if (!fichier.exists()) {
            log("Fichier duree-max.txt introuvable → pas de limite de durée appliquée");
            return -1;
        }
        try {
            for (String ligneBrute : Files.readAllLines(fichier.toPath())) {
                String ligne = ligneBrute.trim();
                if (ligne.isEmpty() || ligne.startsWith("#")) continue;
                int val = Integer.parseInt(ligne);
                log("Durée max chargée : " + val + "s");
                return val;
            }
        } catch (Exception e) {
            log("Erreur lecture duree-max.txt : " + e.getMessage());
        }
        return -1;
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