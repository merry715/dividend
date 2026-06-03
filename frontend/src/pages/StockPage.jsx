import { useState, useEffect, useMemo, useRef, useCallback } from 'react'
import './StockPage.css'
import logo from '../assets/logo.png'
import { getStocks, createStock, deleteStock, searchStocks, updateStockSector } from '../api/stocks'

const fmt = (n) => Number(n || 0).toLocaleString('ko-KR')

const CYCLE_COUNT = { MONTHLY: 12, QUARTERLY: 4, SEMI_ANNUAL: 2, ANNUAL: 1 }
const CURRENT_YEAR = new Date().getFullYear()

function formatPaymentMonth(dto, currentYear) {
  if (dto.year === currentYear) return `${dto.month}월`
  return `${dto.year}.${dto.month}월`
}

function perPaymentDividend(annualPerShare, dividendCycle) {
  const count = CYCLE_COUNT[dividendCycle] ?? 1
  return Math.round(annualPerShare / count)
}

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
const SECTOR_COLORS = ['#1D9E75', '#5DCAA5', '#9FE1CB', '#C8EFE3', '#DDF0E9', '#E8F7F1']

const EMPTY_FORM = { stockName: '', stockCode: '', sector: 'IT' }
const EMPTY_EDIT = { open: false, stock: null, sector: 'IT' }

export default function StockPage() {
  const [stocks, setStocks]               = useState([])
  const [loading, setLoading]             = useState(true)
  const [form, setForm]                   = useState(EMPTY_FORM)
  const [selectedStock, setSelectedStock] = useState(null)
  const [isDetailOpen, setIsDetailOpen]   = useState(false)
  const [suggestions, setSuggestions]     = useState([])
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [editModal, setEditModal]         = useState(EMPTY_EDIT)
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

  /* ── 섹터별 투자비중 ── */
  const totalInvested = useMemo(() =>
    stocks.reduce((s, st) => s + st.quantity * Number(st.avgPrice || 0), 0),
    [stocks]
  )

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

  /* ── 종목 추가 ── */
  const handleAdd = async () => {
    if (!form.stockName.trim() || !form.stockCode.trim()) return
    try {
      await createStock({
        stockName: form.stockName.trim(),
        stockCode: form.stockCode.trim(),
        sector:    form.sector || undefined,
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

  /* ── 섹터 수정 ── */
  const openEdit = (e, stock) => {
    e.stopPropagation()
    setEditModal({ open: true, stock, sector: stock.sectorCode ?? 'IT' })
  }

  const handleEditSave = async () => {
    try {
      await updateStockSector(editModal.stock.id, editModal.sector)
      setEditModal(EMPTY_EDIT)
      await fetchStocks()
    } catch (err) {
      alert(err.response?.data?.message ?? '수정에 실패했습니다')
    }
  }

  return (
    <div className="sp-page">

      {/* ── 헤더 ── */}
      <div className="sp-header">
        <h1 className="sp-title">종목 관리</h1>
        <p className="sp-subtitle">종목을 추가하고 관리하세요</p>
      </div>

      {/* ── 상단: 폼 + 섹터 비중 ── */}
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
            <button
              className="sp-add-btn"
              onClick={handleAdd}
              disabled={!form.stockName.trim() || !form.stockCode.trim()}
            >
              추가하기
            </button>
          </div>
        </div>

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
                    <span className="sp-detail-label">
                      주당 예상 배당금
                      {selectedStock.expectedDividendPerShare > 0 && selectedStock.dividendCycle && selectedStock.dividendCycle !== 'ANNUAL' && (
                        <span style={{ fontSize: 11, color: '#aaa', fontWeight: 400, marginLeft: 4 }}>
                          (1회 평균)
                        </span>
                      )}
                    </span>
                    {selectedStock.expectedDividendPerShare > 0 ? (
                      <span className="sp-detail-value">
                        {fmt(perPaymentDividend(selectedStock.expectedDividendPerShare, selectedStock.dividendCycle))}원
                      </span>
                    ) : (
                      <span className="sp-detail-value sp-detail-na">해당사항 없음</span>
                    )}
                  </div>
                  <div className="sp-detail-item">
                    <span className="sp-detail-label">총 예상 배당금</span>
                    {selectedStock.expectedDividendPerShare > 0 ? (
                      <span className="sp-detail-value">
                        {fmt(selectedStock.expectedDividendPerShare * selectedStock.quantity)}원
                      </span>
                    ) : (
                      <span className="sp-detail-value sp-detail-na">해당사항 없음</span>
                    )}
                  </div>
                  <div className="sp-detail-item">
                    <span className="sp-detail-label">배당 지급월</span>
                    {selectedStock.paymentMonths?.length > 0 ? (
                      <span className="sp-detail-value neutral">
                        {selectedStock.paymentMonths
                          .map(dto => formatPaymentMonth(dto, CURRENT_YEAR))
                          .join(' ')}
                      </span>
                    ) : (
                      <span className="sp-detail-value sp-detail-na">해당사항 없음</span>
                    )}
                  </div>
                  <div className="sp-detail-item">
                    <span className="sp-detail-label">배당수익률</span>
                    {selectedStock.expectedDividendPerShare > 0 && selectedStock.previousClose > 0 ? (
                      <span className="sp-detail-value">
                        {(selectedStock.expectedDividendPerShare / Number(selectedStock.previousClose) * 100).toFixed(2)}%
                      </span>
                    ) : (
                      <span className="sp-detail-value sp-detail-na">해당사항 없음</span>
                    )}
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
                      {Number(selectedStock.evaluationProfit ?? 0) >= 0 ? '+' : ''}{fmt(Number(selectedStock.evaluationProfit ?? 0))}원
                    </span>
                  </div>
                </div>
                <div className="sp-detail-divider" />
                <div className="sp-detail-footer">
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
                    <th>종목명</th>
                    <th>종목코드</th>
                    <th className="narrow">섹터</th>
                    <th className="right narrow">보유수량</th>
                    <th className="center">관리</th>
                  </tr>
                </thead>
                <tbody>
                  {stocks.length === 0 ? (
                    <tr><td colSpan={5} className="sp-empty">보유 종목이 없습니다.</td></tr>
                  ) : stocks.map(s => (
                    <tr
                      key={s.id}
                      className={selectedStock?.id === s.id ? 'selected' : ''}
                      onClick={() => handleRowClick(s)}
                    >
                      <td className="stock-name">{s.stockName}</td>
                      <td>{s.stockCode}</td>
                      <td className="narrow">{s.sectorLabel ?? s.sectorCode ?? '-'}</td>
                      <td className="right narrow">{fmt(s.quantity)}</td>
                      <td className="center" onClick={e => e.stopPropagation()}>
                        <button className="sp-btn-edit" onClick={e => openEdit(e, s)}>수정</button>
                        <button className="sp-btn-delete" onClick={() => handleDelete(s.id)}>삭제</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

      </div>

      {/* ── 섹터 수정 모달 ── */}
      {editModal.open && (
        <div className="sp-modal-overlay" onClick={() => setEditModal(EMPTY_EDIT)}>
          <div className="sp-modal" onClick={e => e.stopPropagation()}>
            <p className="sp-modal-title">섹터 수정</p>
            <div className="sp-modal-form">
              <label className="sp-modal-label">
                섹터
                <select
                  className="sp-modal-select"
                  value={editModal.sector}
                  onChange={e => setEditModal(m => ({ ...m, sector: e.target.value }))}
                >
                  {SECTORS.map(s => <option key={s.code} value={s.code}>{s.label}</option>)}
                </select>
              </label>
            </div>
            <div className="sp-modal-actions">
              <button className="sp-modal-cancel" onClick={() => setEditModal(EMPTY_EDIT)}>취소</button>
              <button className="sp-modal-save" onClick={handleEditSave}>저장</button>
            </div>
          </div>
        </div>
      )}

    </div>
  )
}
