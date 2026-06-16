package com.example._rdproject.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

public class ChatLogDto {

    @Getter
    @Builder
    public static class HistoryResponse {
        private String session_id;
        private List<LogItem> logs;
    }

    @Getter
    @Builder
    public static class LogItem {
        private Long log_id;
        private String user_text;
        private String user_audio_url;
        private String ai_text_content;
        private String ai_audio_url;
    }
}