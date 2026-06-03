package com.example.dividend.controller;

import com.example.dividend.dto.ApiResponse;
import com.example.dividend.dto.LoginRequest;
import com.example.dividend.dto.SignupRequest;
import com.example.dividend.dto.TokenResponse;
import com.example.dividend.dto.UserResponse;
import com.example.dividend.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 회원가입
    @PostMapping("/signup")
    public ApiResponse<TokenResponse> signup(@RequestBody SignupRequest req) {
        try {
            return ApiResponse.ok(authService.signup(req), "회원가입이 완료되었습니다");
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage(), "SIGNUP_FAILED");
        }
    }

    // 로그인
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody LoginRequest req) {
        try {
            return ApiResponse.ok(authService.login(req), "로그인 성공");
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage(), "LOGIN_FAILED");
        }
    }

    // 로그아웃
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody Map<String, String> req) {
        authService.logout(req.get("refreshToken"));
        return ApiResponse.ok(null, "로그아웃 되었습니다");
    }

    // 내 정보 조회
    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(authService.getMe(userId), "사용자 정보 조회 성공");
    }

    // Access Token 재발급
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody Map<String, String> req) {
        try {
            return ApiResponse.ok(authService.refresh(req.get("refreshToken")));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage(), "TOKEN_REFRESH_FAILED");
        }
    }
}
