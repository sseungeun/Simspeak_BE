package com.example._rdproject.repository;

import com.example._rdproject.entity.ChatLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {
    List<ChatLog> findBySessionIdOrderByTurnCountAsc(String sessionId);
}
