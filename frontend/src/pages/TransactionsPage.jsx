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
