package com.example.dividend.controller;

import com.example.dividend.service.AdminStatsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminStatsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AdminStatsService adminStatsService;

    // ── getUserStats ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN: GET /api/admin/stats/users → 200")
    @WithMockUser(roles = "ADMIN")
    void getUserStats_asAdmin_returns200() throws Exception {
        given(adminStatsService.getUserStats())
                .willReturn(Map.of("totalUsers", 42L, "monthlyTrend", List.of()));

        mockMvc.perform(get("/api/admin/stats/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(42));
    }

    @Test
    @DisplayName("USER: GET /api/admin/stats/users → 403")
    @WithMockUser(roles = "USER")
    void getUserStats_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/stats/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("비인증: GET /api/admin/stats/users → 403")
    void getUserStats_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/stats/users"))
                .andExpect(status().isForbidden());
    }

    // ── getActiveUsers ────────────────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN: GET /api/admin/stats/users/active → 200")
    @WithMockUser(roles = "ADMIN")
    void getActiveUsers_asAdmin_returns200() throws Exception {
        given(adminStatsService.getActiveUsers())
                .willReturn(Map.of("activeUsers", 10L));

        mockMvc.perform(get("/api/admin/stats/users/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeUsers").value(10));
    }

    @Test
    @DisplayName("USER: GET /api/admin/stats/users/active → 403")
    @WithMockUser(roles = "USER")
    void getActiveUsers_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/stats/users/active"))
                .andExpect(status().isForbidden());
    }

    // ── getTop10Stocks ────────────────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN: GET /api/admin/stats/stocks/top10 → 200")
    @WithMockUser(roles = "ADMIN")
    void getTop10Stocks_asAdmin_returns200() throws Exception {
        given(adminStatsService.getTop10Stocks())
                .willReturn(List.of(Map.of("stockCode", "AAPL", "stockName", "Apple", "count", 5L)));

        mockMvc.perform(get("/api/admin/stats/stocks/top10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stockCode").value("AAPL"));
    }

    @Test
    @DisplayName("USER: GET /api/admin/stats/stocks/top10 → 403")
    @WithMockUser(roles = "USER")
    void getTop10Stocks_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/stats/stocks/top10"))
                .andExpect(status().isForbidden());
    }

    // ── getSectorDistribution ─────────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN: GET /api/admin/stats/sectors → 200")
    @WithMockUser(roles = "ADMIN")
    void getSectorDistribution_asAdmin_returns200() throws Exception {
        given(adminStatsService.getSectorDistribution())
                .willReturn(List.of(Map.of("sector", "IT", "label", "IT", "count", 3L, "weight", 100.0)));

        mockMvc.perform(get("/api/admin/stats/sectors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sector").value("IT"));
    }

    @Test
    @DisplayName("USER: GET /api/admin/stats/sectors → 403")
    @WithMockUser(roles = "USER")
    void getSectorDistribution_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/stats/sectors"))
                .andExpect(status().isForbidden());
    }

    // ── getDividendAverages ───────────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN: GET /api/admin/stats/dividends/average → 200")
    @WithMockUser(roles = "ADMIN")
    void getDividendAverages_asAdmin_returns200() throws Exception {
        given(adminStatsService.getDividendAverages())
                .willReturn(Map.of("bySector", List.of(), "byStock", List.of()));

        mockMvc.perform(get("/api/admin/stats/dividends/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bySector").isArray());
    }

    @Test
    @DisplayName("USER: GET /api/admin/stats/dividends/average → 403")
    @WithMockUser(roles = "USER")
    void getDividendAverages_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/stats/dividends/average"))
                .andExpect(status().isForbidden());
    }
}
