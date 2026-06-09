package com.example._rdproject.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import java.util.List;

@Getter
public class AiResponseDto {
    @JsonProperty("text_content")
    private String textContent;

    @JsonProperty("action_description")
    private String actionDescription;

    @JsonProperty("affinity_delta")
    private int affinityDelta;

    @JsonProperty("system_notification")
    private String systemNotification;

    @JsonProperty("is_active")
    private boolean isActive;

    @JsonProperty("system_evaluation")
    private SystemEvaluation systemEvaluation;

    @JsonProperty("audio_url")
    private String audioUrl;

    @JsonProperty("current_total_affinity")
    private int currentTotalAffinity;

    @JsonProperty("user_recognized_text")
    private String userRecognizedText;

    @Getter
    public static class SystemEvaluation {
        @JsonProperty("is_penalty")
        private boolean isPenalty;
        private String grammar_feedback;
        private String pronunciation_feedback;
        private List<CorrectionItem> corrections_json;

        @JsonProperty("pronunciation_evaluations")
        private PronunciationEvaluations pronunciationEvaluations;
    }

    @Getter
    public static class CorrectionItem {
        private String original_sentence;
        private String corrected_sentence;
        private String corrected_audio_url;
    }

    @Getter
    public static class PronunciationEvaluations {
        private int accuracy;
        private int fluency;
        private int completeness;
        private int prosody;

        @JsonProperty("word_details_json")
        private List<WordDetail> wordDetails;
    }

    @Getter
    public static class WordDetail {
        private String word;
        private int accuracy;
        private String error_type;
        private String guide;
    }
}