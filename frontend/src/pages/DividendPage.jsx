import { useState, useEffect, useMemo, useCallback } from 'react'
import './DividendPage.css'
import {
  Chart as ChartJS, CategoryScale, LinearScale,
  BarElement, Tooltip, Legend,
} from 'chart.js'
import { Bar } from 'react-chartjs-2'
import {
  getAnnual, getCumulative, getMonthly, getYearly,
  getDividends, confirmDividend, generateDividends,
  getByStockYear, confirmWithAutoGenerate, updateDividend,
} from '../api/dividend'
import { getStocks } from '../api/stocks'

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip, Legend)

const fmt = (n) => Number(n).toLocaleString('ko-KR')
const MONTH_LABELS = ['1월','2월','3월','4월','5월','6월','7월','8월','9월','10월','11월','12월']
const today = new Date().toISOString().split('T')[0]
const CURRENT_YEAR = new Date().getFullYear()

// 지급월 표시: 현재 연도면 "5월", 다른 연도면 "2027.4월"
function formatPayMonth(paymentDate, paymentYear, currentYear) {
  const month = new Date(paymentDate + 'T00:00:00').getMonth() + 1
  if (paymentYear === currentYear) return `${month}월`
  return `${paymentYear}.${month}월`
}

// 배당락일 표시: 같은 연도면 "3/30", 다른 연도면 "2027.3/30"
function formatExDate(exDate, currentYear) {
  if (!exDate) return '-'
  const d = new Date(exDate + 'T00:00:00')
  const month = d.getMonth() + 1
  const day = d.getDate()
  const year = d.getFullYear()
  if (year === currentYear) return `${month}/${day}`
  return `${year}.${month}/${day}`
}

export default function DividendPage() {
  const [annualData, setAnnualData]         = useState(null)
  const [cumulativeData, setCumulativeData] = useState(null)
  const [monthlyData, setMonthlyData]       = useState([])
  const [yearlyData, setYearlyData]         = useState([])
  const [dividendList, setDividendList]     = useState([])
  const [stocks, setStocks]                 = useState([])
  const [byStockList, setByStockList]       = useState([])
  const [loading, setLoading]               = useState(true)
  const [converting, setConverting]         = useState(false)

  // 통합 확정 전환 폼
  const [confirmForm, setConfirmForm] = useState({
    stockId:    '',   // 선택 종목 ID
    dividendId: '',   // 확정할 분기 row id (EXPECTED 목록에서 선택)
    exDivDate:  '',   // 배당락일
    payDate:    '',   // 배당지급일
    perShare:   '',   // 주당 배당금
  })

  // 수정 모달
  const [editModal, setEditModal] = useState(null)
  const [editRows, setEditRows]   = useState([])
  const [saving, setSaving]       = useState(false)

  const loadAll = useCallback(async (autoGenerate = true) => {
    try {
      const [annual, cumulative, monthly, yearly, dividends, stocksRes, byStock] = await Promise.all([
        getAnnual(CURRENT_YEAR),
        getCumulative(),
        getMonthly(CURRENT_YEAR),
        getYearly(),
        getDividends(),
        getStocks(),
        getByStockYear(CURRENT_YEAR),
      ])

      setAnnualData(annual.data.data)
      setCumulativeData(cumulative.data.data)
      setMonthlyData(monthly.data.data ?? [])
      setYearlyData(yearly.data.data ?? [])
      const dividendData = dividends.data.data ?? []
      const stockList    = stocksRes.data.data ?? []
      setDividendList(dividendData)
      setStocks(stockList)
      setByStockList(byStock.data.data ?? [])

      // 올해 배당 레코드 없는 종목만 자동 생성
      const stockIdsWithDividends = new Set(
        dividendData.filter(d => d.year === CURRENT_YEAR).map(d => d.stockId)
      )
      const stocksWithoutDividends = stockList.filter(s => !stockIdsWithDividends.has(s.id))
      if (stocksWithoutDividends.length > 0 && autoGenerate) {
        await Promise.all(stocksWithoutDividends.map(s => generateDividends(s.id, CURRENT_YEAR)))
        return loadAll(false)
      }
    } catch (e) {
      console.error('배당 데이터 로딩 실패', e)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadAll() }, [loadAll])

  // 12개월 배열 정규화 (paymentDate null 배당 제외, 현재 연도만)
  const normalizedMonthly = useMemo(() =>
    Array.from({ length: 12 }, (_, i) => {
      const month = i + 1
      const found = monthlyData.find(m => m.month === month)
      const monthDivs = dividendList.filter(d =>
        d.year === CURRENT_YEAR && d.month === month && d.paymentDate != null
      )
      const confirmedCount = monthDivs.filter(d => d.status === 'CONFIRMED').length
      const status = monthDivs.length === 0              ? 'EXPECTED'
                   : confirmedCount === monthDivs.length ? 'CONFIRMED'
                   : confirmedCount > 0                  ? 'PARTIAL'
                   : 'EXPECTED'
      return {
        month,
        label:       MONTH_LABELS[i],
        expected:    found?.expectedAmount  ?? 0,
        confirmed:   found?.confirmedAmount ?? 0,
        status,
        isConfirmed: status === 'CONFIRMED',
      }
    }),
  [monthlyData, dividendList])

  // EXPECTED 배당 목록
  const expectedItems = useMemo(
    () => dividendList.filter(d => d.status === 'EXPECTED'),
    [dividendList]
  )

  // 선택된 종목의 cycle
  const selectedStock = useMemo(() =>
    confirmForm.stockId ? stocks.find(s => s.id === Number(confirmForm.stockId)) : null,
  [confirmForm.stockId, stocks])

  const selectedCycle = selectedStock?.dividendCycle ?? null

  // 확정 전환 드롭다운용: EXPECTED 배당이 있는 종목
  const stocksForConfirm = useMemo(() => {
    const stocksWithExpected = new Set(
      expectedItems.map(d => d.stockId)
    )
    return stocks.filter(s => stocksWithExpected.has(s.id))
  }, [stocks, expectedItems])

  // 선택 종목의 EXPECTED 배당 목록 (분기 선택 드롭다운용)
  const selectedStockExpected = useMemo(() =>
    expectedItems.filter(d => d.stockId === Number(confirmForm.stockId)),
  [expectedItems, confirmForm.stockId])

  // 통합 확정 저장 — 선택한 분기 1건만 확정, 나머지는 EXPECTED 유지
  const handleConfirm = async () => {
    if (!confirmForm.stockId || !confirmForm.perShare) return
    const stock = selectedStock
    if (!stock) return

    // 분기 선택 있으면 그 row, 없으면 첫 번째 EXPECTED
    const targetId = confirmForm.dividendId
      ? Number(confirmForm.dividendId)
      : selectedStockExpected[0]?.id
    if (!targetId) return

    setConverting(true)
    try {
      await confirmDividend(targetId, {
        confirmedAmount:  Number(confirmForm.perShare) * stock.quantity,
        exDividendDate:   confirmForm.exDivDate  || null,
        paymentDate:      confirmForm.payDate    || null,
      })
      setConfirmForm({ stockId: '', dividendId: '', exDivDate: '', payDate: '', perShare: '' })
      await loadAll()
    } catch (e) {
      console.error('확정 전환 실패', e)
    } finally {
      setConverting(false)
    }
  }

  // 수정 모달 열기
  const handleOpenEdit = (stockItem) => {
    setEditModal(stockItem)
    setEditRows(stockItem.paymentDates.map(pd => ({
      ...pd,
      editAmount:       pd.amount,
      editExDivDate:    pd.exDividendDate ?? '',
      editPaymentDate:  pd.paymentDate,
      toConfirm:        false,
    })))
  }

  // 수정 모달 저장
  const handleSaveEdit = async () => {
    setSaving(true)
    // 수정 모달이 열린 종목의 보유수량 — 주당 배당금 × 수량 = 저장할 총액
    const editStock = stocks.find(s => s.id === editModal?.stockId)
    const qty = editStock?.quantity ?? 1
    try {
      for (const row of editRows) {
        const body = {
          amount:         Number(row.editAmount) * qty,   // 입력값(주당) × 보유수량 = 총액
          exDividendDate: row.editExDivDate   || null,
          paymentDate:    row.editPaymentDate || null,
        }
        if (row.status === 'CONFIRMED' || row.toConfirm) {
          body.status = 'CONFIRMED'
        }
        await updateDividend(row.dividendId, body)
      }
      setEditModal(null)
      await loadAll()
    } catch (e) {
      console.error('수정 저장 실패', e)
    } finally {
      setSaving(false)
    }
  }

  const barData = {
    labels: yearlyData.map(y => String(y.year)),
    datasets: [{
      label: '연간 배당금',
      data: yearlyData.map(y => y.expectedAmount ?? 0),
      backgroundColor: yearlyData.map((_, i, arr) =>
        i === arr.length - 1 ? 'rgba(29,158,117,0.75)'
        : i === arr.length - 2 ? '#5DCAA5'
        : i === arr.length - 3 ? '#9FE1CB'
        : '#C8EFE3'
      ),
      borderColor: yearlyData.map((_, i, arr) =>
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

  if (loading) return (
    <div className="dp-page dp-loading">
      <p>배당 데이터를 불러오는 중...</p>
    </div>
  )

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
          <p className="dp-sum-label">{CURRENT_YEAR} 연간 예상 배당금</p>
          <p className="dp-sum-value">{fmt(annualData?.totalExpectedAmount ?? 0)}<span className="dp-sum-unit">원</span></p>
          <p className="dp-sum-sub">12개월 예상 합계</p>
        </div>
        <div className="dp-card dp-sum-card">
          <p className="dp-sum-label">누적 배당금</p>
          <p className="dp-sum-value">{fmt(cumulativeData?.totalConfirmedAmount ?? 0)}<span className="dp-sum-unit">원</span></p>
          <p className="dp-sum-sub">올해 확정 금액 포함</p>
        </div>
      </div>

      {/* ── 월별 배당 조회 ── */}
      <div className="dp-card dp-monthly-section">
        <p className="dp-card-title">월별 배당 조회</p>
        <div className="dp-monthly-grid">
          {normalizedMonthly.map(m => (
            <div key={m.month} className={`dp-month-card${m.status === 'CONFIRMED' ? ' confirmed' : m.status === 'PARTIAL' ? ' partial' : ''}`}>
              <div className="dp-month-header">
                <span className="dp-month-name">{m.label}</span>
                <span className={`dp-month-badge ${m.status === 'CONFIRMED' ? 'confirmed' : m.status === 'PARTIAL' ? 'partial' : 'expected'}`}>
                  {m.status === 'CONFIRMED' ? '확정' : m.status === 'PARTIAL' ? '부분확정' : '예상'}
                </span>
              </div>
              <div className="dp-month-amounts">
                <div className="dp-month-row">
                  <span className="dp-month-row-label">예상</span>
                  <span className="dp-month-row-val">{m.expected > 0 ? fmt(m.expected) : '—'}</span>
                </div>
                <div className="dp-month-row">
                  <span className="dp-month-row-label confirmed">확정</span>
                  <span className={`dp-month-row-val${m.confirmed > 0 ? ' confirmed' : ' na'}`}>
                    {m.confirmed > 0 ? fmt(m.confirmed) : '—'}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ── 연도별 배당 차트 ── */}
      <div className="dp-card dp-bar-card">
        <p className="dp-card-title">연도별 배당</p>
        <div className="dp-bar-wrap">
          <Bar data={barData} options={barOptions} />
        </div>
      </div>

      {/* ── 확정 배당 전환 ── */}
      <div className="dp-card dp-convert-card">
        <p className="dp-card-title">확정 배당 전환</p>
        <div className="dp-convert-row">

          {/* ① 종목 선택 */}
          <select
            className="dp-fi dp-fi-select"
            value={confirmForm.stockId}
            onChange={e => setConfirmForm(f => ({
              ...f, stockId: e.target.value, dividendId: '', exDivDate: '', payDate: '', perShare: ''
            }))}
          >
            <option value="">종목 선택</option>
            {stocksForConfirm.map(s => (
              <option key={s.id} value={s.id}>
                {s.stockName}
                {s.dividendCycle === 'QUARTERLY' ? ' (분기)' :
                 s.dividendCycle === 'SEMI_ANNUAL' ? ' (반기)' : ''}
              </option>
            ))}
          </select>

          {/* ② 분기 선택 (EXPECTED 목록, 2개 이상일 때만 표시) */}
          {confirmForm.stockId && selectedStockExpected.length > 1 && (
            <select
              className="dp-fi dp-fi-select"
              value={confirmForm.dividendId}
              onChange={e => setConfirmForm(f => ({ ...f, dividendId: e.target.value }))}
            >
              <option value="">분기 선택</option>
              {selectedStockExpected.map(d => (
                <option key={d.id} value={d.id}>
                  {d.month}월
                </option>
              ))}
            </select>
          )}

          {/* ③ 배당락일 */}
          {confirmForm.stockId && (
            <input
              className="dp-fi dp-fi-date"
              type="date"
              placeholder="배당락일"
              value={confirmForm.exDivDate}
              onChange={e => setConfirmForm(f => ({ ...f, exDivDate: e.target.value }))}
            />
          )}

          {/* ④ 배당지급일 */}
          {confirmForm.stockId && (
            <input
              className="dp-fi dp-fi-date"
              type="date"
              placeholder="배당지급일"
              value={confirmForm.payDate}
              onChange={e => setConfirmForm(f => ({ ...f, payDate: e.target.value }))}
            />
          )}

          {/* ⑤ 주당 배당금 */}
          {confirmForm.stockId && (
            <input
              className="dp-fi dp-fi-amount"
              type="number"
              placeholder="주당 배당금 (원)"
              value={confirmForm.perShare}
              onChange={e => setConfirmForm(f => ({ ...f, perShare: e.target.value }))}
            />
          )}

          <button
            className="dp-convert-cancel"
            onClick={() => setConfirmForm({ stockId: '', dividendId: '', exDivDate: '', payDate: '', perShare: '' })}
          >취소</button>
          <button
            className="dp-convert-save"
            onClick={handleConfirm}
            disabled={!confirmForm.stockId || !confirmForm.perShare || converting}
          >{converting ? '저장 중...' : '저장'}</button>
        </div>
      </div>

      {/* ── 종목별 배당 정보 테이블 ── */}
      <div className="dp-card dp-stock-card">
        <p className="dp-card-title">종목별 배당 정보</p>
        <div className="dp-stock-table-wrap">
          <table className="dp-stock-table">
            <thead>
              <tr>
                <th>종목명</th>
                <th className="right">주당 배당금</th>
                <th className="center">배당락일</th>
                <th className="center">지급월</th>
                <th className="center">상태</th>
                <th className="right">예상 배당금</th>
                <th className="center">수정</th>
              </tr>
            </thead>
            <tbody>
              {byStockList.length === 0 ? (
                <tr>
                  <td colSpan={7} className="dp-empty">
                    배당 정보가 없습니다. 예상 배당을 생성해 주세요.
                  </td>
                </tr>
              ) : (
                byStockList.flatMap(item => {
                  const rows = item.paymentDates ?? []
                  return rows.map((pd, pdIdx) => {
                    // 경과: EXPECTED이고 isReceivable=false
                    const isExpired = pd.status === 'EXPECTED' && pd.isReceivable === false
                    const tagClass  = pd.status === 'CONFIRMED' ? 'confirmed' : isExpired ? 'expired' : 'expected'
                    const badgeClass = pd.status === 'CONFIRMED' ? 'confirmed' : isExpired ? 'expired' : 'expected'
                    const badgeLabel = pd.status === 'CONFIRMED' ? '확정' : isExpired ? '경과' : '예상'

                    return (
                      <tr
                        key={pd.dividendId}
                        className={isExpired ? 'dp-tr-expired' : ''}
                      >
                        {/* 종목명 — 첫 행만, rowSpan */}
                        {pdIdx === 0 && (
                          <td className="dp-stock-name" rowSpan={rows.length}>
                            {item.stockName}
                          </td>
                        )}

                        {/* 주당 배당금 (해당 분기 per-share) */}
                        <td className="right">
                          {isExpired ? <span style={{ color: '#ccc' }}>—</span> : `${fmt(pd.amount ?? 0)}원`}
                        </td>

                        {/* 배당락일 */}
                        <td className="center">
                          <span className={`dp-month-tag ${tagClass}`}>
                            {formatExDate(pd.exDividendDate, CURRENT_YEAR)}
                          </span>
                        </td>

                        {/* 지급월 */}
                        <td className="center">
                          <span className={`dp-month-tag ${tagClass}`}>
                            {formatPayMonth(pd.paymentDate, pd.paymentYear, CURRENT_YEAR)}
                          </span>
                        </td>

                        {/* 상태 */}
                        <td className="center">
                          <span className={`dp-status-badge ${badgeClass}`}>{badgeLabel}</span>
                        </td>

                        {/* 예상 배당금 (경과 EXPECTED는 "-") */}
                        <td className="right dp-expected-total">
                          {isExpired
                            ? <span style={{ color: '#ccc' }}>—</span>
                            : `${fmt(pd.totalAmount ?? 0)}원`}
                        </td>

                        {/* 수정 버튼 — 첫 행만, rowSpan */}
                        {pdIdx === 0 && (
                          <td className="center" rowSpan={rows.length}>
                            <button
                              className="dp-edit-btn"
                              onClick={() => handleOpenEdit(item)}
                            >수정</button>
                          </td>
                        )}
                      </tr>
                    )
                  })
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── 수정 모달 ── */}
      {editModal && (
        <div className="dp-modal-overlay" onClick={() => setEditModal(null)}>
          <div className="dp-modal" onClick={e => e.stopPropagation()}>
            <div className="dp-modal-header">
              <h3 className="dp-modal-title">{editModal.stockName} 배당 수정</h3>
              <button className="dp-modal-close" onClick={() => setEditModal(null)}>✕</button>
            </div>
            <div className="dp-modal-body">
              {editRows.map((row, idx) => (
                <div key={row.dividendId} className="dp-modal-row">
                  <div className="dp-modal-row-label">
                    <span className="dp-modal-month">
                      {formatPayMonth(row.paymentDate, row.paymentYear, CURRENT_YEAR)}
                    </span>
                    <span className={`dp-modal-badge ${row.status === 'CONFIRMED' ? 'confirmed' : 'expected'}`}>
                      {row.status === 'CONFIRMED' ? '확정' : '예상'}
                    </span>
                  </div>
                  <div className="dp-modal-inputs">
                    <input
                      className="dp-fi dp-fi-amount"
                      type="number"
                      placeholder="주당 배당금 (원)"
                      value={row.editAmount}
                      onChange={e => setEditRows(rows => rows.map((r, i) =>
                        i === idx ? { ...r, editAmount: e.target.value } : r
                      ))}
                    />
                    <input
                      className="dp-fi dp-fi-date"
                      type="date"
                      title="배당락일"
                      placeholder="배당락일"
                      value={row.editExDivDate}
                      onChange={e => setEditRows(rows => rows.map((r, i) =>
                        i === idx ? { ...r, editExDivDate: e.target.value } : r
                      ))}
                    />
                    <input
                      className="dp-fi dp-fi-date"
                      type="date"
                      title="배당지급일"
                      placeholder="배당지급일"
                      value={row.editPaymentDate}
                      onChange={e => setEditRows(rows => rows.map((r, i) =>
                        i === idx ? { ...r, editPaymentDate: e.target.value } : r
                      ))}
                    />
                    {row.status === 'EXPECTED' && (
                      <label className="dp-confirm-check">
                        <input
                          type="checkbox"
                          checked={row.toConfirm}
                          onChange={e => setEditRows(rows => rows.map((r, i) =>
                            i === idx ? { ...r, toConfirm: e.target.checked } : r
                          ))}
                        />
                        확정으로 전환
                      </label>
                    )}
                  </div>
                </div>
              ))}
            </div>
            <div className="dp-modal-footer">
              <button className="dp-convert-cancel" onClick={() => setEditModal(null)}>취소</button>
              <button className="dp-convert-save" onClick={handleSaveEdit} disabled={saving}>
                {saving ? '저장 중...' : '저장'}
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  )
}
