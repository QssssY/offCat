export const templateLibraryRouteLoader = () => import('@/views/template/TemplateLibraryView.vue')
export const communityRouteLoader = () => import('@/views/community/CommunityView.vue')
export const growthCenterRouteLoader = () => import('@/views/growth/GrowthCenterView.vue')
export const resumeUploadRouteLoader = () => import('@/views/resume/UploadView.vue')
export const interviewEntryRouteLoader = () => import('@/views/interview/InterviewEntryView.vue')
export const offerAssistRouteLoader = () => import('@/views/offer/OfferAssistView.vue')

export const prefetchableUserRouteLoaders = {
  '/templates': templateLibraryRouteLoader,
  '/community': communityRouteLoader,
  '/growth': growthCenterRouteLoader,
  '/resume/upload': resumeUploadRouteLoader,
  '/interview/entry': interviewEntryRouteLoader,
  '/offer': offerAssistRouteLoader
}

const prefetchedRoutes = new Set()

export function prefetchUserRoute(path) {
  const loader = prefetchableUserRouteLoaders[path]
  if (!loader || prefetchedRoutes.has(path)) return null

  prefetchedRoutes.add(path)
  return loader().catch((error) => {
    prefetchedRoutes.delete(path)
    throw error
  })
}

const idleWarmupRoutes = ['/templates', '/community', '/growth']

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
