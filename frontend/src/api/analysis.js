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
  useMock ? mockGetStockWeight() : api.get('/analysis/stock-weight')

export const getSectorWeight = () =>
  useMock ? mockGetSectorWeight() : api.get('/analysis/sector-weight')

export const getDividendHistory = () =>
  useMock ? mockGetDividendHistory() : api.get('/analysis/dividend-history')

export const getGoal = () =>
  useMock ? mockGetGoal() : api.get('/goals')

export const saveGoal = (targetDividend, goalId) => {
  if (useMock) return mockSaveGoal({ targetDividend })
  return goalId
    ? api.patch(`/goals/${goalId}`, { targetDividend })
    : api.post('/goals', { targetDividend, year: new Date().getFullYear() })
}
