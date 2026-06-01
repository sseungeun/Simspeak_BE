package com.example._rdproject.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

public class StageProgressDto {

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        private Long userId;
        private Long currentStageId;
        private int score;
        private boolean passed; // 합격 기준 점수를 넘었는지 여부
    }

    @Getter
    public static class UpdateResponse {
        private boolean isCurrentStageCompleted;
        private boolean isNextStageUnlocked;
        private Long nextStageId; // 다음 스테이지가 있다면 ID 반환 (없으면 null)

        public UpdateResponse(boolean isCurrentStageCompleted, boolean isNextStageUnlocked, Long nextStageId) {
            this.isCurrentStageCompleted = isCurrentStageCompleted;
            this.isNextStageUnlocked = isNextStageUnlocked;
            this.nextStageId = nextStageId;
        }
    }
}