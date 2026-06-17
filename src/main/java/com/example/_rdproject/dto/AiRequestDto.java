package com.example._rdproject.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AiRequestDto {
    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("character_id")
    private String characterId;

    private String text;

    @JsonProperty("is_video_call")
    private boolean isVideoCall;

    @JsonProperty("user_audio_url")
    private String userAudioUrl;

    @JsonProperty("stage_id")
    private String stageId;

    @JsonProperty("action_description")
    private String actionDescription;

    @JsonProperty("history")
    private List<HistoryItemDto> history;
}