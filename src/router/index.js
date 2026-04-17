import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '@/utils/auth'

// Route table:
// `/membership` is the new member center page and must require login.
const routes = [
  {
    path: '/',
    name: 'HomePage',
    component: () => import('@/views/HomePageView.vue'),
    meta: { requiresAuth: false, useLayout: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { requiresAuth: false, useLayout: false }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/DashboardView.vue'),
    meta: { requiresAuth: true, useLayout: true }
  },
  {
    path: '/membership',
    name: 'Membership',
    component: () => import('@/views/MembershipView.vue'),
    meta: { requiresAuth: true, useLayout: true }
  },
  {
    path: '/resume/upload',
    name: 'ResumeUpload',
    component: () => import('@/views/resume/UploadView.vue'),
    meta: { requiresAuth: true, useLayout: true }
  },
  {
    path: '/resume/result/:taskId',
    name: 'ResumeResult',
    component: () => import('@/views/resume/ResultView.vue'),
    meta: { requiresAuth: true, useLayout: true }
  },
  {
    path: '/resume/history',
    name: 'ResumeHistory',
    component: () => import('@/views/resume/HistoryView.vue'),
    meta: { requiresAuth: true, useLayout: true }
  },
  {
    path: '/interview/entry',
    name: 'InterviewEntry',
    component: () => import('@/views/interview/InterviewEntryView.vue'),
    meta: { requiresAuth: true, useLayout: true }
  },
  {
    path: '/interview/session/:sessionId',
    name: 'InterviewSession',
    component: () => import('@/views/interview/InterviewSessionView.vue'),
    meta: { requiresAuth: true, useLayout: false }
  },
  {
    path: '/interview/history',
    name: 'InterviewHistory',
    component: () => import('@/views/interview/InterviewHistoryView.vue'),
    meta: { requiresAuth: true, useLayout: true }
  },
  {
    path: '/interview/report/:sessionId',
    name: 'InterviewReport',
    component: () => import('@/views/interview/InterviewReportView.vue'),
    meta: { requiresAuth: true, useLayout: true }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Global auth guard:
// protected pages depend on token-based auth status from the existing auth utility.
// When a protected page is opened without login, the user is redirected to `/login`.
router.beforeEach((to, from, next) => {
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth === true)

  if (requiresAuth && !isLoggedIn()) {
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  } else if (to.path === '/login' && isLoggedIn()) {
    next('/')
  } else {
    next()
  }
})

export default router
