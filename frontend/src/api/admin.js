import axios from 'axios';

const adminApi = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

adminApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export const getUserStats = () => adminApi.get('/admin/stats/users');
export const getActiveUsers = () => adminApi.get('/admin/stats/users/active');
export const getTop10Stocks = () => adminApi.get('/admin/stats/stocks/top10');
export const getSectorDistribution = () => adminApi.get('/admin/stats/sectors');
export const getDividendAverages = () => adminApi.get('/admin/stats/dividends/average');
