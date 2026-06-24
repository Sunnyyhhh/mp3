package com.mp3.demo.services;
import com.mp3.demo.entities.Mp3;
import com.mp3.demo.repositories.Mp3Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

import org.springframework.context.annotation.Profile;

@Profile("api")
@Service
@RequiredArgsConstructor
public class Mp3Service {
    private final Mp3Repository mp3Repository; 

    public Mp3 save(Mp3 mp3) {
        return mp3Repository.save(mp3);
    }
    public List<Mp3> findAll() {
        return mp3Repository.findAll();
    }
    public Mp3 findById(Long id) {
        return mp3Repository.findById(id)
                .orElseThrow(() -> new RuntimeException("MP3 introuvable"));
    }
    public void delete(Long id) {
        mp3Repository.deleteById(id);
    }
}