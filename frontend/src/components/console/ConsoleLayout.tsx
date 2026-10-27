import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Icon, type IconName } from '../ui/Icon'
import { Logo } from '../ui/Logo'
import { authService } from '../../services/authService'

export interface ConsoleNavItem {
  to: string
  label: string
  icon: IconName
  end?: boolean
}

interface ConsoleLayoutProps {
  variant: 'admin' | 'staff'
  roleLabel: string
  items: ConsoleNavItem[]
}

export function ConsoleLayout({ variant, roleLabel, items }: ConsoleLayoutProps) {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const user = authService.getUser()

  useEffect(() => {
    setSidebarOpen(false)
  }, [location.pathname])

  const activeItem = items.find((item) =>
    item.end ? location.pathname === item.to : location.pathname.startsWith(item.to)
  )
  const initials = (user?.fullName || user?.username || 'CX')
    .split(' ')
    .filter(Boolean)
    .map((part) => part[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()

  const logout = () => {
    authService.logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className={`console-shell console-shell--${variant}`}>
      <aside className={`console-sidebar ${sidebarOpen ? 'is-open' : ''}`}>
        <div className="console-sidebar__brand">
          <Logo to={variant === 'admin' ? '/admin' : '/staff/pos'} />
          <span className="console-role-pill">{roleLabel}</span>
        </div>

        <nav className="console-nav" aria-label="Điều hướng console">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `console-nav-item ${isActive ? 'is-active' : ''}`}
            >
              <Icon name={item.icon} size={19} />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="console-sidebar__foot">
          <Link to="/" className="console-nav-item">
            <Icon name="globe" size={19} />
            <span>Về trang khách hàng</span>
          </Link>
          <button type="button" className="console-nav-item" onClick={logout}>
            <Icon name="logout" size={19} />
            <span>Đăng xuất</span>
          </button>
        </div>
      </aside>

      {sidebarOpen && (
        <button
          type="button"
          className="console-sidebar-overlay"
          aria-label="Đóng menu"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      <div className="console-main">
        <header className="console-topbar">
          <button
            type="button"
            className="icon-btn console-menu-btn"
            aria-label="Mở menu"
            onClick={() => setSidebarOpen(true)}
          >
            <Icon name="menu" size={20} />
          </button>

          <div className="console-topbar__title">
            <span>{roleLabel}</span>
            <strong>{activeItem?.label ?? 'Cinex Console'}</strong>
          </div>

          <div className="console-topbar__right">
            <Link to="/" className="console-store-link">
              <Icon name="globe" size={16} />
              <span>Cửa hàng</span>
            </Link>
            <div className="console-user">
              <span className="console-avatar">{initials || 'CX'}</span>
              <div>
                <strong>{user?.fullName || user?.username || 'Cinex'}</strong>
                <span>@{user?.username || 'guest'}</span>
              </div>
            </div>
          </div>
        </header>

        <main className="console-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
