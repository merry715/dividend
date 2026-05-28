# Dashboard API Connect — Design Spec
**Date:** 2026-05-26  
**Branch:** feature/api-connect-dashboard  
**Author:** 박민서 (Frontend)

---

## 1. 범위

배당 포트폴리오 관리 서비스의 프론트엔드에서 대시보드·종목·거래·관리자 페이지를 실제 백엔드 API에 연결한다. UI 뼈대 구현 + API 레이어 작성이 목표이며, 시각적 디자인은 이후 단계에서 입힌다.

### 생성/수정 파일

| 파일 | 작업 |
|---|---|
| `frontend/src/api/stocks.js` | 내용 채우기 |
| `frontend/src/api/transactions.js` | 신규 생성 |
| `frontend/src/api/dashboard.js` | 신규 생성 |
| `frontend/src/api/admin.js` | 신규 생성 |
| `frontend/src/pages/DashboardPage.jsx` | 신규 생성 |
| `frontend/src/pages/StocksPage.jsx` | 신규 생성 |
| `frontend/src/pages/TransactionsPage.jsx` | 신규 생성 |
| `frontend/src/pages/AdminPage.jsx` | 신규 생성 |
| `frontend/src/App.jsx` | import 4개 추가 + 라우트 4개 교체만 |

### 절대 수정 금지 파일

`axios.js` / `auth.js` / `mockAuth.js` / `LoginPage.jsx` / `SignupPage.jsx` / `Sidebar.jsx` / `Topbar.jsx` / `main.jsx`  
App.jsx 안의 `/dividends`, `/analysis`, `/rebalancing` 라우트 및 기존 코드

---

## 2. API 레이어 설계

### 공통 원칙

- `import api from './axios'` 사용 — interceptor(토큰 주입, 401 refresh)는 이미 완비됨
- **named export만** 사용, default export 없음
- 함수는 **axios promise를 그대로 반환** — 페이지에서 `res.data.data`로 실제 데이터 접근
- mock 분기 없음 (auth 전용 패턴, 나머지 API는 서버 직접 호출)

### stocks.js

```js
import api from './axios';

export const getStocks = () => api.get('/stocks');
export const createStock = (body) => api.post('/stocks', body);
export const updateStock = (id, body) => api.patch(`/stocks/${id}`, body);
export const deleteStock = (id) => api.delete(`/stocks/${id}`);
export const searchStocks = (keyword) => api.get('/stocks/search', { params: { keyword } });
export const updateStockSector = (id, sector) => api.patch(`/stocks/${id}/sector`, { sector });
```

### transactions.js

```js
import api from './axios';

export const getTransactions = (params) => api.get('/transactions', { params });
export const createTransaction = (body) => api.post('/transactions', body);
export const updateTransaction = (id, body) => api.patch(`/transactions/${id}`, body);
export const deleteTransaction = (id) => api.delete(`/transactions/${id}`);
export const getTransactionsByStock = (stockId) => api.get(`/transactions/stocks/${stockId}`);
export const getTransactionsSummary = () => api.get('/transactions/summary');
export const getTransactionsChart = (year) => api.get('/transactions/chart', { params: { year } });
```

### dashboard.js

```js
import api from './axios';

export const getDashboardSummary = () => api.get('/analysis/dashboard');
export const getStockWeight = () => api.get('/analysis/stock-weight');
export const getMonthlyDividends = () => api.get('/dividends/monthly');
```

### admin.js

```js
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
```

---

## 3. 페이지 컴포넌트 설계

### 공통 뼈대 패턴

```jsx
const [data, setData] = useState(null);
const [isLoading, setIsLoading] = useState(false);
const [error, setError] = useState(null);

useEffect(() => {
  const fetch = async () => {
    setIsLoading(true);
    try {
      const res = await getSomething();
      console.log(res.data.data);
      setData(res.data.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  };
  fetch();
}, []); // 의존성 배열은 각 페이지 필터 상태에 맞게 정확히 설정
```

### DashboardPage

**상태:** `summary`, `stockWeights`, `monthlyDividends`, `isLoading`, `error`

**호출:** `Promise.all([getDashboardSummary(), getStockWeight(), getMonthlyDividends()])` 병렬 호출

**방어 처리:**
- `summary.goalAchievementRate ?? null` — 목표 미설정 시 null 허용
- `summary.targetDividend ?? null` — 동일
- monthlyDividends: 12-slot 배열 초기화 후 `item.month - 1` 인덱스로 매핑

### StocksPage

**상태:** `stocks`, `searchResults`, `searchKeyword`, `isLoading`, `error`

**호출:**
- 마운트: `getStocks()`
- keyword 변경: `useEffect` 내부 300ms debounce (`setTimeout`/`clearTimeout`) → `searchStocks(keyword)`
- DELETE: `deleteStock(id)` 후 `getStocks()` 재호출로 목록 즉시 갱신

**debounce 구현 (외부 라이브러리 없이):**
```js
useEffect(() => {
  if (!searchKeyword.trim()) return;
  const timer = setTimeout(async () => {
    const res = await searchStocks(searchKeyword);
    setSearchResults(res.data.data);
  }, 300);
  return () => clearTimeout(timer);
}, [searchKeyword]);
```

### TransactionsPage

**상태:** `transactions`, `summary`, `chartData`, `filterYear`, `filterType`, `isLoading`, `error`

**호출:**
- `filterYear`, `filterType` 변경 시 `getTransactions({ year: filterYear, type: filterType })` 재호출
- `getTransactionsSummary()` 마운트 시 1회
- `getTransactionsChart(filterYear)` year 변경 시 재호출
- chartData: 12-slot 배열 초기화 후 month 기준 매핑
- DELETE: `deleteTransaction(id)` 후 `getTransactions(...)` 재호출

### AdminPage

**상태:** `users`, `stats` (객체로 묶음), `isLoading`, `error`, `isForbidden`

**호출:** `Promise.all` 로 12개 엔드포인트 병렬 호출

**403 방어:**
```js
catch (err) {
  if (err.response?.status === 403) {
    setIsForbidden(true);
  } else {
    setError(err.message);
  }
}
```
`isForbidden === true`이면 `<p>권한이 없습니다</p>` 표시

---

## 4. App.jsx 변경 범위

```jsx
// 추가할 import (SignupPage import 다음에)
import DashboardPage    from './pages/DashboardPage'
import StocksPage       from './pages/StocksPage'
import TransactionsPage from './pages/TransactionsPage'
import AdminPage        from './pages/AdminPage'

// 교체할 라우트 4개만
<Route path="/dashboard"    element={<DashboardPage />} />
<Route path="/stocks"       element={<StocksPage />} />
<Route path="/transactions" element={<TransactionsPage />} />
<Route path="/admin"        element={<AdminPage />} />
```

`/dividends`, `/analysis`, `/rebalancing` 라우트는 `<div />`로 유지.

---

## 5. 방어 처리 체크리스트

| # | 케이스 | 처리 |
|---|---|---|
| 1 | `goalAchievementRate`, `targetDividend` null | `?? null` 기본값, 렌더 시 조건부 표시 |
| 2 | 월별 누락 달 (monthly, chart) | 12-slot 배열 init → month-1 인덱스 매핑 |
| 3 | AdminPage 403 | `isForbidden` 상태 분리 → "권한이 없습니다" |
| 4 | search 타이핑마다 API 폭발 | 300ms debounce (setTimeout/clearTimeout) |
| 5 | DELETE 후 목록 stale | 삭제 성공 후 list getter 재호출 |
