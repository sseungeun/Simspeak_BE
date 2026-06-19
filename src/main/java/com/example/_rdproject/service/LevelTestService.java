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

import static reactor.netty.http.HttpConnectionLiveness.log;

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

        LevelTestDto.AiLevelTestResponse aiResponse;
        try {
            aiResponse = aiServerWebClient.post()
                    .uri("/api/v1/chat/level_test")
                    .bodyValue(aiRequest)
                    .retrieve()
                    .bodyToMono(LevelTestDto.AiLevelTestResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("[LEVEL TEST ERROR] AI 채점 서버 연동 실패 원인: {}", e.getMessage());
            // 8번째 문항에서 가짜 URL 분석 실패 등으로 터진 경우 프론트 요구 조건대로 핸들링 우회
            if (aiQuestionIndex == 8) {
                log.info("[LEVEL TEST BYPASS] 마지막 문항 예외 감지로 인한 자체 종료 패키징 포장 시작.");
                aiResponse = new LevelTestDto.AiLevelTestResponse();
                aiResponse.setUserRecognizedText("가짜 오디오 파일 분석 우회 처리 완료");
                aiResponse.setIsFinished(true); // 종료 처리 허용
                aiResponse.setFinalResult(null); // 파싱 에러 방지용 null 세팅
                aiResponse.setNextQuestionText(null);
                aiResponse.setNextQuestionAudioUrl(null);
            } else {
                throw new RuntimeException("AI 서버 통신 예외 발생", e);
            }
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