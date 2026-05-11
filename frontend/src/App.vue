<template>
  <el-container class="app-layout">
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="app-aside">
      <div class="logo" @click="$router.push('/')">
        <span v-if="!isCollapsed" class="logo-text">DF Monitor</span>
        <span v-else class="logo-icon">DF</span>
      </div>
      <el-menu
        :default-active="$route.path"
        :collapse="isCollapsed"
        router
        class="side-menu"
        background-color="#1e1e2e"
        text-color="#a6adc8"
        active-text-color="#89b4fa"
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
      </el-menu>
      <div class="collapse-btn" @click="isCollapsed = !isCollapsed">
        <el-icon><Fold v-if="!isCollapsed" /><Expand v-else /></el-icon>
      </div>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <h3>{{ $route.meta.title }}</h3>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import {
  Monitor, User, Wallet, Picture, DataAnalysis,
  Fold, Expand
} from '@element-plus/icons-vue'

const isCollapsed = ref(false)
</script>

<style scoped>
.app-layout {
  height: 100vh;
}

.app-aside {
  background: #1e1e2e;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  overflow: hidden;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.logo-text {
  font-size: 1.1rem;
  font-weight: 700;
  color: #89b4fa;
  letter-spacing: 0.05em;
}

.logo-icon {
  font-size: 1rem;
  font-weight: 700;
  color: #89b4fa;
}

.side-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
}

.side-menu::-webkit-scrollbar {
  width: 0;
}

.side-menu .el-menu-item {
  border-radius: 8px;
  margin: 4px 8px;
  height: 44px;
}

.side-menu .el-menu-item.is-active {
  background: rgba(137, 180, 250, 0.12) !important;
}

.collapse-btn {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #6c7086;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.collapse-btn:hover {
  color: #cdd6f4;
}

.app-header {
  background: #fff;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  padding: 0 24px;
  height: 56px;
}

.app-header h3 {
  font-size: 1.1rem;
  font-weight: 600;
}

.app-main {
  background: var(--bg-primary);
  padding: 24px;
  overflow-y: auto;
}
</style>
