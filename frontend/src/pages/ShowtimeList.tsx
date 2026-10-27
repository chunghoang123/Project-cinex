import { useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useMovies } from '../hooks/useMovies'
import { cinemas, showtimesFor } from '../data/mockData'
import { buildDates, formatDuration, formatVnd, todayLabel } from '../utils/format'
import { EmptyState } from '../components/ui/EmptyState'
import { Icon } from '../components/ui/Icon'
import { PageBar } from '../components/ui/PageBar'

export function ShowtimeList() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { movies } = useMovies()
  const dates = useMemo(() => buildDates(7), [])
  const [dateIndex, setDateIndex] = useState(0)
  const [cinemaId, setCinemaId] = useState<number | null>(null)

  const movie = movies.find((item) => String(item.id) === searchParams.get('movieId')) ?? movies[0]

  if (!movie) {
    return (
      <div className="container">
        <EmptyState icon="calendar" title="Chưa có lịch chiếu" description="Vui lòng chọn phim để xem suất chiếu." />
      </div>
    )
  }

  const slots = showtimesFor(movie.id)
  const visibleCinemas = cinemaId ? cinemas.filter((cinema) => cinema.id === cinemaId) : cinemas

  return (
    <div className="container">
      <PageBar
        title="Lịch chiếu"
        backTo={`/movies/${movie.id}`}
        actions={
          <Link to={`/movies/${movie.id}`} className="header-icon" aria-label="Thông tin phim">
            <Icon name="info" size={19} />
          </Link>
        }
      />

      <div className="showtime-layout">
        <aside>
          <div className="showtime-summary">
            <img src={movie.posterUrl} alt={movie.title} />
            <div>
              <span className="eyebrow">Đang chiếu</span>
              <h2 className="h3" style={{ margin: '4px 0 6px' }}>
                {movie.title}
              </h2>
              <div className="meta-row">
                <span>{movie.genre}</span>
                <span className="meta-dot" />
                <span>{formatDuration(movie.durationMinutes)}</span>
              </div>
              <div className="pill-row" style={{ marginTop: 8 }}>
                {movie.formats.map((format) => (
                  <span key={format} className="badge badge-muted">
                    {format}
                  </span>
                ))}
              </div>
              <div className="row gap-6" style={{ marginTop: 12, color: 'var(--muted)', fontSize: 12 }}>
                <Icon name="calendar" size={14} />
                {todayLabel()} · {dates[dateIndex].label}
              </div>
            </div>
          </div>
        </aside>

        <section>
          <div className="row between" style={{ marginBottom: 10 }}>
            <h2 className="h3">Chọn ngày</h2>
            <span className="muted" style={{ fontSize: 12 }}>
              {dates[dateIndex].weekday}, {dates[dateIndex].day}/{dates[dateIndex].month}
            </span>
          </div>
          <div className="date-tabs">
            {dates.map((date, index) => (
              <button
                key={date.key}
                type="button"
                className={`date-tab ${index === dateIndex ? 'is-active' : ''}`}
                onClick={() => setDateIndex(index)}
              >
                <span>{date.weekday}</span>
                <strong>{date.day}</strong>
              </button>
            ))}
          </div>

          <div className="row between" style={{ margin: '20px 0 10px' }}>
            <h2 className="h3">Chọn rạp</h2>
            <button type="button" className="btn-link" onClick={() => setCinemaId(null)}>
              Tất cả rạp
            </button>
          </div>

          <div className="filter-bar" style={{ marginBottom: 12 }}>
            {cinemas.map((cinema) => (
              <button
                key={cinema.id}
                type="button"
                className={`chip ${cinemaId === cinema.id ? 'is-active' : ''}`}
                onClick={() => setCinemaId(cinemaId === cinema.id ? null : cinema.id)}
              >
                {cinema.name}
              </button>
            ))}
          </div>

          {visibleCinemas.map((cinema, cinemaIndex) => (
            <div key={cinema.id} className="cinema-block">
              <div className="cinema-block__head">
                <div>
                  <div className="cinema-block__name">{cinema.name}</div>
                  <div className="cinema-block__addr">
                    {cinema.address} · {cinema.distance}
                  </div>
                </div>
                <span className="badge badge-success">Còn vé</span>
              </div>

              <div className="showtime-slots">
                {slots.map((slot, slotIndex) => {
                  const active = cinemaIndex === 0 && slotIndex === 0
                  return (
                    <button
                      key={`${cinema.id}-${slot.id}`}
                      type="button"
                      className={`slot ${active ? 'is-active' : ''}`}
                      onClick={() => navigate(`/seats/${slot.id}?movieId=${movie.id}&cinemaId=${cinema.id}`)}
                    >
                      <strong>{slot.time}</strong>
                      <span>{formatVnd(slot.price)}</span>
                    </button>
                  )
                })}
              </div>
            </div>
          ))}
        </section>
      </div>
    </div>
  )
}
