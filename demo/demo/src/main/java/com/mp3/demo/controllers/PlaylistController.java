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

    // POST /playlists
    @PostMapping
    public Playlist create(
            @RequestParam String nom,
            @RequestParam Integer dureeCible,
            @RequestParam Long utilisateurId
    ) {
        return playlistService.create(nom, dureeCible, utilisateurId);
    }

    // GET /playlists
    @GetMapping
    public List<Playlist> getAll() {
        return playlistService.findAll();
    }

    // GET /playlists/{id}
    @GetMapping("/{id}")
    public Playlist getById(@PathVariable Long id) {
        return playlistService.findById(id);
    }

    // DELETE /playlists/{id}
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        playlistService.delete(id);
    }

    // POST /playlists/{id}/suggerer (sans filtre, respecte durée cible)
    @PostMapping("/{id}/suggerer")
    public List<PlaylistMp3> suggerer(@PathVariable Long id) {
        return playlistService.suggerer(id);
    }

    // POST /playlists/{id}/suggerer-par-filtres
    // Body JSON : { "artistes": ["Olivia Rodrigo"], "genres": ["Pop"] }
    @PostMapping("/{id}/suggerer-par-filtres")
    public List<PlaylistMp3> suggererParFiltres(
            @PathVariable Long id,
            @RequestBody Map<String, List<String>> body
    ) {
        List<String> artistes = body.getOrDefault("artistes", List.of());
        List<String> genres   = body.getOrDefault("genres",   List.of());
        return playlistService.suggererParFiltres(id, artistes, genres);
    }

    // POST /playlists/{id}/ajouter-morceau?mp3Id=5
    @PostMapping("/{id}/ajouter-morceau")
    public PlaylistMp3 ajouterMorceau(
            @PathVariable Long id,
            @RequestParam Long mp3Id
    ) {
        return playlistService.ajouterMorceau(id, mp3Id);
    }

    // DELETE /playlists/morceaux/{playlistMp3Id}
    @DeleteMapping("/morceaux/{playlistMp3Id}")
    public void supprimerMorceau(@PathVariable Long playlistMp3Id) {
        playlistService.supprimerMorceau(playlistMp3Id);
    }

    // POST /playlists/{id}/confirmer → verrouille la playlist
    @PostMapping("/{id}/confirmer")
    public Playlist confirmer(@PathVariable Long id) {
        return playlistService.confirmer(id);
    }

    // GET /playlists/{id}/morceaux
    @GetMapping("/{id}/morceaux")
    public List<PlaylistMp3> getMorceaux(@PathVariable Long id) {
        return playlistService.getMorceaux(id);
    }

    // GET /playlists/{id}/zip
    @GetMapping("/{id}/zip")
    public ResponseEntity<byte[]> telechargerZip(@PathVariable Long id) throws IOException {
        byte[] zip = playlistService.telechargerZip(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"playlist_" + id + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zip);
    }
}