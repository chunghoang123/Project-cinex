import { useEffect, useState } from 'react'
import { bookingService } from '../services/bookingService'
import { catalogService } from '../services/catalogService'
import type { Cinema, Showtime } from '../types/Admin'

/**
 * Dữ liệu dùng cho ô tìm kiếm: rạp đang hoạt động + suất chiếu 7 ngày tới.
 * Tải song song, lỗi từng nguồn không chặn phần còn lại.
 */
export function useSearchData() {
  const [cinemas, setCinemas] = useState<Cinema[]>([])
  const [showtimes, setShowtimes] = useState<Showtime[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    Promise.all([
      catalogService.getCinemas().catch(() => [] as Cinema[]),
      bookingService.getShowtimes().catch(() => [] as Showtime[]),
    ])
      .then(([cinemaList, showtimeList]) => {
        if (cancelled) return
        setCinemas(cinemaList)
        setShowtimes(showtimeList)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  return { cinemas, showtimes, loading }
}
