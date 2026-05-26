import { useState, useMemo } from 'react'
import './StockPage.css'
import logo from '../assets/logo.png'

const fmt = (n) => Number(n).toLocaleString('ko-KR')
const fmtSign = (n) => (n >= 0 ? '+' : '') + fmt(n)

const STOCK_CODE_MAP = {
  '삼성전자': '005930', 'SK하이닉스': '000660', '현대차': '005380',
  '카카오': '035720',   '네이버': '035420',     'LG전자': '066570',
  'AAPL': 'AAPL',       'MSFT': 'MSFT',         'NVDA': 'NVDA',
  'GOOGL': 'GOOGL',     'TSLA': 'TSLA',
}

const SECTORS = ['반도체', '기술', '자동차', '금융', '바이오', '소비재', '에너지', '기타']
const SUMMARY_COLORS  = ['#1D9E75', '#5DCAA5', '#9FE1CB', '#2DB589', '#C8EFDF']
const SECTOR_COLORS   = ['#1D9E75', '#5DCAA5', '#9FE1CB', '#C8EFE3', '#DDF0E9', '#E8F7F1']
const TOTAL_SAVINGS   = 15_200_000

const INITIAL_STOCKS = [
  { id: 1, name: '삼성전자', code: '005930', sector: '반도체', type: '매수', quantity: 50,  avgPrice: 68000,  currentPrice: 72500, dividendPerShare: 1444, dividendDate: '2026-04-10' },
  { id: 2, name: 'AAPL',    code: 'AAPL',    sector: '기술',   type: '매수', quantity: 10,  avgPrice: 178000, currentPrice: 185000, dividendPerShare: 1065, dividendDate: '2026-02-15' },
  { id: 3, name: 'SK하이닉스', code: '000660', sector: '반도체', type: '매도', quantity: 20, avgPrice: 132000, currentPrice: 119000, dividendPerShare: 1200, dividendDate: '2026-04-10' },
  { id: 4, name: '현대차',  code: '005380',  sector: '자동차', type: '매수', quantity: 15,  avgPrice: 195000, currentPrice: 212000, dividendPerShare: 8000, dividendDate: '2026-12-20' },
]

export default function StockPage() {
  const [stocks, setStocks] = useState(INITIAL_STOCKS)
  const [form, setForm] = useState({ name: '', code: '', sector: SECTORS[0], quantity: '', avgPrice: '' })
  const [filter, setFilter] = useState('전체')
  const [selectedStock, setSelectedStock] = useState(null)
  const [editModal, setEditModal] = useState(null)
  const [editForm, setEditForm] = useState({})
  const [isDetailOpen, setIsDetailOpen] = useState(false)

  /* ── 투자 합계 ── */
  const totalInvested = useMemo(() =>
    stocks.reduce((s, st) => s + st.quantity * st.avgPrice, 0), [stocks])

  const summaryItems = useMemo(() =>
    [...stocks]
      .sort((a, b) => b.quantity * b.avgPrice - a.quantity * a.avgPrice)
      .map(s => ({
        id: s.id, name: s.name,
        pct: totalInvested > 0 ? Math.round(s.quantity * s.avgPrice / totalInvested * 100) : 0,
      }))
  , [stocks, totalInvested])

  /* ── 섹터별 투자비중 ── */
  const sectorWeights = useMemo(() => {
    const map = {}
    stocks.forEach(s => {
      const val = s.quantity * s.avgPrice
      map[s.sector] = (map[s.sector] || 0) + val
    })
    return Object.entries(map)
      .map(([sector, val]) => ({ sector, val, pct: totalInvested > 0 ? Math.round(val / totalInvested * 100) : 0 }))
      .sort((a, b) => b.val - a.val)
  }, [stocks, totalInvested])

  /* ── 종목별 수익/손실 ── */
  const stockPnl = useMemo(() => {
    const list = stocks.map(s => {
      const pnl    = (s.currentPrice - s.avgPrice) * s.quantity
      const pnlPct = s.avgPrice > 0 ? ((s.currentPrice - s.avgPrice) / s.avgPrice * 100).toFixed(1) : '0.0'
      return { ...s, pnl, pnlPct }
    }).sort((a, b) => Math.abs(b.pnl) - Math.abs(a.pnl))
    const maxAbs = Math.max(...list.map(s => Math.abs(s.pnl)), 1)
    return list.map(s => ({ ...s, barWidth: Math.abs(s.pnl) / maxAbs * 100 }))
  }, [stocks])

  /* ── 종목 추가 ── */
  const handleNameChange = e => {
    const name = e.target.value
    setForm(f => ({ ...f, name, code: STOCK_CODE_MAP[name] ?? '' }))
  }

  const handleAdd = () => {
    if (!form.name.trim()) return
    setStocks(prev => [...prev, {
      id: Date.now(),
      name: form.name.trim(),
      code: form.code || form.name.trim().toUpperCase(),
      sector: form.sector,
      type: '매수',
      quantity:    Number(form.quantity)  || 0,
      avgPrice:    Number(form.avgPrice)  || 0,
      currentPrice: 0,
      dividendPerShare: 0,
      dividendDate: '-',
    }])
    setForm({ name: '', code: '', sector: SECTORS[0], quantity: '', avgPrice: '' })
  }

  /* ── 필터 ── */
  const filteredStocks = filter === '전체' ? stocks : stocks.filter(s => s.type === filter)

  /* ── 상세 패널 ── */
  const handleRowClick = stock => { setSelectedStock(stock); setIsDetailOpen(true) }
  const closeDetail = () => { setIsDetailOpen(false); setTimeout(() => setSelectedStock(null), 300) }

  /* ── 수정 모달 ── */
  const openEdit = stock => { setEditModal(stock); setEditForm({ quantity: stock.quantity, avgPrice: stock.avgPrice, currentPrice: stock.currentPrice }) }
  const saveEdit = () => {
    const updated = { quantity: Number(editForm.quantity), avgPrice: Number(editForm.avgPrice), currentPrice: Number(editForm.currentPrice) }
    setStocks(prev => prev.map(s => s.id === editModal.id ? { ...s, ...updated } : s))
    if (selectedStock?.id === editModal.id) setSelectedStock(prev => ({ ...prev, ...updated }))
    setEditModal(null)
  }

  /* ── 삭제 ── */
  const handleDelete = id => {
    if (!window.confirm('해당 종목을 삭제하시겠습니까?')) return
    setStocks(prev => prev.filter(s => s.id !== id))
    if (selectedStock?.id === id) closeDetail()
  }

  return (
    <div className="sp-page">

      {/* ── 헤더 ── */}
      <div className="sp-header">
        <h1 className="sp-title">종목관리</h1>
        <p className="sp-subtitle">종목을 추가하고 관리하세요</p>
      </div>

      {/* ── 상단: 폼 + 투자 합계 ── */}
      <div className="sp-top-row">

        <div className="sp-card sp-form-card">
          <p className="sp-card-title">종목 추가</p>
          <div className="sp-form-row">
            <input className="sp-input name" placeholder="종목명" value={form.name} onChange={handleNameChange} onKeyDown={e => e.key === 'Enter' && handleAdd()} />
            <input className="sp-input code" placeholder="종목코드 자동" value={form.code} readOnly />
            <select className="sp-select" value={form.sector} onChange={e => setForm(f => ({ ...f, sector: e.target.value }))}>
              {SECTORS.map(s => <option key={s}>{s}</option>)}
            </select>
            <input className="sp-input qty" type="number" placeholder="보유수량" value={form.quantity} onChange={e => setForm(f => ({ ...f, quantity: e.target.value }))} />
            <input className="sp-input price" type="number" placeholder="평균단가" value={form.avgPrice} onChange={e => setForm(f => ({ ...f, avgPrice: e.target.value }))} />
            <button className="sp-add-btn" onClick={handleAdd} disabled={!form.name.trim()}>추가하기</button>
          </div>
        </div>

        <div className="sp-card sp-summary-card">
          <p className="sp-card-title">투자 합계</p>
          <div className="sp-summary-list">
            {summaryItems.map((item, i) => (
              <div key={item.id} className="sp-summary-item">
                <span className="sp-summary-dot" style={{ background: SUMMARY_COLORS[i % SUMMARY_COLORS.length] }} />
                <span className="sp-summary-name">{item.name}</span>
                <div className="sp-pct-bar">
                  <div className="sp-pct-fill" style={{ width: `${item.pct}%`, background: SUMMARY_COLORS[i % SUMMARY_COLORS.length] }} />
                </div>
                <span className="sp-summary-pct">{item.pct}%</span>
              </div>
            ))}
          </div>
          <div className="sp-summary-totals">
            <div className="sp-summary-total-item">
              <span className="sp-summary-total-label">총 투자금</span>
              <span className="sp-summary-total-value">{fmt(totalInvested)}원</span>
            </div>
            <div className="sp-summary-total-item" style={{ textAlign: 'right' }}>
              <span className="sp-summary-total-label">총 저축금</span>
              <span className="sp-summary-total-value">{fmt(TOTAL_SAVINGS)}원</span>
            </div>
          </div>
        </div>

      </div>

      {/* ── 중단: 상세 패널 + 보유종목 테이블 ── */}
      <div className="sp-middle-row">

        <div className={`sp-detail-panel${isDetailOpen ? ' open' : ''}`}>
          {selectedStock && (
            <div className="sp-detail-card">
              <div className="sp-detail-top-bar">
                <div className="sp-detail-top">
                  <img src={logo} alt="" className="sp-detail-logo" />
                  <div className="sp-detail-name-group">
                    <span className="sp-detail-name">{selectedStock.name}</span>
                    <span className="sp-detail-code">{selectedStock.code}</span>
                  </div>
                </div>
                <button className="sp-detail-close" onClick={closeDetail}>✕</button>
              </div>
              <div className="sp-detail-body">
                <div className="sp-detail-divider" />
                <div className="sp-detail-items">
                  <div className="sp-detail-item">
                    <span className="sp-detail-label">주당 예상 배당금</span>
                    <span className="sp-detail-value">{fmt(selectedStock.dividendPerShare)}원</span>
                  </div>
                  <div className="sp-detail-item">
                    <span className="sp-detail-label">총 예상 배당금</span>
                    <span className="sp-detail-value">{fmt(selectedStock.dividendPerShare * selectedStock.quantity)}원</span>
                  </div>
                  <div className="sp-detail-item">
                    <span className="sp-detail-label">배당 지급일</span>
                    <span className="sp-detail-value neutral">{selectedStock.dividendDate}</span>
                  </div>
                  <div className="sp-detail-item">
                    <span className="sp-detail-label">배당수익률</span>
                    <span className="sp-detail-value">
                      {selectedStock.currentPrice > 0 ? (selectedStock.dividendPerShare / selectedStock.currentPrice * 100).toFixed(2) : '0.00'}%
                    </span>
                  </div>
                </div>
                <div className="sp-detail-divider" />
                <p className="sp-detail-section-label">보유 현황</p>
                <div className="sp-detail-items">
                  <div className="sp-detail-item">
                    <span className="sp-detail-label">보유수량</span>
                    <span className="sp-detail-value neutral">{fmt(selectedStock.quantity)}주</span>
                  </div>
                  <div className="sp-detail-item">
                    <span className="sp-detail-label">평균단가</span>
                    <span className="sp-detail-value neutral">{fmt(selectedStock.avgPrice)}원</span>
                  </div>
                  <div className="sp-detail-item">
                    <span className="sp-detail-label">현재가</span>
                    <span className="sp-detail-value neutral">{fmt(selectedStock.currentPrice)}원</span>
                  </div>
                  {(() => {
                    const pnl = (selectedStock.currentPrice - selectedStock.avgPrice) * selectedStock.quantity
                    return (
                      <div className="sp-detail-item">
                        <span className="sp-detail-label">평가손익</span>
                        <span className={`sp-detail-value ${pnl >= 0 ? 'profit' : 'loss'}`}>{fmtSign(pnl)}원</span>
                      </div>
                    )
                  })()}
                </div>
                <div className="sp-detail-divider" />
                <div className="sp-detail-footer">
                  <button className="sp-detail-btn-edit" onClick={e => { e.stopPropagation(); openEdit(selectedStock) }}>수정</button>
                  <button className="sp-detail-btn-delete" onClick={e => { e.stopPropagation(); handleDelete(selectedStock.id) }}>삭제</button>
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="sp-table-section">
          <div className="sp-filters">
            {['전체', '매수', '매도'].map(f => (
              <button key={f} className={`sp-filter-tab${filter === f ? ' active' : ''}`} onClick={() => setFilter(f)}>{f}</button>
            ))}
          </div>
          <div className="sp-table-wrap">
            <table className="sp-table">
              <thead>
                <tr>
                  <th>종목명</th><th>종목코드</th><th>섹터</th>
                  <th className="center">구분</th>
                  <th className="right">보유수량</th><th className="right">평균단가</th>
                  <th className="right">현재가</th><th className="right">평가손익</th>
                  <th className="center">수정/삭제</th>
                </tr>
              </thead>
              <tbody>
                {filteredStocks.length === 0 ? (
                  <tr><td colSpan={9} className="sp-empty">보유 종목이 없습니다.</td></tr>
                ) : filteredStocks.map(s => {
                  const pnl = (s.currentPrice - s.avgPrice) * s.quantity
                  return (
                    <tr key={s.id} className={selectedStock?.id === s.id ? 'selected' : ''} onClick={() => handleRowClick(s)}>
                      <td className="stock-name">{s.name}</td>
                      <td>{s.code}</td><td>{s.sector}</td>
                      <td className="center">
                        <span className={`sp-type-badge ${s.type === '매수' ? 'buy' : 'sell'}`}>{s.type}</span>
                      </td>
                      <td className="right">{fmt(s.quantity)}</td>
                      <td className="right">{fmt(s.avgPrice)}</td>
                      <td className="right">{fmt(s.currentPrice)}</td>
                      <td className={`right ${pnl >= 0 ? 'profit' : 'loss'}`}>{fmtSign(pnl)}</td>
                      <td className="center" onClick={e => e.stopPropagation()}>
                        <button className="sp-btn-edit" onClick={() => openEdit(s)}>수정</button>
                        <button className="sp-btn-delete" onClick={() => handleDelete(s.id)}>삭제</button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>

      </div>

      {/* ── 하단: 섹터별 투자비중 + 종목별 수익/손실 ── */}
      <div className="sp-bottom-row">

        {/* 섹터별 투자비중 */}
        <div className="sp-card sp-sector-card">
          <p className="sp-card-title">섹터별 투자비중</p>
          <div className="sp-sector-list">
            {sectorWeights.map((sw, i) => (
              <div key={sw.sector} className="sp-sector-item">
                <span className="sp-sector-dot" style={{ background: SECTOR_COLORS[i % SECTOR_COLORS.length] }} />
                <span className="sp-sector-name">{sw.sector}</span>
                <div className="sp-sector-bar-bg">
                  <div className="sp-sector-bar-fill" style={{ width: `${sw.pct}%`, background: SECTOR_COLORS[i % SECTOR_COLORS.length] }} />
                </div>
                <span className="sp-sector-pct">{sw.pct}%</span>
              </div>
            ))}
          </div>
        </div>

        {/* 종목별 수익/손실 */}
        <div className="sp-card sp-pnl-card">
          <p className="sp-card-title">종목별 수익/손실</p>
          <div className="sp-pnl-list">
            {stockPnl.map(s => (
              <div key={s.id} className="sp-pnl-item">
                <span className="sp-pnl-name">{s.name}</span>
                <div className="sp-pnl-bar-bg">
                  <div
                    className={`sp-pnl-bar-fill ${s.pnl >= 0 ? 'profit' : 'loss'}`}
                    style={{ width: `${s.barWidth}%` }}
                  />
                </div>
                <span className={`sp-pnl-amount ${s.pnl >= 0 ? 'profit' : 'loss'}`}>{fmtSign(s.pnl)}</span>
                <span className={`sp-pnl-pct ${s.pnl >= 0 ? 'profit' : 'loss'}`}>
                  {s.pnl >= 0 ? '+' : ''}{s.pnlPct}%
                </span>
              </div>
            ))}
          </div>
        </div>

      </div>

      {/* ── 수정 모달 ── */}
      {editModal && (
        <div className="sp-modal-overlay" onClick={() => setEditModal(null)}>
          <div className="sp-modal" onClick={e => e.stopPropagation()}>
            <p className="sp-modal-title">종목 수정 — {editModal.name}</p>
            <div className="sp-modal-form">
              {[
                { key: 'quantity',     label: '보유수량' },
                { key: 'avgPrice',     label: '평균단가 (원)' },
                { key: 'currentPrice', label: '현재가 (원)' },
              ].map(({ key, label }) => (
                <label key={key} className="sp-modal-label">
                  {label}
                  <input className="sp-modal-input" type="number" value={editForm[key]} onChange={e => setEditForm(f => ({ ...f, [key]: e.target.value }))} />
                </label>
              ))}
            </div>
            <div className="sp-modal-actions">
              <button className="sp-modal-cancel" onClick={() => setEditModal(null)}>취소</button>
              <button className="sp-modal-save" onClick={saveEdit}>저장</button>
            </div>
          </div>
        </div>
      )}

    </div>
  )
}
