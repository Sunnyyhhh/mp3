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

    // GET /mp3
    @GetMapping
    public List<Mp3> getAll() {
        return mp3Service.findAll();
    }

    // GET /mp3/{id}
    @GetMapping("/{id}")
    public Mp3 getById(@PathVariable Long id) {
        return mp3Service.findById(id);
    }

    // DELETE /mp3/{id}
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        mp3Service.delete(id);
    }

    // GET /mp3/{id}/stream
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

    /**
     * POST /mp3/upload 
     */
    @PostMapping("/upload")
    public Mp3 upload(
            @RequestParam("file")    MultipartFile file,
            @RequestParam(value = "titre",   defaultValue = "") String titre,
            @RequestParam(value = "artiste", defaultValue = "") String artiste,
            @RequestParam(value = "album",   defaultValue = "") String album,
            @RequestParam(value = "genre",   defaultValue = "") String genre
    ) throws Exception {
        return mp3Service.sauvegarder(file, titre, artiste, album, genre);
    }

    /**
     * POST /mp3/upload-manuel — Appelé depuis le formulaire web.
     */
    @PostMapping("/upload-manuel")
    public Mp3 uploadManuel(
            @RequestParam("file")    MultipartFile file,
            @RequestParam("titre")   String titre,
            @RequestParam("artiste") String artiste,
            @RequestParam(value = "genre", defaultValue = "") String genre
    ) throws Exception {
        return mp3Service.sauvegarderManuel(file, titre, artiste, genre);
    }

    /**
     * PUT /mp3/{id} 
     */
    @PutMapping("/{id}")
    public Mp3 update(
            @PathVariable Long id,
            @RequestParam(value = "titre",   required = false) String titre,
            @RequestParam(value = "artiste", required = false) String artiste,
            @RequestParam(value = "genre",   required = false) String genre
    ) {
        return mp3Service.update(id, titre, artiste, genre);
    }
}