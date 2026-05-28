import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { doLogout } from '../../utils/logout'
import './Topbar.css'

export default function Topbar({ onHamburger }) {
  const navigate = useNavigate()
  const userName = localStorage.getItem('userName') || localStorage.getItem('_mockName') || '사용자'
  const userRole = localStorage.getItem('userRole') || localStorage.getItem('_mockRole') || 'ROLE_USER'
  const roleLabel = userRole === 'ROLE_ADMIN' ? '관리자' : '일반 사용자'

  const [showPopup, setShowPopup] = useState(false)
  const popupRef = useRef(null)

  useEffect(() => {
    function handleClickOutside(e) {
      if (popupRef.current && !popupRef.current.contains(e.target)) {
        setShowPopup(false)
      }
    }
    if (showPopup) document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [showPopup])

  return (
    <header className="topbar">
      <button className="topbar__hamburger" onClick={onHamburger} aria-label="메뉴 열기">
        <span />
        <span />
        <span />
      </button>

      <div className="topbar__right">
        <div className="topbar__user-wrap" ref={popupRef}>
          <button className="topbar__user" onClick={() => setShowPopup(v => !v)}>
            <div className="topbar__avatar">{userName.slice(0, 2)}</div>
            <span>{userName}</span>
          </button>

          {showPopup && (
            <div className="topbar__popup">
              <div className="topbar__popup-avatar">{userName.slice(0, 2)}</div>
              <div className="topbar__popup-name">{userName}</div>
              <div className="topbar__popup-role">{roleLabel}</div>
            </div>
          )}
        </div>

        <button className="topbar__logout" onClick={() => doLogout(navigate)}>
          로그아웃
        </button>
      </div>
    </header>
  )
}
