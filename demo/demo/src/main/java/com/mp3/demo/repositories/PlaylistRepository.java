package com.mp3.demo.repositories;

import com.mp3.demo.entities.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistRepository
        extends JpaRepository<Playlist, Long> {
}