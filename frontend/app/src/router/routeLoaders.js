export const templateLibraryRouteLoader = () => import('@/views/template/TemplateLibraryView.vue')
export const communityRouteLoader = () => import('@/views/community/CommunityView.vue')
export const growthCenterRouteLoader = () => import('@/views/growth/GrowthCenterView.vue')
export const resumeUploadRouteLoader = () => import('@/views/resume/UploadView.vue')
export const resumeResultRouteLoader = () => import('@/views/resume/ResultView.vue')
export const interviewEntryRouteLoader = () => import('@/views/interview/InterviewEntryView.vue')
export const interviewSessionRouteLoader = () => import('@/views/interview/InterviewSessionView.vue')
export const interviewReportRouteLoader = () => import('@/views/interview/InterviewReportView.vue')
export const offerAssistRouteLoader = () => import('@/views/offer/OfferAssistView.vue')
export const settingsRouteLoader = () => import('@/views/settings/SettingsView.vue')
export const communityMyRouteLoader = () => import('@/views/community/MyActivity.vue')
export const templateEditorRouteLoader = () => import('@/views/template/TemplateEditorView.vue')
export const adminLayoutRouteLoader = () => import('@/layouts/AdminLayout.vue')
export const adminDashboardRouteLoader = () => import('@/views/admin/AdminDashboardView.vue')
export const adminCommunityReviewRouteLoader = () => import('@/views/admin/AdminCommunityReviewView.vue')
export const adminMonitorRouteLoader = () => import('@/views/admin/AdminMonitorView.vue')
export const adminUserRightsRouteLoader = () => import('@/views/admin/AdminUserRightsView.vue')
export const adminAuditLogRouteLoader = () => import('@/views/admin/AdminAuditLogView.vue')
export const adminNotificationRouteLoader = () => import('@/views/admin/AdminNotificationView.vue')
export const adminFeedbackRouteLoader = () => import('@/views/admin/AdminFeedbackView.vue')
export const adminVersionLogRouteLoader = () => import('@/views/admin/AdminVersionLogView.vue')
export const adminMembershipPlanRouteLoader = () => import('@/views/admin/AdminMembershipPlanView.vue')
export const adminMembershipOrderRouteLoader = () => import('@/views/admin/AdminMembershipOrderView.vue')
export const adminJobRoleRouteLoader = () => import('@/views/admin/AdminJobRoleView.vue')
export const adminPromptRouteLoader = () => import('@/views/admin/AdminPromptView.vue')
export const adminAiEngineRouteLoader = () => import('@/views/admin/AdminAiEngineView.vue')
export const adminGrowthConfigRouteLoader = () => import('@/views/admin/AdminGrowthConfigView.vue')

export const prefetchableUserRouteLoaders = {
  '/templates': templateLibraryRouteLoader,
  '/community': communityRouteLoader,
  '/settings': settingsRouteLoader,
  '/community/my': communityMyRouteLoader,
  '/growth': growthCenterRouteLoader,
  '/resume/upload': resumeUploadRouteLoader,
  '/resume/result': resumeResultRouteLoader,
  '/interview/entry': interviewEntryRouteLoader,
  '/offer': offerAssistRouteLoader
}

export const prefetchableAdminRouteLoaders = {
  '/admin/dashboard': adminDashboardRouteLoader,
  '/admin/community': adminCommunityReviewRouteLoader,
  '/admin/monitor': adminMonitorRouteLoader,
  '/admin/users': adminUserRightsRouteLoader,
  '/admin/audit-logs': adminAuditLogRouteLoader,
  '/admin/notifications': adminNotificationRouteLoader,
  '/admin/feedback': adminFeedbackRouteLoader,
  '/admin/version-logs': adminVersionLogRouteLoader,
  '/admin/membership/plans': adminMembershipPlanRouteLoader,
  '/admin/membership/orders': adminMembershipOrderRouteLoader,
  '/admin/job-roles': adminJobRoleRouteLoader,
  '/admin/prompts': adminPromptRouteLoader,
  '/admin/ai-engines': adminAiEngineRouteLoader,
  '/admin/growth-config': adminGrowthConfigRouteLoader
}

const prefetchedRoutes = new Set()
const prefetchedAdminRoutes = new Set()
const prefetchedRoutePromises = new Map()
const prefetchedAdminRoutePromises = new Map()
const prefetchedTemplateEditorAssets = new Map()
let interviewSessionPrefetchPromise = null
let interviewReportPrefetchPromise = null
let templateEditorPrefetchPromise = null
let adminShellPrefetchPromise = null

export function prefetchUserRoute(path) {
  const loader = prefetchableUserRouteLoaders[path]
  if (!loader) return null
  if (prefetchedRoutes.has(path)) return prefetchedRoutePromises.get(path) || null

  prefetchedRoutes.add(path)
  const prefetchPromise = loader().catch((error) => {
    prefetchedRoutes.delete(path)
    prefetchedRoutePromises.delete(path)
    throw error
  })
  prefetchedRoutePromises.set(path, prefetchPromise)
  return prefetchPromise
}

export function prefetchAdminRoute(path) {
  const loader = prefetchableAdminRouteLoaders[path]
  if (!loader) return null
  if (prefetchedAdminRoutes.has(path)) return prefetchedAdminRoutePromises.get(path) || null

  // 管理端只在导航意图明确时预取，避免进入后台后一次性拉满所有页面 chunk。
  prefetchedAdminRoutes.add(path)
  const prefetchPromise = loader().catch((error) => {
    prefetchedAdminRoutes.delete(path)
    prefetchedAdminRoutePromises.delete(path)
    throw error
  })
  prefetchedAdminRoutePromises.set(path, prefetchPromise)
  return prefetchPromise
}

export function prefetchInterviewReportRoute() {
  if (interviewReportPrefetchPromise) return interviewReportPrefetchPromise

  // 面试结束后会立刻进入报告等待页，提前加载页面 chunk，避免从无布局会话页切到报告页时白屏。
  interviewReportPrefetchPromise = interviewReportRouteLoader().catch((error) => {
    interviewReportPrefetchPromise = null
    throw error
  })
  return interviewReportPrefetchPromise
}

export function prefetchInterviewSessionRoute() {
  if (interviewSessionPrefetchPromise) return interviewSessionPrefetchPromise

  // 创建面试接口等待期间提前拉会话页 chunk，避免拿到 sessionId 后再冷加载整页。
  interviewSessionPrefetchPromise = interviewSessionRouteLoader().catch((error) => {
    interviewSessionPrefetchPromise = null
    throw error
  })
  return interviewSessionPrefetchPromise
}

function prefetchTemplateEditorChunk() {
  if (templateEditorPrefetchPromise) return templateEditorPrefetchPromise

  templateEditorPrefetchPromise = templateEditorRouteLoader().catch((error) => {
    templateEditorPrefetchPromise = null
    throw error
  })
  return templateEditorPrefetchPromise
}

export function prefetchTemplateEditorRoute(templateId) {
  const normalizedTemplateId = templateId ? String(templateId) : ''
  if (!normalizedTemplateId) return prefetchTemplateEditorChunk()

  if (prefetchedTemplateEditorAssets.has(normalizedTemplateId)) {
    return prefetchedTemplateEditorAssets.get(normalizedTemplateId)
  }

  // 模板编辑页进入后会马上加载内容和样式，点击模板时一起预取可减少首屏空白。
  const prefetchPromise = Promise.all([
    prefetchTemplateEditorChunk(),
    import(`@/data/contents/${templateId}.js`),
    import(`@/data/styles/${templateId}.css?raw`)
  ]).catch((error) => {
    prefetchedTemplateEditorAssets.delete(normalizedTemplateId)
    throw error
  })
  prefetchedTemplateEditorAssets.set(normalizedTemplateId, prefetchPromise)
  return prefetchPromise
}

export function prefetchAdminShellRoute() {
  if (adminShellPrefetchPromise) return adminShellPrefetchPromise

  // 登录成功后默认进入后台首页，提前加载外层布局和仪表盘首屏 chunk。
  adminShellPrefetchPromise = Promise.all([
    adminLayoutRouteLoader(),
    adminDashboardRouteLoader()
  ]).catch((error) => {
    adminShellPrefetchPromise = null
    throw error
  })
  return adminShellPrefetchPromise
}

const adminIdleWarmupRoutes = ['/admin/dashboard', '/admin/users', '/admin/ai-engines', '/admin/prompts', '/admin/monitor']
const idleWarmupRoutes = ['/templates', '/community', '/settings', '/growth', '/resume/upload', '/resume/result', '/interview/entry', '/offer']

export function warmupHighFrequencyUserRoutes() {
  const runWarmup = () => {
    idleWarmupRoutes.forEach((path) => {
      prefetchUserRoute(path)?.catch(() => {})
    })
  }

  if (typeof window === 'undefined') return null

  if (typeof window.requestIdleCallback === 'function') {
    return window.requestIdleCallback(runWarmup, { timeout: 3000 })
  }

  return window.setTimeout(runWarmup, 800)
}

export function warmupHighFrequencyAdminRoutes() {
  const runWarmup = () => {
    adminIdleWarmupRoutes.forEach((path) => {
      prefetchAdminRoute(path)?.catch(() => {})
    })
  }

  if (typeof window === 'undefined') return null

  // 管理端只预热高频入口，降低首次切换白屏感，同时避免一次性拉取全部后台页面 chunk。
  if (typeof window.requestIdleCallback === 'function') {
    return window.requestIdleCallback(runWarmup, { timeout: 3000 })
  }

  return window.setTimeout(runWarmup, 800)
}
