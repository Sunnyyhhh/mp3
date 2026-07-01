package com.mp3.demo.services;

import com.mp3.demo.entities.Mp3;
import com.mp3.demo.repositories.Mp3Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;

@Profile("api")
@Service
@RequiredArgsConstructor
public class Mp3Service {

    private final Mp3Repository mp3Repository;

    @Value("${app.storage.path}")
    private String storagePath;

    public Mp3 save(Mp3 mp3) {
        return mp3Repository.save(mp3);
    }

    public List<Mp3> findAll() {
        return mp3Repository.findAll();
    }

    public Mp3 findById(Long id) {
        return mp3Repository.findById(id)
                .orElseThrow(() -> new RuntimeException("MP3 introuvable"));
    }

    public void delete(Long id) {
        mp3Repository.deleteById(id);
    }

    /**
     * Sauvegarde un MP3 venant du pipeline automatique (Programme 3).
     * Les métadonnées ont déjà été extraites par Programme 2.
     */
    public Mp3 sauvegarder(MultipartFile file, String titre, String artiste,
                            String album, String genre) throws Exception {
        Path destination = preparerDestination(file);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        int duree = extraireDuree(destination);

        Mp3 mp3 = Mp3.builder()
                .nomFichier(destination.getFileName().toString())
                .artiste(artiste)
                .titre(titre)
                .genre(genre)
                .album(album)
                .duree(duree)
                .cheminStockage(destination.toString())
                .taille(file.getSize())
                .dateUpload(LocalDateTime.now())
                .statut("UPLOADED")
                .build();

        return mp3Repository.save(mp3);
    }

    public Mp3 sauvegarderManuel(MultipartFile file, String titre,
                                  String artiste, String genre) throws Exception {
        validerFichierMp3(file);
        Path destination = preparerDestination(file);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        int duree = extraireDuree(destination);

        Mp3 mp3 = Mp3.builder()
                .nomFichier(destination.getFileName().toString())
                .artiste(artiste.trim())
                .titre(titre.trim())
                .genre(genre != null ? genre.trim() : "")
                .album("")
                .duree(duree)
                .cheminStockage(destination.toString())
                .taille(file.getSize())
                .dateUpload(LocalDateTime.now())
                .statut("UPLOADED")
                .build();

        return mp3Repository.save(mp3);
    }

    public Mp3 update(Long id, String titre, String artiste, String genre) {
        Mp3 mp3 = findById(id);
        if (titre   != null) mp3.setTitre(titre.trim());
        if (artiste != null) mp3.setArtiste(artiste.trim());
        if (genre   != null) mp3.setGenre(genre.trim());
        return mp3Repository.save(mp3);
    }

    private void validerFichierMp3(MultipartFile file) {
        String nom = file.getOriginalFilename();
        if (nom == null || !nom.toLowerCase().endsWith(".mp3")) {
            throw new RuntimeException("Fichier invalide — MP3 uniquement");
        }
    }

    private Path preparerDestination(MultipartFile file) throws Exception {
        Path dir = Paths.get(storagePath);
        if (!Files.exists(dir)) Files.createDirectories(dir);
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        return dir.resolve(fileName);
    }

    private int extraireDuree(Path fichierMp3) {
        try {
            com.mpatric.mp3agic.Mp3File mp3File = new com.mpatric.mp3agic.Mp3File(fichierMp3.toFile());
            return (int) mp3File.getLengthInSeconds();
        } catch (Exception e) {
            return 0;
        }
    }
}