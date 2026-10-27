import { useMemo, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { buildSeatMap, cinemas, movies, showtimesFor } from '../data/mockData'
import { formatDuration, formatVnd } from '../utils/format'
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
  const [selected, setSelected] = useState<string[]>([])

  const movie = movies.find((item) => String(item.id) === searchParams.get('movieId')) ?? movies[0]
  const cinema = cinemas.find((item) => String(item.id) === searchParams.get('cinemaId')) ?? cinemas[0]
  const showtime = showtimesFor(movie.id).find((item) => String(item.id) === showtimeId) ?? showtimesFor(movie.id)[0]
  const seats = useMemo(() => buildSeatMap(), [])

  const selectedLabels = selected.map((id) => id.replace('-', ' - '))

  const total = selected.reduce((sum, id) => {
    if (id.startsWith('H')) return sum + seatPrice.couple
    if (id.startsWith('E') || id.startsWith('F')) return sum + seatPrice.vip
    return sum + seatPrice.standard
  }, 0)

  const toggleSeat = (id: string) => {
    setSelected((current) => {
      if (current.includes(id)) return current.filter((seat) => seat !== id)
      if (current.length >= 8) return current
      return [...current, id]
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

  return (
    <div className="container seat-page">
      <PageBar
        title="Chọn ghế"
        backTo={`/showtimes?movieId=${movie.id}`}
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
              <div className="movie-mini__title">{movie.title}</div>
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
                      key={id}
                      type="button"
                      className={`seat is-couple ${selected.includes(id) ? 'is-selected' : ''} ${disabled ? 'is-booked' : ''}`}
                      disabled={disabled}
                      onClick={() => toggleSeat(id)}
                      aria-label={`Ghế đôi ${first.number}-${second.number}`}
                    >
                      {first.number}-{second.number}
                    </button>,
                  )
                }
                return (
                  <div key={row} className="seat-row">
                    <span className="seat-row__label">{row}</span>
                    {pairs}
                  </div>
                )
              }
              return (
                <div key={row} className="seat-row">
                  <span className="seat-row__label">{row}</span>
                  {rowSeats.map((seat) => {
                    const isSelected = selected.includes(seat.id)
                    const typeClass = seat.type === 'vip' ? 'is-vip' : ''
                    const statusClass = seat.status === 'booked' ? 'is-booked' : seat.status === 'held' ? 'is-held' : ''
                    return (
                      <button
                        key={seat.id}
                        type="button"
                        className={`seat ${typeClass} ${statusClass} ${isSelected ? 'is-selected' : ''}`}
                        disabled={seat.status !== 'available'}
                        onClick={() => toggleSeat(seat.id)}
                        aria-label={`Ghế ${seat.id}`}
                      >
                        {seat.number}
                      </button>
                    )
                  })}
                </div>
              )
            })}
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
              <strong>{cinema.name}</strong>
            </div>
            <div className="summary-line">
              <span>Phòng</span>
              <strong>{showtime.hall}</strong>
            </div>
            <div className="summary-line is-total">
              <span>Tạm tính</span>
              <strong>{formatVnd(total)}</strong>
            </div>

            <button type="button" className="btn btn-gradient btn-block btn-lg" disabled={selected.length === 0} onClick={handleContinue} style={{ marginTop: 14 }}>
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
