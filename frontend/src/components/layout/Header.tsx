import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import type { FormEvent } from 'react'
import { authService } from '../../services/authService'
import { demoUser } from '../../data/mockData'
import { Avatar } from '../ui/Avatar'
import { Icon } from '../ui/Icon'
import { Logo } from '../ui/Logo'

const navItems = [
  { to: '/', label: 'Trang chủ', end: true },
  { to: '/movies', label: 'Phim' },
  { to: '/showtimes', label: 'Lịch chiếu' },
  { to: '/ai-chat', label: 'AI Chat' },
]

export function Header() {
  const navigate = useNavigate()
  const location = useLocation()
  const token = authService.getToken()
  const user = authService.getUser()
  const isConsoleUser = authService.isStaffOrAdmin()
  const consolePath = authService.hasRole('ROLE_ADMIN') ? '/admin' : '/staff/pos'
  const isAuthPage = location.pathname === '/login' || location.pathname === '/register'

  if (isAuthPage) return null

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const query = String(form.get('q') ?? '').trim()
    navigate(query ? `/search?q=${encodeURIComponent(query)}` : '/search')
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
            isConsoleUser ? (
              <Link to={consolePath} className="header-user">
                <div>
                  <div className="header-user__name">{user?.fullName || user?.username || 'Cinex Console'}</div>
                  <div className="header-user__role">Vào console</div>
                </div>
                <span className="console-avatar" style={{ width: 30, height: 30, fontSize: 10 }}>
                  {(user?.fullName || user?.username || 'CX').slice(0, 2).toUpperCase()}
                </span>
              </Link>
            ) : (
              <Link to="/profile" className="header-user">
                <div>
                  <div className="header-user__name">{user?.fullName || demoUser.name}</div>
                  <div className="header-user__role">Thành viên Cinex</div>
                </div>
                <Avatar src={demoUser.avatar} alt={user?.fullName || demoUser.name} size="sm" />
              </Link>
            )
          ) : (
            <Link to="/login" className="btn btn-primary btn-sm">
              Đăng nhập
            </Link>
          )}
        </div>
      </div>
    </header>
  )
}
