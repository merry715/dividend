import api from './axios'
import {
  mockGetSummary,
  mockGetStockWeight,
  mockGetSectorWeight,
  mockGetDividendHistory,
  mockGetGoal,
  mockSaveGoal,
} from './mockAnalysis'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

export const getSummary = () =>
  useMock ? mockGetSummary() : api.get('/analysis/summary')

export const getStockWeight = () =>
  useMock ? mockGetStockWeight() : api.get('/analysis/stock-weights')

export const getSectorWeight = () =>
  useMock ? mockGetSectorWeight() : api.get('/analysis/sector-weights')

export const getDividendHistory = () =>
  useMock ? mockGetDividendHistory() : api.get('/analysis/annual-dividends')

export const getGoal = () =>
  useMock ? mockGetGoal() : api.get('/analysis/goal-achievement')

export const saveGoal = (targetDividend) => {
  if (useMock) return mockSaveGoal({ targetDividend })
  return api.post('/analysis/goal', { targetDividend })
}
