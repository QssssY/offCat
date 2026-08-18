import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import GitHubIcon from '@/components/common/GitHubIcon.vue'

describe('GitHubIcon', () => {
  it('renders the official decorative GitHub mark using the current text color', () => {
    const wrapper = mount(GitHubIcon)
    const icon = wrapper.get('svg')

    expect(icon.classes()).toContain('github-icon')
    expect(icon.attributes('viewBox')).toBe('0 0 24 24')
    expect(icon.attributes('aria-hidden')).toBe('true')
    expect(icon.attributes('focusable')).toBe('false')
    expect(wrapper.get('path').attributes('fill')).toBe('currentColor')
  })
})
