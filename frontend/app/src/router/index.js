import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '@/utils/auth'
import { hasAdminRole, isAdminLoggedIn } from '@/utils/adminAuth'
import {
  adminAiEngineRouteLoader,
  adminAuditLogRouteLoader,
  adminCommunityReviewRouteLoader,
  adminDashboardRouteLoader,
  adminFeedbackRouteLoader,
  adminGrowthConfigRouteLoader,
  adminJobRoleRouteLoader,
  adminLayoutRouteLoader,
  adminMembershipOrderRouteLoader,
  adminMembershipPlanRouteLoader,
  adminMonitorRouteLoader,
  adminNotificationRouteLoader,
  adminPromptRouteLoader,
  adminUserRightsRouteLoader,
  adminVersionLogRouteLoader,
  communityMyRouteLoader,
  communityRouteLoader,
  growthCenterRouteLoader,
  interviewEntryRouteLoader,
  interviewReportRouteLoader,
  interviewSessionRouteLoader,
  offerAssistRouteLoader,
  resumeResultRouteLoader,
  resumeUploadRouteLoader,
  settingsRouteLoader,
  templateEditorRouteLoader,
  templateLibraryRouteLoader
} from '@/router/routeLoaders'

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
    component: adminLayoutRouteLoader,
    meta: { useLayout: false, requiresAdminAuth: true },
    children: [
      {
        path: '',
        redirect: '/admin/dashboard'
      },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: adminDashboardRouteLoader,
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'community',
        name: 'AdminCommunityReview',
        component: adminCommunityReviewRouteLoader,
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'job-roles',
        name: 'AdminJobRoles',
        component: adminJobRoleRouteLoader,
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'prompts',
        name: 'AdminPrompts',
        component: adminPromptRouteLoader,
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'ai-engines',
        name: 'AdminAiEngines',
        component: adminAiEngineRouteLoader,
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: adminUserRightsRouteLoader,
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'monitor',
        name: 'AdminMonitor',
        component: adminMonitorRouteLoader,
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'notifications',
        name: 'AdminNotifications',
        component: adminNotificationRouteLoader,
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'feedback',
        name: 'AdminFeedback',
        component: adminFeedbackRouteLoader,
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'version-logs',
        name: 'AdminVersionLogs',
        component: adminVersionLogRouteLoader,
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'audit-logs',
        name: 'AdminAuditLogs',
        component: adminAuditLogRouteLoader,
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'membership/plans',
        name: 'AdminMembershipPlans',
        component: adminMembershipPlanRouteLoader,
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'membership/orders',
        name: 'AdminMembershipOrders',
        component: adminMembershipOrderRouteLoader,
        meta: { useLayout: false, requiresAdminAuth: true }
      },
      {
        path: 'growth-config',
        name: 'AdminGrowthConfig',
        component: adminGrowthConfigRouteLoader,
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
    meta: { requiresAuth: true, useLayout: true, keepAlive: true }
  },
  {
    path: '/membership',
    name: 'Membership',
    component: () => import('@/views/MembershipView.vue'),
    meta: { requiresAuth: true, useLayout: true, keepAlive: true }
  },
  {
    path: '/growth',
    name: 'GrowthCenter',
    component: growthCenterRouteLoader,
    meta: { requiresAuth: true, useLayout: true, keepAlive: true }
  },
  {
    path: '/offer',
    name: 'OfferAssist',
    component: offerAssistRouteLoader,
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
    component: settingsRouteLoader,
    meta: { requiresAuth: true, useLayout: true, keepAlive: true }
  },
  {
    path: '/resume/upload',
    name: 'ResumeUpload',
    component: resumeUploadRouteLoader,
    meta: { requiresAuth: true, useLayout: true }
  },
  {
    path: '/resume/result/:taskId',
    name: 'ResumeResult',
    component: resumeResultRouteLoader,
    meta: { requiresAuth: true, useLayout: true }
  },
  {
    path: '/resume/history',
    name: 'ResumeHistory',
    component: () => import('@/views/resume/HistoryView.vue'),
    meta: { requiresAuth: true, useLayout: true, keepAlive: true }
  },
  {
    path: '/interview/entry',
    name: 'InterviewEntry',
    component: interviewEntryRouteLoader,
    meta: { requiresAuth: true, useLayout: true }
  },
  {
    path: '/interview/session/:sessionId',
    name: 'InterviewSession',
    component: interviewSessionRouteLoader,
    meta: { requiresAuth: true, useLayout: false }
  },
  {
    path: '/interview/history',
    name: 'InterviewHistory',
    component: () => import('@/views/interview/InterviewHistoryView.vue'),
    meta: { requiresAuth: true, useLayout: true, keepAlive: true }
  },
  {
    path: '/interview/report/:sessionId',
    name: 'InterviewReport',
    component: interviewReportRouteLoader,
    meta: { requiresAuth: true, useLayout: true }
  },
  {
    path: '/templates',
    name: 'TemplateLibrary',
    component: templateLibraryRouteLoader,
    meta: { requiresAuth: true, useLayout: true, keepAlive: true }
  },
  {
    path: '/templates/editor/:templateId',
    name: 'TemplateEditor',
    component: templateEditorRouteLoader,
    meta: { requiresAuth: true, useLayout: false }
  },
  {
    path: '/community',
    name: 'Community',
    component: communityRouteLoader,
    meta: { requiresAuth: true, useLayout: true, keepAlive: true }
  },
  {
    path: '/community/post/:postId',
    name: 'PostDetail',
    component: () => import('@/views/community/PostDetailView.vue'),
    meta: { requiresAuth: true, useLayout: true }
  },
  {
    path: '/community/my',
    name: 'MyActivity',
    component: communityMyRouteLoader,
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
