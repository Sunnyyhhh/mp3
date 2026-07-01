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

    public Playlist confirmer(Long playlistId) {
        Playlist playlist = findById(playlistId);
        playlist.setStatut("CONFIRMEE");
        return playlistRepository.save(playlist);
    }

    public Playlist fusionner(List<Long> ids, Long utilisateurId) {
        if (ids == null || ids.size() < 2) {
            throw new RuntimeException("Il faut au moins 2 playlists pour fusionner");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

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

        int dureeTotale = mp3Uniques.stream()
                .mapToInt(mp3 -> mp3.getDuree() != null ? mp3.getDuree() : 0)
                .sum();

        String nomFusion = ids.stream()
                .map(id -> findById(id).getNom())
                .collect(Collectors.joining(" + "));

        Playlist fusion = Playlist.builder()
                .nom("Fusion : " + nomFusion)
                .dureeCible(dureeTotale)
                .statut("CONFIRMEE")
                .utilisateur(utilisateur)
                .build();

        fusion = playlistRepository.save(fusion);

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

    /*public List<PlaylistMp3> suggerer(Long playlistId) {
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
    }*/
    public List<PlaylistMp3> suggerer(Long playlistId) {
        Playlist playlist = findById(playlistId);
        verifierBrouillon(playlist);
        viderPlaylist(playlistId);

        List<Mp3> candidats = mp3Repository.findAll()
                .stream()
                .filter(mp3 -> mp3.getDuree() != null && mp3.getDuree() > 0)
                .toList();

        return enregistrerMeilleurMix(playlist, candidats);
    }

    public List<PlaylistMp3> suggererParFiltres(Long playlistId, List<String> artistes, List<String> genres) {
        Playlist playlist = findById(playlistId);
        verifierBrouillon(playlist);
        viderPlaylist(playlistId);

        List<Mp3> candidats = mp3Repository.findAll()
                .stream()
                .filter(mp3 -> mp3.getDuree() != null && mp3.getDuree() > 0)
                .filter(mp3 -> {
                    boolean artisteOk = artistes.isEmpty() ||
                            artistes.stream().anyMatch(a -> a.equalsIgnoreCase(mp3.getArtiste()));
                    boolean genreOk = genres.isEmpty() ||
                            genres.stream().anyMatch(g -> g.equalsIgnoreCase(mp3.getGenre()));
                    return artisteOk && genreOk;
                })
                .toList();

        return enregistrerMeilleurMix(playlist, candidats);
    }

    /**
     * Cherche, parmi les candidats, le sous-ensemble dont la somme des durées
     * est la plus proche possible de dureeCible SANS la dépasser (knapsack 0/1).
     * Puis persiste le résultat comme morceaux de la playlist.
     */
    private List<PlaylistMp3> enregistrerMeilleurMix(Playlist playlist, List<Mp3> candidats) {
        List<Mp3> meilleurMix = trouverMeilleurMix(candidats, playlist.getDureeCible());

        List<PlaylistMp3> result = new ArrayList<>();
        int ordre = 1;
        for (Mp3 mp3 : meilleurMix) {
            PlaylistMp3 pm = PlaylistMp3.builder()
                    .playlist(playlist).mp3(mp3).ordre(ordre++).build();
            playlistMp3Repository.save(pm);
            result.add(pm);
        }
        return result;
    }

    /**
     * Knapsack 0/1 : trouve le sous-ensemble de mp3 dont la somme des durées
     * est maximale sans dépasser dureeCible (= la plus proche possible en dessous).
     * dp[i][c] = true si, en utilisant les i premiers candidats, on peut atteindre
     * exactement une somme de durée c (c <= dureeCible).
     */
    private List<Mp3> trouverMeilleurMix(List<Mp3> candidats, Integer dureeCible) {
        int capacite = (dureeCible == null || dureeCible <= 0) ? 0 : dureeCible;
        int n = candidats.size();

        if (capacite == 0 || n == 0) {
            return new ArrayList<>();
        }

        boolean[][] dp = new boolean[n + 1][capacite + 1];
        dp[0][0] = true;

        for (int i = 1; i <= n; i++) {
            int duree = candidats.get(i - 1).getDuree();
            for (int c = 0; c <= capacite; c++) {
                dp[i][c] = dp[i - 1][c];
                if (!dp[i][c] && c >= duree && dp[i - 1][c - duree]) {
                    dp[i][c] = true;
                }
            }
        }

        // Cherche la plus grande somme atteignable <= capacite
        int meilleureSomme = 0;
        for (int c = capacite; c >= 0; c--) {
            if (dp[n][c]) {
                meilleureSomme = c;
                break;
            }
        }

        // Reconstruction du sous-ensemble
        List<Mp3> resultat = new ArrayList<>();
        int c = meilleureSomme;
        for (int i = n; i >= 1; i--) {
            if (c > 0 && !dp[i - 1][c]) {
                Mp3 mp3 = candidats.get(i - 1);
                resultat.add(mp3);
                c -= mp3.getDuree();
            }
        }

        Collections.reverse(resultat);
        return resultat;
    }

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

    public List<PlaylistMp3> getMorceaux(Long playlistId) {
        return playlistMp3Repository.findAll()
                .stream()
                .filter(pm -> pm.getPlaylist().getId().equals(playlistId))
                .sorted(Comparator.comparingInt(PlaylistMp3::getOrdre))
                .toList();
    }

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