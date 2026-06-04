package com.example._rdproject.dto;

import com.example._rdproject.entity.Stage;
import com.example._rdproject.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class UserStageProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @Column(name = "is_unlocked", nullable = false)
    private boolean isUnlocked = false;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;

    @Column(name = "best_score")
    private Integer bestScore; // null 값을 가질 수 있으므로 Wrapper 클래스 사용

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
