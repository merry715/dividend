import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { signup } from '../api/auth'
import logo from '../assets/logo.png'
import './LoginPage.css'

const EyeIcon = () => (
  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor"
    strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
    <circle cx="12" cy="12" r="3"/>
  </svg>
)
const EyeOffIcon = () => (
  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor"
    strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M17.94 17.94A10.94 10.94 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
    <path d="M9.9 4.24A10.94 10.94 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
    <line x1="2" y1="2" x2="22" y2="22"/>
  </svg>
)

export default function SignupPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '', confirmPassword: '' })
  const [showPw, setShowPw] = useState(false)
  const [showConfirmPw, setShowConfirmPw] = useState(false)
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [success, setSuccess] = useState(false)
  const [toast, setToast] = useState({ msg: '', show: false })

  const set = (field) => (e) => setForm(prev => ({ ...prev, [field]: e.target.value }))

  function showToast(msg) {
    setToast({ msg, show: true })
    setTimeout(() => setToast(t => ({ ...t, show: false })), 2600)
  }

  function validate() {
    const errs = {}
    if (!form.name.trim()) errs.name = '이름을 입력해 주세요'
    if (!form.email.trim()) errs.email = '이메일을 입력해 주세요'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errs.email = '올바른 이메일 형식이 아닙니다'
    if (!form.password) errs.password = '비밀번호를 입력해 주세요'
    else if (form.password.length < 8) errs.password = '8자 이상 입력해 주세요'
    if (!form.confirmPassword) errs.confirmPassword = '비밀번호 확인을 입력해 주세요'
    else if (form.password !== form.confirmPassword) errs.confirmPassword = '비밀번호가 일치하지 않습니다'
    return errs
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length) { setErrors(errs); return }
    setErrors({})
    setLoading(true)
    try {
      await signup(form.name, form.email, form.password)
      setSuccess(true)
    } catch (err) {
      showToast(err.response?.data?.message || '회원가입에 실패했습니다. 다시 시도해 주세요.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="lp-page">

      {/* ── 배경 장식 ── */}
      <div className="lp-bg-deco" aria-hidden="true">
        <svg className="lp-leaf-1" viewBox="0 0 200 200" fill="none">
          <path d="M40 160 C40 80, 100 30, 170 30 C170 100, 110 170, 40 160 Z"
            stroke="currentColor" strokeWidth="1.2" fill="currentColor" fillOpacity="0.18"/>
          <path d="M40 160 C80 130, 130 80, 170 30" stroke="currentColor" strokeWidth="1.2"/>
          <path d="M60 140 L100 110 M80 150 L120 100 M100 158 L140 80"
            stroke="currentColor" strokeWidth="0.8" opacity="0.6"/>
        </svg>
        <svg className="lp-leaf-2" viewBox="0 0 200 200" fill="none">
          <circle cx="100" cy="100" r="90" stroke="currentColor" strokeWidth="1"
            fill="currentColor" fillOpacity="0.1"/>
          <circle cx="100" cy="100" r="60" stroke="currentColor" strokeWidth="0.8" opacity="0.5"/>
        </svg>
        <div className="lp-grain" />
      </div>

      {/* ── 카드 ── */}
      <div className="lp-card">

        {/* 로고 */}
        <div className="lp-logo">
          <img src={logo} alt="leafpay" className="lp-logo-img" />
          <div className="lp-logo-text">leafpay</div>
        </div>

        {/* 탭 — 회원가입 활성 */}
        <div className="lp-mode-tabs">
          <div className="lp-mode-indicator right" />
          <Link to="/" className="lp-mode-tab">로그인</Link>
          <button className="lp-mode-tab active" type="button">회원가입</button>
        </div>

        {/* 폼 */}
        <form onSubmit={handleSubmit} noValidate>
          <div className="lp-field">
            <label htmlFor="name" className="lp-label">이름</label>
            <div className="lp-input-wrap">
              <input
                id="name"
                type="text"
                className={`lp-input${errors.name ? ' invalid' : ''}`}
                placeholder="이름을 입력하세요"
                value={form.name}
                onChange={set('name')}
                autoComplete="name"
              />
            </div>
            <div className="lp-field-error">{errors.name || ''}</div>
          </div>

          <div className="lp-field">
            <label htmlFor="email" className="lp-label">이메일</label>
            <div className="lp-input-wrap">
              <input
                id="email"
                type="email"
                className={`lp-input${errors.email ? ' invalid' : ''}`}
                placeholder="이메일을 입력하세요"
                value={form.email}
                onChange={set('email')}
                autoComplete="email"
              />
            </div>
            <div className="lp-field-error">{errors.email || ''}</div>
          </div>

          <div className="lp-field">
            <label htmlFor="password" className="lp-label">비밀번호</label>
            <div className="lp-input-wrap">
              <input
                id="password"
                type={showPw ? 'text' : 'password'}
                className={`lp-input lp-input-pw${errors.password ? ' invalid' : ''}`}
                placeholder="8자 이상 입력하세요"
                value={form.password}
                onChange={set('password')}
                autoComplete="new-password"
              />
              <button type="button" className="lp-pw-toggle" onClick={() => setShowPw(v => !v)}
                aria-label="비밀번호 표시">
                {showPw ? <EyeOffIcon /> : <EyeIcon />}
              </button>
            </div>
            <div className="lp-field-error">{errors.password || ''}</div>
          </div>

          <div className="lp-field">
            <label htmlFor="confirmPassword" className="lp-label">비밀번호 확인</label>
            <div className="lp-input-wrap">
              <input
                id="confirmPassword"
                type={showConfirmPw ? 'text' : 'password'}
                className={`lp-input lp-input-pw${errors.confirmPassword ? ' invalid' : ''}`}
                placeholder="비밀번호를 다시 입력하세요"
                value={form.confirmPassword}
                onChange={set('confirmPassword')}
                autoComplete="new-password"
              />
              <button type="button" className="lp-pw-toggle" onClick={() => setShowConfirmPw(v => !v)}
                aria-label="비밀번호 확인 표시">
                {showConfirmPw ? <EyeOffIcon /> : <EyeIcon />}
              </button>
            </div>
            <div className="lp-field-error">{errors.confirmPassword || ''}</div>
          </div>

          <button type="submit" className="lp-submit" disabled={loading}>
            {loading ? <span className="lp-spinner" /> : '회원가입'}
          </button>
        </form>

        <div className="lp-footer">
          이미 계정이 있으신가요? <Link to="/" className="lp-link">로그인</Link>
        </div>

        {/* 성공 오버레이 */}
        {success && (
          <div className="lp-success-overlay">
            <div className="lp-success-ring">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3"
                strokeLinecap="round" strokeLinejoin="round">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </div>
            <div className="lp-success-title">가입 완료!</div>
            <div className="lp-success-sub">회원가입이 완료되었습니다.</div>
            <button className="lp-submit lp-submit-inline" onClick={() => navigate('/')}>
              로그인하러 가기
            </button>
          </div>
        )}
      </div>

      {/* 토스트 */}
      <div className={`lp-toast${toast.show ? ' show' : ''}`}>{toast.msg}</div>

    </div>
  )
}
