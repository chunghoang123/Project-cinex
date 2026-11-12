import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useMovies } from '../hooks/useMovies'
import { useSearchData } from '../hooks/useSearchData'
import { cinemas, genres, quickGenres } from '../data/mockData'
import { EmptyState } from '../components/ui/EmptyState'
import { Icon } from '../components/ui/Icon'
import { MovieCard } from '../components/ui/MovieCard'
import { SectionHeader } from '../components/ui/SectionHeader'
import { openChat } from '../components/chat/ChatWidget'
import { formatTime } from '../utils/consoleFormat'
import { formatVnd, localDateKey } from '../utils/format'
import { includesText, normalizeText } from '../utils/searchText'
import type { Cinema, Showtime } from '../types/Admin'

const MAX_SHOWTIME_RESULTS = 12

function dayKey(offsetDays = 0): string {
  const date = new Date()
  date.setDate(date.getDate() + offsetDays)
  return localDateKey(date)
}

function showtimeDateLabel(startTime: string): string {
  const date = new Date(startTime)
  return new Intl.DateTimeFormat('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit' }).format(date)
}

/** Từ khoá thời gian đặc biệt: hôm nay, ngày mai, tối nay. */
function timeKeyword(query: string): { today: boolean; tomorrow: boolean; evening: boolean } {
  return {
    today: query.includes('hom nay'),
    tomorrow: query.includes('ngay mai'),
    evening: query.includes('toi nay') || query.includes('buoi toi'),
  }
}

function matchesShowtime(item: Showtime, query: string): boolean {
  if (!query) return true
  const keyword = timeKeyword(query)
  const cleaned = query
    .replace(/hom nay|ngay mai|toi nay|buoi toi/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()

  const key = localDateKey(item.startTime)
  const hour = Number(item.startTime.slice(11, 13))
  if (keyword.today && key !== dayKey(0)) return false
  if (keyword.tomorrow && key !== dayKey(1)) return false
  if (keyword.evening && !(key === dayKey(0) && hour >= 17)) return false

  if (!cleaned) return true

  const textFields = [
    item.movieTitle,
    item.cinemaName,
    item.auditoriumName,
    showtimeDateLabel(item.startTime),
    item.startTime.slice(0, 10).split('-').reverse().join('/'),
    item.startTime.slice(0, 10).split('-').reverse().slice(0, 2).join('/'),
    formatTime(item.startTime),
    formatTime(item.startTime).replace(':', 'h'),
  ]

  return textFields.some((field) => includesText(field, cleaned))
}

function matchesCinema(cinema: Cinema, query: string): boolean {
  if (!query) return true
  return [cinema.name, cinema.address, cinema.phone].some((field) => includesText(field, query))
}

export function Search() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [query, setQuery] = useState(searchParams.get('q') ?? '')
  const [genre, setGenre] = useState('')
  const [status, setStatus] = useState<'' | 'NOW_SHOWING' | 'COMING_SOON'>('')
  const { movies, loading } = useMovies(status || undefined)
  const { cinemas: apiCinemas, showtimes, loading: dataLoading } = useSearchData()

  useEffect(() => {
    setQuery(searchParams.get('q') ?? '')
  }, [searchParams])

  const normalizedQuery = useMemo(() => normalizeText(query), [query])

  const movieResults = useMemo(() => {
    return movies.filter((movie) => {
      const matchesQuery =
        !normalizedQuery ||
        includesText(movie.title, normalizedQuery) ||
        includesText(movie.genre, normalizedQuery) ||
        includesText(movie.description, normalizedQuery)
      const matchesGenre = !genre || movie.genre.toLowerCase().includes(genre.toLowerCase())
      return matchesQuery && matchesGenre
    })
  }, [movies, normalizedQuery, genre])

  const cinemaResults = useMemo(
    () => apiCinemas.filter((cinema) => matchesCinema(cinema, normalizedQuery)),
    [apiCinemas, normalizedQuery],
  )

  const showtimeResults = useMemo(() => {
    const matched = showtimes.filter((item) => matchesShowtime(item, normalizedQuery))
    return matched.slice(0, MAX_SHOWTIME_RESULTS)
  }, [showtimes, normalizedQuery])

  const showtimeCountByCinema = useMemo(() => {
    const counts = new Map<number, number>()
    showtimes.forEach((item) => counts.set(item.cinemaId, (counts.get(item.cinemaId) ?? 0) + 1))
    return counts
  }, [showtimes])

  const totalResults = movieResults.length + cinemaResults.length + showtimeResults.length

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSearchParams(query ? { q: query } : {})
  }

  const nothingFound = !loading && !dataLoading && totalResults === 0

  return (
    <div className="container">
      <div className="page-bar">
        <div>
          <span className="eyebrow">Discover</span>
          <h1 className="page-bar__title" style={{ fontSize: 30 }}>
            Tìm kiếm
          </h1>
        </div>
        <button type="button" className="header-icon" aria-label="Trợ lý AI" onClick={openChat}>
          <Icon name="sparkles" size={19} />
        </button>
      </div>

      <form className="search-bar" onSubmit={submit} role="search">
        <Icon name="search" size={18} />
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Tìm phim, rạp, suất chiếu..."
          aria-label="Tìm kiếm"
        />
        {query && (
          <button type="button" className="input-action" onClick={() => setQuery('')} aria-label="Xóa">
            <Icon name="close" size={16} />
          </button>
        )}
      </form>

      <div className="filter-bar" style={{ marginTop: 12 }}>
        {quickGenres.map((item, index) => (
          <button
            key={item}
            type="button"
            className={`chip ${index === 0 && !genre ? 'is-active' : genre === item ? 'is-active' : ''}`}
            onClick={() => setGenre(genre === item || index === 0 ? '' : item)}
          >
            {item}
          </button>
        ))}
      </div>

      <div className="search-layout section-tight">
        <aside className="search-sidebar hide-mobile">
          <div className="surface card-pad">
            <h3 className="h3" style={{ marginBottom: 12 }}>
              Trạng thái
            </h3>
            <div className="stack gap-8">
              <button
                type="button"
                className={`chip ${!status ? 'is-active' : ''}`}
                style={{ justifyContent: 'flex-start' }}
                onClick={() => setStatus('')}
              >
                Tất cả
              </button>
              <button
                type="button"
                className={`chip ${status === 'NOW_SHOWING' ? 'is-active' : ''}`}
                style={{ justifyContent: 'flex-start' }}
                onClick={() => setStatus('NOW_SHOWING')}
              >
                Đang chiếu
              </button>
              <button
                type="button"
                className={`chip ${status === 'COMING_SOON' ? 'is-active' : ''}`}
                style={{ justifyContent: 'flex-start' }}
                onClick={() => setStatus('COMING_SOON')}
              >
                Sắp chiếu
              </button>
            </div>
          </div>

          <div className="surface card-pad">
            <h3 className="h3" style={{ marginBottom: 12 }}>
              Thể loại
            </h3>
            <div className="stack gap-8">
              {genres.map((item) => (
                <button
                  key={item}
                  type="button"
                  className={`chip ${genre === item ? 'is-active' : ''}`}
                  style={{ justifyContent: 'flex-start' }}
                  onClick={() => setGenre(genre === item ? '' : item)}
                >
                  {item}
                </button>
              ))}
            </div>
          </div>

          <div className="surface card-pad">
            <h3 className="h3" style={{ marginBottom: 12 }}>
              Rạp gần bạn
            </h3>
            <div className="stack gap-10">
              {apiCinemas.length > 0
                ? apiCinemas.map((cinema) => (
                    <Link
                      key={cinema.id}
                      to={`/showtimes?cinemaId=${cinema.id}`}
                      className="row gap-8"
                      style={{ alignItems: 'flex-start' }}
                    >
                      <Icon name="location" size={15} />
                      <div>
                        <div style={{ fontSize: 12, fontWeight: 600 }}>{cinema.name}</div>
                        <div className="muted" style={{ fontSize: 10 }}>
                          {cinema.address}
                        </div>
                      </div>
                    </Link>
                  ))
                : cinemas.map((cinema) => (
                    <div key={cinema.id} className="row gap-8" style={{ alignItems: 'flex-start' }}>
                      <Icon name="location" size={15} />
                      <div>
                        <div style={{ fontSize: 12, fontWeight: 600 }}>{cinema.name}</div>
                        <div className="muted" style={{ fontSize: 10 }}>
                          {cinema.distance} · {cinema.address}
                        </div>
                      </div>
                    </div>
                  ))}
            </div>
          </div>
        </aside>

        <section>
          <div className="row between" style={{ marginBottom: 12 }}>
            <h2 className="h3">{query ? `Kết quả cho "${query}"` : 'Phim nổi bật'}</h2>
            <span className="muted" style={{ fontSize: 12 }}>
              {totalResults} kết quả
            </span>
          </div>

          {nothingFound ? (
            <EmptyState
              icon="search"
              title="Không tìm thấy kết quả"
              description="Thử từ khóa khác hoặc bỏ bớt bộ lọc."
            />
          ) : (
            <>
              {movieResults.length > 0 && (
                <div className="search-group">
                  <div className="search-group__head">
                    <h3 className="h3">
                      <Icon name="film" size={16} /> Phim
                    </h3>
                    <span className="muted" style={{ fontSize: 12 }}>
                      {movieResults.length} phim
                    </span>
                  </div>
                  {loading ? (
                    <div className="movie-grid">
                      {Array.from({ length: 6 }).map((_, index) => (
                        <div key={index} className="skeleton" style={{ aspectRatio: '2 / 3', borderRadius: 14 }} />
                      ))}
                    </div>
                  ) : (
                    <div className="movie-grid">
                      {movieResults.map((movie) => (
                        <MovieCard key={movie.id} movie={movie} />
                      ))}
                    </div>
                  )}
                </div>
              )}

              {cinemaResults.length > 0 && (
                <div className="search-group">
                  <div className="search-group__head">
                    <h3 className="h3">
                      <Icon name="location" size={16} /> Rạp
                    </h3>
                    <span className="muted" style={{ fontSize: 12 }}>
                      {cinemaResults.length} rạp
                    </span>
                  </div>
                  <div className="search-list">
                    {cinemaResults.map((cinema) => (
                      <Link key={cinema.id} to={`/showtimes?cinemaId=${cinema.id}`} className="search-item">
                        <span className="search-item__icon">
                          <Icon name="building" size={18} />
                        </span>
                        <div className="search-item__body">
                          <strong>{cinema.name}</strong>
                          <span>{cinema.address}</span>
                          <span className="muted" style={{ fontSize: 11 }}>
                            {cinema.phone || '—'} · {showtimeCountByCinema.get(cinema.id) ?? 0} suất chiếu
                          </span>
                        </div>
                        <Icon name="chevron-right" size={16} className="menu-item__chevron" />
                      </Link>
                    ))}
                  </div>
                </div>
              )}

              {showtimeResults.length > 0 && (
                <div className="search-group">
                  <div className="search-group__head">
                    <h3 className="h3">
                      <Icon name="clock" size={16} /> Suất chiếu
                    </h3>
                    <span className="muted" style={{ fontSize: 12 }}>
                      {showtimeResults.length} suất
                    </span>
                  </div>
                  <div className="search-list">
                    {showtimeResults.map((item) => (
                      <Link
                        key={item.id}
                        to={`/seats/${item.id}?movieId=${item.movieId}`}
                        className="search-item search-item--showtime"
                      >
                        {item.posterUrl && <img className="search-item__poster" src={item.posterUrl} alt={item.movieTitle} loading="lazy" />}
                        <div className="search-item__body">
                          <strong>
                            {formatTime(item.startTime)} · {showtimeDateLabel(item.startTime)}
                          </strong>
                          <span>{item.movieTitle}</span>
                          <span className="muted" style={{ fontSize: 11 }}>
                            {item.cinemaName} · {item.auditoriumName} · {formatVnd(Number(item.basePrice))}
                          </span>
                        </div>
                        <span className="badge badge-success">Còn {item.availableSeats} ghế</span>
                      </Link>
                    ))}
                  </div>
                  {showtimes.length > showtimeResults.length && (
                    <div className="search-more">
                      <Link to="/showtimes" className="btn btn-ghost btn-sm">
                        <Icon name="calendar" size={15} /> Xem toàn bộ lịch chiếu
                      </Link>
                    </div>
                  )}
                </div>
              )}
            </>
          )}

          {!query && (
            <div className="section">
              <SectionHeader title="Có thể bạn thích" action="Xem tất cả" actionTo="/movies" />
              <div className="h-scroll">
                {movies.slice(2, 8).map((movie) => (
                  <MovieCard key={movie.id} movie={movie} showMatch={false} />
                ))}
              </div>
            </div>
          )}
        </section>
      </div>
    </div>
  )
}
