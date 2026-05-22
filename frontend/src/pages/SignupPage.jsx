import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { signup } from '../api/auth'
import logo from '../assets/logo.png'
import './LoginPage.css'
import './SignupPage.css'

const NAV_ITEMS = [
  { label: '대시보드', icon: <svg viewBox="0 0 18 18" fill="none"><rect x="1" y="1" width="6.5" height="6.5" rx="1.5" fill="currentColor"/><rect x="1" y="10.5" width="6.5" height="6.5" rx="1.5" fill="currentColor" opacity="0.6"/><rect x="10.5" y="1" width="6.5" height="6.5" rx="1.5" fill="currentColor" opacity="0.6"/><rect x="10.5" y="10.5" width="6.5" height="6.5" rx="1.5" fill="currentColor"/></svg> },
  { label: '종목관리', icon: <svg viewBox="0 0 18 18" fill="none"><polyline points="1,13 5,8 8,11 12,5 17,7" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/><polyline points="13,5 17,5 17,9" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/></svg> },
  { label: '거래관리', icon: <svg viewBox="0 0 18 18" fill="none"><path d="M2 5.5h11M10 2.5l3 3-3 3" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/><path d="M16 12.5H5M8 9.5l-3 3 3 3" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/></svg> },
  { label: '배당관리', icon: <svg viewBox="0 0 18 18" fill="none"><rect x="1" y="3" width="16" height="13" rx="2" stroke="currentColor" strokeWidth="1.8"/><path d="M12 1v4M6 1v4M1 8h16" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/></svg> },
  { label: '분석',     icon: <svg viewBox="0 0 18 18" fill="none"><circle cx="9" cy="9" r="8" stroke="currentColor" strokeWidth="1.8"/><path d="M9 1a8 8 0 0 1 8 8H9z" fill="currentColor" opacity="0.5"/><path d="M9 9L3.5 14.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/></svg> },
  { label: '리밸런싱', icon: <svg viewBox="0 0 18 18" fill="none"><line x1="2" y1="5" x2="16" y2="5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/><line x1="2" y1="9" x2="16" y2="9" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/><line x1="2" y1="13" x2="16" y2="13" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/><circle cx="6" cy="5" r="2" fill="currentColor"/><circle cx="12" cy="9" r="2" fill="currentColor"/><circle cx="7" cy="13" r="2" fill="currentColor"/></svg> },
]

export default function SignupPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '', confirmPassword: '' })
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const [loading, setLoading] = useState(false)
  const [showModal, setShowModal] = useState(false)

  const set = (field) => (e) => setForm(prev => ({ ...prev, [field]: e.target.value }))

  function validate() {
    const errs = {}
    if (!form.name.trim()) errs.name = '이름을 입력해 주세요.'
    if (!form.email.trim()) errs.email = '이메일을 입력해 주세요.'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errs.email = '올바른 이메일 형식을 입력해 주세요.'
    if (!form.password) errs.password = '비밀번호를 입력해 주세요.'
    else if (form.password.length < 8) errs.password = '비밀번호는 8자 이상이어야 합니다.'
    if (!form.confirmPassword) errs.confirmPassword = '비밀번호 확인을 입력해 주세요.'
    else if (form.password !== form.confirmPassword) errs.confirmPassword = '비밀번호가 일치하지 않습니다.'
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
      await signup(form.name, form.email, form.password)
      setShowModal(true)
    } catch (err) {
      setApiError(err.response?.data?.message || '회원가입에 실패했습니다. 다시 시도해 주세요.')
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
          <Link to="/" className="lp-tab">로그인</Link>
          <span className="lp-tab active">회원가입</span>
        </div>

        <div className="lp-form-card">
          <h2 className="lp-form-title">회원가입</h2>
          <p className="lp-form-subtitle">배당 포트폴리오 관리를 시작하세요</p>

          {apiError && <div className="lp-alert">{apiError}</div>}

          <form onSubmit={handleSubmit} noValidate>
            <div className="lp-field">
              <label htmlFor="name" className="lp-label">이름</label>
              <input
                id="name"
                type="text"
                className={`lp-input${errors.name ? ' error' : ''}`}
                placeholder="홍길동"
                value={form.name}
                onChange={set('name')}
                autoComplete="name"
              />
              {errors.name && <span className="lp-field-error">{errors.name}</span>}
            </div>

            <div className="lp-field">
              <label htmlFor="email" className="lp-label">이메일</label>
              <input
                id="email"
                type="email"
                className={`lp-input${errors.email ? ' error' : ''}`}
                placeholder="example@email.com"
                value={form.email}
                onChange={set('email')}
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
                placeholder="8자 이상 입력"
                value={form.password}
                onChange={set('password')}
                autoComplete="new-password"
              />
              {errors.password && <span className="lp-field-error">{errors.password}</span>}
            </div>

            <div className="lp-field">
              <label htmlFor="confirmPassword" className="lp-label">비밀번호 확인</label>
              <input
                id="confirmPassword"
                type="password"
                className={`lp-input${errors.confirmPassword ? ' error' : ''}`}
                placeholder="비밀번호 재입력"
                value={form.confirmPassword}
                onChange={set('confirmPassword')}
                autoComplete="new-password"
              />
              {errors.confirmPassword && <span className="lp-field-error">{errors.confirmPassword}</span>}
            </div>

            <button type="submit" className="lp-submit" disabled={loading}>
              {loading ? '가입 중...' : '회원가입'}
            </button>
          </form>

          <p className="lp-form-link">
            이미 계정이 있으신가요? <Link to="/">로그인</Link>
          </p>
        </div>
      </div>

      {/* ── 완료 모달 ── */}
      {showModal && (
        <div className="sp-modal-overlay">
          <div className="sp-modal">
            <div className="sp-modal__icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="20 6 9 17 4 12" />
              </svg>
            </div>
            <p className="sp-modal__title">가입 완료!</p>
            <p className="sp-modal__desc">회원가입이 완료되었습니다.<br />로그인 후 서비스를 이용해 주세요.</p>
            <button className="lp-submit" style={{ marginTop: 8 }} onClick={() => navigate('/')}>
              로그인하러 가기
            </button>
          </div>
        </div>
      )}

    </div>
  )
}
