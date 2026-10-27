import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { bookings } from '../data/mockData'
import { formatVnd } from '../utils/format'
import { errorMessage, formatTime } from '../utils/consoleFormat'
import { Icon } from '../components/ui/Icon'
import { PageBar } from '../components/ui/PageBar'

const tabs = [
  { value: 'UPCOMING', label: 'Sắp chiếu' },
  { value: 'COMPLETED', label: 'Đã xem' },
] as const

export function BookingHistory() {
  const [tab, setTab] = useState<'UPCOMING' | 'COMPLETED'>('UPCOMING')
  const [items, setItems] = useState(bookings)
  const visible = items.filter((booking) => booking.status === tab)

  const handleCancel = (id: number) => {
    if (!window.confirm('Bạn có chắc muốn hủy vé này? Tiền sẽ được hoàn về phương thức thanh toán ban đầu.')) {
      return
    }
    setItems((current) => current.filter((booking) => booking.id !== id))
  }

  return (
    <div className="container">
      <PageBar title="Vé của tôi" backTo="/" />

      <div className="history-layout">
        <div>
          <div className="tabs" style={{ marginBottom: 16 }}>
            {tabs.map((item) => (
              <button
                key={item.value}
                type="button"
                className={tab === item.value ? 'is-active' : ''}
                onClick={() => setTab(item.value)}
              >
                {item.label}
              </button>
            ))}
          </div>

          {visible.length === 0 ? (
            <div className="empty-state">
              <Icon name="ticket" size={40} />
              <strong>Chưa có vé nào</strong>
              <p>Đặt vé đầu tiên của bạn để bắt đầu hành trình điện ảnh.</p>
              <Link to="/movies" className="btn btn-primary btn-sm">
                Khám phá phim
              </Link>
            </div>
          ) : (
            <div>
              {visible.map((booking) => {
                const showtime = booking.showtime
                const seats = booking.seats.map((seat) => seat.seatCode)
                const canViewTicket = booking.status === 'CONFIRMED'
                const badgeClass =
                  booking.status === 'CONFIRMED'
                    ? 'badge-success'
                    : booking.status === 'PENDING'
                      ? 'badge-warning'
                      : 'badge-muted'
                return (
                  <div key={booking.id} className="booking-item">
                    <img src={showtime.posterUrl} alt={showtime.movieTitle} loading="lazy" />
                    <div className="booking-item__body">
                      <div className="row between gap-8">
                        <div className="booking-item__title">{showtime.movieTitle}</div>
                        <span className={`badge ${badgeClass}`}>
                          {STATUS_LABEL[booking.status] ?? booking.status}
                        </span>
                      </div>
                      <div className="booking-item__meta">
                        {showtime.cinemaName} · {showtime.auditoriumName}
                      </div>
                      <div className="booking-item__meta">
                        <Icon name="calendar" size={12} /> {formatTime(showtime.startTime)} · Ghế{' '}
                        {seats.join(', ')}
                      </div>
                      <div className="row between gap-8" style={{ marginTop: 4 }}>
                        <span className="booking-item__meta">Mã: {booking.bookingCode}</span>
                        <strong style={{ fontSize: 13 }}>{formatVnd(Number(booking.totalAmount))}</strong>
                      </div>
                      <div className="booking-item__actions">
                        {booking.status === 'PENDING' ? (
                          <Link
                            to={`/booking/payment?code=${booking.bookingCode}`}
                            className="btn btn-accent btn-sm"
                          >
                            <Icon name="qr" size={14} />
                            Thanh toán
                          </Link>
                        ) : canViewTicket ? (
                          <Link to={`/ticket/${booking.bookingCode}`} className="btn btn-accent btn-sm">
                            <Icon name="qr" size={14} />
                            Xem vé
                          </Link>
                        ) : (
                          <Link to={`/movies/${showtime.movieId}`} className="btn btn-ghost btn-sm">
                            Đặt lại
                          </Link>
                        )}
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>

        <aside className="hide-mobile">
          <div className="promo-banner">
            <span className="eyebrow">Cinex Rewards</span>
            <h3>Bạn có 1.280 điểm</h3>
            <p>Đổi điểm lấy combo bắp nước và vé xem phim miễn phí.</p>
            <Link to="/profile" className="btn btn-accent btn-sm" style={{ marginTop: 14 }}>
              Xem ưu đãi
            </Link>
          </div>

          <div className="surface card-pad" style={{ marginTop: 16 }}>
            <h3 className="h3" style={{ marginBottom: 10 }}>
              Cần hỗ trợ?
            </h3>
            <p className="muted" style={{ fontSize: 13, marginBottom: 12 }}>
              Liên hệ Cinex qua hotline 1900 1234 hoặc chat với trợ lý AI.
            </p>
            <Link to="/ai-chat" className="btn btn-ghost btn-block btn-sm">
              <Icon name="chat" size={15} />
              Chat với Cinex AI
            </Link>
          </div>
        </aside>
      </div>
    </div>
  )
}
