import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const sourceFile = (path) => readFileSync(resolve(process.cwd(), path), 'utf8')

describe('routeLoaders', () => {
  it('prefetches only selected high-frequency user routes', () => {
    const source = sourceFile('src/router/routeLoaders.js')

    expect(source).toContain("'/templates': templateLibraryRouteLoader")
    expect(source).toContain("'/community': communityRouteLoader")
    expect(source).toContain("'/growth': growthCenterRouteLoader")
    expect(source).toContain("'/resume/upload': resumeUploadRouteLoader")
    expect(source).toContain("'/interview/entry': interviewEntryRouteLoader")
    expect(source).toContain("'/offer': offerAssistRouteLoader")
    expect(source).not.toContain("'/settings':")
    expect(source).not.toContain("'/admin")
  })

  it('deduplicates repeated route prefetch calls without full route preload', () => {
    const source = sourceFile('src/router/routeLoaders.js')

    expect(source).toContain('const prefetchedRoutes = new Set()')
    expect(source).toContain('prefetchedRoutes.has(path)')
    expect(source).toContain('prefetchedRoutes.add(path)')
    expect(source).not.toMatch(/Object\.values\(.*RouteLoaders\).*forEach/s)
  })

  it('warms only the three heaviest routes while idle', () => {
    const source = sourceFile('src/router/routeLoaders.js')

    expect(source).toContain("const idleWarmupRoutes = ['/templates', '/community', '/growth']")
    expect(source).toContain('requestIdleCallback')
    expect(source).not.toContain("const idleWarmupRoutes = ['/templates', '/community', '/growth', '/resume/upload'")
  })
})
