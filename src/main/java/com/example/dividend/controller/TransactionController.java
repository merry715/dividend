package com.example.dividend.controller;

import com.example.dividend.entity.Stock;
import com.example.dividend.entity.Transaction;
import com.example.dividend.repository.StockRepository;
import com.example.dividend.repository.TransactionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final StockRepository stockRepository;

    public TransactionController(TransactionRepository transactionRepository,
                                 StockRepository stockRepository) {
        this.transactionRepository = transactionRepository;
        this.stockRepository = stockRepository;
    }

    @GetMapping("/transactions")
    public String transactions(Model model) {
        model.addAttribute("transactions", transactionRepository.findAll());
        model.addAttribute("stocks", stockRepository.findAll());
        return "transactions";
    }

    @PostMapping("/transactions/add")
    public String addTransaction(@RequestParam Long stockId,
                                 @RequestParam String tradeDate,
                                 @RequestParam String tradeType,
                                 @RequestParam int quantity,
                                 @RequestParam int price,
                                 @RequestParam int fee) {

        Stock stock = stockRepository.findById(stockId).orElse(null);

        Transaction transaction = new Transaction();
        transaction.setStock(stock);
        transaction.setTradeDate(java.time.LocalDate.parse(tradeDate));
        transaction.setTradeType(tradeType);
        transaction.setQuantity(quantity);
        transaction.setPrice(price);
        transaction.setFee(fee);

        transactionRepository.save(transaction);

        return "redirect:/transactions";
    }

    @PostMapping("/transactions/update")
    public String updateTransaction(@RequestParam Long id,
                                    @RequestParam Long stockId,
                                    @RequestParam String tradeDate,
                                    @RequestParam String tradeType,
                                    @RequestParam int quantity,
                                    @RequestParam int price,
                                    @RequestParam int fee) {
        Transaction tx = transactionRepository.findById(id).orElseThrow();
        Stock stock = stockRepository.findById(stockId).orElseThrow();
        tx.setStock(stock);
        tx.setTradeDate(java.time.LocalDate.parse(tradeDate));
        tx.setTradeType(tradeType);
        tx.setQuantity(quantity);
        tx.setPrice(price);
        tx.setFee(fee);
        transactionRepository.save(tx);
        return "redirect:/transactions";
    }

    @GetMapping("/transactions/delete/{id}")
    public String deleteTransaction(@PathVariable Long id) {
        transactionRepository.deleteById(id);
        return "redirect:/transactions";
    }
}