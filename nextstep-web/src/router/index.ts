import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/api/http'

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
      { path: 'job',       component: () => import('@/views/Job.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  if (!getToken()) return { path: '/login', query: { redirect: to.fullPath } }
  return true
})

export default router
