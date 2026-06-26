import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import OnboardingTaskCard from '@/components/OnboardingTaskCard.vue'

const mountCard = (props = {}) => mount(OnboardingTaskCard, {
  props: {
    tasks: [],
    completedCount: 0,
    totalCount: 4,
    ...props,
  },
  global: {
    stubs: {
      RouterLink: {
        props: ['to'],
        template: '<a><slot /></a>',
      },
    },
  },
})

describe('OnboardingTaskCard', () => {
  it('keeps progress ring finite when total count is zero', () => {
    const wrapper = mountCard({ totalCount: 0, completedCount: 0 })
    const ring = wrapper.find('.ring-fill')

    expect(wrapper.text()).toContain('0%')
    expect(ring.attributes('stroke-dashoffset')).not.toContain('NaN')
    expect(ring.attributes('stroke-dashoffset')).not.toContain('Infinity')
  })
})
