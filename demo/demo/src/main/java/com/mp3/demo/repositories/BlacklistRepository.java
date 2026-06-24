package com.mp3.demo.repositories;

import com.mp3.demo.entities.Blacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {
    List<Blacklist> findByPlaylistId(Long playlistId);
    void deleteByPlaylistIdAndTypeAndValeur(Long playlistId, String type, String valeur);
}