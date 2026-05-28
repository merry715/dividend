import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login, getMe } from '../api/auth'
import logo from '../assets/logo.png'
import './LoginPage.css'

export default function LoginPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPw, setShowPw] = useState(false)
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState({ msg: '', show: false })

  function showToast(msg) {
    setToast({ msg, show: true })
    setTimeout(() => setToast(t => ({ ...t, show: false })), 2600)
  }

  function validate() {
    const errs = {}
    if (!email.trim()) errs.email = '이메일을 입력해 주세요'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errs.email = '올바른 이메일 형식이 아닙니다'
    if (!password) errs.password = '비밀번호를 입력해 주세요'
    return errs
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length) { setErrors(errs); return }
    setErrors({})
    setLoading(true)
    try {
      const { data } = await login(email, password)
      if (!data.success) {
        showToast(data.message || '로그인에 실패했습니다. 다시 시도해 주세요.')
        return
      }
      localStorage.setItem('accessToken', data.data.accessToken)
      localStorage.setItem('refreshToken', data.data.refreshToken)
      const meRes = await getMe()
      const { role, name } = meRes.data.data
      if (name) localStorage.setItem('userName', name)
      if (role) localStorage.setItem('userRole', role)
      navigate(role === 'ROLE_ADMIN' ? '/admin' : '/dashboard')
    } catch (err) {
      showToast(err.response?.data?.message || '로그인에 실패했습니다. 다시 시도해 주세요.')
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

        {/* 탭 */}
        <div className="lp-mode-tabs">
          <div className="lp-mode-indicator" />
          <button className="lp-mode-tab active" type="button">로그인</button>
          <Link to="/signup" className="lp-mode-tab">회원가입</Link>
        </div>

        {/* 폼 */}
        <form onSubmit={handleSubmit} noValidate>
          <div className="lp-field">
            <label htmlFor="email" className="lp-label">이메일</label>
            <div className="lp-input-wrap">
              <input
                id="email"
                type="email"
                className={`lp-input${errors.email ? ' invalid' : ''}`}
                placeholder="이메일을 입력하세요"
                value={email}
                onChange={e => setEmail(e.target.value)}
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
                placeholder="비밀번호를 입력하세요"
                value={password}
                onChange={e => setPassword(e.target.value)}
                autoComplete="current-password"
              />
              <button type="button" className="lp-pw-toggle" onClick={() => setShowPw(v => !v)}
                aria-label="비밀번호 표시">
                {showPw ? (
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor"
                    strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M17.94 17.94A10.94 10.94 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                    <path d="M9.9 4.24A10.94 10.94 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                    <line x1="2" y1="2" x2="22" y2="22"/>
                  </svg>
                ) : (
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor"
                    strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                    <circle cx="12" cy="12" r="3"/>
                  </svg>
                )}
              </button>
            </div>
            <div className="lp-field-error">{errors.password || ''}</div>
          </div>

          <button type="submit" className="lp-submit" disabled={loading}>
            {loading ? <span className="lp-spinner" /> : '로그인'}
          </button>
        </form>

        <div className="lp-footer">
          계정이 없으신가요? <Link to="/signup" className="lp-link">회원가입</Link>
        </div>

      </div>

      {/* 토스트 */}
      <div className={`lp-toast${toast.show ? ' show' : ''}`}>{toast.msg}</div>

    </div>
  )
}
