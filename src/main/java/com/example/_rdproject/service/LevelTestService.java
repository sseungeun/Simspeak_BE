package com.example._rdproject.service;

import com.example._rdproject.entity.LevelTest;
import com.example._rdproject.entity.User;
import com.example._rdproject.dto.LevelTestDto;
import com.example._rdproject.repository.LevelTestRepository;
import com.example._rdproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LevelTestService {

    private final LevelTestRepository levelTestRepository;
    private final UserRepository userRepository;

    /**
     * 레벨 테스트 결과 등록 및 유저 등급 업데이트
     */
    @Transactional
    public void saveLevelTestResult(LevelTestDto.SaveRequest request) {
        // 1. 유저 조회 (없으면 예외 처리)
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. ID: " + request.getUserId()));

        // 2. 레벨 테스트 이력(english_level_tests) 생성 및 저장
        LevelTest levelTest = LevelTest.builder()
                .user(user)
                .testType(request.getTestType())
                .assignedLevel(request.getAssignedLevel())
                .testScore(request.getTestScore())
                .build();

        levelTestRepository.save(levelTest);

        String levelString = request.getAssignedLevel().name(); // 예: "A1"
        User.CefrLevel cefrLevel = User.CefrLevel.valueOf(levelString);

        // 3. 유저의 현재 레벨
        user.updateCurrentLevel(cefrLevel);
    }
    /**
     * 유저의 현재 레벨 상태 조회
     */
    public LevelTestDto.StatusResponse getUserLevelStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. ID: " + userId));

        return new LevelTestDto.StatusResponse(user.getId(), user.getCurrentLevel());
    }
}