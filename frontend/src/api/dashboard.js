import api from './axios';

export const getDashboardSummary = () => api.get('/analysis/dashboard');
export const getStockWeight = () => api.get('/analysis/stock-weight');
export const getMonthlyDividends = () => api.get('/dividends/monthly');
