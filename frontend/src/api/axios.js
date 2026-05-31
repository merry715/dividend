import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api/v1',
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
        const { data } = await axios.post(`${api.defaults.baseURL}/auth/refresh`, { refreshToken });
        if (!data.success || !data.data?.accessToken) {
          clearAuth();
          window.location.href = '/';
          return Promise.reject(error);
        }
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
    error.uiMessage = error.response?.data?.message
      || '일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.';
    return Promise.reject(error);
  }
);

export default api;
