package com.example._rdproject.service;

import com.example._rdproject.dto.LevelTestDto;
import com.example._rdproject.entity.*;
import com.example._rdproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LevelTestService {

    private final LevelTestRepository levelTestRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final AnswerHistoryRepository answerHistoryRepository; // 1. 명칭 수정

    @Transactional
    public void saveLevelTestResult(LevelTestDto.SaveRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        EnglishLevelTest levelTest = EnglishLevelTest.builder()
                .user(user)
                .testType(request.getLevelTestType())
                .assignedLevel(request.getCefrLevelType())
                .testScore(request.getTestScore())
                .build();

        levelTestRepository.save(levelTest);

        // 2. 메서드 인자 및 호출 수정 (오타 수정: cefrLevel 사용)
        user.updateCurrentLevel(request.getCefrLevelType());
    }

    public LevelTestDto.StatusResponse getUserLevelStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        return new LevelTestDto.StatusResponse(user.getId(), user.getCurrentLevel());
    }

    public LevelTestDto.QuestionListResponse getAllQuestions() {
        List<Question> questions = questionRepository.findAll();

        List<LevelTestDto.QuestionDto> dtos = questions.stream()
                .map(q -> LevelTestDto.QuestionDto.builder()
                        .questionId(q.getId())
                        .questionText(q.getQuestionText())
                        .difficultyLevel(q.getDifficultyLevel())
                        .category(q.getCategory())
                        .build())
                .collect(Collectors.toList());

        return LevelTestDto.QuestionListResponse.builder()
                .questions(dtos)
                .build();
    }

    @Transactional
    public void submitAnswer(Long userId, Long questionId, String answerText) {
        User user = userRepository.findById(userId).orElseThrow();
        Question question = questionRepository.findById(questionId).orElseThrow();

        AnswerHistory history = AnswerHistory.builder()
                .user(user)
                .question(question)
                .answerText(answerText)
                .build();

        // 4. 레포지토리 인스턴스 사용 (정적 메서드 호출이 아님)
        answerHistoryRepository.save(history);
    }
}