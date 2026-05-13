# 云服务器部署完整指南（CentOS）

## 架构说明

```
用户浏览器
    │
    ▼
┌──────────┐       ┌──────────────┐       ┌──────────────────┐
│  Nginx   │──────▶│   Backend    │──────▶│  云 MySQL        │
│  :80/:443│       │  (Spring     │       │  47.110.91.251   │
│  反向代理 │       │   Boot)      │       │  :3306           │
│          │       │  :8080       │       └──────────────────┘
└──────────┘       └──────────────┘
  宿主机 Nginx        Docker 容器
```

> 部署方式：宿主机 Nginx 做反向代理 + Docker 运行后端。

---

## 第 1 步：连接云服务器

```bash
ssh root@你的服务器IP
```

---

## 第 2 步：安装 Docker

```bash
# 更新系统
yum update -y

# 安装 Docker（官方脚本自动识别 CentOS）
curl -fsSL https://get.docker.com | sh

# 启动 Docker 并设为开机自启
systemctl start docker
systemctl enable docker

# 验证安装
docker --version
```

---

## 第 3 步：安装 Nginx（如未安装）

```bash
yum install -y nginx
systemctl start nginx
systemctl enable nginx
```

---

## 第 4 步：上传项目代码

方式 A：**Git 克隆**（推荐）

```bash
# CentOS 可能需要先安装 git
yum install -y git

cd /opt
git clone https://github.com/你的用户名/DeltaForceManager.git
cd DeltaForceManager
```

方式 B：**本地打包上传**

在本地 PowerShell 执行：

```powershell
# 排除不需要的文件，打成 zip
Compress-Archive -Path "e:\learning\DeltaForceManager\*" `
  -DestinationPath "e:\DeltaForceManager.zip" `
  -CompressionLevel Optimal

# 上传到服务器
scp e:\DeltaForceManager.zip root@你的服务器IP:/opt/
```

在服务器上解压：

```bash
cd /opt
yum install -y unzip
unzip DeltaForceManager.zip -d DeltaForceManager
cd DeltaForceManager
```

---

## 第 5 步：创建配置文件

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑配置
vim .env
```

`.env` 内容修改为：

```env
# 后端数据库连接
DB_USERNAME=root
DB_PASSWORD=kskblzdjdwkzkbl

# JWT 密钥 —— 生产环境必须修改！
JWT_SECRET=请替换为随机字符串

# 前端端口（用 8088 避免与宿主机 Nginx 冲突）
FRONTEND_PORT=8088
```

---

## 第 6 步：构建并启动 Docker 容器

```bash
cd /opt/DeltaForceManager

# 构建镜像（首次约 3-5 分钟）
docker compose up -d --build
```

等待构建完成，检查状态：

```bash
# 查看容器运行状态
docker compose ps

# 查看后端日志
docker compose logs -f backend
```

看到 `Started Application` 和 `已创建默认管理员账号` 即表示启动成功。

---

## 第 7 步：配置 Nginx 反向代理

```bash
# 创建配置文件
vim /etc/nginx/conf.d/deltaforce.conf
```

写入以下配置：

```nginx
server {
    listen 80;
    server_name 你的域名;   # 没有域名就删掉此行

    client_max_body_size 20M;

    location / {
        proxy_pass http://127.0.0.1:8088;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8088;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

测试并重载 Nginx：

```bash
# 检查配置语法
nginx -t

# 重载生效
nginx -s reload
```

---

## 第 8 步：配置防火墙

CentOS 默认有防火墙，需要放通端口：

```bash
# 放通 HTTP
firewall-cmd --permanent --add-service=http

# 如果需要 HTTPS
firewall-cmd --permanent --add-service=https

# 重载防火墙
firewall-cmd --reload

# 查看已开放的端口
firewall-cmd --list-all
```

同时确保云服务器的 **安全组** 也放通了 80（HTTP）和 443（HTTPS）端口。

---

## 第 9 步：验证部署

在浏览器中访问 `http://你的服务器IP`，你应该能看到登录页面。

- **默认账号**：`admin` / `admin123`
- **登录后立即修改密码**

也可以通过命令验证：

```bash
# 测试前端
curl -I http://127.0.0.1:8088

# 测试后端 API
curl http://127.0.0.1:8088/api/auth/ping
```

---

## 第 10 步：配置 HTTPS（可选但推荐）

```bash
# 安装 certbot
yum install -y epel-release
yum install -y certbot python3-certbot-nginx

# 申请证书（需先有域名并解析到服务器IP）
certbot --nginx -d 你的域名

# 证书自动续期测试
certbot renew --dry-run
```

---

## 日常运维命令

```bash
# 查看容器状态
docker compose ps

# 查看实时日志
docker compose logs -f backend

# 重启服务
docker compose restart

# 停止服务
docker compose stop

# 更新代码后重新部署
cd /opt/DeltaForceManager
git pull
docker compose down
docker compose up -d --build

# 进入后端容器调试
docker compose exec backend bash

# 清理构建缓存（磁盘空间不足时）
docker system prune -f
```

---

## 故障排查

| 问题 | 排查命令 |
|------|---------|
| 构建失败 | `docker compose logs` 查看报错 |
| 后端无法连接数据库 | `docker compose exec backend curl 47.110.91.251:3306` 或检查数据库安全组 |
| 前端白屏 | `docker compose exec frontend cat /var/log/nginx/error.log` |
| 端口冲突 | `ss -tlnp \| grep :8088` |
| OCR 不工作 | `docker compose exec backend tesseract --version` |
| 防火墙拦截 | `firewall-cmd --list-all` |
