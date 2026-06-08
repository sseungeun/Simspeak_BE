package com.example._rdproject.controller;

import com.example._rdproject.dto.ChatMessageDto;
import com.example._rdproject.dto.ChatSessionDto;
import com.example._rdproject.dto.common.CommonResponse;
import com.example._rdproject.service.ChatService;
import com.example._rdproject.service.ChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatSessionService chatSessionService;
    private final ChatService chatService;

    @Operation(summary = "초기 대화 수신", description = "대화 세션 시작(방 생성) 및 초기 대사 수신을 보여줍니다.")
    @PostMapping("/sessions")
    public ResponseEntity<CommonResponse<ChatSessionDto.CreateResponse>> createSession(
            @RequestBody ChatSessionDto.CreateRequest request) {
        ChatSessionDto.CreateResponse response = chatSessionService.createSession(request);
        return ResponseEntity.ok(CommonResponse.success("대화 세션이 생성되었습니다.", response));
    }
    /**
     * 유저 메시지 전송 및 AI 응답 처리 API
     */
    @Operation(summary = "유저 메시지 전송 및 AI 응답 처리", description = "대화의 전체적인 응답 처리를 도와줍니다.")
    @PostMapping("/message")
    public ResponseEntity<CommonResponse<ChatMessageDto.Response>> sendMessage(
            @RequestBody ChatMessageDto.Request request) {

        // 서비스 호출하여 비즈니스 로직(AI 통신 + 로그 저장) 수행
        ChatMessageDto.Response response = chatService.processMessage(request);

        // 성공 응답 반환
        return ResponseEntity.ok(CommonResponse.success("메시지 처리가 완료되었습니다.", response));
    }
}
