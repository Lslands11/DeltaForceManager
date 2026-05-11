# 手游搬砖余额监控系统 — 技术设计方案

## 一、背景与目标

手游搬砖业务，5+ 台安卓手机分别运行不同游戏账号，需要每天 2-3 个时段自动记录游戏币余额变化。

**痛点:** 手动截图查看效率低，无法自动化统计收益。

**目标:** 搭建 JavaWeb 后台，自动获取截图 → OCR 识别余额 → 入库 → 可视化报表。

---

## 二、整体架构

```
┌─────────────────────────┐         ┌─────────────────────────────────────┐
│   Android 手机 × N      │         │         JavaWeb Server              │
│                         │         │        (JeecgBoot 3.8.2)            │
│  Tasker / MacroDroid    │  HTTP   │                                     │
│  ┌───────────────────┐  │  POST   │  ┌───────────────────────────┐      │
│  │ 定时截图 (3次/天) │──┼────────▶│  │ ScreenshotUploadController│      │
│  └───────────────────┘  │         │  └──────────┬────────────────┘      │
│                         │         │             │                       │
│  device_token 认证      │         │             ▼                       │
│                         │         │  ┌───────────────────┐              │
└─────────────────────────┘         │  │  存储截图到 OSS   │              │
                                    │  └──────────┬────────┘              │
                                    │             │ 异步                   │
                                    │             ▼                       │
                                    │  ┌───────────────────────────┐      │
                                    │  │     OCR 识别流水线        │      │
                                    │  │                           │      │
                                    │  │ 裁剪→放大→灰度→二值化→OCR │      │
                                    │  │      → 金额解析           │      │
                                    │  └──────────┬────────────────┘      │
                                    │             │                       │
                                    │             ▼                       │
                                    │  ┌───────────────────┐              │
                                    │  │  余额入库 (MySQL) │              │
                                    │  └──────────┬────────┘              │
                                    │             │                       │
                                    │             ▼                       │
                                    │  ┌───────────────────────────┐      │
                                    │  │  可视化报表 Dashboard     │      │
                                    │  │  趋势图 / 收益 / 汇总    │      │
                                    │  └───────────────────────────┘      │
                                    └─────────────────────────────────────┘
```

---

## 三、模块设计

### 新建模块: `globe-game-monitor`

位置: `jeecg-boot-module/globe-game-monitor/`

### 包结构

```
org.jeecg.modules.gamemonitor
├── constant/         # 常量定义
├── config/           # Tesseract 配置
├── controller/       # 5 个 Controller
│   ├── ScreenshotUploadController.java   # [公开] 设备截图上传
│   ├── GameAccountController.java        # [后台] 账号管理
│   ├── BalanceRecordController.java      # [后台] 余额记录
│   ├── ScreenshotLogController.java      # [后台] 截图日志/审核
│   └── ReportController.java            # [后台] 报表数据
├── dto/              # 传输对象
├── entity/           # 4 个实体
│   ├── GameAccount.java
│   ├── GameOcrConfig.java
│   ├── GameScreenshotLog.java
│   └── GameBalanceRecord.java
├── mapper/           # MyBatis-Plus Mapper
├── service/          # 6 个 Service 接口 + 实现
├── ocr/              # OCR 核心处理
│   ├── ImagePreprocessor.java
│   ├── BalanceTextParser.java
│   └── OcrPipeline.java
└── job/              # 定时任务
    └── OcrRetryJob.java
```

---

## 四、数据库设计

### 4.1 `game_account` — 游戏账号表

```sql
CREATE TABLE `game_account` (
  `id`            bigint       NOT NULL  COMMENT '主键',
  `account_name`  varchar(64)  NOT NULL  COMMENT '账号名称',
  `game_name`     varchar(64)  DEFAULT NULL COMMENT '游戏名称',
  `device_id`     varchar(128) NOT NULL  COMMENT '设备标识',
  `device_token`  varchar(128) NOT NULL  COMMENT '上传鉴权Token',
  `device_model`  varchar(64)  DEFAULT NULL COMMENT '设备型号',
  `status`        tinyint      NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
  `remark`        varchar(255) DEFAULT NULL COMMENT '备注',
  `create_by`     varchar(64)  DEFAULT NULL,
  `create_time`   datetime     DEFAULT NULL,
  `update_by`     varchar(64)  DEFAULT NULL,
  `update_time`   datetime     DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_token` (`device_token`),
  KEY `idx_device_id` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏账号';
```

### 4.2 `game_ocr_config` — OCR 配置表

每个账号独立配置裁剪坐标和 OCR 参数，适配不同手机分辨率/游戏界面。

```sql
CREATE TABLE `game_ocr_config` (
  `id`              bigint       NOT NULL  COMMENT '主键',
  `account_id`      bigint       NOT NULL  COMMENT '关联账号ID',
  `crop_x`          int          NOT NULL DEFAULT 0    COMMENT '裁剪X坐标',
  `crop_y`          int          NOT NULL DEFAULT 0    COMMENT '裁剪Y坐标',
  `crop_width`      int          NOT NULL DEFAULT 200  COMMENT '裁剪宽度',
  `crop_height`     int          NOT NULL DEFAULT 60   COMMENT '裁剪高度',
  `threshold_value` int          NOT NULL DEFAULT 128  COMMENT '二值化阈值',
  `invert_colors`   tinyint      NOT NULL DEFAULT 0    COMMENT '是否反色',
  `scale_factor`    decimal(3,1) NOT NULL DEFAULT 2.0  COMMENT '放大倍数',
  `tesseract_psm`   int          NOT NULL DEFAULT 7    COMMENT 'PSM模式(7=单行)',
  `unit_suffix`     varchar(16)  DEFAULT '万'          COMMENT '金额单位后缀',
  `create_time`     datetime     DEFAULT NULL,
  `update_time`     datetime     DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_id` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OCR配置';
```

### 4.3 `game_screenshot_log` — 截图日志表

```sql
CREATE TABLE `game_screenshot_log` (
  `id`              bigint        NOT NULL  COMMENT '主键',
  `account_id`      bigint        NOT NULL  COMMENT '账号ID',
  `original_url`    varchar(512)  NOT NULL  COMMENT '原图OSS路径',
  `cropped_url`     varchar(512)  DEFAULT NULL COMMENT '裁剪图路径',
  `upload_time`     datetime      NOT NULL  COMMENT '上传时间',
  `ocr_status`      tinyint       NOT NULL DEFAULT 0 COMMENT '0=待处理 1=成功 2=失败 3=待审核',
  `ocr_raw_text`    varchar(128)  DEFAULT NULL COMMENT 'OCR原始文本',
  `ocr_confidence`  decimal(5,2)  DEFAULT NULL COMMENT '置信度(0-100)',
  `parsed_amount`   decimal(18,2) DEFAULT NULL COMMENT '解析金额',
  `manual_amount`   decimal(18,2) DEFAULT NULL COMMENT '人工修正金额',
  `error_message`   varchar(512)  DEFAULT NULL COMMENT '错误信息',
  `create_time`     datetime      DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_upload_time` (`upload_time`),
  KEY `idx_ocr_status` (`ocr_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='截图日志';
```

### 4.4 `game_balance_record` — 余额记录表

```sql
CREATE TABLE `game_balance_record` (
  `id`                bigint        NOT NULL  COMMENT '主键',
  `account_id`        bigint        NOT NULL  COMMENT '账号ID',
  `screenshot_log_id` bigint        DEFAULT NULL COMMENT '关联截图ID',
  `balance`           decimal(18,2) NOT NULL  COMMENT '余额',
  `balance_change`    decimal(18,2) DEFAULT NULL COMMENT '环比变化量',
  `record_time`       datetime      NOT NULL  COMMENT '记录时间',
  `source`            tinyint       NOT NULL DEFAULT 1 COMMENT '1=OCR 2=手动 3=修正',
  `create_time`       datetime      DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_account_time` (`account_id`, `record_time`),
  KEY `idx_record_time` (`record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余额记录';
```

---

## 五、核心技术方案

### 5.1 截图获取 — Tasker/MacroDroid 自动化

**手机端配置:**
- 触发器: 每天 08:00、14:00、22:00
- 动作: 截图 → HTTP POST 到服务器

**Tasker 配置示例:**
```
Action: HTTP Request
  Method: POST
  URL: https://<你的域名>/gameMonitor/device/upload
  Headers: Content-Type: multipart/form-data
  Body:
    device_token = "预注册的Token"
    screenshot = file:///sdcard/Screenshots/latest.png
```

**MacroDroid 配置示例:**
```
Trigger: Time of Day (08:00, 14:00, 22:00)
Action 1: Take Screenshot
Action 2: HTTP Request (POST, multipart)
  URL: https://<你的域名>/gameMonitor/device/upload
  Form Data: device_token + screenshot file
```

**服务端接口:**
```
POST /gameMonitor/device/upload
Content-Type: multipart/form-data

Parameters:
  - device_token: String (必填, 设备认证)
  - screenshot: File (必填, 截图文件)

Response:
  { "success": true, "result": { "screenshotId": "1234567890" } }
```

- 免登录: Shiro 配置 `/gameMonitor/device/**` 为 anon
- 认证: device_token 匹配 game_account 表记录
- 接收后: 存 Aliyun OSS → 异步触发 OCR

---

### 5.2 OCR 识别 — Tesseract 本地识别

**依赖:** tess4j 5.11.0 (Tesseract 的 Java 封装)

#### OCR 流水线

```
原始截图 (1080×2400)
    │
    ▼
① 裁剪余额区域 (根据 game_ocr_config 配置)
    │  例: crop_x=800, crop_y=50, crop_width=200, crop_height=60
    ▼
② 放大 2 倍 (提高小字识别率)
    │
    ▼
③ 灰度化 (RGB → Grayscale)
    │
    ▼
④ 对比度增强 (直方图拉伸)
    │
    ▼
⑤ 二值化 (阈值分割, 默认 128)
    │
    ▼
⑥ 可选反色 (白字黑底 → 黑字白底)
    │
    ▼
⑦ 去噪 (中值滤波)
    │
    ▼
⑧ Tesseract OCR
    │  char_whitelist: "0123456789.,KkMm万Ww"
    │  PSM: 7 (单行文本)
    ▼
⑨ 金额文本解析
    │  "1.2万" → 12000
    │  "12K"   → 12000
    │  "1,234" → 1234
    ▼
⑩ 置信度判定
    │  ≥80% → 自动入库
    │  <80% → 标记待人工审核
```

#### 图像预处理 (`ImagePreprocessor.java`)

使用 `java.awt.image.BufferedImage` + `Graphics2D` 实现，无需额外依赖 OpenCV。

核心方法:
- `crop(image, x, y, w, h)` — 裁剪
- `scale(image, factor)` — 放大
- `toGrayscale(image)` — 灰度化
- `enhanceContrast(image)` — 对比度增强
- `binarize(image, threshold)` — 二值化
- `invert(image)` — 反色
- `denoise(image)` — 中值滤波去噪

#### 金额解析 (`BalanceTextParser.java`)

支持格式:
| OCR 文本 | 解析结果 |
|----------|---------|
| `12345` | 12345.00 |
| `1.2万` | 12000.00 |
| `12K` | 12000.00 |
| `1.5M` | 1500000.00 |
| `1,234,567` | 1234567.00 |

解析逻辑:
1. 去除空白字符
2. 修正常见 OCR 误识别 (`O`→`0`, `l`→`1`)
3. 检测单位后缀 (万/K/M)
4. 提取数字部分, 去逗号
5. 乘以单位倍率
6. 计算置信度 (匹配程度越高分越高)

#### Tesseract 配置 (`TesseractConfig.java`)

```java
@Bean
public Tesseract tesseract() {
    Tesseract tess = new Tesseract();
    tess.setDatapath("tessdata路径");
    tess.setLanguage("eng");
    tess.setTessVariable("tessedit_char_whitelist",
        "0123456789.,KkMm万WwB");
    return tess;
}
```

> **注意:** 服务器需安装 Tesseract 原生库。Windows 下 tess4j 自带; Linux 需 `apt install tesseract-ocr`。
> `eng.traineddata` 语言数据文件需放在 tessdata 目录。

---

### 5.3 可视化报表

#### API 设计

| 接口 | 功能 | 用途 |
|------|------|------|
| `GET /report/dailyTrend` | 每日余额趋势 | 折线图: X=日期, Y=余额 |
| `GET /report/multiAccountSummary` | 多账号汇总看板 | 卡片: 总余额/今日总收益/各账号状态 |
| `GET /report/accountTrend` | 单账号趋势 | 折线图: X=时间, Y=余额 |
| `GET /report/profitSummary` | 收益统计 | 表格: 账号/周收益/月收益/总收益 |

#### 返回数据结构示例

**多账号汇总:**
```json
{
  "totalBalance": 156000.00,
  "totalDailyProfit": 3200.00,
  "accounts": [
    {
      "accountName": "账号1",
      "currentBalance": 32000.00,
      "dailyChange": 800.00,
      "lastUpdateTime": "2026-05-11 14:00:00",
      "status": "online"
    }
  ]
}
```

**每日余额趋势:**
```json
[
  {
    "date": "2026-05-10",
    "accountName": "账号1",
    "openBalance": 31200.00,
    "closeBalance": 32000.00,
    "dailyProfit": 800.00
  }
]
```

---

## 六、项目依赖

### pom.xml 新增依赖

```xml
<!-- Tesseract OCR Java 封装 -->
<dependency>
  <groupId>net.sourceforge.tess4j</groupId>
  <artifactId>tess4j</artifactId>
  <version>5.11.0</version>
</dependency>
```

### Shiro 配置修改

在 `ShiroConfig.java` 中添加:
```java
filterChainDefinitionMap.put("/gameMonitor/device/**", "anon");
```

### 父 pom 注册

在 `jeecg-boot-module/pom.xml` 中添加:
```xml
<module>globe-game-monitor</module>
```

---

## 七、实施步骤

| 阶段 | 内容 | 产出 |
|------|------|------|
| **P1** | 模块脚手架 + 建表 SQL | pom.xml, 4 Entity, 4 Mapper, SQL |
| **P2** | 账号管理 CRUD | GameAccountController + Service |
| **P3** | 截图上传接口 | ScreenshotUploadController + OSS + Shiro |
| **P4** | OCR 核心流水线 | ImagePreprocessor + BalanceTextParser + OcrPipeline |
| **P5** | 异步 OCR 集成 | OcrProcessService (上传→OCR→入库全链路) |
| **P6** | 人工审核功能 | ScreenshotLogController (审核/修正) |
| **P7** | 报表 API | ReportController + SQL 聚合查询 |
| **P8** | 定时任务 | OcrRetryJob (失败重试) |

---

## 八、OCR 精度提升策略

如果 Tesseract 对游戏字体识别效果不理想，有以下升级路径:

| 阶段 | 方案 | 成本 |
|------|------|------|
| **Level 1** | 调整预处理参数 (阈值/对比度/裁剪坐标) | 免费 |
| **Level 2** | 用游戏截图样本训练 Tesseract LSTM 模型 | 免费, 需时间 |
| **Level 3** | 换用 PaddleOCR (百度开源, 中文识别更强) | 免费, 需部署 Python 服务 |
| **Level 4** | 换用多模态大模型 (GPT-4V / Gemini) | 有 API 费用 |

当前方案中 `game_ocr_config` 表的设计已经为每个账号独立配置预留了灵活性，无需改代码即可调参。

---

## 九、验证计划

1. **上传接口:** Postman 模拟 multipart 上传, 验证 OSS 存储 + 日志记录
2. **OCR 精度:** 准备 10+ 张游戏截图, 调试裁剪坐标和参数, 目标准确率 > 90%
3. **全链路:** 上传 → OCR → 余额入库 → 报表查询
4. **手机实测:** Tasker 配置定时截图上传, 运行 24 小时验证稳定性
