package com.example._rdproject.repository;

import com.example._rdproject.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
    // 유저 ID와 스테이지 ID를 기반으로 생성일 기준 내림차순(가장 최근) 1건 조회
    Optional<ChatSession> findFirstByUserIdAndStageIdOrderByCreatedAtDesc(Long userId, Long stageId);

    Long countByUserId(Long userId);
}
