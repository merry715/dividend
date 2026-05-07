package com.example.dividend.controller;

import com.example.dividend.entity.Dividend;
import com.example.dividend.entity.Stock;
import com.example.dividend.repository.DividendRepository;
import com.example.dividend.repository.StockRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class DividendController {

    private final DividendRepository dividendRepository;
    private final StockRepository stockRepository;

    public DividendController(DividendRepository dividendRepository,
                              StockRepository stockRepository) {
        this.dividendRepository = dividendRepository;
        this.stockRepository = stockRepository;
    }

    @GetMapping("/dividends")
    public String dividends(Model model) {
        model.addAttribute("dividends", dividendRepository.findAll());
        model.addAttribute("stocks", stockRepository.findAll());
        return "dividends";
    }

    @PostMapping("/dividends/add")
    public String addDividend(@RequestParam Long stockId,
                              @RequestParam int dividendPerShare,
                              @RequestParam int paymentMonth,
                              @RequestParam String status) {

        Stock stock = stockRepository.findById(stockId).orElse(null);

        Dividend dividend = new Dividend();
        dividend.setStock(stock);
        dividend.setDividendPerShare(dividendPerShare);
        dividend.setPaymentMonth(paymentMonth);
        dividend.setStatus(status);

        dividendRepository.save(dividend);

        return "redirect:/dividends";
    }

    @PostMapping("/dividends/update")
    public String updateDividend(@RequestParam Long id,
                                 @RequestParam Long stockId,
                                 @RequestParam int dividendPerShare,
                                 @RequestParam int paymentMonth,
                                 @RequestParam String status) {
        Dividend dividend = dividendRepository.findById(id).orElseThrow();
        Stock stock = stockRepository.findById(stockId).orElseThrow();
        dividend.setStock(stock);
        dividend.setDividendPerShare(dividendPerShare);
        dividend.setPaymentMonth(paymentMonth);
        dividend.setStatus(status);
        dividendRepository.save(dividend);
        return "redirect:/dividends";
    }

    @GetMapping("/dividends/delete/{id}")
    public String deleteDividend(@PathVariable Long id) {
        dividendRepository.deleteById(id);
        return "redirect:/dividends";
    }
}