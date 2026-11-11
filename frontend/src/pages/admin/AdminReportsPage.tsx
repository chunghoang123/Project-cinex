import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ConsolePageHeader } from '../../components/console/ConsolePageHeader'
import { StatCard } from '../../components/console/StatCard'
import { adminService } from '../../services/adminService'
import type { AdminDashboard, Booking } from '../../types/Admin'
import { formatShortVnd, formatVnd, localDateKey } from '../../utils/format'
import { errorMessage } from '../../utils/consoleFormat'

/**
 * Admin: báo cáo doanh thu (task 13.2).
 * Tổng hợp doanh thu theo ngày/kênh, phim bán chạy và phân bố trạng thái đơn
 * từ dữ liệu dashboard + danh sách đơn của hệ thống.
 */
export function AdminReportsPage() {
  const [dashboard, setDashboard] = useState<AdminDashboard | null>(null)
  const [bookings, setBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = () => {
    setLoading(true)
    setError('')
    Promise.all([adminService.getDashboard(), adminService.getBookings({})])
      .then(([dash, list]) => {
        setDashboard(dash)
        setBookings(list)
      })
      .catch((err) => setError(errorMessage(err, 'Không tải được báo cáo')))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [])

  const report = useMemo(() => {
    // Doanh thu chỉ tính đơn đã xác nhận thanh toán (CONFIRMED).
    const paid = bookings.filter((b) => b.status === 'CONFIRMED')
    const byDay = new Map<string, number>()
    const channels = {
      ONLINE: { revenue: 0, orders: 0 },
      COUNTER: { revenue: 0, orders: 0 },
    }
    for (const b of paid) {
      const key = localDateKey(b.paidAt || b.createdAt)
      byDay.set(key, (byDay.get(key) ?? 0) + b.totalAmount)
      const ch = b.channel === 'COUNTER' ? channels.COUNTER : channels.ONLINE
      ch.revenue += b.totalAmount
      ch.orders += 1
    }
    const days: Array<{ date: string; revenue: number }> = []
    const today = new Date()
    for (let i = 13; i >= 0; i--) {
      const d = new Date(today)
      d.setDate(d.getDate() - i)
      const key = localDateKey(d)
      days.push({ date: key, revenue: byDay.get(key) ?? 0 })
    }
    const totalRevenue = paid.reduce((sum, b) => sum + b.totalAmount, 0)
    const totalTickets = paid.reduce((sum, b) => sum + (b.seats?.length ?? 0), 0)
    const statusCounts = bookings.reduce<Record<string, number>>((acc, b) => {
      acc[b.status] = (acc[b.status] ?? 0) + 1
      return acc
    }, {})
    return { paid, days, channels, totalRevenue, totalTickets, statusCounts }
  }, [bookings])

  if (loading) {
    return (
      <div className="console-card console-empty">
        <strong>Đang tải báo cáo…</strong>
        Dữ liệu doanh thu đang được tổng hợp từ toàn hệ thống.
      </div>
    )
  }

  if (error || !dashboard) {
    return (
      <div className="console-card console-empty">
        <strong>Không tải được báo cáo</strong>
        {error || 'Vui lòng thử lại.'}
        <div style={{ marginTop: 14 }}>
          <button type="button" className="btn btn-ghost btn-sm" onClick={load}>
            Thử lại
          </button>
        </div>
      </div>
    )
  }

  const maxDayRevenue = Math.max(...report.days.map((d) => d.revenue), 1)
  const avgOrder = report.paid.length ? Math.round(report.totalRevenue / report.paid.length) : 0
  const cancelRate = bookings.length
    ? Math.round(((report.statusCounts.CANCELLED ?? 0) / bookings.length) * 100)
    : 0
  const statusRows: Array<{ key: string; label: string }> = [
    { key: 'CONFIRMED', label: 'Đã xác nhận' },
    { key: 'PENDING', label: 'Chờ thanh toán' },
    { key: 'EXPIRED', label: 'Hết hạn giữ ghế' },
    { key: 'CANCELLED', label: 'Đã hủy' },
  ]

  return (
    <div>
      <ConsolePageHeader
        eyebrow="Báo cáo"
        title="Doanh thu & bán vé"
        description="Thống kê doanh thu theo ngày, kênh bán và hiệu suất phim trong 14 ngày gần nhất."
        actions={
          <button type="button" className="btn btn-ghost btn-sm" onClick={load}>
            Làm mới
          </button>
        }
      />

      <div className="console-stat-grid">
        <StatCard icon="cash" label="Doanh thu (14 ngày)" value={formatVnd(report.totalRevenue)} hint="Từ đơn đã thanh toán" tone="accent" />
        <StatCard icon="ticket" label="Vé bán ra" value={report.totalTickets.toLocaleString('vi-VN')} hint="Ghế trong đơn hợp lệ" tone="violet" />
        <StatCard icon="receipt" label="Giá trị TB / đơn" value={formatVnd(avgOrder)} hint={`${report.paid.length} đơn hợp lệ`} tone="green" />
        <StatCard icon="alert" label="Tỉ lệ đơn hủy" value={`${cancelRate}%`} hint={`${bookings.length} đơn toàn hệ thống`} tone="amber" />
      </div>

      <div className="console-grid console-grid--2">
        <div className="console-card">
          <div className="console-card__head">
            <div>
              <h2>Doanh thu 14 ngày</h2>
              <p>Đơn vị: đồng — chỉ tính đơn đã thanh toán</p>
            </div>
            <span className="badge badge-accent">14 ngày</span>
          </div>
          <div className="revenue-chart">
            {report.days.map((d) => (
              <div className="revenue-bar-wrap" key={d.date}>
                <strong>{d.revenue > 0 ? formatShortVnd(d.revenue) : ''}</strong>
                <div
                  className="revenue-bar"
                  style={{ height: `${Math.max(6, (d.revenue / maxDayRevenue) * 100)}%` }}
                  title={`${d.date}: ${formatVnd(d.revenue)}`}
                />
                <span>{d.date.slice(5)}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="console-card">
          <div className="console-card__head">
            <div>
              <h2>Doanh thu theo kênh</h2>
              <p>So sánh kênh online và bán tại quầy</p>
            </div>
          </div>
          <div className="top-movie-row">
            <span className="top-movie-rank">1</span>
            <div>
              <strong>Kênh online</strong>
              <small>{report.channels.ONLINE.orders} đơn</small>
            </div>
            <b>{formatVnd(report.channels.ONLINE.revenue)}</b>
          </div>
          <div className="top-movie-row">
            <span className="top-movie-rank">2</span>
            <div>
              <strong>Bán tại quầy</strong>
              <small>{report.channels.COUNTER.orders} đơn</small>
            </div>
            <b>{formatVnd(report.channels.COUNTER.revenue)}</b>
          </div>

          <div className="console-card__head" style={{ marginTop: 18 }}>
            <div>
              <h2>Trạng thái đơn hàng</h2>
              <p>Phân bố {bookings.length} đơn trong hệ thống</p>
            </div>
          </div>
          <div className="console-table-wrap">
            <table className="console-table">
              <thead>
                <tr>
                  <th>Trạng thái</th>
                  <th className="num">Số đơn</th>
                  <th className="num">Tỷ lệ</th>
                </tr>
              </thead>
              <tbody>
                {statusRows.map((row) => (
                  <tr key={row.key}>
                    <td>{row.label}</td>
                    <td className="num">{(report.statusCounts[row.key] ?? 0).toLocaleString('vi-VN')}</td>
                    <td className="num">
                      {bookings.length ? Math.round(((report.statusCounts[row.key] ?? 0) / bookings.length) * 100) : 0}%
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div className="console-card" style={{ marginTop: 16 }}>
        <div className="console-card__head">
          <div>
            <h2>Phim bán chạy</h2>
            <p>Xếp theo doanh thu 30 ngày gần nhất</p>
          </div>
          <Link to="/admin/movies" className="console-card__link">Quản lý phim</Link>
        </div>
        {dashboard.topMovies.length === 0 ? (
          <div className="console-empty">
            <strong>Chưa có dữ liệu bán vé</strong>
            Doanh thu theo phim sẽ xuất hiện sau giao dịch đầu tiên.
          </div>
        ) : (
          <div className="console-table-wrap">
            <table className="console-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Phim</th>
                  <th className="num">Vé bán ra</th>
                  <th className="num">Doanh thu</th>
                </tr>
              </thead>
              <tbody>
                {dashboard.topMovies.map((movie, index) => (
                  <tr key={movie.movieId}>
                    <td>{index + 1}</td>
                    <td>{movie.title}</td>
                    <td className="num">{movie.tickets.toLocaleString('vi-VN')}</td>
                    <td className="num">{formatVnd(movie.revenue)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
