package com.example._rdproject.repository;

import com.example._rdproject.entity.UserStageProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserStageProgressRepository extends JpaRepository<UserStageProgress, Long> {

    /**
     * 특정 유저가 특정 스테이지를 플레이한 기록(진행도)을 단건 조회합니다.
     * StageService에서 현재 단계를 완료 처리(isCompleted = true)할 때 사용됩니다.
     */
    Optional<UserStageProgress> findByUserIdAndStageId(Long userId, Long stageId);
}