import { useState, useEffect, useCallback } from 'react'
import './AnalysisPage.css'
import {
  Chart as ChartJS,
  ArcElement,
  CategoryScale, LinearScale, BarElement,
  Tooltip, Legend,
} from 'chart.js'
import { Doughnut, Bar } from 'react-chartjs-2'
import {
  getSummary, getStockWeight, getSectorWeight,
  getDividendHistory, getGoal, saveGoal,
} from '../api/analysis'
import { getStocks } from '../api/stocks'
import { getCumulative } from '../api/dividend'

ChartJS.register(ArcElement, CategoryScale, LinearScale, BarElement, Tooltip, Legend)

const fmt = (n) => Number(n).toLocaleString('ko-KR')
const WEIGHT_COLORS = ['#1D9E75', '#5DCAA5', '#9FE1CB', '#B8E4D4', '#DDF0E9', '#C8EFE3']

export default function AnalysisPage() {
  const [summary, setSummary]               = useState(null)
  const [stocks, setStocks]                 = useState([])
  const [cumulativeAmount, setCumulativeAmount] = useState(0)
  const [stockWeights, setStockWeights]     = useState([])
  const [sectorWeights, setSectorWeights]   = useState([])
  const [dividendHistory, setDividendHistory] = useState([])
  const [goal, setGoal]                     = useState(null)
  const [goalId, setGoalId]                 = useState(null)
  const [loading, setLoading]               = useState(true)
  const [saving, setSaving]                 = useState(false)
  const [weightTab, setWeightTab]           = useState('stock')
  const [goalInput, setGoalInput]           = useState('')
  const [error, setError]                   = useState(null)
  const [toast, setToast]                   = useState({ msg: '', show: false })

  function showToast(msg) {
    setToast({ msg, show: true })
    setTimeout(() => setToast(t => ({ ...t, show: false })), 2600)
  }

  const loadAll = useCallback(async () => {
    setError(null)
    try {
      const [summaryRes, stockRes, sectorRes, historyRes, goalRes, stocksRes, cumulativeRes] = await Promise.all([
        getSummary(),
        getStockWeight(),
        getSectorWeight(),
        getDividendHistory(),
        getGoal(),
        getStocks(),
        getCumulative(),
      ])
      setSummary(summaryRes.data.data)
      setStocks(stocksRes.data.data ?? [])
      setCumulativeAmount(cumulativeRes.data.data?.totalConfirmedAmount ?? 0)
      setStockWeights((stockRes.data.data ?? []).map((w, i) => ({
        ...w, color: WEIGHT_COLORS[i] ?? '#C8EFE3',
      })))
      setSectorWeights((sectorRes.data.data ?? []).map((w, i) => ({
        ...w, color: WEIGHT_COLORS[i] ?? '#C8EFE3',
      })))
      setDividendHistory([...(historyRes.data.data ?? [])].sort((a, b) => a.year - b.year))
      const g = goalRes.data.data
      setGoal(g)
      setGoalInput(String(g?.targetDividend ?? ''))
      if (g?.id) setGoalId(g.id)
    } catch (e) {
      console.error('분석 데이터 로딩 실패', e)
      setError('분석 데이터를 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadAll() }, [loadAll])

  const handleSaveGoal = async () => {
    const num = Number(String(goalInput).replace(/,/g, ''))
    setSaving(true)
    try {
      const saveRes = await saveGoal(num)
      const res = await getGoal()
      const g = res.data.data
      setGoal(g)
      if (g?.id) setGoalId(g.id)
      showToast(saveRes.data?.message || '목표가 저장되었습니다.')
    } catch (e) {
      console.error('목표 저장 실패', e)
      showToast(e.uiMessage)
    } finally {
      setSaving(false)
    }
  }

  // 비중 리스트 — 탭에 따라 stockName / sector 필드 통일
  const activeWeights = (weightTab === 'stock' ? stockWeights : sectorWeights).map((w, i) => ({
    rank:   i + 1,
    name:   w.stockName ?? w.sectorLabel,
    weight: w.weightPercent,
    color:  w.color,
  }))

  const donutData = {
    labels: activeWeights.map(w => w.name),
    datasets: [{
      data:            activeWeights.map(w => w.weight),
      backgroundColor: activeWeights.map(w => w.color),
      borderColor: '#fff',
      borderWidth: 2,
      hoverOffset: 6,
    }],
  }

  const donutOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: { callbacks: { label: ctx => ` ${ctx.label}: ${ctx.parsed}%` } },
    },
    cutout: '64%',
  }

  const barData = {
    labels: dividendHistory.map(y => String(y.year)),
    datasets: [{
      label: '연간 배당금',
      data:  dividendHistory.map(y => y.totalDividend),
      backgroundColor: dividendHistory.map((_, i, arr) =>
        i === arr.length - 1 ? 'rgba(29,158,117,0.75)'
        : i === arr.length - 2 ? '#5DCAA5'
        : i === arr.length - 3 ? '#9FE1CB'
        : '#C8EFE3'
      ),
      borderColor: dividendHistory.map((_, i, arr) =>
        i >= arr.length - 2 ? '#1D9E75'
        : i === arr.length - 3 ? '#5DCAA5'
        : '#9FE1CB'
      ),
      borderWidth: 2,
      borderRadius: 8,
      borderSkipped: false,
    }],
  }

  const barOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: { callbacks: { label: ctx => ` ${fmt(ctx.raw)}원` } },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { font: { family: 'Outfit', size: 11 }, color: '#bbb' },
        border: { display: false },
      },
      y: {
        grid: { color: '#f4f4f4' },
        ticks: {
          font: { family: 'Outfit', size: 10 }, color: '#bbb',
          callback: v =>
            v >= 1_000_000 ? (v / 1_000_000).toFixed(1) + 'M'
            : v >= 1_000   ? (v / 1_000).toFixed(0)     + 'K'
            : v,
        },
        border: { display: false },
      },
    },
  }

  // 파생 값
  const progressPct    = goal ? Math.min(100, Math.floor(goal.achievementRate)) : 0
  const monthsToGoal   = goal?.estimatedMonthsToGoal ?? 0
  const totalEvalAmount = stocks.reduce((sum, s) => sum + Number(s.evaluationAmount ?? 0), 0)
  const pnl             = stocks.reduce((sum, s) => sum + Number(s.evaluationProfit ?? 0), 0)
  const totalInvestment = Number(summary?.totalInvestment ?? 0)
  const pnlPct = totalInvestment > 0
    ? ((pnl / totalInvestment) * 100).toFixed(2)
    : '0.00'
  const dividendYield = totalInvestment > 0
    ? ((summary.totalExpectedDividend / totalInvestment) * 100).toFixed(1)
    : '0.0'
  const lastTwo   = dividendHistory.slice(-2)
  const yoyGrowth = lastTwo.length === 2 && lastTwo[0].totalDividend > 0
    ? (((lastTwo[1].totalDividend / lastTwo[0].totalDividend) - 1) * 100).toFixed(1)
    : null

  if (loading) return (
    <div className="ap-page ap-loading">
      <p>분석 데이터를 불러오는 중...</p>
    </div>
  )

  if (error) return (
    <div className="ap-page ap-status">
      <p className="ap-status-msg">{error}</p>
      <button className="ap-retry-btn" onClick={() => { setLoading(true); loadAll() }}>다시 시도</button>
    </div>
  )

  if (stocks.length === 0) return (
    <div className="ap-page ap-status">
      <p className="ap-status-msg">아직 보유 종목이 없습니다.</p>
      <p className="ap-status-sub">종목 관리에서 종목을 먼저 추가해 주세요.</p>
    </div>
  )

  return (
    <div className="ap-page">

      {/* ── 헤더 ── */}
      <div className="ap-header">
        <h1 className="ap-title">분석</h1>
        <p className="ap-subtitle">포트폴리오 성과를 분석하세요</p>
      </div>

      {/* ── 연간 목표·달성률 카드 ── */}
      <div className="ap-card ap-goal-card">

        <div className="ap-goal-left">
          <div className="ap-goal-amounts">
            <div className="ap-goal-item">
              <span className="ap-goal-item-label">예상 배당금</span>
              <span className="ap-goal-item-val">
                {fmt(goal?.currentDividend ?? 0)}<span className="ap-goal-item-unit">원</span>
              </span>
            </div>
            <span className="ap-goal-sep">/</span>
            <div className="ap-goal-item">
              <span className="ap-goal-item-label">목표 배당금</span>
              <span className="ap-goal-item-val accent">
                {fmt(goal?.targetDividend ?? 0)}<span className="ap-goal-item-unit">원</span>
              </span>
            </div>
          </div>

          <div className="ap-goal-bar-row">
            <div className="ap-goal-bar-bg">
              <div className="ap-goal-bar-fill" style={{ width: `${progressPct}%` }} />
            </div>
            <span className="ap-goal-pct">{progressPct}%</span>
          </div>

          <p className="ap-goal-eta">
            {progressPct >= 100
              ? '연간 목표를 달성했습니다!'
              : !goal?.targetDividend
              ? '목표를 설정해 주세요'
              : `달성 예상 기간: 약 ${monthsToGoal}개월 후`}
          </p>
        </div>

        <div className="ap-goal-divider" />

        <div className="ap-goal-right">
          <span className="ap-goal-input-label">연간 목표 금액</span>
          <div className="ap-goal-input-row">
            <input
              className="ap-goal-input"
              type="number"
              placeholder="목표 금액 (원)"
              value={goalInput}
              onChange={e => setGoalInput(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleSaveGoal()}
            />
            <button
              className="ap-goal-save-btn"
              onClick={handleSaveGoal}
              disabled={saving}
            >{saving ? '저장 중...' : '저장'}</button>
          </div>
        </div>

      </div>

      {/* ── 포트폴리오 요약 카드 4개 ── */}
      <div className="ap-portfolio-row">

        <div className="ap-card ap-port-card">
          <p className="ap-port-label">총 투자금</p>
          <p className="ap-port-value">{fmt(totalInvestment)}<span className="ap-port-unit">원</span></p>
          <p className="ap-port-sub">{summary?.stockCount ?? 0}개 종목</p>
        </div>

        <div className="ap-card ap-port-card">
          <p className="ap-port-label">평가금액</p>
          <p className="ap-port-value">{fmt(totalEvalAmount)}<span className="ap-port-unit">원</span></p>
          <p className={`ap-port-sub ${pnl >= 0 ? 'profit' : 'loss'}`}>
            {pnl >= 0 ? '+' : ''}{fmt(pnl)}원 ({pnl >= 0 ? '+' : ''}{pnlPct}%)
          </p>
        </div>

        <div className="ap-card ap-port-card">
          <p className="ap-port-label">배당수익률</p>
          <p className="ap-port-value">{dividendYield}<span className="ap-port-unit">%</span></p>
        </div>

        <div className="ap-card ap-port-card">
          <p className="ap-port-label">누적 배당</p>
          <p className="ap-port-value">{fmt(cumulativeAmount)}<span className="ap-port-unit">원</span></p>
        </div>

      </div>

      {/* ── 하단 행 ── */}
      <div className="ap-bottom-row">

        {/* 종목·섹터 비중 */}
        <div className="ap-card ap-weight-card">
          <div className="ap-weight-head">
            <p className="ap-card-title">종목·섹터 비중</p>
            <div className="ap-weight-tabs">
              <button
                className={`ap-weight-tab${weightTab === 'stock' ? ' active' : ''}`}
                onClick={() => setWeightTab('stock')}
              >종목별</button>
              <button
                className={`ap-weight-tab${weightTab === 'sector' ? ' active' : ''}`}
                onClick={() => setWeightTab('sector')}
              >섹터별</button>
            </div>
          </div>

          <div className="ap-weight-body">
            <div className="ap-donut-wrap">
              <Doughnut data={donutData} options={donutOptions} />
            </div>
            <div className="ap-weight-list">
              {activeWeights.map(w => (
                <div key={w.rank} className="ap-weight-item">
                  <span className="ap-weight-rank">{w.rank}</span>
                  <span className="ap-weight-dot" style={{ background: w.color }} />
                  <span className="ap-weight-name">{w.name}</span>
                  <div className="ap-weight-bar-bg">
                    <div className="ap-weight-bar-fill" style={{ width: `${w.weight}%`, background: w.color }} />
                  </div>
                  <span className="ap-weight-pct">{w.weight}%</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* 연도별 배당 비교 */}
        <div className="ap-card ap-yearly-card">
          <div className="ap-yearly-head">
            <p className="ap-card-title">연도별 비교</p>
            {yoyGrowth !== null && (
              <span
                className="ap-yearly-growth"
                style={Number(yoyGrowth) < 0 ? { color: '#E24B4A' } : undefined}
              >
                전년 대비 {Number(yoyGrowth) >= 0 ? `+${yoyGrowth}` : yoyGrowth}% {Number(yoyGrowth) >= 0 ? '증가' : '감소'}
              </span>
            )}
          </div>
          <div className="ap-bar-wrap">
            <Bar data={barData} options={barOptions} />
          </div>
        </div>

      </div>

      <div className={`ap-toast${toast.show ? ' show' : ''}`}>{toast.msg}</div>

    </div>
  )
}
