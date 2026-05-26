import { logout } from '../api/auth';

const KEYS = ['accessToken', 'refreshToken', '_mockRole', '_mockName', 'userName', 'userRole'];

export async function doLogout(navigate) {
  try { await logout(); } catch { /* 서버 에러 무시 */ }
  KEYS.forEach((key) => localStorage.removeItem(key));
  navigate('/');
}
