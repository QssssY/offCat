import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '@/utils/auth'
import { hasAdminRole, isAdminLoggedIn } from '@/utils/adminAuth'

// 路由表说明：
// 1. 用户端保持现有路由结构。
// 2. 管理端独立走 /admin 前缀，不与用户端入口混用。
const routes = [
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/AdminLoginView.vue'),
    meta: { useLayout: false, adminPublic: true }
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { useLayout: false, requiresAdminAuth: true },
    children: [
      {
        path: '',
        redirect: '/admin/dashboard'
      },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/AdminDashboardView.vue'),
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'job-roles',
        name: 'AdminJobRoles',
        component: () => import('@/views/admin/AdminJobRoleView.vue'),
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'prompts',
        name: 'AdminPrompts',
        component: () => import('@/views/admin/AdminPromptView.vue'),
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'ai-engines',
        name: 'AdminAiEngines',
        component: () => import('@/views/admin/AdminAiEngineView.vue'),
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/AdminUserRightsView.vue'),
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'monitor',
        name: 'AdminMonitor',
        component: () => import('@/views/admin/AdminMonitorView.vue'),
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'notifications',
        name: 'AdminNotifications',
        component: () => import('@/views/admin/AdminNotificationView.vue'),
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'version-logs',
        name: 'AdminVersionLogs',
        component: () => import('@/views/admin/AdminVersionLogView.vue'),
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'audit-logs',
        name: 'AdminAuditLogs',
        component: () => import('@/views/admin/AdminAuditLogView.vue'),
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'membership/plans',
        name: 'AdminMembershipPlans',
        component: () => import('@/views/admin/AdminMembershipPlanView.vue'),
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'membership/orders',
        name: 'AdminMembershipOrders',
        component: () => import('@/views/admin/AdminMembershipOrderView.vue'),
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'growth-config',
        name: 'AdminGrowthConfig',
        component: () => import('@/views/admin/AdminGrowthConfigView.vue'),
        meta: { useLayout: false, requiresAdminAuth: true }
      }
    ]
  },
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
    path: '/version-logs',
    name: 'VersionLogs',
    component: () => import('@/views/VersionLogView.vue'),
    meta: { requiresAuth: false, useLayout: true }
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
    path: '/growth',
    name: 'GrowthCenter',
    component: () => import('@/views/growth/GrowthCenterView.vue'),
    meta: { requiresAuth: true, useLayout: true }
  },
  {
    path: '/offer',
    name: 'OfferAssist',
    component: () => import('@/views/offer/OfferAssistView.vue'),
    meta: { requiresAuth: true, useLayout: true }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/notification/NotificationView.vue'),
    meta: { requiresAuth: true, useLayout: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/settings/SettingsView.vue'),
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
    path: '/templates',
    name: 'TemplateLibrary',
    component: () => import('@/views/template/TemplateLibraryView.vue'),
    meta: { requiresAuth: true, useLayout: true }
  },
  {
    path: '/templates/editor/:templateId',
    name: 'TemplateEditor',
    component: () => import('@/views/template/TemplateEditorView.vue'),
    meta: { requiresAuth: true, useLayout: false }
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

// 全局路由守卫：
// - 管理端路由和用户端路由分别鉴权，互不干扰。
router.beforeEach((to) => {
  const requiresAdminAuth = to.matched.some((record) => record.meta.requiresAdminAuth === true)
  const adminPublic = to.matched.some((record) => record.meta.adminPublic === true)
  const requiresUserAuth = to.matched.some((record) => record.meta.requiresAuth === true)

  if (requiresAdminAuth) {
    if (!isAdminLoggedIn() || !hasAdminRole()) {
      return {
        path: '/admin/login',
        query: { redirect: to.fullPath }
      }
    }
    return true
  }

  if (adminPublic) {
    if (isAdminLoggedIn() && hasAdminRole()) {
      return '/admin/dashboard'
    }
    return true
  }

  if (requiresUserAuth && !isLoggedIn()) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  if (to.path === '/login' && isLoggedIn()) {
    return '/'
  }

  return true
})

export default router
