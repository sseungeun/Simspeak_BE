package com.example._rdproject.service;

import com.example._rdproject.dto.ChatSessionDto;
import com.example._rdproject.entity.ChatSession;
import com.example._rdproject.repository.ChatSessionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class ChatSessionService {
    private final ChatSessionRepository sessionRepository;

    @Transactional
    public ChatSessionDto.CreateResponse createSession(ChatSessionDto.CreateRequest request) {
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

        // 하드코딩
        var firstMessage = ChatSessionDto.FirstMessage.builder()
                .textContent("Hey there! Welcome to the beach. Are you ready to surf?")
                .actionDescription("서핑보드를 툭툭 치며 환하게 웃는다.")
                .audioUrl("https://azure-storage.net/audio/leo_first_say.mp3")
                .build();

        return ChatSessionDto.CreateResponse.builder()
                .sessionId(sessionId)
                .firstMessage(firstMessage)
                .build();
    }
}
