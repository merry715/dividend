import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import './AdminPage.css'
import {
  Chart as ChartJS,
  CategoryScale, LinearScale,
  PointElement, LineElement,
  Tooltip, Legend, Filler,
} from 'chart.js'
import { Line } from 'react-chartjs-2'
import { doLogout } from '../utils/logout'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend, Filler)

const fmt = n => Number(n).toLocaleString('ko-KR')

async function apiFetch(url) {
  const token = localStorage.getItem('token') || localStorage.getItem('accessToken')
  const res = await fetch(url, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (!res.ok) throw new Error(`${url} → HTTP ${res.status}`)
  const json = await res.json()
  return json?.data ?? json
}

export default function AdminPage() {
  const navigate = useNavigate()
  const [loading, setLoading]                 = useState(true)
  const [totalUsers, setTotalUsers]           = useState(0)
  const [activeUsers, setActiveUsers]         = useState(0)
  const [totalStocks, setTotalStocks]         = useState(0)
  const [monthlyTrend, setMonthlyTrend]       = useState([])
  const [topStocks, setTopStocks]             = useState([])
  const [sectorDividends, setSectorDividends] = useState([])

  useEffect(() => {
    async function load() {
      const [userStats, activeData, top10, sectorDist, dividendAvg] = await Promise.all([
        apiFetch('https://dividend-production.up.railway.app/api/v1/admin/stats/users'),
        apiFetch('https://dividend-production.up.railway.app/api/v1/admin/stats/users/active'),
        apiFetch('https://dividend-production.up.railway.app/api/v1/admin/stats/stocks/top10'),
        apiFetch('https://dividend-production.up.railway.app/api/v1/admin/stats/sectors'),
        apiFetch('https://dividend-production.up.railway.app/api/v1/admin/stats/dividends/average'),
      ])

      setTotalUsers(userStats.totalUsers)
      setActiveUsers(activeData.activeUsers)
      setMonthlyTrend(userStats.monthlyTrend.map(m => ({ month: m.month, users: m.count })))
      setTotalStocks(sectorDist.reduce((s, r) => s + Number(r.count), 0))
      setTopStocks(top10.slice(0, 3).map((s, i) => ({
        rank: i + 1, name: s.stockName, count: s.count, sector: s.sector ?? '-'
      })))
      setSectorDividends(dividendAvg.bySector.map(s => ({
        sector: s.label,
        avgDividend: s.avgAmount,
        stocks: sectorDist.find(d => d.sector === s.sector)?.count ?? 0,
      })))

      setLoading(false)
    }
    load()
  }, [])

  const lineData = {
    labels: monthlyTrend.map(m => m.month),
    datasets: [{
      label: '가입자 수',
      data: monthlyTrend.map(m => m.users),
      borderColor: '#1D9E75',
      backgroundColor: 'rgba(29, 158, 117, 0.08)',
      borderWidth: 2.5,
      pointBackgroundColor: '#1D9E75',
      pointBorderColor: '#fff',
      pointBorderWidth: 2,
      pointRadius: 4,
      pointHoverRadius: 6,
      tension: 0.4,
      fill: true,
    }],
  }

  const lineOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: { callbacks: { label: ctx => ` ${ctx.parsed.y}명` } },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { font: { family: 'Outfit', size: 11 }, color: '#bbb' },
        border: { display: false },
      },
      y: {
        grid: { color: '#f4f4f4' },
        ticks: { font: { family: 'Outfit', size: 10 }, color: '#bbb' },
        border: { display: false },
        beginAtZero: true,
      },
    },
  }

  if (loading) return (
    <div className="adm-page" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '60vh' }}>
      불러오는 중...
    </div>
  )

  return (
    <div className="adm-page">

      {/* ── 상단 바 ── */}
      <div className="adm-topbar">
        <div className="adm-brand">
          <span className="adm-brand-text">leafpay</span>
          <span className="adm-brand-badge">관리자</span>
        </div>
        <div className="adm-user-row">
          <div className="adm-avatar">A</div>
          <span className="adm-user-label">admin</span>
          <button className="adm-logout-btn" onClick={() => doLogout(navigate)}>로그아웃</button>
        </div>
      </div>

      <div className="adm-content">

        {/* 요약 카드 3개 */}
        <div className="adm-summary-row">
          <div className="adm-sum-card">
            <p className="adm-sum-label">전체 가입 회원수</p>
            <p className="adm-sum-value">{fmt(totalUsers)}<span className="adm-sum-unit">명</span></p>
            <p className="adm-sum-sub">누적 회원 합계</p>
          </div>
          <div className="adm-sum-card">
            <p className="adm-sum-label">최근 30일 활성 사용자</p>
            <p className="adm-sum-value">{fmt(activeUsers)}<span className="adm-sum-unit">명</span></p>
            <p className="adm-sum-sub">최근 30일 이내 로그인</p>
          </div>
          <div className="adm-sum-card">
            <p className="adm-sum-label">전체 등록 종목 수</p>
            <p className="adm-sum-value">{fmt(totalStocks)}<span className="adm-sum-unit">종목</span></p>
            <p className="adm-sum-sub">현재 활성 종목</p>
          </div>
        </div>

        {/* 월별 가입자 추이 */}
        <div className="adm-card adm-chart-card">
          <p className="adm-card-title">전체 가입 회원 수 월별 추이 (2025)</p>
          <div className="adm-chart-wrap">
            <Line data={lineData} options={lineOptions} />
          </div>
        </div>

        {/* 하단 테이블 2개 */}
        <div className="adm-bottom-row">

          <div className="adm-card adm-table-card">
            <p className="adm-card-title">많이 등록된 종목 Top 3</p>
            <table className="adm-table">
              <thead>
                <tr>
                  <th>순위</th><th>종목명</th><th>섹터</th><th className="right">등록 수</th>
                </tr>
              </thead>
              <tbody>
                {topStocks.map(s => (
                  <tr key={s.rank}>
                    <td><span className={`adm-rank adm-rank-${s.rank}`}>{s.rank}</span></td>
                    <td className="adm-stock-name">{s.name}</td>
                    <td className="adm-sector-tag">{s.sector}</td>
                    <td className="right adm-count">{fmt(s.count)}명</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="adm-card adm-table-card">
            <p className="adm-card-title">섹터별 평균 배당금</p>
            <table className="adm-table">
              <thead>
                <tr>
                  <th>섹터</th><th className="right">평균 배당금</th><th className="right">종목 수</th>
                </tr>
              </thead>
              <tbody>
                {sectorDividends.map(s => (
                  <tr key={s.sector}>
                    <td className="adm-sector-name">{s.sector}</td>
                    <td className="right adm-dividend">{fmt(s.avgDividend)}원</td>
                    <td className="right adm-stocks-count">{s.stocks}개</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

        </div>
      </div>
    </div>
  )
}
