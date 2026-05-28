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
