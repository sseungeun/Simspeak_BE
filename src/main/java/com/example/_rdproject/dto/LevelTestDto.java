package com.example._rdproject.dto;

import com.example._rdproject.domain.CefrLevelType;
import com.example._rdproject.domain.ChatInputType;
import com.example._rdproject.domain.LevelTestType;
import com.example._rdproject.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

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

        private String characterId;
        private Integer currentQuestionIndex;
        private String userAudioUrl;
        private List<String> accumulatedAnswers;
        private Boolean isQuit;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AiLevelTestRequest {
        @JsonProperty("user_id")
        private Integer userId;

        @JsonProperty("character_id")
        private String characterId;

        @JsonProperty("current_question_index")
        private Integer currentQuestionIndex;

        @JsonProperty("user_text")
        private String userText;

        @JsonProperty("user_audio_url")
        private String userAudioUrl;

        @JsonProperty("accumulated_answers")
        private List<String> accumulatedAnswers;

        @JsonProperty("is_quit")
        private Boolean isQuit;
    }

    @Getter
    @Builder
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiLevelTestResponse {
        @JsonProperty("user_recognized_text")
        private String userRecognizedText;

        @JsonProperty("pronunciation_evaluations")
        private PronunciationEvaluations pronunciationEvaluations;

        @JsonProperty("is_finished")
        private Boolean isFinished;

        @JsonProperty("final_result")
        private FinalResult finalResult;

        @JsonProperty("next_question_text")
        private String nextQuestionText;

        @JsonProperty("next_question_audio_url")
        private String nextQuestionAudioUrl;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PronunciationEvaluations {
        private int accuracy;
        private int fluency;
        private int completeness;
        private int prosody;

        @JsonProperty("word_details_json")
        private List<Map<String, Object>> wordDetailsJson;
    }
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinalResult {
        @JsonProperty("assigned_level")
        private String assignedLevel; // 예: "B2"

        @JsonProperty("test_score")
        private Integer testScore;

        @JsonProperty("fluency_score")
        private Integer fluencyScore;

        @JsonProperty("expression_score")
        private Integer expressionScore;

        @JsonProperty("grammar_score")
        private Integer grammarScore;

        @JsonProperty("task_completion_score")
        private Integer taskCompletionScore;

        @JsonProperty("vocabulary_score")
        private Integer vocabularyScore;
    }

}