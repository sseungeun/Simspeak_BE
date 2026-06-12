package com.example._rdproject.controller;

import com.example._rdproject.dto.AiRequestDto;
import com.example._rdproject.dto.AiResponseDto;
import com.example._rdproject.dto.CorrectionDto;
import com.example._rdproject.dto.ReportDto;
import com.example._rdproject.dto.common.CommonResponse;
import com.example._rdproject.service.ChatReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Chat Report API", description = "세션 리포트 및 오답 노트 관련 API")
public class ChatReportController {

    private final ChatReportService chatReportService;

    // 1. 스테이지/세션 종료 리포트 종합 조회
    @Operation(summary = "세션/스테이지 리포트 조회", description = "특정 세션 종료 후 분석된 리포트를 종합적으로 조회합니다.")
    @GetMapping("/reports/sessions/{sessionId}")
    public ResponseEntity<CommonResponse<ReportDto.SessionReportResponse>> getSessionReport(
            @PathVariable String sessionId,
            @RequestParam Long userId) {

        ReportDto.SessionReportResponse response = chatReportService.getSessionReport(sessionId, userId);

        return ResponseEntity.ok(CommonResponse.success("세션 분석 리포트를 성공적으로 구성했습니다.", response));
    }

    // 2. 오답 노트 체크 및 유저 해석 저장
    @Operation(summary = "오답 노트 수정", description = "오답 노트의 체크 여부 및 유저의 해석 내용을 저장합니다.")
    @PatchMapping("/corrections/{correctionId}")
    public ResponseEntity<CommonResponse<CorrectionDto.CorrectionUpdateResponse>> updateCorrection(
            @PathVariable Long correctionId,
            @RequestBody CorrectionDto.CorrectionUpdateRequest request) {

        CorrectionDto.CorrectionUpdateResponse response = chatReportService.updateCorrection(correctionId, request);

        return ResponseEntity.ok(CommonResponse.success("오답 노트 복습 내용이 반영되었습니다.", response));
    }

    // 3. 전체 미복습/복습 목록 조회
    @Operation(summary = "전체 오답 노트 목록 조회", description = "유저의 전체 오답 노트를 조회하며, 복습 여부에 따라 필터링할 수 있습니다.")
    @GetMapping("/corrections")
    public ResponseEntity<CommonResponse<Map<String, List<CorrectionDto.CorrectionSummary>>>> getCorrections(
            @RequestParam Long userId,
            @RequestParam(required = false) Boolean isReviewed) { // 선택 파라미터로 변경

        // 서비스 로직에서 isReviewed가 null이면 전체, 값이 있으면 필터링하도록 수정
        List<CorrectionDto.CorrectionSummary> list = chatReportService.getCorrections(userId, isReviewed);

        String msg = (isReviewed == null) ? "전체 오답 노트를 불러왔습니다." : "오답 노트를 불러왔습니다.";
        return ResponseEntity.ok(CommonResponse.success(msg, Map.of("corrections", list)));
    }
    @Operation(summary = "북마크된 오답 노트 조회", description = "복습 완료(북마크)된 오답 노트 목록을 조회합니다.")
    @GetMapping("/corrections/bookmarks")
    public ResponseEntity<CommonResponse<Map<String, List<CorrectionDto.CorrectionSummary>>>> getBookmarks(@RequestParam Long userId) {
        // isReviewed를 true로 고정하여 호출
        List<CorrectionDto.CorrectionSummary> list = chatReportService.getCorrections(userId, true);
        return ResponseEntity.ok(CommonResponse.success("북마크된 오답 노트를 불러왔습니다.", Map.of("corrections", list)));
    }

    @Operation(summary = "발음 분석", description = "업로드된 오디오 파일을 분석하여 발음 결과를 반환하고 저장합니다.",
            responses = { @ApiResponse(responseCode = "200", description = "분석 성공") })
    @PostMapping(value = "/api/analysis/pronunciation", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<AiResponseDto>> analyzePronunciation(
            @RequestPart("audioFile") MultipartFile audioFile,
            @RequestPart("request") AiRequestDto request) {

        AiResponseDto result = chatReportService.analyzeAndSave(request, audioFile);
        return ResponseEntity.ok(CommonResponse.success("음성 분석 및 저장이 완료되었습니다.", result));
    }

    // 캐릭터별 세션(학습 기록) 목록 조회
    @Operation(summary = "캐릭터별 학습 세션 목록 조회", description = "특정 캐릭터와 유저의 전체 학습 기록을 날짜별(Day) 리스트로 조회합니다.")
    @GetMapping("/characters/{characterId}/sessions")
    public ResponseEntity<CommonResponse<List<ReportDto.SessionSummaryResponse>>> getCharacterSessions(
            @PathVariable String characterId,
            @RequestParam Long userId) {

        List<ReportDto.SessionSummaryResponse> list = chatReportService.getCharacterSessions(characterId, userId);
        return ResponseEntity.ok(CommonResponse.success("세션 목록을 성공적으로 조회했습니다.", list));
    }

    // 세션 종료 처리 (엔딩)
    @Operation(summary = "학습 세션 종료", description = "학습 세션을 최종 종료하고 최종 성적(호감도, 등급)을 확정합니다.")
    @PostMapping("/sessions/{sessionId}/end")
    public ResponseEntity<CommonResponse<ReportDto.SessionEndResponse>> endSession(
            @PathVariable String sessionId) {

        ReportDto.SessionEndResponse response = chatReportService.endSession(sessionId);
        return ResponseEntity.ok(CommonResponse.success("세션이 성공적으로 종료되었습니다.", response));
    }

}