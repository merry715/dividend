package com.example.dividend.service;

import com.example.dividend.client.PythonServerClient;
import com.example.dividend.dto.response.StockSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockSearchService {

    private final PythonServerClient pythonServerClient;

    public List<StockSearchResult> search(String name) {
        if (name == null || name.isBlank()) {
            return Collections.emptyList();
        }
        return pythonServerClient.searchStocks(name);
    }
}
