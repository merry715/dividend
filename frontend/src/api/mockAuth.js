const delay = (ms = 400) => new Promise((r) => setTimeout(r, ms));

const MOCK_USERS = [
  { email: 'test@test.com',  password: 'test1234',  name: '테스트유저', role: 'ROLE_USER'  },
  { email: 'admin@test.com', password: 'admin1234', name: '관리자',     role: 'ROLE_ADMIN' },
];

export const mockLogin = async (email, password) => {
  await delay();
  const user = MOCK_USERS.find((u) => u.email === email && u.password === password);
  if (!user) {
    const err = new Error('이메일 또는 비밀번호가 올바르지 않습니다.');
    err.response = { data: { message: '이메일 또는 비밀번호가 올바르지 않습니다.' } };
    throw err;
  }
  localStorage.setItem('_mockRole', user.role);
  localStorage.setItem('_mockName', user.name);
  return { data: { data: { accessToken: 'mock-access-token', refreshToken: 'mock-refresh-token' } } };
};

export const mockSignup = async (name, email, _password) => {
  await delay();
  if (MOCK_USERS.find((u) => u.email === email)) {
    const err = new Error('이미 사용 중인 이메일입니다.');
    err.response = { data: { message: '이미 사용 중인 이메일입니다.' } };
    throw err;
  }
  return { data: { data: { id: 99, email, name } } };
};

export const mockLogout = async () => {
  await delay(200);
  return { data: { data: null, message: '로그아웃 되었습니다' } };
};

export const mockGetMe = async () => {
  await delay(200);
  const role = localStorage.getItem('_mockRole') || 'ROLE_USER';
  const name = localStorage.getItem('_mockName') || '사용자';
  return { data: { data: { role, name } } };
};
