package com.example._rdproject.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_stage_progress")
public class UserStageProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    private Stage stage;

    @ColumnDefault("false")
    @Column(name = "is_completed")
    private Boolean isCompleted;

    @ColumnDefault("false")
    @Column(name = "is_unlocked")
    private Boolean isUnlocked;

    @Column(name = "best_score")
    private Integer bestScore;

    public void updateScoreAndComplete(int score) {
        this.isCompleted = true;
        // 기존 점수가 없거나, 새로운 점수가 더 높을 때만 갱신
        if (this.bestScore == null || score > this.bestScore) {
            this.bestScore = score;
        }
    }
}