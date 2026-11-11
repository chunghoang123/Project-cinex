import api from '../api/axios'
import type { Movie } from '../types/Movie'
import type {
  AdminDashboard,
  AdminUser,
  Auditorium,
  AuditoriumPayload,
  Booking,
  Cinema,
  CinemaPayload,
  CreateUserPayload,
  Seat,
  Showtime,
  ShowtimePayload,
} from '../types/Admin'

interface Envelope<T> {
  success: boolean
  message: string
  data: T
}

export interface YoutubeTrailer {
  videoId: string
  title: string
  channelTitle: string
  thumbnailUrl: string
  embedUrl: string
  watchUrl: string
  duration: string
}

export const youtubeService = {
  searchTrailers: async (query: string, maxResults = 10): Promise<YoutubeTrailer[]> => {
    const res = await api.get<Envelope<YoutubeTrailer[]>>('/youtube/trailers', {
      params: { q: query, maxResults },
    })
    return res.data.data
  },

  fetchVideos: async (ids: string[]): Promise<YoutubeTrailer[]> => {
    const res = await api.get<Envelope<YoutubeTrailer[]>>('/youtube/videos', {
      params: { ids: ids.join(',') },
    })
    return res.data.data
  },

  syncMovieTrailer: async (movieId: number): Promise<void> => {
    await api.post(`/youtube/sync/${movieId}`)
  },
}

export const adminService = {
  getDashboard: async (): Promise<AdminDashboard> => {
    const res = await api.get<Envelope<AdminDashboard>>('/admin/dashboard')
    return res.data.data
  },

  getMovies: async (): Promise<Movie[]> => {
    const res = await api.get<Envelope<Movie[]>>('/admin/movies')
    return res.data.data
  },
  createMovie: async (payload: Omit<Movie, 'id' | 'active' | 'createdAt' | 'updatedAt'>): Promise<Movie> => {
    const res = await api.post<Envelope<Movie>>('/admin/movies', payload)
    return res.data.data
  },
  updateMovie: async (id: number, payload: Omit<Movie, 'id' | 'active' | 'createdAt' | 'updatedAt'>): Promise<Movie> => {
    const res = await api.put<Envelope<Movie>>(`/admin/movies/${id}`, payload)
    return res.data.data
  },
  deleteMovie: async (id: number): Promise<void> => {
    await api.delete(`/admin/movies/${id}`)
  },

  getCinemas: async (): Promise<Cinema[]> => {
    const res = await api.get<Envelope<Cinema[]>>('/admin/cinemas')
    return res.data.data
  },
  createCinema: async (payload: CinemaPayload): Promise<Cinema> => {
    const res = await api.post<Envelope<Cinema>>('/admin/cinemas', payload)
    return res.data.data
  },
  updateCinema: async (id: number, payload: CinemaPayload): Promise<Cinema> => {
    const res = await api.put<Envelope<Cinema>>(`/admin/cinemas/${id}`, payload)
    return res.data.data
  },
  deleteCinema: async (id: number): Promise<void> => {
    await api.delete(`/admin/cinemas/${id}`)
  },
  /** Bật/tắt bảo trì rạp. */
  setCinemaStatus: async (id: number, active: boolean): Promise<Cinema> => {
    const res = await api.patch<Envelope<Cinema>>(`/admin/cinemas/${id}/status`, { active })
    return res.data.data
  },

  getAuditoriums: async (cinemaId: number): Promise<Auditorium[]> => {
    const res = await api.get<Envelope<Auditorium[]>>(`/admin/cinemas/${cinemaId}/auditoriums`)
    return res.data.data
  },
  createAuditorium: async (cinemaId: number, payload: AuditoriumPayload): Promise<Auditorium> => {
    const res = await api.post<Envelope<Auditorium>>(`/admin/cinemas/${cinemaId}/auditoriums`, payload)
    return res.data.data
  },
  updateAuditorium: async (id: number, payload: AuditoriumPayload): Promise<Auditorium> => {
    const res = await api.put<Envelope<Auditorium>>(`/admin/auditoriums/${id}`, payload)
    return res.data.data
  },
  deleteAuditorium: async (id: number): Promise<void> => {
    await api.delete(`/admin/auditoriums/${id}`)
  },
  getSeats: async (auditoriumId: number): Promise<Seat[]> => {
    const res = await api.get<Envelope<Seat[]>>(`/admin/auditoriums/${auditoriumId}/seats`)
    return res.data.data
  },
  generateSeats: async (auditoriumId: number): Promise<Seat[]> => {
    const res = await api.post<Envelope<Seat[]>>(`/admin/auditoriums/${auditoriumId}/seats/generate`)
    return res.data.data
  },

  getShowtimes: async (params: { date?: string; cinemaId?: number; movieId?: number }): Promise<Showtime[]> => {
    const res = await api.get<Envelope<Showtime[]>>('/admin/showtimes', { params })
    return res.data.data
  },
  createShowtime: async (payload: ShowtimePayload): Promise<Showtime> => {
    const res = await api.post<Envelope<Showtime>>('/admin/showtimes', payload)
    return res.data.data
  },
  updateShowtime: async (id: number, payload: ShowtimePayload): Promise<Showtime> => {
    const res = await api.put<Envelope<Showtime>>(`/admin/showtimes/${id}`, payload)
    return res.data.data
  },
  deleteShowtime: async (id: number): Promise<void> => {
    await api.delete(`/admin/showtimes/${id}`)
  },
  autoGenerateShowtimes: async (days = 10): Promise<AutoScheduleResult> => {
    const res = await api.post<Envelope<AutoScheduleResult>>('/admin/showtimes/auto-generate', null, {
      params: { days },
    })
    return res.data.data
  },

  chatAi: async (
    message: string,
    history: Array<{ role: 'user' | 'assistant'; content: string }> = [],
  ): Promise<AdminAiResponse> => {
    const res = await api.post<Envelope<AdminAiResponse>>(
      '/admin/ai/chat',
      { message, history },
      { timeout: 60_000 },
    )
    return res.data.data
  },

  getBookings: async (params: { status?: string; date?: string; q?: string }): Promise<Booking[]> => {
    const res = await api.get<Envelope<Booking[]>>('/admin/bookings', { params })
    return res.data.data
  },
  cancelBooking: async (code: string): Promise<Booking> => {
    const res = await api.post<Envelope<Booking>>(`/admin/bookings/${code}/cancel`)
    return res.data.data
  },

  getUsers: async (): Promise<AdminUser[]> => {
    const res = await api.get<Envelope<AdminUser[]>>('/admin/users')
    return res.data.data
  },
  createUser: async (payload: CreateUserPayload): Promise<AdminUser> => {
    const res = await api.post<Envelope<AdminUser>>('/admin/users', payload)
    return res.data.data
  },
  updateUserRoles: async (id: number, roles: string[]): Promise<AdminUser> => {
    const res = await api.patch<Envelope<AdminUser>>(`/admin/users/${id}/roles`, { roles })
    return res.data.data
  },
  updateUserStatus: async (id: number, active: boolean): Promise<AdminUser> => {
    const res = await api.patch<Envelope<AdminUser>>(`/admin/users/${id}/status`, { active })
    return res.data.data
  },
}
