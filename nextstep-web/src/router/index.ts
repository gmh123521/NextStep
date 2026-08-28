import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/api/http'
import { useUserStore } from '@/stores/user'

const routes = [
  { path: '/login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/Dashboard.vue') },
      { path: 'profile',   component: () => import('@/views/Profile.vue') },
      { path: 'plan',      component: () => import('@/views/Plan.vue') },
      { path: 'school',    component: () => import('@/views/School.vue') },
      { path: 'gov',       component: () => import('@/views/Gov.vue') },
      { path: 'job',       component: () => import('@/views/Job.vue') },
      {
        path: 'admin',
        component: () => import('@/views/admin/AdminDashboard.vue'),
        meta: { requiresAdmin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  if (to.meta.public) return true
  if (!getToken()) return { path: '/login', query: { redirect: to.fullPath } }

  const userStore = useUserStore()
  if (!userStore.me) {
    try {
      await userStore.refreshMe()
    } catch {
      userStore.logout()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    return { path: '/dashboard' }
  }
  return true
})

export default router
