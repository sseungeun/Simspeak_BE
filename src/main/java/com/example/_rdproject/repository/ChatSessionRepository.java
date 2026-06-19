package com.example._rdproject.repository;

import com.example._rdproject.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
    // 유저 ID와 스테이지 ID를 기반으로 생성일 기준 내림차순(가장 최근) 1건 조회
    Optional<ChatSession> findFirstByUserIdAndStageIdOrderByCreatedAtDesc(Long userId, Long stageId);

    Long countByUserId(Long userId);
    // 특정 캐릭터와 특정 유저가 개설했던 모든 대화 세션방 목록 조회
    List<ChatSession> findByCharacterIdAndUserId(String characterId, Long userId);
}
