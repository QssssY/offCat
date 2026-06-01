import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const sourceFile = (path) => readFileSync(resolve(process.cwd(), path), 'utf8')

describe('routeLoaders', () => {
  it('prefetches only selected high-frequency user routes', () => {
    const source = sourceFile('src/router/routeLoaders.js')

    expect(source).toContain('resumeResultRouteLoader')
    expect(source).toContain("'/templates': templateLibraryRouteLoader")
    expect(source).toContain("'/community': communityRouteLoader")
    expect(source).toContain("'/growth': growthCenterRouteLoader")
    expect(source).toContain("'/resume/upload': resumeUploadRouteLoader")
    expect(source).toContain("'/resume/result': resumeResultRouteLoader")
    expect(source).toContain("'/interview/entry': interviewEntryRouteLoader")
    expect(source).toContain("'/offer': offerAssistRouteLoader")
    expect(source).not.toContain("'/settings':")
  })

  it('prefetches admin navigation routes without warming every admin chunk on idle', () => {
    const source = sourceFile('src/router/routeLoaders.js')

    expect(source).toContain('adminDashboardRouteLoader')
    expect(source).toContain('adminCommunityReviewRouteLoader')
    expect(source).toContain('adminMonitorRouteLoader')
    expect(source).toContain('adminMembershipPlanRouteLoader')
    expect(source).toContain("'/admin/dashboard': adminDashboardRouteLoader")
    expect(source).toContain("'/admin/monitor': adminMonitorRouteLoader")
    expect(source).toContain("'/admin/community': adminCommunityReviewRouteLoader")
    expect(source).toContain("'/admin/membership/plans': adminMembershipPlanRouteLoader")
    expect(source).toContain('const prefetchedAdminRoutes = new Set()')
    expect(source).toContain('export function prefetchAdminRoute(path)')
    expect(source).toContain('prefetchedAdminRoutes.has(path)')
    expect(source).toContain('prefetchedAdminRoutes.add(path)')
    expect(source).not.toMatch(/idleWarmupRoutes\s*=\s*\[[\s\S]*?\/admin/)
  })

  it('warms selected high-frequency admin routes while idle without full admin preload', () => {
    const source = sourceFile('src/router/routeLoaders.js')

    expect(source).toContain('export function warmupHighFrequencyAdminRoutes()')
    expect(source).toContain("const adminIdleWarmupRoutes = ['/admin/dashboard', '/admin/users', '/admin/ai-engines', '/admin/prompts', '/admin/monitor']")
    expect(source).toContain('adminIdleWarmupRoutes.forEach((path) => {')
    expect(source).toContain('prefetchAdminRoute(path)?.catch(() => {})')
    expect(source).toContain('requestIdleCallback(runWarmup, { timeout: 3000 })')
    expect(source).not.toMatch(/adminIdleWarmupRoutes\s*=\s*\[[\s\S]*?\/admin\/membership\/orders/)
  })

  it('deduplicates repeated route prefetch calls without full route preload', () => {
    const source = sourceFile('src/router/routeLoaders.js')

    expect(source).toContain('const prefetchedRoutes = new Set()')
    expect(source).toContain('prefetchedRoutes.has(path)')
    expect(source).toContain('prefetchedRoutes.add(path)')
    expect(source).not.toMatch(/Object\.values\(.*RouteLoaders\).*forEach/s)
  })

  it('warms all high-frequency user routes while idle without admin routes', () => {
    const source = sourceFile('src/router/routeLoaders.js')

    expect(source).toContain("'/resume/upload'")
    expect(source).toContain("'/resume/result'")
    expect(source).toContain("'/interview/entry'")
    expect(source).toContain("'/offer'")
    expect(source).toContain("const idleWarmupRoutes = ['/templates', '/community', '/growth', '/resume/upload', '/resume/result', '/interview/entry', '/offer']")
    expect(source).toContain('requestIdleCallback')
    expect(source).not.toMatch(/idleWarmupRoutes\s*=\s*\[[\s\S]*?\/admin/)
  })

  it('uses the shared resume result loader in the router so prefetch warms the same chunk', () => {
    const routerSource = sourceFile('src/router/index.js')

    expect(routerSource).toContain('resumeResultRouteLoader')
    expect(routerSource).toMatch(/path:\s*'\/resume\/result\/:taskId',[\s\S]*component:\s*resumeResultRouteLoader/)
    expect(routerSource).not.toContain("component: () => import('@/views/resume/ResultView.vue')")
  })

  it('registers the admin community review route with admin authentication', () => {
    const routerSource = sourceFile('src/router/index.js')

    expect(routerSource).toContain('adminCommunityReviewRouteLoader')
    expect(routerSource).toMatch(/path:\s*'community',[\s\S]*name:\s*'AdminCommunityReview',[\s\S]*component:\s*adminCommunityReviewRouteLoader/)
    expect(routerSource).toMatch(/path:\s*'community',[\s\S]*requiresAdminAuth:\s*true/)
  })
})
