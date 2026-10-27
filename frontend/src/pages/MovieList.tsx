import { useMemo, useState } from 'react'
import { useMovies } from '../hooks/useMovies'
import { genres } from '../data/mockData'
import { EmptyState } from '../components/ui/EmptyState'
import { Icon } from '../components/ui/Icon'
import { MovieCard } from '../components/ui/MovieCard'

const statuses = [
  { value: '', label: 'Tất cả' },
  { value: 'NOW_SHOWING', label: 'Đang chiếu' },
  { value: 'COMING_SOON', label: 'Sắp chiếu' },
] as const

export function MovieList() {
  const [status, setStatus] = useState<'' | 'NOW_SHOWING' | 'COMING_SOON'>('')
  const [genre, setGenre] = useState('')
  const [query, setQuery] = useState('')
  const { movies, loading } = useMovies(status || undefined)

  const filtered = useMemo(() => {
    return movies.filter((movie) => {
      const matchesGenre = !genre || movie.genre.toLowerCase().includes(genre.toLowerCase())
      const matchesQuery = !query || movie.title.toLowerCase().includes(query.toLowerCase())
      return matchesGenre && matchesQuery
    })
  }, [movies, genre, query])

  return (
    <div className="container">
      <div className="page-bar">
        <div>
          <span className="eyebrow">Cinex Library</span>
          <h1 className="page-bar__title" style={{ fontSize: 30 }}>
            Khám phá phim
          </h1>
        </div>
        <span className="badge badge-muted">{filtered.length} phim</span>
      </div>

      <div className="search-bar" style={{ marginBottom: 14 }}>
        <Icon name="search" size={18} />
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Tìm phim theo tên..."
          aria-label="Tìm phim"
        />
        {query && (
          <button type="button" className="input-action" onClick={() => setQuery('')} aria-label="Xóa tìm kiếm">
            <Icon name="close" size={16} />
          </button>
        )}
      </div>

      <div className="filter-bar">
        {statuses.map((item) => (
          <button
            key={item.value}
            type="button"
            className={`chip ${status === item.value ? 'is-active' : ''}`}
            onClick={() => setStatus(item.value)}
          >
            {item.label}
          </button>
        ))}
        <span className="meta-dot" />
        {genres.slice(0, 4).map((item) => (
          <button
            key={item}
            type="button"
            className={`chip chip-outline ${genre === item ? 'is-active' : ''}`}
            onClick={() => setGenre(genre === item ? '' : item)}
          >
            {item}
          </button>
        ))}
      </div>

      <div className="movie-list-layout section-tight">
        <aside className="filter-sidebar hide-mobile">
          <div className="surface card-pad">
            <h3 className="h3" style={{ marginBottom: 12 }}>
              Trạng thái
            </h3>
            <div className="stack gap-8">
              {statuses.map((item) => (
                <button
                  key={item.value}
                  type="button"
                  className={`chip ${status === item.value ? 'is-active' : ''}`}
                  style={{ justifyContent: 'flex-start' }}
                  onClick={() => setStatus(item.value)}
                >
                  {item.label}
                </button>
              ))}
            </div>
          </div>

          <div className="surface card-pad">
            <h3 className="h3" style={{ marginBottom: 12 }}>
              Thể loại
            </h3>
            <div className="stack gap-8">
              <button
                type="button"
                className={`chip ${!genre ? 'is-active' : ''}`}
                style={{ justifyContent: 'flex-start' }}
                onClick={() => setGenre('')}
              >
                Tất cả thể loại
              </button>
              {genres.map((item) => (
                <button
                  key={item}
                  type="button"
                  className={`chip ${genre === item ? 'is-active' : ''}`}
                  style={{ justifyContent: 'flex-start' }}
                  onClick={() => setGenre(item)}
                >
                  {item}
                </button>
              ))}
            </div>
          </div>
        </aside>

        <section>
          {loading ? (
            <div className="movie-grid">
              {Array.from({ length: 8 }).map((_, index) => (
                <div key={index} className="skeleton" style={{ aspectRatio: '2 / 3', borderRadius: 14 }} />
              ))}
            </div>
          ) : filtered.length === 0 ? (
            <EmptyState
              icon="search"
              title="Không tìm thấy phim phù hợp"
              description="Thử đổi từ khóa hoặc bộ lọc thể loại."
            />
          ) : (
            <div className="movie-grid">
              {filtered.map((movie) => (
                <MovieCard key={movie.id} movie={movie} />
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  )
}
