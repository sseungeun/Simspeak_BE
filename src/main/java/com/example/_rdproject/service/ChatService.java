package com.example._rdproject.service;

import com.example._rdproject.dto.ChatLogDto;
import com.example._rdproject.dto.ChatMessageDto;
import com.example._rdproject.domain.ChatRoleType;
import com.example._rdproject.dto.HistoryItemDto;
import com.example._rdproject.entity.*;
import com.example._rdproject.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {
    private final ChatLogRepository chatLogRepository;
    private final ChatSessionRepository sessionRepository;
    private final UserCharacterStatusRepository userCharacterStatusRepository;
    private final PronunciationEvaluationRepository pronunciationEvaluationRepository;
    private final RawAiResponseRepository rawAiResponseRepository;
    private final WebClient aiServerWebClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public ChatMessageDto.Response processMessage(ChatMessageDto.Request request) {
        ChatSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

        UserCharacterStatus status = userCharacterStatusRepository.findByUserIdAndCharacterId(
                session.getUserId(), session.getCharacterId());

        List<ChatLog> historyLogs = chatLogRepository.findBySessionIdOrderByTurnCountAsc(request.getSessionId());

        ChatMessageDto.Request aiRequest = ChatMessageDto.Request.builder()
                .sessionId(request.getSessionId())
                .textContent(request.getTextContent())
                .inputType(request.getInputType())
                .characterId(session.getCharacterId())
                .scenarioId(session.getScenarioId())
                .stageLevel(session.getStageId().intValue())
                .userLevel(status.getUserLevel())
                .turnCount(status.getTurnCount())
                .currentAffinity(status.getCurrentAffinity())
                .history(mapToHistory(historyLogs))
                .build();

        // AI 서버 통신 (1초 내 단건 응답)
        ChatMessageDto.Response aiResponse = aiServerWebClient.post()
                .uri("/api/chat")
                .bodyValue(aiRequest)
                .retrieve()
                .bodyToMono(ChatMessageDto.Response.class)
                .block();

        if (aiResponse != null) {
            // 1. 대화 로그 및 행동/호감도 변동량 저장
            saveAiResponseToLog(session, aiResponse);

            // 2. 턴 수 1 증가
            status.setTurnCount(status.getTurnCount() + 1);

            // 3. [v3.0 반영] AI 1차 응답에 포함된 호감도 즉시 갱신
            if (aiResponse.getCurrentTotalAffinity() != null) {
                // 서버에서 총 호감도를 계산해서 보내준 경우
                status.setCurrentAffinity(aiResponse.getCurrentTotalAffinity());
            } else if (aiResponse.getAffinityDelta() != null) {
                // 서버에서 변동량(+3, -1 등)만 보내준 경우 기존 호감도에 더하기
                status.setCurrentAffinity(status.getCurrentAffinity() + aiResponse.getAffinityDelta());
            }

            // 4. 상태 저장
            userCharacterStatusRepository.save(status);
        }

        return aiResponse;
    }

    private void saveAiResponseToLog(ChatSession session, ChatMessageDto.Response response) {
        // 1. ChatLog 저장
        ChatLog aiLog = ChatLog.builder()
                .sessionId(session.getSessionId())
                .role(ChatRoleType.assistant)
                .textContent(response.getTextContent())
                .audioUrl(response.getAudioUrl())
                // v3.0: 1차 응답으로 즉시 넘어오는 행동 묘사와 호감도 변동량 저장
                .actionDescription(response.getActionDescription())
                .affinityDelta(response.getAffinityDelta())
                .grammarFeedback(response.getSystemEvaluation() != null ? response.getSystemEvaluation().getGrammar_feedback() : null)
                .isPenalty(response.getSystemEvaluation() != null ? response.getSystemEvaluation().isPenalty() : null)
                .build();
        ChatLog savedLog = chatLogRepository.save(aiLog);

        // 2. RawAiResponse 저장 (Map 변환)
        Map<String, Object> responseMap = objectMapper.convertValue(response, Map.class);
        rawAiResponseRepository.save(RawAiResponse.builder()
                .chatLog(savedLog)
                .responseData(responseMap)
                .createdAt(Instant.now())
                .build());

        // 3. 발음 점수 저장
        if (response.getSystemEvaluation() != null && response.getSystemEvaluation().getPronunciationEvaluations() != null) {
            pronunciationEvaluationRepository.save(PronunciationEvaluation.builder()
                    .chatLog(savedLog)
                    .accuracy(response.getSystemEvaluation().getPronunciationEvaluations().getAccuracy())
                    .fluency(response.getSystemEvaluation().getPronunciationEvaluations().getFluency())
                    .completeness(response.getSystemEvaluation().getPronunciationEvaluations().getCompleteness())
                    .prosody(response.getSystemEvaluation().getPronunciationEvaluations().getProsody())
                    .build());
        }
    }

    private List<HistoryItemDto> mapToHistory(List<ChatLog> logs) {
        return logs.stream().map(log ->
                HistoryItemDto.builder()
                        .role(log.getRole() != null ? log.getRole().toString().toLowerCase() : "user")
                        .text_content(log.getTextContent())
                        .build()
        ).toList();
    }

    @Transactional(readOnly = true)
    public ChatLogDto.HistoryResponse getChatLogsBySessionId(String sessionId, Long userId) {
        // 1. 세션 조회
        List<ChatLog> logs = chatLogRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

//         2. [보안] 세션의 주인이 요청한 userId와 일치하는지 검증하는 로직 추가
        if (logs.size() > 0 && !logs.get(0).getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 세션만 조회할 수 있습니다.");
        }

        // 3. 매핑 및 응답
        List<ChatLogDto.LogItem> logItems = logs.stream()
                .map(log -> ChatLogDto.LogItem.builder()
                        .message_id(log.getMessageId())
                        .role(log.getRole())
                        .input_type(log.getInputType())
                        .text_content(log.getTextContent())
                        .audio_url(log.getAudioUrl())
                        .created_at(log.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ChatLogDto.HistoryResponse.builder()
                .session_id(sessionId)
                .logs(logItems)
                .build();
    }
}