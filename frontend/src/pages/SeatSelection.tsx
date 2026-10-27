import { useMemo, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { buildSeatMap, cinemas, movies, showtimesFor } from '../data/mockData'
import { formatDuration, formatVnd } from '../utils/format'
import { errorMessage, formatDateLong, formatTime } from '../utils/consoleFormat'
import { Icon } from '../components/ui/Icon'
import { PageBar } from '../components/ui/PageBar'

const seatPrice = {
  standard: 110000,
  vip: 140000,
  couple: 220000,
}

export function SeatSelection() {
  const { showtimeId } = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const location = useLocation()
  const numericShowtimeId = Number(showtimeId)

  const movie = movies.find((item) => String(item.id) === searchParams.get('movieId')) ?? movies[0]
  const cinema = cinemas.find((item) => String(item.id) === searchParams.get('cinemaId')) ?? cinemas[0]
  const showtime = showtimesFor(movie.id).find((item) => String(item.id) === showtimeId) ?? showtimesFor(movie.id)[0]
  const seats = useMemo(() => buildSeatMap(), [])

  useEffect(() => {
    if (!Number.isFinite(numericShowtimeId)) {
      setError('Suất chiếu không hợp lệ')
      setLoading(false)
      return
    }
    let cancelled = false
    setLoading(true)
    setError('')
    Promise.all([
      bookingService.getShowtime(numericShowtimeId),
      bookingService.getSeats(numericShowtimeId),
    ])
      .then(([showtimeData, seatData]) => {
        if (cancelled) return
        setShowtime(showtimeData)
        setSeats(seatData)
      })
      .catch((err) => {
        if (!cancelled) setError(errorMessage(err, 'Không tải được sơ đồ ghế'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [numericShowtimeId, reloadKey])

  const total = selected.reduce((sum, id) => {
    if (id.startsWith('H')) return sum + seatPrice.couple
    if (id.startsWith('E') || id.startsWith('F')) return sum + seatPrice.vip
    return sum + seatPrice.standard
  }, 0)

  const toggleSeat = (id: string) => {
    setSelected((current) => {
      if (current.includes(seat.id)) return current.filter((id) => id !== seat.id)
      if (current.length >= MAX_SEATS) return current
      return [...current, seat.id]
    })
  }

  const handleContinue = () => {
    if (selected.length === 0) return
    sessionStorage.setItem('selectedSeats', JSON.stringify(selected))
    sessionStorage.setItem('selectedSeatLabels', JSON.stringify(selectedLabels))
    sessionStorage.setItem('showtimeId', showtimeId ?? String(showtime.id))
    sessionStorage.setItem('movieId', String(movie.id))
    sessionStorage.setItem('cinemaId', String(cinema.id))
    sessionStorage.setItem('bookingTotal', String(total))
    navigate('/booking/confirm')
  }

  if (loading) {
    return (
      <div className="container">
        <div className="loading-screen">
          <span className="spinner" />
          Đang tải sơ đồ ghế...
        </div>
      </div>
    )
  }

  if (!showtime || error) {
    return (
      <div className="container">
        <PageBar title="Chọn ghế" backTo="/showtimes" />
        <div className="empty-state">
          <Icon name="seat" size={40} />
          <strong>Không tải được suất chiếu</strong>
          <p>{error || 'Suất chiếu có thể đã bị hủy hoặc không tồn tại.'}</p>
          <button type="button" className="btn btn-primary btn-sm" onClick={() => setReloadKey((v) => v + 1)}>
            Thử lại
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="container seat-page">
      <PageBar
        title="Chọn ghế"
        backTo={`/showtimes?movieId=${showtime.movieId}`}
        actions={
          <span className="badge badge-muted">
            {showtime.time} · {showtime.hall}
          </span>
        }
      />

      <div className="seat-layout">
        <div className="surface card-pad">
          <div className="movie-mini" style={{ marginBottom: 16 }}>
            <img src={movie.posterUrl} alt={movie.title} />
            <div>
              <span className="eyebrow">Đang chiếu</span>
              <div className="movie-mini__title">{showtime.movieTitle}</div>
              <div className="meta-row" style={{ marginTop: 5 }}>
                <span>{cinema.name}</span>
                <span className="meta-dot" />
                <span>{showtime.hall}</span>
              </div>
              <div className="meta-row" style={{ marginTop: 3 }}>
                <Icon name="clock" size={12} />
                {showtime.time} · {formatDuration(movie.durationMinutes)}
              </div>
            </div>
          </div>

          <div className="screen-wrap">
            <div className="screen-curve" />
            <span className="screen-label">Màn hình</span>
          </div>

          <div className="seat-map" aria-label="Sơ đồ ghế">
            {['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'].map((row) => {
              const rowSeats = seats.filter((seat) => seat.row === row)
              if (row === 'H') {
                const pairs = []
                for (let index = 0; index < rowSeats.length; index += 2) {
                  const first = rowSeats[index]
                  const second = rowSeats[index + 1]
                  if (!first || !second) continue
                  const id = `${first.id}-${second.id}`
                  const disabled = first.status !== 'available' || second.status !== 'available'
                  pairs.push(
                    <button
                      key={seat.id}
                      type="button"
                      title={`${seat.seatCode} · ${formatVnd(Number(seat.price))}`}
                      className={`seat ${isVip ? 'is-vip' : ''} ${isCouple ? 'is-couple' : ''} ${
                        disabled ? 'is-booked' : ''
                      } ${isSelected ? 'is-selected' : ''}`}
                      disabled={disabled}
                      onClick={() => toggleSeat(seat)}
                      aria-label={`Ghế ${seat.seatCode}`}
                    >
                      {first.number}-{second.number}
                    </button>,
                  )
                })}
              </div>
            ))}
          </div>

          <div className="seat-legend">
            <span className="seat-legend__item">
              <span className="seat-legend__swatch" /> Thường
            </span>
            <span className="seat-legend__item">
              <span className="seat-legend__swatch is-vip" /> VIP
            </span>
            <span className="seat-legend__item">
              <span className="seat-legend__swatch is-selected" /> Đang chọn
            </span>
            <span className="seat-legend__item">
              <span className="seat-legend__swatch is-booked" /> Đã đặt
            </span>
          </div>

          <button
            type="button"
            className="btn-link"
            style={{ marginTop: 12 }}
            onClick={() => setReloadKey((value) => value + 1)}
          >
            Làm mới sơ đồ ghế
          </button>
        </div>

        <aside>
          <div className="seat-summary surface card-pad">
            <div className="seat-summary__row">
              <div>
                <div className="seat-summary__label">Ghế đã chọn</div>
                <div className="seat-summary__seats">
                  {selectedLabels.length > 0 ? selectedLabels.join(', ') : 'Chưa chọn ghế'}
                </div>
              </div>
              <span className="badge badge-accent">{selected.length}/8</span>
            </div>

            <div className="summary-line">
              <span>Suất chiếu</span>
              <strong>{showtime.time}</strong>
            </div>
            <div className="summary-line">
              <span>Rạp</span>
              <strong>{showtime.cinemaName}</strong>
            </div>
            <div className="summary-line">
              <span>Phòng</span>
              <strong>{showtime.hall}</strong>
            </div>
            <div className="summary-line is-total">
              <span>Tạm tính</span>
              <strong>{formatVnd(total)}</strong>
            </div>

            <button
              type="button"
              className="btn btn-gradient btn-block btn-lg"
              disabled={selected.length === 0}
              onClick={handleContinue}
              style={{ marginTop: 14 }}
            >
              Tiếp tục
              <Icon name="arrow-right" size={17} />
            </button>
            <p className="form-hint" style={{ marginTop: 10, textAlign: 'center' }}>
              Ghế được giữ trong 10 phút sau khi xác nhận.
            </p>
          </div>
        </aside>
      </div>

      <div className="seat-mobile-bar">
        <div className="seat-mobile-bar__info">
          <div className="seat-summary__label">Tạm tính</div>
          <div className="seat-summary__value">{formatVnd(total)}</div>
          <div className="seat-summary__seats">
            {selectedLabels.length > 0 ? selectedLabels.join(', ') : 'Chưa chọn ghế'}
          </div>
        </div>
        <button
          type="button"
          className="btn btn-gradient"
          disabled={selected.length === 0}
          onClick={handleContinue}
        >
          Tiếp tục
          <Icon name="arrow-right" size={17} />
        </button>
      </div>
    </div>
  )
}
