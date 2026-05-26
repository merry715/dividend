import api from './axios';

export const getUsers = () => api.get('/admin/users');
export const getActiveNow = () => api.get('/admin/stats/active-now');
export const getUserStats = () => api.get('/admin/stats/users');
export const getActiveUsers = () => api.get('/admin/stats/users/active');
export const getStockStats = () => api.get('/admin/stats/stocks');
export const getTopStocks = () => api.get('/admin/stats/stocks/top');
export const getSectorWeight = () => api.get('/admin/stats/stocks/sector');
export const getAvgInvestment = () => api.get('/admin/stats/stocks/investment');
export const getStockDividend = () => api.get('/admin/stats/stocks/dividend');
export const getAvgDividend = () => api.get('/admin/stats/dividends/average');
export const getSectorDividend = () => api.get('/admin/stats/sector/dividend');
export const getSectorInvestment = () => api.get('/admin/stats/sector/investment');
