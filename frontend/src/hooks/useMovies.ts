import { useEffect, useMemo, useState } from 'react'
import { movieService } from '../services/movieService'
import type { Movie } from '../types/Movie'
import { movies as mockMovies, type UiMovie } from '../data/mockData'

function enrich(movie: Movie, index: number): UiMovie {
  const fallback = mockMovies.find((item) => item.id === movie.id) ?? mockMovies[index % mockMovies.length]
  return {
    ...fallback,
    id: movie.id,
    title: movie.title || fallback.title,
    description: movie.description || fallback.description,
    posterUrl: movie.posterUrl || fallback.posterUrl,
    trailerUrl: movie.trailerUrl || fallback.trailerUrl,
    durationMinutes: movie.durationMinutes || fallback.durationMinutes,
    ageRating: movie.ageRating || fallback.ageRating,
    genre: movie.genre || fallback.genre,
    status: movie.status || fallback.status,
  }
}

export function useMovies(status?: 'NOW_SHOWING' | 'COMING_SOON') {
  const [apiMovies, setApiMovies] = useState<UiMovie[] | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    movieService
      .getAll(status)
      .then((data) => {
        if (!cancelled && data.length > 0) {
          setApiMovies(data.map(enrich))
        }
      })
      .catch(() => {
        // API chưa chạy: dùng dữ liệu mock để giao diện vẫn hoạt động.
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [status])

  const data = useMemo(() => {
    if (apiMovies && apiMovies.length > 0) return apiMovies
    return status ? mockMovies.filter((movie) => movie.status === status) : mockMovies
  }, [apiMovies, status])

  return { movies: data, loading: loading && !apiMovies }
}

export function useMovie(id?: string) {
  const [movie, setMovie] = useState<UiMovie | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!id) {
      setLoading(false)
      return
    }
    const numericId = Number(id)
    const fallback = mockMovies.find((item) => item.id === numericId) ?? null
    setMovie(fallback)
    setLoading(!fallback)
    movieService
      .getById(numericId)
      .then((data) => setMovie(enrich(data, 0)))
      .catch(() => {
        // Giữ dữ liệu mock nếu API chưa sẵn sàng.
      })
      .finally(() => setLoading(false))
  }, [id])

  return { movie, loading }
}
