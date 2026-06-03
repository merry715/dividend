import api from './axios';

const adminGet = (path) => api.get(path, { baseURL: '' });

export const getUsers = () => adminGet('/api/admin/users');
export const getActiveNow = () => adminGet('/api/admin/stats/active-now');
export const getUserStats = () => adminGet('/api/admin/stats/users');
export const getActiveUsers = () => adminGet('/api/admin/stats/users/active');
export const getStockStats = () => adminGet('/api/admin/stats/stocks');
export const getTopStocks = () => adminGet('/api/admin/stats/stocks/top10');
export const getSectorWeight = () => adminGet('/api/admin/stats/sectors');
export const getAvgInvestment = () => adminGet('/api/admin/stats/stocks/investment');
export const getStockDividend = () => adminGet('/api/admin/stats/stocks/dividend');
export const getAvgDividend = () => adminGet('/api/admin/stats/dividends/average');
export const getSectorDividend = () => adminGet('/api/admin/stats/sector/dividend');
export const getSectorInvestment = () => adminGet('/api/admin/stats/sector/investment');
