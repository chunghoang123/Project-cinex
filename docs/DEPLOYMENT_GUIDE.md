# 🚀 HƯỚNG DẪN TRIỂN KHAI (DEPLOYMENT GUIDE)

## Website đặt vé xem phim — Rạp Cinex

---

## 1. Thông tin Server

| Thông tin | Giá trị |
|---|---|
| Domain | `chung-ai.io.vn` |
| Server IP | `100.115.192.85` |
| SSH User | `chunghoang` |
| SSH Password | `<your-ssh-password>` |
| MySQL User | `cinex` |
| MySQL Password | `<your-mysql-password>` |
| Docker Hub User | `chunghoang123` |
| Docker Hub Token | `<your-docker-token>` |

---

## 2. Chuẩn bị Server

### 2.1 Cài đặt Docker & Docker Compose

```bash
# Cài Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Cài Docker Compose
sudo apt-get install docker-compose-plugin

# Thêm user vào group docker
sudo usermod -aG docker chunghoang

# Kiểm tra
docker --version
docker compose version
```

### 2.2 Cài đặt Git

```bash
sudo apt-get update
sudo apt-get install git
```

### 2.3 Clone repository

```bash
cd /opt
sudo git clone https://github.com/YOUR_USERNAME/project-Cinex.git cinex
sudo chown -R chunghoang:chunghoang cinex
cd cinex
```

### 2.4 Cấu hình SSL (Let's Encrypt)

```bash
# Cài Certbot
sudo apt-get install certbot

# Tạo SSL certificate
sudo certbot certonly --standalone -d chung-ai.io.vn -d www.chung-ai.io.vn

# Copy certificates vào nginx/ssl
sudo mkdir -p nginx/ssl
sudo cp /etc/letsencrypt/live/chung-ai.io.vn/fullchain.pem nginx/ssl/
sudo cp /etc/letsencrypt/live/chung-ai.io.vn/privkey.pem nginx/ssl/
sudo chown -R chunghoang:chunghoang nginx/ssl
```

### 2.5 Cấu hình biến môi trường

```bash
cp .env.example .env
nano .env
```

Điền các giá trị thực:
```env
MYSQL_ROOT_PASSWORD=<your-mysql-password>
MYSQL_DATABASE=cinex_db
MYSQL_USER=cinex
MYSQL_PASSWORD=<your-mysql-password>
SPRING_PROFILES_ACTIVE=prod
APP_JWT_SECRET=<your-jwt-secret>
APP_JWT_EXPIRATION=86400000
VITE_API_URL=https://chung-ai.io.vn/api
```

---

## 3. Triển khai (Manual Deploy)

```bash
cd /opt/cinex

# Build và chạy
docker-compose up -d --build

# Kiểm tra trạng thái
docker-compose ps

# Xem logs
docker-compose logs -f backend
docker-compose logs -f frontend

# Health check
curl https://chung-ai.io.vn/actuator/health
```

### 3.1 Cập nhật code (re-deploy)

```bash
cd /opt/cinex
git pull origin main
docker-compose down
docker-compose up -d --build
docker system prune -f
```

---

## 4. Cấu hình GitHub Secrets (cho CD Pipeline)

Truy cập: **GitHub Repo → Settings → Secrets and variables → Actions → New repository secret**

| Secret Name | Value |
|---|---|
| `SERVER_HOST` | `100.115.192.85` |
| `SERVER_USER` | `chunghoang` |
| `SSH_PRIVATE_KEY` | *(nội dung private key — xem bên dưới)* |
| `VITE_API_URL` | `https://chung-ai.io.vn/api` |
| `HEALTH_CHECK_URL` | `https://chung-ai.io.vn/actuator/health` |

### SSH Private Key (lưu vào GitHub Secrets)

```
-----BEGIN OPENSSH PRIVATE KEY-----
<your-ssh-private-key-here>
-----END OPENSSH PRIVATE KEY-----
```

> ⚠️ **Lưu ý bảo mật:** Private key này chỉ dùng cho deploy. Không commit vào repo.

---

## 5. Cấu hình Docker Hub (cho CI Pipeline)

Truy cập: **GitHub Repo → Settings → Secrets and variables → Actions → New repository secret**

| Secret Name | Value |
|---|---|
| `DOCKER_USERNAME` | `chunghoang123` |
| `DOCKER_TOKEN` | `<your-docker-token>` |

---

## 6. Kiểm tra sau deploy

```bash
# Health check
curl https://chung-ai.io.vn/actuator/health

# API test
curl https://chung-ai.io.vn/api/movies

# Frontend
curl -I https://chung-ai.io.vn/

# Database
docker exec -it cinex-mysql mysql -u cinex -p<your-mysql-password> cinex_db -e "SHOW TABLES;"
```

---

## 7. Troubleshooting

### Backend không khởi động
```bash
docker-compose logs backend
# Kiểm tra MySQL connection
docker exec -it cinex-backend curl http://localhost:8080/actuator/health
```

### Frontend không load
```bash
docker-compose logs frontend
# Kiểm tra Nginx config
docker exec -it cinex-nginx nginx -t
```

### Database connection refused
```bash
# Kiểm tra MySQL đang chạy
docker-compose ps mysql
# Kiểm tra network
docker network inspect cinex_cinex-network
```

### SSL certificate hết hạn
```bash
sudo certbot renew
sudo cp /etc/letsencrypt/live/chung-ai.io.vn/fullchain.pem /opt/cinex/nginx/ssl/
sudo cp /etc/letsencrypt/live/chung-ai.io.vn/privkey.pem /opt/cinex/nginx/ssl/
docker-compose restart nginx
```

---

## 8. Monitoring & Maintenance

### 8.1 Xem logs
```bash
# Tất cả services
docker-compose logs -f

# Chỉ backend
docker-compose logs -f backend

# Chỉ frontend
docker-compose logs -f frontend
```

### 8.2 Backup database
```bash
docker exec cinex-mysql mysqldump -u cinex -p<your-mysql-password> cinex_db > backup_$(date +%Y%m%d).sql
```

### 8.3 Restore database
```bash
docker exec -i cinex-mysql mysql -u cinex -p<your-mysql-password> cinex_db < backup_20261027.sql
```

### 8.4 Cập nhật SSL tự động (Cron)
```bash
# Thêm vào crontab
0 0 1 * * certbot renew --quiet && cp /etc/letsencrypt/live/chung-ai.io.vn/*.pem /opt/cinex/nginx/ssl/ && docker-compose -f /opt/cinex/docker-compose.yml restart nginx
```

---

*— Hết hướng dẫn triển khai —*