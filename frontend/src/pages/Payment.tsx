import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { cinemas, movies, showtimesFor } from '../data/mockData'
import { formatVnd } from '../utils/format'
import { Icon, type IconName } from '../components/ui/Icon'
import { PageBar } from '../components/ui/PageBar'

const methods: Array<{ id: string; name: string; desc: string; icon: IconName; color: string }> = [
  { id: 'momo', name: 'MoMo', desc: 'Ví điện tử MoMo', icon: 'momo', color: '#a50064' },
  { id: 'vnpay', name: 'VNPay', desc: 'Cổng thanh toán VNPay', icon: 'vnpay', color: '#0066cc' },
  { id: 'card', name: 'Thẻ ngân hàng', desc: 'Visa, Mastercard, JCB', icon: 'credit-card', color: '#22d3ee' },
  { id: 'apple', name: 'Apple Pay', desc: 'Thanh toán nhanh với Face ID', icon: 'apple', color: '#f8fafc' },
]

export function Payment() {
  const navigate = useNavigate()
  const [method, setMethod] = useState('momo')
  const [paying, setPaying] = useState(false)

  const selectedSeats: string[] = JSON.parse(sessionStorage.getItem('selectedSeatLabels') || '[]')
  const showtimeId = sessionStorage.getItem('showtimeId')
  const movieId = sessionStorage.getItem('movieId')
  const cinemaId = sessionStorage.getItem('cinemaId')
  const total = Number(sessionStorage.getItem('finalTotal') || sessionStorage.getItem('bookingTotal') || 0)

  const movie = movies.find((item) => String(item.id) === movieId) ?? movies[0]
  const cinema = cinemas.find((item) => String(item.id) === cinemaId) ?? cinemas[0]
  const showtime = showtimesFor(movie.id).find((item) => String(item.id) === showtimeId) ?? showtimesFor(movie.id)[0]

  const handlePay = () => {
    setPaying(true)
    window.setTimeout(() => {
      const code = `CX-${Date.now().toString().slice(-6)}`
      sessionStorage.setItem('ticketCode', code)
      sessionStorage.setItem('ticketMovieId', String(movie.id))
      sessionStorage.setItem('ticketCinemaId', String(cinema.id))
      sessionStorage.setItem('ticketShowtimeId', String(showtime.id))
      sessionStorage.setItem('ticketSeats', JSON.stringify(selectedSeats))
      sessionStorage.setItem('ticketTotal', String(total))
      navigate(`/ticket/${code}`)
    }, 900)
  }

  if (!total || selectedSeats.length === 0) {
    return (
      <div className="container">
        <PageBar title="Thanh toán" backTo="/movies" />
        <div className="empty-state">
          <Icon name="card" size={40} />
          <strong>Chưa có giao dịch nào</strong>
          <p>Hãy chọn ghế và xác nhận đặt vé trước khi thanh toán.</p>
          <Link to="/movies" className="btn btn-primary btn-sm">
            Chọn phim
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="container">
      <PageBar title="Thanh toán" backTo="/booking/confirm" />

      <div className="checkout-layout">
        <div className="stack gap-16">
          <div className="movie-mini">
            <img src={movie.posterUrl} alt={movie.title} />
            <div>
              <span className="eyebrow">Thanh toán an toàn</span>
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

          <section>
            <h2 className="h3" style={{ marginBottom: 12 }}>
              Phương thức thanh toán
            </h2>
            <div className="stack gap-10">
              {methods.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className={`pay-method ${method === item.id ? 'is-active' : ''}`}
                  onClick={() => setMethod(item.id)}
                >
                  <span className="pay-method__icon" style={{ color: item.color }}>
                    <Icon name={item.icon} size={22} />
                  </span>
                  <span>
                    <span className="pay-method__name">{item.name}</span>
                    <span className="pay-method__desc">{item.desc}</span>
                  </span>
                  <span className="pay-method__radio" />
                </button>
              ))}
            </div>
          </section>

          <div className="secure-note">
            <Icon name="shield" size={16} />
            Thông tin thanh toán của bạn được mã hóa theo chuẩn PCI DSS.
          </div>
        </div>

        <aside className="checkout-aside">
          <div className="surface card-pad">
            <h2 className="h3" style={{ marginBottom: 12 }}>
              Chi tiết thanh toán
            </h2>
            <div className="summary-line">
              <span>Ghế</span>
              <strong>{selectedSeats.join(', ')}</strong>
            </div>
            <div className="summary-line">
              <span>Suất chiếu</span>
              <strong>{showtime.time}</strong>
            </div>
            <div className="summary-line">
              <span>Rạp</span>
              <strong>{cinema.name}</strong>
            </div>
            <div className="summary-line is-total">
              <span>Tổng cộng</span>
              <strong>{formatVnd(total)}</strong>
            </div>
            <button
              type="button"
              className="btn btn-gradient btn-block btn-lg"
              style={{ marginTop: 14 }}
              disabled={paying}
              onClick={handlePay}
            >
              {paying ? (
                <>
                  <span className="spinner" style={{ width: 18, height: 18, borderWidth: 2 }} />
                  Đang xử lý...
                </>
              ) : (
                <>
                  <Icon name="lock" size={16} />
                  Thanh toán {formatVnd(total)}
                </>
              )}
            </button>
            <p className="form-hint" style={{ marginTop: 10, textAlign: 'center' }}>
              Bằng việc thanh toán, bạn đồng ý với điều khoản của Cinex.
            </p>
          </div>
        </aside>
      </div>
    </div>
  )
}
