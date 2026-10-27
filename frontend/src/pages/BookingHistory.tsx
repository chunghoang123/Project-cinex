import { useState } from 'react'
import { Link } from 'react-router-dom'
import { bookings } from '../data/mockData'
import { formatVnd } from '../utils/format'
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
              {visible.map((booking) => (
                <div key={booking.id} className="booking-item">
                  <img src={booking.posterUrl} alt={booking.movieTitle} loading="lazy" />
                  <div className="booking-item__body">
                    <div className="row between gap-8">
                      <div className="booking-item__title">{booking.movieTitle}</div>
                      <span className={`badge ${booking.status === 'UPCOMING' ? 'badge-success' : 'badge-muted'}`}>
                        {booking.status === 'UPCOMING' ? 'Sắp chiếu' : 'Đã xem'}
                      </span>
                    </div>
                    <div className="booking-item__meta">
                      {booking.cinema} · {booking.hall}
                    </div>
                    <div className="booking-item__meta">
                      <Icon name="calendar" size={12} /> {booking.date} · {booking.time} · Ghế {booking.seats.join(', ')}
                    </div>
                    <div className="row between gap-8" style={{ marginTop: 4 }}>
                      <span className="booking-item__meta">Mã: {booking.code}</span>
                      <strong style={{ fontSize: 13 }}>{formatVnd(booking.total)}</strong>
                    </div>
                    <div className="booking-item__actions">
                      {booking.status === 'UPCOMING' ? (
                        <>
                          <Link to={`/ticket/${booking.code}`} className="btn btn-accent btn-sm">
                            <Icon name="qr" size={14} />
                            Xem vé
                          </Link>
                          <button
                            type="button"
                            className="btn btn-outline btn-sm"
                            onClick={() => handleCancel(booking.id)}
                          >
                            Hủy vé
                          </button>
                        </>
                      ) : (
                        <Link to={`/movies/${booking.movieId}`} className="btn btn-ghost btn-sm">
                          Đặt lại
                        </Link>
                      )}
                    </div>
                  </div>
                </div>
              ))}
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
