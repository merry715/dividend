# Dashboard API Connect Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 대시보드·종목·거래·관리자 페이지의 API 레이어와 뼈대 컴포넌트를 구현하여 백엔드와 연결한다.

**Architecture:** `api/` 폴더에 named export 함수를 추가하고, 각 페이지 컴포넌트는 useState/useEffect 뼈대로만 구성한다. axios.js interceptor가 토큰 주입과 refresh를 이미 처리하므로 API 함수는 단순 래퍼로 작성한다.

**Tech Stack:** React 19, Vite, axios, react-router-dom 7

---

## 파일 맵

| 파일 | 작업 |
|---|---|
| `frontend/src/api/stocks.js` | 내용 채우기 (현재 빈 파일) |
| `frontend/src/api/transactions.js` | 신규 생성 |
| `frontend/src/api/dashboard.js` | 신규 생성 |
| `frontend/src/api/admin.js` | 신규 생성 |
| `frontend/src/pages/DashboardPage.jsx` | 신규 생성 |
| `frontend/src/pages/StocksPage.jsx` | 신규 생성 |
| `frontend/src/pages/TransactionsPage.jsx` | 신규 생성 |
| `frontend/src/pages/AdminPage.jsx` | 신규 생성 |
| `frontend/src/App.jsx` | import 4개 추가 + 라우트 4개 교체만 |

---

### Task 1: API 레이어 4개 파일 작성

**Files:**
- Write: `frontend/src/api/stocks.js`
- Create: `frontend/src/api/transactions.js`
- Create: `frontend/src/api/dashboard.js`
- Create: `frontend/src/api/admin.js`

- [ ] **Step 1: stocks.js 작성**

`frontend/src/api/stocks.js` 전체 내용:

```js
import api from './axios';

export const getStocks = () => api.get('/stocks');
export const createStock = (body) => api.post('/stocks', body);
export const updateStock = (id, body) => api.patch(`/stocks/${id}`, body);
export const deleteStock = (id) => api.delete(`/stocks/${id}`);
export const searchStocks = (keyword) => api.get('/stocks/search', { params: { keyword } });
export const updateStockSector = (id, sector) => api.patch(`/stocks/${id}/sector`, { sector });
```

- [ ] **Step 2: transactions.js 생성**

`frontend/src/api/transactions.js` 전체 내용:

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

- [ ] **Step 3: dashboard.js 생성**

`frontend/src/api/dashboard.js` 전체 내용:

```js
import api from './axios';

export const getDashboardSummary = () => api.get('/analysis/dashboard');
export const getStockWeight = () => api.get('/analysis/stock-weight');
export const getMonthlyDividends = () => api.get('/dividends/monthly');
```

- [ ] **Step 4: admin.js 생성**

`frontend/src/api/admin.js` 전체 내용:

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

- [ ] **Step 5: 커밋**

```bash
git add frontend/src/api/stocks.js frontend/src/api/transactions.js \
        frontend/src/api/dashboard.js frontend/src/api/admin.js
git commit -m "feat: API 레이어 4개 파일 작성 (stocks/transactions/dashboard/admin)"
```

---

### Task 2: DashboardPage.jsx 생성

**Files:**
- Create: `frontend/src/pages/DashboardPage.jsx`

- [ ] **Step 1: DashboardPage.jsx 작성**

`frontend/src/pages/DashboardPage.jsx` 전체 내용:

```jsx
import { useState, useEffect } from 'react';
import { getDashboardSummary, getStockWeight, getMonthlyDividends } from '../api/dashboard';

const init12Months = () =>
  Array.from({ length: 12 }, (_, i) => ({
    month: i + 1,
    expectedAmount: 0,
    confirmedAmount: 0,
  }));

export default function DashboardPage() {
  const [summary, setSummary] = useState(null);
  const [stockWeights, setStockWeights] = useState([]);
  const [monthlyDividends, setMonthlyDividends] = useState(init12Months());
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetch = async () => {
      setIsLoading(true);
      try {
        const [summaryRes, weightRes, monthlyRes] = await Promise.all([
          getDashboardSummary(),
          getStockWeight(),
          getMonthlyDividends(),
        ]);

        const summaryData = summaryRes.data.data;
        console.log('dashboard summary:', summaryData);
        setSummary({
          ...summaryData,
          goalAchievementRate: summaryData.goalAchievementRate ?? null,
          targetDividend: summaryData.targetDividend ?? null,
        });

        const weights = weightRes.data.data;
        console.log('stock weights:', weights);
        setStockWeights(weights);

        const monthly = monthlyRes.data.data;
        console.log('monthly dividends:', monthly);
        const merged = init12Months();
        monthly.forEach((item) => {
          merged[item.month - 1] = item;
        });
        setMonthlyDividends(merged);
      } catch (err) {
        setError(err.message);
      } finally {
        setIsLoading(false);
      }
    };
    fetch();
  }, []);

  if (isLoading) return <p>로딩 중...</p>;
  if (error) return <p>에러: {error}</p>;

  return (
    <div>
      <h1>대시보드</h1>

      <section>
        <h2>요약 카드</h2>
        {summary && (
          <ul>
            <li>총 투자금: {summary.totalInvestment}</li>
            <li>보유 종목 수: {summary.stockCount}</li>
            <li>예상 연간 배당금: {summary.expectedAnnualDividend}</li>
            <li>배당 수익률: {summary.dividendYield}</li>
            <li>목표 달성률: {summary.goalAchievementRate ?? '목표 미설정'}</li>
            <li>목표 배당금: {summary.targetDividend ?? '목표 미설정'}</li>
          </ul>
        )}
      </section>

      <section>
        <h2>종목별 투자 비중 (도넛 차트 영역)</h2>
        <pre>{JSON.stringify(stockWeights, null, 2)}</pre>
      </section>

      <section>
        <h2>월별 배당금 (막대그래프 영역)</h2>
        <pre>{JSON.stringify(monthlyDividends, null, 2)}</pre>
      </section>
    </div>
  );
}
```

- [ ] **Step 2: 커밋**

```bash
git add frontend/src/pages/DashboardPage.jsx
git commit -m "feat: DashboardPage 뼈대 구현 (Promise.all 병렬 호출, 12달 초기화)"
```

---

### Task 3: StocksPage.jsx 생성

**Files:**
- Create: `frontend/src/pages/StocksPage.jsx`

- [ ] **Step 1: StocksPage.jsx 작성**

`frontend/src/pages/StocksPage.jsx` 전체 내용:

```jsx
import { useState, useEffect } from 'react';
import { getStocks, deleteStock, searchStocks } from '../api/stocks';

export default function StocksPage() {
  const [stocks, setStocks] = useState([]);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchStocks = async () => {
    setIsLoading(true);
    try {
      const res = await getStocks();
      console.log('stocks:', res.data.data);
      setStocks(res.data.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchStocks();
  }, []);

  useEffect(() => {
    if (!searchKeyword.trim()) {
      setSearchResults([]);
      return;
    }
    const timer = setTimeout(async () => {
      try {
        const res = await searchStocks(searchKeyword);
        console.log('search results:', res.data.data);
        setSearchResults(res.data.data);
      } catch (err) {
        console.error('검색 에러:', err.message);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [searchKeyword]);

  const handleDelete = async (id) => {
    try {
      await deleteStock(id);
      await fetchStocks();
    } catch (err) {
      setError(err.message);
    }
  };

  if (isLoading) return <p>로딩 중...</p>;
  if (error) return <p>에러: {error}</p>;

  return (
    <div>
      <h1>보유 종목</h1>

      <section>
        <h2>종목 검색 (자동완성)</h2>
        <input
          type="text"
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          placeholder="종목명 또는 코드 검색"
        />
        {searchResults.length > 0 && (
          <ul>
            {searchResults.map((r) => (
              <li key={r.code}>{r.name} ({r.code}) — {r.sector}</li>
            ))}
          </ul>
        )}
      </section>

      <section>
        <h2>보유 종목 목록</h2>
        <table>
          <thead>
            <tr>
              <th>종목명</th><th>코드</th><th>섹터</th><th>수량</th>
              <th>평균단가</th><th>평가금액</th><th>수익률</th><th>작업</th>
            </tr>
          </thead>
          <tbody>
            {stocks.map((s) => (
              <tr key={s.id}>
                <td>{s.name}</td>
                <td>{s.code}</td>
                <td>{s.sector}</td>
                <td>{s.quantity}</td>
                <td>{s.averagePrice}</td>
                <td>{s.evaluationAmount}</td>
                <td>{s.profitRate}%</td>
                <td>
                  <button onClick={() => handleDelete(s.id)}>삭제</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}
```

- [ ] **Step 2: 커밋**

```bash
git add frontend/src/pages/StocksPage.jsx
git commit -m "feat: StocksPage 뼈대 구현 (목록 조회, 검색 300ms debounce, 삭제 후 갱신)"
```

---

### Task 4: TransactionsPage.jsx 생성

**Files:**
- Create: `frontend/src/pages/TransactionsPage.jsx`

- [ ] **Step 1: TransactionsPage.jsx 작성**

`frontend/src/pages/TransactionsPage.jsx` 전체 내용:

```jsx
import { useState, useEffect } from 'react';
import {
  getTransactions,
  deleteTransaction,
  getTransactionsSummary,
  getTransactionsChart,
} from '../api/transactions';

const init12Months = () =>
  Array.from({ length: 12 }, (_, i) => ({
    month: i + 1,
    buyAmount: 0,
    sellAmount: 0,
  }));

export default function TransactionsPage() {
  const [transactions, setTransactions] = useState([]);
  const [summary, setSummary] = useState(null);
  const [chartData, setChartData] = useState(init12Months());
  const [filterYear, setFilterYear] = useState('');
  const [filterType, setFilterType] = useState('ALL');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchTransactions = async () => {
    setIsLoading(true);
    try {
      const params = {};
      if (filterYear) params.year = filterYear;
      if (filterType !== 'ALL') params.type = filterType;
      const res = await getTransactions(params);
      console.log('transactions:', res.data.data);
      setTransactions(res.data.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, [filterYear, filterType]);

  useEffect(() => {
    const fetch = async () => {
      try {
        const res = await getTransactionsSummary();
        console.log('transactions summary:', res.data.data);
        setSummary(res.data.data);
      } catch (err) {
        console.error('summary 에러:', err.message);
      }
    };
    fetch();
  }, []);

  useEffect(() => {
    const fetch = async () => {
      try {
        const res = await getTransactionsChart(filterYear || undefined);
        console.log('chart data:', res.data.data);
        const merged = init12Months();
        res.data.data.forEach((item) => {
          merged[item.month - 1] = item;
        });
        setChartData(merged);
      } catch (err) {
        console.error('chart 에러:', err.message);
      }
    };
    fetch();
  }, [filterYear]);

  const handleDelete = async (id) => {
    try {
      await deleteTransaction(id);
      await fetchTransactions();
    } catch (err) {
      setError(err.message);
    }
  };

  if (isLoading) return <p>로딩 중...</p>;
  if (error) return <p>에러: {error}</p>;

  return (
    <div>
      <h1>거래 내역</h1>

      <section>
        <h2>필터</h2>
        <input
          type="number"
          value={filterYear}
          onChange={(e) => setFilterYear(e.target.value)}
          placeholder="연도 (예: 2026)"
        />
        <select value={filterType} onChange={(e) => setFilterType(e.target.value)}>
          <option value="ALL">전체</option>
          <option value="BUY">매수</option>
          <option value="SELL">매도</option>
        </select>
      </section>

      {summary && (
        <section>
          <h2>요약</h2>
          <ul>
            <li>총 매수금액: {summary.totalBuyAmount}</li>
            <li>총 매도금액: {summary.totalSellAmount}</li>
            <li>총 거래 수: {summary.totalTradeCount}</li>
          </ul>
        </section>
      )}

      <section>
        <h2>월별 매수/매도 (꺾은선 차트 영역)</h2>
        <pre>{JSON.stringify(chartData, null, 2)}</pre>
      </section>

      <section>
        <h2>거래 목록</h2>
        <table>
          <thead>
            <tr>
              <th>종목명</th><th>유형</th><th>수량</th><th>가격</th>
              <th>날짜</th><th>수수료</th><th>거래세</th><th>총액</th><th>작업</th>
            </tr>
          </thead>
          <tbody>
            {transactions.map((t) => (
              <tr key={t.id}>
                <td>{t.stockName}</td>
                <td>{t.type}</td>
                <td>{t.quantity}</td>
                <td>{t.price}</td>
                <td>{t.date}</td>
                <td>{t.brokerFee}</td>
                <td>{t.type === 'SELL' ? t.transactionTax : '-'}</td>
                <td>{t.totalAmount}</td>
                <td>
                  <button onClick={() => handleDelete(t.id)}>삭제</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}
```

- [ ] **Step 2: 커밋**

```bash
git add frontend/src/pages/TransactionsPage.jsx
git commit -m "feat: TransactionsPage 뼈대 구현 (필터링, 요약, 차트 데이터, 삭제)"
```

---

### Task 5: AdminPage.jsx 생성

**Files:**
- Create: `frontend/src/pages/AdminPage.jsx`

- [ ] **Step 1: AdminPage.jsx 작성**

`frontend/src/pages/AdminPage.jsx` 전체 내용:

```jsx
import { useState, useEffect } from 'react';
import {
  getUsers,
  getActiveNow,
  getUserStats,
  getActiveUsers,
  getStockStats,
  getTopStocks,
  getSectorWeight,
  getAvgInvestment,
  getStockDividend,
  getAvgDividend,
  getSectorDividend,
  getSectorInvestment,
} from '../api/admin';

export default function AdminPage() {
  const [users, setUsers] = useState([]);
  const [stats, setStats] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [isForbidden, setIsForbidden] = useState(false);

  useEffect(() => {
    const fetch = async () => {
      setIsLoading(true);
      try {
        const [
          usersRes,
          activeNowRes,
          userStatsRes,
          activeUsersRes,
          stockStatsRes,
          topStocksRes,
          sectorWeightRes,
          avgInvestmentRes,
          stockDividendRes,
          avgDividendRes,
          sectorDividendRes,
          sectorInvestmentRes,
        ] = await Promise.all([
          getUsers(),
          getActiveNow(),
          getUserStats(),
          getActiveUsers(),
          getStockStats(),
          getTopStocks(),
          getSectorWeight(),
          getAvgInvestment(),
          getStockDividend(),
          getAvgDividend(),
          getSectorDividend(),
          getSectorInvestment(),
        ]);

        console.log('users:', usersRes.data.data);
        setUsers(usersRes.data.data);

        const statsData = {
          activeNow: activeNowRes.data.data,
          userStats: userStatsRes.data.data,
          activeUsers: activeUsersRes.data.data,
          stockStats: stockStatsRes.data.data,
          topStocks: topStocksRes.data.data,
          sectorWeight: sectorWeightRes.data.data,
          avgInvestment: avgInvestmentRes.data.data,
          stockDividend: stockDividendRes.data.data,
          avgDividend: avgDividendRes.data.data,
          sectorDividend: sectorDividendRes.data.data,
          sectorInvestment: sectorInvestmentRes.data.data,
        };
        console.log('admin stats:', statsData);
        setStats(statsData);
      } catch (err) {
        if (err.response?.status === 403) {
          setIsForbidden(true);
        } else {
          setError(err.message);
        }
      } finally {
        setIsLoading(false);
      }
    };
    fetch();
  }, []);

  if (isLoading) return <p>로딩 중...</p>;
  if (isForbidden) return <p>권한이 없습니다</p>;
  if (error) return <p>에러: {error}</p>;

  return (
    <div>
      <h1>관리자</h1>

      <section>
        <h2>통계</h2>
        {stats && <pre>{JSON.stringify(stats, null, 2)}</pre>}
      </section>

      <section>
        <h2>회원 목록</h2>
        <table>
          <thead>
            <tr>
              <th>ID</th><th>이메일</th><th>이름</th><th>역할</th>
              <th>가입일</th><th>최근 로그인</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td>{u.id}</td>
                <td>{u.email}</td>
                <td>{u.name}</td>
                <td>{u.role}</td>
                <td>{u.createdAt}</td>
                <td>{u.lastLoginAt}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}
```

- [ ] **Step 2: 커밋**

```bash
git add frontend/src/pages/AdminPage.jsx
git commit -m "feat: AdminPage 뼈대 구현 (12개 통계 병렬 호출, 403 권한 없음 처리)"
```

---

### Task 6: App.jsx 수정

**Files:**
- Modify: `frontend/src/App.jsx`

- [ ] **Step 1: import 4개 추가**

`frontend/src/App.jsx` 6번 줄 (`import SignupPage from './pages/SignupPage'`) 바로 다음에 추가:

```jsx
import DashboardPage    from './pages/DashboardPage'
import StocksPage       from './pages/StocksPage'
import TransactionsPage from './pages/TransactionsPage'
import AdminPage        from './pages/AdminPage'
```

- [ ] **Step 2: 라우트 4개 교체**

기존:
```jsx
<Route path="/dashboard"    element={<div />} />
<Route path="/stocks"       element={<div />} />
<Route path="/transactions" element={<div />} />
```
그리고 `/admin`의 `<div />`

교체 후:
```jsx
<Route path="/dashboard"    element={<DashboardPage />} />
<Route path="/stocks"       element={<StocksPage />} />
<Route path="/transactions" element={<TransactionsPage />} />
```
그리고 `/admin`을 `<AdminPage />`로

`/dividends`, `/analysis`, `/rebalancing`은 `<div />`로 유지.

- [ ] **Step 3: 커밋**

```bash
git add frontend/src/App.jsx
git commit -m "feat: App.jsx 라우트 연결 (dashboard/stocks/transactions/admin)"
```
