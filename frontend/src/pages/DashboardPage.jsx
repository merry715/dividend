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
