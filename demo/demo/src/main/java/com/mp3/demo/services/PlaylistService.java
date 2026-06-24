package com.mp3.demo.services;

import com.mp3.demo.entities.*;
import com.mp3.demo.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Profile("api")
@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistMp3Repository playlistMp3Repository;
    private final Mp3Repository mp3Repository;
    private final UtilisateurRepository utilisateurRepository;
    private final BlacklistRepository blacklistRepository;

    // ==============================
    // CRUD Playlist
    // ==============================

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

    public List<Playlist> findAll() {
        return playlistRepository.findAll();
    }

    public Playlist findById(Long id) {
        return playlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Playlist introuvable"));
    }

    public void delete(Long id) {
        playlistRepository.deleteById(id);
    }

    // ==============================
    // Blacklist
    // ==============================

    public List<Blacklist> getBlacklist(Long playlistId) {
        return blacklistRepository.findByPlaylistId(playlistId);
    }

    public Blacklist addToBlacklist(Long playlistId, String type, String valeur) {
        Playlist playlist = findById(playlistId);
        Blacklist entry = Blacklist.builder()
                .playlist(playlist)
                .type(type.toUpperCase())
                .valeur(valeur)
                .build();
        return blacklistRepository.save(entry);
    }

    @Transactional
    public void removeFromBlacklist(Long playlistId, String type, String valeur) {
        blacklistRepository.deleteByPlaylistIdAndTypeAndValeur(playlistId, type.toUpperCase(), valeur);
    }

    // ==============================
    // Génération avec filtre blacklist
    // ==============================

    private List<Mp3> filtrerBlacklist(List<Mp3> mp3List, Long playlistId) {
        List<Blacklist> blacklist = blacklistRepository.findByPlaylistId(playlistId);

        List<String> artistesBlacklistes = blacklist.stream()
                .filter(b -> b.getType().equals("ARTISTE"))
                .map(b -> b.getValeur().toLowerCase())
                .toList();

        List<String> genresBlacklistes = blacklist.stream()
                .filter(b -> b.getType().equals("GENRE"))
                .map(b -> b.getValeur().toLowerCase())
                .toList();

        return mp3List.stream()
                .filter(mp3 -> {
                    if (mp3.getArtiste() != null && artistesBlacklistes.contains(mp3.getArtiste().toLowerCase()))
                        return false;
                    if (mp3.getGenre() != null && genresBlacklistes.contains(mp3.getGenre().toLowerCase()))
                        return false;
                    return true;
                })
                .toList();
    }

    public List<PlaylistMp3> generer(Long playlistId, int dureeCibleSecondes) {
        Playlist playlist = findById(playlistId);

        List<PlaylistMp3> existants = playlistMp3Repository.findAll()
                .stream()
                .filter(pm -> pm.getPlaylist().getId().equals(playlistId))
                .toList();
        playlistMp3Repository.deleteAll(existants);

        List<Mp3> tousMp3 = filtrerBlacklist(mp3Repository.findAll(), playlistId);

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

    public List<PlaylistMp3> genererParArtistes(Long playlistId, List<String> artistes) {
        Playlist playlist = findById(playlistId);

        List<PlaylistMp3> existants = playlistMp3Repository.findAll()
                .stream()
                .filter(pm -> pm.getPlaylist().getId().equals(playlistId))
                .toList();
        playlistMp3Repository.deleteAll(existants);

        List<Mp3> mp3Filtres = mp3Repository.findAll()
                .stream()
                .filter(mp3 -> artistes.stream()
                        .anyMatch(a -> a.equalsIgnoreCase(mp3.getArtiste())))
                .toList();

        // Appliquer la blacklist (genres blacklistés s'appliquent même ici)
        mp3Filtres = filtrerBlacklist(mp3Filtres, playlistId);

        List<PlaylistMp3> result = new ArrayList<>();
        int ordre = 1;

        for (Mp3 mp3 : mp3Filtres) {
            PlaylistMp3 pm = PlaylistMp3.builder()
                    .playlist(playlist)
                    .mp3(mp3)
                    .ordre(ordre++)
                    .build();
            playlistMp3Repository.save(pm);
            result.add(pm);
        }

        return result;
    }

    // ==============================
    // Morceaux
    // ==============================

    public List<PlaylistMp3> getMorceaux(Long playlistId) {
        return playlistMp3Repository.findAll()
                .stream()
                .filter(pm -> pm.getPlaylist().getId().equals(playlistId))
                .sorted((a, b) -> a.getOrdre() - b.getOrdre())
                .toList();
    }

    public PlaylistMp3 remplacerMorceau(Long playlistMp3Id, Long nouveauMp3Id) {
        PlaylistMp3 pm = playlistMp3Repository.findById(playlistMp3Id)
                .orElseThrow(() -> new RuntimeException("Entrée playlist introuvable"));

        Mp3 nouveauMp3 = mp3Repository.findById(nouveauMp3Id)
                .orElseThrow(() -> new RuntimeException("MP3 introuvable"));

        pm.setMp3(nouveauMp3);
        return playlistMp3Repository.save(pm);
    }

    // ==============================
    // ZIP
    // ==============================

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