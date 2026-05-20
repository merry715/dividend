package com.example.dividend.service;

import com.example.dividend.dto.LoginRequest;
import com.example.dividend.dto.SignupRequest;
import com.example.dividend.dto.TokenResponse;
import com.example.dividend.entity.RefreshToken;
import com.example.dividend.entity.User;
import com.example.dividend.repository.RefreshTokenRepository;
import com.example.dividend.repository.UserRepository;
import com.example.dividend.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtUtil jwtUtil,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TokenResponse signup(SignupRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다");
        }
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setName(req.getName());
        user.setRole("ROLE_USER");
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

    // Refresh Token Rotation: 기존 토큰 삭제 후 새로운 토큰 쌍 발급
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다"));
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new IllegalArgumentException("만료된 리프레시 토큰입니다");
        }
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        refreshTokenRepository.delete(stored);
        return issueTokens(user);
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtUtil.generateToken(user.getEmail());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        RefreshToken rt = new RefreshToken();
        rt.setToken(newRefreshToken);
        rt.setUserId(user.getId());
        rt.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(rt);

        TokenResponse response = new TokenResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(newRefreshToken);
        response.setExpiresIn(86400L);  // 24시간 (초)
        return response;
    }
}
