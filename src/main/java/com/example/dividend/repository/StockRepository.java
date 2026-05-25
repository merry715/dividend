package com.example.dividend.repository;

import com.example.dividend.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// @SQLRestriction("deleted_at IS NULL") 이 Stock 엔티티에 적용되어 있어
// 모든 쿼리에 자동으로 soft delete 필터가 적용됨
public interface StockRepository extends JpaRepository<Stock, Long> {

    // 특정 사용자의 전체 보유 종목 조회
    List<Stock> findByUser_Id(Long userId);

    // 단건 조회 (사용자 귀속 검증 포함)
    Optional<Stock> findByIdAndUser_Id(Long id, Long userId);

    // 종목 코드 중복 확인 (사용자 내, soft delete 제외)
    boolean existsByUser_IdAndStockCode(Long userId, String stockCode);

    // 종목명·코드 키워드 검색 (사용자 내)
    @Query("""
            SELECT s FROM Stock s
            WHERE s.user.id = :userId
              AND (LOWER(s.stockName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR  LOWER(s.stockCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    List<Stock> searchByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    // User를 fetch join하여 단건 조회 — 소유권 검증 시 추가 쿼리 방지
    @Query("SELECT s FROM Stock s JOIN FETCH s.user WHERE s.id = :id")
    Optional<Stock> findByIdWithUser(@Param("id") Long id);

    // 전체 목록 조회 시 User를 fetch join — StockResponse.from()에서 getUser() 호출 시 추가 쿼리 방지
    @Query("SELECT s FROM Stock s JOIN FETCH s.user WHERE s.user.id = :userId")
    List<Stock> findByUser_IdWithUser(@Param("userId") Long userId);

    // 많이 등록된 종목 Top 10 (전체 사용자) — [stock_code, stock_name, count]
    @Query(value = """
        SELECT stock_code, stock_name, COUNT(*) AS cnt
        FROM stock
        WHERE deleted_at IS NULL
        GROUP BY stock_code, stock_name
        ORDER BY cnt DESC
        LIMIT 10
        """, nativeQuery = true)
    List<Object[]> findTop10Stocks();

    // 전체 사용자 섹터 비중 — [sector, count]
    @Query(value = """
        SELECT sector, COUNT(*) AS cnt
        FROM stock
        WHERE deleted_at IS NULL
        GROUP BY sector
        ORDER BY cnt DESC
        """, nativeQuery = true)
    List<Object[]> findSectorDistribution();
}
