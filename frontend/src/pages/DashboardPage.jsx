import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  BarElement,
} from 'chart.js'
import { Doughnut, Bar } from 'react-chartjs-2'
import './DashboardPage.css'

ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement)

const fmt = (n) => n.toLocaleString('ko-KR')

const MOCK = {
  date: '2026.05',
  totalBalance: 15_200_000,
  totalInvested: 12_500_000,
  totalSavings: 15_200_000,
  expectedAnnualDividend: 487_500,
  goalAchievement: 68,
  portfolio: {
    labels: ['삼성전자', 'AAPL', 'MSFT', '기타'],
    data: [4_375_000, 3_125_000, 2_500_000, 2_500_000],
  },
  monthlyDividends: [0, 15000, 48000, 0, 42000, 55000, 0, 44000, 0, 68000, 0, 58000],
}

const ALL_MONTHS = ['1월','2월','3월','4월','5월','6월','7월','8월','9월','10월','11월','12월']
const PORTFOLIO_COLORS = ['#1D9E75', '#5DCAA5', '#9FE1CB', '#e0e0e0']

const maxMonthly = Math.max(...MOCK.monthlyDividends.filter(v => v > 0))

const getBarColor = (v) => {
  if (v === 0) return '#f0f0f0'
  const r = v / maxMonthly
  if (r <= 0.25) return '#9FE1CB'
  if (r <= 0.55) return '#5DCAA5'
  if (r <= 0.82) return '#2DB589'
  return '#1D9E75'
}

const donutData = {
  labels: MOCK.portfolio.labels,
  datasets: [{
    data: MOCK.portfolio.data,
    backgroundColor: PORTFOLIO_COLORS,
    borderWidth: 2,
    borderColor: '#fff',
    hoverOffset: 6,
  }],
}

const donutOptions = {
  responsive: true,
  maintainAspectRatio: false,
  cutout: '40%',
  plugins: {
    legend: { display: false },
    tooltip: { callbacks: { label: (ctx) => ` ${fmt(ctx.raw)}원` } },
  },
}

const barData = {
  labels: ALL_MONTHS,
  datasets: [{
    data: MOCK.monthlyDividends,
    backgroundColor: MOCK.monthlyDividends.map(getBarColor),
    borderRadius: 5,
    borderSkipped: false,
  }],
}

const barOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: { callbacks: { label: (ctx) => ` ${fmt(ctx.raw)}원` } },
  },
  scales: {
    x: {
      grid: { display: false },
      border: { display: false },
      ticks: { color: '#999', font: { family: 'Outfit', size: 11 } },
    },
    y: {
      grid: { color: '#f0f0f0' },
      border: { display: false },
      ticks: {
        color: '#999',
        font: { family: 'Outfit', size: 10 },
        callback: (v) => v > 0 ? `${v / 10000}만` : '0',
      },
    },
  },
}

export default function DashboardPage() {
  return (
    <div className="db-page">

      {/* ── 헤더 ── */}
      <div className="db-header">
        <div>
          <h1 className="db-title">대시보드</h1>
          <p className="db-subtitle">포트폴리오 현황을 한눈에 확인하세요</p>
        </div>
        <div className="db-header-right">
          <span className="db-date-badge">{MOCK.date}</span>
          <div className="db-total-block">
            <span className="db-total-label">총 잔액</span>
            <span className="db-total-value">
              {fmt(MOCK.totalBalance)}<span className="db-unit">원</span>
            </span>
          </div>
        </div>
      </div>

      {/* ── 중간: 도넛(45%) + 수익현황(55%) ── */}
      <div className="db-main-grid">

        <div className="db-card db-donut-card">
          <p className="db-card-title">투자 포트폴리오</p>
          <div className="db-donut-wrap">
            <Doughnut data={donutData} options={donutOptions} />
            <div className="db-donut-center">
              <span className="db-donut-label">총 투자금</span>
              <span className="db-donut-amount">
                {fmt(MOCK.totalInvested / 10000)}
                <span className="db-donut-unit">만원</span>
              </span>
            </div>
          </div>
          <div className="db-legend">
            {MOCK.portfolio.labels.map((label, i) => (
              <div key={i} className="db-legend-item">
                <span className="db-legend-dot" style={{ background: PORTFOLIO_COLORS[i] }} />
                <span className="db-legend-name">{label}</span>
                <span className="db-legend-pct">
                  {Math.round(MOCK.portfolio.data[i] / MOCK.totalInvested * 100)}%
                </span>
              </div>
            ))}
          </div>
        </div>

        <div className="db-stats-col">
          <div className="db-stat-card db-stat-card--savings">
            <span className="db-stat-label">총 저축금</span>
            <span className="db-stat-value">
              {fmt(MOCK.totalSavings)}<span className="db-stat-unit">원</span>
            </span>
            <span className="db-stat-meta db-stat-meta-green">전월 대비 +120,000원 ▲</span>
          </div>
          <div className="db-stat-card db-stat-card--dividend">
            <span className="db-stat-label">예상 연간 배당금</span>
            <span className="db-stat-value profit">
              {fmt(MOCK.expectedAnnualDividend)}<span className="db-stat-unit">원</span>
            </span>
            <span className="db-stat-meta">배당수익률 3.2%</span>
          </div>
          <div className="db-stat-card db-stat-card--goal">
            <span className="db-stat-label">목표 달성률</span>
            <span className="db-stat-value">
              {MOCK.goalAchievement}<span className="db-stat-unit">%</span>
            </span>
            <div className="db-goal-track">
              <div className="db-goal-fill" style={{ width: `${MOCK.goalAchievement}%` }} />
            </div>
            <span className="db-stat-meta">목표까지 158,500원 남음</span>
          </div>
        </div>

      </div>

      {/* ── 하단: 월별 배당 바차트 ── */}
      <div className="db-card db-bottom-card">
        <p className="db-card-title">
          월별 배당 현황
          <span className="db-card-title-sub">2026년 기준</span>
        </p>
        <div className="db-bar-wrap">
          <Bar data={barData} options={barOptions} />
        </div>
      </div>

    </div>
  )
}
