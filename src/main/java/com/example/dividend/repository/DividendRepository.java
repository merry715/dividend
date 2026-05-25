package com.example.dividend.repository;

import com.example.dividend.entity.Dividend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DividendRepository extends JpaRepository<Dividend, Long> {

    // 사용자별 전체 배당 조회
    List<Dividend> findByUserId(Long userId);

    // 사용자별 특정 연도 배당 조회
    List<Dividend> findByUserIdAndYear(Long userId, int year);

    // 사용자+ID로 단건 조회 (소유권 검증)
    Optional<Dividend> findByIdAndUserId(Long id, Long userId);

    // 종목별 배당 조회 (StockService의 N+1 방지용)
    List<Dividend> findByStockIdIn(List<Long> stockIds);

    // 종목 단건 배당 조회
    List<Dividend> findByStockId(Long stockId);

    // 중복 생성 방지 체크
    boolean existsByUserIdAndStockIdAndYearAndMonth(Long userId, Long stockId, int year, int month);

    // 연도별 그룹 집계 (JPQL)
    @Query("""
        SELECT d.year,
               SUM(CASE WHEN d.status = 'CONFIRMED' THEN d.confirmedAmount ELSE 0 END),
               SUM(CASE WHEN d.status = 'EXPECTED'  THEN d.expectedAmount
                        WHEN d.status = 'CONFIRMED' THEN d.confirmedAmount
                        ELSE 0 END)
        FROM Dividend d
        WHERE d.userId = :userId
        GROUP BY d.year
        ORDER BY d.year ASC
        """)
    List<Object[]> findYearlyAggregation(@Param("userId") Long userId);

    // 누적 배당 집계
    @Query("""
        SELECT
            SUM(CASE WHEN d.status = 'CONFIRMED' THEN d.confirmedAmount ELSE 0 END),
            SUM(CASE WHEN d.status = 'EXPECTED'  THEN d.expectedAmount
                     WHEN d.status = 'CONFIRMED' THEN d.confirmedAmount
                     ELSE 0 END)
        FROM Dividend d
        WHERE d.userId = :userId
        """)
    List<Object[]> findCumulativeAggregation(@Param("userId") Long userId);

    // 섹터별 평균 배당금 — [sector, avg_amount]
    @Query(value = """
        SELECT s.sector,
               AVG(CASE WHEN d.status = 'CONFIRMED' THEN d.confirmed_amount
                        ELSE d.expected_amount END) AS avg_amount
        FROM dividend d
        JOIN stock s ON d.stock_id = s.id
        WHERE s.deleted_at IS NULL
        GROUP BY s.sector
        ORDER BY avg_amount DESC
        """, nativeQuery = true)
    List<Object[]> findAvgDividendBySector();

    // 종목별 평균 배당금 — [stock_code, stock_name, avg_amount]
    @Query(value = """
        SELECT s.stock_code, s.stock_name,
               AVG(CASE WHEN d.status = 'CONFIRMED' THEN d.confirmed_amount
                        ELSE d.expected_amount END) AS avg_amount
        FROM dividend d
        JOIN stock s ON d.stock_id = s.id
        WHERE s.deleted_at IS NULL
        GROUP BY s.stock_code, s.stock_name
        ORDER BY avg_amount DESC
        """, nativeQuery = true)
    List<Object[]> findAvgDividendByStock();
}
