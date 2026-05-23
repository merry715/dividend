import { useState, useMemo } from 'react'
import './DividendPage.css'
import {
  Chart as ChartJS, CategoryScale, LinearScale,
  BarElement, Tooltip, Legend,
} from 'chart.js'
import { Bar } from 'react-chartjs-2'

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip, Legend)

const fmt = (n) => Number(n).toLocaleString('ko-KR')

const MONTHLY_DATA = [
  { month: 1,  label: '1월',  expected: 72000,  confirmed: 75000, isConfirmed: true  },
  { month: 2,  label: '2월',  expected: 45000,  confirmed: 48000, isConfirmed: true  },
  { month: 3,  label: '3월',  expected: 95000,  confirmed: 91000, isConfirmed: true  },
  { month: 4,  label: '4월',  expected: 62000,  confirmed: 65000, isConfirmed: true  },
  { month: 5,  label: '5월',  expected: 38000,  confirmed: null,  isConfirmed: false },
  { month: 6,  label: '6월',  expected: 112000, confirmed: null,  isConfirmed: false },
  { month: 7,  label: '7월',  expected: 28000,  confirmed: null,  isConfirmed: false },
  { month: 8,  label: '8월',  expected: 55000,  confirmed: null,  isConfirmed: false },
  { month: 9,  label: '9월',  expected: 88000,  confirmed: null,  isConfirmed: false },
  { month: 10, label: '10월', expected: 42000,  confirmed: null,  isConfirmed: false },
  { month: 11, label: '11월', expected: 66000,  confirmed: null,  isConfirmed: false },
  { month: 12, label: '12월', expected: 120000, confirmed: null,  isConfirmed: false },
]

const INITIAL_DIVIDENDS = [
  { id: 1, name: '삼성전자',   perShare: 1444, payMonth: '4월', exDivDate: '2026-03-29', status: '확정', expectedTotal: 72200 },
  { id: 2, name: 'NVDA',       perShare: 1200, payMonth: '6월', exDivDate: '2026-05-28', status: '예상', expectedTotal: 6000  },
  { id: 3, name: 'SK하이닉스', perShare: 3000, payMonth: '4월', exDivDate: '2026-03-29', status: '확정', expectedTotal: 30000 },
  { id: 4, name: 'AAPL',       perShare: 800,  payMonth: '8월', exDivDate: '2026-08-07', status: '예상', expectedTotal: 2400  },
  { id: 5, name: 'TSLA',       perShare: 500,  payMonth: '6월', exDivDate: '2026-05-28', status: '예상', expectedTotal: 1000  },
]

const today = new Date().toISOString().split('T')[0]

export default function DividendPage() {
  const [dividends, setDividends] = useState(INITIAL_DIVIDENDS)
  const [convertForm, setConvertForm] = useState({ stockId: '', payDate: today, amount: '' })

  const annualExpected = useMemo(() =>
    MONTHLY_DATA.reduce((s, m) => s + m.expected, 0), [])

  const accumulated = useMemo(() =>
    MONTHLY_DATA.filter(m => m.isConfirmed).reduce((s, m) => s + (m.confirmed ?? 0), 0), [])

  const handleConvert = () => {
    if (!convertForm.stockId || !convertForm.amount) return
    setDividends(prev =>
      prev.map(d =>
        d.id === Number(convertForm.stockId)
          ? { ...d, status: '확정', expectedTotal: Number(convertForm.amount) }
          : d
      )
    )
    setConvertForm(f => ({ ...f, stockId: '', amount: '' }))
  }

  const barData = {
    labels: ['2023', '2024', '2025', '2026'],
    datasets: [{
      label: '연간 배당금',
      data: [480000, 624000, 852000, annualExpected],
      backgroundColor: ['#C8EFE3', '#9FE1CB', '#5DCAA5', 'rgba(29,158,117,0.72)'],
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
    <div className="dp-page">

      {/* ── 헤더 ── */}
      <div className="dp-header">
        <h1 className="dp-title">배당관리</h1>
        <p className="dp-subtitle">배당 수익 현황을 확인하세요</p>
      </div>

      {/* ── 요약 카드 2개 ── */}
      <div className="dp-summary-row">
        <div className="dp-card dp-sum-card">
          <p className="dp-sum-label">2026 연간 예상 배당금</p>
          <p className="dp-sum-value">{fmt(annualExpected)}<span className="dp-sum-unit">원</span></p>
          <p className="dp-sum-sub">12개월 예상 합계</p>
        </div>
        <div className="dp-card dp-sum-card">
          <p className="dp-sum-label">누적 배당금</p>
          <p className="dp-sum-value">{fmt(accumulated)}<span className="dp-sum-unit">원</span></p>
          <p className="dp-sum-sub">확정 배당 합계 · 4개월</p>
        </div>
      </div>

      {/* ── 월별 배당 조회 ── */}
      <div className="dp-card dp-monthly-section">
        <p className="dp-card-title">월별 배당 조회</p>
        <div className="dp-monthly-grid">
          {MONTHLY_DATA.map(m => (
            <div key={m.month} className={`dp-month-card${m.isConfirmed ? ' confirmed' : ''}`}>
              <div className="dp-month-header">
                <span className="dp-month-name">{m.label}</span>
                <span className={`dp-month-badge ${m.isConfirmed ? 'confirmed' : 'expected'}`}>
                  {m.isConfirmed ? '확정' : '예상'}
                </span>
              </div>
              <div className="dp-month-amounts">
                <div className="dp-month-row">
                  <span className="dp-month-row-label">예상</span>
                  <span className="dp-month-row-val">{fmt(m.expected)}</span>
                </div>
                <div className="dp-month-row">
                  <span className="dp-month-row-label confirmed">확정</span>
                  <span className={`dp-month-row-val${m.isConfirmed ? ' confirmed' : ' na'}`}>
                    {m.isConfirmed ? fmt(m.confirmed) : '—'}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ── 연도별 배당 차트 (전체 너비) ── */}
      <div className="dp-card dp-bar-card">
        <p className="dp-card-title">연도별 배당</p>
        <div className="dp-bar-wrap">
          <Bar data={barData} options={barOptions} />
        </div>
      </div>

      {/* ── 확정 배당 전환 폼 (전체 너비 한 줄) ── */}
      <div className="dp-card dp-convert-card">
        <p className="dp-card-title">확정 배당 전환</p>
        <div className="dp-convert-row">
          <select
            className="dp-fi dp-fi-select"
            value={convertForm.stockId}
            onChange={e => setConvertForm(f => ({ ...f, stockId: e.target.value }))}
          >
            <option value="">종목 선택</option>
            {dividends.filter(d => d.status === '예상').map(d => (
              <option key={d.id} value={d.id}>{d.name}</option>
            ))}
          </select>
          <input
            className="dp-fi dp-fi-date"
            type="date"
            value={convertForm.payDate}
            onChange={e => setConvertForm(f => ({ ...f, payDate: e.target.value }))}
          />
          <input
            className="dp-fi dp-fi-amount"
            type="number"
            placeholder="실제 배당금 (원)"
            value={convertForm.amount}
            onChange={e => setConvertForm(f => ({ ...f, amount: e.target.value }))}
          />
          <button
            className="dp-convert-cancel"
            onClick={() => setConvertForm({ stockId: '', payDate: today, amount: '' })}
          >취소</button>
          <button
            className="dp-convert-save"
            onClick={handleConvert}
            disabled={!convertForm.stockId || !convertForm.amount}
          >저장</button>
        </div>
      </div>

      {/* ── 하단: 종목별 배당 정보 테이블 (전체 너비) ── */}
      <div className="dp-card dp-stock-card">
        <p className="dp-card-title">종목별 배당 정보</p>
        <div className="dp-stock-table-wrap">
          <table className="dp-stock-table">
            <thead>
              <tr>
                <th>종목명</th>
                <th className="right">주당 배당금</th>
                <th className="center">지급월</th>
                <th>배당락일</th>
                <th className="center">상태</th>
                <th className="right">예상 배당금</th>
              </tr>
            </thead>
            <tbody>
              {dividends.map(d => (
                <tr key={d.id}>
                  <td className="dp-stock-name">{d.name}</td>
                  <td className="right">{fmt(d.perShare)}원</td>
                  <td className="center">{d.payMonth}</td>
                  <td className="dp-date">{d.exDivDate}</td>
                  <td className="center">
                    <span className={`dp-status-badge ${d.status === '확정' ? 'confirmed' : 'expected'}`}>
                      {d.status}
                    </span>
                  </td>
                  <td className="right dp-expected-total">{fmt(d.expectedTotal)}원</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

    </div>
  )
}
