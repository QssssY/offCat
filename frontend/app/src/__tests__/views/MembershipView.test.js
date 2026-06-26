import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import MembershipView from '@/views/MembershipView.vue'
import { getMembershipPlans, mockUpgradeMembership } from '@/api/membership'
import { ElMessage } from 'element-plus'

const fetchUserInfo = vi.fn(() => Promise.resolve())

const userInfo = {
  username: 'offer-user',
  nickname: '求职用户',
  role: 1,
  membershipPlanCode: 'vip_month',
  vipExpireTime: '2026-08-01T10:30:00',
  resumeQuota: 5,
  interviewQuota: 10
}

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    userInfo,
    fetchUserInfo
  })
}))

vi.mock('@/api/membership', () => ({
  getMembershipPlans: vi.fn(),
  mockUpgradeMembership: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    warning: vi.fn()
  }
}))

const plans = [
  {
    planCode: 'vip_month',
    planName: 'Monthly VIP',
    description: '后台配置的月卡介绍词',
    priceAmount: 19.9,
    durationDays: 30,
    sort: 1,
    resumeQuota: 8,
    interviewQuota: 12
  },
  {
    planCode: 'vip_quarter',
    planName: 'Quarterly VIP',
    description: '后台配置的季卡介绍词',
    priceAmount: 49.9,
    durationDays: 90,
    sort: 2,
    resumeQuota: 15,
    interviewQuota: 25
  }
]

const source = () => readFileSync(resolve(process.cwd(), 'src/views/MembershipView.vue'), 'utf8')

const mountView = async () => {
  const wrapper = mount(MembershipView)
  await flushPromises()
  return wrapper
}

describe('MembershipView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getMembershipPlans.mockResolvedValue({ data: plans })
    mockUpgradeMembership.mockResolvedValue({ data: { success: true } })
  })

  it('renders the membership workbench structure and current benefits', async () => {
    const wrapper = await mountView()

    expect(wrapper.find('.membership-view').exists()).toBe(true)
    expect(wrapper.find('.membership-workbench-hero').exists()).toBe(true)
    expect(wrapper.find('.membership-status-panel').exists()).toBe(true)
    expect(wrapper.find('.membership-quota-strip').exists()).toBe(true)
    expect(wrapper.find('.plan-comparison-section').exists()).toBe(true)
    expect(wrapper.text()).toContain('会员用户')
    expect(wrapper.text()).toContain('月度会员')
    expect(wrapper.text()).toContain('后台配置的月卡介绍词')
    expect(wrapper.text()).toContain('2026-08-01 10:30')
    expect(wrapper.text()).toContain('8')
    expect(wrapper.text()).toContain('12')
  })

  it('does not generate frontend-only plan tags or render more than six plans', async () => {
    const sevenPlans = Array.from({ length: 7 }, (_, index) => ({
      planCode: `vip_${index + 1}`,
      planName: `Custom VIP ${index + 1}`,
      description: `管理端介绍 ${index + 1}`,
      priceAmount: 10 + index,
      durationDays: 30,
      resumeQuota: 3 + index,
      interviewQuota: 6 + index
    }))
    getMembershipPlans.mockResolvedValueOnce({ data: sevenPlans })

    const wrapper = await mountView()

    expect(wrapper.findAll('.plan-card')).toHaveLength(6)
    expect(wrapper.text()).toContain('管理端介绍 1')
    expect(wrapper.text()).not.toContain('管理端介绍 7')
    expect(wrapper.text()).not.toContain('杞婚噺寮€鍚')
    expect(wrapper.text()).not.toContain('鐑棬鎺ㄨ崘')
    expect(wrapper.text()).not.toContain('楂橀鎺ㄨ崘')
  })

  it('renders renewal and upgrade actions without blocking current plan purchase', async () => {
    const wrapper = await mountView()
    const planCards = wrapper.findAll('.plan-card')

    expect(planCards).toHaveLength(2)
    expect(planCards[0].text()).toContain('续费')
    expect(planCards[1].text()).toContain('立即升级')
  })

  it('shows unavailable notice for renewal and upgrade without calling payment endpoint', async () => {
    const wrapper = await mountView()

    await wrapper.findAll('.plan-card')[0].find('button').trigger('click')
    await wrapper.findAll('.plan-card')[1].find('button').trigger('click')
    await flushPromises()

    expect(ElMessage.warning).toHaveBeenCalledTimes(2)
    expect(ElMessage.warning).toHaveBeenCalledWith('当前未开放充值功能，请联系管理员进行升级')
    expect(mockUpgradeMembership).not.toHaveBeenCalled()
    expect(fetchUserInfo).not.toHaveBeenCalled()
  })

  it('renders empty and loading states', async () => {
    getMembershipPlans.mockResolvedValueOnce({ data: [] })
    const emptyWrapper = await mountView()

    expect(emptyWrapper.find('.empty-card').exists()).toBe(true)

    let resolvePlans
    getMembershipPlans.mockReturnValueOnce(new Promise((resolve) => {
      resolvePlans = resolve
    }))

    const loadingWrapper = mount(MembershipView)
    await loadingWrapper.vm.$nextTick()

    expect(loadingWrapper.find('.plan-skeleton-card').exists()).toBe(true)
    resolvePlans({ data: plans })
    await flushPromises()
  })

  it('keeps motion and icon constraints maintainable', () => {
    const viewSource = source()

    expect(viewSource).toContain('@media (prefers-reduced-motion: reduce)')
    expect(viewSource).not.toContain('transition: all')
    expect(viewSource).not.toContain('hero-orb')
    expect(viewSource).not.toContain('size="xs"')
  })
})
