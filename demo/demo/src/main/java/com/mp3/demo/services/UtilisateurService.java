package com.mp3.demo.services;

import com.mp3.demo.entities.Utilisateur;
import com.mp3.demo.repositories.UtilisateurRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    @Autowired
    private final UtilisateurRepository utilisateurRepository;

    public Utilisateur save(Utilisateur utilisateur) {
        return utilisateurRepository.save(utilisateur);
    }

    public List<Utilisateur> findAll() {
        return utilisateurRepository.findAll();
    }

    public Utilisateur findById(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    public Utilisateur update(Long id, Utilisateur utilisateur) {
        Utilisateur existing = findById(id);

        existing.setNom(utilisateur.getNom());
        existing.setEmail(utilisateur.getEmail());
        existing.setMotDePasse(utilisateur.getMotDePasse());

        return utilisateurRepository.save(existing);
    }

    public void delete(Long id) {
        utilisateurRepository.deleteById(id);
    }
}