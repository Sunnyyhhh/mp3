package com.mp3.demo.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "playlist_mp3")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaylistMp3 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "playlist_id")
    private Playlist playlist;

    @ManyToOne
    @JoinColumn(name = "mp3_id")
    private Mp3 mp3;

    private Integer ordre;
}