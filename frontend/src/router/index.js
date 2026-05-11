import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/dashboard/index.vue'),
    meta: { title: '总览面板' }
  },
  {
    path: '/accounts',
    name: 'Accounts',
    component: () => import('../views/account/index.vue'),
    meta: { title: '账号管理' }
  },
  {
    path: '/accounts/:id/ocr-config',
    name: 'OcrConfig',
    component: () => import('../views/account/ocr-config.vue'),
    meta: { title: 'OCR 配置' }
  },
  {
    path: '/balances',
    name: 'Balances',
    component: () => import('../views/balance/index.vue'),
    meta: { title: '余额记录' }
  },
  {
    path: '/screenshots',
    name: 'Screenshots',
    component: () => import('../views/screenshot/index.vue'),
    meta: { title: '截图日志' }
  },
  {
    path: '/reports',
    name: 'Reports',
    component: () => import('../views/report/index.vue'),
    meta: { title: '报表统计' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
