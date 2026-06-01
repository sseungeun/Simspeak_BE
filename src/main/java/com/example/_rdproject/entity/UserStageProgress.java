package com.example._rdproject.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_stage_progress")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserStageProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 테이블의 user_id bigint 외래키 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 테이블의 stage_id bigint 외래키 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @Column(name = "is_unlocked", nullable = false)
    private boolean isUnlocked;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted;

    @Column(name = "best_score")
    private Integer bestScore; // INT 타입 매핑 (Null 허용)

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // === 비즈니스 도메인 로직 ===

    public static UserStageProgress createFirstComplete(User user, Stage stage, int score) {
        return UserStageProgress.builder()
                .user(user)
                .stage(stage)
                .isUnlocked(true)    // 클리어했으니 당연히 해금 상태
                .isCompleted(true)   // 통과했으므로 true
                .bestScore(score)    // 획득한 점수를 최고 점수로 초기화
                .build();
    }

    public static UserStageProgress createNextUnlock(User user, Stage stage) {
        return UserStageProgress.builder()
                .user(user)
                .stage(stage)
                .isUnlocked(true)
                .isCompleted(false)  // 아직 플레이 전이므로 false
                .bestScore(null)     // 점수 없음
                .build();
    }

    /**
     * 현재 단계 완료 처리 및 최고 점수 기록
     */
    public void completeStage(int score) {
        this.isCompleted = true;
        if (this.bestScore == null || score > this.bestScore) {
            this.bestScore = score;
        }
    }

    /**
     * 잠금 해제(Unlock) 처리
     */
    public void unlock() {
        this.isUnlocked = true;
    }
}