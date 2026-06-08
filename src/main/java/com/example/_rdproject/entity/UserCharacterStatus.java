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
@Table(name = "user_character_status")
public class UserCharacterStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private Character character;

    @ColumnDefault("0")
    @Column(name = "current_affinity")
    private Integer currentAffinity;

    @ColumnDefault("3")
    @Column(name = "remaining_penalties")
    private Integer remainingPenalties;

    @Column(name = "current_level") // 사용자의 실력 레벨 (예: A1, B2)
    private String userLevel;

    @Column(name = "current_turn") // 현재 진행 중인 턴 (AI 서버 요청 시 바로 쓰기 위함)
    private Integer turnCount;

}