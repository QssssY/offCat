import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useThemeStore } from '@/stores/theme'

describe('theme store', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
    setActivePinia(createPinia())
    vi.stubGlobal('matchMedia', vi.fn(() => ({
      matches: false,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn()
    })))
  })

  it('persists manual theme preference in localStorage', () => {
    const themeStore = useThemeStore()

    themeStore.setTheme('dark')

    expect(localStorage.getItem('theme')).toBe('dark')
    expect(localStorage.getItem('followSystem')).toBe('false')
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })

  it('restores saved theme preference when store is created again', () => {
    localStorage.setItem('theme', 'dark')
    localStorage.setItem('followSystem', 'false')

    const themeStore = useThemeStore()

    expect(themeStore.manualTheme).toBe('dark')
    expect(themeStore.followSystem).toBe(false)
    expect(themeStore.resolvedTheme).toBe('dark')
  })
})
