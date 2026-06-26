import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import TemplatePreviewImage from '@/components/template/TemplatePreviewImage.vue'
import { templates } from '@/data/templates'
import { getTemplatePreviewMeta, templatePreviewMeta } from '@/data/templatePreviewMeta'

const sourceFile = (path) => readFileSync(resolve(process.cwd(), path), 'utf8')

describe('TemplatePreviewImage', () => {
  it('covers every template id with dedicated lightweight preview metadata', () => {
    const configuredIds = Object.keys(templatePreviewMeta).sort()
    const templateIds = templates.map((item) => item.id).sort()

    expect(configuredIds).toEqual(templateIds)
  })

  it.each([
    ['tech-minimal', 'minimal-photo', 'minimal', 'pill-dot'],
    ['tech-modern', 'top-band', 'gradient', 'bar'],
    ['tech-dark', 'dark-page', 'gradient', 'dark-line'],
    ['finance-classic', 'centered-authority', 'centered', 'classic-line'],
    ['education-clean', 'sidebar', 'sidebar', 'icon-dot']
  ])('uses representative style metadata for %s', (templateId, layout, headerStyle, sectionStyle) => {
    const meta = getTemplatePreviewMeta(templateId)

    expect(meta.layout).toBe(layout)
    expect(meta.headerStyle).toBe(headerStyle)
    expect(meta.sectionStyle).toBe(sectionStyle)
  })

  it('uses the real green tech-minimal accent and pale green background', () => {
    const wrapper = mount(TemplatePreviewImage, {
      props: {
        templateId: 'tech-minimal',
        color: '#0EA5E9',
        bgColor: '#ffffff'
      }
    })

    expect(wrapper.classes()).toContain('layout-minimal-photo')
    expect(wrapper.classes()).toContain('header-minimal')
    expect(wrapper.classes()).toContain('section-pill-dot')
    expect(wrapper.attributes('style')).toContain('--preview-accent: #5B7A2E')
    expect(wrapper.attributes('style')).toContain('--preview-bg: #F4F7EE')
    expect(wrapper.attributes('style')).toContain('--preview-line:')
    expect(wrapper.attributes('style')).toContain('--preview-gradient-end:')
    expect(wrapper.attributes('style')).not.toContain('--preview-accent: #0EA5E9')
  })

  it('renders the lightweight thumbnail skeleton nodes', () => {
    const wrapper = mount(TemplatePreviewImage, {
      props: { templateId: 'tech-modern' }
    })

    expect(wrapper.find('.preview-header').exists()).toBe(true)
    expect(wrapper.find('.name-line').exists()).toBe(true)
    expect(wrapper.findAll('.preview-section')).toHaveLength(3)
  })

  it('keeps the preview image lightweight and independent from full renderer CSS imports', () => {
    const previewSource = sourceFile('src/components/template/TemplatePreviewImage.vue')

    expect(previewSource).not.toContain('color-mix(')
    expect(previewSource).not.toContain('TemplateRenderer')
    expect(previewSource).not.toContain('css?raw')
    expect(previewSource).not.toContain('data/styles')
  })
})
