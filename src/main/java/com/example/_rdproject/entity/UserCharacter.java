package com.example._rdproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_characters", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "character_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private Character character;

    @Column(name = "affinity_score", nullable = false)
    private Integer affinityScore = 0; // 누적 호감도

    @Column(name = "is_unlocked", nullable = false)
    private boolean isUnlocked; // 유저별 해금 여부 (기본 활성화 캐릭터는 true 시작)

    @Builder
    public UserCharacter(User user, Character character, Integer affinityScore, boolean isUnlocked) {
        this.user = user;
        this.character = character;
        this.affinityScore = affinityScore;
        this.isUnlocked = isUnlocked;
    }
}