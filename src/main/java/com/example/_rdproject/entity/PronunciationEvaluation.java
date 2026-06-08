package com.example._rdproject.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @JoinColumn(name = "chat_log_id")
    private ChatLog chatLog;

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
    private Map<String, Object> wordDetailsJson;

}