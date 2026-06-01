package com.example._rdproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "characters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Character {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_initial_unlocked", nullable = false)
    private boolean isInitialUnlocked;

    @Column(nullable = false, length = 4)
    private String mbti;

    @Column(name = "stat_affinity")
    private int statAffinity;

    @Column(name = "stat_tsundere")
    private int statTsundere;

    @Column(name = "stat_wit")
    private int statWit;
}