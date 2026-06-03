import api from './axios'
import {
  mockGetAnnual,
  mockGetCumulative,
  mockGetMonthly,
  mockGetYearly,
  mockGetByStock,
  mockGetDividends,
  mockConfirmDividend,
  mockGenerateDividends,
} from './mockDividend'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

export const getAnnual = (year) =>
  useMock ? mockGetAnnual(year) : api.get('/dividends/annual', { params: { year } })

export const getCumulative = () =>
  useMock ? mockGetCumulative() : api.get('/dividends/cumulative')

export const getMonthly = (year) =>
  useMock ? mockGetMonthly(year) : api.get('/dividends/monthly', { params: { year } })

export const getYearly = () =>
  useMock ? mockGetYearly() : api.get('/dividends/yearly')

export const getByStock = () =>
  useMock ? mockGetByStock() : api.get('/dividends/by-stock')

export const getDividends = () =>
  useMock ? mockGetDividends() : api.get('/dividends')

export const confirmDividend = (id, body) =>
  useMock ? mockConfirmDividend(id, body) : api.patch(`/dividends/${id}/confirm`, body)

export const generateDividends = (stockId, year) =>
  useMock ? mockGenerateDividends(year) : api.post('/dividends/generate', { stockId, year })

export const getByStockYear = (year) =>
  api.get('/dividends/by-stock', { params: { year } })

export const confirmWithAutoGenerate = (body) =>
  api.post('/dividends/confirm-with-auto-generate', body)

export const updateDividend = (dividendId, body) =>
  api.patch(`/dividends/${dividendId}`, body)

export const getStocksForConfirm = (year) =>
  api.get('/dividends/stocks-for-confirm', { params: { year } })
