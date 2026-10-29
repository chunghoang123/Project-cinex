export type MovieStatus = 'NOW_SHOWING' | 'COMING_SOON'

export interface CastMember {
  name: string
  role: string
  avatar: string
}

export interface UiMovie {
  id: number
  title: string
  description: string
  posterUrl: string
  backdropUrl: string
  trailerUrl: string
  durationMinutes: number
  ageRating: number
  genre: string
  status: MovieStatus
  rating: number
  match: number
  releaseDate: string
  language: string
  formats: string[]
  cast: CastMember[]
}

export interface Cinema {
  id: number
  name: string
  address: string
  distance: string
}

export interface ShowtimeSlot {
  id: number
  time: string
  price: number
  format: string
  hall: string
}

export interface Snack {
  id: string
  name: string
  description: string
  price: number
  image: string
}

export interface BookingRecord {
  id: number
  code: string
  movieId: number
  movieTitle: string
  posterUrl: string
  cinema: string
  hall: string
  date: string
  time: string
  seats: string[]
  total: number
  status: 'UPCOMING' | 'COMPLETED' | 'CANCELLED'
}

const tmdb = 'https://image.tmdb.org/t/p'

export const demoUser = {
  name: 'Minh Dương',
  email: 'duong@cinex.vn',
  phone: '090 123 4567',
  avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80',
  memberSince: '2024',
  points: 1280,
}

export const movies: UiMovie[] = [
  {
    id: 1,
    title: 'Dune: Part Two',
    description:
      'Paul Atreides liên minh với Chani và người Fremen để trả thù những kẻ đã hủy diệt gia đình mình. Đứng trước lựa chọn giữa tình yêu và vận mệnh của vũ trụ, cậu phải ngăn chặn một tương lai khủng khiếp mà chỉ mình cậu nhìn thấy.',
    posterUrl: `${tmdb}/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg`,
    backdropUrl: `${tmdb}/original/xOMo8BRK7PfcJv9JCnx7s5hj0PX.jpg`,
    trailerUrl: 'https://www.youtube.com/embed/Way9Dexny3w',
    durationMinutes: 166,
    ageRating: 13,
    genre: 'Sci-Fi, Adventure',
    status: 'NOW_SHOWING',
    rating: 9.2,
    match: 98,
    releaseDate: '01/03/2024',
    language: 'Tiếng Anh - Phụ đề Việt',
    formats: ['IMAX', '4DX', '2D'],
    cast: [
      { name: 'Timothée Chalamet', role: 'Paul', avatar: 'https://i.pravatar.cc/150?img=1' },
      { name: 'Zendaya', role: 'Chani', avatar: 'https://i.pravatar.cc/150?img=2' },
      { name: 'Rebecca Ferguson', role: 'Jessica', avatar: 'https://i.pravatar.cc/150?img=3' },
      { name: 'Austin Butler', role: 'Feyd-Rautha', avatar: 'https://i.pravatar.cc/150?img=4' },
    ],
  },
  {
    id: 2,
    title: 'Blade Runner 2049',
    description:
      'Một thợ săn replicant trẻ tuổi phát hiện ra một bí mật bị chôn vùi từ lâu có thể đẩy xã hội vào hỗn loạn. Hành trình đi tìm sự thật đưa anh đến với Rick Deckard, người đã mất tích hơn ba thập kỷ.',
    posterUrl: `${tmdb}/w500/gajva2L0rPYkEWjzgFlBXCAVBE5.jpg`,
    backdropUrl: `${tmdb}/original/ilRyazdMJwN05exqhwK4tMKBYZs.jpg`,
    trailerUrl: 'https://www.youtube.com/embed/gCcx85zbxz4',
    durationMinutes: 164,
    ageRating: 16,
    genre: 'Sci-Fi, Thriller',
    status: 'NOW_SHOWING',
    rating: 8.9,
    match: 96,
    releaseDate: '06/10/2017',
    language: 'Tiếng Anh - Phụ đề Việt',
    formats: ['IMAX', '2D'],
    cast: [
      { name: 'Ryan Gosling', role: 'K', avatar: 'https://i.pravatar.cc/150?img=5' },
      { name: 'Harrison Ford', role: 'Deckard', avatar: 'https://i.pravatar.cc/150?img=6' },
      { name: 'Ana de Armas', role: 'Joi', avatar: 'https://i.pravatar.cc/150?img=7' },
      { name: 'Robin Wright', role: 'Lt. Joshi', avatar: 'https://i.pravatar.cc/150?img=8' },
    ],
  },
  {
    id: 3,
    title: 'Civil War',
    description:
      'Trong tương lai gần, một nhóm nhà báo chiến tranh đi xuyên nước Mỹ đang bị chia cắt để đến Washington D.C. trước khi quân nổi dậy tấn công Nhà Trắng. Họ phải đối mặt với những mất mát và lựa chọn đạo đức khắc nghiệt.',
    posterUrl: `${tmdb}/w500/sh7Rg8Er3tFcN9BpKIPOMvALgZd.jpg`,
    backdropUrl: `${tmdb}/original/sh7Rg8Er3tFcN9BpKIPOMvALgZd.jpg`,
    trailerUrl: 'https://www.youtube.com/embed/aDyQxtg0V2w',
    durationMinutes: 109,
    ageRating: 18,
    genre: 'Action, Thriller',
    status: 'NOW_SHOWING',
    rating: 8.1,
    match: 85,
    releaseDate: '12/04/2024',
    language: 'Tiếng Anh - Phụ đề Việt',
    formats: ['IMAX', '2D'],
    cast: [
      { name: 'Kirsten Dunst', role: 'Lee', avatar: 'https://i.pravatar.cc/150?img=9' },
      { name: 'Wagner Moura', role: 'Joel', avatar: 'https://i.pravatar.cc/150?img=10' },
      { name: 'Cailee Spaeny', role: 'Jessie', avatar: 'https://i.pravatar.cc/150?img=11' },
      { name: 'Stephen McKinley Henderson', role: 'Sammy', avatar: 'https://i.pravatar.cc/150?img=12' },
    ],
  },
  {
    id: 4,
    title: 'The Creator',
    description:
      'Giữa cuộc chiến tranh giữa loài người và trí tuệ nhân tạo, một cựu đặc vụ được giao nhiệm vụ săn lùng "Vũ khí", một AI có hình dạng một đứa trẻ. Khi biết sự thật, anh phải chọn giữa nhiệm vụ và tương lai của nhân loại.',
    posterUrl: `${tmdb}/w500/vBZ0qvaRxqEhZwl6LWmruJqWE8Z.jpg`,
    backdropUrl: `${tmdb}/original/vBZ0qvaRxqEhZwl6LWmruJqWE8Z.jpg`,
    trailerUrl: 'https://www.youtube.com/embed/ex3C1-5Dhb8',
    durationMinutes: 133,
    ageRating: 16,
    genre: 'Sci-Fi, Drama',
    status: 'NOW_SHOWING',
    rating: 7.8,
    match: 78,
    releaseDate: '29/09/2023',
    language: 'Tiếng Anh - Phụ đề Việt',
    formats: ['IMAX', '2D'],
    cast: [
      { name: 'John David Washington', role: 'Joshua', avatar: 'https://i.pravatar.cc/150?img=13' },
      { name: 'Gemma Chan', role: 'Maya', avatar: 'https://i.pravatar.cc/150?img=14' },
      { name: 'Ken Watanabe', role: 'Harun', avatar: 'https://i.pravatar.cc/150?img=15' },
      { name: 'Allison Janney', role: 'Howell', avatar: 'https://i.pravatar.cc/150?img=16' },
    ],
  },
  {
    id: 5,
    title: 'Interstellar',
    description:
      'Khi Trái Đất dần trở nên không thể sinh sống, một nhóm nhà thám hiểm dấn thân vào hành trình xuyên qua hố đen để tìm một ngôi nhà mới cho nhân loại. Cuộc hành trình thử thách mối quan hệ cha con và giới hạn của thời gian.',
    posterUrl: `${tmdb}/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg`,
    backdropUrl: `${tmdb}/original/xJHokMbljvjADYdit5fK5VQsXEG.jpg`,
    trailerUrl: 'https://www.youtube.com/embed/zSWdZVtXT7E',
    durationMinutes: 169,
    ageRating: 13,
    genre: 'Sci-Fi, Drama',
    status: 'NOW_SHOWING',
    rating: 9.0,
    match: 94,
    releaseDate: '07/11/2014',
    language: 'Tiếng Anh - Phụ đề Việt',
    formats: ['IMAX', '2D'],
    cast: [
      { name: 'Matthew McConaughey', role: 'Cooper', avatar: 'https://i.pravatar.cc/150?img=17' },
      { name: 'Anne Hathaway', role: 'Brand', avatar: 'https://i.pravatar.cc/150?img=18' },
      { name: 'Jessica Chastain', role: 'Murph', avatar: 'https://i.pravatar.cc/150?img=19' },
      { name: 'Michael Caine', role: 'Professor Brand', avatar: 'https://i.pravatar.cc/150?img=20' },
    ],
  },
  {
    id: 6,
    title: 'The Batman',
    description:
      'Hai năm theo dõi tội phạm ở Gotham, Batman bị cuốn vào vụ án của Riddler - kẻ giết người hàng loạt để lại những câu đố. Khi sự thật về gia đình Wayne dần lộ diện, Batman phải định nghĩa lại vai trò của mình.',
    posterUrl: `${tmdb}/w500/74xTEgt7R36Fpooo50r9T25onhq.jpg`,
    backdropUrl: `${tmdb}/original/b0PlSFdDwbyK0cf5RxwDpaOJQvQ.jpg`,
    trailerUrl: 'https://www.youtube.com/embed/mqqft2x_Aa4',
    durationMinutes: 176,
    ageRating: 16,
    genre: 'Action, Crime',
    status: 'NOW_SHOWING',
    rating: 8.7,
    match: 88,
    releaseDate: '04/03/2022',
    language: 'Tiếng Anh - Phụ đề Việt',
    formats: ['IMAX', '2D'],
    cast: [
      { name: 'Robert Pattinson', role: 'Batman', avatar: 'https://i.pravatar.cc/150?img=21' },
      { name: 'Zoë Kravitz', role: 'Selina', avatar: 'https://i.pravatar.cc/150?img=22' },
      { name: 'Paul Dano', role: 'Riddler', avatar: 'https://i.pravatar.cc/150?img=23' },
      { name: 'Colin Farrell', role: 'Penguin', avatar: 'https://i.pravatar.cc/150?img=24' },
    ],
  },
  {
    id: 7,
    title: 'Oppenheimer',
    description:
      'Câu chuyện về J. Robert Oppenheimer, cha đẻ của bom nguyên tử, và những lựa chọn đạo đức đã thay đổi mãi mãi lịch sử nhân loại. Bộ phim khắc họa hành trình từ thiên tài vật lý đến người đàn ông bị ám ảnh bởi di sản của mình.',
    posterUrl: `${tmdb}/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg`,
    backdropUrl: `${tmdb}/original/fm6KqXpk3M2HVveHwCrBSSBaO0V.jpg`,
    trailerUrl: 'https://www.youtube.com/embed/uYPbbksJxIg',
    durationMinutes: 180,
    ageRating: 16,
    genre: 'Drama, History',
    status: 'COMING_SOON',
    rating: 8.8,
    match: 90,
    releaseDate: '21/07/2023',
    language: 'Tiếng Anh - Phụ đề Việt',
    formats: ['IMAX', '2D'],
    cast: [
      { name: 'Cillian Murphy', role: 'Oppenheimer', avatar: 'https://i.pravatar.cc/150?img=25' },
      { name: 'Emily Blunt', role: 'Kitty', avatar: 'https://i.pravatar.cc/150?img=26' },
      { name: 'Matt Damon', role: 'Groves', avatar: 'https://i.pravatar.cc/150?img=27' },
      { name: 'Robert Downey Jr.', role: 'Strauss', avatar: 'https://i.pravatar.cc/150?img=28' },
    ],
  },
  {
    id: 8,
    title: 'Spider-Man: Across the Spider-Verse',
    description:
      'Miles Morales du hành qua đa vũ trụ và gặp gỡ đội quân Spider-Man. Khi mâu thuẫn về cách xử lý mối đe dọa mới nổ ra, Miles phải tự định nghĩa lại điều gì làm nên một người hùng.',
    posterUrl: `${tmdb}/w500/8Vt6mWEReuy4Of61Lnj5Xj704m8.jpg`,
    backdropUrl: `${tmdb}/original/8Vt6mWEReuy4Of61Lnj5Xj704m8.jpg`,
    trailerUrl: 'https://www.youtube.com/embed/cqGjhVJWtEg',
    durationMinutes: 140,
    ageRating: 13,
    genre: 'Animation, Action',
    status: 'NOW_SHOWING',
    rating: 9.1,
    match: 93,
    releaseDate: '02/06/2023',
    language: 'Tiếng Anh - Phụ đề Việt',
    formats: ['IMAX', '2D'],
    cast: [
      { name: 'Shameik Moore', role: 'Miles', avatar: 'https://i.pravatar.cc/150?img=29' },
      { name: 'Hailee Steinfeld', role: 'Gwen', avatar: 'https://i.pravatar.cc/150?img=30' },
      { name: 'Oscar Isaac', role: 'Miguel', avatar: 'https://i.pravatar.cc/150?img=31' },
      { name: 'Jake Johnson', role: 'Peter B.', avatar: 'https://i.pravatar.cc/150?img=32' },
    ],
  },
]

export const cinemas: Cinema[] = [
  { id: 1, name: 'Cinex Landmark 81', address: '720A Điện Biên Phủ, Bình Thạnh, TP.HCM', distance: '1.2 km' },
  { id: 2, name: 'Cinex Galaxy Nguyễn Du', address: '116 Nguyễn Du, Quận 1, TP.HCM', distance: '2.5 km' },
  { id: 3, name: 'Cinex Aeon Tân Phú', address: '30 Bờ Bao Tân Thắng, Tân Phú, TP.HCM', distance: '4.1 km' },
]

const times = {
  morning: ['09:30', '11:00'],
  afternoon: ['13:15', '14:45', '16:20'],
  evening: ['19:00', '21:30', '23:15'],
}

export function showtimesFor(movieId: number): ShowtimeSlot[] {
  const base = 95000 + (movieId % 4) * 15000
  const all = [...times.morning, ...times.afternoon, ...times.evening]
  return all.map((time, index) => ({
    id: movieId * 100 + index,
    time,
    price: base + (index >= 5 ? 45000 : index >= 2 ? 15000 : 0),
    format: index % 3 === 0 ? 'IMAX' : '2D',
    hall: `Screen ${(index % 4) + 1}`,
  }))
}

export const snacks: Snack[] = [
  {
    id: 'duo',
    name: 'Duo Combo',
    description: '1 bắp lớn + 2 nước ngọt',
    price: 165000,
    image: 'https://images.unsplash.com/photo-1585647347483-22b66260dfff?auto=format&fit=crop&w=200&q=80',
  },
  {
    id: 'popcorn',
    name: 'Bắp rang bơ',
    description: 'Bắp rang bơ cỡ lớn',
    price: 75000,
    image: 'https://images.unsplash.com/photo-1578849278619-e73505e9610f?auto=format&fit=crop&w=200&q=80',
  },
  {
    id: 'coke',
    name: 'Coca-Cola',
    description: 'Ly lớn 32oz',
    price: 45000,
    image: 'https://images.unsplash.com/photo-1554866585-cd94860890b7?auto=format&fit=crop&w=200&q=80',
  },
]

export const bookings: BookingRecord[] = [
  {
    id: 1,
    code: 'CX-240315-01',
    movieId: 1,
    movieTitle: 'Dune: Part Two',
    posterUrl: `${tmdb}/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg`,
    cinema: 'Cinex Landmark 81',
    hall: 'Screen 4',
    date: '15/03/2025',
    time: '19:30',
    seats: ['D5', 'D6'],
    total: 380000,
    status: 'UPCOMING',
  },
  {
    id: 2,
    code: 'CX-240308-02',
    movieId: 8,
    movieTitle: 'Spider-Man: Across the Spider-Verse',
    posterUrl: `${tmdb}/w500/8Vt6mWEReuy4Of61Lnj5Xj704m8.jpg`,
    cinema: 'Cinex Galaxy Nguyễn Du',
    hall: 'Screen 2',
    date: '08/03/2025',
    time: '20:15',
    seats: ['F7'],
    total: 190000,
    status: 'UPCOMING',
  },
  {
    id: 3,
    code: 'CX-240224-03',
    movieId: 7,
    movieTitle: 'Oppenheimer',
    posterUrl: `${tmdb}/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg`,
    cinema: 'Cinex Aeon Tân Phú',
    hall: 'IMAX',
    date: '24/02/2025',
    time: '18:00',
    seats: ['H5', 'H6'],
    total: 420000,
    status: 'COMPLETED',
  },
  {
    id: 4,
    code: 'CX-240210-04',
    movieId: 5,
    movieTitle: 'Interstellar',
    posterUrl: `${tmdb}/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg`,
    cinema: 'Cinex Landmark 81',
    hall: 'IMAX',
    date: '10/02/2025',
    time: '21:00',
    seats: ['E8'],
    total: 220000,
    status: 'COMPLETED',
  },
]

export const seatRows = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']

export interface SeatCell {
  id: string
  row: string
  number: number
  type: 'standard' | 'vip' | 'couple'
  status: 'available' | 'booked' | 'held'
}

export function buildSeatMap(): SeatCell[] {
  const booked = new Set([
    'A3', 'A4', 'A9', 'B2', 'B7', 'B8', 'C5', 'C11', 'D4', 'D9',
    'E6', 'F3', 'F10', 'G7', 'H4', 'H9',
  ])
  const held = new Set(['C8', 'D7', 'F6'])
  const seats: SeatCell[] = []
  seatRows.forEach((row) => {
    const isCouple = row === 'H'
    const isVip = row === 'E' || row === 'F'
    for (let number = 1; number <= 12; number += 1) {
      const id = `${row}${number}`
      seats.push({
        id,
        row,
        number,
        type: isCouple ? 'couple' : isVip ? 'vip' : 'standard',
        status: booked.has(id) ? 'booked' : held.has(id) ? 'held' : 'available',
      })
    }
  })
  return seats
}

export const genres = ['Hành động', 'Kinh dị', 'Tình cảm', 'Hoạt hình', 'Sci-Fi', 'Hài', 'Tâm lý']

export const quickGenres = ['Tất cả', 'Hành động', 'Sci-Fi', 'Kinh dị', 'Hoạt hình', 'Tình cảm']

