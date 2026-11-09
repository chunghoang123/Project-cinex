import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import type { FormEvent } from 'react'
import { authService } from '../../services/authService'
import { Icon } from '../ui/Icon'
import { Logo } from '../ui/Logo'

const navItems = [
  { to: '/', label: 'Trang chủ', end: true },
  { to: '/movies', label: 'Phim' },
  { to: '/showtimes', label: 'Lịch chiếu' },
]

export function Header() {
  const navigate = useNavigate()
  const location = useLocation()
  const token = authService.getToken()
  const isAuthPage = location.pathname === '/login' || location.pathname === '/register'

  const [loggingOut, setLoggingOut] = useState(false)

  useEffect(() => {
    if (!loggingOut) return
    const safety = window.setTimeout(() => setLoggingOut(false), 5000)
    return () => {
      window.clearTimeout(safety)
      setLoggingOut(false)
    }
  }, [loggingOut])

  if (isAuthPage) return null

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const query = String(form.get('q') ?? '').trim()
    navigate(query ? `/search?q=${encodeURIComponent(query)}` : '/search')
  }

  const handleLogout = () => {
    setMenuOpen(false)
    setLoggingOut(true)
    window.setTimeout(() => {
      authService.logout()
      navigate('/login')
    }, 300)
  }

  return (
    <header className="site-header">
      <div className="container site-header__inner">
        <Logo />

        <nav className="desktop-nav" aria-label="Điều hướng chính">
          {navItems.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => (isActive ? 'active' : '')}>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <form className="header-search" onSubmit={handleSearch} role="search">
          <Icon name="search" size={17} />
          <input name="q" placeholder="Tìm phim, rạp, suất chiếu..." aria-label="Tìm kiếm" />
        </form>

        <div className="header-actions">
          <Link to="/history" className="header-icon mobile-only" aria-label="Vé của tôi">
            <Icon name="ticket" size={19} />
          </Link>
          <Link to="/history" className="header-icon hide-mobile" aria-label="Thông báo">
            <Icon name="bell" size={19} />
          </Link>

          {token ? (
            <button
              type="button"
              className="header-icon mobile-only"
              aria-label="Đăng xuất"
              title="Đăng xuất"
              onClick={handleLogout}
            >
              <Icon name="logout" size={19} />
            </button>
          ) : (
            <Link to="/login" className="btn btn-primary btn-sm">
              Đăng nhập
            </Link>
          )}
        </div>
      </div>
      {loggingOut && createPortal(
        <div className="auth-loading-overlay" role="status" aria-live="polite">
          <div className="spinner" />
          <p>Đang đăng xuất...</p>
        </div>,
        document.body
      )}
    </header>
  )
}
