import { useState, useEffect, useMemo, useRef, useCallback } from 'react'
import './StockPage.css'
import logo from '../assets/logo.png'
import { getStocks, createStock, updateStock, deleteStock, searchStocks } from '../api/stocks'

const fmt = (n) => Number(n || 0).toLocaleString('ko-KR')
const fmtSign = (n) => (n >= 0 ? '+' : '') + fmt(n)

const SECTORS = [
  { code: 'IT',                     label: 'IT' },
  { code: 'FINANCIALS',             label: '금융' },
  { code: 'HEALTHCARE',             label: '헬스케어' },
  { code: 'ENERGY',                 label: '에너지' },
  { code: 'MATERIALS',              label: '소재' },
  { code: 'INDUSTRIALS',            label: '산업재' },
  { code: 'CONSUMER_DISCRETIONARY', label: '경기소비재' },
  { code: 'CONSUMER_STAPLES',       label: '필수소비재' },
  { code: 'COMMUNICATION',          label: '커뮤니케이션' },
  { code: 'UTILITIES',              label: '유틸리티' },
  { code: 'REAL_ESTATE',            label: '부동산' },
]
const SUMMARY_COLORS = ['#1D9E75', '#5DCAA5', '#9FE1CB', '#2DB589', '#C8EFDF']
const SECTOR_COLORS  = ['#1D9E75', '#5DCAA5', '#9FE1CB', '#C8EFE3', '#DDF0E9', '#E8F7F1']

const EMPTY_FORM = { stockName: '', stockCode: '', sector: 'IT', quantity: '', avgPrice: '' }

export default function StockPage() {
  const [stocks, setStocks]               = useState([])
  const [loading, setLoading]             = useState(true)
  const [form, setForm]                   = useState(EMPTY_FORM)
  const [selectedStock, setSelectedStock] = useState(null)
  const [editModal, setEditModal]         = useState(null)
  const [editForm, setEditForm]           = useState({})
  const [isDetailOpen, setIsDetailOpen]   = useState(false)
  const [suggestions, setSuggestions]     = useState([])
  const [showSuggestions, setShowSuggestions] = useState(false)
  const searchTimer   = useRef(null)
  const suggestionRef = useRef(null)

  const fetchStocks = async () => {
    setLoading(true)
    try {
      const res = await getStocks()
      const list = res.data.data ?? []
      setStocks(list)
      setSelectedStock(prev => prev ? (list.find(s => s.id === prev.id) ?? null) : null)
    } catch {
      // 오류 시 빈 상태 유지
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchStocks() }, [])

  /* ── 자동완성 ── */
  const handleStockNameChange = useCallback((value) => {
    setForm(f => ({ ...f, stockName: value }))
    clearTimeout(searchTimer.current)
    if (!value.trim()) { setSuggestions([]); setShowSuggestions(false); return }
    searchTimer.current = setTimeout(async () => {
      try {
        const res = await searchStocks(value.trim())
        const list = res.data?.data ?? []
        setSuggestions(list)
        setShowSuggestions(list.length > 0)
      } catch {
        setSuggestions([]); setShowSuggestions(false)
      }
    }, 300)
  }, [])

  const selectSuggestion = (item) => {
    setForm(f => ({ ...f, stockName: item.stockName, stockCode: item.stockCode }))
    setSuggestions([]); setShowSuggestions(false)
  }

  useEffect(() => {
    const handler = (e) => {
      if (suggestionRef.current && !suggestionRef.current.contains(e.target))
        setShowSuggestions(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  /* ── 투자 합계 ── */
  const totalInvested = useMemo(() =>
    stocks.reduce((s, st) => s + st.quantity * Number(st.avgPrice || 0), 0),
    [stocks]
  )

  const summaryItems = useMemo(() =>
    [...stocks]
      .sort((a, b) => b.quantity * Number(b.avgPrice) - a.quantity * Number(a.avgPrice))
      .map(s => ({
        id: s.id,
        name: s.stockName,
        pct: totalInvested > 0
          ? Math.round(s.quantity * Number(s.avgPrice) / totalInvested * 100)
          : 0,
      })),
    [stocks, totalInvested]
  )

  /* ── 섹터별 투자비중 ── */
  const sectorWeights = useMemo(() => {
    const map = {}
    stocks.forEach(s => {
      const label = s.sectorLabel ?? s.sectorCode ?? '기타'
      const val = s.quantity * Number(s.avgPrice || 0)
      map[label] = (map[label] || 0) + val
    })
    return Object.entries(map)
      .map(([sector, val]) => ({
        sector, val,
        pct: totalInvested > 0 ? Math.round(val / totalInvested * 100) : 0,
      }))
      .sort((a, b) => b.val - a.val)
  }, [stocks, totalInvested])

  /* ── 종목별 수익/손실 ── */
  const stockPnl = useMemo(() => {
    const list = stocks.map(s => {
      const pnl    = Number(s.evaluationProfit ?? 0)
      const pnlPct = s.profitRate != null ? Number(s.profitRate).toFixed(1) : '0.0'
      return { ...s, pnl, pnlPct }
    }).sort((a, b) => Math.abs(b.pnl) - Math.abs(a.pnl))
    const maxAbs = Math.max(...list.map(s => Math.abs(s.pnl)), 1)
    return list.map(s => ({ ...s, barWidth: Math.abs(s.pnl) / maxAbs * 100 }))
  }, [stocks])

  /* ── 종목 추가 ── */
  const handleAdd = async () => {
    if (!form.stockName.trim() || !form.stockCode.trim()) return
    try {
      await createStock({
        stockName:  form.stockName.trim(),
        stockCode:  form.stockCode.trim(),
        sector:     form.sector || undefined,
        quantity:   Number(form.quantity) || 0,
        avgPrice:   Number(form.avgPrice) || 0,
      })
      setForm(EMPTY_FORM)
      await fetchStocks()
    } catch (err) {
      alert(err.response?.data?.message ?? '종목 추가에 실패했습니다')
    }
  }

  /* ── 상세 패널 ── */
  const handleRowClick = stock => { setSelectedStock(stock); setIsDetailOpen(true) }
  const closeDetail = () => {
    setIsDetailOpen(false)
    setTimeout(() => setSelectedStock(null), 300)
  }

  /* ── 수정 모달 ── */
  const openEdit = stock => {
    setEditModal(stock)
    setEditForm({
      quantity: stock.quantity,
      avgPrice: stock.avgPrice,
    })
  }

  const saveEdit = async () => {
    try {
      await updateStock(editModal.id, {
        quantity: Number(editForm.quantity),
        avgPrice: Number(editForm.avgPrice),
      })
      setEditModal(null)
      await fetchStocks()
    } catch (err) {
      alert(err.response?.data?.message ?? '수정에 실패했습니다')
    }
  }

  /* ── 삭제 ── */
  const handleDelete = async id => {
    if (!window.confirm('해당 종목을 삭제하시겠습니까?')) return
    try {
      await deleteStock(id)
      if (selectedStock?.id === id) closeDetail()
      await fetchStocks()
    } catch (err) {
      alert(err.response?.data?.message ?? '삭제에 실패했습니다')
    }
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
            <div className="sp-autocomplete-wrap" ref={suggestionRef}>
              <input
                className="sp-input name"
                placeholder="종목명 검색"
                value={form.stockName}
                onChange={e => handleStockNameChange(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleAdd()}
                autoComplete="off"
              />
              {showSuggestions && (
                <ul className="sp-suggestions">
                  {suggestions.map(item => (
                    <li key={item.stockCode} className="sp-suggestion-item"
                        onMouseDown={() => selectSuggestion(item)}>
                      <span className="sp-sug-name">{item.stockName}</span>
                      <span className="sp-sug-code">{item.stockCode}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
            <input
              className="sp-input code"
              placeholder="종목코드"
              value={form.stockCode}
              readOnly
            />
            <select
              className="sp-select"
              value={form.sector}
              onChange={e => setForm(f => ({ ...f, sector: e.target.value }))}
            >
              {SECTORS.map(s => <option key={s.code} value={s.code}>{s.label}</option>)}
            </select>
            <input
              className="sp-input qty"
              type="number"
              placeholder="보유수량"
              value={form.quantity}
              onChange={e => setForm(f => ({ ...f, quantity: e.target.value }))}
            />
            <input
              className="sp-input price"
              type="number"
              placeholder="단가"
              value={form.avgPrice}
              onChange={e => setForm(f => ({ ...f, avgPrice: e.target.value }))}
            />
            <button
              className="sp-add-btn"
              onClick={handleAdd}
              disabled={!form.stockName.trim() || !form.stockCode.trim()}
            >
              추가하기
            </button>
          </div>
        </div>

        <div className="sp-card sp-summary-card">
          <p className="sp-card-title">투자 합계</p>
          <div className="sp-summary-list">
            {loading ? (
              <span style={{ color: '#bbb', fontSize: 13 }}>로딩 중...</span>
            ) : summaryItems.length === 0 ? (
              <span style={{ color: '#bbb', fontSize: 13 }}>보유 종목이 없습니다</span>
            ) : summaryItems.map((item, i) => (
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
                    <span className="sp-detail-name">{selectedStock.stockName}</span>
                    <span className="sp-detail-code">{selectedStock.stockCode}</span>
                  </div>
                </div>
                <button className="sp-detail-close" onClick={closeDetail}>✕</button>
              </div>
              <div className="sp-detail-body">
                <div className="sp-detail-divider" />
                <div className="sp-detail-items">
                  <div className="sp-detail-item">
                    <span className="sp-detail-label">주당 예상 배당금</span>
                    <span className="sp-detail-value">{fmt(selectedStock.expectedDividendPerShare ?? 0)}원</span>
                  </div>
                  <div className="sp-detail-item">
                    <span className="sp-detail-label">총 예상 배당금</span>
                    <span className="sp-detail-value">
                      {fmt((selectedStock.expectedDividendPerShare ?? 0) * selectedStock.quantity)}원
                    </span>
                  </div>
                  <div className="sp-detail-item">
                    <span className="sp-detail-label">배당 지급월</span>
                    <span className="sp-detail-value neutral">
                      {selectedStock.paymentMonths?.length > 0
                        ? selectedStock.paymentMonths.join('월, ') + '월'
                        : '-'}
                    </span>
                  </div>
                  <div className="sp-detail-item">
                    <span className="sp-detail-label">배당수익률</span>
                    <span className="sp-detail-value">
                      {selectedStock.previousClose > 0
                        ? ((selectedStock.expectedDividendPerShare ?? 0) / Number(selectedStock.previousClose) * 100).toFixed(2)
                        : '0.00'}%
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
                    <span className="sp-detail-label">단가</span>
                    <span className="sp-detail-value neutral">{fmt(selectedStock.avgPrice)}원</span>
                  </div>
                  <div className="sp-detail-item">
                    <span className="sp-detail-label">전일 종가</span>
                    <span className="sp-detail-value neutral">
                      {selectedStock.previousClose ? fmt(selectedStock.previousClose) + '원' : '-'}
                    </span>
                  </div>
                  <div className="sp-detail-item">
                    <span className="sp-detail-label">평가손익</span>
                    <span className={`sp-detail-value ${Number(selectedStock.evaluationProfit ?? 0) >= 0 ? 'profit' : 'loss'}`}>
                      {fmtSign(Number(selectedStock.evaluationProfit ?? 0))}원
                    </span>
                  </div>
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
          <div className="sp-table-wrap">
            {loading ? (
              <div style={{ textAlign: 'center', padding: 40, color: '#bbb' }}>로딩 중...</div>
            ) : (
              <table className="sp-table">
                <thead>
                  <tr>
                    <th>종목명</th><th>종목코드</th><th className="narrow">섹터</th>
                    <th className="right narrow">보유수량</th>
                    <th className="right">단가</th>
                    <th className="right">전일 종가</th>
                    <th className="right">평가손익</th>
                    <th className="center">수정/삭제</th>
                  </tr>
                </thead>
                <tbody>
                  {stocks.length === 0 ? (
                    <tr><td colSpan={8} className="sp-empty">보유 종목이 없습니다.</td></tr>
                  ) : stocks.map(s => {
                    const pnl = Number(s.evaluationProfit ?? 0)
                    return (
                      <tr
                        key={s.id}
                        className={selectedStock?.id === s.id ? 'selected' : ''}
                        onClick={() => handleRowClick(s)}
                      >
                        <td className="stock-name">{s.stockName}</td>
                        <td>{s.stockCode}</td>
                        <td className="narrow">{s.sectorLabel ?? s.sectorCode ?? '-'}</td>
                        <td className="right narrow">{fmt(s.quantity)}</td>
                        <td className="right">{fmt(s.avgPrice)}</td>
                        <td className="right">{s.previousClose ? fmt(s.previousClose) : '-'}</td>
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
            )}
          </div>
        </div>

      </div>

      {/* ── 하단: 섹터별 투자비중 + 종목별 수익/손실 ── */}
      <div className="sp-bottom-row">

        <div className="sp-card sp-sector-card">
          <p className="sp-card-title">섹터별 투자비중</p>
          <div className="sp-sector-list">
            {sectorWeights.length === 0 ? (
              <span style={{ color: '#bbb', fontSize: 13 }}>데이터 없음</span>
            ) : sectorWeights.map((sw, i) => (
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

        <div className="sp-card sp-pnl-card">
          <p className="sp-card-title">종목별 수익/손실</p>
          <div className="sp-pnl-list">
            {stockPnl.length === 0 ? (
              <span style={{ color: '#bbb', fontSize: 13 }}>데이터 없음</span>
            ) : stockPnl.map(s => (
              <div key={s.id} className="sp-pnl-item">
                <span className="sp-pnl-name">{s.stockName}</span>
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
            <p className="sp-modal-title">종목 수정 — {editModal.stockName}</p>
            <div className="sp-modal-form">
              {[
                { key: 'quantity', label: '보유수량' },
                { key: 'avgPrice', label: '단가 (원)' },
              ].map(({ key, label }) => (
                <label key={key} className="sp-modal-label">
                  {label}
                  <input
                    className="sp-modal-input"
                    type="number"
                    value={editForm[key]}
                    onChange={e => setEditForm(f => ({ ...f, [key]: e.target.value }))}
                  />
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