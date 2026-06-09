package com.example._rdproject.repository;

import com.example._rdproject.entity.Correction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CorrectionRepository extends JpaRepository<Correction, Long> {
    // 세션별 오답 노트 조회
    List<Correction> findByChatLog_SessionId(String sessionId);

    // 유저의 전체 노트 조회
    List<Correction> findByChatLog_User_Id(Long userId);

    // 유저의 미복습/복습 오답 노트 조회
    List<Correction> findByChatLog_User_IdAndIsReviewed(Long userId, Boolean isReviewed);
}