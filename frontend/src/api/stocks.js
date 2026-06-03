import api from './axios';

export const getStocks = () => api.get('/stocks');
export const createStock = (body) => api.post('/stocks', body);
export const updateStock = (id, body) => api.patch(`/stocks/${id}`, body);
export const deleteStock = (id) => api.delete(`/stocks/${id}`);
export const searchStocks = (keyword) => api.get('/stocks/search', { params: { keyword } });
export const updateStockSector = (id, sector) => api.patch(`/stocks/${id}/sector`, { sector });
