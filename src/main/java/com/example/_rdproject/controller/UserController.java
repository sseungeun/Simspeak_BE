package com.example._rdproject.controller;

import com.example._rdproject.dto.common.CommonResponse;
import com.example._rdproject.dto.GuestAuthDto;
import com.example._rdproject.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth & User Manager", description = "게스트 로그인 및 유저 프로필 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "게스트 로그인 및 초기 진입", description = "기기 고유 UUID를 기반으로 소셜 로그인 없이 유저를 식별 및 가입 처리합니다.")
    @PostMapping("/guest")
    public CommonResponse<GuestAuthDto.ResponseData> guestLogin(@RequestBody GuestAuthDto.Request request) {

        GuestAuthDto.ResponseData responseData = userService.loginOrCreateGuest(request);

        return CommonResponse.success("게스트 로그인이 완료되었습니다.", responseData);
    }
}