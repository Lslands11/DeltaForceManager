# =============================================
# 阶段 1: Node.js 构建前端
# =============================================
FROM node:20-alpine AS builder

WORKDIR /build

# 先复制依赖文件利用缓存
COPY package.json package-lock.json* ./
RUN npm install

# 复制源码并构建
COPY . .
RUN npm run build

# =============================================
# 阶段 2: Nginx 部署
# =============================================
FROM nginx:1.27-alpine

# 删除默认配置
RUN rm -f /etc/nginx/conf.d/default.conf

# 复制自定义 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制构建产物到 Nginx 静态目录
COPY --from=builder /build/dist /usr/share/nginx/html

EXPOSE 80
