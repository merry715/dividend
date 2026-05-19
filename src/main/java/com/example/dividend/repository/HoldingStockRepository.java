package com.example.dividend.repository;

import com.example.dividend.entity.HoldingStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HoldingStockRepository extends JpaRepository<HoldingStock, Long> {

    List<HoldingStock> findByStockId(Long stockId);
}
