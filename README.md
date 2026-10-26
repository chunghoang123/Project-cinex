# Cinex — Website đặt vé xem phim

Website đặt vé xem phim cho rạp tư nhân Cinex, hỗ trợ khách hàng đặt vé online, nhân viên bán vé tại quầy, admin quản trị hệ thống và chatbot AI hỗ trợ đặt vé.

## Cấu trúc dự án

```
project-Cinex/
├── backend/          # Spring Boot 3 + Java 21 + Gradle
├── frontend/         # React + TypeScript + Vite
├── docs/             # Tài liệu đặc tả (SRS, task list)
├── nginx/            # Cấu hình reverse proxy
├── docker-compose.yml
├── README.md
└── .gitignore
```

## Công nghệ

- **Frontend:** React 18 + TypeScript (Vite), React Router, Axios
- **Backend:** Java 21 + Spring Boot 3.2 + Gradle
- **Database:** MySQL 8 (mặc định) hoặc H2 in-memory cho môi trường local
- **Bảo mật:** Spring Security (JWT), phân quyền theo Role (`ROLE_ADMIN`, `ROLE_STAFF`, `ROLE_CUSTOMER`)
- **Giao diện:** CSS thuần, icon SVG inline, font Inter + Space Grotesk, responsive mobile-first với breakpoint desktop `1024px`

## Chức năng chính

### Khách hàng (storefront)

- Trang chủ, danh sách phim, chi tiết phim, lịch chiếu, tìm kiếm
- Chọn suất chiếu, sơ đồ ghế, bắp nước, xác nhận đặt vé
- Thanh toán demo, vé điện tử, lịch sử đặt vé, hồ sơ cá nhân
- Chatbot AI hỗ trợ tư vấn phim và đặt vé
- Giao diện mobile có bottom navigation 5 tab; desktop dùng top navigation và bố cục nhiều cột

> Luồng đặt vé của khách hàng hiện là demo frontend dùng `sessionStorage` + mock data. Backend đã có API đặt vé, giữ ghế, thanh toán và vé điện tử để tích hợp tiếp.

### Admin (`/admin`)

- Dashboard doanh thu, vé bán, đơn đặt vé, suất chiếu, khách hàng
- Quản lý phim: thêm, sửa, ẩn phim
- Quản lý rạp, phòng chiếu, sơ đồ ghế
- Quản lý suất chiếu theo ngày, rạp, phim
- Quản lý đơn đặt vé: tra cứu, xem chi tiết, hủy đơn
- Quản lý người dùng: tạo tài khoản, phân quyền, khóa/mở khóa

### Nhân viên bán vé (`/staff`)

- POS bán vé tại quầy: chọn suất chiếu, ghế, bắp nước, thanh toán tiền mặt/thẻ/chuyển khoản
- Phát hành vé điện tử ngay sau giao dịch, in vé
- Tra cứu đơn theo mã đơn, số điện thoại hoặc tên khách
- Soát vé tại cổng, chống soát trùng
- Tổng hợp doanh thu ca bán vé theo ngày

## Tài khoản demo

| Vai trò | Tên đăng nhập | Mật khẩu | Trang mặc định |
| --- | --- | --- | --- |
| Quản trị viên | `admin` | `Admin@123` | `/admin` |
| Nhân viên bán vé | `staff` | `Staff@123` | `/staff/pos` |
| Khách hàng | `customer` | `Customer@123` | `/` |
| Khách vãng lai (nội bộ) | `walkin` | `walkin123` | — |

## Chạy dự án

### 1. Backend với H2 local (khuyến nghị cho demo)

Yêu cầu Java 21.

```powershell
cd backend
.\gradlew.bat bootRun --args="--spring.profiles.active=local" --console=plain
```

Backend chạy tại `http://localhost:8080`. Dữ liệu demo được seed tự động mỗi lần khởi động:

- 3 cụm rạp, 6 phòng chiếu (8 × 12 ghế)
- 8 phim, 72 suất chiếu
- 10 đơn đặt vé mẫu
- Tài khoản admin/staff/customer/walkin

H2 dùng `create-drop`, nên dữ liệu sẽ được tạo lại khi restart.

### 2. Backend với MySQL (mặc định)

Tạo database `cinex` và cấu hình kết nối trong `backend/src/main/resources/application.properties` hoặc biến môi trường. Sau đó chạy:

```powershell
cd backend
.\gradlew.bat bootRun --console=plain
```

Nếu MySQL chưa sẵn sàng, dùng profile `local` ở trên.

### 3. Frontend

```powershell
cd frontend
npm install
npm run dev
```

Mở `http://localhost:5173`.

Các lệnh kiểm tra:

```powershell
npm run lint
npm run build
```

## API chính

- `POST /api/auth/login`, `POST /api/auth/register`, `GET /api/auth/me`
- `GET /api/movies`, `GET /api/movies/{id}`
- `GET /api/cinemas`, `GET /api/showtimes`
- `POST /api/bookings/hold`, `POST /api/bookings/{code}/confirm`, `POST /api/bookings/{code}/cancel`
- `GET /api/admin/dashboard`
- `GET/POST/PUT/DELETE /api/admin/movies`, `/api/admin/cinemas`, `/api/admin/showtimes`
- `GET /api/admin/bookings`, `POST /api/admin/bookings/{code}/cancel`
- `GET/POST/PATCH /api/admin/users`
- `GET /api/staff/showtimes`, `GET /api/staff/showtimes/{id}/seats`
- `POST /api/staff/sales`, `GET /api/staff/bookings`, `POST /api/staff/bookings/{code}/cancel`
- `POST /api/staff/tickets/validate`, `GET /api/staff/shift-summary`

## Tài liệu

- `docs/SRS_Cinex_from_template.docx` — Tài liệu đặc tả nghiệp vụ (SRS v1)
- `docs/SRS_Cinex.md` — Bản markdown của SRS
- `docs/TaskList_3Weeks.md` — Danh sách task 3 tuần cho 3 thành viên

## Nhóm phát triển

- Hoàng Văn Chung — Backend lead
- Đỗ Minh Tuyến — Frontend lead
- Vũ Nguyễn Hoàng Dương — Full-stack / Tích hợp / QA
