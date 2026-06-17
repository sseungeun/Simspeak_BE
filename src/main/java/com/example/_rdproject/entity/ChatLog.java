package com.example._rdproject.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "chat_logs")
public class ChatLog {

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

    @Column(name = "turn_count")
    private Integer turnCount;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "user_text", columnDefinition = "text")
    private String userText;

    @Column(name = "user_audio_url", columnDefinition = "text")
    private String userAudioUrl;

    @Column(name = "ai_text_content", columnDefinition = "text")
    private String aiTextContent;

    @Column(name = "ai_audio_url", columnDefinition = "text")
    private String aiAudioUrl;

    @Column(name = "current_affinity")
    private Integer currentAffinity;

    @Column(name = "summary_context", columnDefinition = "text")
    private String summaryContext;

    @Column(name = "stage_id", length = 50)
    private String stageId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "chat_history_context", columnDefinition = "jsonb")
    private Map<String, Object> chatHistoryContext;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_llm_log", columnDefinition = "jsonb")
    private Map<String, Object> rawLlmLog;

    @Column(name = "grammar_feedback", columnDefinition = "text")
    private String grammarFeedback;

    @CreationTimestamp // 자동으로 생성 시간 저장
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}