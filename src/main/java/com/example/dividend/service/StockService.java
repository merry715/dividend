package com.example.dividend.service;

import com.example.dividend.entity.Stock;
import com.example.dividend.repository.StockRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class StockService {

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public List<Stock> getAll() {
        return stockRepository.findAll();
    }

    public Stock add(Stock stock) {
        return stockRepository.save(stock);
    }

    public Stock update(Long id, Map<String, Object> req) {
        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("종목을 찾을 수 없습니다: " + id));

        if (req.containsKey("stockName"))     stock.setStockName(req.get("stockName").toString());
        if (req.containsKey("stockCode"))     stock.setStockCode(req.get("stockCode").toString());
        if (req.containsKey("sector"))        stock.setSector(req.get("sector").toString());
        if (req.containsKey("lastClosePrice"))
            stock.setLastClosePrice(Integer.parseInt(req.get("lastClosePrice").toString()));

        return stockRepository.save(stock);
    }

    public void delete(Long id) {
        if (!stockRepository.existsById(id)) {
            throw new NoSuchElementException("종목을 찾을 수 없습니다: " + id);
        }
        stockRepository.deleteById(id);
    }

    public List<Stock> search(String keyword) {
        return stockRepository
                .findByStockNameContainingIgnoreCaseOrStockCodeContainingIgnoreCase(keyword, keyword);
    }

    public Stock updateSector(Long id, String sector) {
        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("종목을 찾을 수 없습니다: " + id));
        stock.setSector(sector);
        return stockRepository.save(stock);
    }
}
