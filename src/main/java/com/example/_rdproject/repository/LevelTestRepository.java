package com.example._rdproject.repository;

import com.example._rdproject.entity.EnglishLevelTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LevelTestRepository extends JpaRepository<EnglishLevelTest, Long> {
    // 특정 유저의 레벨 테스트 이력을 조회할 때 사용
    List<EnglishLevelTest> findByUserId(Long userId);
}