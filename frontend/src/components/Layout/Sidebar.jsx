import { useState } from 'react'
import { NavLink } from 'react-router-dom'
import logo from '../../assets/logo.png'
import './Sidebar.css'

const DashboardIcon = () => (
  <svg viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect x="1" y="1" width="6.5" height="6.5" rx="1.5" fill="currentColor" />
    <rect x="1" y="10.5" width="6.5" height="6.5" rx="1.5" fill="currentColor" opacity="0.6" />
    <rect x="10.5" y="1" width="6.5" height="6.5" rx="1.5" fill="currentColor" opacity="0.6" />
    <rect x="10.5" y="10.5" width="6.5" height="6.5" rx="1.5" fill="currentColor" />
  </svg>
)

const StocksIcon = () => (
  <svg viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
    <polyline
      points="1,13 5,8 8,11 12,5 17,7"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
    <polyline
      points="13,5 17,5 17,9"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
)

const TradesIcon = () => (
  <svg viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path
      d="M2 5.5h11M10 2.5l3 3-3 3"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
    <path
      d="M16 12.5H5M8 9.5l-3 3 3 3"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
)

const DividendsIcon = () => (
  <svg viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect x="1" y="3" width="16" height="13" rx="2" stroke="currentColor" strokeWidth="1.8" />
    <path d="M12 1v4M6 1v4M1 8h16" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
  </svg>
)

const AnalysisIcon = () => (
  <svg viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
    <circle cx="9" cy="9" r="8" stroke="currentColor" strokeWidth="1.8" />
    <path d="M9 1a8 8 0 0 1 8 8H9z" fill="currentColor" opacity="0.5" />
    <path d="M9 9L3.5 14.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
  </svg>
)

const navItems = [
  { path: '/dashboard',    label: '대시보드', icon: DashboardIcon   },
  { path: '/stocks',       label: '종목관리', icon: StocksIcon      },
  { path: '/transactions', label: '거래관리', icon: TradesIcon      },
  { path: '/dividends',    label: '배당관리', icon: DividendsIcon   },
  { path: '/analysis',     label: '분석',     icon: AnalysisIcon    },
]

const TIPS = [
  { term: '배당수익률', desc: '주가 대비 연간 배당금의 비율 (배당금 ÷ 주가 × 100)' },
  { term: '배당락일',   desc: '이 날 이전에 주식 보유 시 배당금을 수령할 수 있는 기준일' },
  { term: '배당성장률', desc: '전년도 대비 배당금이 증가한 비율' },
  { term: 'DRIP',      desc: '배당금 재투자 제도 — 받은 배당금으로 해당 주식을 자동 매수' },
]

export default function Sidebar({ isCollapsed, onToggle, isMobileOpen, onMobileClose }) {
  const [showTips, setShowTips] = useState(false)

  return (
    <>
      {isMobileOpen && (
        <div className="sidebar__backdrop" onClick={onMobileClose} />
      )}
      <aside className={`sidebar${isCollapsed ? ' collapsed' : ''}${isMobileOpen ? ' mobile-open' : ''}`}>

        {/* 토글 버튼 */}
        <button className="sidebar__toggle" onClick={onToggle} title={isCollapsed ? '펼치기' : '접기'}>
          {isCollapsed ? '›' : '‹'}
        </button>

        {/* 브랜드 */}
        <div className="sidebar__brand">
          <img src={logo} alt="leafpay" className="sidebar__logo" />
          <span className="sidebar__brand-name">leafpay</span>
        </div>

        {/* 내비게이션 */}
        <nav className="sidebar__nav">
          <span className="sidebar__nav-label">메뉴</span>
          {navItems.map(({ path, label, icon: Icon }) => (
            <NavLink
              key={path}
              to={path}
              onClick={onMobileClose}
              className={({ isActive }) => `sidebar__item${isActive ? ' active' : ''}`}
              title={isCollapsed ? label : undefined}
            >
              <span className="sidebar__item-icon"><Icon /></span>
              <span className="sidebar__item-label">{label}</span>
            </NavLink>
          ))}
        </nav>

        {/* 하단 TIP 버튼 */}
        <div className="sidebar__footer">
          {showTips && !isCollapsed && (
            <div className="sidebar__tips-popover">
              <div className="sidebar__tips-header">배당 용어 TIP</div>
              {TIPS.map((t, i) => (
                <div key={i} className="sidebar__tip-item">
                  <span className="sidebar__tip-term">{t.term}</span>
                  <span className="sidebar__tip-desc">{t.desc}</span>
                </div>
              ))}
            </div>
          )}
          <button
            className={`sidebar__tip-btn${showTips ? ' open' : ''}`}
            onClick={() => !isCollapsed && setShowTips(v => !v)}
            title="배당 용어 TIP"
          >
            <span className="sidebar__tip-emoji">💡</span>
            <span className="sidebar__tip-label">배당 용어 TIP</span>
            <span className="sidebar__tip-arrow">{showTips ? '▲' : '▼'}</span>
          </button>
        </div>

      </aside>
    </>
  )
}
