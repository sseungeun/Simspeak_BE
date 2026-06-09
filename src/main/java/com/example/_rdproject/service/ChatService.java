package com.example._rdproject.service;

import com.example._rdproject.dto.ChatLogDto;
import com.example._rdproject.dto.ChatMessageDto;
import com.example._rdproject.domain.ChatRoleType;
import com.example._rdproject.entity.*;
import com.example._rdproject.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.HashMap;
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

        ChatMessageDto.Response aiResponse = aiServerWebClient.post()
                .uri("/api/chat")
                .bodyValue(aiRequest)
                .retrieve()
                .bodyToMono(ChatMessageDto.Response.class)
                .block();

        if (aiResponse != null) {
            saveAiResponseToLog(session, aiResponse);
            status.setTurnCount(status.getTurnCount() + 1);
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
                .grammarFeedback(response.getSystemEvaluation() != null ? response.getSystemEvaluation().getGrammarFeedback() : null)
                .isPenalty(response.getSystemEvaluation() != null ? response.getSystemEvaluation().getIsPenalty() : null)
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
        if (response.getSystemEvaluation() != null && response.getSystemEvaluation().getPronunciationScore() != null) {
            pronunciationEvaluationRepository.save(PronunciationEvaluation.builder()
                    .chatLog(savedLog)
                    .accuracy(response.getSystemEvaluation().getPronunciationScore().getAccuracy())
                    .fluency(response.getSystemEvaluation().getPronunciationScore().getFluency())
                    .completeness(response.getSystemEvaluation().getPronunciationScore().getCompleteness())
                    .prosody(response.getSystemEvaluation().getPronunciationScore().getProsody())
                    .build());
        }
    }

    private List<Map<String, String>> mapToHistory(List<ChatLog> logs) {
        return logs.stream().map(log -> {
            Map<String, String> map = new HashMap<>();
            map.put("role", log.getRole() != null ? log.getRole().toString().toLowerCase() : "user");
            map.put("text_content", log.getTextContent());
            return map;
        }).toList();
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