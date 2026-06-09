package com.example._rdproject.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

public class CorrectionDto {
    @Getter
    @NoArgsConstructor
    public static class CorrectionUpdateRequest {
        private String translation;
        private Boolean is_reviewed;
    }

    @Getter @Builder
    public static class CorrectionUpdateResponse {
        private Long correction_id;
        private Boolean is_reviewed;
        private Instant updated_at;
    }

    @Getter @Builder
    public static class CorrectionSummary {
        private Long correction_id;
        private String original_sentence;
        private String corrected_sentence;
        private String corrected_audio_url;
    }
}