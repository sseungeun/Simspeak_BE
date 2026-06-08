package com.example._rdproject.entity;

import com.example._rdproject.domain.ChatInputType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "question")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "question_text")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type")
    private ChatInputType questionType; // ChatInputType enum 사용

    @Column(name = "category")
    private String category;

    @Column(name = "difficulty_level")
    private String difficultyLevel;
}