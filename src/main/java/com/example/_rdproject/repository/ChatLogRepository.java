package com.example._rdproject.repository;

import com.example._rdproject.entity.ChatLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {
    // 턴 수 기준으로 조회 -ai 조회시
    List<ChatLog> findBySessionIdOrderByTurnCountAsc(String sessionId);

    //생성 시간 기준으로 조회 (대화 로그 전체 불러오기용)
    List<ChatLog> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    //ㅅㅔ션 목록 조회
    List<ChatLog> findByCharacterIdAndUserId(String characterId, Long userId);
}
