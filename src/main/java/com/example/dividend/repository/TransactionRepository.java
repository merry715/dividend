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

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND YEAR(t.date) = :year AND t.type = :type")
    List<Transaction> findByUserIdAndYearAndType(@Param("userId") Long userId, @Param("year") int year, @Param("type") String type);
}
