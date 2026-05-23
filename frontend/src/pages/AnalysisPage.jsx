import { useState } from 'react'
import './AnalysisPage.css'
import {
  Chart as ChartJS,
  ArcElement,
  CategoryScale, LinearScale, BarElement,
  Tooltip, Legend,
} from 'chart.js'
import { Doughnut, Bar } from 'react-chartjs-2'

ChartJS.register(ArcElement, CategoryScale, LinearScale, BarElement, Tooltip, Legend)

const fmt = (n) => Number(n).toLocaleString('ko-KR')

const ANNUAL_EXPECTED  = 823_000
const ACCUMULATED      = 279_000
const TOTAL_INVESTED   = 7_240_000
const EVAL_AMOUNT      = 8_150_000
const DIVIDEND_YIELD   = 3.8
const PNL              = EVAL_AMOUNT - TOTAL_INVESTED
const PNL_PCT          = ((PNL / TOTAL_INVESTED) * 100).toFixed(2)

const YEARLY_VALUES    = [420_000, 568_000, 732_000, ANNUAL_EXPECTED]
const YOY_GROWTH       = (((ANNUAL_EXPECTED / YEARLY_VALUES[2]) - 1) * 100).toFixed(1)

const STOCK_WEIGHTS = [
  { rank: 1, name: '삼성전자',   weight: 32.5, color: '#1D9E75' },
  { rank: 2, name: 'NVDA',       weight: 24.8, color: '#5DCAA5' },
  { rank: 3, name: 'SK하이닉스', weight: 18.2, color: '#9FE1CB' },
  { rank: 4, name: 'AAPL',       weight: 14.1, color: '#B8E4D4' },
  { rank: 5, name: 'TSLA',       weight: 10.4, color: '#DDF0E9' },
]

const SECTOR_WEIGHTS = [
  { rank: 1, name: '반도체', weight: 50.7, color: '#1D9E75' },
  { rank: 2, name: '기술',   weight: 35.5, color: '#5DCAA5' },
  { rank: 3, name: '자동차', weight: 8.9,  color: '#9FE1CB' },
  { rank: 4, name: '기타',   weight: 4.9,  color: '#C8EFE3' },
]

export default function AnalysisPage() {
  const [weightTab, setWeightTab] = useState('stock')
  const [goalInput, setGoalInput] = useState('1000000')
  const [savedGoal, setSavedGoal] = useState(1_000_000)

  const progressPct   = Math.min(100, Math.round((ANNUAL_EXPECTED / savedGoal) * 100))
  const monthsToGoal  = progressPct >= 100 ? 0 : Math.ceil((savedGoal - ANNUAL_EXPECTED) / (ANNUAL_EXPECTED / 12))
  const goalAccPct    = Math.round((ACCUMULATED / savedGoal) * 100)

  const handleSaveGoal = () => {
    const num = Number(String(goalInput).replace(/,/g, ''))
    if (num > 0) setSavedGoal(num)
  }

  const activeWeights = weightTab === 'stock' ? STOCK_WEIGHTS : SECTOR_WEIGHTS

  const donutData = {
    labels: activeWeights.map(w => w.name),
    datasets: [{
      data: activeWeights.map(w => w.weight),
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
    labels: ['2023', '2024', '2025', '2026'],
    datasets: [{
      label: '연간 배당금',
      data: YEARLY_VALUES,
      backgroundColor: ['#C8EFE3', '#9FE1CB', '#5DCAA5', 'rgba(29,158,117,0.75)'],
      borderColor:     ['#9FE1CB', '#5DCAA5', '#1D9E75', '#1D9E75'],
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
                {fmt(ANNUAL_EXPECTED)}<span className="ap-goal-item-unit">원</span>
              </span>
            </div>
            <span className="ap-goal-sep">/</span>
            <div className="ap-goal-item">
              <span className="ap-goal-item-label">목표 배당금</span>
              <span className="ap-goal-item-val accent">
                {fmt(savedGoal)}<span className="ap-goal-item-unit">원</span>
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
            <button className="ap-goal-save-btn" onClick={handleSaveGoal}>저장</button>
          </div>
        </div>

      </div>

      {/* ── 포트폴리오 요약 카드 4개 ── */}
      <div className="ap-portfolio-row">

        <div className="ap-card ap-port-card">
          <p className="ap-port-label">총 투자금</p>
          <p className="ap-port-value">{fmt(TOTAL_INVESTED)}<span className="ap-port-unit">원</span></p>
          <p className={`ap-port-sub ${PNL >= 0 ? 'profit' : 'loss'}`}>
            {PNL >= 0 ? '+' : ''}{fmt(PNL)}원
          </p>
        </div>

        <div className="ap-card ap-port-card">
          <p className="ap-port-label">평가금액</p>
          <p className="ap-port-value">{fmt(EVAL_AMOUNT)}<span className="ap-port-unit">원</span></p>
          <p className={`ap-port-sub ${PNL >= 0 ? 'profit' : 'loss'}`}>
            {PNL >= 0 ? '+' : ''}{PNL_PCT}% 수익
          </p>
        </div>

        <div className="ap-card ap-port-card">
          <p className="ap-port-label">배당수익률</p>
          <p className="ap-port-value">{DIVIDEND_YIELD.toFixed(1)}<span className="ap-port-unit">%</span></p>
          <p className="ap-port-sub profit">시장평균 대비 +1.7%p</p>
        </div>

        <div className="ap-card ap-port-card">
          <p className="ap-port-label">누적 배당</p>
          <p className="ap-port-value">{fmt(ACCUMULATED)}<span className="ap-port-unit">원</span></p>
          <p className="ap-port-sub neutral">목표 달성률 {goalAccPct}%</p>
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
            <span className="ap-yearly-growth">전년 대비 +{YOY_GROWTH}% 증가</span>
          </div>
          <div className="ap-bar-wrap">
            <Bar data={barData} options={barOptions} />
          </div>
        </div>

      </div>

    </div>
  )
}
