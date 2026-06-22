package com.mp3.demo.repositories;

import com.mp3.demo.entities.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilisateurRepository
        extends JpaRepository<Utilisateur, Long> {
}