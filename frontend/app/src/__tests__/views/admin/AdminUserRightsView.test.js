import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const source = () =>
  readFileSync(resolve(process.cwd(), 'src/views/admin/AdminUserRightsView.vue'), 'utf8')

describe('AdminUserRightsView ban workflow', () => {
  it('uses the new ban and unban APIs instead of the legacy status endpoints', () => {
    const componentSource = source()

    expect(componentSource).toContain('banAdminUser')
    expect(componentSource).toContain('unbanAdminUser')
    expect(componentSource).toContain('banAdminUsersBatch')
    expect(componentSource).toContain('unbanAdminUsersBatch')
    expect(componentSource).toContain('<AdminUserBanDialog')
    expect(componentSource).not.toContain('updateAdminUserStatus')
    expect(componentSource).not.toContain('updateUsersBatchStatus')
  })

  it('shows ban metadata in the user table and CSV export', () => {
    const componentSource = source()

    expect(componentSource).toContain('封禁到期')
    expect(componentSource).toContain('封禁原因')
    expect(componentSource).toContain('formatBanUntil(row)')
    expect(componentSource).toContain('row.banReason')
    expect(componentSource).toContain("['用户ID', '用户名', '角色', '状态', '封禁到期', '封禁原因'")
  })
})
