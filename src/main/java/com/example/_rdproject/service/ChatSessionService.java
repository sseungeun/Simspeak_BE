package com.example._rdproject.service;

import com.example._rdproject.dto.ChatSessionDto;
import com.example._rdproject.entity.ChatSession;
import com.example._rdproject.entity.Stage;
import com.example._rdproject.repository.ChatSessionRepository;
import com.example._rdproject.repository.StageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class ChatSessionService {
    private final ChatSessionRepository sessionRepository;
    private final StageRepository stageRepository;

    @Transactional
    public ChatSessionDto.CreateResponse createSession(ChatSessionDto.CreateRequest request) {
        Stage stage = stageRepository.findById(request.getStageId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스테이지 식별자입니다: " + request.getStageId()));

        String sessionId = "sess_" + request.getCharacterId() + "_" + UUID.randomUUID().toString().substring(0, 8);

        ChatSession session = ChatSession.builder()
                .sessionId(sessionId)
                .scenarioId(request.getScenarioId())
                .userId(request.getUserId())
                .stageId(request.getStageId())
                .characterId(request.getCharacterId())
                .createdAt(LocalDateTime.now())
                .build();

        sessionRepository.save(session);

        var firstMessage = ChatSessionDto.FirstMessage.builder()
                .textContent(stage.getFirstText())
                .actionDescription(stage.getFirstAction())
                .audioUrl(stage.getFirstAudioUrl())
                .build();

        return ChatSessionDto.CreateResponse.builder()
                .sessionId(sessionId)
                .firstMessage(firstMessage)
                .build();
    }
    @Transactional
    public ChatSessionDto.ActiveSessionResponse getActiveSession(Long userId, Long stageId) {
        Optional<ChatSession> sessionOpt = sessionRepository.findFirstByUserIdAndStageIdOrderByCreatedAtDesc(userId, stageId);

        // 1. 세션이 아예 없는 경우 -> 프론트에서 새 세션을 시작하도록 유도
        if (sessionOpt.isEmpty()) {
            return ChatSessionDto.ActiveSessionResponse.builder()
                    .sessionId(null)
                    .isCompleted(true)
                    .build();
        }

        // 2. 세션이 있는 경우 -> ID와 완료 여부 반환
        ChatSession session = sessionOpt.get();
        return ChatSessionDto.ActiveSessionResponse.builder()
                .sessionId(session.getSessionId())
                // null 방어: DB에 null로 들어있으면 false로 취급
                .isCompleted(session.getIsCompleted() != null ? session.getIsCompleted() : false)
                .build();
    }
}
