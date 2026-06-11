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

        // 유저 발화 인식 텍스트 (프론트엔드 전달용)
        @JsonProperty("user_recognized_text")
        private String userRecognizedText;

        // 알아서 null 처리
        @JsonProperty("system_evaluation")
        private AiResponseDto.SystemEvaluation systemEvaluation;

        @JsonProperty("audio_url")
        private String audioUrl;

        @JsonProperty("current_total_affinity")
        private Integer currentTotalAffinity;
    }
    public static class PronunciationScore {
        private Double accuracy;
        private Double fluency;
        private Double completeness;
        private Double prosody;
    }
}