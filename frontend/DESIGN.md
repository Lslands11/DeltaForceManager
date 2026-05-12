# DF Monitor 前端设计规范

## 技术栈

| 依赖 | 用途 |
|------|------|
| Vue 3 (`<script setup>`) | 框架 |
| Element Plus | UI 组件库 |
| ECharts | 图表 |
| Axios | HTTP 请求 |
| Vite 6 | 构建工具 |
| Inter (Google Fonts) | 字体 |

---

## 全局主题变量

所有设计 token 定义在 [src/assets/main.css](src/assets/main.css) 的 `:root` 中，**新增页面必须使用这些变量，禁止硬编码颜色/字号/间距**。

### 颜色体系

```
主色    --color-primary          #6366f1    按钮、链接、高亮
        --color-primary-light    #818cf8
        --color-primary-bg       #eef2ff    行 hover 背景

语义色  --color-success          #10b981    正向值、成功
        --color-danger           #ef4444    负向值、危险
        --color-warning          #f59e0b    警告
        --color-info             #3b82f6    信息

中性色  --color-bg               #f8fafc    页面底色
        --color-bg-elevated      #ffffff    卡片/面板背景
        --color-bg-muted         #f1f5f0    表头背景
        --color-text             #0f172a    主文字
        --color-text-secondary   #64748b    次要文字
        --color-text-muted       #94a3b8    弱化文字
        --color-border           #e2e8f0    边框

侧边栏  --sidebar-bg             #0f172a
        --sidebar-bg-hover       #1e293b
        --sidebar-text           #94a3b8
        --sidebar-text-active    #ffffff
        --sidebar-accent         #6366f1    激活指示色
```

### 间距系统

```
--space-xs   4px
--space-sm   8px
--space-md   16px
--space-lg   24px
--space-xl   32px
--space-2xl  48px
```

### 字体系统

```
--font-size-xs       0.75rem (12px)    标签、辅助文字
--font-size-sm       0.8125rem (13px)  表格正文
--font-size-base     0.875rem (14px)   默认正文
--font-size-md       1rem (16px)       卡片标题
--font-size-lg       1.25rem (20px)    小标题
--font-size-xl       1.5rem (24px)     页面标题
--font-size-2xl      1.875rem (30px)   大数字展示
```

### 圆角 & 阴影

```
--radius-sm   6px     输入框
--radius-md   8px     按钮、Tag
--radius-lg   12px    卡片、面板
--radius-xl   16px    Dialog、Drawer

--shadow-xs   轻微    卡片默认
--shadow-sm   轻      卡片 hover
--shadow-md   中      交互卡片 hover
--shadow-xl   重      登录卡片
```

---

## 布局结构

### 整体布局 (App.vue)

```
┌──────────────────────────────────────────────┐
│  Sidebar (dark)  │  Header (white)           │
│  64px / 220px    ├───────────────────────────┤
│                  │  Main (bg: --color-bg)    │
│  - Logo          │                           │
│  - Menu items    │  <router-view />          │
│  - Collapse btn  │                           │
└──────────────────────────────────────────────┘
```

- 侧边栏宽度: 折叠 64px / 展开 220px
- Header 高度: 56px (`--header-height`)
- 主内容区自动填充剩余空间

### 页面布局

每个页面使用 `.page-container` 包裹:

```html
<div class="page-container">
  <div class="page-header">
    <h2>页面标题</h2>
    <p>页面描述</p>
  </div>
  <!-- 内容 -->
</div>
```

标题行带操作按钮时使用 `.page-header-row`:

```html
<div class="page-header page-header-row">
  <div>
    <h2>页面标题</h2>
    <p>页面描述</p>
  </div>
  <el-button type="primary">操作</el-button>
</div>
```

---

## 公用样式类

### 卡片

| 类名 | 用途 |
|------|------|
| `.card` | 通用卡片容器 (白底、圆角、阴影、边框) |
| `.card-interactive` | 可交互卡片 (hover 上浮效果) |
| `.stat-card` | 数据统计卡片 (label + value 垂直排列) |

### 布局

| 类名 | 用途 |
|------|------|
| `.page-container` | 页面内容包裹 (居中、最大宽度 1400px) |
| `.page-header` | 页面标题区域 |
| `.page-header-row` | 标题 + 右侧操作 的 flex 布局 |
| `.section-gap` | 区块间距 (24px margin-bottom) |
| `.grid-2` / `.grid-3` / `.grid-4` | 响应式网格 (自动适配移动端) |
| `.filter-bar` | 筛选栏 (卡片样式、内边距紧凑) |
| `.table-container` | 表格容器 (圆角包裹) |
| `.table-pagination` | 分页右对齐容器 |
| `.empty-state` | 空状态居中提示 |

### 文字

| 类名 | 用途 |
|------|------|
| `.text-success` | 绿色加粗 (正数) |
| `.text-danger` | 红色加粗 (负数) |

---

## Element Plus 覆写

全局覆写集中在 [main.css](src/assets/main.css) 底部，统一了:

- **el-table**: 表头背景、hover 行高亮色、字号
- **el-button**: 圆角、主色背景、字重
- **el-dialog**: 圆角 (16px)、header/footer 分割线
- **el-drawer**: 左侧圆角
- **el-tabs**: 激活色、字重
- **el-tag**: 胶囊圆角
- **el-input / el-select**: 输入框圆角
- **el-divider**: 辅助文字样式

**新增页面不需要单独调整这些组件样式**，全局覆写已经覆盖。

---

## 新页面开发指南

### 1. 创建页面

```vue
<template>
  <div class="page-container">
    <!-- 标题 -->
    <div class="page-header page-header-row">
      <div>
        <h2>新页面</h2>
        <p>页面描述</p>
      </div>
      <el-button type="primary">
        <el-icon><Plus /></el-icon>添加
      </el-button>
    </div>

    <!-- 筛选 -->
    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input placeholder="搜索" clearable style="width: 200px;" />
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="table-container">
      <el-table :data="data" stripe>
        <!-- columns -->
      </el-table>
      <div class="table-pagination">
        <el-pagination ... />
      </div>
    </div>

    <!-- Dialog -->
    <el-dialog title="弹窗" width="500px">
      <!-- form -->
      <template #footer>
        <el-button>取消</el-button>
        <el-button type="primary">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
```

### 2. 样式编写原则

- **优先使用全局公用类** (`.card`, `.filter-bar`, `.table-container` 等)
- **使用 CSS 变量**而非硬编码值: `var(--space-md)` 而非 `16px`
- **scoped style 只写页面特有样式**，不要重复定义全局已有的样式
- 需要新增公用样式时，在 [main.css](main.css) 中添加，而非各页面重复

### 3. 间距规范

- 页面级区块间距: `.section-gap` 或 `margin-bottom: var(--space-lg)`
- 卡片内间距: `.card` 自带 `var(--space-lg)` padding
- 表单元素间距: Element Plus 默认即可
- 筛选栏与表格间距: `.filter-bar` 自带 `margin-bottom: var(--space-md)`

### 4. 颜色使用

```css
/* 正确 - 使用变量 */
color: var(--color-text-secondary);

/* 错误 - 硬编码 */
color: #666;
```

### 5. 图表容器

图表使用 `.card` 包裹，内部 `div` 设置固定高度:

```html
<div class="card">
  <div class="chart-header">
    <h3 class="chart-title">图表标题</h3>
  </div>
  <div ref="chartRef" style="height: 300px;"></div>
</div>
```

---

## 文件结构

```
src/
├── assets/
│   └── main.css              # 全局样式 & 主题变量 (唯一)
├── App.vue                   # 布局壳 (侧边栏 + Header + Main)
├── views/
│   ├── login/index.vue       # 登录页 (独立布局)
│   ├── dashboard/index.vue   # 总览面板
│   ├── account/
│   │   ├── index.vue         # 账号管理 (CRUD 表格)
│   │   └── ocr-config.vue    # OCR 配置 (表单页)
│   ├── balance/index.vue     # 余额记录 (CRUD 表格)
│   ├── screenshot/index.vue  # 截图日志 (表格 + Drawer)
│   ├── report/index.vue      # 报表统计 (Tabs + 图表)
│   └── user/index.vue        # 用户管理 (CRUD 表格)
└── ...
```

---

## 注意事项

1. **不要在 scoped style 中重新定义全局已有的样式** (如 `.card`, `.page-container`)
2. **新增 CSS 变量必须在 `:root` 中声明**，保持单一来源
3. **Element Plus 组件样式覆写统一在 main.css**，不要分散在各页面
4. **响应式**: `.grid-*` 类已内置断点 (1024px / 640px)，不需要额外写 media query
5. **滚动条**: 全局已统一自定义滚动条样式 (6px, 圆角)
