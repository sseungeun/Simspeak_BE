package com.example._rdproject.dto;

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
        private Long correction_id;
        private String original_sentence;
        private String corrected_sentence;
        private String grammar_feedback;
        private String corrected_audio_url;
    }
    @Getter @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionSummaryResponse {
        private String session_id;
        private Integer day_number;
        private Integer latest_day;
    }

    // 세션 종료 처리용 DTO
    @Getter @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionEndResponse {
        private String session_id;
        private Integer final_affinity;
        private String achieved_level;
    }
}
