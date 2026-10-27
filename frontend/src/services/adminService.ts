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
