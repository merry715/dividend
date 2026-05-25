import { useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom'
import Sidebar from './components/Layout/Sidebar'
import Topbar from './components/Layout/Topbar'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import AdminPage from './pages/AdminPage'
import TradePage from './pages/TradePage'
import StockPage from './pages/StockPage'
import DashboardPage from './pages/DashboardPage'

function PrivateRoute({ children }) {
  const token = localStorage.getItem('accessToken')
  return token ? children : <Navigate to="/" replace />
}

function MainLayout({ collapsed, onToggle }) {
  return (
    <div style={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
      <Sidebar isCollapsed={collapsed} onToggle={onToggle} />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0, overflow: 'hidden' }}>
        <Topbar />
        <main style={{
          flex: 1,
          minWidth: 0,
          minHeight: 0,
          padding: '20px 24px',
          background: '#ffffff',
          overflow: 'hidden',
          boxSizing: 'border-box',
        }}>
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default function App() {
  const [collapsed, setCollapsed] = useState(false)

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route element={<PrivateRoute><MainLayout collapsed={collapsed} onToggle={() => setCollapsed(v => !v)} /></PrivateRoute>}>
          <Route path="/dashboard"    element={<DashboardPage />} />
          <Route path="/stocks"       element={<StockPage />} />
          <Route path="/transactions" element={<TradePage />} />
          <Route path="/dividends"    element={<div />} />
          <Route path="/analysis"     element={<div />} />
          <Route path="/rebalancing"  element={<div />} />
          <Route path="/admin"        element={<AdminPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
