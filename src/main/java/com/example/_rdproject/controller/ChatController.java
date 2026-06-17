package com.example._rdproject.controller;

import com.example._rdproject.dto.ChatLogDto;
import com.example._rdproject.dto.ChatMessageDto;
import com.example._rdproject.dto.ChatSessionDto;
import com.example._rdproject.dto.common.CommonResponse;
import com.example._rdproject.service.ChatService;
import com.example._rdproject.service.ChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<CommonResponse<ChatMessageDto.FrontendResponse>> sendMessage(
            @RequestBody ChatMessageDto.Request request) {

        // 서비스 호출하여 비즈니스 로직(AI 통신 + 로그 저장) 수행
        ChatMessageDto.FrontendResponse response = chatService.processMessage(request);

        // 성공 응답 반환
        return ResponseEntity.ok(CommonResponse.success("메시지 처리가 완료되었습니다.", response));
    }
    /**
     * 유저 메시지 히스토리 조회
     */
    @Operation(summary = "메시지 히스토리 조회", description = "스테이지 대화 히스토리 조회 (이어하기/채팅방 진입용)")
    @GetMapping("/sessions/{session_id}/logs")
    public CommonResponse<ChatLogDto.HistoryResponse> getChatLogs(
            @PathVariable("session_id") String sessionId,
            @RequestParam("userId") Long userId) {

        // 서비스 로직에서 userId 검증 및 로그 조회
        ChatLogDto.HistoryResponse history = chatService.getChatLogsBySessionId(sessionId, userId);

        return CommonResponse.success("지난 대화 로그를 성공적으로 불러왔습니다.", history);
    }

    @Operation(summary = "이어하기 세션 조회", description = "특정 유저와 스테이지의 최근(미완료) 세션 ID를 반환합니다. 없으면 null을 반환합니다.")
    @GetMapping("/sessions/active")
    public ResponseEntity<CommonResponse<ChatSessionDto.ActiveSessionResponse>> getActiveSession(
            @RequestParam("userId") Long userId,
            @RequestParam("stageId") Long stageId) {

        ChatSessionDto.ActiveSessionResponse response = chatSessionService.getActiveSession(userId, stageId);

        String msg = response.getSessionId() != null ? "진행 중인 세션을 찾았습니다." : "진행 중인 세션이 없습니다.";
        return ResponseEntity.ok(CommonResponse.success(msg, response));
    }
}
