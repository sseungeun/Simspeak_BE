package com.example._rdproject.dto;

import com.example._rdproject.domain.CefrLevelType;
import com.example._rdproject.domain.ChatInputType;
import com.example._rdproject.domain.LevelTestType;
import com.example._rdproject.entity.User;
import lombok.*;

import java.util.List;

public class LevelTestDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveRequest {
        private Long userId;
        private LevelTestType levelTestType; // SELECT 또는 TEST
        private CefrLevelType cefrLevelType; // A1 ~ C2
        private Integer testScore; // 점수 (SELECT일 경우 null 가능)
    }
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusResponse {
        private Long userId;
        private CefrLevelType currentLevel;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionListResponse {
        private List<QuestionDto> questions;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionDto {
        private Long questionId;
        private String questionText;
        private String difficultyLevel; // 문항별 목표 레벨
        private String category;        // 문항 유형
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitAnswerRequest {
        private Long userId;
        private Long questionId;
        private String answerText;
        private ChatInputType answerType; // VOICE 또는 TEXT
    }
}