const delay = (ms = 300) => new Promise((r) => setTimeout(r, ms))

// ── 지급 스케줄 ──────────────────────────────────────────────────────────────
// 삼성전자 Q(3/6/9/12)  Q1~Q3: 18,000 / Q4: 36,000 = 90,000
// NVDA     Q(3/6/9/12)  17,500×4               = 70,000
// SK하이닉스 연간(4)     40,000×1               = 40,000
// AAPL     Q(2/5/8/11)  10,000×4               = 40,000
// TSLA     Q(1/4/7/10)   8,750×4               = 35,000
// ─────────────────────────────────────────────────────────────────────────────
// 기준일: 2026-05-24 → 1~5월 전부 확정
// confirmed 합계: 8,750+10,000+35,500+48,750+10,000 = 113,000
// ─────────────────────────────────────────────────────────────────────────────

export const mockGetAnnual = async () => {
  await delay()
  return {
    data: {
      data: {
        year:           2026,
        totalExpected:  275_000,
        totalConfirmed: 113_000,
      },
    },
  }
}

// 누적: 2023(140K)+2024(190K)+2025(245K)+2026확정(113K) = 688,000
export const mockGetCumulative = async () => {
  await delay()
  return {
    data: {
      data: {
        cumulativeAmount: 688_000,
        confirmedAmount:  113_000,
        expectedAmount:   162_000,
      },
    },
  }
}

export const mockGetMonthly = async () => {
  await delay()
  return {
    data: {
      data: [
        { month: 1,  expectedAmount:  8_750, confirmedAmount:  8_750 },  // TSLA Q1 확정
        { month: 2,  expectedAmount: 10_000, confirmedAmount: 10_000 },  // AAPL Q1 확정
        { month: 3,  expectedAmount: 35_500, confirmedAmount: 35_500 },  // 삼성Q1+NVDA Q1 확정
        { month: 4,  expectedAmount: 48_750, confirmedAmount: 48_750 },  // SK+TSLA Q2 확정
        { month: 5,  expectedAmount: 10_000, confirmedAmount: 10_000 },  // AAPL Q2 확정
        { month: 6,  expectedAmount: 35_500, confirmedAmount:      0 },  // 삼성Q2+NVDA Q2
        { month: 7,  expectedAmount:  8_750, confirmedAmount:      0 },  // TSLA Q3
        { month: 8,  expectedAmount: 10_000, confirmedAmount:      0 },  // AAPL Q3
        { month: 9,  expectedAmount: 35_500, confirmedAmount:      0 },  // 삼성Q3+NVDA Q3
        { month: 10, expectedAmount:  8_750, confirmedAmount:      0 },  // TSLA Q4
        { month: 11, expectedAmount: 10_000, confirmedAmount:      0 },  // AAPL Q4
        { month: 12, expectedAmount: 53_500, confirmedAmount:      0 },  // 삼성Q4+NVDA Q4
      ],
    },
  }
}

export const mockGetYearly = async () => {
  await delay()
  return {
    data: {
      data: [
        { year: 2023, totalAmount: 140_000 },
        { year: 2024, totalAmount: 190_000 },
        { year: 2025, totalAmount: 245_000 },
        { year: 2026, totalAmount: 275_000 },
      ],
    },
  }
}

export const mockGetByStock = async () => {
  await delay()
  return {
    data: {
      data: [
        { stockId: 1, stockName: '삼성전자',   lastYearDividendPerShare:  2_700, paymentMonths: [3, 6, 9, 12], exDividendDate: '2026-03-28', status: 'CONFIRMED', expectedDividend:  90_000 },
        { stockId: 2, stockName: 'NVDA',       lastYearDividendPerShare: 35_000, paymentMonths: [3, 6, 9, 12], exDividendDate: '2026-03-28', status: 'CONFIRMED', expectedDividend:  70_000 },
        { stockId: 3, stockName: 'SK하이닉스', lastYearDividendPerShare:  6_700, paymentMonths: [4],           exDividendDate: '2026-03-28', status: 'CONFIRMED', expectedDividend:  40_000 },
        { stockId: 4, stockName: 'AAPL',       lastYearDividendPerShare: 13_300, paymentMonths: [2, 5, 8, 11], exDividendDate: '2026-02-07', status: 'CONFIRMED', expectedDividend:  40_000 },
        { stockId: 5, stockName: 'TSLA',       lastYearDividendPerShare: 17_500, paymentMonths: [1, 4, 7, 10], exDividendDate: '2026-01-10', status: 'CONFIRMED', expectedDividend:  35_000 },
      ],
    },
  }
}

// CONFIRMED 합계 = 8,750+10,000+18,000+17,500+40,000+8,750+10,000 = 113,000
export const mockGetDividends = async () => {
  await delay()
  return {
    data: {
      data: [
        { id: 1, stockName: 'TSLA',       paymentMonth: 1,  exDividendDate: '2026-01-10', paymentDate: '2026-01-17', expectedDividend:  8_750, confirmedDividend:  8_750, status: 'CONFIRMED', year: 2026 },
        { id: 2, stockName: 'AAPL',       paymentMonth: 2,  exDividendDate: '2026-02-07', paymentDate: '2026-02-14', expectedDividend: 10_000, confirmedDividend: 10_000, status: 'CONFIRMED', year: 2026 },
        { id: 3, stockName: '삼성전자',   paymentMonth: 3,  exDividendDate: '2026-03-28', paymentDate: '2026-03-28', expectedDividend: 18_000, confirmedDividend: 18_000, status: 'CONFIRMED', year: 2026 },
        { id: 4, stockName: 'NVDA',       paymentMonth: 3,  exDividendDate: '2026-03-28', paymentDate: '2026-03-28', expectedDividend: 17_500, confirmedDividend: 17_500, status: 'CONFIRMED', year: 2026 },
        { id: 5, stockName: 'SK하이닉스', paymentMonth: 4,  exDividendDate: '2026-03-28', paymentDate: '2026-04-15', expectedDividend: 40_000, confirmedDividend: 40_000, status: 'CONFIRMED', year: 2026 },
        { id: 6, stockName: 'TSLA',       paymentMonth: 4,  exDividendDate: '2026-04-10', paymentDate: '2026-04-17', expectedDividend:  8_750, confirmedDividend:  8_750, status: 'CONFIRMED', year: 2026 },
        { id: 7, stockName: 'AAPL',       paymentMonth: 5,  exDividendDate: '2026-05-08', paymentDate: '2026-05-15', expectedDividend: 10_000, confirmedDividend: 10_000, status: 'CONFIRMED', year: 2026 },
        { id: 8, stockName: '삼성전자',   paymentMonth: 6,  exDividendDate: '2026-06-28', paymentDate: null,         expectedDividend: 18_000, confirmedDividend: null,   status: 'EXPECTED',  year: 2026 },
        { id: 9, stockName: 'NVDA',       paymentMonth: 6,  exDividendDate: '2026-06-28', paymentDate: null,         expectedDividend: 17_500, confirmedDividend: null,   status: 'EXPECTED',  year: 2026 },
      ],
    },
  }
}

export const mockConfirmDividend = async (id, body) => {
  await delay(300)
  return {
    data: {
      data: {
        id,
        status: 'CONFIRMED',
        confirmedDividend: body.confirmedDividend,
      },
    },
  }
}

export const mockGenerateDividends = async (_year) => {
  await delay(500)
  return {
    data: {
      data: { generatedCount: 9 },
      message: '예상 배당 9건이 생성되었습니다',
    },
  }
}
