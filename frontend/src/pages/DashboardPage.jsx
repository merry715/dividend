import { useState, useEffect, useMemo } from 'react'
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
import { getDashboard } from '../api/dashboard'

ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement)

const fmt = (n) => (Number(n) || 0).toLocaleString('ko-KR')

const ALL_MONTHS = ['1월','2월','3월','4월','5월','6월','7월','8월','9월','10월','11월','12월']
const PORTFOLIO_COLORS = ['#1D9E75', '#5DCAA5', '#9FE1CB', '#e0e0e0']

const getBarColor = (v, maxMonthly) => {
  if (!v || v === 0) return '#f0f0f0'
  const r = v / maxMonthly
  if (r <= 0.25) return '#9FE1CB'
  if (r <= 0.55) return '#5DCAA5'
  if (r <= 0.82) return '#2DB589'
  return '#1D9E75'
}

const EMPTY = {
  totalInvestment: 0,
  totalDividend: 0,
  targetDividend: 0,
  achievementRate: '0.0',
  holdings: [],
  monthlyDividends: Array.from({ length: 12 }, (_, i) => ({ month: i + 1, amount: 0 })),
}

export default function DashboardPage() {
  const [data, setData]     = useState(EMPTY)
  const [loading, setLoading] = useState(true)

  const now = new Date()
  const dateLabel = `${now.getFullYear()}.${String(now.getMonth() + 1).padStart(2, '0')}`

  useEffect(() => {
    getDashboard()
      .then(res => setData({ ...EMPTY, ...res.data }))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  const monthlyArr = useMemo(() =>
    (data.monthlyDividends ?? []).map(d => d.amount ?? 0),
    [data.monthlyDividends]
  )

  const maxMonthly = useMemo(() =>
    Math.max(...monthlyArr.filter(v => v > 0), 1),
    [monthlyArr]
  )

  const holdings     = data.holdings ?? []
  const top3         = holdings.slice(0, 3)
  const restHoldings = holdings.slice(3)

  const portfolioLabels = [
    ...top3.map(h => h.stockName),
    ...(restHoldings.length > 0 ? ['기타'] : []),
  ]
  const portfolioData = [
    ...top3.map(h => h.totalInvestment),
    ...(restHoldings.length > 0 ? [restHoldings.reduce((s, h) => s + h.totalInvestment, 0)] : []),
  ]

  const hasPortfolio = portfolioData.length > 0

  const donutData = {
    labels: hasPortfolio ? portfolioLabels : ['데이터 없음'],
    datasets: [{
      data: hasPortfolio ? portfolioData : [1],
      backgroundColor: hasPortfolio ? PORTFOLIO_COLORS : ['#f0f0f0'],
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
      tooltip: {
        callbacks: {
          label: (ctx) => hasPortfolio ? ` ${fmt(ctx.raw)}원` : ' 데이터 없음',
        },
      },
    },
  }

  const barData = {
    labels: ALL_MONTHS,
    datasets: [{
      data: monthlyArr,
      backgroundColor: monthlyArr.map(v => getBarColor(v, maxMonthly)),
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
        ticks: { display: false },
      },
    },
  }

  const achievementRate = Math.min(parseFloat(data.achievementRate) || 0, 100)
  const totalInvestment = Number(data.totalInvestment) || 0
  const totalDividend   = Number(data.totalDividend) || 0
  const targetDividend  = Number(data.targetDividend) || 0

  if (loading) {
    return (
      <div className="db-page">
        <div className="db-header">
          <div>
            <h1 className="db-title">대시보드</h1>
            <p className="db-subtitle">포트폴리오 현황을 한눈에 확인하세요</p>
          </div>
        </div>
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 300, color: '#bbb' }}>
          데이터를 불러오는 중...
        </div>
      </div>
    )
  }

  return (
    <div className="db-page">

      {/* ── 헤더 ── */}
      <div className="db-header">
        <div>
          <h1 className="db-title">대시보드</h1>
          <p className="db-subtitle">포트폴리오 현황을 한눈에 확인하세요</p>
        </div>
        <div className="db-header-right">
          <span className="db-date-badge">{dateLabel}</span>
          <div className="db-total-block">
            <span className="db-total-label">총 투자금</span>
            <span className="db-total-value">
              {fmt(totalInvestment)}<span className="db-unit">원</span>
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
                {fmt(Math.round(totalInvestment / 10000))}
                <span className="db-donut-unit">만원</span>
              </span>
            </div>
          </div>
          <div className="db-legend">
            {hasPortfolio ? portfolioLabels.map((label, i) => (
              <div key={i} className="db-legend-item">
                <span className="db-legend-dot" style={{ background: PORTFOLIO_COLORS[i] }} />
                <span className="db-legend-name">{label}</span>
                <span className="db-legend-pct">
                  {totalInvestment > 0 ? Math.round(portfolioData[i] / totalInvestment * 100) : 0}%
                </span>
              </div>
            )) : (
              <span style={{ color: '#bbb', fontSize: 12 }}>보유 종목이 없습니다</span>
            )}
          </div>
        </div>

        <div className="db-stats-col">
          <div className="db-stat-card db-stat-card--savings">
            <span className="db-stat-label">총 투자 원금</span>
            <span className="db-stat-value">
              {fmt(totalInvestment)}<span className="db-stat-unit">원</span>
            </span>
          </div>
          <div className="db-stat-card db-stat-card--dividend">
            <span className="db-stat-label">예상 연간 배당금</span>
            <span className="db-stat-value profit">
              {fmt(totalDividend)}<span className="db-stat-unit">원</span>
            </span>
            <span className="db-stat-meta">
              {totalInvestment > 0
                ? `배당수익률 ${(totalDividend / totalInvestment * 100).toFixed(1)}%`
                : '데이터 없음'}
            </span>
          </div>
          <div className="db-stat-card db-stat-card--goal">
            <span className="db-stat-label">목표 달성률</span>
            <span className="db-stat-value">
              {parseFloat(data.achievementRate || 0).toFixed(1)}<span className="db-stat-unit">%</span>
            </span>
            <div className="db-goal-track">
              <div className="db-goal-fill" style={{ width: `${achievementRate}%` }} />
            </div>
            <span className="db-stat-meta">
              {targetDividend > 0
                ? `목표까지 ${fmt(Math.max(targetDividend - totalDividend, 0))}원 남음`
                : '목표 미설정'}
            </span>
          </div>
        </div>

      </div>

      {/* ── 하단: 월별 배당 바차트 ── */}
      <div className="db-card db-bottom-card">
        <p className="db-card-title">
          월별 배당 현황
          <span className="db-card-title-sub">{now.getFullYear()}년 기준</span>
        </p>
        <div className="db-bar-wrap">
          <Bar data={barData} options={barOptions} />
        </div>
      </div>

    </div>
  )
}
