package com.mp3.demo.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mp3")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mp3 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomFichier;

    private String titre;

    private String artiste;

    private String album;

    private String genre;

    private Integer duree;

    private Long taille;

    private String cheminStockage;

    private LocalDateTime dateUpload;

    private String statut;
}