import api from '../api/axios'
import { Movie, ApiResponse } from '../types/Movie'

export const movieService = {
  getAll: async (status?: string): Promise<Movie[]> => {
    const params = status ? { status } : {}
    const res = await api.get<ApiResponse<Movie[]>>('/movies', { params })
    return res.data.data
  },
  getById: async (id: number): Promise<Movie> => {
    const res = await api.get<ApiResponse<Movie>>(`/movies/${id}`)
    return res.data.data
  },
}