import api from './axios';

export const getTransactions = (params) => api.get('/transactions', { params });
export const createTransaction = (body) => api.post('/transactions', body);
export const updateTransaction = (id, body) => api.patch(`/transactions/${id}`, body);
export const deleteTransaction = (id) => api.delete(`/transactions/${id}`);
export const getTransactionsByStock = (stockId) => api.get(`/transactions/stocks/${stockId}`);
export const getTransactionsSummary = () => api.get('/transactions/summary');
export const getTransactionsChart = (year) => api.get('/transactions/chart', { params: { year } });
