package com.example._rdproject.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class ReportDto {
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionReportResponse {
        private String session_id;
        private String scenario_id;
        private ChatMessageDto.PronunciationScore average_pronunciation;
        private List<CorrectionItem> corrections;
    }

    @Getter @Builder
    public static class CorrectionItem {
        @JsonProperty("correction_id")
        private Long correction_id;
        @JsonProperty("original_sentence")
        private String original_sentence;
        @JsonProperty("corrected_sentence")
        private String corrected_sentence;
        @JsonProperty("grammar_feedback")
        private String grammar_feedback;
        @JsonProperty("corrected_audio_url")
        private String corrected_audio_url;
    }
    @Getter @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionSummaryResponse {
        @JsonProperty("session_id")
        private String sessionId;

        @JsonProperty("day_number")
        private Integer dayNumber;

        @JsonProperty("latest_day")
        private Integer latestDay;
    }

    // 세션 종료 처리용 DTO
    @Getter @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionEndResponse {
        @JsonProperty("session_id")
        private String sessionId;

        @JsonProperty("final_affinity")
        private Integer finalAffinity;

        @JsonProperty("achieved_level")
        private String achievedLevel;
    }
}
