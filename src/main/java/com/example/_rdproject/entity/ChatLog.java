package com.example._rdproject.entity;

import com.example._rdproject.domain.ChatInputType;
import com.example._rdproject.domain.ChatRoleType;
import com.example._rdproject.domain.PenaltyReasonType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "chat_log")
public class ChatLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "message_id", unique = true)
    private String messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private Character character;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "scenario_id")
    private String scenarioId;

    @Column(name = "turn_count")
    private Integer turnCount;

    @Column(name = "action_description", columnDefinition = "text")
    private String actionDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private ChatRoleType role;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type")
    private ChatInputType inputType;

    @Column(name = "text_content", columnDefinition = "text")
    private String textContent;

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(name = "grammar_feedback", columnDefinition = "text")
    private String grammarFeedback;

    @Column(name = "is_penalty")
    private Boolean isPenalty;

    @Column(name = "affinity_delta")
    private Integer affinityDelta;

    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_reason")
    private PenaltyReasonType penaltyReason;

    @Column(name = "created_at")
    private Instant createdAt;
}