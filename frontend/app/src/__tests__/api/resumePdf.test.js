import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  push: vi.fn(),
  getToken: vi.fn(() => 'user-token'),
  getTokenType: vi.fn(() => 'Bearer'),
  removeToken: vi.fn(),
  isLoggedIn: vi.fn(() => true),
}))

vi.mock('axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

vi.mock('@/router', () => ({
  default: {
    currentRoute: { value: { fullPath: '/resume/result/1' } },
    push: mocks.push,
  },
}))

vi.mock('@/utils/auth', () => ({
  getToken: mocks.getToken,
  getTokenType: mocks.getTokenType,
  removeToken: mocks.removeToken,
  isLoggedIn: mocks.isLoggedIn,
}))

import axios from 'axios'
import { downloadPdfFile } from '@/api/resumePdf'

describe('resume PDF API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    global.URL.createObjectURL = vi.fn(() => 'blob:pdf')
    global.URL.revokeObjectURL = vi.fn()
  })

  it('downloads PDF with Authorization header instead of query token', async () => {
    axios.get.mockResolvedValueOnce({
      data: new Blob(['%PDF-1.7'], { type: 'application/pdf' }),
      headers: { 'content-type': 'application/pdf' },
    })

    await downloadPdfFile('20260520010203004', 'resume.pdf')

    expect(axios.get).toHaveBeenCalledWith('/api/resume/download-pdf/20260520010203004', {
      responseType: 'blob',
      timeout: 60000,
      headers: {
        Authorization: 'Bearer user-token',
      },
    })
    expect(axios.get.mock.calls[0][0]).not.toContain('token=')
    expect(URL.createObjectURL).toHaveBeenCalled()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:pdf')
  })
})
