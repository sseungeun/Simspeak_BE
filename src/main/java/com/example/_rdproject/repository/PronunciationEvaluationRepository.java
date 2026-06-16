package com.example._rdproject.repository;

import com.example._rdproject.entity.PronunciationEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PronunciationEvaluationRepository extends JpaRepository<PronunciationEvaluation, Long> {
    @Query("SELECT AVG((p.accuracy + p.fluency + p.completeness + p.prosody) / 4.0) " +
            "FROM PronunciationEvaluation p JOIN p.chatLog c " +
            "WHERE c.user.id = :userId")
    Double findAverageScoreByUserId(@Param("userId") Long userId);
}