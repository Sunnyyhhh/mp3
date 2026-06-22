package com.mp3.demo.repositories;

import com.mp3.demo.entities.PlaylistMp3;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistMp3Repository
        extends JpaRepository<PlaylistMp3, Long> {
}