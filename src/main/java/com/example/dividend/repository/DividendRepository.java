package com.example.dividend.repository;

import com.example.dividend.entity.Dividend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DividendRepository extends JpaRepository<Dividend, Long> {

    List<Dividend> findByStockId(Long stockId);

    List<Dividend> findByYear(int year);
}
