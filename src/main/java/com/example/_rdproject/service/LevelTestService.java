package com.example._rdproject.service;

import com.example._rdproject.domain.CefrLevelType;
import com.example._rdproject.domain.LevelTestType;
import com.example._rdproject.dto.LevelTestDto;
import com.example._rdproject.entity.*;
import com.example._rdproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LevelTestService {

    private final LevelTestRepository levelTestRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final AnswerHistoryRepository answerHistoryRepository;
    private final WebClient aiServerWebClient;
    private final PronunciationEvaluationRepository pronunciationEvaluationRepository;

    @Transactional
    public void saveLevelTestResult(LevelTestDto.SaveRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        EnglishLevelTest levelTest = EnglishLevelTest.builder()
                .user(user)
                .testType(request.getLevelTestType())
                .assignedLevel(request.getCefrLevelType())
                .build();

        levelTestRepository.save(levelTest);
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
    public LevelTestDto.AiLevelTestResponse submitAndEvaluateAnswer(LevelTestDto.SubmitAnswerRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문항입니다."));

        AnswerHistory history = AnswerHistory.builder()
                .user(user)
                .question(question)
                .answerText(request.getAnswerText())
                .build();

        AnswerHistory savedHistory = answerHistoryRepository.save(history);

        // 인덱스 보정 (0~7 -> 1~8)
        int aiQuestionIndex = (request.getCurrentQuestionIndex() != null ? request.getCurrentQuestionIndex() : 0) + 1;

        LevelTestDto.AiLevelTestRequest aiRequest = LevelTestDto.AiLevelTestRequest.builder()
                .userId(request.getUserId().intValue())
                .characterId(request.getCharacterId() != null ? request.getCharacterId() : "ian")
                .currentQuestionIndex(aiQuestionIndex)
                .userText(request.getAnswerText() != null ? request.getAnswerText() : "")
                .userAudioUrl(request.getUserAudioUrl() != null ? request.getUserAudioUrl() : "")
                .accumulatedAnswers(request.getAccumulatedAnswers() != null ? request.getAccumulatedAnswers() : new ArrayList<>())
                .isQuit(request.getIsQuit() != null ? request.getIsQuit() : false)
                .build();

        LevelTestDto.AiLevelTestResponse aiResponse = aiServerWebClient.post()
                .uri("/api/v1/chat/level_test")
                .bodyValue(aiRequest)
                .retrieve()
                .bodyToMono(LevelTestDto.AiLevelTestResponse.class)
                .block();

        if (aiResponse == null) {
            throw new RuntimeException("AI 서버로부터 응답을 받지 못했습니다.");
        }

        if (aiResponse.getPronunciationEvaluations() != null) {
            LevelTestDto.PronunciationEvaluations evalDto = aiResponse.getPronunciationEvaluations();

            PronunciationEvaluation evaluation = PronunciationEvaluation.builder()
                    .chatLog(null)
                    .answerHistory(savedHistory)
                    .accuracy(evalDto.getAccuracy())
                    .fluency(evalDto.getFluency())
                    .completeness(evalDto.getCompleteness())
                    .prosody(evalDto.getProsody())
                    .wordDetailsJson(evalDto.getWordDetailsJson())
                    .build();

            pronunciationEvaluationRepository.save(evaluation);
        }

        if (Boolean.TRUE.equals(aiResponse.getIsFinished()) && aiResponse.getFinalResult() != null) {
            LevelTestDto.FinalResult finalResult = aiResponse.getFinalResult();
            EnglishLevelTest levelTestResult = EnglishLevelTest.builder()
                    .user(user)
                    .testType(LevelTestType.test)
                    .assignedLevel(CefrLevelType.valueOf(finalResult.getAssignedLevel().toUpperCase()))
                    .testScore(finalResult.getTestScore())
                    .build();
            levelTestRepository.save(levelTestResult);

            user.updateCurrentLevel(CefrLevelType.valueOf(finalResult.getAssignedLevel().toUpperCase()));
        }

        return aiResponse;
    }
}