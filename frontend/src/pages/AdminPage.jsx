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
