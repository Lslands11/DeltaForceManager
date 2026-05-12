<template>
  <router-view v-if="$route.path === '/login'" />
  <el-container v-else class="app-layout">
    <el-aside :width="isCollapsed ? 'var(--sidebar-width-collapsed)' : 'var(--sidebar-width)'" class="app-aside">
      <div class="logo" @click="$router.push('/')">
        <div class="logo-mark">DF</div>
        <span v-if="!isCollapsed" class="logo-text">DF Monitor</span>
      </div>
      <el-menu
        :default-active="$route.path"
        :collapse="isCollapsed"
        router
        class="side-menu"
        :background-color="'var(--sidebar-bg)'"
        :text-color="'var(--sidebar-text)'"
        :active-text-color="'var(--sidebar-text-active)'"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Monitor /></el-icon>
          <template #title>总览面板</template>
        </el-menu-item>
        <el-menu-item index="/accounts">
          <el-icon><User /></el-icon>
          <template #title>账号管理</template>
        </el-menu-item>
        <el-menu-item index="/balances">
          <el-icon><Wallet /></el-icon>
          <template #title>余额记录</template>
        </el-menu-item>
        <el-menu-item index="/screenshots">
          <el-icon><Picture /></el-icon>
          <template #title>截图日志</template>
        </el-menu-item>
        <el-menu-item index="/reports">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>报表统计</template>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/users">
          <el-icon><UserFilled /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
      </el-menu>
      <div class="collapse-btn" @click="isCollapsed = !isCollapsed">
        <el-icon><Fold v-if="!isCollapsed" /><Expand v-else /></el-icon>
      </div>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <h3 class="header-title">{{ $route.meta.title }}</h3>
        <div class="header-right">
          <span class="header-user">{{ userInfo.nickname || userInfo.username }}</span>
          <el-button link class="header-logout" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            <span v-if="!isCollapsed">退出</span>
          </el-button>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  Monitor, User, Wallet, Picture, DataAnalysis,
  Fold, Expand, UserFilled, SwitchButton
} from '@element-plus/icons-vue'

const router = useRouter()
const isCollapsed = ref(false)

const userInfo = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
})

const isAdmin = computed(() => userInfo.value.role === 'ADMIN')

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  router.push('/login')
}
</script>

<style scoped>
.app-layout {
  height: 100vh;
}

/* --- Sidebar --- */
.app-aside {
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  transition: width var(--transition-slow);
  overflow: hidden;
}

.logo {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  cursor: pointer;
  border-bottom: 1px solid var(--sidebar-border);
}

.logo-mark {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  color: white;
  flex-shrink: 0;
}

.logo-text {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-bold);
  color: var(--sidebar-text-active);
  letter-spacing: 0.02em;
}

.side-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
  padding: var(--space-sm) 0;
}

.side-menu::-webkit-scrollbar {
  width: 0;
}

.side-menu .el-menu-item {
  border-radius: var(--radius-md);
  margin: 2px var(--space-sm);
  height: 42px;
}

.side-menu .el-menu-item:hover {
  background-color: var(--sidebar-bg-hover) !important;
  color: var(--sidebar-text-hover) !important;
}

.side-menu .el-menu-item.is-active {
  background: rgba(99, 102, 241, 0.15) !important;
  color: var(--sidebar-text-active) !important;
}

.collapse-btn {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--color-text-muted);
  border-top: 1px solid var(--sidebar-border);
  transition: color var(--transition-fast);
}

.collapse-btn:hover {
  color: var(--sidebar-text-hover);
}

/* --- Header --- */
.app-header {
  background: var(--color-bg-elevated);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  height: var(--header-height);
  backdrop-filter: blur(8px);
}

.header-title {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text);
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.header-user {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
}

.header-logout {
  color: var(--color-text-muted) !important;
  transition: color var(--transition-fast) !important;
}

.header-logout:hover {
  color: var(--color-danger) !important;
}

/* --- Main --- */
.app-main {
  background: var(--color-bg);
  padding: var(--space-lg);
  overflow-y: auto;
}
</style>
