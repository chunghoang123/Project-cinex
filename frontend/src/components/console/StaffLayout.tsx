import { ConsoleLayout, type ConsoleNavItem } from './ConsoleLayout'

const items: ConsoleNavItem[] = [
  { to: '/staff/pos', label: 'Bán vé tại quầy', icon: 'ticket', end: true },
  { to: '/staff/bookings', label: 'Tra cứu đơn', icon: 'receipt' },
  { to: '/staff/validate', label: 'Soát vé', icon: 'scan' },
  { to: '/staff/shift', label: 'Doanh thu ca', icon: 'chart' },
]

export function StaffLayout() {
  return <ConsoleLayout variant="staff" roleLabel="Nhân viên bán vé" items={items} />
}
