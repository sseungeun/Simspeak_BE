package com.example._rdproject.controller;

import com.example._rdproject.dto.AuthDto;
import com.example._rdproject.dto.GuestAuthDto;
import com.example._rdproject.dto.UserDto;
import com.example._rdproject.dto.common.CommonResponse;

import com.example._rdproject.service.AuthService;
import com.example._rdproject.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth & User Manager", description = "게스트 로그인 및 유저 프로필 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @Operation(summary = "게스트 로그인 및 초기 진입", description = "기기 고유 UUID를 기반으로 소셜 로그인 없이 유저를 식별 및 가입 처리합니다.")
    @PostMapping("/guest")
    public CommonResponse<GuestAuthDto.GuestLoginResponseData> guestLogin(@RequestBody GuestAuthDto.GuestLoginRequest request) {

        GuestAuthDto.GuestLoginResponseData responseData = userService.loginOrCreateGuest(request);

        return CommonResponse.success("게스트 로그인이 완료되었습니다.", responseData);
    }
    @Operation(summary = "자체 회원가입", description = "ID/PW 기반으로 신규 회원을 등록합니다.")
    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<AuthDto.SignupResponse>> signup(@RequestBody AuthDto.SignupRequest request) {
        AuthDto.SignupResponse response = authService.signup(request);
        return ResponseEntity.ok(CommonResponse.success("회원가입이 완료되었습니다. 로그인을 진행해주세요.", response));
    }

    @Operation(summary = "자체 로그인", description = "ID/PW 기반으로 로그인을 수행합니다.")
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<AuthDto.LoginResponse>> login(@RequestBody AuthDto.LoginRequest request) {
        AuthDto.LoginResponse response = authService.login(request);
        return ResponseEntity.ok(CommonResponse.success("로그인에 성공했습니다.", response));
    }

    // --- [마이페이지 프로필 수정 API 창구 (진짜 실무자 연결 버전!)] ---
    @Operation(summary = "내 프로필 수정", description = "마이페이지에서 유저의 닉네임과 선호 성별을 수정합니다.")
    @PutMapping("/{user_id}/profile")
    public ResponseEntity<com.example._rdproject.dto.common.CommonResponse<UserDto.UpdateProfileResponse>> updateProfile(
            @PathVariable("user_id") Long userId,
            @RequestBody UserDto.UpdateProfileRequest request
    ) {
        // 1. 드디어 창구 직원이 실무자(Service)에게 진짜로 일을 시킵니다!
        UserDto.UpdateProfileResponse response = userService.updateProfile(userId, request);
        
        // 2. 예쁜 공통 응답 상자(CommonResponse)에 담아서 프론트엔드에게 돌려줍니다!
        return ResponseEntity.ok(com.example._rdproject.dto.common.CommonResponse.success("프로필 수정을 성공했습니다.", response));
    }

    // --- [내 프로필 정보 조회 API 창구] ---
    @Operation(summary = "내 프로필 조회", description = "마이페이지 진입 시 유저의 프로필 정보를 조회합니다.")
    @GetMapping("/{user_id}/profile")
    public ResponseEntity<com.example._rdproject.dto.common.CommonResponse<UserDto.GetProfileResponse>> getProfile(
            @PathVariable("user_id") Long userId
    ) {
        // 실무자(Service)에게 조회를 시키고 결과를 받습니다.
        UserDto.GetProfileResponse response = userService.getProfile(userId);
        
        // 팀에서 쓰는 공통 응답 규격(CommonResponse)에 예쁘게 담아서 돌려보냅니다!
        return ResponseEntity.ok(com.example._rdproject.dto.common.CommonResponse.success("프로필 조회를 성공했습니다.", response));
    }
}