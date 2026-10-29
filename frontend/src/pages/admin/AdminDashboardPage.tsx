import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ConsolePageHeader } from '../../components/console/ConsolePageHeader'
import { StatCard } from '../../components/console/StatCard'
import { StatusBadge } from '../../components/console/StatusBadge'
import { adminService } from '../../services/adminService'
import type { AdminDashboard } from '../../types/Admin'
import { formatShortVnd, formatVnd } from '../../utils/format'
import { errorMessage, formatTime } from '../../utils/consoleFormat'

export function AdminDashboardPage() {
  const [data, setData] = useState<AdminDashboard | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = () => {
    setLoading(true)
    setError('')
    adminService.getDashboard()
      .then(setData)
      .catch((err) => setError(errorMessage(err, 'Không tải được dashboard')))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [])

  if (loading) {
    return (
      <div className="console-card console-empty">
        <strong>Đang tải dashboard…</strong>
        Dữ liệu đang được tổng hợp từ hệ thống.
      </div>
    )
  }

  if (error || !data) {
    return (
      <div className="console-card console-empty">
        <strong>Không tải được dashboard</strong>
        {error || 'Vui lòng thử lại.'}
        <div style={{ marginTop: 14 }}>
          <button type="button" className="btn btn-ghost btn-sm" onClick={load}>
            Thử lại
          </button>
        </div>
      </div>
    )
  }

  const maxRevenue = Math.max(...data.revenueLast7Days.map((item) => item.revenue), 1)

  return (
    <div>
      <ConsolePageHeader
        eyebrow="Tổng quan"
        title="Dashboard vận hành"
        description="Theo dõi doanh thu, vé bán ra và tình hình đặt vé của toàn hệ thống Cinex."
        actions={
          <>
            <Link to="/admin/showtimes" className="btn btn-ghost btn-sm">
              Quản lý suất chiếu
            </Link>
            <Link to="/admin/bookings" className="btn btn-primary btn-sm">
              Xem đơn hàng
            </Link>
          </>
        }
      />

      <div className="console-stat-grid">
        <StatCard icon="cash" label="Doanh thu hôm nay" value={formatVnd(data.revenueToday)} hint="Từ các giao dịch thành công" tone="accent" />
        <StatCard icon="ticket" label="Vé đã bán" value={data.ticketsToday.toLocaleString('vi-VN')} hint="Vé phát hành trong ngày" tone="violet" />
        <StatCard icon="receipt" label="Đơn đặt vé" value={data.bookingsToday.toLocaleString('vi-VN')} hint="Tất cả kênh" tone="green" />
        <StatCard icon="calendar" label="Suất chiếu" value={data.showtimesToday.toLocaleString('vi-VN')} hint="Đang hoạt động hôm nay" tone="amber" />
        <StatCard icon="users" label="Khách hàng" value={data.customers.toLocaleString('vi-VN')} hint="Tài khoản khách hàng" tone="accent" />
      </div>

      <div className="console-grid console-grid--2">
        <div className="console-card">
          <div className="console-card__head">
            <div>
              <h2>Doanh thu 7 ngày gần nhất</h2>
              <p>Đơn vị: đồng · chỉ tính giao dịch thành công</p>
            </div>
            <span className="badge badge-accent">7 ngày</span>
          </div>
          <div className="revenue-chart">
            {data.revenueLast7Days.map((item) => (
              <div className="revenue-bar-wrap" key={item.date}>
                <strong>{item.revenue > 0 ? formatShortVnd(item.revenue) : ''}</strong>
                <div
                  className="revenue-bar"
                  style={{ height: `${Math.max(6, (item.revenue / maxRevenue) * 100)}%` }}
                  title={`${item.date}: ${formatVnd(item.revenue)}`}
                />
                <span>{item.date}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="console-card">
          <div className="console-card__head">
            <div>
              <h2>Phim bán chạy</h2>
              <p>Xếp theo doanh thu 30 ngày gần nhất</p>
            </div>
            <Link to="/admin/movies" className="console-card__link">Quản lý phim</Link>
          </div>
          {data.topMovies.length === 0 ? (
            <div className="console-empty">
              <strong>Chưa có dữ liệu bán vé</strong>
              Doanh thu sẽ xuất hiện sau giao dịch đầu tiên.
            </div>
          ) : (
            data.topMovies.map((movie, index) => (
              <div className="top-movie-row" key={movie.movieId}>
                <span className="top-movie-rank">{index + 1}</span>
                <div>
                  <strong>{movie.title}</strong>
                  <small>{movie.tickets} vé</small>
                </div>
                <b>{formatVnd(movie.revenue)}</b>
              </div>
            ))
          )}
        </div>
      </div>

      <div className="console-card" style={{ marginTop: 16 }}>
        <div className="console-card__head">
          <div>
            <h2>Đơn đặt vé gần đây</h2>
            <p>8 đơn mới nhất trên toàn hệ thống</p>
          </div>
          <Link to="/admin/bookings" className="console-card__link">Xem tất cả</Link>
        </div>
        <div className="console-table-wrap">
          <table className="console-table">
            <thead>
              <tr>
                <th>Mã đơn</th>
                <th>Khách hàng</th>
                <th>Phim / suất chiếu</th>
                <th>Kênh</th>
                <th className="num">Tổng tiền</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {data.recentBookings.length === 0 ? (
                <tr>
                  <td colSpan={6}>
                    <div className="console-empty">Chưa có đơn đặt vé nào.</div>
                  </td>
                </tr>
              ) : data.recentBookings.map((booking) => (
                <tr key={booking.id}>
                  <td className="mono">{booking.bookingCode}</td>
                  <td>
                    <div className="cell-main">{booking.customerName || 'Khách lẻ'}</div>
                    <div className="cell-sub">{booking.customerPhone || '—'}</div>
                  </td>
                  <td>
                    <div className="cell-main">{booking.showtime.movieTitle}</div>
                    <div className="cell-sub">
                      {formatTime(booking.showtime.startTime)} · {booking.showtime.cinemaName} · {booking.showtime.auditoriumName}
                    </div>
                  </td>
                  <td><StatusBadge status={booking.channel} /></td>
                  <td className="num">{formatVnd(booking.totalAmount)}</td>
                  <td><StatusBadge status={booking.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
