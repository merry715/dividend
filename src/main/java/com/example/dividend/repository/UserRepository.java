package com.example.dividend.repository;

import com.example.dividend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    String ROLE_ADMIN = "ROLE_ADMIN";

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRoleNot(String role);

    // 월별 신규 가입자 추이 (최근 12개월, 관리자 제외) — [year_month, count]
    @Query(value = """
        SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, COUNT(*) AS cnt
        FROM users
        WHERE role != 'ROLE_ADMIN'
          AND created_at >= DATE_SUB(NOW(), INTERVAL 12 MONTH)
        GROUP BY DATE_FORMAT(created_at, '%Y-%m')
        ORDER BY month ASC
        """, nativeQuery = true)
    List<Object[]> findMonthlySignupTrend();

    // 활성 사용자 수 (최근 30일 내 로그인, 관리자 제외)
    @Query(value = """
        SELECT COUNT(*) FROM users
        WHERE role != 'ROLE_ADMIN'
          AND last_login_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
        """, nativeQuery = true)
    long countActiveUsers();
}
