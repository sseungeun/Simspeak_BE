package com.example._rdproject.service;

import com.example._rdproject.dto.ChatLogDto;
import com.example._rdproject.dto.ChatMessageDto;
import com.example._rdproject.dto.EvaluationDto;
import com.example._rdproject.dto.HistoryItemDto;
import com.example._rdproject.entity.*;
import com.example._rdproject.entity.Character;
import com.example._rdproject.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatService.class);

    private final ChatLogRepository chatLogRepository;
    private final ChatSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final UserCharacterStatusRepository userCharacterStatusRepository;
    private final PronunciationEvaluationRepository pronunciationEvaluationRepository;
    private final RawAiResponseRepository rawAiResponseRepository;
    private final WebClient aiServerWebClient;
    private final ObjectMapper objectMapper;
    private final LocalFileService fileService;

    @Value("${ai-server.api-key}")
    private String aiApiKey;

    @Transactional
    public ChatMessageDto.FrontendResponse processMessage(ChatMessageDto.Request request, MultipartFile audioFile) {
        ChatSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

        log.info("AI 서버로 보내는 데이터: userId={}, characterId={}, text={}, stageId={}",
                session.getUserId(), session.getCharacterId(), request.getText(), session.getStageId());

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        Character character = characterRepository.findById(session.getCharacterId())
                .orElseThrow(() -> new IllegalArgumentException("캐릭터를 찾을 수 없습니다."));

        UserCharacterStatus status = userCharacterStatusRepository
                .findByUserIdAndCharacterId(user.getId(), character.getId())
                .orElse(null);

        if (status == null) {
            UserCharacterStatus newStatus = UserCharacterStatus.builder()
                    .user(user)
                    .character(character)
                    .turnCount(0)
                    .userLevel("B2")
                    .currentAffinity(0)
                    .remainingPenalties(3)
                    .build();
            status = userCharacterStatusRepository.save(newStatus);
        }

        String finalUserAudioUrl = request.getUserAudioUrl();
        if (audioFile != null && !audioFile.isEmpty()) {
            log.info("[VOICE UPLOAD] 프론트엔드 녹음 파일 감지됨. 업로드 프로세스 시작.");
            finalUserAudioUrl = fileService.upload(audioFile); // 업로드 후 상대 경로 확보 (/uploads/xxx.mp3)
        }

        List<ChatLog> historyLogs = chatLogRepository.findBySessionIdOrderByTurnCountAsc(request.getSessionId());
        List<HistoryItemDto> historyList = mapToHistory(historyLogs);
        if (historyList == null) {
            historyList = new ArrayList<>();
        }

        ChatMessageDto.AiRequest aiRequest = ChatMessageDto.AiRequest.builder()
                .userId(session.getUserId().intValue())
                .characterId(session.getCharacterId())
                .text(request.getText())
                .isVideoCall(true)
                .userAudioUrl(finalUserAudioUrl) // ◀ 실제 업로드된 진짜 오디오 경로 매핑 완료
                .stageId(session.getStageId().intValue())
                .history(historyList)
                .build();

        try {
            log.info("--- [AI 전송 데이터] ---");
            log.info("전송 JSON: {}", objectMapper.writeValueAsString(aiRequest));
        } catch (Exception e) {
            log.error("JSON 변환 실패: {}", e.getMessage());
        }

        // AI 서버 통신
        ChatMessageDto.Response aiResponse = aiServerWebClient.post()
                .uri("/api/v1/chat/message")
                .bodyValue(aiRequest)
                .header("X-API-KEY", aiApiKey)
                .retrieve()
                .bodyToMono(ChatMessageDto.Response.class)
                .block();

        if (aiResponse != null) {
            log.info("AI 서버 원본 응답: {}", aiResponse);
        } else {
            log.warn("AI 서버로부터 응답이 null입니다.");
        }

        // 프론트엔드로 보낼 최종 응답 객체 선언
        ChatMessageDto.FrontendResponse frontendResponse = null;

        if (aiResponse != null) {
            // ───────────────── [교정 완료] updatedRequest 변수 선언 및 객체 조립 ─────────────────
            ChatMessageDto.Request updatedRequest = ChatMessageDto.Request.builder()
                    .sessionId(request.getSessionId())
                    .text(request.getText())
                    .inputType(request.getInputType())
                    .characterId(request.getCharacterId())
                    .scenarioId(request.getScenarioId())
                    .targetLanguage(request.getTargetLanguage())
                    .stageLevel(request.getStageLevel())
                    .userLevel(request.getUserLevel())
                    .turnCount(request.getTurnCount())
                    .currentAffinity(request.getCurrentAffinity())
                    .userAudioUrl(finalUserAudioUrl) // ◀ 진짜 새로 저장된 업로드 경로 바인딩
                    .history(request.getHistory())
                    .build();

            // 1. 대화 로그 저장 (이제 변수가 존재하므로 컴파일 에러가 나지 않습니다)
            saveAiResponseToLog(session, updatedRequest, aiResponse, historyLogs);

            // 2. 턴 수 1 증가
            status.setTurnCount(status.getTurnCount() + 1);

            // 3. AI 1차 응답에 포함된 호감도 즉시 갱신
            if (aiResponse.getCurrentTotalAffinity() != null) {
                status.setCurrentAffinity(aiResponse.getCurrentTotalAffinity());
            } else if (aiResponse.getAffinityDelta() != null) {
                status.setCurrentAffinity(status.getCurrentAffinity() + aiResponse.getAffinityDelta());
            }
            // 4. 상태 저장
            userCharacterStatusRepository.save(status);

            EvaluationDto.Response evalResponse = null;
            var aiEval = aiResponse.getSystemEvaluation(); // AI가 준 원본 데이터

            if (aiEval != null) {
                // [A] 문법 포장
                EvaluationDto.GrammarInfo grammar = EvaluationDto.GrammarInfo.builder()
                        .grammarFeedback(aiEval.getGrammarFeedback())
                        .build();

                // [B] 표현 포장
                List<EvaluationDto.CorrectionItem> correctionItems = new ArrayList<>();
                if (aiEval.getCorrectionsJson() != null) {
                    correctionItems = aiEval.getCorrectionsJson().stream()
                            .map(c -> EvaluationDto.CorrectionItem.builder()
                                    .originalSentence(c.getOriginalSentence())
                                    .correctedSentence(c.getCorrectedSentence())
                                    .correctedAudioUrl(c.getCorrectedAudioUrl())
                                    .build())
                            .collect(Collectors.toList());
                }
                EvaluationDto.ExpressionInfo expression = EvaluationDto.ExpressionInfo.builder()
                        .correctionsJson(correctionItems)
                        .detectedInvalidWords(aiEval.getDetectedInvalidWords())
                        .build();

                // [C] 발음 포장
                EvaluationDto.PronunciationInfo pronunciation = null;
                if (aiEval.getPronunciationEvaluations() != null) {
                    pronunciation = EvaluationDto.PronunciationInfo.builder()
                            .accuracy(aiEval.getPronunciationEvaluations().getAccuracy())
                            .fluency(aiEval.getPronunciationEvaluations().getFluency())
                            .wordDetailsJson(aiEval.getPronunciationEvaluations().getWordDetails())
                            .build();
                }

                // 3개 그룹 합체!
                evalResponse = EvaluationDto.Response.builder()
                        .grammar(grammar)
                        .expression(expression)
                        .pronunciation(pronunciation)
                        .build();
            }

            // 최종적으로 프론트엔드에 보낼 객체 완성
            frontendResponse = ChatMessageDto.FrontendResponse.builder()
                    .text(aiResponse.getText())
                    .actionDescription(aiResponse.getActionDescription())
                    .affinityDelta(aiResponse.getAffinityDelta())
                    .currentTotalAffinity(aiResponse.getCurrentTotalAffinity())
                    .audioUrl(aiResponse.getAudioUrl())
                    .systemEvaluation(evalResponse) // 그룹화된 데이터 탑재!
                    .build();
        }

        return frontendResponse;
    }

    private void saveAiResponseToLog(ChatSession session, ChatMessageDto.Request userRequest, ChatMessageDto.Response aiResponse, List<ChatLog> historyLogs) {
        // 1. 객체 조회
        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        Character character = characterRepository.findById(session.getCharacterId())
                .orElseThrow(() -> new IllegalArgumentException("캐릭터를 찾을 수 없습니다."));

        List<HistoryItemDto> historyList = mapToHistory(historyLogs);
        Map<String, Object> historyMap = new java.util.HashMap<>();
        historyMap.put("history", historyList);

        Map<String, Object> rawLogMap = new java.util.HashMap<>();
        rawLogMap.put("model", aiResponse.getModelInfo());

        // 2. ChatLog 저장 (엔티티에 정의된 필드에 맞게 수정)
        ChatLog aiLog = ChatLog.builder()
                .user(user)
                .character(character)
                .userText(userRequest.getText())
                .userAudioUrl(userRequest.getUserAudioUrl())
                .sessionId(session.getSessionId())
                .createdAt(LocalDateTime.now())
                .aiTextContent(aiResponse.getText())
                .aiAudioUrl(aiResponse.getAudioUrl())
                .currentAffinity(aiResponse.getCurrentTotalAffinity())
                .grammarFeedback(aiResponse.getSystemEvaluation() != null ?
                        aiResponse.getSystemEvaluation().getGrammarFeedback() : null)
                .chatHistoryContext(historyMap)
                .rawLlmLog(rawLogMap)
                .build();

        ChatLog savedLog = chatLogRepository.save(aiLog);

        // 2. RawAiResponse 저장 (Map 변환)
        Map<String, Object> responseMap = objectMapper.convertValue(aiResponse, Map.class);
        rawAiResponseRepository.save(RawAiResponse.builder()
                .chatLog(savedLog)
                .responseData(responseMap)
                .createdAt(Instant.now())
                .build());

        // 3. 발음 점수 저장
        if (aiResponse.getSystemEvaluation() != null && aiResponse.getSystemEvaluation().getPronunciationEvaluations() != null) {
            pronunciationEvaluationRepository.save(PronunciationEvaluation.builder()
                    .chatLog(savedLog)
                    .accuracy(aiResponse.getSystemEvaluation().getPronunciationEvaluations().getAccuracy())
                    .fluency(aiResponse.getSystemEvaluation().getPronunciationEvaluations().getFluency())
                    .completeness(aiResponse.getSystemEvaluation().getPronunciationEvaluations().getCompleteness())
                    .prosody(aiResponse.getSystemEvaluation().getPronunciationEvaluations().getProsody())
                    .build());
        }
    }

    private List<HistoryItemDto> mapToHistory(List<ChatLog> logs) {
        List<HistoryItemDto> historyList = new ArrayList<>();

        for (ChatLog log : logs) {
            // 1. 유저 발화 세팅
            if (log.getUserText() != null && !log.getUserText().isEmpty()) {
                historyList.add(HistoryItemDto.builder()
                        .role("user")
                        .text(log.getUserText())
                        .build());
            }
            // 2. AI 발화 세팅
            if (log.getAiTextContent() != null && !log.getAiTextContent().isEmpty()) {
                historyList.add(HistoryItemDto.builder()
                        .role("assistant")
                        .text(log.getAiTextContent())
                        .build());
            }
        }
        return historyList;
    }

    @Transactional(readOnly = true)
    public ChatLogDto.HistoryResponse getChatLogsBySessionId(String sessionId, Long userId) {
        // 1. 세션 조회 (id 오름차순 정렬)
        List<ChatLog> logs = chatLogRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        // 세션이 존재하는 경우에만 소유자 확인
        if (!logs.isEmpty()) {
            if (logs.get(0).getUser() != null && !logs.get(0).getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("본인의 세션만 조회할 수 있습니다.");
            }
        }

        // 3. 매핑 및 응답 (새로운 DTO 구조에 맞춤)
        List<ChatLogDto.LogItem> logItems = logs.stream()
                .map(log -> ChatLogDto.LogItem.builder()
                        .log_id(log.getId())
                        .user_text(log.getUserText())
                        .user_audio_url(log.getUserAudioUrl())
                        .ai_text_content(log.getAiTextContent())
                        .ai_audio_url(log.getAiAudioUrl())
                        .build())
                .collect(Collectors.toList());

        return ChatLogDto.HistoryResponse.builder()
                .session_id(sessionId)
                .logs(logItems)
                .build();
    }
}