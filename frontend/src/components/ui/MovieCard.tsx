import { Link } from 'react-router-dom'
import type { UiMovie } from '../../data/mockData'
import { formatDuration } from '../../utils/format'
import { Icon } from './Icon'

interface MovieCardProps {
  movie: UiMovie
  showMatch?: boolean
  className?: string
}

export function MovieCard({ movie, showMatch = true, className }: MovieCardProps) {
  return (
    <Link to={`/movies/${movie.id}`} className={`movie-card ${className ?? ''}`}>
      <div className="movie-card__poster">
        <img src={movie.posterUrl} alt={movie.title} loading="lazy" />
        <div className="movie-card__top">
          {showMatch ? (
            <span className="badge badge-accent">{movie.match}% Match</span>
          ) : (
            <span className="badge badge-primary">{movie.ageRating}+</span>
          )}
          <span className="movie-card__rating">
            <Icon name="star" size={10} filled />
            {movie.rating.toFixed(1)}
          </span>
        </div>
        <span className="movie-card__hover btn btn-accent btn-sm">
          <Icon name="ticket" size={14} />
          Đặt vé
        </span>
      </div>
      <div>
        <h3 className="movie-card__title">{movie.title}</h3>
        <p className="movie-card__meta">
          {movie.genre.split(',')[0]} · {formatDuration(movie.durationMinutes)}
        </p>
      </div>
    </Link>
  )
}
