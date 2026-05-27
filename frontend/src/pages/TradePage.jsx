import { useState, useMemo } from 'react'
import './TradePage.css'
import {
  Chart as ChartJS, CategoryScale, LinearScale,
  PointElement, LineElement, Tooltip, Legend, Filler,
} from 'chart.js'
import { Line } from 'react-chartjs-2'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend, Filler)

const fmt = (n) => Number(n).toLocaleString('ko-KR')

const STOCK_OPTIONS = ['삼성전자', 'AAPL', 'SK하이닉스', '현대차', '카카오', '네이버', 'MSFT', 'NVDA', 'GOOGL', 'TSLA']
const SECTORS       = ['반도체', '기술', '자동차', '금융', '바이오', '소비재', '에너지', '기타']
const TRADE_TYPES   = ['매수', '매도', '배당']
const MONTHS        = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월']

const STOCK_SECTOR_MAP = {
  '삼성전자': '반도체', 'SK하이닉스': '반도체',
  'AAPL': '기술', 'MSFT': '기술', 'NVDA': '기술', 'GOOGL': '기술', 'TSLA': '기술',
  '카카오': '기술', '네이버': '기술', '현대차': '자동차',
}

const INITIAL_TRADES = [
  { id: 1, date: '2026-01-15', name: '삼성전자', sector: '반도체', type: '매수', quantity: 10, price: 68000,  fee: 3400 },
  { id: 2, date: '2026-02-25', name: 'NVDA',     sector: '기술',   type: '매수', quantity: 5,  price: 890000, fee: 22250 },
  { id: 3, date: '2026-03-05', name: 'SK하이닉스', sector: '반도체', type: '매수', quantity: 10, price: 132000, fee: 6600 },
  { id: 4, date: '2026-03-20', name: '현대차',   sector: '자동차', type: '매도', quantity: 3,  price: 215000, fee: 3225 },
  { id: 5, date: '2026-04-10', name: '삼성전자', sector: '반도체', type: '배당', quantity: 50, price: 1444,   fee: 0 },
  { id: 6, date: '2026-05-08', name: 'TSLA',     sector: '기술',   type: '매수', quantity: 2,  price: 385000, fee: 3850 },
  { id: 7, date: '2026-05-20', name: 'NVDA',     sector: '기술',   type: '매도', quantity: 3,  price: 920000, fee: 13800 },
  { id: 8, date: '2026-06-12', name: 'AAPL',     sector: '기술',   type: '매수', quantity: 3,  price: 183000, fee: 2745 },
]

const calcTotal = (t) => {
  if (t.type === '매수') return t.quantity * t.price + t.fee
  if (t.type === '매도') return t.quantity * t.price - t.fee
  return t.quantity * t.price
}

const typeClass = (t) => t === '매수' ? 'buy' : t === '매도' ? 'sell' : 'div'
const today = new Date().toISOString().split('T')[0]

export default function TradePage() {
  const [trades, setTrades]           = useState(INITIAL_TRADES)
  const [form, setForm]               = useState({ name: STOCK_OPTIONS[0], type: '매수', quantity: '', price: '', date: today, fee: '' })
  const [typeFilter, setTypeFilter]   = useState('전체')
  const [searchName, setSearchName]   = useState('')
  const [searchSector, setSearchSector] = useState('')

  /* ── 월별/합계 통계 (차트 + 요약) ── */
  const stats = useMemo(() => {
    const monthlyBuy  = Array(12).fill(0)
    const monthlySell = Array(12).fill(0)
    let totalBuy = 0, totalSell = 0

    trades.forEach(t => {
      const m = new Date(t.date).getMonth()
      const amt = t.quantity * t.price
      if (t.type === '매수') { monthlyBuy[m]  += amt; totalBuy  += amt }
      if (t.type === '매도') { monthlySell[m] += amt; totalSell += amt }
    })

    return { monthlyBuy, monthlySell, totalBuy, totalSell, totalCount: trades.length }
  }, [trades])

  /* ── 거래 등록 ── */
  const handleAdd = () => {
    if (!form.quantity || !form.price || !form.date) return
    setTrades(prev => [...prev, {
      id:       Date.now(),
      date:     form.date,
      name:     form.name,
      sector:   STOCK_SECTOR_MAP[form.name] ?? '기타',
      type:     form.type,
      quantity: Number(form.quantity),
      price:    Number(form.price),
      fee:      Number(form.fee) || 0,
    }].sort((a, b) => b.date.localeCompare(a.date)))
    setForm(f => ({ ...f, quantity: '', price: '', fee: '' }))
  }

  const handleDelete = (id) => {
    if (!window.confirm('거래 내역을 삭제하시겠습니까?')) return
    setTrades(prev => prev.filter(t => t.id !== id))
  }

  /* ── 필터링 ── */
  const filtered = useMemo(() => trades.filter(t => {
    if (typeFilter !== '전체' && t.type !== typeFilter) return false
    if (searchName && !t.name.toLowerCase().includes(searchName.toLowerCase())) return false
    if (searchSector && t.sector !== searchSector) return false
    return true
  }), [trades, typeFilter, searchName, searchSector])

  /* ── 차트 데이터 ── */
  const chartData = {
    labels: MONTHS,
    datasets: [
      {
        label: '매수금액',
        data: stats.monthlyBuy,
        borderColor: '#1D9E75',
        backgroundColor: 'rgba(29,158,117,0.08)',
        fill: true,
        tension: 0.4,
        borderWidth: 2,
        pointBackgroundColor: '#1D9E75',
        pointBorderColor: '#fff',
        pointBorderWidth: 2,
        pointRadius: 4,
        pointHoverRadius: 6,
      },
      {
        label: '매도금액',
        data: stats.monthlySell,
        borderColor: '#E24B4A',
        backgroundColor: 'rgba(226,75,74,0.04)',
        fill: false,
        tension: 0.4,
        borderWidth: 2,
        borderDash: [6, 3],
        pointBackgroundColor: '#E24B4A',
        pointBorderColor: '#fff',
        pointBorderWidth: 2,
        pointRadius: 4,
        pointHoverRadius: 6,
      },
    ],
  }

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: { font: { family: 'Outfit', size: 11 }, color: '#999', boxWidth: 12, padding: 12 },
      },
      tooltip: {
        callbacks: { label: ctx => ` ${fmt(ctx.raw)}원` },
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { font: { family: 'Outfit', size: 10 }, color: '#bbb' },
        border: { display: false },
      },
      y: {
        grid: { color: '#f4f4f4' },
        ticks: {
          font: { family: 'Outfit', size: 10 }, color: '#bbb',
          callback: v => v >= 1_000_000 ? (v / 1_000_000).toFixed(0) + 'M'
                       : v >= 1_000    ? (v / 1_000).toFixed(0) + 'K'
                       : v,
        },
        border: { display: false },
      },
    },
  }

  const formTotal = Math.round((Number(form.quantity) || 0) * (Number(form.price) || 0))

  return (
    <div className="tp-page">

      {/* ── 헤더 ── */}
      <div className="tp-header">
        <h1 className="tp-title">거래관리</h1>
        <p className="tp-subtitle">거래 내역을 확인하세요</p>
      </div>

      {/* ── 상단: 차트(60%) + 요약(40%) ── */}
      <div className="tp-top-row">

        <div className="tp-card tp-chart-card">
          <p className="tp-card-title">매수/매도 금액 추이</p>
          <div className="tp-chart-wrap">
            <Line data={chartData} options={chartOptions} />
          </div>
        </div>

        <div className="tp-card tp-summary-card">
          <p className="tp-card-title">거래 현황</p>
          <div className="tp-sum-items">
            <div className="tp-sum-item">
              <span className="tp-sum-label">총 매수금액</span>
              <span className="tp-sum-value buy">{fmt(stats.totalBuy)}<span className="tp-sum-unit">원</span></span>
            </div>
            <div className="tp-sum-item">
              <span className="tp-sum-label">총 매도금액</span>
              <span className="tp-sum-value sell">{fmt(stats.totalSell)}<span className="tp-sum-unit">원</span></span>
            </div>
            <div className="tp-sum-item">
              <span className="tp-sum-label">총 거래횟수</span>
              <span className="tp-sum-value count">{stats.totalCount}<span className="tp-sum-unit">건</span></span>
            </div>
          </div>
        </div>

      </div>

      {/* ── 중단: 거래 등록 폼 (한 줄) ── */}
      <div className="tp-card tp-form-section">
        <p className="tp-card-title">거래 등록</p>
        <div className="tp-form-row">

          <select className="tp-fi tp-fi-name" value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))}>
            {STOCK_OPTIONS.map(s => <option key={s}>{s}</option>)}
          </select>

          <div className="tp-type-tabs">
            {TRADE_TYPES.map(t => (
              <button
                key={t}
                className={`tp-type-tab${form.type === t ? ` active ${typeClass(t)}` : ''}`}
                onClick={() => setForm(f => ({ ...f, type: t }))}
              >
                {t}
              </button>
            ))}
          </div>

          <input className="tp-fi tp-fi-sm" type="number" placeholder="수량" value={form.quantity} onChange={e => setForm(f => ({ ...f, quantity: e.target.value }))} />
          <input className="tp-fi tp-fi-md" type="number" placeholder="단가 (원)" value={form.price} onChange={e => setForm(f => ({ ...f, price: e.target.value }))} />
          <input className="tp-fi tp-fi-date" type="date" value={form.date} onChange={e => setForm(f => ({ ...f, date: e.target.value }))} />
          <input className="tp-fi tp-fi-sm" type="number" placeholder="수수료" value={form.fee} onChange={e => setForm(f => ({ ...f, fee: e.target.value }))} />

          <div className="tp-form-total">
            <span className="tp-form-total-label">총액</span>
            <span className="tp-form-total-value">{formTotal > 0 ? fmt(formTotal) + '원' : '—'}</span>
          </div>

          <button className="tp-add-btn" onClick={handleAdd} disabled={!form.quantity || !form.price || !form.date}>
            등록
          </button>

        </div>
      </div>

      {/* ── 하단: 거래 내역 테이블 ── */}
      <div className="tp-table-section">

        <div className="tp-table-controls">
          <div className="tp-filters">
            {['전체', '매수', '매도', '배당'].map(f => (
              <button key={f} className={`tp-filter-tab${typeFilter === f ? ' active' : ''}`} onClick={() => setTypeFilter(f)}>{f}</button>
            ))}
          </div>
          <div className="tp-search">
            <input className="tp-search-input" placeholder="종목명 검색" value={searchName} onChange={e => setSearchName(e.target.value)} />
            <select className="tp-search-select" value={searchSector} onChange={e => setSearchSector(e.target.value)}>
              <option value="">섹터 전체</option>
              {SECTORS.map(s => <option key={s}>{s}</option>)}
            </select>
          </div>
        </div>

        <div className="tp-table-wrap">
          <table className="tp-table">
            <thead>
              <tr>
                <th>날짜</th>
                <th>종목명</th>
                <th className="center">유형</th>
                <th className="right">수량</th>
                <th className="right">단가</th>
                <th className="right">수수료</th>
                <th className="right">총액</th>
                <th className="center">삭제</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr><td colSpan={8} className="tp-empty">거래 내역이 없습니다.</td></tr>
              ) : filtered.map(t => {
                const total = calcTotal(t)
                return (
                  <tr key={t.id}>
                    <td className="tp-date">{t.date}</td>
                    <td className="tp-stock-name">{t.name}</td>
                    <td className="center">
                      <span className={`tp-type-badge ${typeClass(t.type)}`}>{t.type}</span>
                    </td>
                    <td className="right">{fmt(t.quantity)}</td>
                    <td className="right">{fmt(t.price)}</td>
                    <td className="right">{t.fee > 0 ? fmt(t.fee) : '—'}</td>
                    <td className={`right tp-total ${t.type === '매수' ? 'outflow' : 'inflow'}`}>
                      {t.type === '매수' ? '-' : '+'}{fmt(total)}
                    </td>
                    <td className="center">
                      <button className="tp-btn-delete" onClick={() => handleDelete(t.id)}>삭제</button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>

      </div>

    </div>
  )
}
