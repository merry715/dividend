package com.example.dividend.repository;

import com.example.dividend.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    Optional<Goal> findByUserIdAndYear(Long userId, int year);
}