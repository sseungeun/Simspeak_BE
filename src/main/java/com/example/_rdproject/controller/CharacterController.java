package com.example._rdproject.controller;

import com.example._rdproject.dto.CharacterDto;
import com.example._rdproject.dto.StageProgressDto;
import com.example._rdproject.dto.UserDto;
import com.example._rdproject.dto.common.CommonResponse;
import com.example._rdproject.service.CharacterService;
import com.example._rdproject.service.StageService;
import com.example._rdproject.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Lobby & Character", description = "로비 메인 및 캐릭터/스테이지 관련 API")
@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;
    private final UserService userService;
    private final StageService stageService;

    @Operation(
            summary = "메인 화면 캐릭터 목록 및 유저 상태 조회",
            description = "홈 화면 진입 시 유저의 기본 상태 데이터와 캐릭터별 호감도/해금 상태 정보를 종합 반환합니다."
    )
    @GetMapping("/status")
    public ResponseEntity<CommonResponse<CharacterDto.MainStatusResponse>> getMainStatus(
            @RequestParam(name = "userId") Long userId
    ) {
        CharacterDto.MainStatusResponse data = characterService.getMainStatus(userId);
        return ResponseEntity.ok(CommonResponse.success("메인 로비 데이터 조회가 완료되었습니다.", data));
    }

    @Operation(
            summary = "특정 캐릭터의 스테이지 리스트 조회",
            description = "선택한 캐릭터에 종속된 챕터/스테이지들의 세부 미션 리스트를 조회합니다."
    )
    @GetMapping("/{character_id}/stages")
    public ResponseEntity<CommonResponse<CharacterDto.StageListResponse>> getCharacterStages(
            @RequestParam(name = "userId") Long userId,
            @PathVariable(name = "character_id") Long characterId
    ) {
        CharacterDto.StageListResponse data = characterService.getCharacterStages(userId, characterId);
        return ResponseEntity.ok(CommonResponse.success("캐릭터별 스테이지 목록 및 진척도 조회가 완료되었습니다.", data));
    }

    @Operation(
            summary = "보유 캐릭터 목록 조회",
            description = "현재 유저가 해금/보유한 모든 캐릭터 목록 및 현재 호감도, MBTI 능력치 정보를 반환합니다."
    )
    @GetMapping("/my-list")
    public ResponseEntity<CommonResponse<List<CharacterDto.MyCharacterResponse>>> getMyCharacters(
            @RequestParam(name = "userId") Long userId
    ) {
        List<CharacterDto.MyCharacterResponse> response = characterService.getMyCharacters(userId);
        return ResponseEntity.ok(CommonResponse.success("보유 캐릭터 목록 조회가 완료되었습니다.", response));
    }

    @Operation(
            summary = "최근 사용 캐릭터 업데이트",
            description = "메인 로비에서 캐릭터를 교체하거나 대화 진입 시, 마지막 사용 캐릭터 ID를 DB에 갱신합니다."
    )
    @PatchMapping("/last-character")
    public ResponseEntity<CommonResponse<String>> updateLastCharacter(
            @RequestParam(name = "userId") Long userId,
            @RequestBody UserDto.UpdateLastCharacterRequest request
    ) {
        userService.updateLastCharacter(userId, request.getCharacterId());
        return ResponseEntity.ok(CommonResponse.success("최근 사용 캐릭터가 성공적으로 변경되었습니다.", "SUCCESS"));
    }
    @Operation(
            summary = "스테이지 진행도 완료 및 다음 단계 해금",
            description = "현재 스테이지 대화 완료 시 점수와 통과 여부를 받아 현재 단계를 완료 처리하고 다음 단계를 활성화합니다."
    )
    @PostMapping("/progress")
    public ResponseEntity<CommonResponse<StageProgressDto.UpdateResponse>> updateStageProgress(
            @RequestBody StageProgressDto.UpdateRequest request
    ) {
        StageProgressDto.UpdateResponse response = stageService.updateStageProgress(request);
        return ResponseEntity.ok(CommonResponse.success("스테이지 진행도 업데이트가 완료되었습니다.", response));
    }

}
