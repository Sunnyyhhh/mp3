package com.mp3.demo.services;

import com.mp3.demo.entities.Mp3;
import com.mp3.demo.entities.Playlist;
import com.mp3.demo.entities.PlaylistMp3;
import com.mp3.demo.entities.Utilisateur;
import com.mp3.demo.repositories.Mp3Repository;
import com.mp3.demo.repositories.PlaylistMp3Repository;
import com.mp3.demo.repositories.PlaylistRepository;
import com.mp3.demo.repositories.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.context.annotation.Profile;

@Profile("api")
@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistMp3Repository playlistMp3Repository;
    private final Mp3Repository mp3Repository;
    private final UtilisateurRepository utilisateurRepository;

    // Créer une playlist vide
    public Playlist create(String nom, Integer dureeCible, Long utilisateurId) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Playlist playlist = Playlist.builder()
                .nom(nom)
                .dureeCible(dureeCible)
                .utilisateur(utilisateur)
                .build();

        return playlistRepository.save(playlist);
    }

    // Récupérer toutes les playlists
    public List<Playlist> findAll() {
        return playlistRepository.findAll();
    }

    // Récupérer une playlist par id
    public Playlist findById(Long id) {
        return playlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Playlist introuvable"));
    }

    // Supprimer une playlist
    public void delete(Long id) {
        playlistRepository.deleteById(id);
    }

    // Générer automatiquement une playlist selon une durée cible (en secondes)
    public List<PlaylistMp3> generer(Long playlistId, int dureeCibleSecondes) {
        Playlist playlist = findById(playlistId);

        // Supprimer les anciens morceaux de la playlist
        List<PlaylistMp3> existants = playlistMp3Repository.findAll()
                .stream()
                .filter(pm -> pm.getPlaylist().getId().equals(playlistId))
                .toList();
        playlistMp3Repository.deleteAll(existants);

        // Récupérer tous les MP3 disponibles
        List<Mp3> tousMp3 = mp3Repository.findAll();

        List<PlaylistMp3> result = new ArrayList<>();
        int dureeAccumulee = 0;
        int ordre = 1;

        for (Mp3 mp3 : tousMp3) {
            if (mp3.getDuree() == null) continue;
            if (dureeAccumulee + mp3.getDuree() <= dureeCibleSecondes) {
                PlaylistMp3 pm = PlaylistMp3.builder()
                        .playlist(playlist)
                        .mp3(mp3)
                        .ordre(ordre++)
                        .build();
                playlistMp3Repository.save(pm);
                result.add(pm);
                dureeAccumulee += mp3.getDuree();
            }
            if (dureeAccumulee >= dureeCibleSecondes) break;
        }

        return result;
    }

    // Récupérer les morceaux d'une playlist
    public List<PlaylistMp3> getMorceaux(Long playlistId) {
        return playlistMp3Repository.findAll()
                .stream()
                .filter(pm -> pm.getPlaylist().getId().equals(playlistId))
                .sorted((a, b) -> a.getOrdre() - b.getOrdre())
                .toList();
    }

    // Remplacer un morceau dans la playlist
    public PlaylistMp3 remplacerMorceau(Long playlistMp3Id, Long nouveauMp3Id) {
        PlaylistMp3 pm = playlistMp3Repository.findById(playlistMp3Id)
                .orElseThrow(() -> new RuntimeException("Entrée playlist introuvable"));

        Mp3 nouveauMp3 = mp3Repository.findById(nouveauMp3Id)
                .orElseThrow(() -> new RuntimeException("MP3 introuvable"));

        pm.setMp3(nouveauMp3);
        return playlistMp3Repository.save(pm);
    }

    // Télécharger la playlist en ZIP
    public byte[] telechargerZip(Long playlistId) throws IOException {
        List<PlaylistMp3> morceaux = getMorceaux(playlistId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (PlaylistMp3 pm : morceaux) {
                Mp3 mp3 = pm.getMp3();
                File fichier = new File(mp3.getCheminStockage());
                if (!fichier.exists()) continue;

                ZipEntry entry = new ZipEntry(pm.getOrdre() + "_" + mp3.getNomFichier());
                zos.putNextEntry(entry);
                Files.copy(fichier.toPath(), zos);
                zos.closeEntry();
            }
        }

        return baos.toByteArray();
    }
}