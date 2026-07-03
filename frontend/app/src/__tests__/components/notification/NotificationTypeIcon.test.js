import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import NotificationTypeIcon from '@/components/notification/NotificationTypeIcon.vue'

const componentSource = () =>
  readFileSync(resolve(process.cwd(), 'src/components/notification/NotificationTypeIcon.vue'), 'utf8')

describe('NotificationTypeIcon', () => {
  it('does not add a halo to static notification icons by default', () => {
    const wrapper = mount(NotificationTypeIcon, {
      props: {
        type: 'resume',
        size: 'md'
      },
      global: {
        stubs: {
          FeatureIcon: {
            props: ['name', 'size'],
            template: '<span class="feature-icon-stub" :data-name="name" :data-size="size"></span>'
          }
        }
      }
    })

    expect(wrapper.element.tagName).toBe('SPAN')
    expect(wrapper.classes()).toContain('notification-type-icon')
    expect(wrapper.classes()).toContain('type-resume')
    expect(wrapper.classes()).toContain('size-md')
    expect(wrapper.classes()).not.toContain('notification-icon-halo')
    expect(wrapper.find('.feature-icon-stub').attributes('data-size')).toBe('md')
  })

  it('adds a compact halo only when explicitly requested by an interactive surface', () => {
    const wrapper = mount(NotificationTypeIcon, {
      props: {
        type: 'resume',
        size: 'sm',
        halo: true
      },
      global: {
        stubs: {
          FeatureIcon: {
            props: ['name', 'size'],
            template: '<span class="feature-icon-stub" :data-name="name" :data-size="size"></span>'
          }
        }
      }
    })

    expect(wrapper.classes()).toContain('notification-icon-halo')
    expect(wrapper.classes()).toContain('size-sm')
    expect(wrapper.find('.feature-icon-stub').attributes('data-size')).toBe('sm')
  })

  it('does not define type-specific solid backgrounds or global halos', () => {
    const source = componentSource()

    expect(source).toContain('default: false')
    expect(source).toContain('notification-icon-halo')
    expect(source).toContain('prefers-reduced-motion: reduce')
    expect(source).toMatch(/\.size-sm\s*\{[\s\S]*?width:\s*34px/)
    expect(source).toMatch(/\.size-md\s*\{[\s\S]*?width:\s*44px/)
    expect(source).not.toMatch(/\.type-(resume|polish|update|interview|quota|maintenance|system|unknown|activity)\s*\{[\s\S]*?background:/)
    expect(source).not.toContain('transition: all')
  })
})
