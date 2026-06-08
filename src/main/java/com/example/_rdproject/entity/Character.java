package com.example._rdproject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "characters")
public class Character {
    @Id
    @Column(name = "id")
    private String id; // character_id

    @Column(name = "name")
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "mbti")
    private String mbti;

    @Column(name = "stat_affinity")
    private Integer statAffinity = 0;

    @Column(name = "stat_tsundere")
    private Integer statTsundere = 0;

    @Column(name = "stat_wit")
    private Integer statWit = 0;
}