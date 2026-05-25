import { useState } from 'react'
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js'
import { Doughnut } from 'react-chartjs-2'
import {
  GICS_SECTORS,
  mockCurrentSectorWeights,
  mockTargetSectorWeights,
  mockWatchlist,
  mockWatchlistPrices,
  mockTotalAsset,
} from '../mock/rebalancingData'

ChartJS.register(ArcElement, Tooltip, Legend)

// ─── 유틸 ──────────────────────────────────────────────────────────────────────
function formatKRW(v) {
  return Number(v).toLocaleString('ko-KR') + '원'
}
function signPct(v, decimals = 1) {
  const n = Number(v)
  return (n > 0 ? '+' : '') + n.toFixed(decimals) + '%'
}

// ─── 뱃지 ──────────────────────────────────────────────────────────────────────
const BADGE_MAP = {
  초과:         { bg: '#fce7f3', color: '#be185d', text: '초과'           },
  부족:         { bg: '#dbeafe', color: '#1d4ed8', text: '부족'           },
  적정:         { bg: '#d1fae5', color: '#065f46', text: '적정'           },
  경고:         { bg: '#fee2e2', color: '#dc2626', text: '경고'           },
  예시메시지:   { bg: '#fef9c3', color: '#854d0e', text: '예시 메시지'    },
  매수후보:     { bg: '#dbeafe', color: '#1d4ed8', text: '매수 후보'      },
  추가매수주의: { bg: '#fce7f3', color: '#be185d', text: '추가 매수 주의' },
  소액분할가능: { bg: '#d1fae5', color: '#065f46', text: '소액 분할 가능' },
}

function Badge({ type }) {
  const b = BADGE_MAP[type]
  if (!b) return null
  return (
    <span style={{
      display: 'inline-block',
      padding: '3px 10px',
      borderRadius: 12,
      fontSize: 12,
      fontWeight: 600,
      background: b.bg,
      color: b.color,
      whiteSpace: 'nowrap',
    }}>
      {b.text}
    </span>
  )
}

// ─── 색상 팔레트 ───────────────────────────────────────────────────────────────
const COLORS = [
  '#10b981', '#3b82f6', '#f59e0b', '#ef4444', '#8b5cf6',
  '#06b6d4', '#84cc16', '#f97316', '#ec4899', '#6366f1', '#14b8a6',
]

// ─── 정적 맵 ───────────────────────────────────────────────────────────────────
const currentMap = Object.fromEntries(mockCurrentSectorWeights.map(s => [s.sector, s.current]))
const priceMap   = Object.fromEntries(mockWatchlistPrices.map(s => [s.name, s.currentPrice]))

// ─── 공통 스타일 ───────────────────────────────────────────────────────────────
const S = {
  card:  { background: '#fff', borderRadius: 12, padding: 24, marginBottom: 20, boxShadow: '0 1px 3px rgba(0,0,0,0.06)' },
  th:    { textAlign: 'left', padding: '8px 12px', fontSize: 13, color: '#6b7280', fontWeight: 500 },
  td:    { padding: '12px 12px', fontSize: 14, color: '#374151' },
  input: { padding: '7px 10px', border: '1px solid #e5e7eb', borderRadius: 8, fontSize: 13, outline: 'none', background: '#fff', boxSizing: 'border-box' },
}

// ─── 섹터 상태 판단 ────────────────────────────────────────────────────────────
function sectorStatus(cur, tgt) {
  if (cur - tgt >  2) return '초과'
  if (tgt - cur >  2) return '부족'
  return '적정'
}

// ─── Chart 옵션 공장 ───────────────────────────────────────────────────────────
function makeChartOpts(labelFn) {
  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: { callbacks: { label: labelFn } },
    },
  }
}

// ──────────────────────────────────────────────────────────────────────────────
export default function RebalancingPage() {

  // ── 섹션 1 상태 ──────────────────────────────────────────────────────────────
  const [rows, setRows] = useState(() =>
    mockTargetSectorWeights.map((s, i) => ({ id: i, sector: s.sector, target: s.target }))
  )
  const [saved, setSaved] = useState(mockTargetSectorWeights)

  // ── 섹션 4 상태 ──────────────────────────────────────────────────────────────
  const [watchlist, setWatchlist] = useState(mockWatchlist)
  const [editId,   setEditId]    = useState(null)
  const [editData, setEditData]  = useState({})
  const [form, setForm] = useState({
    name: '', currentPrice: '', targetPrice: '', sector: '', alertDropRate: '',
  })

  // ── 섹션 6–7 상태 ────────────────────────────────────────────────────────────
  const [simItems,  setSimItems]  = useState([])   // 추가된 항목 목록
  const [simForm,   setSimForm]   = useState({ stockName: '', totalAmount: '', splitCount: '1', sector: '' })
  const [simQuery,  setSimQuery]  = useState(null)
  const [simResult, setSimResult] = useState(null)

  // ────────────────────────────────────────────────────────────────────────────
  // 섹션 1 로직
  // ────────────────────────────────────────────────────────────────────────────
  const total   = rows.reduce((s, r) => s + (parseFloat(r.target) || 0), 0)
  const canSave = Math.abs(total - 100) < 0.01
  const totalDisplay = Number.isInteger(total) ? String(total) : total.toFixed(1)

  function getAvailableSectors(rowId) {
    const used = rows.filter(r => r.id !== rowId).map(r => r.sector).filter(Boolean)
    return GICS_SECTORS.filter(s => !used.includes(s))
  }
  function addRow()             { setRows(p => [...p, { id: Date.now(), sector: '', target: 0 }]) }
  function removeRow(id)        { setRows(p => p.filter(r => r.id !== id)) }
  function updateRow(id, k, v)  { setRows(p => p.map(r => r.id === id ? { ...r, [k]: v } : r)) }
  function saveSectors()        { setSaved(rows.filter(r => r.sector).map(r => ({ sector: r.sector, target: parseFloat(r.target) || 0 }))) }

  // ────────────────────────────────────────────────────────────────────────────
  // 섹션 2 파생 — 비교 행 + 차트 데이터
  // ────────────────────────────────────────────────────────────────────────────
  const compareRows = saved.map((s, i) => ({
    sector:  s.sector,
    current: currentMap[s.sector] ?? 0,
    target:  s.target,
    color:   COLORS[i % COLORS.length],
  }))

  const mainChartData = {
    labels: compareRows.map(r => r.sector),
    datasets: [{
      data: compareRows.map(r => r.current || 0.5),
      backgroundColor: compareRows.map(r => r.color),
      borderWidth: 2,
      borderColor: '#fff',
    }],
  }
  const mainChartOpts = makeChartOpts(ctx => ` ${ctx.label}: ${compareRows[ctx.dataIndex]?.current ?? 0}%`)

  // ────────────────────────────────────────────────────────────────────────────
  // 섹션 3 파생 — 분석 결과
  // ────────────────────────────────────────────────────────────────────────────
  const analysisResults = compareRows
    .map(r => {
      const diff = r.current - r.target
      if (Math.abs(diff) <= 2) return null
      const isOver   = diff > 0
      const direction = isOver ? '초과' : '부족'
      const action    = isOver ? '매도 또는 신규 매수 자제' : '매수'
      return { sector: r.sector, diff, isOver, message: `목표 대비 ${Math.abs(diff).toFixed(0)}% ${direction}이므로 해당 섹터 ${action}가 적절` }
    })
    .filter(Boolean)

  // ────────────────────────────────────────────────────────────────────────────
  // 섹션 4 로직
  // ────────────────────────────────────────────────────────────────────────────
  function addStock() {
    if (!form.name.trim() || !form.targetPrice) return
    setWatchlist(p => [...p, {
      id: Date.now(),
      name: form.name.trim(),
      currentPrice: Number(form.currentPrice) || (priceMap[form.name.trim()] ?? 0),
      targetPrice: Number(form.targetPrice),
      sector: form.sector,
      alertDropRate: Number(form.alertDropRate) || 0,
    }])
    setForm({ name: '', currentPrice: '', targetPrice: '', sector: '', alertDropRate: '' })
  }
  function removeStock(id)  { setWatchlist(p => p.filter(s => s.id !== id)) }
  function startEdit(stock) { setEditId(stock.id); setEditData({ ...stock }) }
  function cancelEdit()     { setEditId(null); setEditData({}) }
  function saveEdit() {
    setWatchlist(p => p.map(s => s.id === editId ? { ...editData, id: s.id } : s))
    cancelEdit()
  }

  // ────────────────────────────────────────────────────────────────────────────
  // 섹션 5 파생 — 3단계 매수 추천 분류
  // ────────────────────────────────────────────────────────────────────────────
  function getStockRec(s) {
    const cur = s.currentPrice || priceMap[s.name] || 0
    const tgt = s.targetPrice
    if (!cur || !tgt) return { rec: '소액분할가능', dropRate: null }
    const dropRate = (cur - tgt) / tgt * 100           // 음수 = 하락
    const absDrop  = Math.abs(Math.min(dropRate, 0))   // 하락분만 계산
    const threshold = s.alertDropRate || 5
    let rec
    if (absDrop >= threshold)         rec = '매수후보'
    else if (absDrop >= threshold / 2) rec = '추가매수주의'
    else                               rec = '소액분할가능'
    return { rec, dropRate }
  }

  // 분할 매수 방향 제안 텍스트
  function getSplitAdvice(rec, s) {
    const thr = s.alertDropRate
    if (rec === '매수후보')
      return `설정 기준 -${thr}% 하락 달성. 보유 현금의 30% 이내에서 1차 매수를 권장합니다.`
    if (rec === '추가매수주의')
      return `기준 하락률의 50% 이상 도달. 소량 선매수 후 추가 하락 시 분할 매수를 고려하세요.`
    return `아직 기준 하락률에 미달. 소액 분할 매수 시작 또는 관망을 권장합니다.`
  }

  // ── 섹션 8 파생 — 부족 섹터 × 관심 종목 교차 추천 ──────────────────────────
  const shortfallSectors = compareRows.filter(r => sectorStatus(r.current, r.target) === '부족')
  const priorityBuyList  = shortfallSectors.map(r => ({
    sector:     r.sector,
    current:    r.current,
    target:     r.target,
    shortfall:  r.target - r.current,
    candidates: watchlist.filter(s => s.sector === r.sector),
  }))

  // ────────────────────────────────────────────────────────────────────────────
  // 섹션 6–7 로직 — 시뮬레이션 (다중 항목)
  // ────────────────────────────────────────────────────────────────────────────

  // 종목 조회 (폼의 종목명 기준)
  function queryStock() {
    const name  = simForm.stockName.trim()
    const found = watchlist.find(s => s.name === name)
    const cp    = found?.currentPrice || priceMap[name] || 0
    setSimQuery({ name, currentPrice: cp, sector: found?.sector || '–' })
  }

  // 항목 추가
  function addSimItem() {
    if (!simForm.stockName.trim() || !simForm.totalAmount) return
    setSimItems(p => [...p, { id: Date.now(), ...simForm }])
    setSimForm({ stockName: '', totalAmount: '', splitCount: '1', sector: '' })
    setSimResult(null)   // 항목이 바뀌면 이전 결과 초기화
  }

  // 항목 삭제
  function removeSimItem(id) {
    setSimItems(p => p.filter(item => item.id !== id))
    setSimResult(null)
  }

  // 시뮬레이션 실행 (여러 항목 합산)
  function runSimulation() {
    if (simItems.length === 0) return

    let totalInvestment = 0
    const sectorInvestments = {}   // sector → 투자금액 합계
    const itemDetails = []

    for (const item of simItems) {
      const totalAmt = Number(item.totalAmount)
      const splits   = Math.max(1, Number(item.splitCount) || 1)
      if (!item.stockName || !totalAmt) continue

      const found    = watchlist.find(s => s.name === item.stockName)
      const curPrice = found?.currentPrice || priceMap[item.stockName] || 0
      const sector   = found?.sector || item.sector || ''

      const perSplit    = totalAmt / splits
      const qtyPerSplit = curPrice > 0 ? Math.floor(perSplit / curPrice) : 0
      const totalQty    = qtyPerSplit * splits
      const actualSpend = curPrice > 0 ? curPrice * totalQty : totalAmt

      totalInvestment += actualSpend
      if (sector) sectorInvestments[sector] = (sectorInvestments[sector] || 0) + actualSpend

      itemDetails.push({ name: item.stockName, sector, curPrice, splits, qtyPerSplit, totalQty, actualSpend })
    }

    if (totalInvestment === 0) return

    const newTotal = mockTotalAsset + totalInvestment
    const DYIELD   = 0.03

    // 섹터 비중 재계산 (전체 투자 합산)
    const afterWeights = mockCurrentSectorWeights.map(s => {
      const prev  = mockTotalAsset * s.current / 100
      const added = sectorInvestments[s.sector] || 0
      return { sector: s.sector, before: s.current, after: +((prev + added) / newTotal * 100).toFixed(1) }
    })
    const nonZeroAfter = afterWeights.filter(w => w.after > 0)

    // 배당 계산
    const curDiv  = 487500
    const newDiv  = Math.round(totalInvestment * DYIELD)
    const curRate = +(curDiv / mockTotalAsset * 100).toFixed(2)
    const newRate = +((curDiv + newDiv) / newTotal * 100).toFixed(2)

    // 목표 달성률
    const afMap       = Object.fromEntries(afterWeights.map(w => [w.sector, w.after]))
    const inRange     = (c, t) => Math.abs(c - t) <= 2
    const beforeMatch = saved.filter(s => inRange(currentMap[s.sector] ?? 0, s.target)).length
    const afterMatch  = saved.filter(s => inRange(afMap[s.sector]  ?? 0, s.target)).length
    const tgtBefore   = saved.length ? Math.round(beforeMatch / saved.length * 100) : 0
    const tgtAfter    = saved.length ? Math.round(afterMatch  / saved.length * 100) : 0

    // 가장 많이 투자한 섹터를 대표 섹터로
    const topSector = Object.entries(sectorInvestments).sort((a, b) => b[1] - a[1])[0]?.[0] || ''

    setSimResult({
      itemDetails,
      totalInvestment,
      afterWeights: nonZeroAfter,
      cmp: {
        totalAsset:     { before: mockTotalAsset, after: newTotal },
        annualDividend: { before: curDiv,  after: curDiv + newDiv },
        dividendRate:   { before: curRate, after: newRate },
        sectorWeight:   {
          label:  topSector || '투자 섹터',
          before: currentMap[topSector] ?? 0,
          after:  afterWeights.find(w => w.sector === topSector)?.after ?? 0,
        },
        targetRate:     { before: tgtBefore, after: tgtAfter },
      },
    })
  }

  // 시뮬레이션 차트 데이터 생성 헬퍼
  const currentNonZero = mockCurrentSectorWeights.filter(s => s.current > 0)

  function buildChartData(items, getValue) {
    return {
      labels: items.map(s => s.sector),
      datasets: [{
        data: items.map(s => getValue(s) || 0.5),
        backgroundColor: items.map((_, i) => COLORS[i % COLORS.length]),
        borderWidth: 2,
        borderColor: '#fff',
      }],
    }
  }

  // ────────────────────────────────────────────────────────────────────────────
  // 렌더
  // ────────────────────────────────────────────────────────────────────────────
  return (
    <div style={{
      margin: '-20px -24px',
      padding: '24px',
      background: '#f5f5f0',
      minHeight: 'calc(100% + 40px)',
      boxSizing: 'border-box',
    }}>

      {/* ── 페이지 헤더 ──────────────────────────────────────────────────────── */}
      <h1 style={{ fontSize: 22, fontWeight: 700, color: '#111827', margin: '0 0 20px 0' }}>
        리밸런싱 / 가상 포트폴리오
      </h1>

      {/* ══════════════════════════════════════════════════════════════════════
          섹션 1: 목표 섹터 비중 설정 — 4열 테이블
         ══════════════════════════════════════════════════════════════════════ */}
      <div style={S.card}>
        <h2 style={{ fontSize: 16, fontWeight: 700, color: '#111827', margin: '0 0 4px 0' }}>목표 섹터 비중 설정</h2>
        <p style={{ fontSize: 13, color: '#6b7280', margin: '0 0 16px 0' }}>
          섹터를 추가하고 목표 비중(%)을 입력하세요. 합계는 100%여야 합니다.
        </p>

        <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 12 }}>
          <thead>
            <tr style={{ borderBottom: '1px solid #f3f4f6' }}>
              <th style={S.th}>섹터명</th>
              <th style={S.th}>현재 비중 (%)</th>
              <th style={S.th}>목표 비중 (%)</th>
              <th style={S.th}>차이</th>
              <th style={{ ...S.th, width: 36 }} />
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td colSpan={5} style={{ textAlign: 'center', padding: '20px 0', color: '#10b981', fontSize: 14 }}>
                  아래 행 추가 버튼으로 섹터를 추가하세요
                </td>
              </tr>
            ) : rows.map(r => {
              const cur  = r.sector ? (currentMap[r.sector] ?? 0) : 0
              const diff = (parseFloat(r.target) || 0) - cur
              return (
                <tr key={r.id} style={{ borderBottom: '1px solid #f9fafb' }}>
                  {/* 섹터명 드롭다운 */}
                  <td style={S.td}>
                    <select
                      value={r.sector}
                      onChange={e => updateRow(r.id, 'sector', e.target.value)}
                      style={{ ...S.input, minWidth: 130, cursor: 'pointer', color: r.sector ? '#111827' : '#9ca3af' }}
                    >
                      <option value="">섹터 선택</option>
                      {getAvailableSectors(r.id).map(s => <option key={s} value={s}>{s}</option>)}
                    </select>
                  </td>
                  {/* 현재 비중 (읽기 전용) */}
                  <td style={{ ...S.td, color: '#6b7280' }}>
                    {r.sector ? `${cur}%` : '–'}
                  </td>
                  {/* 목표 비중 입력 */}
                  <td style={S.td}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                      <input
                        type="number" min={0} max={100}
                        value={r.target}
                        onChange={e => updateRow(r.id, 'target', e.target.value)}
                        style={{ ...S.input, width: 68, textAlign: 'center' }}
                      />
                      <span style={{ fontSize: 13, color: '#6b7280' }}>%</span>
                    </div>
                  </td>
                  {/* 차이 (자동 계산) */}
                  <td style={{
                    ...S.td, fontWeight: 600,
                    color: diff > 0 ? '#10b981' : diff < 0 ? '#ef4444' : '#9ca3af',
                  }}>
                    {r.sector ? signPct(diff) : '–'}
                  </td>
                  {/* 삭제 */}
                  <td style={{ padding: '12px 8px' }}>
                    <button
                      onClick={() => removeRow(r.id)}
                      style={{ width: 30, height: 30, border: '1px solid #e5e7eb', borderRadius: 6, background: '#fff', cursor: 'pointer', color: '#ef4444', fontSize: 13, display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                    >✕</button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>

        {/* 하단: 행 추가 + 합계 + 저장 */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <button
            onClick={addRow}
            style={{ padding: '8px 14px', background: '#f3f4f6', border: '1px solid #e5e7eb', borderRadius: 8, fontSize: 13, fontWeight: 500, cursor: 'pointer', color: '#374151' }}
          >+ 행 추가</button>

          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <span style={{ fontSize: 13, fontWeight: 600, color: canSave ? '#10b981' : '#ef4444' }}>
              합계: {totalDisplay}%
            </span>
            <button
              onClick={saveSectors}
              disabled={!canSave}
              style={{ padding: '8px 20px', background: canSave ? '#111827' : '#e5e7eb', color: canSave ? '#fff' : '#9ca3af', border: 'none', borderRadius: 8, fontSize: 14, fontWeight: 600, cursor: canSave ? 'pointer' : 'not-allowed' }}
            >저장</button>
          </div>
        </div>
      </div>

      {/* ══════════════════════════════════════════════════════════════════════
          섹션 2: 현재 vs 목표 비중 비교 — 차트 전용
         ══════════════════════════════════════════════════════════════════════ */}
      {saved.length > 0 && (
        <div style={S.card}>
          <h2 style={{ fontSize: 16, fontWeight: 700, color: '#111827', margin: '0 0 16px 0' }}>현재 vs 목표 비중 비교</h2>

          {/* 도넛 차트 */}
          <div style={{ border: '1.5px dashed #d1d5db', borderRadius: 12, height: 260, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
            <Doughnut key="main-donut" data={mainChartData} options={mainChartOpts} />
          </div>

          {/* 범례 — 섹터명 · 현재% / 목표% · 상태 뱃지 */}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, marginTop: 16 }}>
            {compareRows.map(r => (
              <div key={r.sector} style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 260 }}>
                <div style={{ width: 12, height: 12, borderRadius: '50%', background: r.color, flexShrink: 0 }} />
                <span style={{ fontSize: 13, fontWeight: 500, color: '#111827' }}>{r.sector}</span>
                <span style={{ fontSize: 13, color: '#6b7280' }}>현재 {r.current}% / 목표 {r.target}%</span>
                <Badge type={sectorStatus(r.current, r.target)} />
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ══════════════════════════════════════════════════════════════════════
          섹션 3: 리밸런싱 조치/분석 결과
         ══════════════════════════════════════════════════════════════════════ */}
      {analysisResults.length > 0 && (
        <div style={S.card}>
          <h2 style={{ fontSize: 16, fontWeight: 700, color: '#111827', margin: '0 0 16px 0' }}>리밸런싱 조치/분석 결과</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {analysisResults.map((r, i) => (
              <div
                key={i}
                style={{
                  display: 'flex', alignItems: 'center', gap: 12,
                  padding: '12px 16px', borderRadius: 8, background: '#f9fafb',
                  borderLeft: `4px solid ${r.isOver ? '#ef4444' : '#10b981'}`,
                }}
              >
                {/* 방향 아이콘 */}
                <span style={{ fontSize: 18, flexShrink: 0 }}>{r.isOver ? '⬇' : '⬆'}</span>

                {/* 섹터명 칩 */}
                <span style={{
                  display: 'inline-block', padding: '2px 12px', borderRadius: 8, fontSize: 13, fontWeight: 600, flexShrink: 0,
                  background: r.isOver ? '#fee2e2' : '#d1fae5',
                  color: r.isOver ? '#dc2626' : '#065f46',
                }}>
                  {r.sector}
                </span>

                {/* 분석 문구 */}
                <span style={{ flex: 1, fontSize: 14, color: '#374151' }}>{r.message}</span>

                {/* 예시 메시지 뱃지 */}
                <Badge type="예시메시지" />
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ══════════════════════════════════════════════════════════════════════
          섹션 4: 관심 종목 등록 — 6열 테이블 + 인라인 수정
         ══════════════════════════════════════════════════════════════════════ */}
      <div style={S.card}>
        <h2 style={{ fontSize: 16, fontWeight: 700, color: '#111827', margin: '0 0 4px 0' }}>관심 종목 등록</h2>
        <p style={{ fontSize: 13, color: '#10b981', margin: '0 0 16px 0' }}>
          관심 종목을 등록하고 목표 가격과 하락 알림 기준을 설정하세요.
        </p>

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 16, minWidth: 680 }}>
            <thead>
              <tr style={{ borderBottom: '1px solid #f3f4f6' }}>
                {['종목명', '현재가', '월/기준가', '섹터', '분할 매수 조건', '관리'].map((h, i) => (
                  <th key={i} style={S.th}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {watchlist.map(s => {
                const isEdit = editId === s.id
                const cur    = s.currentPrice || priceMap[s.name] || 0
                return (
                  <tr key={s.id} style={{ borderBottom: '1px solid #f9fafb', background: isEdit ? '#f0fdf4' : 'transparent' }}>
                    {/* 종목명 */}
                    <td style={{ ...S.td, fontWeight: 500, color: '#111827' }}>
                      {isEdit
                        ? <input value={editData.name || ''} onChange={e => setEditData(p => ({ ...p, name: e.target.value }))} style={{ ...S.input, width: '100%' }} />
                        : s.name}
                    </td>
                    {/* 현재가 */}
                    <td style={S.td}>
                      {isEdit
                        ? <input type="number" value={editData.currentPrice || ''} onChange={e => setEditData(p => ({ ...p, currentPrice: Number(e.target.value) }))} style={{ ...S.input, width: 90 }} />
                        : (cur ? formatKRW(cur) : '–')}
                    </td>
                    {/* 월/기준가 */}
                    <td style={S.td}>
                      {isEdit
                        ? <input type="number" value={editData.targetPrice || ''} onChange={e => setEditData(p => ({ ...p, targetPrice: Number(e.target.value) }))} style={{ ...S.input, width: 90 }} />
                        : formatKRW(s.targetPrice)}
                    </td>
                    {/* 섹터 */}
                    <td style={S.td}>
                      {isEdit
                        ? (
                          <select value={editData.sector || ''} onChange={e => setEditData(p => ({ ...p, sector: e.target.value }))} style={{ ...S.input, cursor: 'pointer' }}>
                            <option value="">–</option>
                            {GICS_SECTORS.map(gs => <option key={gs} value={gs}>{gs}</option>)}
                          </select>
                        )
                        : (s.sector || '–')}
                    </td>
                    {/* 분할 매수 조건 */}
                    <td style={S.td}>
                      {isEdit
                        ? <input type="number" value={editData.alertDropRate || ''} onChange={e => setEditData(p => ({ ...p, alertDropRate: Number(e.target.value) }))} style={{ ...S.input, width: 60 }} />
                        : `-${s.alertDropRate}%`}
                    </td>
                    {/* 관리 버튼 */}
                    <td style={{ padding: '12px 12px' }}>
                      <div style={{ display: 'flex', gap: 6 }}>
                        {isEdit ? (
                          <>
                            <button onClick={saveEdit}   style={{ padding: '4px 10px', background: '#10b981', color: '#fff', border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 12, fontWeight: 600 }}>저장</button>
                            <button onClick={cancelEdit} style={{ padding: '4px 10px', background: '#f3f4f6', color: '#374151', border: '1px solid #e5e7eb', borderRadius: 6, cursor: 'pointer', fontSize: 12 }}>취소</button>
                          </>
                        ) : (
                          <>
                            <button onClick={() => startEdit(s)}  style={{ padding: '4px 10px', border: '1px solid #e5e7eb', borderRadius: 6, background: '#fff', cursor: 'pointer', fontSize: 12, color: '#374151' }}>수정</button>
                            <button onClick={() => removeStock(s.id)} style={{ padding: '4px 10px', border: '1px solid #e5e7eb', borderRadius: 6, background: '#fff', cursor: 'pointer', fontSize: 12, color: '#ef4444' }}>🗑️</button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>

        {/* 인라인 추가 폼 */}
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
          <input placeholder="종목명 검색"       value={form.name}         onChange={e => setForm(p => ({ ...p, name: e.target.value }))}         style={{ ...S.input, flex: '2 1 100px' }} />
          <input placeholder="현재가"            type="number" value={form.currentPrice}  onChange={e => setForm(p => ({ ...p, currentPrice: e.target.value }))}  style={{ ...S.input, flex: '1 1 80px' }} />
          <input placeholder="월/기준가"         type="number" value={form.targetPrice}   onChange={e => setForm(p => ({ ...p, targetPrice: e.target.value }))}   style={{ ...S.input, flex: '1 1 80px' }} />
          <select value={form.sector} onChange={e => setForm(p => ({ ...p, sector: e.target.value }))}
            style={{ ...S.input, flex: '1 1 100px', cursor: 'pointer', color: form.sector ? '#111827' : '#9ca3af' }}>
            <option value="">섹터 선택</option>
            {GICS_SECTORS.map(s => <option key={s} value={s}>{s}</option>)}
          </select>
          <input placeholder="분할매수 조건 (%)" type="number" value={form.alertDropRate} onChange={e => setForm(p => ({ ...p, alertDropRate: e.target.value }))} style={{ ...S.input, flex: '1 1 80px' }} />
          <button onClick={addStock} style={{ padding: '8px 16px', background: '#10b981', color: '#fff', border: 'none', borderRadius: 8, fontSize: 14, fontWeight: 600, cursor: 'pointer', whiteSpace: 'nowrap' }}>
            + 추가
          </button>
        </div>
      </div>

      {/* ══════════════════════════════════════════════════════════════════════
          섹션 5: 관심 종목 목록 — 3단계 매수 추천 분류
         ══════════════════════════════════════════════════════════════════════ */}
      <div style={S.card}>
        <h2 style={{ fontSize: 16, fontWeight: 700, color: '#111827', margin: '0 0 16px 0' }}>관심 종목 목록</h2>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid #f3f4f6' }}>
              {['종목명', '현재가', '하락률', '분할 매수 조건', '매수 추천'].map((h, i) => (
                <th key={i} style={S.th}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {watchlist.map(s => {
              const cur = s.currentPrice || priceMap[s.name] || 0
              const { rec, dropRate } = getStockRec(s)
              return (
                <tr key={s.id} style={{ borderBottom: '1px solid #f9fafb' }}>
                  <td style={{ ...S.td, fontWeight: 500, color: '#111827' }}>{s.name}</td>
                  <td style={S.td}>{cur ? formatKRW(cur) : '–'}</td>
                  <td style={{ ...S.td, color: dropRate !== null && dropRate < 0 ? '#ef4444' : '#6b7280' }}>
                    {dropRate !== null ? signPct(dropRate) : '–'}
                  </td>
                  <td style={S.td}>{s.alertDropRate ? `-${s.alertDropRate}% 이하 하락 시` : '–'}</td>
                  <td style={{ padding: '12px 12px' }}><Badge type={rec} /></td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {/* ══════════════════════════════════════════════════════════════════════
          섹션 5-B: 분할 매수 방향 제안
         ══════════════════════════════════════════════════════════════════════ */}
      <div style={S.card}>
        <h2 style={{ fontSize: 16, fontWeight: 700, color: '#111827', margin: '0 0 4px 0' }}>분할 매수 방향 제안</h2>
        <p style={{ fontSize: 13, color: '#6b7280', margin: '0 0 14px 0' }}>
          관심 종목별 현재 하락률에 따른 분할 매수 전략을 안내합니다.
        </p>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {watchlist.map(s => {
            const { rec, dropRate } = getStockRec(s)
            const advice = getSplitAdvice(rec, s)
            return (
              <div key={s.id} style={{
                display: 'flex', alignItems: 'center', gap: 12,
                padding: '12px 16px', borderRadius: 8, background: '#f9fafb',
                borderLeft: `4px solid ${rec === '매수후보' ? '#1d4ed8' : rec === '추가매수주의' ? '#be185d' : '#065f46'}`,
              }}>
                <span style={{ fontSize: 14, fontWeight: 600, color: '#111827', minWidth: 90, flexShrink: 0 }}>{s.name}</span>
                <Badge type={rec} />
                <span style={{ fontSize: 13, color: dropRate !== null && dropRate < 0 ? '#ef4444' : '#6b7280', minWidth: 60, flexShrink: 0 }}>
                  {dropRate !== null ? signPct(dropRate) : '–'}
                </span>
                <span style={{ flex: 1, fontSize: 13, color: '#374151' }}>{advice}</span>
              </div>
            )
          })}
        </div>
      </div>

      {/* ══════════════════════════════════════════════════════════════════════
          섹션 5-C: 부족 섹터 우선 매수 추천
         ══════════════════════════════════════════════════════════════════════ */}
      {priorityBuyList.length > 0 && (
        <div style={S.card}>
          <h2 style={{ fontSize: 16, fontWeight: 700, color: '#111827', margin: '0 0 4px 0' }}>부족 섹터 우선 매수 추천</h2>
          <p style={{ fontSize: 13, color: '#6b7280', margin: '0 0 14px 0' }}>
            목표 비중 대비 부족한 섹터와 해당 섹터의 관심 종목을 연결합니다.
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {priorityBuyList.map((row, i) => (
              <div key={i} style={{ borderRadius: 8, border: '1px solid #e5e7eb', overflow: 'hidden' }}>
                {/* 섹터 헤더 */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 16px', background: '#eff6ff' }}>
                  <Badge type="부족" />
                  <span style={{ fontSize: 14, fontWeight: 600, color: '#1d4ed8' }}>{row.sector}</span>
                  <span style={{ fontSize: 13, color: '#4b5563' }}>
                    현재 {row.current}% → 목표 {row.target}% (부족 {row.shortfall.toFixed(1)}%)
                  </span>
                </div>
                {/* 후보 종목 */}
                {row.candidates.length > 0 ? (
                  <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                    <thead>
                      <tr style={{ background: '#f9fafb', borderBottom: '1px solid #f3f4f6' }}>
                        {['종목명', '현재가', '목표가', '하락률', '추천 단계'].map((h, j) => (
                          <th key={j} style={{ ...S.th, fontSize: 12 }}>{h}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {row.candidates.map(s => {
                        const { rec, dropRate } = getStockRec(s)
                        return (
                          <tr key={s.id} style={{ borderBottom: '1px solid #f9fafb' }}>
                            <td style={{ ...S.td, fontWeight: 500, color: '#111827' }}>{s.name}</td>
                            <td style={S.td}>{formatKRW(s.currentPrice)}</td>
                            <td style={S.td}>{formatKRW(s.targetPrice)}</td>
                            <td style={{ ...S.td, color: dropRate !== null && dropRate < 0 ? '#ef4444' : '#6b7280' }}>
                              {dropRate !== null ? signPct(dropRate) : '–'}
                            </td>
                            <td style={{ padding: '12px 12px' }}><Badge type={rec} /></td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                ) : (
                  <p style={{ fontSize: 13, color: '#9ca3af', padding: '10px 16px', margin: 0 }}>
                    해당 섹터에 등록된 관심 종목이 없습니다.
                  </p>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ══════════════════════════════════════════════════════════════════════
          섹션 6: 가상 매수 시뮬레이터 — 다중 항목
         ══════════════════════════════════════════════════════════════════════ */}
      <div style={S.card}>
        <h2 style={{ fontSize: 16, fontWeight: 700, color: '#111827', margin: '0 0 4px 0' }}>가상 매수 시뮬레이터</h2>
        <p style={{ fontSize: 13, color: '#6b7280', margin: '0 0 14px 0' }}>
          여러 종목을 추가한 뒤 시뮬레이션을 실행하면 합산된 포트폴리오 변화를 확인할 수 있습니다.
        </p>

        {/* ── 항목 추가 폼 ── */}
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginBottom: 14 }}>
          <input
            placeholder="종목명 검색"
            value={simForm.stockName}
            onChange={e => setSimForm(p => ({ ...p, stockName: e.target.value }))}
            onKeyDown={e => e.key === 'Enter' && addSimItem()}
            style={{ ...S.input, flex: '2 1 120px' }}
          />
          <select
            value={simForm.sector}
            onChange={e => setSimForm(p => ({ ...p, sector: e.target.value }))}
            style={{ ...S.input, flex: '1 1 120px', cursor: 'pointer', color: simForm.sector ? '#111827' : '#9ca3af' }}
          >
            <option value="">섹터 선택</option>
            {GICS_SECTORS.map(s => <option key={s} value={s}>{s}</option>)}
          </select>
          <input
            placeholder="총 투자 금액 (원)"
            type="number"
            value={simForm.totalAmount}
            onChange={e => setSimForm(p => ({ ...p, totalAmount: e.target.value }))}
            onKeyDown={e => e.key === 'Enter' && addSimItem()}
            style={{ ...S.input, flex: '2 1 120px' }}
          />
          <input
            placeholder="분할매수 횟수 (회)"
            type="number" min={1}
            value={simForm.splitCount}
            onChange={e => setSimForm(p => ({ ...p, splitCount: e.target.value }))}
            onKeyDown={e => e.key === 'Enter' && addSimItem()}
            style={{ ...S.input, flex: '1 1 70px' }}
          />
          <button
            onClick={queryStock}
            style={{ padding: '8px 12px', background: '#f3f4f6', border: '1px solid #e5e7eb', borderRadius: 8, fontSize: 13, fontWeight: 500, cursor: 'pointer', color: '#374151', whiteSpace: 'nowrap' }}
          >조회</button>
          <button
            onClick={addSimItem}
            style={{ padding: '8px 14px', background: '#10b981', color: '#fff', border: 'none', borderRadius: 8, fontSize: 13, fontWeight: 600, cursor: 'pointer', whiteSpace: 'nowrap' }}
          >+ 추가</button>
        </div>

        {/* 조회 결과 */}
        {simQuery && (
          <div style={{ display: 'flex', gap: 24, padding: '10px 14px', background: '#f0fdf4', border: '1px solid #d1fae5', borderRadius: 8, fontSize: 13, flexWrap: 'wrap', marginBottom: 14 }}>
            <span><strong>종목명:</strong> {simQuery.name || '–'}</span>
            <span><strong>현재가:</strong> {simQuery.currentPrice ? formatKRW(simQuery.currentPrice) : '–'}</span>
            <span><strong>섹터:</strong> {simQuery.sector}</span>
          </div>
        )}

        {/* ── 추가된 항목 테이블 ── */}
        {simItems.length > 0 && (
          <>
            <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 12 }}>
              <thead>
                <tr style={{ borderBottom: '1px solid #f3f4f6' }}>
                  {['종목명', '총 투자금액', '분할매수 횟수', '예상 섹터', '삭제'].map((h, i) => (
                    <th key={i} style={S.th}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {simItems.map(item => {
                  const found      = watchlist.find(s => s.name === item.stockName)
                  const sectorDisp = found?.sector || item.sector || '–'
                  return (
                    <tr key={item.id} style={{ borderBottom: '1px solid #f9fafb' }}>
                      <td style={{ ...S.td, fontWeight: 500, color: '#111827' }}>{item.stockName}</td>
                      <td style={S.td}>{formatKRW(Number(item.totalAmount))}</td>
                      <td style={S.td}>{item.splitCount}회</td>
                      <td style={S.td}>{sectorDisp}</td>
                      <td style={{ padding: '12px 12px' }}>
                        <button
                          onClick={() => removeSimItem(item.id)}
                          style={{ padding: '4px 10px', border: '1px solid #e5e7eb', borderRadius: 6, background: '#fff', cursor: 'pointer', fontSize: 12, color: '#ef4444' }}
                        >✕</button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
              {/* 합계 행 */}
              <tfoot>
                <tr style={{ borderTop: '1px solid #e5e7eb', background: '#f9fafb' }}>
                  <td style={{ ...S.td, fontWeight: 600, color: '#111827' }}>합계</td>
                  <td style={{ ...S.td, fontWeight: 600, color: '#10b981' }}>
                    {formatKRW(simItems.reduce((s, item) => s + Number(item.totalAmount), 0))}
                  </td>
                  <td colSpan={3} />
                </tr>
              </tfoot>
            </table>

            {/* 시뮬레이션 실행 버튼 */}
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button
                onClick={runSimulation}
                style={{ padding: '10px 24px', background: '#111827', color: '#fff', border: 'none', borderRadius: 8, fontSize: 14, fontWeight: 600, cursor: 'pointer' }}
              >시뮬레이션 실행</button>
            </div>
          </>
        )}

        {/* 항목 없을 때 안내 */}
        {simItems.length === 0 && (
          <p style={{ textAlign: 'center', color: '#9ca3af', fontSize: 13, padding: '16px 0', margin: 0 }}>
            위 폼에서 종목을 추가하면 시뮬레이션을 실행할 수 있습니다.
          </p>
        )}
      </div>

      {/* ══════════════════════════════════════════════════════════════════════
          섹션 7: 시뮬레이션 결과 (실행 전 숨김)
         ══════════════════════════════════════════════════════════════════════ */}
      {simResult && (
        <div style={S.card}>
          <h2 style={{ fontSize: 16, fontWeight: 700, color: '#111827', margin: '0 0 16px 0' }}>시뮬레이션 결과</h2>

          {/* 도넛 차트 2개 나란히 */}
          <div style={{ display: 'flex', gap: 20, marginBottom: 24 }}>
            {/* 현재 포트폴리오 */}
            <div style={{ flex: 1, border: '1.5px dashed #d1d5db', borderRadius: 12, padding: 16 }}>
              <p style={{ fontSize: 13, fontWeight: 600, color: '#6b7280', margin: '0 0 10px 0', textAlign: 'center' }}>현재 포트폴리오 모습 (현재)</p>
              <div style={{ height: 180 }}>
                <Doughnut
                  key="sim-before"
                  data={buildChartData(currentNonZero, s => s.current)}
                  options={makeChartOpts(ctx => ` ${ctx.label}: ${currentNonZero[ctx.dataIndex]?.current ?? 0}%`)}
                />
              </div>
              <div style={{ marginTop: 10, display: 'flex', flexDirection: 'column', gap: 3 }}>
                {currentNonZero.map((s, i) => (
                  <div key={s.sector} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <div style={{ width: 8, height: 8, borderRadius: '50%', background: COLORS[i % COLORS.length], flexShrink: 0 }} />
                    <span style={{ fontSize: 11, color: '#6b7280' }}>{s.sector} {s.current}%</span>
                  </div>
                ))}
              </div>
            </div>

            {/* 가상 매수 후 포트폴리오 */}
            <div style={{ flex: 1, border: '1.5px dashed #10b981', borderRadius: 12, padding: 16 }}>
              <p style={{ fontSize: 13, fontWeight: 600, color: '#10b981', margin: '0 0 10px 0', textAlign: 'center' }}>가상 매수 후 포트폴리오 모습 (시뮬레이션 후)</p>
              <div style={{ height: 180 }}>
                <Doughnut
                  key="sim-after"
                  data={buildChartData(simResult.afterWeights, s => s.after)}
                  options={makeChartOpts(ctx => ` ${ctx.label}: ${simResult.afterWeights[ctx.dataIndex]?.after ?? 0}%`)}
                />
              </div>
              <div style={{ marginTop: 10, display: 'flex', flexDirection: 'column', gap: 3 }}>
                {simResult.afterWeights.map((s, i) => (
                  <div key={s.sector} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <div style={{ width: 8, height: 8, borderRadius: '50%', background: COLORS[i % COLORS.length], flexShrink: 0 }} />
                    <span style={{ fontSize: 11, color: '#6b7280' }}>{s.sector} {s.after}%</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* 비교 테이블 */}
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid #f3f4f6' }}>
                {['항목', '현재', '가상 매수 후', '변화'].map((h, i) => (
                  <th key={i} style={S.th}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {[
                {
                  label: '총 투자금액',
                  before: formatKRW(simResult.cmp.totalAsset.before),
                  after:  formatKRW(simResult.cmp.totalAsset.after),
                  diff:   simResult.cmp.totalAsset.after - simResult.cmp.totalAsset.before,
                  fmt: v => `+${formatKRW(v)}`,
                },
                {
                  label: '연간 예상 배당',
                  before: formatKRW(simResult.cmp.annualDividend.before),
                  after:  formatKRW(simResult.cmp.annualDividend.after),
                  diff:   simResult.cmp.annualDividend.after - simResult.cmp.annualDividend.before,
                  fmt: v => `+${formatKRW(v)}`,
                },
                {
                  label: '배당수익률',
                  before: `${simResult.cmp.dividendRate.before}%`,
                  after:  `${simResult.cmp.dividendRate.after}%`,
                  diff:   simResult.cmp.dividendRate.after - simResult.cmp.dividendRate.before,
                  fmt: v => signPct(v, 2),
                },
                {
                  label: `${simResult.cmp.sectorWeight.label} 비중`,
                  before: `${simResult.cmp.sectorWeight.before}%`,
                  after:  `${simResult.cmp.sectorWeight.after}%`,
                  diff:   simResult.cmp.sectorWeight.after - simResult.cmp.sectorWeight.before,
                  fmt: v => signPct(v),
                },
                {
                  label: '목표 달성률',
                  before: `${simResult.cmp.targetRate.before}%`,
                  after:  `${simResult.cmp.targetRate.after}%`,
                  diff:   simResult.cmp.targetRate.after - simResult.cmp.targetRate.before,
                  fmt: v => signPct(v, 0),
                },
              ].map((row, i) => (
                <tr key={i} style={{ borderBottom: '1px solid #f9fafb' }}>
                  <td style={{ ...S.td, fontWeight: 500, color: '#111827' }}>{row.label}</td>
                  <td style={S.td}>{row.before}</td>
                  <td style={S.td}>{row.after}</td>
                  <td style={{ ...S.td, fontWeight: 600, color: row.diff >= 0 ? '#10b981' : '#ef4444' }}>
                    {row.fmt(row.diff)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

    </div>
  )
}
