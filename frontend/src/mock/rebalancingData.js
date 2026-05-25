export const GICS_SECTORS = [
  '에너지', '소재', '산업재', '경기소비재', '필수소비재',
  '헬스케어', '금융', '정보기술', '커뮤니케이션 서비스', '유틸리티', '부동산',
]

export const mockCurrentSectorWeights = [
  { sector: '정보기술',            current: 63 },
  { sector: '금융',                current: 5  },
  { sector: '헬스케어',            current: 32 },
  { sector: '에너지',              current: 0  },
  { sector: '소재',                current: 0  },
  { sector: '산업재',              current: 0  },
  { sector: '경기소비재',          current: 0  },
  { sector: '필수소비재',          current: 0  },
  { sector: '커뮤니케이션 서비스', current: 0  },
  { sector: '유틸리티',            current: 0  },
  { sector: '부동산',              current: 0  },
]

export const mockTargetSectorWeights = [
  { sector: '정보기술', target: 40 },
  { sector: '금융',     target: 30 },
  { sector: '헬스케어', target: 30 },
]

export const mockWatchlist = [
  { id: 1, name: 'KB금융',      sector: '금융',    currentPrice: 57400,  targetPrice: 65000,  alertDropRate: 10 },
  { id: 2, name: '카카오',      sector: '정보기술', currentPrice: 41200,  targetPrice: 45000,  alertDropRate: 8  },
  { id: 3, name: 'POSCO홀딩스', sector: '소재',    currentPrice: 360000, targetPrice: 380000, alertDropRate: 5  },
]

export const mockWatchlistPrices = [
  { name: 'KB금융',      currentPrice: 57400   },
  { name: '카카오',      currentPrice: 41200   },
  { name: 'POSCO홀딩스', currentPrice: 360000  },
]

export const mockWatchlistStatus = [
  { name: 'KB금융',      currentPrice: 57400,   targetPrice: 65000,  condition: '목표가 미달', status: '경고' },
  { name: '카카오',      currentPrice: 41200,   targetPrice: 45000,  condition: '목표가 미달', status: '경고' },
  { name: 'POSCO홀딩스', currentPrice: 360000,  targetPrice: 380000, condition: '목표가 미달', status: '적정' },
]

export const mockTotalAsset = 15200000
