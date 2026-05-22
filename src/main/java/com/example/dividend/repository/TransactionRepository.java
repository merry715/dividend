package com.example.dividend.repository;

import com.example.dividend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByStockId(Long stockId);

    List<Transaction> findByType(String type);

    List<Transaction> findByStockIdAndType(Long stockId, String type);

    @Query("SELECT t FROM Transaction t WHERE YEAR(t.date) = :year")
    List<Transaction> findByYear(@Param("year") int year);

    @Query("SELECT t FROM Transaction t WHERE YEAR(t.date) = :year AND t.type = :type")
    List<Transaction> findByYearAndType(@Param("year") int year, @Param("type") String type);
}
