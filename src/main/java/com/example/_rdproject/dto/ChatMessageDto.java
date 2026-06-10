package com.example._rdproject.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;
import java.util.Map;

public class ChatMessageDto {

    // --- Request DTO ---
    @Getter @NoArgsConstructor
    @Builder
    @AllArgsConstructor
    public static class Request {
        private String sessionId;
        private String textContent;
        private String inputType;
        private String characterId;
        private String scenarioId;
        @Builder.Default
        private String targetLanguage = "en-US";
        private Integer stageLevel;
        private String userLevel;
        private Integer turnCount;
        private Integer currentAffinity;
        private List<HistoryItemDto> history;
    }

    @Getter @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        @JsonProperty("text_content")
        private String textContent;

        @JsonProperty("action_description")
        private String actionDescription;

        @JsonProperty("affinity_delta")
        private Integer affinityDelta;

        @JsonProperty("is_active")
        private Boolean isActive;

        @JsonProperty("system_evaluation")
        private SystemEvaluation systemEvaluation;

        @JsonProperty("audio_url")
        private String audioUrl;

        @JsonProperty("current_total_affinity")
        private Integer currentTotalAffinity;
    }

    @Getter @Setter
    public static class SystemEvaluation {
        @JsonProperty("grammar_feedback")
        private String grammarFeedback;

        @JsonProperty("is_penalty")
        private Boolean isPenalty;

        @JsonProperty("pronunciation_score")
        private PronunciationScore pronunciationScore;
    }

    @Getter @Setter
    public static class PronunciationScore {
        private Integer accuracy;
        private Integer fluency;
        private Integer completeness;
        private Integer prosody;

        @JsonProperty("word_details")
        private List<WordDetail> wordDetails;
    }

    @Getter @Setter
    public static class WordDetail {
        private String word;
        private Integer accuracy;

        @JsonProperty("error_type")
        private String errorType;
    }
}