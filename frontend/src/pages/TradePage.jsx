import { useState, useEffect, useMemo } from 'react'
import './TradePage.css'
import {
  Chart as ChartJS, CategoryScale, LinearScale,
  PointElement, LineElement, Tooltip, Legend, Filler,
} from 'chart.js'
import { Line } from 'react-chartjs-2'
import { getStocks } from '../api/stocks'
import {
  getTransactions,
  createTransaction,
  updateTransaction,
  deleteTransaction,
  getTransactionsSummary,
  getTransactionsChart,
} from '../api/transactions'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend, Filler)

const fmt = (n) => Number(n || 0).toLocaleString('ko-KR')

const MONTHS      = ['1월','2월','3월','4월','5월','6월','7월','8월','9월','10월','11월','12월']
const TODAY       = new Date().toISOString().split('T')[0]
const CURRENT_YEAR = new Date().getFullYear()

const TYPE_LABEL = { BUY: '매수', SELL: '매도' }
const TYPE_API   = { '매수': 'BUY', '매도': 'SELL' }
const typeClass  = (t) => (t === '매수' || t === 'BUY') ? 'buy' : 'sell'

const EMPTY_SUMMARY = { totalTransactions: 0, totalBuyAmount: 0, totalSellAmount: 0 }
const EMPTY_CHART   = Array(12).fill(0)

export default function TradePage() {
  const [stocks, setStocks]                   = useState([])
  const [trades, setTrades]                   = useState([])
  const [summary, setSummary]                 = useState(EMPTY_SUMMARY)
  const [chartBuy, setChartBuy]               = useState(EMPTY_CHART)
  const [chartSell, setChartSell]             = useState(EMPTY_CHART)
  const [loading, setLoading]                 = useState(true)
  const [filterType, setFilterType]           = useState('전체')
  const [searchName, setSearchName]           = useState('')
  const [editModal, setEditModal]             = useState(null)
  const [editForm, setEditForm]               = useState({})
  const [form, setForm]                       = useState({
    stockId: '', type: '매수',
    quantity: '', price: '', date: TODAY,
    brokerFee: '', transactionTax: '',
  })

  /* stockId → stockName 조회 테이블 */
  const stockMap = useMemo(
    () => Object.fromEntries(stocks.map(s => [s.id, s.stockName])),
    [stocks]
  )

  const fetchAll = async () => {
    setLoading(true)
    try {
      const [stocksRes, tradesRes, summaryRes, chartRes] = await Promise.all([
        getStocks(),
        getTransactions(),
        getTransactionsSummary(),
        getTransactionsChart(CURRENT_YEAR),
      ])

      const stockList = stocksRes.data.data ?? []
      setStocks(stockList)
      // 종목이 있고 현재 선택된 종목이 없으면 첫 번째로 설정
      if (stockList.length > 0) {
        setForm(f => ({ ...f, stockId: f.stockId || String(stockList[0].id) }))
      }

      setTrades(tradesRes.data.data ?? [])
      setSummary(summaryRes.data.data ?? EMPTY_SUMMARY)

      const chartItems = chartRes.data.data?.data ?? []
      const buyArr  = Array(12).fill(0)
      const sellArr = Array(12).fill(0)
      chartItems.forEach(d => {
        buyArr[d.month - 1]  = d.buyAmount  ?? 0
        sellArr[d.month - 1] = d.sellAmount ?? 0
      })
      setChartBuy(buyArr)
      setChartSell(sellArr)
    } catch {
      // 오류 시 빈 상태 유지
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchAll() }, [])

  /* ── 거래 등록 ── */
  const handleAdd = async () => {
    if (!form.stockId || !form.quantity || !form.price || !form.date) return
    try {
      await createTransaction({
        stockId:         Number(form.stockId),
        type:            TYPE_API[form.type],
        quantity:        Number(form.quantity),
        price:           Number(form.price),
        date:            form.date,
        brokerFee:       Number(form.brokerFee) || 0,
        transactionTax:  Number(form.transactionTax) || 0,
      })
      setForm(f => ({ ...f, quantity: '', price: '', brokerFee: '', transactionTax: '' }))
      await fetchAll()
    } catch (err) {
      alert(err.response?.data?.message ?? '거래 등록에 실패했습니다')
    }
  }

  /* ── 수정 모달 ── */
  const openEditModal = (trade) => {
    setEditModal(trade)
    setEditForm({
      stockId:        String(trade.stockId),
      type:           TYPE_LABEL[trade.type] ?? trade.type,
      quantity:       trade.quantity,
      price:          trade.price,
      date:           trade.date,
      brokerFee:      trade.brokerFee ?? 0,
      transactionTax: trade.transactionTax ?? 0,
    })
  }

  const saveEdit = async () => {
    try {
      await updateTransaction(editModal.id, {
        stockId:        Number(editForm.stockId),
        type:           TYPE_API[editForm.type],
        quantity:       Number(editForm.quantity),
        price:          Number(editForm.price),
        date:           editForm.date,
        brokerFee:      Number(editForm.brokerFee) || 0,
        transactionTax: Number(editForm.transactionTax) || 0,
      })
      setEditModal(null)
      await fetchAll()
    } catch (err) {
      alert(err.response?.data?.message ?? '수정에 실패했습니다')
    }
  }

  /* ── 삭제 ── */
  const handleDelete = async (id) => {
    if (!window.confirm('거래 내역을 삭제하시겠습니까?')) return
    try {
      await deleteTransaction(id)
      await fetchAll()
    } catch (err) {
      alert(err.response?.data?.message ?? '삭제에 실패했습니다')
    }
  }

  /* ── 필터링 (클라이언트) ── */
  const filtered = useMemo(() => trades.filter(t => {
    const label = TYPE_LABEL[t.type] ?? t.type
    if (filterType !== '전체' && label !== filterType) return false
    if (searchName) {
      const name = stockMap[t.stockId] ?? ''
      if (!name.toLowerCase().includes(searchName.toLowerCase())) return false
    }
    return true
  }), [trades, filterType, searchName, stockMap])

  /* ── 차트 ── */
  const chartData = {
    labels: MONTHS,
    datasets: [
      {
        label: '매수금액',
        data: chartBuy,
        borderColor: '#1D9E75',
        backgroundColor: 'rgba(29,158,117,0.08)',
        fill: true, tension: 0.4, borderWidth: 2,
        pointBackgroundColor: '#1D9E75', pointBorderColor: '#fff',
        pointBorderWidth: 2, pointRadius: 4, pointHoverRadius: 6,
      },
      {
        label: '매도금액',
        data: chartSell,
        borderColor: '#E24B4A',
        backgroundColor: 'rgba(226,75,74,0.04)',
        fill: false, tension: 0.4, borderWidth: 2, borderDash: [6, 3],
        pointBackgroundColor: '#E24B4A', pointBorderColor: '#fff',
        pointBorderWidth: 2, pointRadius: 4, pointHoverRadius: 6,
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
      tooltip: { callbacks: { label: ctx => ` ${fmt(ctx.raw)}원` } },
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
        <h1 className="tp-title">거래 관리</h1>
        <p className="tp-subtitle">거래 내역을 확인하세요</p>
      </div>

      {/* ── 상단: 차트(60%) + 요약(40%) ── */}
      <div className="tp-top-row">

        <div className="tp-card tp-chart-card">
          <p className="tp-card-title">매수/매도 금액 추이</p>
          <div className="tp-chart-wrap">
            {loading ? (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: '#bbb' }}>
                로딩 중...
              </div>
            ) : (
              <Line data={chartData} options={chartOptions} />
            )}
          </div>
        </div>

        <div className="tp-card tp-summary-card">
          <p className="tp-card-title">거래 현황</p>
          <div className="tp-sum-items">
            <div className="tp-sum-item">
              <span className="tp-sum-label">총 매수금액</span>
              <span className="tp-sum-value buy">
                {fmt(summary.totalBuyAmount)}<span className="tp-sum-unit">원</span>
              </span>
            </div>
            <div className="tp-sum-item">
              <span className="tp-sum-label">총 매도금액</span>
              <span className="tp-sum-value sell">
                {fmt(summary.totalSellAmount)}<span className="tp-sum-unit">원</span>
              </span>
            </div>
            <div className="tp-sum-item">
              <span className="tp-sum-label">총 거래횟수</span>
              <span className="tp-sum-value count">
                {summary.totalTransactions}<span className="tp-sum-unit">건</span>
              </span>
            </div>
          </div>
        </div>

      </div>

      {/* ── 중단: 거래 등록 폼 ── */}
      <div className="tp-card tp-form-section">
        <p className="tp-card-title">거래 등록</p>
        {!loading && stocks.length === 0 ? (
          <p style={{ color: '#bbb', fontSize: 13 }}>종목 관리 페이지에서 종목을 먼저 추가해주세요</p>
        ) : (
          <div className="tp-form-row">

            <select
              className="tp-fi tp-fi-name"
              value={form.stockId}
              onChange={e => setForm(f => ({ ...f, stockId: e.target.value }))}
            >
              {stocks.length === 0
                ? <option value="">종목 로딩 중...</option>
                : stocks.map(s => <option key={s.id} value={s.id}>{s.stockName}</option>)
              }
            </select>

            <div className="tp-type-tabs">
              {['매수', '매도'].map(t => (
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
            <input className="tp-fi tp-fi-sm" type="number" placeholder="위탁기관 수수료" value={form.brokerFee} onChange={e => setForm(f => ({ ...f, brokerFee: e.target.value }))} />
            <input className="tp-fi tp-fi-sm" type="number" placeholder="유관기관 제비용" value={form.transactionTax} onChange={e => setForm(f => ({ ...f, transactionTax: e.target.value }))} />

            <div className="tp-form-total">
              <span className="tp-form-total-label">총액</span>
              <span className="tp-form-total-value">{formTotal > 0 ? fmt(formTotal) + '원' : '—'}</span>
            </div>

            <button
              className="tp-add-btn"
              onClick={handleAdd}
              disabled={!form.stockId || !form.quantity || !form.price || !form.date}
            >
              등록
            </button>

          </div>
        )}
      </div>

      {/* ── 하단: 거래 내역 테이블 ── */}
      <div className="tp-table-section">

        <div className="tp-table-controls">
          <div className="tp-filters">
            {['전체', '매수', '매도'].map(f => (
              <button
                key={f}
                className={`tp-filter-tab${filterType === f ? ' active' : ''}`}
                onClick={() => setFilterType(f)}
              >
                {f}
              </button>
            ))}
          </div>
          <div className="tp-search">
            <input
              className="tp-search-input"
              placeholder="종목명 검색"
              value={searchName}
              onChange={e => setSearchName(e.target.value)}
            />
          </div>
        </div>

        <div className="tp-table-wrap">
          {loading ? (
            <div style={{ textAlign: 'center', padding: 40, color: '#bbb' }}>로딩 중...</div>
          ) : (
            <table className="tp-table">
              <thead>
                <tr>
                  <th>날짜</th>
                  <th>종목명</th>
                  <th className="center">유형</th>
                  <th className="right">수량</th>
                  <th className="right">단가</th>
                  <th className="right has-tip" data-tip="매수·매도 시 모두 발생하는 증권사 위탁 수수료입니다.">위탁기관<br />수수료</th>
                  <th className="right has-tip" data-tip="매도 시에만 발생하는 거래소·예탁결제원 등 유관기관 수수료입니다.">유관기관<br />제비용</th>
                  <th className="right">총액</th>
                  <th className="center">수정/삭제</th>
                </tr>
              </thead>
              <tbody>
                {filtered.length === 0 ? (
                  <tr><td colSpan={9} className="tp-empty">거래 내역이 없습니다.</td></tr>
                ) : filtered.map(t => {
                  const label = TYPE_LABEL[t.type] ?? t.type
                  const total = t.type === 'BUY'
                    ? t.quantity * t.price + (t.brokerFee ?? 0)
                    : t.quantity * t.price - (t.brokerFee ?? 0) - (t.transactionTax ?? 0)
                  return (
                    <tr key={t.id}>
                      <td className="tp-date">{t.date}</td>
                      <td className="tp-stock-name">{stockMap[t.stockId] ?? `ID:${t.stockId}`}</td>
                      <td className="center">
                        <span className={`tp-type-badge ${typeClass(label)}`}>{label}</span>
                      </td>
                      <td className="right">{fmt(t.quantity)}</td>
                      <td className="right">{fmt(t.price)}</td>
                      <td className="right">{t.brokerFee > 0 ? fmt(t.brokerFee) : '—'}</td>
                      <td className="right">{t.transactionTax > 0 ? fmt(t.transactionTax) : '—'}</td>
                      <td className={`right tp-total ${t.type === 'BUY' ? 'outflow' : 'inflow'}`}>
                        {t.type === 'BUY' ? '-' : '+'}{fmt(Math.abs(total))}
                      </td>
                      <td className="center">
                        <button className="tp-btn-edit" onClick={() => openEditModal(t)}>수정</button>
                        <button className="tp-btn-delete" onClick={() => handleDelete(t.id)}>삭제</button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          )}
        </div>

      </div>

      {/* ── 수정 모달 ── */}
      {editModal && (
        <div className="tp-modal-overlay" onClick={() => setEditModal(null)}>
          <div className="tp-modal" onClick={e => e.stopPropagation()}>
            <p className="tp-modal-title">거래 수정</p>
            <div className="tp-modal-form">
              <label className="tp-modal-label">
                종목
                <select
                  className="tp-modal-select"
                  value={editForm.stockId}
                  onChange={e => setEditForm(f => ({ ...f, stockId: e.target.value }))}
                >
                  {stocks.map(s => <option key={s.id} value={String(s.id)}>{s.stockName}</option>)}
                </select>
              </label>
              <div className="tp-modal-label">
                유형
                <div className="tp-type-tabs">
                  {['매수', '매도'].map(t => (
                    <button
                      key={t}
                      className={`tp-type-tab${editForm.type === t ? ` active ${typeClass(t)}` : ''}`}
                      onClick={() => setEditForm(f => ({ ...f, type: t }))}
                    >
                      {t}
                    </button>
                  ))}
                </div>
              </div>
              {[
                { key: 'quantity',       label: '수량',                  type: 'number' },
                { key: 'price',          label: '단가 (원)',              type: 'number' },
                { key: 'date',           label: '날짜',                  type: 'date'   },
                { key: 'brokerFee',      label: '위탁기관 수수료 (원)',    type: 'number' },
                { key: 'transactionTax', label: '유관기관 제비용 (원)',    type: 'number' },
              ].map(({ key, label, type }) => (
                <label key={key} className="tp-modal-label">
                  {label}
                  <input
                    className="tp-modal-input"
                    type={type}
                    value={editForm[key]}
                    onChange={e => setEditForm(f => ({ ...f, [key]: e.target.value }))}
                  />
                </label>
              ))}
            </div>
            <div className="tp-modal-actions">
              <button className="tp-modal-cancel" onClick={() => setEditModal(null)}>취소</button>
              <button className="tp-modal-save" onClick={saveEdit}>저장</button>
            </div>
          </div>
        </div>
      )}

    </div>
  )
}
