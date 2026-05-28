package com.example._rdproject.entity;

import com.example._rdproject.domain.AssignedLevel;
import com.example._rdproject.domain.TestType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "english_level_tests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LevelTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 대다수 연관관계는 비즈니스 로직 및 성능(Lazy Loading)을 위해 ManyToOne 객체 참조로 잡는 것이 좋습니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", nullable = false)
    private TestType testType;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_level", nullable = false)
    private AssignedLevel assignedLevel;

    @Column(name = "test_score")
    private Integer testScore;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public LevelTest(User user, TestType testType, AssignedLevel assignedLevel, Integer testScore) {
        this.user = user;
        this.testType = testType;
        this.assignedLevel = assignedLevel;
        this.testScore = testScore;
    }
}