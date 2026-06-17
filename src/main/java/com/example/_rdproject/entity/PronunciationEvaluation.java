package com.example._rdproject.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "pronunciation_evaluations")
public class PronunciationEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_log_id", nullable = true) // 일반 채팅일 때 사용
    private ChatLog chatLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_history_id", nullable = true) // 레벨 테스트일 때 사용
    private AnswerHistory answerHistory;

    @Column(name = "accuracy")
    private Integer accuracy;

    @Column(name = "fluency")
    private Integer fluency;

    @Column(name = "completeness")
    private Integer completeness;

    @Column(name = "prosody")
    private Integer prosody;

    @Column(name = "word_details_json")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, Object>> wordDetailsJson;

}