package com.example.dividend.repository;

import com.example.dividend.entity.StockPriceCache;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockPriceCacheRepository extends JpaRepository<StockPriceCache, String> {
}
