# DeltaForceManager

三角洲行动游戏余额监控系统。通过设备上传游戏截图，利用 OCR 自动识别余额数值，记录余额变化趋势，支持多账号管理和报表查看。

## 技术栈

| 组件 | 版本 |
|------|------|
| Spring Boot | 3.5.0 |
| JDK | 17 |
| MyBatis-Plus | 3.5.10 |
| MySQL | 8.x |
| Tesseract OCR | 5.11.0 (tess4j) |
| SpringDoc OpenAPI | 2.8.6 |

## 核心功能

### 1. 游戏账号管理

管理多个游戏账号，每个账号绑定唯一设备标识（deviceToken），支持账号的增删改查、启停用。

- 表：`game_account`
- 字段：账号名称、游戏名称、设备ID、设备Token、设备型号、状态

### 2. 截图上传与 OCR 识别

设备通过 API 上传游戏截图，系统自动执行 OCR 流程提取余额：

1. **图片预处理**（`ImagePreprocessor`）：裁剪指定区域 → 放大 → 灰度化 → 对比度增强 → 二值化 → 可选反色 → 降噪
2. **OCR 识别**（`OcrPipeline`）：调用 Tesseract 引擎，限定白名单字符（`0-9.,KkMm万Ww`）
3. **文本解析**（`BalanceTextParser`）：清洗 OCR 文本，提取数值和单位（万/W/K/M/B），计算置信度
4. **结果判定**：置信度 >= 80% 自动入库；低于阈值进入人工审核队列

- 表：`game_screenshot_log`
- OCR 状态：待处理(0) / 成功(1) / 失败(2) / 待审核(3)

### 3. 余额记录

每次 OCR 成功识别或人工录入后，自动计算与上次记录的差额（balanceChange），生成余额变动记录。

- 表：`game_balance_record`
- 数据来源：OCR 自动识别(1) / 手动录入(2) / 人工校正(3)

### 4. OCR 配置

每个账号可独立配置 OCR 参数，适配不同游戏界面：

| 参数 | 说明 |
|------|------|
| cropX / cropY / cropWidth / cropHeight | 截图裁剪区域 |
| scaleFactor | 放大倍数 |
| thresholdValue | 二值化阈值 |
| invertColors | 是否反色（0/1） |
| tesseractPsm | Tesseract 页面分割模式 |
| unitSuffix | 期望单位后缀（如"万"） |

- 表：`game_ocr_config`

### 5. 报表统计

- **每日趋势**：指定账号和日期范围，查看每日开盘/收盘余额和日利润
- **多账号总览**：汇总所有启用账号的当前余额、日变动、在线状态
- **账号趋势**：单账号最近 N 天的余额走势
- **利润报表**：按周/月汇总各账号利润

### 6. 定时重试

`OcrRetryJob` 每 5 分钟扫描待处理的截图记录，自动重新提交 OCR 处理。

## API 接口

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 设备 | POST | `/api/device/upload` | 上传截图 |
| 设备 | GET | `/api/device/heartbeat` | 设备心跳 |
| 截图 | GET | `/api/screenshot/list` | 截图列表（分页） |
| 截图 | GET | `/api/screenshot/pendingReview` | 待审核列表 |
| 截图 | PUT | `/api/screenshot/review/{id}` | 人工审核 |
| 截图 | POST | `/api/screenshot/reprocess/{id}` | 重新OCR |
| 账号 | GET | `/api/account/list` | 账号列表（分页） |
| 账号 | POST | `/api/account/add` | 添加账号 |
| 账号 | PUT | `/api/account/edit` | 编辑账号 |
| 账号 | DELETE | `/api/account/delete` | 删除账号 |
| 账号 | GET | `/api/account/ocrConfig` | 获取OCR配置 |
| 账号 | POST | `/api/account/saveOcrConfig` | 保存OCR配置 |
| 余额 | GET | `/api/balance/list` | 余额记录（分页） |
| 余额 | POST | `/api/balance/manualInput` | 手动录入余额 |
| 余额 | DELETE | `/api/balance/delete` | 删除记录 |
| 报表 | GET | `/api/report/dailyTrend` | 每日趋势 |
| 报表 | GET | `/api/report/multiAccountSummary` | 多账号总览 |
| 报表 | GET | `/api/report/accountTrend` | 账号趋势 |
| 报表 | GET | `/api/report/profitSummary` | 利润报表 |

## 快速开始

### 环境要求

- JDK 17+
- MySQL 8.x
- Tesseract OCR（需安装并下载 tessdata 语言包）

### 配置

修改 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://<host>:<port>/<database>?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=Asia/Shanghai
    username: <username>
    password: <password>

game-monitor:
  tesseract:
    datapath: <tessdata目录路径>
    language: eng
  upload:
    path: <截图上传存储路径>
```

### 运行

```bash
mvn spring-boot:run
```

服务默认启动在 `http://localhost:8080`，Swagger 文档地址：`http://localhost:8080/swagger-ui.html`

## 项目结构

```
src/main/java/com/deltaforce/manager/
├── config/          # 配置类（MyBatis-Plus、线程池、Tesseract、Quartz）
├── constant/        # 常量定义
├── controller/      # REST 接口
├── dto/             # 数据传输对象
├── entity/          # 实体类
├── mapper/          # MyBatis-Plus Mapper
├── ocr/             # OCR 引擎（图片预处理、文本解析、识别流水线）
├── service/         # 业务逻辑
│   └── impl/
└── job/             # 定时任务

frontend/            # 前端项目（Vue 3）
├── src/
│   ├── api/         # API 接口层
│   ├── assets/      # 全局样式
│   ├── router/      # 路由配置
│   ├── utils/       # 工具函数（Axios 封装）
│   └── views/       # 页面组件
│       ├── dashboard/    # 总览面板
│       ├── account/      # 账号管理 + OCR 配置
│       ├── balance/      # 余额记录
│       ├── screenshot/   # 截图日志 + 审核
│       └── report/       # 报表统计
├── index.html
├── package.json
└── vite.config.js
```

## 前端

基于 Vue 3 + Vite 构建的管理后台，使用 Element Plus 组件库和 ECharts 图表。

### 技术栈

| 组件 | 版本 |
|------|------|
| Vue | 3.5+ |
| Vite | 6.x |
| Element Plus | 2.9+ |
| Axios | 1.7+ |
| ECharts | 5.5+ |
| Vue Router | 4.4+ |

### 页面说明

| 页面 | 路径 | 功能 |
|------|------|------|
| 总览面板 | `/dashboard` | 总余额/今日盈亏/在线数统计卡片，账号状态卡片，7天余额走势图 |
| 账号管理 | `/accounts` | 账号增删改查，生成设备Token，跳转OCR配置 |
| OCR 配置 | `/accounts/:id/ocr-config` | 配置裁剪区域、图像处理参数、Tesseract 模式 |
| 余额记录 | `/balances` | 分页查看余额变动，手动录入余额 |
| 截图日志 | `/screenshots` | 截图列表筛选，待审核队列，人工审核/重试OCR |
| 报表统计 | `/reports` | 每日趋势(柱状图)、利润报表(周/月)、账号走势(面积图) |

### 启动

```bash
cd frontend
npm install
npm run dev
```

开发模式启动在 `http://localhost:3000`，API 请求自动代理到后端 `http://localhost:8080`。

### 生产构建

```bash
npm run build
```

构建产物输出到 `frontend/dist/`，可部署到 Nginx 等静态服务器。
