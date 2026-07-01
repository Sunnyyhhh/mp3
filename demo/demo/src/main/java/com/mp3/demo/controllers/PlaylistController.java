package com.mp3.demo.controllers;

import com.mp3.demo.entities.Playlist;
import com.mp3.demo.entities.PlaylistMp3;
import com.mp3.demo.services.PlaylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Profile("api")
@RestController
@RequestMapping("/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    @PostMapping
    public Playlist create(
            @RequestParam String nom,
            @RequestParam Integer dureeCible,
            @RequestParam Long utilisateurId
    ) {
        return playlistService.create(nom, dureeCible, utilisateurId);
    }

    @GetMapping
    public List<Playlist> getAll() {
        return playlistService.findAll();
    }

    @GetMapping("/{id}")
    public Playlist getById(@PathVariable Long id) {
        return playlistService.findById(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        playlistService.delete(id);
    }

    @PostMapping("/{id}/suggerer")
    public List<PlaylistMp3> suggerer(@PathVariable Long id) {
        return playlistService.suggerer(id);
    }

    @PostMapping("/{id}/suggerer-par-filtres")
    public List<PlaylistMp3> suggererParFiltres(
            @PathVariable Long id,
            @RequestBody Map<String, List<String>> body
    ) {
        List<String> artistes = body.getOrDefault("artistes", List.of());
        List<String> genres   = body.getOrDefault("genres",   List.of());
        return playlistService.suggererParFiltres(id, artistes, genres);
    }

    @PostMapping("/{id}/ajouter-morceau")
    public PlaylistMp3 ajouterMorceau(
            @PathVariable Long id,
            @RequestParam Long mp3Id
    ) {
        return playlistService.ajouterMorceau(id, mp3Id);
    }

    @DeleteMapping("/morceaux/{playlistMp3Id}")
    public void supprimerMorceau(@PathVariable Long playlistMp3Id) {
        playlistService.supprimerMorceau(playlistMp3Id);
    }

    @PostMapping("/{id}/confirmer")
    public Playlist confirmer(@PathVariable Long id) {
        return playlistService.confirmer(id);
    }

    @GetMapping("/{id}/morceaux")
    public List<PlaylistMp3> getMorceaux(@PathVariable Long id) {
        return playlistService.getMorceaux(id);
    }

    @GetMapping("/{id}/zip")
    public ResponseEntity<byte[]> telechargerZip(@PathVariable Long id) throws IOException {
        byte[] zip = playlistService.telechargerZip(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"playlist_" + id + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zip);
    }

    @PostMapping("/fusionner")
    public Playlist fusionner(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> idsRaw = (List<Integer>) body.get("ids");
        List<Long> ids = idsRaw.stream().map(Long::valueOf).toList();
        Long utilisateurId = Long.valueOf(body.get("utilisateurId").toString());
        return playlistService.fusionner(ids, utilisateurId);
    }
}