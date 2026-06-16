import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn, getCurrentUser } from '@/utils/auth'
import { ElMessage } from 'element-plus'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue')
    },
    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: () => import('../views/ForgotPasswordView.vue')
    },
    {
      path: '/user',
      name: 'user',
      component: () => import('../views/UserView.vue'),
      meta: { requiresAuth: true, role: 'user' }
    },
    {
      path: '/service',
      name: 'service',
      component: () => import('../views/ServiceView.vue'),
      meta: { requiresAuth: true, role: 'service' }
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('../views/AdminView.vue'),
      meta: { requiresAuth: true, role: 'admin' }
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

const roleHome = {
  user: '/user',
  service: '/service',
  admin: '/admin'
}

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.role) {
    const user = getCurrentUser()
    if (!user || user.role !== to.meta.role) {
      ElMessage.error('无权访问该页面')
      if (user && roleHome[user.role]) {
        next(roleHome[user.role])
      } else {
        next('/login')
      }
      return
    }
  }

  next()
})

export default router
