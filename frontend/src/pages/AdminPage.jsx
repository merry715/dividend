import { useNavigate } from 'react-router-dom'
import './AdminPage.css'
import {
  Chart as ChartJS,
  CategoryScale, LinearScale,
  PointElement, LineElement,
  Tooltip, Legend, Filler,
} from 'chart.js'
import { Line } from 'react-chartjs-2'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend, Filler)

const fmt = n => Number(n).toLocaleString('ko-KR')

const MONTHLY_SIGNUPS = [
  { month: '1월',  users: 12  },
  { month: '2월',  users: 19  },
  { month: '3월',  users: 31  },
  { month: '4월',  users: 28  },
  { month: '5월',  users: 45  },
  { month: '6월',  users: 52  },
  { month: '7월',  users: 61  },
  { month: '8월',  users: 58  },
  { month: '9월',  users: 74  },
  { month: '10월', users: 83  },
  { month: '11월', users: 91  },
  { month: '12월', users: 108 },
]

const TOP_STOCKS = [
  { rank: 1, name: '삼성전자', count: 1284, sector: '반도체' },
  { rank: 2, name: 'AAPL',    count: 976,  sector: '기술'   },
  { rank: 3, name: 'NVDA',    count: 841,  sector: '반도체' },
]

const SECTOR_DIVIDENDS = [
  { sector: '반도체', avgDividend: 82400,  stocks: 3 },
  { sector: '기술',   avgDividend: 64200,  stocks: 5 },
  { sector: '금융',   avgDividend: 112000, stocks: 2 },
  { sector: '자동차', avgDividend: 38600,  stocks: 2 },
  { sector: '기타',   avgDividend: 29800,  stocks: 4 },
]

export default function AdminPage() {
  const navigate  = useNavigate()
  const totalUsers  = MONTHLY_SIGNUPS.reduce((s, m) => s + m.users, 0)
  const activeUsers = MONTHLY_SIGNUPS.slice(-2).reduce((s, m) => s + m.users, 0)
  const totalStocks = 5

  const handleLogout = () => {
    localStorage.removeItem('auth')
    navigate('/login', { replace: true })
  }

  const lineData = {
    labels: MONTHLY_SIGNUPS.map(m => m.month),
    datasets: [{
      label: '가입자 수',
      data: MONTHLY_SIGNUPS.map(m => m.users),
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
          <button className="adm-logout-btn" onClick={handleLogout}>로그아웃</button>
        </div>
      </div>

      <div className="adm-content">

        {/* ── 요약 카드 3개 ── */}
        <div className="adm-summary-row">
          <div className="adm-sum-card">
            <p className="adm-sum-label">전체 가입 회원수</p>
            <p className="adm-sum-value">{fmt(totalUsers)}<span className="adm-sum-unit">명</span></p>
            <p className="adm-sum-sub">누적 회원 합계</p>
          </div>
          <div className="adm-sum-card">
            <p className="adm-sum-label">최근 30일 활성 사용자</p>
            <p className="adm-sum-value">{fmt(activeUsers)}<span className="adm-sum-unit">명</span></p>
            <p className="adm-sum-sub">최근 2개월 신규 가입</p>
          </div>
          <div className="adm-sum-card">
            <p className="adm-sum-label">전체 등록 종목 수</p>
            <p className="adm-sum-value">{fmt(totalStocks)}<span className="adm-sum-unit">종목</span></p>
            <p className="adm-sum-sub">현재 활성 종목</p>
          </div>
        </div>

        {/* ── 월별 가입자 추이 라인 차트 ── */}
        <div className="adm-card adm-chart-card">
          <p className="adm-card-title">전체 가입 회원 수 월별 추이 (2025)</p>
          <div className="adm-chart-wrap">
            <Line data={lineData} options={lineOptions} />
          </div>
        </div>

        {/* ── 하단: 테이블 2개 ── */}
        <div className="adm-bottom-row">

          {/* 많이 등록된 종목 Top 3 */}
          <div className="adm-card adm-table-card">
            <p className="adm-card-title">많이 등록된 종목 Top 3</p>
            <table className="adm-table">
              <thead>
                <tr>
                  <th>순위</th>
                  <th>종목명</th>
                  <th>섹터</th>
                  <th className="right">등록 수</th>
                </tr>
              </thead>
              <tbody>
                {TOP_STOCKS.map(s => (
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

          {/* 섹터별 평균 배당금 */}
          <div className="adm-card adm-table-card">
            <p className="adm-card-title">섹터별 평균 배당금</p>
            <table className="adm-table">
              <thead>
                <tr>
                  <th>섹터</th>
                  <th className="right">평균 배당금</th>
                  <th className="right">종목 수</th>
                </tr>
              </thead>
              <tbody>
                {SECTOR_DIVIDENDS.map(s => (
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
