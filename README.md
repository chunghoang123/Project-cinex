# Cinex — Website đặt vé xem phim

Website đặt vé xem phim cho rạp tư nhân Cinex, hỗ trợ khách hàng đặt vé online, nhân viên bán vé tại quầy, admin quản trị hệ thống và chatbot AI hỗ trợ đặt vé.

## Cấu trúc dự án

```
project-Cinex/
├── backend/          # Spring Boot 3 + Java 21 + MySQL
├── frontend/         # React + TypeScript
├── docs/             # Tài liệu đặc tả (SRS, task list)
├── README.md
└── .gitignore
```

## Công nghệ

- **Frontend:** React + TypeScript (Vite)
- **Backend:** Java 21 + Spring Boot 3 + Maven
- **Database:** MySQL 8
- **Bảo mật:** Spring Security (JWT), phân quyền theo Role (ADMIN / STAFF / CUSTOMER)

## Tài liệu

- `docs/SRS_Cinex_from_template.docx` — Tài liệu đặc tả nghiệp vụ (SRS v1)
- `docs/SRS_Cinex.md` — Bản markdown của SRS
- `docs/TaskList_3Weeks.md` — Danh sách task 3 tuần cho 3 thành viên

## Giai đoạn phát triển

1. **Nền tảng** — CSDL, auth, admin CRUD phim/rạp/phòng/ghế/suất chiếu
2. **Khách đặt vé** — Trang phim, lịch chiếu, sơ đồ ghế, giữ ghế, đơn, vé QR, thanh toán (mock)
3. **Nhân viên** — Bán vé tại quầy, tra cứu đơn, soát vé QR
4. **Chatbot AI** — Hỏi đáp, tìm suất chiếu, hỗ trợ chọn ghế
5. **Hoàn thiện** — Báo cáo, khuyến mãi, real-time ghế, kiểm thử

## Nhóm phát triển

- Hoàng Văn Chung — Backend lead
- Đỗ Minh Tuyến — Frontend lead
- Vũ Nguyễn Hoàng Dương — Full-stack / Tích hợp / QA