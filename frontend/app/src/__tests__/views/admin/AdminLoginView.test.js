import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const sourceFile = (path) => readFileSync(resolve(process.cwd(), path), 'utf8')

describe('AdminLoginView', () => {
  it('prefetches the admin shell before redirecting into the backend after login', () => {
    const source = sourceFile('src/views/admin/AdminLoginView.vue')

    expect(source).toContain("import { prefetchAdminShellRoute } from '@/router/routeLoaders'")
    expect(source).toContain('await prefetchAdminShellRoute()')
    expect(source).toContain('router.push(redirect)')
  })
})
