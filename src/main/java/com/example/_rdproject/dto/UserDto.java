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
        private Long characterId;
    }
}