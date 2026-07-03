import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import TemplateCard from '@/components/template/TemplateCard.vue'
import { getTemplatePreviewMeta } from '@/data/templatePreviewMeta'

const sourceFile = (path) => readFileSync(resolve(process.cwd(), path), 'utf8')

const template = {
  id: 'tech-modern',
  name: '科技现代',
  description: '轻量模板预览',
  colorAccent: '#3B82F6',
  tags: ['现代', '技术']
}

describe('TemplateCard', () => {
  it('uses the lightweight preview image instead of mounting full resume HTML', () => {
    const wrapper = mount(TemplateCard, {
      props: { template },
      global: {
        stubs: {
          TemplatePreviewImage: {
            props: ['templateId', 'color', 'bgColor'],
            template: '<div class="template-preview-image-stub" :data-template-id="templateId" :data-color="color" :data-bg-color="bgColor" />'
          },
          ElButton: {
            template: '<button><slot /></button>'
          }
        }
      }
    })

    const preview = wrapper.find('.template-preview-image-stub')
    expect(preview.exists()).toBe(true)
    expect(preview.attributes('data-template-id')).toBe('tech-modern')
    expect(preview.attributes('data-color')).toBe('#3B82F6')
    expect(wrapper.findComponent({ name: 'TemplateRenderer' }).exists()).toBe(false)
  })

  it('keeps the list card isolated from the full template renderer path', () => {
    const cardSource = sourceFile('src/components/template/TemplateCard.vue')
    const librarySource = sourceFile('src/views/template/TemplateLibraryView.vue')
    const dialogSource = sourceFile('src/components/template/TemplatePreviewDialog.vue')
    const editorSource = sourceFile('src/views/template/TemplateEditorView.vue')

    expect(cardSource).toContain('TemplatePreviewImage')
    expect(cardSource).not.toContain('TemplateRenderer')
    expect(cardSource).not.toContain('ResizeObserver')
    expect(librarySource).toContain('defineAsyncComponent')
    expect(librarySource).toContain("import('@/components/template/TemplatePreviewDialog.vue')")
    expect(dialogSource).toContain('TemplateRenderer')
    expect(dialogSource).toContain('templateStyle')
    expect(dialogSource).toContain("import(`@/data/styles/${templateId}.css?raw`)")
    expect(dialogSource).toContain('<component :is="\'style\'" v-if="templateStyle" v-html="templateStyle" />')
    expect(editorSource).toContain('TemplateRenderer')
  })

  it('prefetches the template editor chunk and selected template assets before navigation', () => {
    const librarySource = sourceFile('src/views/template/TemplateLibraryView.vue')
    const routeLoaderSource = sourceFile('src/router/routeLoaders.js')

    expect(librarySource).toContain("import { prefetchTemplateEditorRoute } from '@/router/routeLoaders'")
    expect(librarySource).toContain('async function useTemplate(templateId)')
    expect(librarySource).toContain('await prefetchTemplateEditorRoute(templateId)')
    expect(librarySource).toContain('router.push(`/templates/editor/${templateId}`)')
    expect(routeLoaderSource).toContain('export function prefetchTemplateEditorRoute(templateId)')
    expect(routeLoaderSource).toContain("import(`@/data/contents/${templateId}.js`)")
    expect(routeLoaderSource).toContain("import(`@/data/styles/${templateId}.css?raw`)")
  })

  it('keeps thumbnail content visible instead of relying on skipped card rendering', () => {
    const cardSource = sourceFile('src/components/template/TemplateCard.vue')

    expect(cardSource).toContain('contain: layout paint style')
    expect(cardSource).not.toContain('transition: all')
    expect(cardSource).not.toContain('content-visibility: auto')
    expect(cardSource).not.toContain('contain-intrinsic-size')
  })

  it('aligns tech-minimal thumbnail colors with the real template CSS instead of stale list metadata', () => {
    const minimalPreview = getTemplatePreviewMeta('tech-minimal', {
      color: '#0EA5E9',
      bgColor: '#ffffff'
    })

    expect(minimalPreview.accent).toBe('#5B7A2E')
    expect(minimalPreview.background).toBe('#F4F7EE')
    expect(minimalPreview.accent).not.toBe('#0EA5E9')
  })
})
