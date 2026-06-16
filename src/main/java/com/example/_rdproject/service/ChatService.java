package com.example._rdproject.service;

import com.example._rdproject.dto.ChatLogDto;
import com.example._rdproject.dto.ChatMessageDto;
import com.example._rdproject.dto.HistoryItemDto;
import com.example._rdproject.entity.*;
import com.example._rdproject.entity.Character;
import com.example._rdproject.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static reactor.netty.http.HttpConnectionLiveness.log;

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

    @Transactional
    public ChatMessageDto.Response processMessage(ChatMessageDto.Request request) {
        ChatSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

        log.info("AI 서버로 보내는 데이터: userId={}, characterId={}, text={}, stageId={}",
                session.getUserId(), session.getCharacterId(), request.getTextContent(), session.getStageId());

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

        List<ChatLog> historyLogs = chatLogRepository.findBySessionIdOrderByTurnCountAsc(request.getSessionId());

//        ChatMessageDto.Request aiRequest = ChatMessageDto.Request.builder()
//                .sessionId(request.getSessionId())
//                .textContent(request.getTextContent())
//                .inputType(request.getInputType())
//                .characterId(session.getCharacterId())
//                .scenarioId(session.getScenarioId())
//                .stageLevel(session.getStageId().intValue())
//                .userLevel(status.getUserLevel())
//                .turnCount(status.getTurnCount())
//                .currentAffinity(status.getCurrentAffinity())
//                .history(mapToHistory(historyLogs))
//                .build();
        ChatMessageDto.AiRequest aiRequest = ChatMessageDto.AiRequest.builder()
                .userId(session.getUserId().intValue())
                .characterId(session.getCharacterId())
                .text(request.getTextContent())
                .isVideoCall(true)
                .userAudioUrl(request.getUserAudioUrl())
                .stageId(session.getStageId().intValue())
                .build();

        // AI 서버 통신
        ChatMessageDto.Response aiResponse = aiServerWebClient.post()
                .uri("/api/v1/chat/message")
                .bodyValue(aiRequest)
                .retrieve()
                .bodyToMono(ChatMessageDto.Response.class)
                .block();

        if (aiResponse != null) {
            // 1. 대화 로그 저장
            saveAiResponseToLog(session, request, aiResponse, historyLogs);

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
        }

        return aiResponse;
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
                .userText(userRequest.getTextContent())
                .sessionId(session.getSessionId())
                .createdAt(LocalDateTime.now())
                .aiTextContent(aiResponse.getTextContent())
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

    // 1줄의 DB 데이터를 -> AI에게 보내기 위해 2줄(user, assistant)로 분리해주는 마법!
    private List<HistoryItemDto> mapToHistory(List<ChatLog> logs) {
        List<HistoryItemDto> historyList = new ArrayList<>();

        for (ChatLog log : logs) {
            // 1. 유저 발화 세팅
            if (log.getUserText() != null && !log.getUserText().isEmpty()) {
                historyList.add(HistoryItemDto.builder()
                        .role("user")
                        .text_content(log.getUserText())
                        .build());
            }
            // 2. AI 발화 세팅
            if (log.getAiTextContent() != null && !log.getAiTextContent().isEmpty()) {
                historyList.add(HistoryItemDto.builder()
                        .role("assistant")
                        .text_content(log.getAiTextContent())
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