package com.mp3.demo.controllers;

import com.mp3.demo.entities.Playlist;
import com.mp3.demo.entities.PlaylistMp3;
import com.mp3.demo.services.PlaylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    // POST /playlists?nom=MaPlaylist&dureeCible=1920&utilisateurId=1
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

    // POST /playlists/{id}/generer?dureeCible=1920
    // dureeCible en secondes (ex: 32min = 1920)
    @PostMapping("/{id}/generer")
    public List<PlaylistMp3> generer(
            @PathVariable Long id,
            @RequestParam int dureeCible
    ) {
        return playlistService.generer(id, dureeCible);
    }

    // GET /playlists/{id}/morceaux
    @GetMapping("/{id}/morceaux")
    public List<PlaylistMp3> getMorceaux(@PathVariable Long id) {
        return playlistService.getMorceaux(id);
    }

    // PUT /playlists/morceaux/{playlistMp3Id}/remplacer?nouveauMp3Id=3
    @PutMapping("/morceaux/{playlistMp3Id}/remplacer")
    public PlaylistMp3 remplacer(
            @PathVariable Long playlistMp3Id,
            @RequestParam Long nouveauMp3Id
    ) {
        return playlistService.remplacerMorceau(playlistMp3Id, nouveauMp3Id);
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