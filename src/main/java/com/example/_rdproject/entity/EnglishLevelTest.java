package com.example._rdproject.entity;

import com.example._rdproject.domain.CefrLevelType;
import com.example._rdproject.domain.LevelTestType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "english_level_tests")
public class EnglishLevelTest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", nullable = false)
    private LevelTestType testType;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_level", nullable = false)
    private CefrLevelType assignedLevel;

    @Column(name = "test_score")
    private Integer testScore;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}