package com.example.dividend.repository;

import com.example.dividend.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockRepository extends JpaRepository<Stock, Long> {

    List<Stock> findByStockNameContainingIgnoreCaseOrStockCodeContainingIgnoreCase(
            String stockName, String stockCode);
}
