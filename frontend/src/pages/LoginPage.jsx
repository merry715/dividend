import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login, getMe } from '../api/auth'
import logo from '../assets/logo.png'
import './LoginPage.css'

const NAV_ITEMS = [
  { label: '대시보드', icon: <svg viewBox="0 0 18 18" fill="none"><rect x="1" y="1" width="6.5" height="6.5" rx="1.5" fill="currentColor"/><rect x="1" y="10.5" width="6.5" height="6.5" rx="1.5" fill="currentColor" opacity="0.6"/><rect x="10.5" y="1" width="6.5" height="6.5" rx="1.5" fill="currentColor" opacity="0.6"/><rect x="10.5" y="10.5" width="6.5" height="6.5" rx="1.5" fill="currentColor"/></svg> },
  { label: '종목관리', icon: <svg viewBox="0 0 18 18" fill="none"><polyline points="1,13 5,8 8,11 12,5 17,7" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/><polyline points="13,5 17,5 17,9" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/></svg> },
  { label: '거래관리', icon: <svg viewBox="0 0 18 18" fill="none"><path d="M2 5.5h11M10 2.5l3 3-3 3" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/><path d="M16 12.5H5M8 9.5l-3 3 3 3" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/></svg> },
  { label: '배당관리', icon: <svg viewBox="0 0 18 18" fill="none"><rect x="1" y="3" width="16" height="13" rx="2" stroke="currentColor" strokeWidth="1.8"/><path d="M12 1v4M6 1v4M1 8h16" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/></svg> },
  { label: '분석',     icon: <svg viewBox="0 0 18 18" fill="none"><circle cx="9" cy="9" r="8" stroke="currentColor" strokeWidth="1.8"/><path d="M9 1a8 8 0 0 1 8 8H9z" fill="currentColor" opacity="0.5"/><path d="M9 9L3.5 14.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/></svg> },
  { label: '리밸런싱', icon: <svg viewBox="0 0 18 18" fill="none"><line x1="2" y1="5" x2="16" y2="5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/><line x1="2" y1="9" x2="16" y2="9" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/><line x1="2" y1="13" x2="16" y2="13" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/><circle cx="6" cy="5" r="2" fill="currentColor"/><circle cx="12" cy="9" r="2" fill="currentColor"/><circle cx="7" cy="13" r="2" fill="currentColor"/></svg> },
]

const SLIDES = [
  { title: '대시보드',   desc: '포트폴리오 전체 현황, 총 투자금, 월별 예상 배당을 한눈에 확인합니다.' },
  { title: '종목 관리',  desc: '보유 종목·섹터를 등록하고 종목 코드를 자동으로 연결합니다.' },
  { title: '거래 관리',  desc: '매수·매도 내역을 입력해 평균 단가와 보유 수량을 관리합니다.' },
  { title: '배당 관리',  desc: '월별 예상·확정 배당금과 종목별 배당 정보를 확인합니다.' },
  { title: '분석',       desc: '연간 목표 달성률, 투자 비중, 연도별 배당금 변화를 시각화합니다.' },
  { title: '리밸런싱',   desc: '섹터 비중 분석, 관심 종목 분할 매수 제안, 가상 시뮬레이션을 제공합니다.' },
]

export default function LoginPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const [loading, setLoading] = useState(false)
  const [slideIndex, setSlideIndex] = useState(0)

  function validate() {
    const errs = {}
    if (!email.trim()) errs.email = '이메일을 입력해 주세요.'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errs.email = '올바른 이메일 형식을 입력해 주세요.'
    if (!password) errs.password = '비밀번호를 입력해 주세요.'
    return errs
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length) { setErrors(errs); return }
    setErrors({})
    setApiError('')
    setLoading(true)
    try {
      const { data } = await login(email, password)
      localStorage.setItem('accessToken', data.data.accessToken)
      localStorage.setItem('refreshToken', data.data.refreshToken)
      const meRes = await getMe()
      const { role, name } = meRes.data.data
      if (name) localStorage.setItem('userName', name)
      if (role) localStorage.setItem('userRole', role)
      navigate(role === 'ROLE_ADMIN' ? '/admin' : '/dashboard')
    } catch (err) {
      setApiError(err.response?.data?.message || '로그인에 실패했습니다. 다시 시도해 주세요.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="lp-shell">

      {/* ── 왼쪽 사이드바 미리보기 ── */}
      <aside className="lp-sidebar">
        <div className="lp-sidebar__brand">
          <img src={logo} alt="leafpay" className="lp-sidebar__logo" />
          <span className="lp-sidebar__brand-name">leafpay</span>
        </div>
        <nav className="lp-sidebar__nav">
          <span className="lp-sidebar__nav-label">메뉴</span>
          {NAV_ITEMS.map((item) => (
            <div key={item.label} className="lp-sidebar__item">
              <span className="lp-sidebar__item-icon">{item.icon}</span>
              <span className="lp-sidebar__item-label">{item.label}</span>
            </div>
          ))}
        </nav>
        <div className="lp-sidebar__footer">
          <div className="lp-sidebar__tip-btn">
            <span>💡</span>
            <span>배당 용어 TIP</span>
          </div>
        </div>
      </aside>

      {/* ── 오른쪽 폼 영역 ── */}
      <div className="lp-form-area">
        <div className="lp-tabs">
          <span className="lp-tab active">로그인</span>
          <Link to="/signup" className="lp-tab">회원가입</Link>
        </div>

        <div className="lp-form-card">
          <h2 className="lp-form-title">로그인</h2>
          <p className="lp-form-subtitle">배당 포트폴리오를 관리하세요</p>

          {apiError && <div className="lp-alert">{apiError}</div>}

          <form onSubmit={handleSubmit} noValidate>
            <div className="lp-field">
              <label htmlFor="email" className="lp-label">이메일</label>
              <input
                id="email"
                type="email"
                className={`lp-input${errors.email ? ' error' : ''}`}
                placeholder="example@email.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
                autoComplete="email"
              />
              {errors.email && <span className="lp-field-error">{errors.email}</span>}
            </div>

            <div className="lp-field">
              <label htmlFor="password" className="lp-label">비밀번호</label>
              <input
                id="password"
                type="password"
                className={`lp-input${errors.password ? ' error' : ''}`}
                placeholder="비밀번호 입력"
                value={password}
                onChange={e => setPassword(e.target.value)}
                autoComplete="current-password"
              />
              {errors.password && <span className="lp-field-error">{errors.password}</span>}
            </div>

            <button type="submit" className="lp-submit" disabled={loading}>
              {loading ? '로그인 중...' : '로그인'}
            </button>
          </form>

          <p className="lp-form-link">
            계정이 없으신가요? <Link to="/signup">회원가입</Link>
          </p>
        </div>

        {/* ── 서비스 가이드 ── */}
        <details className="lp-guide">
          <summary className="lp-guide__summary">
            <span>서비스 가이드</span>
            <span className="lp-guide__arrow">▾</span>
          </summary>
          <div className="lp-guide__body">
            <div className="lp-slide-nav">
              <button
                className="lp-slide-btn"
                type="button"
                onClick={() => setSlideIndex(i => (i - 1 + SLIDES.length) % SLIDES.length)}
              >‹</button>

              <div className="lp-slide-content">
                <div className="lp-slide-header">
                  <span className="lp-slide-title">{SLIDES[slideIndex].title}</span>
                  <span className="lp-slide-count">{slideIndex + 1} / {SLIDES.length}</span>
                </div>
                <div className="lp-slide-preview">화면 미리보기</div>
                <p className="lp-slide-desc">{SLIDES[slideIndex].desc}</p>
              </div>

              <button
                className="lp-slide-btn"
                type="button"
                onClick={() => setSlideIndex(i => (i + 1) % SLIDES.length)}
              >›</button>
            </div>

            <div className="lp-slide-dots">
              {SLIDES.map((_, i) => (
                <button
                  key={i}
                  type="button"
                  className={`lp-slide-dot${i === slideIndex ? ' active' : ''}`}
                  onClick={() => setSlideIndex(i)}
                />
              ))}
            </div>

            <div className="lp-slide-tabs">
              {SLIDES.map((s, i) => (
                <button
                  key={i}
                  type="button"
                  className={`lp-slide-tab${i === slideIndex ? ' active' : ''}`}
                  onClick={() => setSlideIndex(i)}
                >
                  {s.title}
                </button>
              ))}
            </div>
          </div>
        </details>

      </div>

    </div>
  )
}
