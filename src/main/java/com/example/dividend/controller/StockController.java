package com.example.dividend.controller;

import com.example.dividend.entity.Stock;
import com.example.dividend.repository.StockRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class StockController {

    private final StockRepository stockRepository;

    public StockController(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @GetMapping("/stocks")
    public String stocks(Model model) {
        model.addAttribute("stocks", stockRepository.findAll());
        return "stocks";
    }

    @PostMapping("/stocks/add")
    public String addStock(Stock stock) {
        stockRepository.save(stock);
        return "redirect:/stocks";
    }

    @PostMapping("/stocks/update")
    public String updateStock(@RequestParam Long id,
                              @RequestParam String stockName,
                              @RequestParam String ticker,
                              @RequestParam String market,
                              @RequestParam String sector,
                              @RequestParam String dividendCycle) {
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.setStockName(stockName);
        stock.setTicker(ticker);
        stock.setMarket(market);
        stock.setSector(sector);
        stock.setDividendCycle(dividendCycle);
        stockRepository.save(stock);
        return "redirect:/stocks";
    }

    @GetMapping("/stocks/delete/{id}")
    public String deleteStock(@PathVariable Long id) {
        stockRepository.deleteById(id);
        return "redirect:/stocks";
    }
}