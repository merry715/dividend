const delay = (ms = 300) => new Promise((r) => setTimeout(r, ms))

export const mockGetSummary = async () => {
  await delay()
  return {
    data: {
      data: {
        totalInvestment:    7_240_000,
        evaluationAmount:   8_150_000,
        evaluationProfit:     910_000,
        dividendYield:            3.8,
        cumulativeDividend:   688_000,
      },
    },
  }
}

export const mockGetStockWeight = async () => {
  await delay()
  return {
    data: {
      data: [
        { stockName: '삼성전자',   investmentAmount: 2_353_000, weight: 32.5 },
        { stockName: 'NVDA',       investmentAmount: 1_795_520, weight: 24.8 },
        { stockName: 'SK하이닉스', investmentAmount: 1_317_680, weight: 18.2 },
        { stockName: 'AAPL',       investmentAmount: 1_020_840, weight: 14.1 },
        { stockName: 'TSLA',       investmentAmount:   752_960, weight: 10.4 },
      ],
    },
  }
}

export const mockGetSectorWeight = async () => {
  await delay()
  return {
    data: {
      data: [
        { sector: '반도체', investmentAmount: 3_670_680, weight: 50.7 },
        { sector: '기술',   investmentAmount: 2_816_360, weight: 38.9 },
        { sector: '자동차', investmentAmount:   752_960, weight: 10.4 },
      ],
    },
  }
}

export const mockGetDividendHistory = async () => {
  await delay()
  return {
    data: {
      data: [
        { year: 2023, totalDividend: 140_000, confirmedDividend: 140_000 },
        { year: 2024, totalDividend: 190_000, confirmedDividend: 190_000 },
        { year: 2025, totalDividend: 245_000, confirmedDividend: 245_000 },
        { year: 2026, totalDividend: 275_000, confirmedDividend: 113_000 },
      ],
    },
  }
}

export const mockGetGoal = async () => {
  await delay()
  return {
    data: {
      data: {
        id:               1,
        targetDividend:   1_000_000,
        expectedDividend:   275_000,
        achievementRate:       27.5,
        timeToGoal:              32,
      },
    },
  }
}

export const mockSaveGoal = async (body) => {
  await delay(300)
  const rate   = Math.min(100, Math.round((275_000 / body.targetDividend) * 100))
  const months = rate >= 100
    ? 0
    : Math.ceil((body.targetDividend - 275_000) / (275_000 / 12))
  return {
    data: {
      data: {
        id:               1,
        targetDividend:   body.targetDividend,
        expectedDividend:   275_000,
        achievementRate:       rate,
        timeToGoal:           months,
      },
    },
  }
}
