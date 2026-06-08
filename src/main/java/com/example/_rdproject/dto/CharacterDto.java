package com.example._rdproject.dto;

import com.example._rdproject.domain.CefrLevelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

public class CharacterDto {

    // 1. 메인 화면 응답 전체 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainStatusResponse {
        private Long userId;
        private String nickname;
        private CefrLevelType currentLevel;
        private Integer continuousDays;
        private List<CharacterStatusResponse> characters;
    }

    // 1-1. 메인 화면 속 개별 캐릭터 정보 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CharacterStatusResponse {
        private String characterId;
        private String name;
        private String description;
        private String imageUrl;
        private Integer affinityScore;
        private boolean isUnlocked;
        private String mbti;
        private int statAffinity;
        private int statTsundere;
        private int statWit;
    }

    // 2. 특정 캐릭터의 스테이지 목록 + 유저 진척도 응답 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder // Service 단에서 편하게 조립할 수 있도록 Builder 추가
    public static class StageListResponse {
        private String characterId;
        private String characterName;
        private List<StageResponse> stages;
    }

    // 2-1. 개별 스테이지 세부 정보 + 유저 진척도 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StageResponse {
        private Long stageId;
        private Integer stageNumber;
        private String stageType;
        private String title;
        private String scenarioId;
        private Integer unlockAffinityRatio;

        // 유저 진행 상태 정보 합체 필드
        private boolean isUnlocked;
        private boolean isCompleted;
        private Integer bestScore;
    }

    // 2-2. 보유 캐릭터 목록 조회 Response DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyCharacterResponse {
        private String id;
        private String name;
        private String description;
        private String imageUrl;
        private int affinityScore;
        private String mbti;
        private int statAffinity;
        private int statTsundere;
        private int statWit;
    }
}