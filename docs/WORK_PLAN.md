# 🗂️ KẾ HOẠCH PHÂN CÔNG CÔNG VIỆC — CineX

> **Phiên bản 2.0** | Ngày cập nhật: 12/11/2026
> Thay thế mục §17 (phân công theo tuần) trong `PROJECT_DETAIL.md` và bảng chia theo ngày 115 đầu việc trong `Template-Technical.xlsx`.

---

## 1. Nguyên tắc phân công mới

1. **Không gắn việc theo thời gian** (không chia tuần/sprint/ngày cụ thể). Việc được chia theo **module nghiệp vụ**, bám sát cấu trúc codebase thật của dự án.
2. **Chia đều 3 người**: mỗi người **20 đầu việc ≈ 20 ngày công** (tổng 60 đầu việc / 60 ngày công).
3. Mỗi module có **1 người chủ trì** (chịu trách nhiệm chính) và **1–2 người hỗ trợ** (phối hợp, nhận chuyển giao nếu trễ).
4. Thứ tự thực hiện theo **lớp phụ thuộc L1 → L4** (mục 4). Mỗi người tự xếp ngày làm việc riêng trong lớp mình phụ trách, tuân thủ điều kiện chuyển lớp.
5. Giữ quy ước cũ: **không làm việc cuối tuần**.
6. Nghiệm thu theo **§15 – Tiêu chí nghiệm thu** trong `PROJECT_DETAIL.md` (liên kết ở mục 7).

**Thành viên & vai trò:**

| Thành viên | Vai trò chính | Phạm vi phụ trách |
|---|---|---|
| Hoàng Văn Chung | Backend Lead | Nền tảng BE, bảo mật, phim/rạp/suất, đặt vé, thanh toán, vé QR, POS BE |
| Đỗ Minh Tuyến | Frontend Lead | Toàn bộ giao diện người dùng + admin, chat UI, responsive |
| Vũ Nguyễn Hoàng Dương | Full-stack / Tích hợp / QA | Seed, tích hợp TMDB, quản trị BE, báo cáo, chatbot BE, kiểm thử, bàn giao |

---

## 2. Bảng phân công theo module

| Mã | Module | Phạm vi | Chủ trì | Hỗ trợ |
|---|---|---|---|---|
| INF | Hạ tầng & CSDL | Setup BE/FE, CSDL 14 bảng, seed data, Git/CI, Docker | Chung | Dương |
| SEC | Bảo mật & Xác thực | JWT, RBAC, OTP reset password, auth FE | Chung | Tuyến (FE) |
| MOV | Phim & Nội dung | CRUD phim, API public, TMDB/trailer, trang phim | Chung (BE) / Tuyến (FE) | Dương (TMDB) |
| CIN | Rạp & Phòng & Ghế | CRUD rạp/phòng/ghế, API public | Chung (BE) | Tuyến (FE) |
| ST | Suất chiếu | CRUD suất, sinh trạng thái ghế theo suất | Chung (BE) | Tuyến (FE) |
| BK | Đặt vé & Giữ ghế | Booking, pessimistic lock, trả ghế hết hạn, sơ đồ ghế | Chung (BE) | Tuyến (FE), Dương (API ghế) |
| PAY | Thanh toán | Mock payment, xác minh kết quả, trang thanh toán | Chung (BE) | Tuyến (FE) |
| TKT | Vé & QR | Phát hành vé, sinh QR, soát vé dùng 1 lần | Chung (BE) | Tuyến (FE), Dương (FE soát) |
| POS | Bán vé tại quầy | StaffBooking API, POS, tra cứu đơn, ca làm việc | Chung (BE) | Tuyến (POS FE), Dương (còn lại FE) |
| ADM | Quản trị | CRUD tài khoản, gán role, dashboard | Dương (BE) | Tuyến (FE) |
| RPT | Báo cáo | API số vé/doanh thu, trang báo cáo KPI + biểu đồ | Dương (BE) | Tuyến (FE) |
| AI | Chatbot | API tìm suất/ghế, giữ ghế qua chat, chat UI | Dương (BE) | Tuyến (FE) |
| QA | Kiểm thử & UAT | Unit test, test kịch bản, E2E, UAT, sửa lỗi | Dương | Chung (BE-fix), Tuyến (FE-fix) |
| OPS | Vận hành & Bàn giao | Deployment, tài liệu, demo | Dương | Chung (deploy), Tuyến (UI polish) |

---

## 3. WBS — 60 đầu việc

> Cột "Ước lượng" tính bằng ngày công. Mỗi đầu việc hoàn thành khi demo được + PR đã merge.

### 3.1 INF — Hạ tầng & CSDL (6 việc)

| Mã | Đầu việc | Chủ trì | Hỗ trợ | Phụ thuộc | Ước lượng |
|---|---|---|---|---|---|
| INF-01 | Setup Spring Boot 3 (Java 21, Maven), cấu hình `application.properties`, cấu trúc package theo domain | Chung | Dương | — | 1.0 |
| INF-02 | Setup React + TypeScript (Vite), routing, cấu trúc `pages/components/services/api`, ESLint | Tuyến | — | — | 1.0 |
| INF-03 | Thiết kế CSDL 14 bảng: DDL, quan hệ, index, `UNIQUE(showtime_id, seat_id)` | Chung | Dương | INF-01 | 1.0 |
| INF-04 | Seed data: roles, users mẫu, rạp, phòng, ghế, phim, suất chiếu | Dương | Chung | INF-03 | 1.0 |
| INF-05 | Git workflow (branch, PR), CI build + test, README | Dương | — | INF-01 | 1.0 |
| INF-06 | Docker Compose BE + FE + MySQL, cấu hình biến môi trường prod | Chung | Dương | INF-03 | 0.5 |

### 3.2 SEC — Bảo mật & Xác thực (8 việc)

| Mã | Đầu việc | Chủ trì | Hỗ trợ | Phụ thuộc | Ước lượng |
|---|---|---|---|---|---|
| SEC-01 | `common`: BaseEntity, ApiResponse, pagination, `GlobalExceptionHandler` tập trung | Chung | — | INF-01 | 1.0 |
| SEC-02 | `SecurityConfig`: SecurityFilterChain, CORS, PasswordEncoder (BCrypt) | Chung | — | SEC-01 | 1.0 |
| SEC-03 | JWT: sinh/verify token, filter xác thực, xử lý token hết hạn | Chung | Dương | SEC-02 | 1.0 |
| SEC-04 | RBAC: `@PreAuthorize` theo role ADMIN/STAFF/CUSTOMER, giới hạn STAFF theo rạp | Chung | Dương | SEC-03 | 1.0 |
| SEC-05 | FE: axios instance + interceptor (gắn token, xử lý 401), ProtectedRoute | Tuyến | — | SEC-03 | 1.0 |
| SEC-06 | FE: trang Login, Register, Profile + validate form | Tuyến | — | SEC-05 | 1.0 |
| SEC-07 | BE: quên mật khẩu — gửi OTP qua email, token reset, API đặt lại | Chung | — | SEC-03 | 1.0 |
| SEC-08 | FE: trang ForgotPassword, ResetPassword (nhập OTP) | Tuyến | — | SEC-07 | 1.0 |

### 3.3 MOV — Phim & Nội dung (5 việc)

| Mã | Đầu việc | Chủ trì | Hỗ trợ | Phụ thuộc | Ước lượng |
|---|---|---|---|---|---|
| MOV-01 | BE: entity/repo Movie + API public (danh sách, chi tiết, tìm kiếm, lọc) | Chung | — | SEC-01 | 1.0 |
| MOV-02 | BE: admin CRUD phim, đồng bộ poster khi nhập phim | Chung | Dương | MOV-01 | 1.0 |
| MOV-03 | FE: trang Home, MovieList, MovieDetail, Search + trailer | Tuyến | — | MOV-01 | 1.0 |
| MOV-04 | FE: AdminMoviesPage (CRUD + modal/table dùng chung) | Tuyến | — | MOV-02 | 1.5 |
| MOV-05 | Gói tích hợp TMDB + trailer (packages `tmdb`, `trailer`) | Dương | Chung | MOV-01 | 1.0 |

### 3.4 CIN — Rạp & Phòng & Ghế (3 việc)

| Mã | Đầu việc | Chủ trì | Hỗ trợ | Phụ thuộc | Ước lượng |
|---|---|---|---|---|---|
| CIN-01 | BE: entity Cinema/Auditorium/Seat + API public danh sách rạp/phòng | Chung | — | SEC-01 | 1.0 |
| CIN-02 | BE: admin CRUD rạp, phòng, sơ đồ ghế | Chung | Dương | CIN-01 | 1.0 |
| CIN-03 | FE: AdminCinemasPage (rạp → phòng → ghế theo cấp) | Tuyến | — | CIN-02 | 1.0 |

### 3.5 ST — Suất chiếu (4 việc)

| Mã | Đầu việc | Chủ trì | Hỗ trợ | Phụ thuộc | Ước lượng |
|---|---|---|---|---|---|
| ST-01 | BE: Showtime + ShowtimeSeat, sinh trạng thái ghế khi tạo suất | Chung | Dương | CIN-02 | 1.0 |
| ST-02 | BE: admin CRUD suất chiếu + validate trùng phòng/thời gian | Chung | — | ST-01 | 1.0 |
| ST-03 | FE: ShowtimeList — chọn rạp, ngày, suất còn đặt được | Tuyến | — | ST-01 | 1.0 |
| ST-04 | FE: AdminShowtimesPage | Tuyến | — | ST-02 | 1.0 |

### 3.6 BK — Đặt vé & Giữ ghế (6 việc)

| Mã | Đầu việc | Chủ trì | Hỗ trợ | Phụ thuộc | Ước lượng |
|---|---|---|---|---|---|
| BK-01 | BE: Booking + BookingSeat, tạo đơn PENDING | Chung | — | ST-01 | 1.0 |
| BK-02 | BE: giữ ghế pessimistic lock, `expiresAt`, scheduled job trả ghế mỗi 1 phút | Chung | — | BK-01 | 1.0 |
| BK-03 | BE: API trạng thái ghế theo suất (polling FE 5–10 giây) | Dương | Chung | BK-02 | 1.0 |
| BK-04 | FE: SeatSelection — sơ đồ ghế, polling, chọn/bỏ chọn, tổng tiền | Tuyến | — | BK-03 | 1.0 |
| BK-05 | FE: BookingConfirm — tóm tắt phim/rạp/suất/ghế, gửi tạo đơn | Tuyến | — | BK-04 | 0.5 |
| BK-06 | FE: BookingHistory + AdminBookingsPage | Tuyến | Dương | BK-01 | 1.0 |

### 3.7 PAY — Thanh toán (2 việc)

| Mã | Đầu việc | Chủ trì | Hỗ trợ | Phụ thuộc | Ước lượng |
|---|---|---|---|---|---|
| PAY-01 | BE: mock payment PROCESSING → SUCCESS/FAILED, xác minh kết quả, vòng đời đơn CONFIRMED/EXPIRED/CANCELLED | Chung | Dương | BK-01 | 1.5 |
| PAY-02 | FE: trang Payment + PaymentReturn | Tuyến | — | PAY-01 | 1.0 |

### 3.8 TKT — Vé & QR (4 việc)

| Mã | Đầu việc | Chủ trì | Hỗ trợ | Phụ thuộc | Ước lượng |
|---|---|---|---|---|---|
| TKT-01 | BE: phát hành vé sau khi thanh toán thành công | Chung | — | PAY-01 | 1.0 |
| TKT-02 | BE: sinh mã QR + API soát vé (đúng suất, chưa dùng, dùng 1 lần) | Chung | — | TKT-01 | 1.0 |
| TKT-03 | FE: TicketView — vé điện tử, tải/in vé | Tuyến | — | TKT-01 | 1.0 |
| TKT-04 | FE: StaffValidatePage — quét/nhập QR, hiển thị kết quả soát | Dương | — | TKT-02 | 1.0 |

### 3.9 POS — Bán vé tại quầy (4 việc)

| Mã | Đầu việc | Chủ trì | Hỗ trợ | Phụ thuộc | Ước lượng |
|---|---|---|---|---|---|
| POS-01 | BE: StaffBookingController — bán quầy dùng chung dữ liệu suất/ghế với online | Chung | — | BK-02 | 1.0 |
| POS-02 | FE: StaffPosPage — chọn suất, chọn ghế, tạo đơn, thanh toán tiền mặt | Tuyến | — | POS-01 | 1.5 |
| POS-03 | FE: StaffBookingsPage — tra cứu đơn theo mã vé/thông tin khách | Dương | — | POS-01 | 1.0 |
| POS-04 | FE: StaffShiftPage — ca làm việc của nhân viên | Dương | — | SEC-04 | 1.0 |

### 3.10 ADM — Quản trị (3 việc)

| Mã | Đầu việc | Chủ trì | Hỗ trợ | Phụ thuộc | Ước lượng |
|---|---|---|---|---|---|
| ADM-01 | BE: CRUD tài khoản, gán/thu hồi role, khóa/mở khóa tài khoản | Dương | Chung | SEC-04 | 1.5 |
| ADM-02 | FE: AdminUsersPage — danh sách, tìm kiếm, form gán role | Tuyến | — | ADM-01 | 1.0 |
| ADM-03 | FE: AdminDashboardPage — tổng quan hệ thống | Dương | — | ADM-01 | 1.0 |

### 3.11 RPT — Báo cáo (2 việc)

| Mã | Đầu việc | Chủ trì | Hỗ trợ | Phụ thuộc | Ước lượng |
|---|---|---|---|---|---|
| RPT-01 | BE: API thống kê số vé, doanh thu, tổng hợp theo thời gian/suất chiếu | Dương | Chung | PAY-02 | 1.5 |
| RPT-02 | FE: AdminReportsPage — KPI, biểu đồ, bảng, lọc thời gian, đối chiếu số liệu | Tuyến | — | RPT-01 | 1.0 |

### 3.12 AI — Chatbot (4 việc)

| Mã | Đầu việc | Chủ trì | Hỗ trợ | Phụ thuộc | Ước lượng |
|---|---|---|---|---|---|
| AI-01 | BE: API chatbot — intent detection, tìm phim/suất/ghế từ dữ liệu thật | Dương | Chung | MOV-01, ST-01 | 1.5 |
| AI-02 | FE: khung chat (`components/chat`), câu hỏi gợi ý, hiển thị kết quả | Tuyến | — | AI-01 | 1.0 |
| AI-03 | Chatbot tạo yêu cầu giữ ghế sau khi khách xác nhận | Dương | — | AI-01, BK-02 | 1.0 |
| AI-04 | Ràng buộc chatbot: không tự báo "đặt vé thành công", chỉ dùng dữ liệu backend + test | Dương | Chung | AI-03 | 0.5 |

### 3.13 QA — Kiểm thử & UAT (5 việc)

| Mã | Đầu việc | Chủ trì | Hỗ trợ | Phụ thuộc | Ước lượng |
|---|---|---|---|---|---|
| QA-01 | Unit test BE: service giữ ghế, thanh toán, phát hành vé | Dương | Chung | BK-02, PAY-01 | 1.0 |
| QA-02 | Test kịch bản: 2 khách đặt 1 ghế, hết hạn trả ghế, vé dùng lại, phân quyền API | Dương | Chung | QA-01 | 1.0 |
| QA-03 | E2E: đặt vé online, bán quầy POS, soát vé QR | Dương | Tuyến | POS-02, TKT-04 | 1.0 |
| QA-04 | Chạy UAT theo §15, ghi nhận và phân loại lỗi | Dương | Chung, Tuyến | QA-03 | 1.0 |
| QA-05 | Sửa lỗi FE sau UAT: responsive, trạng thái loading/empty/error | Tuyến | — | QA-04 | 1.0 |

### 3.14 OPS — Vận hành & Bàn giao (4 việc)

| Mã | Đầu việc | Chủ trì | Hỗ trợ | Phụ thuộc | Ước lượng |
|---|---|---|---|---|---|
| OPS-01 | Cấu hình môi trường triển khai, cập nhật `DEPLOYMENT_GUIDE.md` | Dương | Chung | INF-06 | 1.0 |
| OPS-02 | Cập nhật tài liệu: SRS, hướng dẫn sử dụng | Dương | — | QA-04 | 0.5 |
| OPS-03 | Kịch bản demo + dữ liệu demo | Dương | — | QA-03 | 0.5 |
| OPS-04 | Polish UI chung: header, toast, modal, icon, trạng thái rỗng | Tuyến | — | QA-05 | 1.0 |

---

## 4. Lớp phụ thuộc (thứ tự thực hiện)

| Lớp | Đầu việc | Điều kiện chuyển lớp |
|---|---|---|
| **L1 — Nền tảng** | INF-01 → INF-05, SEC-01 → SEC-03, SEC-05 | CI xanh, đăng nhập/đăng ký được qua JWT |
| **L2 — Dữ liệu & Quản trị** | SEC-04, SEC-06, SEC-07, SEC-08, MOV-01→05, CIN-01→03, ST-01→04, ADM-01, ADM-02 | Admin tạo được phim → rạp → phòng → ghế → suất chiếu đầy đủ |
| **L3 — Lõi nghiệp vụ** | BK-01→06, PAY-01→02, TKT-01→04, POS-01→04, ADM-03, RPT-01, RPT-02, AI-01, AI-02 | Đặt vé online end-to-end ra vé QR, bán quầy được, báo cáo ra số |
| **L4 — Hoàn thiện** | AI-03, AI-04, QA-01→05, OPS-01→04, INF-06 | Đạt đủ 7 tiêu chí nghiệm thu §15 |

Lưu ý: INF-06 (Docker/prod) có thể làm song song ở L3 nếu cần deploy sớm để test tích hợp.

---

## 5. Tổng hợp theo người

| Thành viên | Số đầu việc | Ngày công | Phạm vi chính |
|---|---|---|---|
| **Hoàng Văn Chung** | 20 | 20.0 | Hạ tầng BE, CSDL, bảo mật JWT/RBAC, OTP reset; BE của phim, rạp/suất, đặt vé + giữ ghế, thanh toán, vé QR, POS |
| **Đỗ Minh Tuyến** | 20 | 20.0 | Toàn bộ FE người dùng (phim, lịch chiếu, sơ đồ ghế, thanh toán, vé QR, lịch sử), FE admin (phim, rạp, suất, tài khoản, báo cáo), chat UI, responsive |
| **Vũ Nguyễn Hoàng Dương** | 20 | 20.0 | Seed data, Git/CI, TMDB/trailer, API trạng thái ghế, BE quản trị tài khoản + báo cáo, chatbot, POS FE phụ, toàn bộ kiểm thử/UAT, deployment, tài liệu, demo |

Cân bằng: **20 việc – 20 ngày công mỗi người** (tổng 60 việc / 60 ngày công), khớp dư địa ~21 ngày công/người của dự án 3 tuần.

---

## 6. Quy tắc phối hợp

1. **Việc FE phụ thuộc API**: chủ trì API thông báo xong qua merge request; trước đó FE dùng mock data (`frontend/src/data`).
2. **PR bắt buộc qua CI** (build + test xanh) mới merge; chủ trì module review PR của người hỗ trợ trong module đó.
3. Nếu 1 đầu việc trễ hơn dự kiến, **người hỗ trợ trong module nhận tiếp** — ưu tiên giữ tiến độ lớp phụ thuộc (L2 chặn L3, L3 chặn L4).
4. Mỗi người **tự cập nhật trạng thái** (To Do / In Progress / Done) ngay khi đổi việc trên Jira; báo cáo tổng hợp theo trạng thái, không theo mốc ngày.
5. Việc có ước lượng ≥ 1.5 ngày công nên tách nhỏ thành PR riêng để review nhanh.

---

## 7. Liên kết tiêu chí nghiệm thu (§15) → đầu việc chịu trách nhiệm

| Tiêu chí | Đầu việc đảm bảo |
|---|---|
| 1. Người dùng chỉ xem chức năng đúng vai trò | SEC-03, SEC-04, SEC-05 |
| 2. Hai khách không đặt thành công cùng một ghế | BK-02, QA-02 |
| 3. Trả ghế khi hết hạn / thanh toán thất bại | BK-02, PAY-01, QA-02 |
| 4. Vé QR đã soát không dùng lại | TKT-02, QA-02 |
| 5. Nhân viên thao tác đúng phạm vi được cấp | SEC-04, POS-01 |
| 6. Đơn chỉ confirm sau khi BE kiểm chứng thanh toán | PAY-01, QA-02 |
| 7. Chatbot chỉ hiển thị dữ liệu từ hệ thống | AI-01, AI-04 |

---

*File này là nguồn chuẩn về phân công. Khi cần đồng bộ lại Jira hoặc `Template-Technical.xlsx`, lấy danh sách 60 đầu việc ở mục 3 làm danh sách gốc.*
