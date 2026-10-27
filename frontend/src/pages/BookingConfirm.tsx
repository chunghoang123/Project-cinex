import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { snacks } from '../data/mockData'
import { bookingService, type SeatDraft } from '../services/bookingService'
import type { Showtime } from '../types/Admin'
import { formatVnd } from '../utils/format'
import { Icon } from '../components/ui/Icon'
import { PageBar } from '../components/ui/PageBar'

function readDraft(): SeatDraft | null {
  const raw = sessionStorage.getItem('seatHoldDraft')
  if (!raw) return null
  try {
    return JSON.parse(raw) as SeatDraft
  } catch {
    return null
  }
}

export function BookingConfirm() {
  const navigate = useNavigate()
  const draft = useMemo(readDraft, [])
  const [showtime, setShowtime] = useState<Showtime | null>(null)
  const [selectedSnacks, setSelectedSnacks] = useState<string[]>([])
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

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
  const seatAmount = draft?.seatAmount ?? 0
  const grandTotal = seatAmount + snackTotal

  const toggleSnack = (id: string) => {
    setSelectedSnacks((current) =>
      current.includes(id) ? current.filter((item) => item !== id) : [...current, id],
    )
  }

  const handleContinue = async () => {
    if (!draft) return
    setSubmitting(true)
    setError('')
    try {
      const booking = await bookingService.createHold({
        showtimeId: draft.showtimeId,
        seatIds: draft.seatIds,
        snackAmount: snackTotal,
        snackNote: selectedSnacks.length > 0
          ? snacks.filter((snack) => selectedSnacks.includes(snack.id)).map((snack) => snack.name).join(', ')
          : undefined,
      })
      sessionStorage.setItem('pendingBookingCode', booking.bookingCode)
      sessionStorage.removeItem('seatHoldDraft')
      navigate('/booking/payment')
    } catch (err) {
      setError(errorMessage(err, 'Không giữ được ghế, vui lòng chọn lại'))
    } finally {
      setSubmitting(false)
    }
  }

  if (!draft) {
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
          {showtime && (
            <div className="movie-mini">
              <img src={showtime.posterUrl} alt={showtime.movieTitle} />
              <div>
                <span className="eyebrow">Đang chiếu</span>
                <div className="movie-mini__title">{showtime.movieTitle}</div>
                <div className="meta-row" style={{ marginTop: 5 }}>
                  <span>{showtime.cinemaName}</span>
                  <span className="meta-dot" />
                  <span>{showtime.auditoriumName}</span>
                </div>
                <div className="meta-row" style={{ marginTop: 3 }}>
                  <Icon name="calendar" size={12} />
                  {formatDateLong(showtime.startTime)} · {formatTime(showtime.startTime)}
                </div>
              </div>
            </div>
          )}

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
            <span className="badge badge-success">{draft.seatCodes.length} vé</span>
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
                    <span
                      className={`pay-method__radio ${active ? 'is-active' : ''}`}
                      style={{ border: active ? '5px solid var(--accent)' : '1px solid var(--border-strong)' }}
                    />
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
              <span>Vé ({draft.seatCodes.length} ghế)</span>
              <strong>{formatVnd(seatAmount)}</strong>
            </div>
            <div className="summary-line">
              <span>Bắp nước &amp; đồ uống</span>
              <strong>{formatVnd(snackTotal)}</strong>
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
              <strong>{showtime ? formatTime(showtime.startTime) : '—'}</strong>
            </div>
            <div className="summary-line">
              <span>Rạp</span>
              <strong>{showtime?.cinemaName ?? '—'}</strong>
            </div>
            <div className="summary-line">
              <span>Ghế</span>
              <strong>{selectedSeats.join(', ')}</strong>
            </div>
            <div className="summary-line is-total">
              <span>Tổng cộng</span>
              <strong>{formatVnd(grandTotal)}</strong>
            </div>

            {error && <p className="form-error" style={{ marginTop: 10 }}>{error}</p>}

            <button
              type="button"
              className="btn btn-gradient btn-block btn-lg"
              style={{ marginTop: 14 }}
              onClick={() => void handleContinue()}
              disabled={submitting || !showtime}
            >
              {submitting ? (
                <>
                  <span className="spinner" style={{ width: 18, height: 18, borderWidth: 2 }} />
                  Đang tạo đơn...
                </>
              ) : (
                <>
                  Tiến hành thanh toán
                  <Icon name="arrow-right" size={17} />
                </>
              )}
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
