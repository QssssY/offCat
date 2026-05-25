export const templateLibraryRouteLoader = () => import('@/views/template/TemplateLibraryView.vue')
export const communityRouteLoader = () => import('@/views/community/CommunityView.vue')
export const growthCenterRouteLoader = () => import('@/views/growth/GrowthCenterView.vue')

const prefetchableUserRouteLoaders = {
  '/templates': templateLibraryRouteLoader,
  '/community': communityRouteLoader,
  '/growth': growthCenterRouteLoader
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
