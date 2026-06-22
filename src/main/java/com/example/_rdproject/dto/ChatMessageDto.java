package com.example._rdproject.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@ToString
public class ChatMessageDto {

    // --- AI 서버 전송용 Request (chatLogId 필드 유실 오류 수정 완료) ---
    @Getter @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AiRequest {
        @JsonProperty("user_id")
        private int userId;

        @JsonProperty("character_id")
        private String characterId;

        private String text;

        @JsonProperty("is_video_call")
        private boolean isVideoCall;

        @JsonProperty("user_audio_url")
        private String userAudioUrl;

        @JsonProperty("stage_id")
        private int stageId;

        private List<HistoryItemDto> history;

        @JsonProperty("chat_log_id")
        private Long chatLogId;
    }

    // --- 기존 서비스 로직용 Request (유지) ---
    @Getter @NoArgsConstructor
    @Builder
    @AllArgsConstructor
    public static class Request {
        private String sessionId;
        private String text;
        private String inputType;
        private String characterId;
        private String scenarioId;
        @Builder.Default
        private String targetLanguage = "en-US";
        private Integer stageLevel;
        private String userLevel;
        private Integer turnCount;
        private Integer currentAffinity;
        private String userAudioUrl;
        private List<HistoryItemDto> history;
    }

    // --- 응답 DTO (AI 서버에서 들어오는 원본 응답) ---
    @Getter @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        @JsonProperty("text_content")
        private String text;

        @JsonProperty("action_description")
        private String actionDescription;

        @JsonProperty("affinity_delta")
        private Integer affinityDelta;

        @JsonProperty("current_total_affinity")
        private Integer currentTotalAffinity;

        @JsonProperty("user_recognized_text")
        private String userRecognizedText;

        @JsonProperty("audio_url")
        private String audioUrl;

        @JsonProperty("model_info")
        private String modelInfo;

        @JsonProperty("system_evaluation")
        private AiResponseDto.SystemEvaluation systemEvaluation;
    }

    // --- 프론트엔드 반환용 최종 응답 DTO ---
    @Getter @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FrontendResponse {
        @JsonProperty("text")
        private String text;

        @JsonProperty("action_description")
        private String actionDescription;

        @JsonProperty("affinity_delta")
        private Integer affinityDelta;

        @JsonProperty("current_total_affinity")
        private Integer currentTotalAffinity;

        @JsonProperty("audio_url")
        private String audioUrl;

        // (Grammar, Expression, Pronunciation)
        @JsonProperty("system_evaluation")
        private EvaluationDto.Response systemEvaluation;

        @JsonProperty("user_recognized_text")
        private String userRecognizedText;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PronunciationScore {
        private int accuracy;
        private int fluency;
        private int completeness;
        private int prosody;
    }

    /**
     * AI 서버가 백그라운드 연산 종료 후 보낼 Callback 요청 바디
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiCallbackRequest {
        @JsonProperty("chat_log_id")
        private Long chatLogId;

        @JsonProperty("system_evaluation")
        private AiServerEvaluation systemEvaluation;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiServerEvaluation {
        @JsonProperty("grammar_feedback")
        private String grammarFeedback;

        @JsonProperty("detected_invalid_words")
        private String detectedInvalidWords;

        @JsonProperty("corrections_json")
        private List<AiServerCorrectionItem> correctionsJson;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiServerCorrectionItem {
        @JsonProperty("original_sentence")
        private String originalSentence;

        @JsonProperty("corrected_sentence")
        private String correctedSentence;

        @JsonProperty("corrected_audio_url")
        private String correctedAudioUrl;
    }
}