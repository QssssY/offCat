import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const sourceFile = (path) => readFileSync(resolve(process.cwd(), path), 'utf8')

describe('ResumeUploadView', () => {
  it('prefetches the resume result route while the upload request is in flight', () => {
    const source = sourceFile('src/views/resume/UploadView.vue')

    expect(source).toContain("import { prefetchUserRoute } from '@/router/routeLoaders'")
    expect(source).toContain('let resumeResultPrefetchPromise = null')
    expect(source).toMatch(/resumeResultPrefetchPromise\s*=\s*prefetchUserRoute\('\/resume\/result'\)/)
    expect(source).toMatch(/await\s+resumeResultPrefetchPromise[\s\S]*await\s+router\.push\(`\/resume\/result\/\$\{taskId\}`\)/)
    expect(source).toMatch(/await\s+router\.push\(`\/resume\/result\/\$\{taskId\}`\)/)
  })
})
