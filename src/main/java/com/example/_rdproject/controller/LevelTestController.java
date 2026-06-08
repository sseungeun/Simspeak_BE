package com.example._rdproject.controller;

import com.example._rdproject.dto.common.CommonResponse;
import com.example._rdproject.dto.LevelTestDto;
import com.example._rdproject.service.LevelTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Level Test", description = "레벨 테스트 관련 API")
@RestController
@RequestMapping("/api/level-tests")
@RequiredArgsConstructor
public class LevelTestController {

    private final LevelTestService levelTestService;

    @Operation(
            summary = "레벨 테스트 결과 등록 및 등급 설정",
            description = "유저가 치른 테스트 결과(또는 직접 선택한 등급)를 저장하고 유저의 현재 레벨을 업데이트합니다."
    )
    @PostMapping
    public ResponseEntity<CommonResponse<Void>> saveLevelTest(
            @RequestBody LevelTestDto.SaveRequest request
    ) {
        levelTestService.saveLevelTestResult(request);
        // 1번째 인자: 메시지, 2번째 인자: 데이터(null) -> 제네릭 명시를 위해 <Void> 추가
        return ResponseEntity.ok(CommonResponse.<Void>success("레벨 테스트 결과 등록 및 등급 설정이 완료되었습니다.", null));
    }

    @Operation(
            summary = "유저 현재 레벨 상태 조회",
            description = "특정 유저의 현재 CEFR 레벨 등급을 조회합니다."
    )
    @GetMapping("/status")
    public ResponseEntity<CommonResponse<LevelTestDto.StatusResponse>> getLevelStatus(
            @RequestParam(name = "userId") Long userId
    ) {
        LevelTestDto.StatusResponse response = levelTestService.getUserLevelStatus(userId);
        // 1번째 인자: 메시지, 2번째 인자: 데이터(response)
        return ResponseEntity.ok(CommonResponse.success("유저 레벨 상태 조회가 완료되었습니다.", response));
    }
    // 문항 조회 API
    @Operation(summary = "레벨테스트 질문 목록", description = "주관식/음성 문항에 대한 질문을 보여줍니다.")
    @GetMapping("/questions")
    public ResponseEntity<CommonResponse<LevelTestDto.QuestionListResponse>> getQuestions() {
        return ResponseEntity.ok(CommonResponse.success("성공", levelTestService.getAllQuestions()));
    }

    // 답변 제출 API
    @Operation(summary = "레벨 테스트 답변 제출", description = "주관식/음성 문항에 대한 답변을 저장합니다.")
    @PostMapping("/answer")
    public ResponseEntity<CommonResponse<Void>> submitAnswer(@RequestBody LevelTestDto.SubmitAnswerRequest request) {
        // AnswerType 제거에 맞춰 인자 3개만 전달
        levelTestService.submitAnswer(
                request.getUserId(),
                request.getQuestionId(),
                request.getAnswerText()
        );
        return ResponseEntity.ok(CommonResponse.success("답변이 저장되었습니다.", null));
    }
}