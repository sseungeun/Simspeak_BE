package com.example._rdproject.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserDto {

    // 1. 최근 사용 캐릭터 업데이트 Request DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateLastCharacterRequest {
        private String characterId;
    }
    // 2. 마이페이지 프로필 수정 Request DTO (프론트 -> 백엔드)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProfileRequest {
        private String nickname;
        private String preferred_partner_gender;
    }

    // 2-1. 마이페이지 프로필 수정 Response DTO (백엔드 -> 프론트)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder // 쉽게 객체를 만들기 위한 마법의 어노테이션
    public static class UpdateProfileResponse {
        private Long user_id;
        private String nickname;
        private String preferred_partner_gender;
        private String current_level;
    }

    // 3. 마이페이지 프로필 조회 Response DTO (백엔드 -> 프론트)
    @Getter
    @lombok.Builder
    public static class GetProfileResponse {
        private Long user_id;
        private String nickname;
        private String preferred_partner_gender;
        private String current_level;
    }
}