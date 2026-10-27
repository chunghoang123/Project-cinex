# 🔧 TÀI LIỆU KỸ THUẬT CHI TIẾT

## Website đặt vé xem phim — Rạp Cinex

> **Phiên bản:** 1.0 (DRAFT) | **Ngày:** 27/10/2026 | **Mã tài liệu:** TECH-CINEX-001

---

## MỤC LỤC

1. [Kiến trúc hệ thống](#1-kiến-trúc-hệ-thống)
2. [Cấu trúc Backend (Spring Boot)](#2-cấu-trúc-backend-spring-boot)
3. [Cấu trúc Frontend (React)](#3-cấu-trúc-frontend-react)
4. [Thiết kế CSDL chi tiết](#4-thiết-kế-csdl-chi-tiết)
5. [Entity & Repository](#5-entity--repository)
6. [Bảo mật & Phân quyền](#6-bảo-mật--phân-quyền)
7. [API Design](#7-api-design)
8. [Xử lý lỗi & Validation](#8-xử-lý-lỗi--validation)
9. [Giữ ghế & Concurrency](#9-giữ-ghế--concurrency)
10. [Thanh toán & Vé QR](#10-thanh-toán--vé-qr)
11. [Chatbot AI Integration](#11-chatbot-ai-integration)
12. [Frontend Architecture](#12-frontend-architecture)
13. [Testing Strategy](#13-testing-strategy)
14. [Deployment & DevOps](#14-deployment--devops)
15. [Quy ước Code](#15-quy-ước-code)

---

## 1. Kiến trúc hệ thống

### 1.1 Mô hình tổng thể

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT (Browser)                     │
│  ┌─────────────────────────────────────────────────────┐    │
│  │           React + TypeScript (Vite)                  │    │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐           │    │
│  │  │  Pages   │ │ Components│ │ Services │           │    │
│  │  └──────────┘ └──────────┘ └──────────┘           │    │
│  └──────────────────────┬──────────────────────────────┘    │
└─────────────────────────┼───────────────────────────────────┘
                          │ HTTP/REST (JSON)
                          │ Authorization: Bearer <JWT>
┌─────────────────────────┼───────────────────────────────────┐
│                         ▼                                    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              Spring Boot 3 (Java 21)                │    │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐           │    │
│  │  │Controller│ │ Service  │ │Repository│           │    │
│  │  └──────────┘ └──────────┘ └──────────┘           │    │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐           │    │
│  │  │ Security │ │  Config  │ │ Exception│           │    │
│  │  └──────────┘ └──────────┘ └──────────┘           │    │
│  └──────────────────────┬──────────────────────────────┘    │
│                         │ JPA/Hibernate                     │
│  ┌──────────────────────┴──────────────────────────────┐    │
│  │                    MySQL 8                          │    │
│  │  ┌─────────────────────────────────────────────┐    │    │
│  │  │  14 tables (users, movies, showtimes, ...)  │    │    │
│  │  └─────────────────────────────────────────────┘    │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 Communication Flow
1. Frontend (React) gọi REST API đến Backend (Spring Boot) qua Axios.
2. Backend xử lý logic, truy xuất CSDL qua JPA/Hibernate.
3. Backend trả JSON response về Frontend.
4. Frontend render dữ liệu.

### 1.3 Security Flow
1. User đăng nhập → Backend xác thực → trả JWT token.
2. Frontend lưu JWT trong localStorage/memory.
3. Mỗi request gửi kèm `Authorization: Bearer <token>`.
4. Backend filter kiểm tra JWT → xác định user & role → phân quyền.

---

## 2. Cấu trúc Backend (Spring Boot)

### 2.1 Cấu trúc package (domain/feature)

```
com.cinex.cinema
├── CinexApplication.java          # Main class + @EnableJpaAuditing
├── common/
│   ├── BaseEntity.java            # @MappedSuperclass (id, createdAt, updatedAt)
│   ├── ApiResponse.java           # Wrapper response chuẩn
│   └── GlobalExceptionHandler.java # @RestControllerAdvice
├── config/
│   └── SecurityConfig.java        # SecurityFilterChain, CORS, PasswordEncoder
├── user/
│   ├── ERole.java                 # Enum: ROLE_ADMIN, ROLE_STAFF, ROLE_CUSTOMER
│   ├── Role.java                  # Entity
│   ├── RoleRepository.java        # JpaRepository
│   ├── User.java                  # Entity
│   └── UserRepository.java        # JpaRepository
├── cinema/
│   ├── Cinema.java                # Entity (rạp)
│   ├── CinemaRepository.java
│   ├── Auditorium.java            # Entity (phòng chiếu)
│   ├── AuditoriumRepository.java
│   ├── Seat.java                  # Entity (ghế)
│   └── SeatRepository.java
├── movie/
│   ├── Movie.java                 # Entity
│   ├── MovieRepository.java
│   └── MovieStatus.java           # Enum: NOW_SHOWING, COMING_SOON
├── showtime/
│   ├── Showtime.java              # Entity
│   ├── ShowtimeRepository.java
│   ├── ShowtimeSeat.java          # Entity (trạng thái ghế theo suất)
│   ├── ShowtimeSeatRepository.java
│   ├── SeatStatus.java            # Enum: AVAILABLE, HELD, BOOKED
│   └── ShowtimeStatus.java        # Enum: ACTIVE, CANCELLED, COMPLETED
├── booking/
│   ├── Booking.java               # Entity
│   ├── BookingRepository.java
│   ├── BookingStatus.java         # Enum: PENDING, CONFIRMED, EXPIRED, CANCELLED
│   ├── BookingSeat.java           # Entity (chi tiết ghế trong đơn)
│   └── BookingSeatRepository.java
├── payment/
│   ├── Payment.java               # Entity
│   ├── PaymentRepository.java
│   └── PaymentStatus.java         # Enum: PROCESSING, SUCCESS, FAILED, REFUNDED
├── ticket/
│   ├── Ticket.java                # Entity
│   ├── TicketRepository.java
│   └── TicketStatus.java          # Enum: UNUSED, SCANNED, CANCELLED
├── promotion/
│   ├── Promotion.java             # Entity (mở rộng sau)
│   └── PromotionRepository.java
└── staff/
    └── (chức năng bán quầy, soát vé sau)
```

### 2.2 Cấu trúc Maven (pom.xml)

```xml
Dependencies:
- spring-boot-starter-web
- spring-boot-starter-security
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- mysql-connector-j (runtime)
- lombok (provided)
- mapstruct + mapstruct-processor (provided)
- spring-boot-starter-test (test)
- spring-security-test (test)
```

### 2.3 Cấu hình application.properties

```properties
# Datasource
spring.datasource.url=jdbc:mysql://localhost:3306/cinex_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
spring.datasource.username=cinex
spring.datasource.password=cinex123

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.open-in-view=false

# Multipart
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# JWT
app.jwt.secret=cinex-secret-key-change-in-production
app.jwt.expiration=86400000

# Logging
logging.level.org.springframework.security=INFO
logging.level.com.cinex=DEBUG
```

---

## 3. Cấu trúc Frontend (React)

### 3.1 Cấu trúc thư mục

```
frontend/
├── package.json
├── tsconfig.json
├── vite.config.ts
├── index.html
└── src/
    ├── main.tsx                    # Entry point
    ├── App.tsx                     # Router + Layout
    ├── api/
    │   └── axios.ts                # Axios instance + interceptor
    ├── types/
    │   ├── User.ts
    │   ├── Movie.ts
    │   ├── Showtime.ts
    │   ├── Seat.ts
    │   ├── Booking.ts
    │   └── Ticket.ts
    ├── services/
    │   ├── authService.ts
    │   ├── movieService.ts
    │   ├── showtimeService.ts
    │   ├── bookingService.ts
    │   └── ticketService.ts
    ├── components/
    │   ├── common/
    │   │   ├── Button.tsx
    │   │   ├── Modal.tsx
    │   │   ├── Toast.tsx
    │   │   ├── Table.tsx
    │   │   └── Loading.tsx
    │   ├── layout/
    │   │   ├── Header.tsx
    │   │   ├── Footer.tsx
    │   │   └── ProtectedRoute.tsx
    │   └── seat/
    │       └── SeatMap.tsx
    ├── pages/
    │   ├── Home.tsx
    │   ├── MovieList.tsx
    │   ├── MovieDetail.tsx
    │   ├── ShowtimeList.tsx
    │   ├── SeatSelection.tsx
    │   ├── BookingConfirm.tsx
    │   ├── Payment.tsx
    │   ├── TicketView.tsx
    │   ├── BookingHistory.tsx
    │   ├── Login.tsx
    │   ├── Register.tsx
    │   ├── Profile.tsx
    │   ├── staff/
    │   │   ├── StaffBooking.tsx
    │   │   ├── StaffSearch.tsx
    │   │   └── StaffValidate.tsx
    │   └── admin/
    │       ├── AdminMovies.tsx
    │       ├── AdminCinemas.tsx
    │       ├── AdminAuditoriums.tsx
    │       ├── AdminSeats.tsx
    │       ├── AdminShowtimes.tsx
    │       ├── AdminUsers.tsx
    │       └── AdminReports.tsx
    ├── hooks/
    │   ├── useAuth.ts
    │   └── useBooking.ts
    └── utils/
        ├── formatDate.ts
        └── formatCurrency.ts
```

### 3.2 Routing

```typescript
/                          → Home
/movies                    → MovieList
/movies/:id                → MovieDetail
/showtimes                 → ShowtimeList
/seats/:showtimeId         → SeatSelection
/booking/confirm           → BookingConfirm
/booking/payment           → Payment
/ticket/:bookingId         → TicketView
/history                   → BookingHistory (protected)
/profile                   → Profile (protected)
/login                     → Login
/register                  → Register

/staff/booking             → StaffBooking (role: STAFF, ADMIN)
/staff/search              → StaffSearch (role: STAFF, ADMIN)
/staff/validate            → StaffValidate (role: STAFF, ADMIN)

/admin/movies              → AdminMovies (role: ADMIN)
/admin/cinemas             → AdminCinemas (role: ADMIN)
/admin/auditoriums         → AdminAuditoriums (role: ADMIN)
/admin/seats               → AdminSeats (role: ADMIN)
/admin/showtimes           → AdminShowtimes (role: ADMIN)
/admin/users               → AdminUsers (role: ADMIN)
/admin/reports             → AdminReports (role: ADMIN)
```

---

## 4. Thiết kế CSDL chi tiết

### 4.1 Entity Relationship Diagram

```
┌──────────┐     ┌──────────┐     ┌──────────────┐
│  users   │     │  roles   │     │  user_roles  │
├──────────┤     ├──────────┤     ├──────────────┤
│ id (PK)  │◄───►│ id (PK)  │◄───►│ user_id (FK) │
│ username │     │ name     │     │ role_id (FK) │
│ email    │     └──────────┘     └──────────────┘
│ password │
│ fullName │
│ phone    │
│ active   │
└────┬─────┘
     │ 1:N
     ▼
┌──────────┐     ┌──────────────┐     ┌──────────┐
│ bookings │     │ booking_seats│     │ showtimes│
├──────────┤     ├──────────────┤     ├──────────┤
│ id (PK)  │◄───►│ id (PK)      │     │ id (PK)  │
│ bookingCode│    │ booking_id   │     │ movie_id │
│ user_id  │     │ showtimeSeatId│    │ auditorium_id│
│ showtime_id│   │ price        │     │ startTime│
│ totalAmount│    └──────┬───────┘     │ endTime  │
│ status   │            │             │ basePrice│
│ expiresAt│            │             │ status   │
└────┬─────┘            │             └────┬─────┘
     │ 1:1              │                  │
     ▼                  ▼                  ▼
┌──────────┐     ┌──────────────┐     ┌──────────┐
│ payments │     │ showtime_    │     │  movies  │
├──────────┤     │ seats        │     ├──────────┤
│ id (PK)  │     ├──────────────┤     │ id (PK)  │
│ booking_id│    │ id (PK)      │     │ title    │
│ transactionId│  │ showtime_id  │     │ description│
│ amount   │     │ seat_id      │     │ posterUrl│
│ status   │     │ status       │     │ trailerUrl│
│ paymentMethod│  │ heldUntil    │     │ durationMinutes│
│ paidAt   │     │ holdByBookingId│   │ ageRating│
└──────────┘     └──────┬───────┘     │ genre    │
                        │             │ status   │
                        ▼             └──────────┘
                   ┌──────────┐
                   │  seats   │
                   ├──────────┤
                   │ id (PK)  │
                   │ seatCode │
                   │ seatType │
                   │ rowNumber│
                   │ colNumber│
                   │ auditorium_id│
                   │ active   │
                   └────┬─────┘
                        │
                        ▼
                   ┌──────────────┐
                   │ auditoriums  │
                   ├──────────────┤
                   │ id (PK)      │
                   │ name         │
                   │ totalRows    │
                   │ colsPerRow   │
                   │ cinema_id    │
                   │ active       │
                   └────┬─────────┘
                        │
                        ▼
                   ┌──────────┐
                   │ cinemas  │
                   ├──────────┤
                   │ id (PK)  │
                   │ name     │
                   │ address  │
                   │ phone    │
                   │ active   │
                   └──────────┘
```

### 4.2 SQL DDL (MySQL)

```sql
-- Rạp
CREATE TABLE cinemas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(200),
    phone VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
);

-- Phòng chiếu
CREATE TABLE auditoriums (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    total_rows INT NOT NULL,
    columns_per_row INT NOT NULL,
    cinema_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (cinema_id) REFERENCES cinemas(id)
);

-- Ghế
CREATE TABLE seats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seat_code VARCHAR(10) NOT NULL,
    seat_type VARCHAR(20) NOT NULL,
    row_number INT NOT NULL,
    column_number INT NOT NULL,
    auditorium_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (auditorium_id) REFERENCES auditoriums(id)
);

-- Phim
CREATE TABLE movies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    poster_url VARCHAR(500),
    trailer_url VARCHAR(500),
    duration_minutes INT,
    age_rating INT,
    genre VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
);

-- Suất chiếu
CREATE TABLE showtimes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    movie_id BIGINT NOT NULL,
    auditorium_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    base_price DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (movie_id) REFERENCES movies(id),
    FOREIGN KEY (auditorium_id) REFERENCES auditoriums(id)
);

-- Trạng thái ghế theo suất
CREATE TABLE showtime_seats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    showtime_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    held_until DATETIME,
    hold_by_booking_id VARCHAR(50),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    UNIQUE (showtime_id, seat_id),
    FOREIGN KEY (showtime_id) REFERENCES showtimes(id),
    FOREIGN KEY (seat_id) REFERENCES seats(id)
);

-- Users
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    phone VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
);

-- Roles
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

-- User-Roles
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Đơn đặt vé
CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_code VARCHAR(20) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    showtime_id BIGINT NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (showtime_id) REFERENCES showtimes(id)
);

-- Chi tiết ghế trong đơn
CREATE TABLE booking_seats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    showtime_seat_id BIGINT NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (booking_id) REFERENCES bookings(id),
    FOREIGN KEY (showtime_seat_id) REFERENCES showtime_seats(id)
);

-- Thanh toán
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    transaction_id VARCHAR(100),
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(30),
    paid_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

-- Vé điện tử
CREATE TABLE tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    ticket_code VARCHAR(20) NOT NULL UNIQUE,
    qr_code TEXT,
    status VARCHAR(20) NOT NULL,
    scanned_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

-- Khuyến mãi
CREATE TABLE promotions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    description VARCHAR(500),
    discount_type VARCHAR(20),
    discount_value DECIMAL(12,2),
    start_date DATETIME,
    end_date DATETIME,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
);
```

---

## 5. Entity & Repository

### 5.1 BaseEntity (common)

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### 5.2 Repository Pattern

Tất cả Repository extend `JpaRepository<T, Long>`:

```java
@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByStatusAndActiveTrue(MovieStatus status);
}

@Repository
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {
    List<ShowtimeSeat> findByShowtimeId(Long showtimeId);
}

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingCode(String bookingCode);
}
```

---

## 6. Bảo mật & Phân quyền

### 6.1 SecurityConfig

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/movies/**",
                        "/api/cinemas/**", "/api/showtimes/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/staff/**").hasAnyRole("STAFF", "ADMIN")
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

### 6.2 JWT Flow

1. User gửi `POST /api/auth/login` với username/password.
2. Backend xác thực → tạo JWT token (chứa userId, username, roles).
3. Trả token về frontend.
4. Frontend lưu token (localStorage/memory).
5. Mỗi request gửi kèm `Authorization: Bearer <token>`.
6. Backend filter parse JWT → set SecurityContext → phân quyền.

### 6.3 Role-based Access Control

```java
// Method-level security
@PreAuthorize("hasRole('ADMIN')")
public Movie createMovie(MovieRequest request) { ... }

@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
public Booking createStaffBooking(StaffBookingRequest request) { ... }

@PreAuthorize("hasRole('CUSTOMER')")
public Booking createOnlineBooking(BookingRequest request) { ... }
```

---

## 7. API Design

### 7.1 Response Format

Tất cả API trả về format chuẩn:

```json
{
  "success": true,
  "message": "Thành công",
  "data": { ... }
}
```

Lỗi:
```json
{
  "success": false,
  "message": "Ghế đã được đặt",
  "data": null
}
```

### 7.2 HTTP Status Codes

| Status | Ý nghĩa |
|---|---|
| 200 OK | Thành công |
| 201 Created | Tạo mới thành công |
| 400 Bad Request | Dữ liệu không hợp lệ |
| 401 Unauthorized | Chưa đăng nhập |
| 403 Forbidden | Không có quyền |
| 404 Not Found | Không tìm thấy |
| 409 Conflict | Xung đột (ghế đã bị đặt) |
| 500 Internal Server Error | Lỗi hệ thống |

### 7.3 Pagination

```
GET /api/movies?page=0&size=10&sort=title,asc
```

Response:
```json
{
  "success": true,
  "message": "Thành công",
  "data": {
    "content": [...],
    "totalPages": 5,
    "totalElements": 48,
    "size": 10,
    "number": 0
  }
}
```

---

## 8. Xử lý lỗi & Validation

### 8.1 GlobalExceptionHandler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(...) { ... }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(...) { ... }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(...) { ... }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(...) { ... }
}
```

### 8.2 Validation Annotations

```java
public class MovieRequest {
    @NotBlank(message = "Tên phim không được để trống")
    @Size(max = 200, message = "Tên phim tối đa 200 ký tự")
    private String title;

    @NotNull(message = "Thời lượng không được để trống")
    @Min(value = 1, message = "Thời lượng phải lớn hơn 0")
    private Integer durationMinutes;

    @NotNull(message = "Trạng thái không được để trống")
    private MovieStatus status;
}
```

### 8.3 Controller với @Valid

```java
@PostMapping("/api/admin/movies")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<MovieResponse>> createMovie(
        @Valid @RequestBody MovieRequest request) {
    MovieResponse response = movieService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok("Tạo phim thành công", response));
}
```

---

## 9. Giữ ghế & Concurrency

### 9.1 Vấn đề
Hai khách hàng có thể chọn cùng một ghế trong cùng một suất chiếu. Cần đảm bảo chỉ một người đặt thành công.

### 9.2 Giải pháp: Pessimistic Locking

```java
@Service
@Transactional
public class BookingService {

    public Booking createBooking(BookingRequest request) {
        // 1. Kiểm tra ghế còn trống (với lock)
        List<ShowtimeSeat> seats = showtimeSeatRepository
            .findByShowtimeIdAndSeatIdsWithLock(showtimeId, seatIds);

        for (ShowtimeSeat seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Ghế " + seat.getSeat().getSeatCode() + " đã được đặt");
            }
        }

        // 2. Giữ ghế (HELD)
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);
        for (ShowtimeSeat seat : seats) {
            seat.setStatus(SeatStatus.HELD);
            seat.setHeldUntil(expiresAt);
            seat.setHoldByBookingId(bookingCode);
        }
        showtimeSeatRepository.saveAll(seats);

        // 3. Tạo đơn PENDING
        Booking booking = Booking.builder()
            .bookingCode(bookingCode)
            .user(user)
            .showtime(showtime)
            .totalAmount(totalAmount)
            .status(BookingStatus.PENDING)
            .expiresAt(expiresAt)
            .build();
        bookingRepository.save(booking);

        return booking;
    }
}
```

### 9.3 Scheduled Job: Trả ghế khi hết hạn

```java
@Scheduled(fixedRate = 60000) // mỗi 1 phút
public void releaseExpiredHolds() {
    List<Booking> expiredBookings = bookingRepository
        .findByStatusAndExpiresAtBefore(BookingStatus.PENDING, LocalDateTime.now());

    for (Booking booking : expiredBookings) {
        booking.setStatus(BookingStatus.EXPIRED);
        // Trả ghế về AVAILABLE
        List<ShowtimeSeat> seats = showtimeSeatRepository
            .findByHoldByBookingId(booking.getBookingCode());
        for (ShowtimeSeat seat : seats) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setHeldUntil(null);
            seat.setHoldByBookingId(null);
        }
        showtimeSeatRepository.saveAll(seats);
        bookingRepository.save(booking);
    }
}
```

### 9.4 Repository với Lock

```java
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ShowtimeSeat s WHERE s.showtime.id = :showtimeId AND s.seat.id IN :seatIds")
    List<ShowtimeSeat> findByShowtimeIdAndSeatIdsWithLock(
        @Param("showtimeId") Long showtimeId,
        @Param("seatIds") List<Long> seatIds);
}
```

---

## 10. Thanh toán & Vé QR

### 10.1 Luồng thanh toán (MVP - Mock)

```
1. Frontend gọi POST /api/payments { bookingId, paymentMethod }
2. Backend tạo Payment (status = PROCESSING)
3. Backend giả lập xử lý (delay 2s)
4. Backend cập nhật Payment status:
   - SUCCESS → Booking status = CONFIRMED → Tạo Ticket (QR)
   - FAILED → Booking status = CANCELLED → Trả ghế về AVAILABLE
5. Trả kết quả về Frontend
```

### 10.2 Phát hành vé QR

```java
@Service
public class TicketService {

    public Ticket issueTicket(Booking booking) {
        String ticketCode = generateTicketCode(); // e.g., "TKT-20261027-001"
        String qrContent = buildQrContent(booking, ticketCode);
        String qrBase64 = generateQrCode(qrContent); // ZXing library

        Ticket ticket = Ticket.builder()
            .booking(booking)
            .ticketCode(ticketCode)
            .qrCode(qrBase64)
            .status(TicketStatus.UNUSED)
            .build();

        return ticketRepository.save(ticket);
    }

    private String buildQrContent(Booking booking, String ticketCode) {
        return String.format("CINEX|%s|%s|%s|%s|%s",
            ticketCode,
            booking.getShowtime().getMovie().getTitle(),
            booking.getShowtime().getAuditorium().getCinema().getName(),
            booking.getShowtime().getStartTime(),
            booking.getBookingSeats().stream()
                .map(bs -> bs.getShowtimeSeat().getSeat().getSeatCode())
                .collect(Collectors.joining(","))
        );
    }
}
```

### 10.3 Soát vé QR

```java
@Service
public class TicketValidationService {

    public ValidationResult validateTicket(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vé không tồn tại"));

        if (ticket.getStatus() == TicketStatus.SCANNED) {
            return ValidationResult.invalid("Vé đã được soát, không thể dùng lại");
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            return ValidationResult.invalid("Vé đã bị hủy");
        }

        // Kiểm tra đúng suất chiếu
        Showtime showtime = ticket.getBooking().getShowtime();
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(showtime.getStartTime().minusMinutes(30)) ||
            now.isAfter(showtime.getEndTime())) {
            return ValidationResult.invalid("Vé không hợp lệ cho suất chiếu này");
        }

        // Đánh dấu đã soát
        ticket.setStatus(TicketStatus.SCANNED);
        ticket.setScannedAt(now);
        ticketRepository.save(ticket);

        return ValidationResult.valid("Vé hợp lệ", ticket);
    }
}
```

---

## 11. Chatbot AI Integration

### 11.1 Architecture

```
User → Frontend (Chat UI) → Backend (Chatbot Controller) → AI Service (LLM)
                                    ↓
                              Backend Services (Movie, Showtime, Seat)
                                    ↓
                              MySQL Database
```

### 11.2 Chatbot Flow

1. User gửi tin nhắn.
2. Frontend gọi `POST /api/chatbot/message { message, sessionId }`.
3. Backend phân tích intent (tìm phim, tìm suất chiếu, hỏi ghế trống, etc.).
4. Backend gọi service tương ứng để lấy dữ liệu thật từ DB.
5. Backend gửi dữ liệu + context đến LLM để sinh câu trả lời tự nhiên.
6. Backend trả response về Frontend.
7. Nếu user xác nhận đặt vé → Backend tạo yêu cầu giữ ghế → trả booking code.

### 11.3 Intent Detection (MVP)

```java
@Service
public class ChatbotService {

    public ChatResponse processMessage(String message, String sessionId) {
        String intent = detectIntent(message);

        switch (intent) {
            case "SEARCH_MOVIE":
                return searchMovies(message);
            case "SEARCH_SHOWTIME":
                return searchShowtimes(message);
            case "CHECK_SEATS":
                return checkAvailableSeats(message);
            case "BOOK_TICKET":
                return initiateBooking(message, sessionId);
            default:
                return generalResponse(message);
        }
    }

    private String detectIntent(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("phim") && (lower.contains("tìm") || lower.contains("có"))) {
            return "SEARCH_MOVIE";
        }
        if (lower.contains("suất chiếu") || lower.contains("lịch chiếu")) {
            return "SEARCH_SHOWTIME";
        }
        if (lower.contains("ghế") && (lower.contains("còn") || lower.contains("trống"))) {
            return "CHECK_SEATS";
        }
        if (lower.contains("đặt vé") || lower.contains("mua vé")) {
            return "BOOK_TICKET";
        }
        return "GENERAL";
    }
}
```

### 11.4 Nguyên tắc quan trọng
- Chatbot **chỉ hiển thị thông tin** từ backend, không tự quyết định.
- Backend **kiểm chứng lại** suất chiếu, ghế, thanh toán trước khi xác nhận.
- Chatbot **không được tự báo "đặt vé thành công"** nếu hệ thống chưa xác nhận đơn.

---

## 12. Frontend Architecture

### 12.1 Axios Instance

```typescript
// api/axios.ts
import axios from 'axios';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
    timeout: 10000,
});

// Request interceptor: gắn JWT token
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// Response interceptor: xử lý 401
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem('token');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

export default api;
```

### 12.2 Auth Service

```typescript
// services/authService.ts
import api from './axios';

export const authService = {
    login: async (username: string, password: string) => {
        const res = await api.post('/auth/login', { username, password });
        localStorage.setItem('token', res.data.data.token);
        return res.data.data;
    },
    register: async (data: RegisterRequest) => {
        const res = await api.post('/auth/register', data);
        return res.data.data;
    },
    getMe: async () => {
        const res = await api.get('/auth/me');
        return res.data.data;
    },
    logout: () => {
        localStorage.removeItem('token');
    },
};
```

### 12.3 Protected Route

```typescript
// components/layout/ProtectedRoute.tsx
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

interface ProtectedRouteProps {
    roles?: string[];
}

export const ProtectedRoute = ({ roles }: ProtectedRouteProps) => {
    const { user, isAuthenticated } = useAuth();

    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    if (roles && !roles.some(role => user?.roles.includes(role))) {
        return <Navigate to="/" replace />;
    }

    return <Outlet />;
};
```

### 12.4 Seat Map Component

```typescript
// components/seat/SeatMap.tsx
interface SeatMapProps {
    seats: ShowtimeSeat[];
    selectedSeats: number[];
    onSeatSelect: (seatId: number) => void;
    maxSeats: number;
}

export const SeatMap = ({ seats, selectedSeats, onSeatSelect, maxSeats }: SeatMapProps) => {
    const getSeatClass = (seat: ShowtimeSeat) => {
        if (seat.status === 'BOOKED') return 'seat-booked';
        if (seat.status === 'HELD') return 'seat-held';
        if (selectedSeats.includes(seat.id)) return 'seat-selected';
        return 'seat-available';
    };

    return (
        <div className="seat-map">
            {seats.map((seat) => (
                <button
                    key={seat.id}
                    className={getSeatClass(seat)}
                    disabled={seat.status !== 'AVAILABLE'}
                    onClick={() => onSeatSelect(seat.id)}
                >
                    {seat.seat.seatCode}
                </button>
            ))}
        </div>
    );
};
```

---

## 13. Testing Strategy

### 13.1 Backend Testing

| Loại test | Tool | Mô tả |
|---|---|---|
| Unit test | JUnit 5 + AssertJ | Test Service, Repository |
| Integration test | Spring Boot Test + Testcontainers | Test với MySQL thật |
| API test | Postman / REST Assured | Test endpoints |

### 13.2 Test Cases quan trọng

1. **Giữ ghế đồng thời:** 2 user cùng giữ 1 ghế → chỉ 1 thành công.
2. **Hết hạn giữ ghế:** Ghế tự động trả về AVAILABLE sau 10 phút.
3. **Thanh toán thất bại:** Ghế trả về AVAILABLE, đơn bị CANCELLED.
4. **Vé QR dùng lại:** Vé đã SCANNED không thể dùng lại.
5. **Phân quyền:** User CUSTOMER không thể gọi API admin.

### 13.3 Frontend Testing

| Loại test | Tool | Mô tả |
|---|---|---|
| Unit test | Vitest + React Testing Library | Test components |
| E2E test | Cypress / Playwright | Test luồng đặt vé |

---

## 14. Deployment & DevOps

### 14.1 Development

```bash
# Backend
cd backend
mvn spring-boot:run

# Frontend
cd frontend
npm install
npm run dev
```

### 14.2 Production (gợi ý)

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Nginx     │     │  Spring Boot│     │   MySQL     │
│  (Reverse   │────►│  (Docker)   │────►│  (Docker)   │
│   Proxy)    │     │             │     │             │
└─────────────┘     └─────────────┘     └─────────────┘
     │
     ▼
┌─────────────┐
│  React SPA  │
│  (Static)   │
└─────────────┘
```

### 14.3 Docker Compose (gợi ý)

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8
    environment:
      MYSQL_DATABASE: cinex_db
      MYSQL_USER: cinex
      MYSQL_PASSWORD: cinex123
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/cinex_db

  frontend:
    build: ./frontend
    ports:
      - "3000:80"
    depends_on:
      - backend

volumes:
  mysql_data:
```

---

## 15. Quy ước Code

### 15.1 Java (Backend)

| Quy ước | Mô tả |
|---|---|
| Package | Theo domain/feature, không gom vào controller/service/repository phẳng |
| Lombok | `@Data`, `@RequiredArgsConstructor`, `@Builder` |
| Mapper | MapStruct cho DTO ↔ Entity |
| Injection | Constructor injection (`@RequiredArgsConstructor`) |
| Response | Trả về DTO, không trả entity JPA |
| Validation | `jakarta.validation` (`@Valid` + `@NotNull`...) |
| Exception | Tập trung ở `@RestControllerAdvice` |
| Naming | Tiếng Anh cho code, tiếng Việt cho comment/tài liệu |

### 15.2 TypeScript (Frontend)

| Quy ước | Mô tả |
|---|---|
| Components | PascalCase (MovieList.tsx) |
| Services | camelCase (movieService.ts) |
| Types | PascalCase (Movie.ts) |
| Hooks | camelCase với prefix "use" (useAuth.ts) |
| Utils | camelCase (formatDate.ts) |
| Styling | CSS Modules hoặc Tailwind |

### 15.3 Git Workflow

```
main (production)
  └── develop (integration)
       ├── feature/auth
       ├── feature/booking
       ├── feature/seat-selection
       └── feature/admin-crud
```

- Branch naming: `feature/<name>`, `bugfix/<name>`, `hotfix/<name>`
- Commit message: tiếng Anh, mô tả ngắn gọn
- PR: review trước khi merge vào develop

---

## Phụ lục: Checklist trước khi bắt đầu code

### Backend
- [ ] Cài đặt JDK 21
- [ ] Cài đặt Maven
- [ ] Cài đặt MySQL 8
- [ ] Tạo database `cinex_db`
- [ ] Tạo user `cinex` / `cinex123`
- [ ] Clone repo và mở trong IDE
- [ ] Chạy `mvn clean install`
- [ ] Chạy `mvn spring-boot:run`
- [ ] Kiểm tra `http://localhost:8080/api/movies`

### Frontend
- [ ] Cài đặt Node.js 20+
- [ ] Cài đặt npm/yarn
- [ ] Clone repo và mở trong IDE
- [ ] Chạy `npm install`
- [ ] Chạy `npm run dev`
- [ ] Kiểm tra `http://localhost:5173`

---

*— Hết tài liệu kỹ thuật chi tiết —*