import api from '../api/axios'
import type { Cinema } from '../types/Admin'

interface Envelope<T> {
  success: boolean
  message: string
  data: T
}

export const catalogService = {
  getCinemas: async (): Promise<Cinema[]> => {
    const res = await api.get<Envelope<Cinema[]>>('/cinemas')
    return res.data.data
  },
}
