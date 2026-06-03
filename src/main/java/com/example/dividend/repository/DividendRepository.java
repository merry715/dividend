package com.example.dividend.repository;

import com.example.dividend.entity.Dividend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    // 특정 종목·연도의 배당 조회 (작년 지급일 참조용)
    List<Dividend> findByUserIdAndStockIdAndYear(Long userId, Long stockId, int year);

    // 재생성 시 EXPECTED row만 삭제 (CONFIRMED는 보존)
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("""
        DELETE FROM Dividend d
        WHERE d.userId = :userId AND d.stockId = :stockId
          AND d.year = :year AND d.status = 'EXPECTED'
        """)
    void deleteExpectedByUserIdAndStockIdAndYear(
        @Param("userId") Long userId,
        @Param("stockId") Long stockId,
        @Param("year") int year);

    // 거래 삭제·전량 매도로 미보유(순보유 0)가 된 종목의 EXPECTED 배당 정리 (CONFIRMED 보존)
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("""
        DELETE FROM Dividend d
        WHERE d.userId = :userId AND d.stockId = :stockId AND d.status = 'EXPECTED'
        """)
    void deleteExpectedByUserIdAndStockId(
        @Param("userId") Long userId,
        @Param("stockId") Long stockId);


    // 과거 연도(작년 이하) 배당 전체 삭제 — 거래 변경 시 과거 배당 재계산용 (올해 row는 보존)
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("""
        DELETE FROM Dividend d
        WHERE d.userId = :userId AND d.stockId = :stockId AND d.year < :currentYear
        """)
    void deletePastByUserIdAndStockId(
        @Param("userId") Long userId,
        @Param("stockId") Long stockId,
        @Param("currentYear") int currentYear);

    // 스케줄 업데이트 시 전체 삭제 (CONFIRMED 포함)
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("""
        DELETE FROM Dividend d
        WHERE d.userId = :userId AND d.stockId = :stockId AND d.year = :year
        """)
    void deleteByUserIdAndStockIdAndYear(
        @Param("userId") Long userId,
        @Param("stockId") Long stockId,
        @Param("year") int year);

    // 연도별 그룹 집계 — 소프트딜리트 종목 제외
    @Query("""
        SELECT d.year,
               SUM(CASE WHEN d.status = 'CONFIRMED' THEN d.confirmedAmount ELSE 0 END),
               SUM(CASE WHEN d.status = 'EXPECTED'  THEN d.expectedAmount
                        WHEN d.status = 'CONFIRMED' THEN d.confirmedAmount
                        ELSE 0 END)
        FROM Dividend d
        WHERE d.userId = :userId
          AND EXISTS (
              SELECT 1 FROM Stock s
              WHERE s.id = d.stockId
                AND s.deletedAt IS NULL
          )
        GROUP BY d.year
        ORDER BY d.year ASC
        """)
    List<Object[]> findYearlyAggregation(@Param("userId") Long userId);

    // 누적 배당 집계 — 소프트딜리트 종목 제외
    @Query("""
        SELECT
            SUM(CASE WHEN d.status = 'CONFIRMED' THEN d.confirmedAmount ELSE 0 END),
            SUM(CASE WHEN d.status = 'EXPECTED'  THEN d.expectedAmount
                     WHEN d.status = 'CONFIRMED' THEN d.confirmedAmount
                     ELSE 0 END)
        FROM Dividend d
        WHERE d.userId = :userId
          AND EXISTS (
              SELECT 1 FROM Stock s
              WHERE s.id = d.stockId
                AND s.deletedAt IS NULL
          )
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
           // dev에서 가져옴
    List<Dividend> findByYear(int year);
    
    List<Dividend> findByStockIdOrderByYearDescMonthAsc(Long stockId);

    // paymentDate가 null이 아닌 배당만 조회 (monthly-summary용)
    List<Dividend> findByUserIdAndYearAndPaymentDateIsNotNull(Long userId, int year);

    // 소프트 딜리트되지 않은 종목의 배당만 조회 (삭제된 종목 배당 row 노출 방지)
    @Query("""
        SELECT d FROM Dividend d
        WHERE d.userId = :userId
          AND EXISTS (
              SELECT 1 FROM Stock s
              WHERE s.id = d.stockId
                AND s.deletedAt IS NULL
          )
        """)
    List<Dividend> findByUserIdWithActiveStocks(@Param("userId") Long userId);

    // 특정 연도 — 소프트 딜리트되지 않은 종목의 배당만 조회
    // getMonthly / getAnnual / getMonthlySummary 집계에서 삭제 종목 제외용
    @Query("""
        SELECT d FROM Dividend d
        WHERE d.userId = :userId
          AND d.year   = :year
          AND EXISTS (
              SELECT 1 FROM Stock s
              WHERE s.id = d.stockId
                AND s.deletedAt IS NULL
          )
        """)
    List<Dividend> findByUserIdAndYearWithActiveStocks(
        @Param("userId") Long userId, @Param("year") int year);

    // CONFIRMED 전체 + 미래 EXPECTED 조회 (stocks-for-confirm용) — 소프트딜리트 종목 제외
    @Query("""
        SELECT d FROM Dividend d
        WHERE d.userId = :userId
          AND d.year = :year
          AND (
            d.status = 'CONFIRMED'
            OR (d.status = 'EXPECTED' AND d.paymentDate IS NOT NULL AND d.paymentDate >= :today)
          )
          AND EXISTS (
              SELECT 1 FROM Stock s
              WHERE s.id = d.stockId
                AND s.deletedAt IS NULL
          )
        ORDER BY d.stockId ASC, d.month ASC
        """)
    List<Dividend> findUpcomingWithPaymentDate(
        @Param("userId") Long userId,
        @Param("year") int year,
        @Param("today") LocalDate today);
}
