import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const sourceFile = (path) => readFileSync(resolve(process.cwd(), path), 'utf8')

describe('AdminLayout', () => {
  it('prefetches admin route chunks from navigation interactions', () => {
    const source = sourceFile('src/layouts/AdminLayout.vue')

    expect(source).toContain('prefetchAdminRoute')
    expect(source).toContain('社区审核')
    expect(source).toContain('"/admin/community"')
    expect(source).toContain('@mouseenter="prefetchAdminNavigationRoute(item.path)"')
    expect(source).toContain('@focus="prefetchAdminNavigationRoute(item.path)"')
    expect(source).toContain('@touchstart.passive="prefetchAdminNavigationRoute(item.path)"')
  })

  it('keeps the admin shell visible while route components load', () => {
    const source = sourceFile('src/layouts/AdminLayout.vue')

    expect(source).toContain('showAdminRouteLoading')
    expect(source).toContain('admin-route-loading-bar')
    expect(source).toContain('admin-route-loading-placeholder')
    expect(source).toContain('正在打开管理模块')
    expect(source).toContain('admin-route-loading-placeholder-card')
    expect(source).toContain('<RouterView v-slot="{ Component }">')
    expect(source).toContain('<Transition name="admin-page-fade" mode="out-in">')
    expect(source).toContain('<component :is="Component" :key="route.fullPath" />')
    expect(source).toContain('setTimeout(() =>')
    expect(source).toContain('}, 120)')
    expect(source).toContain('onBeforeUnmount')
  })

  it('warms frequent admin route chunks after admin shell is mounted', () => {
    const source = sourceFile('src/layouts/AdminLayout.vue')

    expect(source).toContain('warmupHighFrequencyAdminRoutes')
    expect(source).toContain('let adminWarmupHandle = null')
    expect(source).toContain('adminWarmupHandle = warmupHighFrequencyAdminRoutes()')
    expect(source).toContain('window.cancelIdleCallback(adminWarmupHandle)')
    expect(source).toContain('window.clearTimeout(adminWarmupHandle)')
  })

  it('uses explicit transition properties for admin navigation and page switching', () => {
    const source = sourceFile('src/layouts/AdminLayout.vue')

    expect(source).not.toContain('transition: all')
    expect(source).toMatch(/\.admin-nav-item\s*\{[\s\S]*?transition:\s*background-color 0\.2s ease,\s*color 0\.2s ease,\s*box-shadow 0\.2s ease,\s*transform 0\.2s ease;/)
    expect(source).toMatch(/\.admin-page-fade-enter-active,[\s\S]*?\.admin-page-fade-leave-active\s*\{[\s\S]*?transition:\s*opacity 0\.16s ease, transform 0\.16s ease;/)
  })
})
