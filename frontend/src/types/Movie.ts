export interface Movie {
  id: number
  title: string
  description: string
  posterUrl: string
  trailerUrl: string
  durationMinutes: number
  ageRating: number
  genre: string
  status: 'NOW_SHOWING' | 'COMING_SOON'
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}