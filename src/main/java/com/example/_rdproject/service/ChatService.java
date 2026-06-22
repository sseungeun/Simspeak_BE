package com.example._rdproject.service;

import com.example._rdproject.dto.ChatLogDto;
import com.example._rdproject.dto.ChatMessageDto;
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
    private final RawAiResponseRepository rawAiResponseRepository;
    private final CorrectionRepository correctionRepository;
    private final WebClient aiServerWebClient;
    private final ObjectMapper objectMapper;
    private final S3Service fileService;

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
            finalUserAudioUrl = fileService.upload(audioFile);
        }

        List<ChatLog> historyLogs = chatLogRepository.findBySessionIdOrderByTurnCountAsc(request.getSessionId());
        List<HistoryItemDto> historyList = mapToHistory(historyLogs);
        if (historyList == null) {
            historyList = new ArrayList<>();
        }

        // 1. 대화 로그 엔티티 '먼저 생성'하여 데이터베이스 고유 ID(PK) 선발급
        ChatLog initialUserLog = ChatLog.builder()
                .user(user)
                .character(character)
                .turnCount(status.getTurnCount())
                .sessionId(session.getSessionId())
                .userText(request.getText())
                .userAudioUrl(finalUserAudioUrl)
                .stageId(session.getStageId() != null ? session.getStageId().toString() : null)
                .build();

        ChatLog savedUserLog = chatLogRepository.save(initialUserLog);

        // 2. 파이썬으로 보낼 요청 바디 생성
        ChatMessageDto.AiRequest aiRequest = ChatMessageDto.AiRequest.builder()
                .userId(session.getUserId().intValue())
                .characterId(session.getCharacterId())
                .text(request.getText())
                .isVideoCall(true)
                .userAudioUrl(finalUserAudioUrl)
                .stageId(session.getStageId() != null ? session.getStageId().intValue() : 0)
                .history(historyList)
                .chatLogId(savedUserLog.getId())
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
            // 파이썬이 음성을 텍스트로 변환해준 진짜 STT 결과 데이터(userRecognizedText)가 존재하면 가져옴
            String finalUserText = (aiResponse.getUserRecognizedText() != null && !aiResponse.getUserRecognizedText().isEmpty())
                    ? aiResponse.getUserRecognizedText()
                    : request.getText();

            // 3. 빌더 조립 대신 엔티티 내에 직접 세팅(Setter 기법)하여 영속성 컨텍스트 안전 업데이트 보장
            savedUserLog.setUserText(finalUserText);
            savedUserLog.setAiTextContent(aiResponse.getText());
            savedUserLog.setAiAudioUrl(aiResponse.getAudioUrl());
            savedUserLog.setCurrentAffinity(aiResponse.getCurrentTotalAffinity());

            // 히스토리 맥락 및 로그용 Map 데이터 세팅
            Map<String, Object> historyMap = new java.util.HashMap<>();
            historyMap.put("history", historyList);
            savedUserLog.setChatHistoryContext(historyMap);

            Map<String, Object> rawLogMap = new java.util.HashMap<>();
            rawLogMap.put("model", aiResponse.getModelInfo());
            savedUserLog.setRawLlmLog(rawLogMap);

            // 최종 영속 데이터 동기화 반영
            chatLogRepository.save(savedUserLog);

            // 4. RawAiResponse 원본 로그 백업 연동
            Map<String, Object> responseMap = objectMapper.convertValue(aiResponse, Map.class);
            rawAiResponseRepository.save(RawAiResponse.builder()
                    .chatLog(savedUserLog)
                    .responseData(responseMap)
                    .createdAt(Instant.now())
                    .build());

            // 5. 턴 수 1 증가
            status.setTurnCount(status.getTurnCount() + 1);

            // 6. AI 응답에 포함된 호감도 즉시 반영
            if (aiResponse.getCurrentTotalAffinity() != null) {
                status.setCurrentAffinity(aiResponse.getCurrentTotalAffinity());
            } else if (aiResponse.getAffinityDelta() != null) {
                status.setCurrentAffinity(status.getCurrentAffinity() + aiResponse.getAffinityDelta());
            }
            userCharacterStatusRepository.save(status);

            // 7. 실시간 응답 타이밍에는 system_evaluation을 기다리지 않고 항상 null로 응답 처리
            frontendResponse = ChatMessageDto.FrontendResponse.builder()
                    .text(aiResponse.getText())
                    .actionDescription(aiResponse.getActionDescription())
                    .affinityDelta(aiResponse.getAffinityDelta())
                    .currentTotalAffinity(aiResponse.getCurrentTotalAffinity())
                    .audioUrl(aiResponse.getAudioUrl())
                    .systemEvaluation(null)
                    .userRecognizedText(aiResponse.getUserRecognizedText())
                    .build();
        }

        return frontendResponse;
    }

    /**
     * ◀ [B안 핵심 신설 비즈니스 로직]
     * 파이썬 AI 서버가 백그라운드 문법 계산 및 평가 완료 후 자바 백엔드의 /api/chat/callback을 때려줄 때 작동하는 저장 전용 트랜잭션 메서드
     */
    @Transactional
    public void saveAsynchronousEvaluation(ChatMessageDto.AiCallbackRequest callbackRequest) {
        if (callbackRequest == null || callbackRequest.getChatLogId() == null) {
            log.error("[CALLBACK ERROR] 콜백 데이터 또는 타깃 대화로그 식별 정보가 유실되었습니다.");
            return;
        }

        // 1. 파이썬이 돌려보낸 chatLogId를 사용해 기존 대화 로그 엔티티 역추적 조회
        ChatLog targetLog = chatLogRepository.findById(callbackRequest.getChatLogId())
                .orElseThrow(() -> new IllegalArgumentException("콜백 대상에 해당하는 대화 로그를 찾을 수 없습니다. ID: " + callbackRequest.getChatLogId()));

        ChatMessageDto.AiServerEvaluation aiEval = callbackRequest.getSystemEvaluation();
        if (aiEval == null) {
            log.warn("[CALLBACK WARN] 파이썬 평가 연산이 누락되었거나 비어있습니다. ID: {}", callbackRequest.getChatLogId());
            return;
        }

        // 2. ChatLog 엔티티에 비동기로 도출된 문법 총평 피드백 사후 업데이트 저장
        if (aiEval.getGrammarFeedback() != null) {
            targetLog.setGrammarFeedback(aiEval.getGrammarFeedback());
            chatLogRepository.save(targetLog);
        }

        // 3. 비어있던 corrections 테이블에 문장별 교정 원본 JSON 데이터 루프 돌며 정상 INSERT 진행
        if (aiEval.getCorrectionsJson() != null) {
            log.info("[CALLBACK PROCESSING] 비동기 문법/표현 오답 데이터 수집 감지됨. corrections 테이블 적재 수량: {}", aiEval.getCorrectionsJson().size());

            for (var item : aiEval.getCorrectionsJson()) {
                Correction correction = Correction.builder()
                        .chatLog(targetLog)
                        .correctionType(com.example._rdproject.domain.CorrectionType.GRAMMAR)
                        .originalSentence(item.getOriginalSentence())
                        .correctedSentence(item.getCorrectedSentence())
                        .correctedAudioUrl(item.getCorrectedAudioUrl())
                        .isReviewed(false)
                        .build();

                correctionRepository.save(correction);
            }
            log.info("[CALLBACK SUCCESS] corrections 테이블 영속화 저장이 정상 완료되었습니다.");
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