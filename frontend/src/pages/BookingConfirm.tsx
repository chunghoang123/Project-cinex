import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { cinemas, movies, snacks, showtimesFor } from '../data/mockData'
import { formatVnd } from '../utils/format'
import { Icon } from '../components/ui/Icon'
import { PageBar } from '../components/ui/PageBar'

export function BookingConfirm() {
  const navigate = useNavigate()
  const [selectedSnacks, setSelectedSnacks] = useState<string[]>(['duo'])

  const selectedSeats: string[] = JSON.parse(sessionStorage.getItem('selectedSeatLabels') || '[]')
  const seatIds: string[] = JSON.parse(sessionStorage.getItem('selectedSeats') || '[]')
  const showtimeId = sessionStorage.getItem('showtimeId')
  const movieId = sessionStorage.getItem('movieId')
  const cinemaId = sessionStorage.getItem('cinemaId')
  const ticketTotal = Number(sessionStorage.getItem('bookingTotal') || 0)

  const movie = movies.find((item) => String(item.id) === movieId) ?? movies[0]
  const cinema = cinemas.find((item) => String(item.id) === cinemaId) ?? cinemas[0]
  const showtime = showtimesFor(movie.id).find((item) => String(item.id) === showtimeId) ?? showtimesFor(movie.id)[0]

  const snackTotal = useMemo(
    () => snacks.filter((snack) => selectedSnacks.includes(snack.id)).reduce((sum, snack) => sum + snack.price, 0),
    [selectedSnacks],
  )
  const serviceFee = 15000
  const grandTotal = ticketTotal + snackTotal + serviceFee

  const toggleSnack = (id: string) => {
    setSelectedSnacks((current) =>
      current.includes(id) ? current.filter((item) => item !== id) : [...current, id],
    )
  }

  const handleContinue = () => {
    sessionStorage.setItem('finalTotal', String(grandTotal))
    sessionStorage.setItem('snackTotal', String(snackTotal))
    navigate('/booking/payment')
  }

  if (seatIds.length === 0) {
    return (
      <div className="container">
        <PageBar title="Xác nhận đặt vé" backTo="/movies" />
        <div className="empty-state">
          <Icon name="seat" size={40} />
          <strong>Chưa có ghế nào được chọn</strong>
          <p>Hãy chọn phim và suất chiếu trước khi thanh toán.</p>
          <Link to="/movies" className="btn btn-primary btn-sm">
            Chọn phim
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="container">
      <PageBar title="Xác nhận đặt vé" backTo={`/seats/${showtime.id}?movieId=${movie.id}`} />

      <div className="checkout-layout">
        <div className="stack gap-16">
          <div className="movie-mini">
            <img src={movie.posterUrl} alt={movie.title} />
            <div>
              <span className="eyebrow">Đang chiếu</span>
              <div className="movie-mini__title">{movie.title}</div>
              <div className="meta-row" style={{ marginTop: 5 }}>
                <span>{cinema.name}</span>
                <span className="meta-dot" />
                <span>{showtime.hall}</span>
              </div>
              <div className="meta-row" style={{ marginTop: 3 }}>
                <Icon name="calendar" size={12} />
                Hôm nay · {showtime.time}
              </div>
            </div>
          </div>

          <div className="surface card-pad row between">
            <div className="row gap-10">
              <span className="header-icon" style={{ width: 36, height: 36 }}>
                <Icon name="seat" size={17} />
              </span>
              <div>
                <div style={{ fontSize: 12, color: 'var(--muted)' }}>Ghế đã giữ</div>
                <strong style={{ fontSize: 14 }}>{selectedSeats.join(', ')}</strong>
              </div>
            </div>
            <span className="badge badge-success">{selectedSeats.length} vé</span>
          </div>

          <section>
            <h2 className="h3" style={{ marginBottom: 12 }}>
              Bắp nước &amp; đồ uống
            </h2>
            <div className="stack gap-10">
              {snacks.map((snack) => {
                const active = selectedSnacks.includes(snack.id)
                return (
                  <button
                    key={snack.id}
                    type="button"
                    className={`snack-card ${active ? 'is-selected' : ''}`}
                    onClick={() => toggleSnack(snack.id)}
                  >
                    <img className="snack-card__img" src={snack.image} alt={snack.name} loading="lazy" />
                    <div style={{ flex: 1, textAlign: 'left' }}>
                      <div className="snack-card__title">{snack.name}</div>
                      <div className="snack-card__desc">{snack.description}</div>
                      <div className="snack-card__price">{formatVnd(snack.price)}</div>
                    </div>
                    <span className={`pay-method__radio ${active ? 'is-active' : ''}`} style={{ border: active ? '5px solid var(--accent)' : '1px solid var(--border-strong)' }} />
                  </button>
                )
              })}
            </div>
          </section>

          <section className="surface card-pad">
            <h2 className="h3" style={{ marginBottom: 10 }}>
              Tóm tắt đơn hàng
            </h2>
            <div className="summary-line">
              <span>Vé ({selectedSeats.length} ghế)</span>
              <strong>{formatVnd(ticketTotal)}</strong>
            </div>
            <div className="summary-line">
              <span>Bắp nước &amp; đồ uống</span>
              <strong>{formatVnd(snackTotal)}</strong>
            </div>
            <div className="summary-line">
              <span>Phí dịch vụ</span>
              <strong>{formatVnd(serviceFee)}</strong>
            </div>
            <div className="summary-line is-total">
              <span>Tổng cộng</span>
              <strong>{formatVnd(grandTotal)}</strong>
            </div>
          </section>
        </div>

        <aside className="checkout-aside">
          <div className="surface card-pad">
            <h2 className="h3" style={{ marginBottom: 12 }}>
              Thông tin thanh toán
            </h2>
            <div className="summary-line">
              <span>Phim</span>
              <strong>{movie.title}</strong>
            </div>
            <div className="summary-line">
              <span>Suất chiếu</span>
              <strong>{showtime.time}</strong>
            </div>
            <div className="summary-line">
              <span>Rạp</span>
              <strong>{cinema.name}</strong>
            </div>
            <div className="summary-line">
              <span>Ghế</span>
              <strong>{selectedSeats.join(', ')}</strong>
            </div>
            <div className="summary-line is-total">
              <span>Tổng cộng</span>
              <strong>{formatVnd(grandTotal)}</strong>
            </div>
            <button type="button" className="btn btn-gradient btn-block btn-lg" style={{ marginTop: 14 }} onClick={handleContinue}>
              Tiến hành thanh toán
              <Icon name="arrow-right" size={17} />
            </button>
            <div className="secure-note" style={{ marginTop: 12 }}>
              <Icon name="shield" size={15} />
              Giao dịch được mã hóa và bảo mật.
            </div>
          </div>
        </aside>
      </div>
    </div>
  )
}
