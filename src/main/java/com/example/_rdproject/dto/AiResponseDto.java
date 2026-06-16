package com.example._rdproject.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import java.util.List;
import java.util.Map;

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
        @JsonProperty("detected_invalid_words")
        private List<String> detectedInvalidWords;

        @JsonProperty("is_penalty")
        private boolean isPenalty;

        @JsonProperty("grammar_feedback")
        private String grammarFeedback;

        @JsonProperty("pronunciation_feedback")
        private String pronunciationFeedback;

        @JsonProperty("corrections_json")
        private List<CorrectionItem> correctionsJson;

        @JsonProperty("ipa_guides")
        private Map<String, String> ipaGuides;

        @JsonProperty("pronunciation_evaluations")
        private PronunciationEvaluations pronunciationEvaluations;

        @JsonProperty("affinity_delta")
        private int affinityDelta;

        @JsonProperty("current_total_affinity")
        private int currentTotalAffinity;
    }

    @Getter
    public static class CorrectionItem {
        @JsonProperty("original_sentence")
        private String originalSentence;

        @JsonProperty("corrected_sentence")
        private String correctedSentence;

        @JsonProperty("corrected_audio_url")
        private String correctedAudioUrl;
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
        @JsonProperty("error_type")
        private String errorType;
        private String guide;
    }
}