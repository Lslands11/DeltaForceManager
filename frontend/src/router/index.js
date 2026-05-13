import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/index.vue'),
    meta: { title: '登录', public: true }
  },
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
    path: '/ocr-presets',
    name: 'OcrPresets',
    component: () => import('../views/ocr-preset/index.vue'),
    meta: { title: 'OCR 预设配置', adminOnly: true }
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
  },
  {
    path: '/users',
    name: 'Users',
    component: () => import('../views/user/index.vue'),
    meta: { title: '用户管理', adminOnly: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.public) {
    // 公开页面：已登录则跳转首页
    if (token && to.path === '/login') {
      next('/dashboard')
    } else {
      next()
    }
  } else {
    // 受保护页面：未登录跳转登录页
    if (!token) {
      next('/login')
    } else if (to.meta.adminOnly) {
      // adminOnly 页面：检查角色
      try {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        if (userInfo.role === 'ADMIN') {
          next()
        } else {
          next('/dashboard')
        }
      } catch {
        next('/dashboard')
      }
    } else {
      next()
    }
  }
})

export default router
