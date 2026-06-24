package com.mp3.demo.repositories;

import com.mp3.demo.entities.Mp3;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Mp3Repository
        extends JpaRepository<Mp3, Long> {
}