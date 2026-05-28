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
    private boolean isInitialUnlocked; // 기획상 활성화 캐릭터 3명(true), 잠금 캐릭터 2명(false) 구분용
}