package com.example._rdproject.service;

import com.example._rdproject.dto.LevelTestDto;
import com.example._rdproject.entity.*;
import com.example._rdproject.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    public LevelTestDto.AiLevelTestResponse submitAndEvaluateAnswer(LevelTestDto.SubmitAnswerRequest request) {
        // 1. 기존 답변 저장 로직 유지 (DB 저장)
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

        // 2. AI 서버에 보낼 요청(Request) 조립
        LevelTestDto.AiLevelTestRequest aiRequest = LevelTestDto.AiLevelTestRequest.builder()
                .userId(request.getUserId().intValue())
                .characterId(request.getCharacterId() != null ? request.getCharacterId() : "ian") // 기본값 세팅 가능
                .currentQuestionIndex(request.getCurrentQuestionIndex() != null ? request.getCurrentQuestionIndex() : 0)
                .userText(request.getAnswerText() != null ? request.getAnswerText() : "")
                .userAudioUrl(request.getUserAudioUrl() != null ? request.getUserAudioUrl() : "")
                .accumulatedAnswers(request.getAccumulatedAnswers() != null ? request.getAccumulatedAnswers() : new ArrayList<>())
                .isQuit(request.getIsQuit() != null ? request.getIsQuit() : false)
                .build();

        // 3. AI 서버로 통신 (Swagger에 정의된 엔드포인트)
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
                    .chatLog(null) // 일반 채팅이 아니므로 null
                    .answerHistory(savedHistory) // 방금 DB에 저장한 레벨테스트 답변 객체
                    .accuracy(evalDto.getAccuracy())
                    .fluency(evalDto.getFluency())
                    .completeness(evalDto.getCompleteness())
                    .prosody(evalDto.getProsody())
                    .wordDetailsJson(evalDto.getWordDetailsJson()) // 별도의 String 변환 없이 바로 삽입!
                    .build();

            pronunciationEvaluationRepository.save(evaluation);
        }

        return aiResponse;
    }
}