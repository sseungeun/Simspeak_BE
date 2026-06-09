package com.example._rdproject.controller;

import com.example._rdproject.dto.AiRequestDto;
import com.example._rdproject.dto.AiResponseDto;
import com.example._rdproject.dto.CorrectionDto;
import com.example._rdproject.dto.ReportDto;
import com.example._rdproject.dto.common.CommonResponse;
import com.example._rdproject.service.ChatReportService;
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
public class ChatReportController {

    private final ChatReportService chatReportService;

    // 1. 스테이지/세션 종료 리포트 종합 조회
    @GetMapping("/reports/sessions/{sessionId}")
    public ResponseEntity<CommonResponse<ReportDto.SessionReportResponse>> getSessionReport(
            @PathVariable String sessionId,
            @RequestParam Long userId) {

        ReportDto.SessionReportResponse response = chatReportService.getSessionReport(sessionId, userId);

        return ResponseEntity.ok(CommonResponse.success("세션 분석 리포트를 성공적으로 구성했습니다.", response));
    }

    // 2. 오답 노트 체크 및 유저 해석 저장
    @PatchMapping("/corrections/{correctionId}")
    public ResponseEntity<CommonResponse<CorrectionDto.CorrectionUpdateResponse>> updateCorrection(
            @PathVariable Long correctionId,
            @RequestBody CorrectionDto.CorrectionUpdateRequest request) {

        CorrectionDto.CorrectionUpdateResponse response = chatReportService.updateCorrection(correctionId, request);

        return ResponseEntity.ok(CommonResponse.success("오답 노트 복습 내용이 반영되었습니다.", response));
    }

    // 3. 전체 미복습/복습 목록 조회
    @GetMapping("/corrections")
    public ResponseEntity<CommonResponse<Map<String, List<CorrectionDto.CorrectionSummary>>>> getCorrections(
            @RequestParam Long userId,
            @RequestParam(required = false) Boolean isReviewed) { // 선택 파라미터로 변경

        // 서비스 로직에서 isReviewed가 null이면 전체, 값이 있으면 필터링하도록 수정
        List<CorrectionDto.CorrectionSummary> list = chatReportService.getCorrections(userId, isReviewed);

        String msg = (isReviewed == null) ? "전체 오답 노트를 불러왔습니다." : "오답 노트를 불러왔습니다.";
        return ResponseEntity.ok(CommonResponse.success(msg, Map.of("corrections", list)));
    }

    @GetMapping("/corrections/bookmarks")
    public ResponseEntity<CommonResponse<Map<String, List<CorrectionDto.CorrectionSummary>>>> getBookmarks(@RequestParam Long userId) {
        // isReviewed를 true로 고정하여 호출
        List<CorrectionDto.CorrectionSummary> list = chatReportService.getCorrections(userId, true);
        return ResponseEntity.ok(CommonResponse.success("북마크된 오답 노트를 불러왔습니다.", Map.of("corrections", list)));
    }

    @PostMapping(value = "/api/analysis/pronunciation", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<AiResponseDto>> analyzePronunciation(
            @RequestPart("audioFile") MultipartFile audioFile,
            @RequestPart("request") AiRequestDto request) {

        AiResponseDto result = chatReportService.analyzeAndSave(request, audioFile);
        return ResponseEntity.ok(CommonResponse.success("음성 분석 및 저장이 완료되었습니다.", result));
    }
}