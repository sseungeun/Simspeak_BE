package com.example._rdproject.entity;

import com.example._rdproject.domain.StageType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stages")
public class Stage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private Character character;

    @NotNull
    @Column(name = "stage_number", nullable = false)
    private Integer stageNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage_type")
    private StageType stageType = StageType.regular;

    @Column(name = "unlock_affinity_ratio")
    private Integer unlockAffinityRatio = 0;

    @Column(name = "korean_allowed_rate")
    private Integer koreanAllowedRate = 30;

    @Column(name = "title")
    private String title;

    @Column(name = "scenario_id")
    private String scenarioId;
}