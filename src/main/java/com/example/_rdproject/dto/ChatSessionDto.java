package com.example._rdproject.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ChatSessionDto {
    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        private Long userId;
        private Long stageId;
        private String characterId;
        private String scenarioId;
    }

    @Getter @Builder
    @AllArgsConstructor
    public static class CreateResponse {
        private String sessionId;
        private FirstMessage firstMessage;
    }

    @Getter @Builder @AllArgsConstructor
    public static class FirstMessage {
        private String textContent;
        private String actionDescription;
        private String audioUrl; // null 가능
    }
    @Getter @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ActiveSessionResponse {

        @JsonProperty("session_id")
        private String sessionId;

        @JsonProperty("is_completed")
        private Boolean isCompleted;
    }
}

