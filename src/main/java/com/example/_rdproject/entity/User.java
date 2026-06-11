package com.example._rdproject.entity;

import com.example._rdproject.domain.CefrLevelType;
import com.example._rdproject.domain.GenderType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "login_id")
    private String loginId;

    @Column(name = "password")
    private String password;

    @Column(name = "guest_id")
    private String guestId;

    @NotNull
    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "preferred_partner_gender", nullable = false)
    private GenderType preferredPartnerGender;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_level")
    private CefrLevelType currentLevel;

    @NotNull
    @Column(name = "continuous_days", nullable = false)
    private Integer continuousDays = 0;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_character_id") // DB의 컬럼명과 일치
    private Character lastCharacter;

    // 상태 업데이트 메서드 추가
    public void updateCurrentLevel(CefrLevelType newLevel) {
        this.currentLevel = newLevel;
    }

    public CefrLevelType getCurrentLevel() {
        return this.currentLevel;
    }

    public void updateLastCharacter(Character character) {
        this.lastCharacter = character;
    }

    // --- [마이페이지 프로필 수정용 스위치 메서드] ---
    public void updateProfile(String newNickname, com.example._rdproject.domain.GenderType newGender) {
        this.nickname = newNickname;
        this.preferredPartnerGender = newGender; 
    }
}