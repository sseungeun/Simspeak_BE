package com.example._rdproject.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "corrections")
public class Correction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_log_id")
    private ChatLog chatLog;

    @Column(name = "original_sentence", length = Integer.MAX_VALUE)
    private String originalSentence;

    @Column(name = "corrected_sentence", length = Integer.MAX_VALUE)
    private String correctedSentence;

    @Column(name = "corrections_json")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> correctionsJson;

    @Column(name = "translation", length = Integer.MAX_VALUE)
    private String translation;

    @ColumnDefault("false")
    @Column(name = "is_reviewed")
    private Boolean isReviewed;

}