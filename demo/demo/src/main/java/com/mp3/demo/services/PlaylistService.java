package com.mp3.demo.services;

import com.mp3.demo.entities.*;
import com.mp3.demo.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
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

    // ==============================
    // CRUD Playlist
    // ==============================

    public Playlist create(String nom, Integer dureeCible, Long utilisateurId) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Playlist playlist = Playlist.builder()
                .nom(nom)
                .dureeCible(dureeCible)
                .statut("BROUILLON")
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
        viderPlaylist(id);
        playlistRepository.deleteById(id);
    }

    // ==============================
    // Confirmer
    // ==============================

    public Playlist confirmer(Long playlistId) {
        Playlist playlist = findById(playlistId);
        playlist.setStatut("CONFIRMEE");
        return playlistRepository.save(playlist);
    }

    // ==============================
    // Fusionner plusieurs playlists
    // Déduplique par mp3.id, crée une nouvelle playlist CONFIRMEE directement
    // ==============================

    public Playlist fusionner(List<Long> ids, Long utilisateurId) {
        if (ids == null || ids.size() < 2) {
            throw new RuntimeException("Il faut au moins 2 playlists pour fusionner");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Récupérer tous les morceaux des playlists sources, dans l'ordre
        // et dédupliquer par mp3.id (on garde la première occurrence)
        List<Mp3> mp3Uniques = new ArrayList<>();
        Set<Long> mp3IdsVus = new LinkedHashSet<>();

        for (Long playlistId : ids) {
            List<PlaylistMp3> morceaux = getMorceaux(playlistId);
            for (PlaylistMp3 pm : morceaux) {
                Long mp3Id = pm.getMp3().getId();
                if (mp3IdsVus.add(mp3Id)) {
                    mp3Uniques.add(pm.getMp3());
                }
            }
        }

        // Calculer la durée totale fusionnée
        int dureeTotale = mp3Uniques.stream()
                .mapToInt(mp3 -> mp3.getDuree() != null ? mp3.getDuree() : 0)
                .sum();

        // Construire le nom depuis les noms des playlists sources
        String nomFusion = ids.stream()
                .map(id -> findById(id).getNom())
                .collect(Collectors.joining(" + "));

        // Créer la nouvelle playlist (CONFIRMEE directement, non modifiable)
        Playlist fusion = Playlist.builder()
                .nom("Fusion : " + nomFusion)
                .dureeCible(dureeTotale)
                .statut("CONFIRMEE")
                .utilisateur(utilisateur)
                .build();

        fusion = playlistRepository.save(fusion);

        // Ajouter les morceaux dédupliqués
        int ordre = 1;
        for (Mp3 mp3 : mp3Uniques) {
            PlaylistMp3 pm = PlaylistMp3.builder()
                    .playlist(fusion)
                    .mp3(mp3)
                    .ordre(ordre++)
                    .build();
            playlistMp3Repository.save(pm);
        }

        return fusion;
    }

    // ==============================
    // Suggestion automatique
    // ==============================

    public List<PlaylistMp3> suggerer(Long playlistId) {
        Playlist playlist = findById(playlistId);
        verifierBrouillon(playlist);

        viderPlaylist(playlistId);

        List<Mp3> tousMp3 = mp3Repository.findAll();
        List<PlaylistMp3> result = new ArrayList<>();
        int dureeAccumulee = 0;
        int ordre = 1;

        for (Mp3 mp3 : tousMp3) {
            if (mp3.getDuree() == null) continue;
            if (dureeAccumulee + mp3.getDuree() <= playlist.getDureeCible()) {
                PlaylistMp3 pm = PlaylistMp3.builder()
                        .playlist(playlist).mp3(mp3).ordre(ordre++).build();
                playlistMp3Repository.save(pm);
                result.add(pm);
                dureeAccumulee += mp3.getDuree();
            }
            if (dureeAccumulee >= playlist.getDureeCible()) break;
        }

        return result;
    }

    // ==============================
    // Suggestion avec filtres
    // ==============================

    public List<PlaylistMp3> suggererParFiltres(Long playlistId, List<String> artistes, List<String> genres) {
        Playlist playlist = findById(playlistId);
        verifierBrouillon(playlist);

        viderPlaylist(playlistId);

        List<Mp3> mp3Filtres = mp3Repository.findAll()
                .stream()
                .filter(mp3 -> {
                    boolean artisteOk = artistes.isEmpty() ||
                            artistes.stream().anyMatch(a -> a.equalsIgnoreCase(mp3.getArtiste()));
                    boolean genreOk = genres.isEmpty() ||
                            genres.stream().anyMatch(g -> g.equalsIgnoreCase(mp3.getGenre()));
                    return artisteOk && genreOk;
                })
                .toList();

        List<PlaylistMp3> result = new ArrayList<>();
        int dureeAccumulee = 0;
        int ordre = 1;

        for (Mp3 mp3 : mp3Filtres) {
            if (mp3.getDuree() == null) continue;
            if (dureeAccumulee + mp3.getDuree() <= playlist.getDureeCible()) {
                PlaylistMp3 pm = PlaylistMp3.builder()
                        .playlist(playlist).mp3(mp3).ordre(ordre++).build();
                playlistMp3Repository.save(pm);
                result.add(pm);
                dureeAccumulee += mp3.getDuree();
            }
            if (dureeAccumulee >= playlist.getDureeCible()) break;
        }

        return result;
    }

    // ==============================
    // Ajouter/Supprimer morceau
    // ==============================

    public PlaylistMp3 ajouterMorceau(Long playlistId, Long mp3Id) {
        Playlist playlist = findById(playlistId);
        verifierBrouillon(playlist);

        Mp3 mp3 = mp3Repository.findById(mp3Id)
                .orElseThrow(() -> new RuntimeException("MP3 introuvable"));

        int ordre = getMorceaux(playlistId).size() + 1;

        PlaylistMp3 pm = PlaylistMp3.builder()
                .playlist(playlist).mp3(mp3).ordre(ordre).build();

        return playlistMp3Repository.save(pm);
    }

    public void supprimerMorceau(Long playlistMp3Id) {
        PlaylistMp3 pm = playlistMp3Repository.findById(playlistMp3Id)
                .orElseThrow(() -> new RuntimeException("Morceau introuvable"));
        verifierBrouillon(pm.getPlaylist());
        playlistMp3Repository.delete(pm);
    }

    // ==============================
    // Morceaux
    // ==============================

    public List<PlaylistMp3> getMorceaux(Long playlistId) {
        return playlistMp3Repository.findAll()
                .stream()
                .filter(pm -> pm.getPlaylist().getId().equals(playlistId))
                .sorted(Comparator.comparingInt(PlaylistMp3::getOrdre))
                .toList();
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

    // ==============================
    // Helpers
    // ==============================

    private void verifierBrouillon(Playlist playlist) {
        if ("CONFIRMEE".equals(playlist.getStatut())) {
            throw new RuntimeException("Cette playlist est confirmée et ne peut plus être modifiée");
        }
    }

    private void viderPlaylist(Long playlistId) {
        List<PlaylistMp3> existants = playlistMp3Repository.findAll()
                .stream()
                .filter(pm -> pm.getPlaylist().getId().equals(playlistId))
                .toList();
        playlistMp3Repository.deleteAll(existants);
    }
}