package com.mp3.demo.controllers;

import com.mp3.demo.entities.Utilisateur;
import com.mp3.demo.services.UtilisateurService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.context.annotation.Profile;

@Profile("api")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UtilisateurController {

    @Autowired
    private final UtilisateurService utilisateurService;

    @PostMapping
    public Utilisateur create(@RequestBody Utilisateur utilisateur) {
        return utilisateurService.save(utilisateur);
    }

    @GetMapping
    public List<Utilisateur> getAll() {
        return utilisateurService.findAll();
    }

    @GetMapping("/{id}")
    public Utilisateur getById(@PathVariable Long id) {
        return utilisateurService.findById(id);
    }

    @PutMapping("/{id}")
    public Utilisateur update(
            @PathVariable Long id,
            @RequestBody Utilisateur utilisateur
    ) {
        return utilisateurService.update(id, utilisateur);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        utilisateurService.delete(id);
    }
}