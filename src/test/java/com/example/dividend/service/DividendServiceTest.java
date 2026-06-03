package com.example.dividend.service;

import com.example.dividend.dto.request.DividendConfirmRequest;
import com.example.dividend.entity.Dividend;
import com.example.dividend.entity.Stock;
import com.example.dividend.entity.User;
import com.example.dividend.repository.DividendRepository;
import com.example.dividend.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DividendServiceTest {

    @Mock DividendRepository dividendRepository;
    @Mock StockRepository    stockRepository;

    @InjectMocks DividendService dividendService;

    private static final Long USER_ID  = 1L;
    private static final Long STOCK_ID = 10L;

    // ── helpers ──────────────────────────────────────────────────────────────

    private Stock buildStock(String cycle, int quantity, Integer perShare) {
        Stock s = new Stock();
        s.setDividendCycle(cycle);
        s.setQuantity(quantity);
        s.setExpectedDividendPerShare(perShare);
        return s;
    }

    private Dividend buildDividend(String status, BigDecimal expected, BigDecimal confirmed, int year, int month) {
        Dividend d = new Dividend();
        d.setUserId(USER_ID);
        d.setStockId(STOCK_ID);
        d.setYear(year);
        d.setMonth(month);
        d.setStatus(status);
        d.setExpectedAmount(expected);
        if (confirmed != null) d.setConfirmedAmount(confirmed);
        return d;
    }

    // ── [1] generateExpectedDividends ────────────────────────────────────────

    @Test
    @DisplayName("ANNUAL 주기 → 12월에만 1건 생성")
    void generate_annual_createsOneRecord() {
        Stock stock = buildStock("ANNUAL", 10, 500);
        given(stockRepository.findByIdAndUser_Id(STOCK_ID, USER_ID)).willReturn(Optional.of(stock));
        given(dividendRepository.existsByUserIdAndStockIdAndYearAndMonth(any(), any(), anyInt(), anyInt()))
                .willReturn(false);
        given(dividendRepository.save(any(Dividend.class))).willAnswer(inv -> inv.getArgument(0));

        List<Dividend> result = dividendService.generateExpectedDividends(USER_ID, STOCK_ID, 2025);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMonth()).isEqualTo(12);
        assertThat(result.get(0).getExpectedAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000)); // 500 * 10
    }

    @Test
    @DisplayName("QUARTERLY 주기 → 3, 6, 9, 12월 4건 생성")
    void generate_quarterly_createsFourRecords() {
        Stock stock = buildStock("QUARTERLY", 5, 1000);
        given(stockRepository.findByIdAndUser_Id(STOCK_ID, USER_ID)).willReturn(Optional.of(stock));
        given(dividendRepository.existsByUserIdAndStockIdAndYearAndMonth(any(), any(), anyInt(), anyInt()))
                .willReturn(false);
        given(dividendRepository.save(any(Dividend.class))).willAnswer(inv -> inv.getArgument(0));

        List<Dividend> result = dividendService.generateExpectedDividends(USER_ID, STOCK_ID, 2025);

        assertThat(result).hasSize(4);
        assertThat(result).extracting(Dividend::getMonth)
                .containsExactlyInAnyOrder(3, 6, 9, 12);
    }

    @Test
    @DisplayName("MONTHLY 주기 → 12건 생성")
    void generate_monthly_createsTwelveRecords() {
        Stock stock = buildStock("MONTHLY", 3, 200);
        given(stockRepository.findByIdAndUser_Id(STOCK_ID, USER_ID)).willReturn(Optional.of(stock));
        given(dividendRepository.existsByUserIdAndStockIdAndYearAndMonth(any(), any(), anyInt(), anyInt()))
                .willReturn(false);
        given(dividendRepository.save(any(Dividend.class))).willAnswer(inv -> inv.getArgument(0));

        List<Dividend> result = dividendService.generateExpectedDividends(USER_ID, STOCK_ID, 2025);

        assertThat(result).hasSize(12);
    }

    @Test
    @DisplayName("이미 존재하는 월 → 건너뜀")
    void generate_skipsDuplicateMonth() {
        Stock stock = buildStock("QUARTERLY", 10, 500);
        given(stockRepository.findByIdAndUser_Id(STOCK_ID, USER_ID)).willReturn(Optional.of(stock));
        // 3월만 이미 존재
        given(dividendRepository.existsByUserIdAndStockIdAndYearAndMonth(USER_ID, STOCK_ID, 2025, 3))
                .willReturn(true);
        given(dividendRepository.existsByUserIdAndStockIdAndYearAndMonth(USER_ID, STOCK_ID, 2025, 6))
                .willReturn(false);
        given(dividendRepository.existsByUserIdAndStockIdAndYearAndMonth(USER_ID, STOCK_ID, 2025, 9))
                .willReturn(false);
        given(dividendRepository.existsByUserIdAndStockIdAndYearAndMonth(USER_ID, STOCK_ID, 2025, 12))
                .willReturn(false);
        given(dividendRepository.save(any(Dividend.class))).willAnswer(inv -> inv.getArgument(0));

        List<Dividend> result = dividendService.generateExpectedDividends(USER_ID, STOCK_ID, 2025);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(Dividend::getMonth).doesNotContain(3);
    }

    @Test
    @DisplayName("수량 0 → 빈 리스트 반환")
    void generate_zeroQuantity_returnsEmpty() {
        Stock stock = buildStock("ANNUAL", 0, 500);
        given(stockRepository.findByIdAndUser_Id(STOCK_ID, USER_ID)).willReturn(Optional.of(stock));

        List<Dividend> result = dividendService.generateExpectedDividends(USER_ID, STOCK_ID, 2025);

        assertThat(result).isEmpty();
        verify(dividendRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 종목 → 404")
    void generate_stockNotFound_throws404() {
        given(stockRepository.findByIdAndUser_Id(STOCK_ID, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> dividendService.generateExpectedDividends(USER_ID, STOCK_ID, 2025))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("종목을 찾을 수 없습니다");
    }

    // ── [2] confirm ──────────────────────────────────────────────────────────

    private DividendConfirmRequest confirmReq(long amount, LocalDate date) {
        // DividendConfirmRequest has no-args constructor + private fields
        // Use reflection-free approach via a helper subclass is not possible without Lombok,
        // so we use a real object via JSON-deserialization-style mock
        DividendConfirmRequest req = mock(DividendConfirmRequest.class);
        given(req.getConfirmedAmount()).willReturn(amount);
        given(req.getPaymentDate()).willReturn(date);
        return req;
    }

    @Test
    @DisplayName("EXPECTED → CONFIRMED 정상 전환")
    void confirm_success() {
        Dividend dividend = buildDividend("EXPECTED", BigDecimal.valueOf(5000), null, 2025, 3);
        given(dividendRepository.findByIdAndUserId(1L, USER_ID)).willReturn(Optional.of(dividend));
        given(dividendRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        DividendConfirmRequest req = confirmReq(4800L, LocalDate.of(2025, 3, 15));

        Dividend result = dividendService.confirm(1L, USER_ID, req);

        assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        assertThat(result.getConfirmedAmount()).isEqualByComparingTo(BigDecimal.valueOf(4800));
        assertThat(result.getPaymentDate()).isEqualTo(LocalDate.of(2025, 3, 15));
    }

    @Test
    @DisplayName("배당 없음 → 404")
    void confirm_notFound_throws404() {
        given(dividendRepository.findByIdAndUserId(99L, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> dividendService.confirm(99L, USER_ID, mock(DividendConfirmRequest.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("이미 CONFIRMED → 400")
    void confirm_alreadyConfirmed_throws400() {
        Dividend dividend = buildDividend("CONFIRMED", BigDecimal.valueOf(5000), BigDecimal.valueOf(5000), 2025, 3);
        given(dividendRepository.findByIdAndUserId(1L, USER_ID)).willReturn(Optional.of(dividend));

        assertThatThrownBy(() -> dividendService.confirm(1L, USER_ID, mock(DividendConfirmRequest.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 확정");
    }

    // ── [3] getMonthly ───────────────────────────────────────────────────────

    @Test
    @DisplayName("월별 조회 → 12개 월 항목 반환, EXPECTED/CONFIRMED 합산")
    void getMonthly_returnsAllMonths() {
        Dividend expected = buildDividend("EXPECTED", BigDecimal.valueOf(3000), null, 2025, 3);
        Dividend confirmed = buildDividend("CONFIRMED", BigDecimal.valueOf(5000), BigDecimal.valueOf(4800), 2025, 6);
        given(dividendRepository.findByUserIdAndYear(USER_ID, 2025)).willReturn(List.of(expected, confirmed));

        List<Map<String, Object>> result = dividendService.getMonthly(USER_ID, 2025);

        assertThat(result).hasSize(12);

        Map<String, Object> march = result.get(2); // index 2 = month 3
        assertThat(march.get("month")).isEqualTo(3);
        assertThat(march.get("expectedAmount")).isEqualTo(3000L);
        assertThat(march.get("confirmedAmount")).isEqualTo(0L);

        Map<String, Object> june = result.get(5); // index 5 = month 6
        assertThat(june.get("month")).isEqualTo(6);
        assertThat(june.get("confirmedAmount")).isEqualTo(4800L);
        assertThat(june.get("expectedAmount")).isEqualTo(5000L);
    }

    // ── [4] getAnnual ────────────────────────────────────────────────────────

    @Test
    @DisplayName("연간 배당 합계 → EXPECTED+CONFIRMED 모두 포함")
    void getAnnual_sumsBothStatuses() {
        Dividend exp  = buildDividend("EXPECTED",  BigDecimal.valueOf(3000), null, 2025, 3);
        Dividend conf = buildDividend("CONFIRMED", BigDecimal.valueOf(5000), BigDecimal.valueOf(4800), 2025, 6);
        given(dividendRepository.findByUserIdAndYear(USER_ID, 2025)).willReturn(List.of(exp, conf));

        Map<String, Object> result = dividendService.getAnnual(USER_ID, 2025);

        assertThat(result.get("year")).isEqualTo(2025);
        // EXPECTED: 3000, CONFIRMED: confirmedAmount 4800
        assertThat(result.get("totalExpectedAmount")).isEqualTo(7800L);
    }

    // ── [5] getCumulative ────────────────────────────────────────────────────

    @Test
    @DisplayName("누적 배당 → 리포지토리 집계 값 반환")
    void getCumulative_returnsAggregation() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{10000L, 25000L});
        given(dividendRepository.findCumulativeAggregation(USER_ID)).willReturn(rows);

        Map<String, Object> result = dividendService.getCumulative(USER_ID);

        assertThat(result.get("totalConfirmedAmount")).isEqualTo(10000L);
        assertThat(result.get("totalExpectedAmount")).isEqualTo(25000L);
    }

    @Test
    @DisplayName("누적 배당 → null 집계 값은 0으로 처리")
    void getCumulative_nullHandled() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{null, null});
        given(dividendRepository.findCumulativeAggregation(USER_ID)).willReturn(rows);

        Map<String, Object> result = dividendService.getCumulative(USER_ID);

        assertThat(result.get("totalConfirmedAmount")).isEqualTo(0L);
        assertThat(result.get("totalExpectedAmount")).isEqualTo(0L);
    }

    // ── [6] getYearly ────────────────────────────────────────────────────────

    @Test
    @DisplayName("연도별 배당 → 연도, confirmedAmount, expectedAmount 포함")
    void getYearly_mapsRows() {
        given(dividendRepository.findYearlyAggregation(USER_ID))
                .willReturn(List.of(
                        new Object[]{2024, 8000L, 10000L},
                        new Object[]{2025, 3000L, 7000L}
                ));

        List<Map<String, Object>> result = dividendService.getYearly(USER_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("year")).isEqualTo(2024);
        assertThat(result.get(0).get("confirmedAmount")).isEqualTo(8000L);
        assertThat(result.get(0).get("expectedAmount")).isEqualTo(10000L);
        assertThat(result.get(1).get("year")).isEqualTo(2025);
    }
}
