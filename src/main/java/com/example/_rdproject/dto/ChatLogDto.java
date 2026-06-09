package com.example._rdproject.dto;

import com.example._rdproject.domain.ChatInputType;
import com.example._rdproject.domain.ChatRoleType;
import lombok.Builder;
import lombok.Getter;
import java.time.Instant;
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
        private String message_id;
        private ChatRoleType role;
        private ChatInputType input_type;
        private String text_content;
        private String audio_url;          // assistant 응답 시 사용
        private Instant created_at;
    }
}
