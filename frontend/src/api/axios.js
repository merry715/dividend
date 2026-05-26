import axios from 'axios';

const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

const CLEAR_KEYS = ['accessToken', 'refreshToken', '_mockRole', '_mockName', 'userName', 'userRole'];
function clearAuth() {
  CLEAR_KEYS.forEach((k) => localStorage.removeItem(k));
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    if (error.response?.status === 401 && original && !original._retry) {
      original._retry = true;
      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) {
        clearAuth();
        window.location.href = '/';
        return Promise.reject(error);
      }
      try {
        const { data } = await axios.post('/api/v1/auth/refresh', { refreshToken });
        localStorage.setItem('accessToken', data.data.accessToken);
        if (data.data.refreshToken) {
          localStorage.setItem('refreshToken', data.data.refreshToken);
        }
        original.headers.Authorization = `Bearer ${data.data.accessToken}`;
        return api(original);
      } catch {
        clearAuth();
        window.location.href = '/';
        return Promise.reject(error);
      }
    }
    return Promise.reject(error);
  }
);

export default api;
