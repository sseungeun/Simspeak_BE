package com.example._rdproject.entity;

import com.example._rdproject.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_character_status")
@Getter @Setter @NoArgsConstructor
public class UserCharacterStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "character_id", nullable = false)
    private String characterId;

    @Column(name = "current_affinity", nullable = false)
    private int currentAffinity = 0;

    @Column(name = "remaining_penalties", nullable = false)
    private int remainingPenalties = 3;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}