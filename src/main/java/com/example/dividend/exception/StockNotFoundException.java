package com.example.dividend.exception;

public class StockNotFoundException extends RuntimeException {

    public StockNotFoundException(Long stockId) {
        super("존재하지 않는 종목입니다: " + stockId);
    }
}
