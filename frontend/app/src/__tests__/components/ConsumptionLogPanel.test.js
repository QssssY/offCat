import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ConsumptionLogPanel from '@/components/growth/ConsumptionLogPanel.vue'
import { getConsumptionLog } from '@/api/quota'

vi.mock('@/api/quota', () => ({
  getConsumptionLog: vi.fn()
}))

vi.mock('@element-plus/icons-vue', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    Headset: { name: 'Headset', template: '<span />' },
    Document: { name: 'Document', template: '<span />' },
    EditPen: { name: 'EditPen', template: '<span />' },
    Position: { name: 'Position', template: '<span />' },
    Files: { name: 'Files', template: '<span />' },
    Suitcase: { name: 'Suitcase', template: '<span />' }
  }
})

const deferred = () => {
  let resolve
  let reject
  const promise = new Promise((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

const record = (id, quotaType, quotaTypeName) => ({
  id,
  quotaType,
  quotaTypeName,
  sourceName: 'unit-test',
  description: '',
  changeAmount: 1,
  createTime: '2026-06-04T12:00:00'
})

const mountPanel = () => mount(ConsumptionLogPanel, {
  global: {
    stubs: {
      FeatureIcon: { template: '<span />' },
      'el-icon': { template: '<span><slot /></span>' }
    }
  }
})

describe('ConsumptionLogPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('ignores stale load-more response after quota type changes', async () => {
    const staleLoadMore = deferred()
    getConsumptionLog
      .mockResolvedValueOnce({
        data: { list: [record(1, 'INTERVIEW', 'initial interview')], total: 40 }
      })
      .mockImplementationOnce(() => staleLoadMore.promise)
      .mockResolvedValueOnce({
        data: { list: [record(3, 'RESUME', 'current resume')], total: 1 }
      })

    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.find('.load-more-btn').trigger('click')
    await wrapper.findAll('.filter-btn')[2].trigger('click')
    await flushPromises()

    staleLoadMore.resolve({
      data: { list: [record(2, 'INTERVIEW', 'stale interview')], total: 40 }
    })
    await flushPromises()

    expect(wrapper.findAll('.log-item')).toHaveLength(1)
    expect(wrapper.text()).toContain('current resume')
    expect(wrapper.text()).not.toContain('stale interview')
  })
})
