package com.example.dividend.repository;

import com.example.dividend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserId(Long userId);

    List<Transaction> findByUserIdAndStockId(Long userId, Long stockId);

    List<Transaction> findByUserIdAndType(Long userId, String type);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND YEAR(t.date) = :year")
    List<Transaction> findByUserIdAndYear(@Param("userId") Long userId, @Param("year") int year);

    // PR#56: userId 포함 버전 (userId 누락 버그 수정)
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND YEAR(t.date) = :year AND t.type = :type")
    List<Transaction> findByUserIdAndYearAndType(@Param("userId") Long userId, @Param("year") int year, @Param("type") String type);

    /** BUY - SELL 합계로 실제 보유수량 계산 */
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN t.type = 'BUY' THEN t.quantity ELSE -t.quantity END), 0)
        FROM Transaction t WHERE t.stockId = :stockId
        """)
    int calculateNetQuantity(@Param("stockId") Long stockId);

    // ── 삭제 종목 제외 (거래관리 페이지용) ───────────────────────────────────
    // Stock.deletedAt IS NULL 조건을 서브쿼리로 명시 (@SQLRestriction 의존 없이 확실하게 처리)

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.userId = :userId
          AND t.stockId IN (
              SELECT s.id FROM Stock s WHERE s.user.id = :userId AND s.deletedAt IS NULL
          )
        """)
    List<Transaction> findActiveByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.userId = :userId
          AND t.stockId IN (
              SELECT s.id FROM Stock s WHERE s.user.id = :userId AND s.deletedAt IS NULL
          )
          AND YEAR(t.date) = :year
        """)
    List<Transaction> findActiveByUserIdAndYear(@Param("userId") Long userId, @Param("year") int year);

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.userId = :userId
          AND t.stockId IN (
              SELECT s.id FROM Stock s WHERE s.user.id = :userId AND s.deletedAt IS NULL
          )
          AND t.type = :type
        """)
    List<Transaction> findActiveByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.userId = :userId
          AND t.stockId IN (
              SELECT s.id FROM Stock s WHERE s.user.id = :userId AND s.deletedAt IS NULL
          )
          AND YEAR(t.date) = :year AND t.type = :type
        """)
    List<Transaction> findActiveByUserIdAndYearAndType(
            @Param("userId") Long userId, @Param("year") int year, @Param("type") String type);
}
