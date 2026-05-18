import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { NButton } from 'naive-ui'
import { ElPagination } from 'element-plus'
import VersionLogView from '@/views/VersionLogView.vue'
import { getPublicVersionLogsPage } from '@/api/publicVersionLog'

vi.mock('@/api/publicVersionLog', () => ({
  getPublicVersionLogsPage: vi.fn()
}))

const mountView = () => mount(VersionLogView, {
  global: {
    stubs: {
      RouterLink: {
        props: ['to'],
        template: '<a><slot /></a>'
      }
    }
  }
})

describe('VersionLogView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render version logs returned by public page API', async () => {
    getPublicVersionLogsPage.mockResolvedValue({
      data: {
        records: [
          {
            id: 1,
            version: '2.1.0',
            type: 'minor',
            title: '优化简历诊断体验',
            content: '提升诊断结果的可读性。',
            publishedAt: '2026-05-17T08:00:00'
          }
        ],
        total: 1
      }
    })

    const wrapper = mountView()
    await flushPromises()

    expect(getPublicVersionLogsPage).toHaveBeenCalledWith({ page: 1, size: 5 })
    expect(wrapper.text()).toContain('v2.1.0')
    expect(wrapper.text()).toContain('功能优化')
    expect(wrapper.text()).toContain('优化简历诊断体验')
    expect(wrapper.text()).toContain('提升诊断结果的可读性。')
    expect(wrapper.text()).toContain('2026-05-17')
    expect(wrapper.find('.version-title').attributes('title')).toBeTruthy()
    expect(wrapper.findAll('.version-date')).toHaveLength(1)
  })

  it('should render empty state when no logs exist', async () => {
    getPublicVersionLogsPage.mockResolvedValue({
      data: { records: [], total: 0 }
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('暂无版本日志')
    expect(wrapper.text()).toContain('发布后的产品更新会展示在这里。')
  })

  it('should render error state and retry loading', async () => {
    getPublicVersionLogsPage
      .mockRejectedValueOnce(new Error('网络异常'))
      .mockResolvedValueOnce({
        data: {
          records: [
            {
              id: 2,
              version: '2.1.1',
              type: 'patch',
              title: '修复分页展示',
              content: '恢复版本日志分页展示。',
              publishedAt: '2026-05-18'
            }
          ],
          total: 1
        }
      })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('版本日志加载失败')
    expect(wrapper.text()).toContain('网络异常')

    const retryButton = wrapper.findAllComponents(NButton).find((button) => button.text() === '重新加载')
    await retryButton.trigger('click')
    await flushPromises()

    expect(getPublicVersionLogsPage).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('修复分页展示')
  })

  it('should reload logs when pagination changes', async () => {
    getPublicVersionLogsPage.mockResolvedValue({
      data: {
        records: [
          {
            id: 3,
            version: '3.0.0',
            type: 'major',
            title: '公开页重构',
            content: '重构公开版本日志页。',
            publishedAt: '2026-05-19'
          }
        ],
        total: 40
      }
    })

    const wrapper = mountView()
    await flushPromises()

    const pagination = wrapper.findComponent(ElPagination)
    await pagination.vm.$emit('current-change', 2)
    await flushPromises()

    expect(getPublicVersionLogsPage).toHaveBeenLastCalledWith({ page: 2, size: 5 })

    const updatedPagination = wrapper.findComponent(ElPagination)
    await updatedPagination.vm.$emit('size-change', 20)
    await flushPromises()

    expect(getPublicVersionLogsPage).toHaveBeenLastCalledWith({ page: 1, size: 20 })
  })

  it('should collapse long content by default and toggle expansion', async () => {
    getPublicVersionLogsPage.mockResolvedValue({
      data: {
        records: [
          {
            id: 4,
            version: '3.1.0',
            type: 'minor',
            title: '长文本展示测试',
            content: '这是很长的版本说明内容。'.repeat(20),
            publishedAt: '2026-05-20'
          }
        ],
        total: 1
      }
    })

    const wrapper = mountView()
    await flushPromises()

    const content = wrapper.find('.version-content')
    expect(content.classes()).toContain('is-collapsed')

    const toggleButton = wrapper.find('.expand-button')
    expect(toggleButton.text()).toBe('展开全文')

    await toggleButton.trigger('click')
    await flushPromises()

    expect(wrapper.find('.version-content').classes()).not.toContain('is-collapsed')
    expect(wrapper.find('.expand-button').text()).toBe('收起内容')
  })
})
