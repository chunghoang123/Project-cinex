import { ConsoleLayout, type ConsoleNavItem } from './ConsoleLayout'

const items: ConsoleNavItem[] = [
  { to: '/admin', label: 'Tổng quan', icon: 'dashboard', end: true },
  { to: '/admin/movies', label: 'Quản lý phim', icon: 'film' },
  { to: '/admin/cinemas', label: 'Rạp & phòng chiếu', icon: 'building' },
  { to: '/admin/showtimes', label: 'Suất chiếu', icon: 'calendar' },
  { to: '/admin/bookings', label: 'Đơn hàng', icon: 'receipt' },
  { to: '/admin/users', label: 'Người dùng', icon: 'users' },
  { to: '/admin/reports', label: 'Báo cáo', icon: 'chart' },
]

export function AdminLayout() {
  return <ConsoleLayout variant="admin" roleLabel="Quản trị hệ thống" items={items} />
}