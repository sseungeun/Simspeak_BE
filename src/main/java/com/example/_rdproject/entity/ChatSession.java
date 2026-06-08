package com.example._rdproject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_sessions")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {
    @Id
    @Column(name = "session_id")
    private String sessionId; // "sess_leo_20260526_999"

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "character_id")
    private String characterId;

    @Column(name = "stage_id")
    private Long stageId;

    @Column(name = "scenario_id")
    private String scenarioId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
