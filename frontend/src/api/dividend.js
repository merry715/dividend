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
  useMock ? mockConfirmDividend(id, body) : api.patch(`/dividends/${id}`, body)

export const generateDividends = (year) =>
  useMock ? mockGenerateDividends(year) : api.post('/dividends/generate', { year })
