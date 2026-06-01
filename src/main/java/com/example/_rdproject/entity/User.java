package com.example._rdproject.entity;

import com.example._rdproject.domain.AssignedLevel;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", unique = true)
    private String loginId;

    private String password;

    @Column(name = "guest_id", unique = true)
    private String guestId;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_partner_gender")
    private Gender preferredPartnerGender;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_level")
    private CefrLevel currentLevel;

    @Column(name = "continuous_days", nullable = false)
    @Builder.Default
    private Integer continuousDays = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_selected_character_id")
    private Character lastCharacter;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // 선호 성별 Enum
    public enum Gender {
        male, female
    }

    // CEFR 등급 Enum (기획서에 맞춰 초기 가입 시엔 null 허용해야 하므로 컬럼 제약 해제)
    public enum CefrLevel {
        A1, A2, B1, B2, C1, C2
    }
    public void updateCurrentLevel(CefrLevel level) {
        this.currentLevel = level;
    }
    public void updateLastCharacter(Character character) {
        this.lastCharacter = character;
    }
}