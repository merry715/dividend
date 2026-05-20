package com.example.dividend.repository;

import com.example.dividend.entity.Dividend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DividendRepository extends JpaRepository<Dividend, Long> {

    List<Dividend> findByStockId(Long stockId);

    List<Dividend> findByYear(int year);

    // 종목의 배당 목록을 연도 내림차순, 지급월 오름차순으로 정렬
    List<Dividend> findByStockIdOrderByYearDescPaymentMonthAsc(Long stockId);

    // 여러 종목 ID의 배당 한번에 조회 (N+1 방지)
    List<Dividend> findByStockIdIn(List<Long> stockIds);
}
