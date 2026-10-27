import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useMovies } from '../hooks/useMovies'
import { cinemas, genres, quickGenres } from '../data/mockData'
import { EmptyState } from '../components/ui/EmptyState'
import { Icon } from '../components/ui/Icon'
import { MovieCard } from '../components/ui/MovieCard'
import { SectionHeader } from '../components/ui/SectionHeader'

export function Search() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [query, setQuery] = useState(searchParams.get('q') ?? '')
  const [genre, setGenre] = useState('')
  const [status, setStatus] = useState<'' | 'NOW_SHOWING' | 'COMING_SOON'>('')
  const { movies, loading } = useMovies(status || undefined)

  useEffect(() => {
    setQuery(searchParams.get('q') ?? '')
  }, [searchParams])

  const results = useMemo(() => {
    return movies.filter((movie) => {
      const matchesQuery = !query || movie.title.toLowerCase().includes(query.toLowerCase())
      const matchesGenre = !genre || movie.genre.toLowerCase().includes(genre.toLowerCase())
      return matchesQuery && matchesGenre
    })
  }, [movies, query, genre])

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSearchParams(query ? { q: query } : {})
  }

  return (
    <div className="container">
      <div className="page-bar">
        <div>
          <span className="eyebrow">Discover</span>
          <h1 className="page-bar__title" style={{ fontSize: 30 }}>
            Tìm kiếm
          </h1>
        </div>
        <Link to="/ai-chat" className="header-icon" aria-label="Trợ lý AI">
          <Icon name="sparkles" size={19} />
        </Link>
      </div>

      <form className="search-bar" onSubmit={submit} role="search">
        <Icon name="search" size={18} />
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Tìm phim, thể loại, rạp..."
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
              {cinemas.map((cinema) => (
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
              {results.length} kết quả
            </span>
          </div>

          {loading ? (
            <div className="movie-grid">
              {Array.from({ length: 6 }).map((_, index) => (
                <div key={index} className="skeleton" style={{ aspectRatio: '2 / 3', borderRadius: 14 }} />
              ))}
            </div>
          ) : results.length === 0 ? (
            <EmptyState
              icon="search"
              title="Không tìm thấy kết quả"
              description="Thử từ khóa khác hoặc bỏ bớt bộ lọc."
            />
          ) : (
            <div className="movie-grid">
              {results.map((movie) => (
                <MovieCard key={movie.id} movie={movie} />
              ))}
            </div>
          )}

          <div className="section">
            <SectionHeader title="Có thể bạn thích" action="Xem tất cả" actionTo="/movies" />
            <div className="h-scroll">
              {movies.slice(2, 8).map((movie) => (
                <MovieCard key={movie.id} movie={movie} showMatch={false} />
              ))}
            </div>
          </div>
        </section>
      </div>
    </div>
  )
}
