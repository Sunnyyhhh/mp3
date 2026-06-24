package com.mp3.demo.controllers;

import com.mp3.demo.entities.Mp3;
import com.mp3.demo.services.Mp3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.annotation.Profile;

@Profile("api")
@RestController
@RequestMapping("/mp3")
@RequiredArgsConstructor
public class Mp3Controller {

    private final Mp3Service mp3Service;

    @Value("${app.storage.path}")
    private String storagePath;

    @GetMapping
    public List<Mp3> getAll() {
        return mp3Service.findAll();
    }

    @GetMapping("/{id}")
    public Mp3 getById(@PathVariable Long id) {
        return mp3Service.findById(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        mp3Service.delete(id);
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<org.springframework.core.io.Resource> stream(@PathVariable Long id) throws Exception {
        Mp3 mp3 = mp3Service.findById(id);

        java.io.File fichier = new java.io.File(mp3.getCheminStockage());
        if (!fichier.isAbsolute()) {
            fichier = new java.io.File(System.getProperty("user.dir"), mp3.getCheminStockage());
        }

        if (!fichier.exists()) {
            return ResponseEntity.notFound().build();
        }

        org.springframework.core.io.Resource resource =
                new org.springframework.core.io.FileSystemResource(fichier);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + mp3.getNomFichier() + "\"")
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(resource);
    }

    @PostMapping("/upload")
    public Mp3 upload(@RequestParam("file") MultipartFile file) throws Exception {
        Path path = Paths.get(storagePath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.toLowerCase().endsWith(".mp3")) {
            throw new RuntimeException("Fichier invalide (MP3 uniquement)");
        }

        String fileName = System.currentTimeMillis() + "_" + originalFileName;
        Path destination = path.resolve(fileName);
        Files.copy(file.getInputStream(), destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        String artiste = "";
        String titre = "";
        String genre = "";
        String album = "";
        int duree = 0;

        try {
            com.mpatric.mp3agic.Mp3File mp3File = new com.mpatric.mp3agic.Mp3File(destination.toFile());
            duree = (int) mp3File.getLengthInSeconds();

            if (mp3File.hasId3v2Tag()) {
                com.mpatric.mp3agic.ID3v2 tag = mp3File.getId3v2Tag();
                titre   = tag.getTitle()            != null ? tag.getTitle()            : "";
                artiste = tag.getArtist()           != null ? tag.getArtist()           : "";
                album   = tag.getAlbum()            != null ? tag.getAlbum()            : "";
                genre   = tag.getGenreDescription() != null ? tag.getGenreDescription() : "";
            } else if (mp3File.hasId3v1Tag()) {
                com.mpatric.mp3agic.ID3v1 tag = mp3File.getId3v1Tag();
                titre   = tag.getTitle()            != null ? tag.getTitle()            : "";
                artiste = tag.getArtist()           != null ? tag.getArtist()           : "";
                album   = tag.getAlbum()            != null ? tag.getAlbum()            : "";
                genre   = tag.getGenreDescription() != null ? tag.getGenreDescription() : "";
            }
        } catch (Exception e) {
            System.out.println("Erreur metadata: " + e.getMessage());
        }

        if (titre.isEmpty() || artiste.isEmpty()) {
            String nameWithoutExt = originalFileName.replace(".mp3", "").replace(".MP3", "");
            String[] parts = nameWithoutExt.split("_", 2);
            if (parts.length == 2) {
                if (artiste.isEmpty()) artiste = parts[0].trim();
                if (titre.isEmpty())   titre   = parts[1].trim();
            } else {
                if (titre.isEmpty()) titre = nameWithoutExt.trim();
            }
        }

        Mp3 mp3 = Mp3.builder()
                .nomFichier(fileName)
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

        return mp3Service.save(mp3);
    }
}