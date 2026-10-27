# 📋 TÀI LIỆU CHI TIẾT DỰ ÁN

## Website đặt vé xem phim — Rạp Cinex

> **Phiên bản:** 1.0 (DRAFT) | **Ngày:** 27/10/2026 | **Mã tài liệu:** SRS-CINEX-001

---

## MỤC LỤC

1. [Tổng quan dự án](#1-tổng-quan-dự-án)
2. [Vai trò và quyền hạn](#2-vai-trò-và-quyền-hạn)
3. [Chức năng chính](#3-chức-năng-chính)
4. [Luồng đặt vé trực tuyến](#4-luồng-đặt-vé-trực-tuyến)
5. [Chatbot AI](#5-chatbot-ai)
6. [Công nghệ đề xuất](#6-công-nghệ-đề-xuất)
7. [Cấu trúc dữ liệu (CSDL)](#7-cấu-trúc-dữ-liệu-csdl)
8. [Trạng thái hệ thống](#8-trạng-thái-hệ-thống)
9. [Nhóm API tham khảo](#9-nhóm-api-tham-khảo)
10. [Phân quyền](#10-phân-quyền)
11. [Site Map](#11-site-map)
12. [Các Use Case chi tiết](#12-các-use-case-chi-tiết)
13. [Components & Thông báo](#13-components--thông-báo)
14. [Phạm vi theo giai đoạn](#14-phạm-vi-theo-giai-đoạn)
15. [Tiêu chí nghiệm thu](#15-tiêu-chí-nghiệm-thu)
16. [Cấu trúc dự án (Codebase)](#16-cấu-trúc-dự-án-codebase)
17. [Phân công công việc (3 tuần)](#17-phân-công-công-việc-3-tuần)

---

## 1. Tổng quan dự án

### 1.1 Mục tiêu
Xây dựng website cho phép khách hàng:
- Xem phim, rạp và lịch chiếu.
- Chọn ghế và đặt vé trực tuyến.
- Thanh toán, nhận vé điện tử có mã QR.
- Dùng chatbot để tìm phim, suất chiếu và được hướng dẫn đặt vé.

Nhân viên có thể bán vé tại quầy và soát vé. Admin quản lý dữ liệu phim, phòng chiếu, lịch chiếu, tài khoản và báo cáo.

### 1.2 Giả định ban đầu
- Hệ thống có thể quản lý **một hoặc nhiều chi nhánh rạp**.
- Cổng thanh toán và chính sách đổi/hủy vé chưa được chọn → để cấu hình hoặc quyết định sau.
- Giai đoạn đầu dùng **thanh toán mock** (giả lập thành công/thất bại).

---

## 2. Vai trò và quyền hạn

### 2.1 Khách hàng (CUSTOMER)
- Xem và tìm kiếm phim theo tên, thể loại, trạng thái đang chiếu/sắp chiếu.
- Xem chi tiết phim: thời lượng, mô tả, ảnh, trailer, độ tuổi phù hợp.
- Chọn rạp, ngày, suất chiếu và ghế.
- Đăng ký, đăng nhập, cập nhật thông tin cá nhân.
- Đặt vé, thanh toán, xem mã QR và lịch sử đặt vé.
- Dùng chatbot để tìm suất chiếu và bắt đầu quy trình đặt vé.

### 2.2 Nhân viên bán vé (STAFF)
- Tìm suất chiếu và kiểm tra ghế trống.
- Tạo đơn bán vé trực tiếp tại quầy.
- Tra cứu đơn theo mã vé hoặc thông tin được phép.
- Quét QR để xác nhận vé hợp lệ và đánh dấu đã sử dụng.
- Xem các đơn hoặc suất chiếu thuộc phạm vi rạp được phân công.

### 2.3 Admin (ADMIN)
- Quản lý phim, rạp, phòng chiếu, sơ đồ ghế và lịch chiếu.
- Quản lý tài khoản, vai trò và quyền truy cập.
- Cấu hình giá vé, khuyến mãi và trạng thái suất chiếu.
- Tra cứu, hỗ trợ xử lý đơn theo chính sách.
- Xem báo cáo vé bán ra, doanh thu và hoạt động của nhân viên.

---

## 3. Chức năng chính

| Phân hệ | Chức năng |
|---|---|
| **Trang phim** | Danh sách phim, tìm kiếm, lọc theo thể loại/trạng thái, trang chi tiết phim |
| **Lịch chiếu** | Chọn rạp, ngày, suất chiếu; chỉ hiển thị suất có thể đặt |
| **Chọn ghế** | Sơ đồ ghế, trạng thái còn trống/đang giữ/đã đặt, giá theo loại ghế |
| **Đặt vé & thanh toán** | Tạo đơn, giữ ghế tạm thời, thanh toán, cập nhật kết quả và phát hành vé |
| **Vé điện tử** | Hiển thị mã QR, thông tin phim, rạp, phòng, giờ chiếu và ghế |
| **Bán vé tại quầy** | Nhân viên chọn suất và ghế, tạo đơn tại rạp, ghi nhận phương thức thanh toán tại quầy |
| **Soát vé** | Kiểm tra vé có tồn tại, đúng suất chiếu và chưa được sử dụng |
| **Quản trị** | CRUD phim, rạp, phòng, ghế, lịch chiếu, giá vé và tài khoản |
| **Báo cáo** | Thống kê số vé, doanh thu, suất chiếu và đơn theo khoảng thời gian |
| **Chatbot AI** | Tư vấn phim, tìm suất chiếu, hỗ trợ chọn ghế và chuyển khách sang bước xác nhận đặt vé |

---

## 4. Luồng đặt vé trực tuyến

1. Khách chọn phim.
2. Khách chọn rạp, ngày và suất chiếu.
3. Website tải sơ đồ ghế cùng trạng thái mới nhất.
4. Khách chọn ghế và xác nhận thông tin đơn.
5. Backend kiểm tra ghế còn trống rồi **giữ ghế tạm thời** (5–10 phút).
6. Khách thanh toán qua cổng thanh toán được chọn.
7. Backend xác minh kết quả thanh toán.
   - **Thành công** → đơn được xác nhận → hệ thống phát hành vé QR.
   - **Thất bại / hết thời gian giữ** → ghế được trả về trạng thái có thể đặt.
8. Đơn bán tại quầy cũng dùng chung dữ liệu suất chiếu, ghế và đơn vé → tránh bán trùng.

---

## 5. Chatbot AI

Chatbot làm **trợ lý** cho quy trình đặt vé, **không tự quyết định dữ liệu thật**. Ví dụ khách nhắn:

> "Tối nay có phim hành động nào ở rạp gần tôi không?"

Chatbot có thể hỏi thêm rạp hoặc khung giờ, sau đó gọi backend để tìm suất chiếu.

### Thao tác chatbot có thể hỗ trợ
- Tìm phim và suất chiếu.
- Hiển thị giá và ghế còn trống.
- Hướng dẫn khách chọn ghế.
- Tạo yêu cầu giữ ghế sau khi khách xác nhận.

### Nguyên tắc quan trọng
- Backend phải kiểm tra lại suất chiếu, ghế và kết quả thanh toán.
- Chatbot **không được tự báo "đặt vé thành công"** nếu hệ thống chưa xác nhận đơn.

---

## 6. Công nghệ đề xuất

| Thành phần | Công nghệ |
|---|---|
| **Frontend** | React + TypeScript (Vite) |
| **Backend** | Java 21 + Spring Boot 3.x + Maven |
| **Database** | MySQL 8 |
| **Bảo mật** | Spring Security (JWT), phân quyền theo Role |
| **ORM** | Spring Data JPA / Hibernate |
| **API** | REST JSON giữa React và Spring Boot |
| **Real-time** | WebSocket (bổ sung sau) — MVP dùng polling 5–10s |
| **Thanh toán** | Tích hợp qua backend (chọn nhà cung cấp sau) |

### Thư viện backend
- Lombok (getter/setter, constructor)
- MapStruct (mapper DTO ↔ Entity)
- Jakarta Validation (`@Valid`, `@NotNull`...)
- Spring Security + JWT
- Spring Data JPA + Hibernate
- MySQL Connector

### Thư viện frontend
- React Router (routing)
- Axios (HTTP client)
- TypeScript (type safety)

---

## 7. Cấu trúc dữ liệu (CSDL)

### 7.1 Các bảng chính

| Bảng | Mô tả |
|---|---|
| `users` | Thông tin tài khoản (khách, nhân viên, admin) |
| `roles` | Vai trò: ADMIN, STAFF, CUSTOMER |
| `user_roles` | Liên kết nhiều-nhiều user ↔ role |
| `cinemas` | Rạp / chi nhánh (tên, địa chỉ, điện thoại) |
| `auditoriums` | Phòng chiếu (tên, số hàng, số cột, thuộc rạp) |
| `seats` | Sơ đồ ghế cố định của từng phòng (mã ghế, loại ghế, hàng, cột) |
| `movies` | Thông tin phim (tên, mô tả, poster, trailer, thời lượng, độ tuổi, thể loại, trạng thái) |
| `showtimes` | Suất chiếu (phim, phòng, giờ bắt đầu/kết thúc, giá cơ bản, trạng thái) |
| `showtime_seats` | Trạng thái ghế theo từng suất chiếu |
| `bookings` | Đơn đặt vé (mã đơn, user, suất, tổng tiền, trạng thái, thời hạn hết hạn) |
| `booking_seats` | Chi tiết ghế trong đơn |
| `payments` | Giao dịch thanh toán (trạng thái: xử lý / thành công / thất bại / hoàn tiền) |
| `tickets` | Vé điện tử (mã QR, trạng thái: chưa dùng / đã soát / đã hủy) |
| `promotions` | Mã giảm giá / khuyến mãi (mở rộng sau) |

### 7.2 Quan hệ giữa các bảng

```
users (1) ── (N) bookings
bookings (1) ── (N) booking_seats
booking_seats (N) ── (1) showtime_seats
showtime_seats (N) ── (1) seats
showtime_seats (N) ── (1) showtimes
showtimes (N) ── (1) movies
showtimes (N) ── (1) auditoriums
auditoriums (N) ── (1) cinemas
seats (N) ── (1) auditoriums
bookings (1) ── (1) payments
bookings (1) ── (1) tickets
users (N) ── (N) roles (via user_roles)
```

---

## 8. Trạng thái hệ thống

### 8.1 Ghế (SeatStatus)
| Trạng thái | Mô tả |
|---|---|
| `AVAILABLE` | Còn trống |
| `HELD` | Đang giữ tạm (chưa thanh toán) |
| `BOOKED` | Đã đặt / đã thanh toán |

### 8.2 Đơn (BookingStatus)
| Trạng thái | Mô tả |
|---|---|
| `PENDING` | Chờ thanh toán |
| `CONFIRMED` | Đã xác nhận (thành công) |
| `EXPIRED` | Hết hạn giữ ghế |
| `CANCELLED` | Đã hủy |

### 8.3 Thanh toán (PaymentStatus)
| Trạng thái | Mô tả |
|---|---|
| `PROCESSING` | Đang xử lý |
| `SUCCESS` | Thành công |
| `FAILED` | Thất bại |
| `REFUNDED` | Hoàn tiền |

### 8.4 Vé (TicketStatus)
| Trạng thái | Mô tả |
|---|---|
| `UNUSED` | Chưa sử dụng |
| `SCANNED` | Đã soát |
| `CANCELLED` | Đã hủy |

### 8.5 Suất chiếu (ShowtimeStatus)
| Trạng thái | Mô tả |
|---|---|
| `ACTIVE` | Có thể đặt |
| `CANCELLED` | Đã hủy |
| `COMPLETED` | Đã chiếu xong |

### 8.6 Phim (MovieStatus)
| Trạng thái | Mô tả |
|---|---|
| `NOW_SHOWING` | Đang chiếu |
| `COMING_SOON` | Sắp chiếu |

---

## 9. Nhóm API tham khảo

### 9.1 Public (không cần đăng nhập)
| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/api/movies` | Danh sách phim |
| `GET` | `/api/movies/{id}` | Chi tiết phim |
| `GET` | `/api/cinemas` | Danh sách rạp |
| `GET` | `/api/showtimes` | Tìm suất chiếu theo phim, rạp, ngày |
| `GET` | `/api/showtimes/{id}/seats` | Lấy sơ đồ và trạng thái ghế |

### 9.2 Auth
| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/auth/register` | Đăng ký |
| `POST` | `/api/auth/login` | Đăng nhập (trả JWT) |
| `GET` | `/api/auth/me` | Thông tin user hiện tại |

### 9.3 Booking (cần đăng nhập)
| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/bookings/holds` | Yêu cầu giữ ghế |
| `POST` | `/api/bookings` | Tạo đơn |
| `POST` | `/api/payments` | Bắt đầu thanh toán |
| `POST` | `/api/payments/webhook` | Nhận cập nhật giao dịch từ cổng thanh toán |
| `GET` | `/api/bookings/history` | Lịch sử đặt vé |

### 9.4 Staff
| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/staff/bookings` | Nhân viên tạo đơn tại quầy |
| `POST` | `/api/staff/tickets/validate` | Kiểm tra và soát vé |
| `GET` | `/api/staff/bookings/search` | Tra cứu đơn |

### 9.5 Admin (`/api/admin/...`)
| Method | Endpoint | Mô tả |
|---|---|---|
| `GET/POST/PUT/DELETE` | `/api/admin/movies` | CRUD phim |
| `GET/POST/PUT/DELETE` | `/api/admin/cinemas` | CRUD rạp |
| `GET/POST/PUT/DELETE` | `/api/admin/auditoriums` | CRUD phòng chiếu |
| `GET/POST/PUT/DELETE` | `/api/admin/seats` | CRUD ghế |
| `GET/POST/PUT/DELETE` | `/api/admin/showtimes` | CRUD suất chiếu |
| `GET/POST/PUT/DELETE` | `/api/admin/users` | CRUD tài khoản |
| `GET` | `/api/admin/reports` | Báo cáo doanh thu |

> Các API quản trị và nhân viên cần kiểm tra quyền người dùng; không chỉ ẩn nút trên giao diện.

---

## 10. Phân quyền

### 10.1 Phân quyền chức năng

| Chức năng | Khách (CUSTOMER) | Nhân viên (STAFF) | Admin (ADMIN) |
|---|---|---|---|
| Xem phim, lịch chiếu | ✅ | ✅ | ✅ |
| Đặt vé & thanh toán | ✅ | ❌ | ❌ |
| Xem lịch sử đặt vé | ✅ (của mình) | ❌ | ✅ (tất cả) |
| Bán vé tại quầy | ❌ | ✅ | ✅ |
| Tra cứu đơn | ❌ | ✅ (phạm vi rạp) | ✅ (tất cả) |
| Soát vé QR | ❌ | ✅ | ✅ |
| CRUD phim/rạp/phòng/ghế/suất | ❌ | ❌ | ✅ |
| Quản lý tài khoản & phân quyền | ❌ | ❌ | ✅ |
| Xem báo cáo doanh thu | ❌ | ❌ (chỉ rạp mình) | ✅ (tất cả) |
| Chatbot AI | ✅ | ✅ | ✅ |

### 10.2 Phân quyền dữ liệu
- Nhân viên chỉ thao tác được trong phạm vi rạp được phân công.
- Admin có quyền trên toàn bộ dữ liệu.
- Khách hàng chỉ xem/thao tác dữ liệu của chính mình.

---

## 11. Site Map

```
Trang chủ
├── Phim (danh sách, chi tiết)
├── Lịch chiếu (chọn rạp, ngày, suất)
├── Chọn ghế
├── Xác nhận đơn
├── Thanh toán
├── Vé điện tử (QR)
├── Lịch sử đặt vé
├── Tài khoản (đăng ký, đăng nhập, cập nhật thông tin)
├── Nhân viên
│   ├── Bán vé tại quầy
│   ├── Tra cứu đơn
│   └── Soát vé QR
├── Admin
│   ├── Quản lý phim
│   ├── Quản lý rạp
│   ├── Quản lý phòng chiếu
│   ├── Quản lý ghế
│   ├── Quản lý suất chiếu
│   ├── Quản lý tài khoản
│   ├── Quản lý giá vé & khuyến mãi
│   └── Báo cáo doanh thu
└── Chatbot AI
```

---

## 12. Các Use Case chi tiết

### UC-01: Xem danh sách & chi tiết phim
- **Mã:** UC-01 | **Phân hệ:** Phim | **Tác nhân:** Khách, Nhân viên, Admin
- **Mô tả:** Xem danh sách phim, tìm kiếm theo tên, lọc theo thể loại/trạng thái. Chi tiết: thời lượng, mô tả, ảnh, trailer, độ tuổi.
- **Ưu tiên:** Cao | **Trigger:** Truy cập trang Phim | **Pre:** Không (công khai) | **Post:** Hiển thị danh sách/chi tiết
- **Luồng cơ bản:** 1. Tải danh sách phim → 2. Lọc/tìm kiếm → 3. Chọn phim → xem chi tiết
- **Ngoại lệ:** Không có dữ liệu → "Không có phim"
- **Business Rules:** Chỉ hiển thị phim active = true

### UC-02: Tìm suất chiếu theo phim/rạp/ngày
- **Mã:** UC-02 | **Phân hệ:** Lịch chiếu | **Tác nhân:** Khách, Nhân viên, Admin
- **Mô tả:** Chọn phim, rạp, ngày → trả danh sách suất chiếu (status = ACTIVE)
- **Ưu tiên:** Cao | **Trigger:** Chọn phim/rạp/ngày | **Pre:** Phim/rạp tồn tại | **Post:** Danh sách suất chiếu
- **Ngoại lệ:** Không có suất chiếu → "Không có suất chiếu"

### UC-03: Chọn ghế & xem trạng thái ghế
- **Mã:** UC-03 | **Phân hệ:** Ghế | **Tác nhân:** Khách, Nhân viên
- **Mô tả:** Hiển thị sơ đồ ghế (trống/đang giữ/đã đặt). Chọn ghế trống.
- **Ưu tiên:** Cao | **Trigger:** Chọn suất chiếu | **Pre:** Suất chiếu tồn tại & ACTIVE | **Post:** Danh sách ghế đã chọn
- **Ngoại lệ:** Ghế đã bị người khác chọn → không cho chọn
- **Business Rules:** Không chọn ghế HELD/BOOKED. Tối đa N ghế/đơn

### UC-04: Đặt vé & thanh toán online (giữ ghế tạm)
- **Mã:** UC-04 | **Phân hệ:** Đặt vé, Thanh toán | **Tác nhân:** Khách
- **Mô tả:** Chọn ghế → xác nhận đơn → backend giữ ghế tạm (5–10 phút) → thanh toán → xác nhận hoặc trả ghế
- **Ưu tiên:** Cao | **Trigger:** Bấm "Đặt vé" | **Pre:** Đã đăng nhập, ghế còn trống
- **Post:** Đơn CONFIRMED + vé QR (thành công) hoặc ghế về trống (thất bại/hết hạn)
- **Luồng cơ bản:** 1. Kiểm tra ghế trống → 2. Giữ ghế (HELD, expiresAt) → 3. Tạo đơn PENDING → 4. Thanh toán → 5. Xác minh → CONFIRMED → phát hành vé QR
- **Luồng thay thế:** Thanh toán mock (giả lập thành công/thất bại) trong MVP
- **Ngoại lệ:** Ghế đã bị đặt → báo lỗi. Thanh toán thất bại → ghế trả về trống. Hết hạn giữ → đơn EXPIRED
- **Business Rules:** Giữ ghế 5–10 phút. Không bán trùng ghế. Đơn chỉ xác nhận sau khi backend kiểm chứng thanh toán
- **NFR:** Xử lý đồng thời, tránh race condition (transaction/lock)

### UC-05: Nhận & xem vé điện tử QR
- **Mã:** UC-05 | **Phân hệ:** Vé | **Tác nhân:** Khách
- **Mô tả:** Sau thanh toán thành công, phát hành vé QR (phim, rạp, phòng, giờ, ghế). Xem/tải/print vé
- **Ưu tiên:** Cao | **Trigger:** Đơn CONFIRMED | **Pre:** Đơn đã xác nhận | **Post:** Vé QR được tạo
- **Business Rules:** Mỗi vé có mã duy nhất, QR chỉ dùng 1 lần

### UC-06: Xem lịch sử đặt vé
- **Mã:** UC-06 | **Phân hệ:** Đặt vé | **Tác nhân:** Khách, Admin
- **Mô tả:** Khách xem danh sách đơn của mình + chi tiết. Admin xem tất cả đơn
- **Ưu tiên:** Trung bình | **Trigger:** Truy cập "Lịch sử" | **Pre:** Đã đăng nhập
- **Business Rules:** Khách chỉ xem đơn của mình

### UC-07: Bán vé tại quầy (nhân viên)
- **Mã:** UC-07 | **Phân hệ:** Nhân viên | **Tác nhân:** Nhân viên, Admin
- **Mô tả:** Chọn suất, ghế, tạo đơn, ghi nhận thanh toán tiền mặt. Dùng chung dữ liệu ghế với online
- **Ưu tiên:** Cao | **Trigger:** Bấm "Bán vé" | **Pre:** Nhân viên đăng nhập, thuộc rạp
- **Post:** Đơn CONFIRMED + vé QR
- **Business Rules:** Không bán trùng ghế với online. Nhân viên chỉ thao tác trong rạp được phân công

### UC-08: Tra cứu đơn (nhân viên)
- **Mã:** UC-08 | **Phân hệ:** Nhân viên | **Tác nhân:** Nhân viên, Admin
- **Mô tả:** Tìm đơn theo mã vé hoặc thông tin khách (trong phạm vi quyền)
- **Ưu tiên:** Trung bình | **Trigger:** Nhập thông tin tìm kiếm | **Pre:** Đã đăng nhập
- **Business Rules:** Nhân viên chỉ xem đơn thuộc rạp mình

### UC-09: Soát vé QR (nhân viên)
- **Mã:** UC-09 | **Phân hệ:** Nhân viên | **Tác nhân:** Nhân viên, Admin
- **Mô tả:** Quét QR → kiểm tra tồn tại, đúng suất, chưa dùng → đánh dấu đã soát
- **Ưu tiên:** Cao | **Trigger:** Quét QR | **Pre:** Vé tồn tại | **Post:** Vé SCANNED
- **Ngoại lệ:** Vé không tồn tại / sai suất / đã dùng → báo lỗi
- **Business Rules:** Vé QR đã soát không thể dùng lại

### UC-10: Quản trị phim/rạp/phòng/ghế/suất chiếu (admin)
- **Mã:** UC-10 | **Phân hệ:** Admin | **Tác nhân:** Admin
- **Mô tả:** CRUD phim, rạp, phòng chiếu, ghế, suất chiếu, giá vé
- **Ưu tiên:** Cao | **Trigger:** Truy cập panel quản trị | **Pre:** Đăng nhập ADMIN
- **Business Rules:** Chỉ admin mới thao tác. Validate input đầy đủ

### UC-11: Quản lý tài khoản & phân quyền (admin)
- **Mã:** UC-11 | **Phân hệ:** Admin | **Tác nhân:** Admin
- **Mô tả:** Tạo/sửa/xóa tài khoản, gán vai trò, kích hoạt/vô hiệu hóa
- **Ưu tiên:** Cao | **Trigger:** Truy cập quản lý tài khoản | **Pre:** Đăng nhập ADMIN
- **Business Rules:** Không xóa tài khoản đang hoạt động (soft delete)

### UC-12: Xem báo cáo doanh thu (admin)
- **Mã:** UC-12 | **Phân hệ:** Admin | **Tác nhân:** Admin
- **Mô tả:** Thống kê số vé, doanh thu, suất chiếu, đơn theo khoảng thời gian
- **Ưu tiên:** Trung bình | **Trigger:** Chọn khoảng thời gian | **Pre:** Đã đăng nhập
- **Business Rules:** Dữ liệu theo thời gian thực

### UC-13: Chatbot AI hỗ trợ đặt vé
- **Mã:** UC-13 | **Phân hệ:** Chatbot | **Tác nhân:** Khách, Nhân viên, Admin
- **Mô tả:** Hỗ trợ tìm phim, suất chiếu, giá, ghế trống, hướng dẫn chọn ghế, tạo yêu cầu giữ ghế sau xác nhận. Backend kiểm chứng tất cả
- **Ưu tiên:** Trung bình (giai đoạn 4) | **Trigger:** Nhắn tin chatbot
- **Business Rules:** Chatbot không tự báo "đặt vé thành công" nếu hệ thống chưa xác nhận đơn

---

## 13. Components & Thông báo

### 13.1 Components dùng chung
- Button (primary/secondary/danger)
- Modal (confirm, form)
- Toast/Notification (success, error, warning, info)
- Table (phân trang, sắp xếp)
- Form input (text, select, date, number)
- Loading spinner, Skeleton
- Seat map component

### 13.2 Thông báo & cảnh báo
| Loại | Nội dung mẫu |
|---|---|
| Thành công | "Đặt vé thành công!", "Thanh toán thành công!" |
| Lỗi | "Ghế đã được đặt, vui lòng chọn ghế khác", "Thanh toán thất bại", "Phiên đăng nhập hết hạn" |
| Cảnh báo | "Ghế sẽ được giữ trong 10 phút", "Vé đã được soát, không thể dùng lại" |
| Thông tin | "Đang tải dữ liệu...", "Đang xử lý thanh toán..." |

---

## 14. Phạm vi theo giai đoạn

### Giai đoạn 1 — Nền tảng
- Tạo database và cấu trúc dự án
- Đăng ký, đăng nhập và phân quyền
- Admin quản lý phim, rạp, phòng, ghế và lịch chiếu

### Giai đoạn 2 — Khách đặt vé
- Trang phim và lịch chiếu
- Sơ đồ ghế
- Tạo đơn và xử lý giữ ghế
- Vé điện tử (ban đầu chạy luồng thanh toán thử nghiệm)

### Giai đoạn 3 — Nhân viên
- Giao diện bán vé tại quầy
- Tra cứu đơn
- Quét QR và đánh dấu vé đã sử dụng

### Giai đoạn 4 — Chatbot AI
- Hỏi đáp về phim và lịch chiếu
- Tìm suất chiếu thông qua backend
- Hỗ trợ chọn ghế và tạo yêu cầu đặt vé sau khi khách xác nhận

### Giai đoạn 5 — Hoàn thiện
- Báo cáo doanh thu và số vé
- Khuyến mãi, đổi/hủy vé theo chính sách
- Cập nhật ghế gần thời gian thực, kiểm thử tải và tối ưu giao diện

---

## 15. Tiêu chí nghiệm thu quan trọng

1. **Người dùng chỉ xem được chức năng phù hợp với vai trò của mình.**
2. **Hai khách không thể đặt thành công cùng một ghế trong cùng một suất chiếu.**
3. **Ghế được trả lại khi đơn hết hạn hoặc thanh toán thất bại.**
4. **Vé QR đã soát không thể dùng lại.**
5. **Nhân viên chỉ thao tác được trong phạm vi quyền được cấp.**
6. **Đơn chỉ được xác nhận sau khi backend nhận và kiểm chứng kết quả thanh toán.**
7. **Chatbot chỉ hiển thị thông tin suất chiếu và ghế dựa trên kết quả từ hệ thống.**

---

## 16. Cấu trúc dự án (Codebase)

### 16.1 Công nghệ & kiến trúc
- **Backend:** Spring Boot 3, Java 21, Maven
- **CSDL:** MySQL (utf8mb4)
- **Bảo mật:** Spring Security (phân quyền theo Role: ADMIN / STAFF / CUSTOMER), JWT
- **Truy xuất dữ liệu:** Spring Data JPA / Hibernate
- **API:** REST trả JSON
- **Xử lý lỗi:** `@RestControllerAdvice` (tập trung)
- **Validation:** `jakarta.validation`
- **FE:** React + TypeScript (tách biệt, gọi API qua CORS)

### 16.2 Cấu trúc package (theo domain/feature)

```
com.cinex.cinema
├── common            # BaseEntity, ApiResponse, GlobalExceptionHandler
├── config            # SecurityConfig, CORS, các bean cấu hình
├── user              # User, Role, ERole, UserRepository, RoleRepository
├── cinema            # Cinema, Auditorium, Seat (+ repositories)
├── movie             # Movie, MovieStatus, MovieRepository
├── showtime          # Showtime, ShowtimeSeat, SeatStatus, ShowtimeStatus (+ repositories)
├── booking           # Booking, BookingStatus, BookingRepository
├── payment           # (để trống, tích hợp cổng thanh toán sau)
├── ticket            # (để trống, mã QR và trạng thái soát vé sau)
├── promotion         # (để trống, khuyến mãi sau)
└── staff             # (để trống, chức năng bán quầy, soát vé sau)
```

### 16.3 Cấu trúc thư mục dự án

```
project-Cinex/
├── backend/          # Spring Boot (Java 21 + Maven)
│   ├── pom.xml
│   └── src/main/java/com/cinex/cinema/...
├── frontend/         # React + TypeScript
│   ├── package.json
│   └── src/...
├── docs/             # Tài liệu đặc tả
│   ├── SRS_Cinex_from_template.docx
│   ├── Template-Technical.xlsx
│   └── PROJECT_DETAIL.md
├── README.md
└── .gitignore
```

### 16.4 Các bảng CSDL chính

| Bảng | Mô tả |
|---|---|
| `users` | Tài khoản người dùng |
| `roles` | Vai trò (ADMIN, STAFF, CUSTOMER) |
| `user_roles` | Liên kết user ↔ role |
| `cinemas` | Rạp / chi nhánh |
| `auditoriums` | Phòng chiếu |
| `seats` | Sơ đồ ghế cố định |
| `movies` | Thông tin phim |
| `showtimes` | Suất chiếu |
| `showtime_seats` | Trạng thái ghế theo suất |
| `bookings` | Đơn đặt vé |
| `booking_seats` | Chi tiết ghế trong đơn |
| `payments` | Giao dịch thanh toán |
| `tickets` | Vé điện tử & mã QR |
| `promotions` | Khuyến mãi (mở rộng sau) |

---

## 17. Phân công công việc (3 tuần)

### 17.1 Tổng quan

| Tuần | Mục tiêu chính |
|---|---|
| **Tuần 1** | Nền tảng: setup project, CSDL, auth, admin CRUD phim/rạp/phòng/ghế/suất chiếu |
| **Tuần 2** | Khách đặt vé: trang phim, lịch chiếu, sơ đồ ghế, giữ ghế, đơn, vé QR, thanh toán thử nghiệm |
| **Tuần 3** | Nhân viên (bán quầy, soát vé QR) + báo cáo cơ bản + kiểm thử & sửa lỗi |

### 17.2 Phân công theo người

#### Hoàng Văn Chung — Backend lead (Spring Boot + CSDL)
- **Tuần 1:** Setup Spring Boot, thiết kế CSDL, Entity + Repository, Spring Security, AuthController, RoleSeeder
- **Tuần 2:** CRUD phim/rạp/phòng/ghế/suất chiếu, BookingService (giữ ghế), PaymentController (mock), TicketService (QR)
- **Tuần 3:** StaffBookingController, TicketValidateController, ReportController, kiểm thử API, hỗ trợ tích hợp

#### Đỗ Minh Tuyến — Frontend lead (React + TypeScript)
- **Tuần 1:** Setup React + TS, routing, auth pages, layout, trang chủ, danh sách/chi tiết phim, danh sách rạp
- **Tuần 2:** Trang lịch chiếu, sơ đồ ghế, xác nhận đơn, thanh toán, vé QR, lịch sử đặt vé
- **Tuần 3:** Admin CRUD (phim, rạp, phòng, ghế, suất chiếu, tài khoản), báo cáo, responsive UI, kiểm thử

#### Vũ Nguyễn Hoàng Dương — Full-stack / Tích hợp / QA
- **Tuần 1:** Setup Git, ERD, seed data, DTO + Mapper, unit test, components dùng chung
- **Tuần 2:** Trang bán vé quầy, tra cứu đơn, soát vé QR, tích hợp FE ↔ BE, xử lý lỗi tích hợp
- **Tuần 3:** Chatbot AI (MVP), kiểm thử E2E, sửa lỗi, chuẩn bị demo

### 17.3 Tổng effort

| Người | Tuần 1 | Tuần 2 | Tuần 3 | Tổng |
|---|---|---|---|---|
| Hoàng Văn Chung | 7 ngày | 7 ngày | 7 ngày | **21 ngày** |
| Đỗ Minh Tuyến | 7 ngày | 7 ngày | 7 ngày | **21 ngày** |
| Vũ Nguyễn Hoàng Dương | 7 ngày | 7 ngày | 7 ngày | **21 ngày** |

### 17.4 Lưu ý quan trọng
1. Tuần 1 là nền tảng — ưu tiên hoàn thành auth + CRUD admin trước.
2. Tuần 2 là lõi nghiệp vụ — Chung phải hoàn thành API booking/payment/ticket đúng hạn.
3. Tuần 3 chatbot (Dương) là phần mở rộng — nếu không kịp, hoãn sang sau.
4. Thanh toán giai đoạn đầu dùng mock.
5. Real-time ghế (WebSocket) để giai đoạn sau — MVP dùng polling.
6. Chatbot chỉ hiển thị thông tin từ backend, không tự báo "đặt vé thành công".

---

## Phụ lục: Thuật ngữ viết tắt

| STT | Từ viết tắt | Diễn giải |
|---|---|---|
| 1 | UC | Use Case — trường hợp sử dụng |
| 2 | QR | Quick Response — mã phản hồi nhanh (vé điện tử) |
| 3 | POS | Point of Sale — bán vé tại quầy |
| 4 | JWT | JSON Web Token — xác thực |
| 5 | MVP | Minimum Viable Product — sản phẩm khả dụng tối thiểu |
| 6 | API | Application Programming Interface |
| 7 | CRUD | Create, Read, Update, Delete |
| 8 | STT | Số thứ tự |

---

*— Hết tài liệu chi tiết dự án —*