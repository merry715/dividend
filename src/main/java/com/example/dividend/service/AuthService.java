package com.example.dividend.service;

import com.example.dividend.dto.request.LoginRequest;
import com.example.dividend.dto.request.RegisterRequest;
import com.example.dividend.dto.response.AuthResponse;
import com.example.dividend.entity.User;
import com.example.dividend.repository.UserRepository;
import com.example.dividend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + req.getEmail());
        }
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPassword(req.getPassword());   // 테스트용 평문 저장
        user.setName(req.getName());
        user.setRole("ROLE_USER");
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다"));

        if (!user.getPassword().equals(req.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
        }
        user.setLastLoginAt(LocalDateTime.now());

        String token = jwtUtil.generateToken(user.getEmail());
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }
}
