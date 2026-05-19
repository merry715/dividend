package com.example.dividend.repository;

import com.example.dividend.entity.SectorTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectorTargetRepository extends JpaRepository<SectorTarget, Long> {

    List<SectorTarget> findByUserId(Long userId);
}
