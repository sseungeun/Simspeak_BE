package com.example._rdproject.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

public class EvaluationDto {

    @Getter @Builder
    public static class Response {
        private GrammarInfo grammar;
        private ExpressionInfo expression;
        private PronunciationInfo pronunciation;
    }

    // 1. 문법
    @Getter @Builder
    public static class GrammarInfo {
        @JsonProperty("grammar_feedback")
        private String grammarFeedback;
    }

    // 2. 표현
    @Getter @Builder
    public static class ExpressionInfo {
        @JsonProperty("corrections_json")
        private List<CorrectionItem> correctionsJson;

        @JsonProperty("detected_invalid_words")
        private List<String> detectedInvalidWords;
    }

    @Getter @Builder
    public static class CorrectionItem {
        @JsonProperty("original_sentence")
        private String originalSentence;

        @JsonProperty("corrected_sentence")
        private String correctedSentence;

        @JsonProperty("corrected_audio_url")
        private String correctedAudioUrl;
    }

    // 3. 발음
    @Getter @Builder
    public static class PronunciationInfo {
        private Integer accuracy;
        private Integer fluency;

        @JsonProperty("word_details_json")
        private Object wordDetailsJson;
    }
}
