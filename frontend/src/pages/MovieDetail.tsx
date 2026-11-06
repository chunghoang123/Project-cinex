import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMovie, useMovies } from '../hooks/useMovies'
import { useShowtimes } from '../hooks/useShowtimes'
import { formatDuration, formatVnd } from '../utils/format'
import { formatTime } from '../utils/consoleFormat'
import { EmptyState } from '../components/ui/EmptyState'
import { Icon } from '../components/ui/Icon'
import { MovieCard } from '../components/ui/MovieCard'
import { SectionHeader } from '../components/ui/SectionHeader'

export function MovieDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { movie, loading } = useMovie(id)
  const { movies } = useMovies()
  const { showtimes } = useShowtimes(movie?.id)
  const [expanded, setExpanded] = useState(false)
  const [trailerOpen, setTrailerOpen] = useState(false)
  const [trailerLoaded, setTrailerLoaded] = useState(false)
  const [trailerTimedOut, setTrailerTimedOut] = useState(false)

  useEffect(() => {
    if (!trailerOpen) {
      setTrailerLoaded(false)
      setTrailerTimedOut(false)
      return
    }
    const timer = window.setTimeout(() => {
      if (!trailerLoaded) setTrailerTimedOut(true)
    }, 3000)
    return () => window.clearTimeout(timer)
  }, [trailerOpen, trailerLoaded])

  if (loading) {
    return (
      <div className="container">
        <div className="loading-screen">
          <span className="spinner" />
          Đang tải thông tin phim...
        </div>
      </div>
    )
  }

  if (!movie) {
    return (
      <div className="container">
        <EmptyState
          icon="film"
          title="Không tìm thấy phim"
          description="Phim có thể đã ngừng chiếu hoặc đường dẫn không đúng."
          action={
            <Link to="/movies" className="btn btn-primary btn-sm">
              Về danh sách phim
            </Link>
          }
        />
      </div>
    )
  }

  const slots = showtimes.slice(0, 4)
  const related = movies.filter((item) => item.id !== movie.id).slice(0, 5)
  const trailerEmbedUrl = toYouTubeEmbed(movie.trailerUrl)
  const trailerAvailable = Boolean(trailerEmbedUrl)
  const trailerWatchUrl = trailerEmbedUrl
    ? trailerEmbedUrl.replace('/embed/', '/watch?v=').replace('?rel=0', '')
    : ''

  function toYouTubeEmbed(url: string): string {
    if (!url) return ''
    const watch = url.match(/[?&]v=([a-zA-Z0-9_-]{11})/)
    if (watch) return `https://www.youtube.com/embed/${watch[1]}?rel=0`
    const short = url.match(/youtu\.be\/([a-zA-Z0-9_-]{11})/)
    if (short) return `https://www.youtube.com/embed/${short[1]}?rel=0`
    const embed = url.match(/youtube\.com\/embed\/([a-zA-Z0-9_-]{11})/)
    if (embed) return `https://www.youtube.com/embed/${embed[1]}?rel=0`
    const bare = url.match(/^(https?:\/\/)?(www\.)?youtube\.com\/?$/)
    if (bare) return ''
    return url
  }

  const openTrailer = () => {
    if (!trailerAvailable) return
    setTrailerOpen(true)
  }

  return (
    <div className="container">
      <section className="detail-hero">
        <img className="detail-hero__bg" src={movie.backdropUrl} alt="" />
        <div className="detail-hero__shade" />
        <button
          type="button"
          className="page-bar__back"
          style={{ position: 'absolute', zIndex: 3, top: 16, left: 18 }}
          aria-label="Quay lại"
          onClick={() => navigate(-1)}
        >
          <Icon name="chevron-left" size={20} />
        </button>
        <div className="detail-hero__inner">
          <img className="detail-hero__poster" src={movie.posterUrl} alt={movie.title} />
          <div>
            <div className="home-hero__badges">
              <span className="badge badge-accent">{movie.match}% AI Match</span>
              <span className="badge badge-warning">
                <Icon name="star" size={10} filled /> {movie.rating.toFixed(1)}
              </span>
              <span className="badge badge-primary">{movie.ageRating}+</span>
            </div>
            <h1 className="detail-title">{movie.title}</h1>
            <div className="detail-meta">
              <span>{movie.genre}</span>
              <span className="meta-dot" />
              <span>{formatDuration(movie.durationMinutes)}</span>
              <span className="meta-dot" />
              <span>{movie.language}</span>
            </div>
            <div className="pill-row">
              {movie.formats.map((format) => (
                <span key={format} className="badge badge-muted">
                  {format}
                </span>
              ))}
            </div>
            <div className="detail-actions">
              <Link to={`/showtimes?movieId=${movie.id}`} className="btn btn-accent">
                <Icon name="ticket" size={17} />
                Đặt vé
              </Link>
              <button type="button" className="btn btn-ghost" onClick={openTrailer} disabled={!trailerAvailable} title={trailerAvailable ? 'Xem trailer' : 'Trailer chưa được cập nhật'}>
                <Icon name="play" size={16} />
                {trailerAvailable ? 'Xem trailer' : 'Trailer sắp có'}
              </button>
            </div>
          </div>
        </div>
      </section>

      <div className="detail-body section">
        <div>
          <section className="detail-section">
            <h2>Nội dung phim</h2>
            <p className={`detail-desc ${expanded ? '' : 'is-clamped'}`}>{movie.description}</p>
            <button type="button" className="btn-link" style={{ marginTop: 8 }} onClick={() => setExpanded((value) => !value)}>
              {expanded ? 'Thu gọn' : 'Đọc thêm'}
            </button>
          </section>

          <section className="detail-section">
            <h2>Official Trailer</h2>
            {trailerAvailable ? (
              <button type="button" className="trailer-frame" onClick={openTrailer} aria-label="Xem trailer">
                <img src={movie.backdropUrl} alt="" />
                <span className="trailer-frame__play">
                  <Icon name="play" size={22} filled />
                </span>
              </button>
            ) : (
              <div className="empty-state" style={{ padding: '20px 16px' }}>
                <Icon name="film" size={28} />
                <p className="muted" style={{ marginTop: 8 }}>
                  Trailer của phim này đang được cập nhật. Hãy quay lại sau.
                </p>
              </div>
            )}
          </section>

          <section className="detail-section">
            <h2>Diễn viên</h2>
            <div className="cast-list">
              {movie.cast.map((member) => (
                <div key={member.name} className="cast-item">
                  <img src={member.avatar} alt={member.name} loading="lazy" />
                  <span>{member.name}</span>
                  <small>{member.role}</small>
                </div>
              ))}
            </div>
          </section>

          <section className="detail-section">
            <h2>Thông tin</h2>
            <div className="detail-info-grid">
              <div className="detail-info-item">
                <span>Ngày khởi chiếu</span>
                <strong>{movie.releaseDate}</strong>
              </div>
              <div className="detail-info-item">
                <span>Thời lượng</span>
                <strong>{formatDuration(movie.durationMinutes)}</strong>
              </div>
              <div className="detail-info-item">
                <span>Độ tuổi</span>
                <strong>{movie.ageRating}+</strong>
              </div>
              <div className="detail-info-item">
                <span>Ngôn ngữ</span>
                <strong>{movie.language}</strong>
              </div>
            </div>
          </section>
        </div>

        <aside className="booking-side">
          <div className="surface card-pad">
            <div className="row between" style={{ marginBottom: 14 }}>
              <h3 className="h3">Suất chiếu hôm nay</h3>
              <span className="badge badge-success">Còn vé</span>
            </div>
            <div className="row gap-6" style={{ marginBottom: 14, color: 'var(--muted)', fontSize: 12 }}>
              <Icon name="location" size={14} />
              {slots[0]?.cinemaName ?? 'Cinex'}
            </div>
            <div className="showtime-slots" style={{ marginBottom: 16 }}>
              {slots.length === 0 && (
                <p className="muted" style={{ fontSize: 13 }}>
                  Chưa có suất chiếu cho phim này.
                </p>
              )}
              {slots.map((slot, index) => (
                <button
                  key={slot.id}
                  type="button"
                  className={`slot ${index === 0 ? 'is-active' : ''}`}
                  onClick={() => navigate(`/seats/${slot.id}`)}
                >
                  <strong>{formatTime(slot.startTime)}</strong>
                  <span>{formatVnd(Number(slot.basePrice))}</span>
                </button>
              ))}
            </div>
            <Link to={`/showtimes?movieId=${movie.id}`} className="btn btn-primary btn-block">
              Chọn suất chiếu khác
            </Link>
            <div className="secure-note" style={{ marginTop: 14 }}>
              <Icon name="shield" size={15} />
              Thanh toán an toàn, ghế được giữ ngay khi thanh toán xong.
            </div>
          </div>
        </aside>
      </div>

      <section className="section">
        <SectionHeader title="Phim tương tự" action="Xem tất cả" actionTo="/movies" />
        <div className="h-scroll">
          {related.map((item) => (
            <MovieCard key={item.id} movie={item} showMatch={false} />
          ))}
        </div>
      </section>

      {trailerOpen && trailerAvailable && (
        <div className="modal-backdrop" onClick={() => setTrailerOpen(false)}>
          <div className="modal" onClick={(event) => event.stopPropagation()}>
            <button type="button" className="modal__close" onClick={() => setTrailerOpen(false)} aria-label="Đóng">
              <Icon name="close" size={20} />
            </button>
            <div className="trailer-frame" style={{ borderRadius: 16, position: 'relative' }}>
              <iframe
                src={trailerEmbedUrl}
                title={`Trailer ${movie.title}`}
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                loading="eager"
                referrerPolicy="strict-origin-when-cross-origin"
                onLoad={() => setTrailerLoaded(true)}
                onError={() => setTrailerTimedOut(true)}
                style={{ width: '100%', height: '100%', border: 0 }}
              />
              {trailerTimedOut && !trailerLoaded && (
                <div
                  style={{
                    position: 'absolute',
                    inset: 0,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: 12,
                    background: 'rgba(0,0,0,0.75)',
                    color: '#fff',
                    padding: 24,
                    textAlign: 'center',
                  }}
                >
                  <Icon name="alert" size={32} />
                  <p style={{ margin: 0, fontSize: 14 }}>
                    Không tải được trailer ngay trong trang (có thể do mạng hoặc trình duyệt chặn).
                  </p>
                  <a
                    href={trailerWatchUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="btn btn-primary btn-sm"
                  >
                    <Icon name="play" size={16} />
                    Mở trailer trên YouTube
                  </a>
                </div>
              )}
            </div>
            <div style={{ marginTop: 10, textAlign: 'center' }}>
              <a
                href={trailerWatchUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="btn-link"
                style={{ fontSize: 13 }}
              >
                Không xem được? Mở trailer trực tiếp trên YouTube ↗
              </a>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
