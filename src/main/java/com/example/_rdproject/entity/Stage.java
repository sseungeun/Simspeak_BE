package com.example._rdproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private Character character;

    @Column(nullable = false)
    private Integer stageNumber; // 1단계, 2단계 이런식

    @Column(nullable = false)
    private String title; // 상황 지문 및 미션 타이틀

    @Column(name = "stage_type", nullable = false)
    private String stageType;

    @Column(name = "unlock_affinity_ratio")
    private Integer unlockAffinityRatio;

    @Column(name = "scenario_id")
    private String scenarioId;

    private String situationDescription; // 상황 묘사 지문 원문
}