import api from './axios';
import { mockLogin, mockSignup, mockLogout, mockGetMe } from './mockAuth';

const useMock = import.meta.env.VITE_USE_MOCK === 'true';

export const login = (email, password) =>
  useMock ? mockLogin(email, password) : api.post('/auth/login', { email, password });

export const signup = (name, email, password) =>
  useMock ? mockSignup(name, email, password) : api.post('/auth/signup', { name, email, password });

export const logout = () =>
  useMock ? mockLogout() : api.post('/auth/logout', { refreshToken: localStorage.getItem('refreshToken') });

export const getMe = () =>
  useMock ? mockGetMe() : api.get('/auth/me');
