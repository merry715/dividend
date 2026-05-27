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
} from '../api/dividend'
import { getStocks } from '../api/stocks'

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip, Legend)

const fmt = (n) => Number(n).toLocaleString('ko-KR')
const MONTH_LABELS = ['1월','2월','3월','4월','5월','6월','7월','8월','9월','10월','11월','12월']
const today = new Date().toISOString().split('T')[0]
const CURRENT_YEAR = new Date().getFullYear()

export default function DividendPage() {
  const [annualData, setAnnualData]       = useState(null)
  const [cumulativeData, setCumulativeData] = useState(null)
  const [monthlyData, setMonthlyData]     = useState([])
  const [yearlyData, setYearlyData]       = useState([])
  const [dividendList, setDividendList]   = useState([])
  const [stocks, setStocks]               = useState([])
  const [loading, setLoading]             = useState(true)
  const [converting, setConverting]       = useState(false)
  const [convertForm, setConvertForm]     = useState({ dividendId: '', payDate: today, perShare: '' })

  const loadAll = useCallback(async (autoGenerate = true) => {
    try {
      const [annual, cumulative, monthly, yearly, dividends, stocksRes] = await Promise.all([
        getAnnual(CURRENT_YEAR),
        getCumulative(),
        getMonthly(CURRENT_YEAR),
        getYearly(),
        getDividends(),
        getStocks(),
      ])

      setAnnualData(annual.data.data)
      setCumulativeData(cumulative.data.data)
      setMonthlyData(monthly.data.data ?? [])
      setYearlyData(yearly.data.data ?? [])
      const dividendData = dividends.data.data ?? []
      const stockList    = stocksRes.data.data ?? []
      setDividendList(dividendData)
      setStocks(stockList)

      // 올해 배당 레코드 없으면 자동 생성
      const thisYearDividends = dividendData.filter(d => d.year === CURRENT_YEAR)
      if (thisYearDividends.length === 0 && autoGenerate && stockList.length > 0) {
        await Promise.all(stockList.map(s => generateDividends(s.id, CURRENT_YEAR)))
        return loadAll(false)
      }
    } catch (e) {
      console.error('배당 데이터 로딩 실패', e)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadAll() }, [loadAll])

  // 12개월 배열로 정규화 (API는 데이터 있는 달만 반환)
  const normalizedMonthly = useMemo(() =>
    Array.from({ length: 12 }, (_, i) => {
      const month = i + 1
      const found = monthlyData.find(m => m.month === month)
      const monthDivs = dividendList.filter(d => d.year === CURRENT_YEAR && d.month === month)
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

  const byStockData = useMemo(() => {
    const thisYear = dividendList.filter(d => d.year === CURRENT_YEAR)
    if (!thisYear.length || !stocks.length) return []

    const grouped = {}
    for (const d of thisYear) {
      if (!grouped[d.stockId]) grouped[d.stockId] = []
      grouped[d.stockId].push(d)
    }

    return Object.entries(grouped).map(([stockId, divs]) => {
      const stock = stocks.find(s => s.id === Number(stockId))
      const paymentMonths = [...new Set(divs.map(d => d.month))]
        .sort((a, b) => a - b)
        .map(month => ({
          month,
          confirmed: divs.find(d => d.month === month)?.status === 'CONFIRMED',
        }))
      const confirmedCount = divs.filter(d => d.status === 'CONFIRMED').length
      const status = confirmedCount === divs.length ? 'CONFIRMED'
                   : confirmedCount > 0             ? 'PARTIAL'
                   : 'EXPECTED'
      const expectedDividend = divs.reduce((sum, d) =>
        sum + Number(d.status === 'CONFIRMED' ? (d.confirmedAmount ?? 0) : (d.expectedAmount ?? 0)), 0)
      const exDividendDate = divs.find(d => d.exDividendDate)?.exDividendDate ?? null

      return {
        stockId: Number(stockId),
        stockName: stock?.stockName ?? String(stockId),
        lastYearDividendPerShare: stock?.expectedDividendPerShare ?? 0,
        paymentMonths,
        exDividendDate,
        status,
        expectedDividend,
      }
    })
  }, [dividendList, stocks])

  const expectedItems = useMemo(
    () => dividendList.filter(d => d.status === 'EXPECTED'),
    [dividendList]
  )

  const handleConvert = async () => {
    if (!convertForm.dividendId || !convertForm.perShare) return
    setConverting(true)
    try {
      const selectedDividend = expectedItems.find(d => d.id === Number(convertForm.dividendId))
      const stock = stocks.find(s => s.id === selectedDividend?.stockId)
      if (!stock) {
        console.error('종목 정보를 찾을 수 없습니다:', selectedDividend?.stockId)
        return
      }
      const total = Number(convertForm.perShare) * stock.quantity
      await confirmDividend(Number(convertForm.dividendId), {
        confirmedAmount: total,
        paymentDate: convertForm.payDate,
      })
      setConvertForm({ dividendId: '', payDate: today, perShare: '' })
      await loadAll()
    } catch (e) {
      console.error('확정 전환 실패', e)
    } finally {
      setConverting(false)
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

      {/* ── 확정 배당 전환 폼 ── */}
      <div className="dp-card dp-convert-card">
        <p className="dp-card-title">확정 배당 전환</p>
        <div className="dp-convert-row">
          <select
            className="dp-fi dp-fi-select"
            value={convertForm.dividendId}
            onChange={e => setConvertForm(f => ({ ...f, dividendId: e.target.value }))}
          >
            <option value="">종목 선택</option>
            {expectedItems.map(d => (
              <option key={d.id} value={d.id}>
                {stocks.find(s => s.id === d.stockId)?.stockName ?? d.stockId} ({d.month}월)
              </option>
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
            placeholder="주당 배당금 (원)"
            value={convertForm.perShare}
            onChange={e => setConvertForm(f => ({ ...f, perShare: e.target.value }))}
          />
          <button
            className="dp-convert-cancel"
            onClick={() => setConvertForm({ dividendId: '', payDate: today, perShare: '' })}
          >취소</button>
          <button
            className="dp-convert-save"
            onClick={handleConvert}
            disabled={!convertForm.dividendId || !convertForm.perShare || converting}
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
                <th className="center">지급월</th>
                <th>배당락일</th>
                <th className="center">상태</th>
                <th className="right">예상 배당금</th>
              </tr>
            </thead>
            <tbody>
              {byStockData.length === 0 ? (
                <tr>
                  <td colSpan={6} className="dp-empty">
                    배당 정보가 없습니다. 예상 배당을 생성해 주세요.
                  </td>
                </tr>
              ) : (
                byStockData.map(d => (
                  <tr key={d.stockId}>
                    <td className="dp-stock-name">{d.stockName}</td>
                    <td className="right">{fmt(d.lastYearDividendPerShare)}원</td>
                    <td className="center">
                      <div className="dp-month-tags">
                        {d.paymentMonths?.map(m => (
                          <span key={m.month} className={`dp-month-tag ${m.confirmed ? 'confirmed' : 'expected'}`}>
                            {m.month}월
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="dp-date">{d.exDividendDate ?? '—'}</td>
                    <td className="center">
                      <span className={`dp-status-badge ${d.status === 'CONFIRMED' ? 'confirmed' : d.status === 'PARTIAL' ? 'partial' : 'expected'}`}>
                        {d.status === 'CONFIRMED' ? '확정' : d.status === 'PARTIAL' ? '부분확정' : '예상'}
                      </span>
                    </td>
                    <td className="right dp-expected-total">{fmt(d.expectedDividend)}원</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

    </div>
  )
}
