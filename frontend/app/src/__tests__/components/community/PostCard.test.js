import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import PostCard from '@/components/community/PostCard.vue'

vi.mock('@/utils/optimizedImages', () => ({
  optimizedImages: {
    userAvatar: {
      webp: '/avatar.webp'
    }
  }
}))

vi.mock('@/components/community/ImageGrid.vue', () => ({
  default: {
    name: 'ImageGrid',
    props: ['images'],
    template: '<div class="image-grid-stub" />'
  }
}))

const makePost = (overrides = {}) => ({
  id: 1,
  category: 'interview_exp',
  authorName: 'tester',
  authorAvatar: '',
  createTime: '2026-05-28T10:00:00',
  content: '面试经验内容',
  title: '一次有收获的前端面试复盘',
  images: [],
  liked: false,
  likeCount: 3,
  commentCount: 2,
  favorited: false,
  ...overrides
})

const source = () => readFileSync(resolve(process.cwd(), 'src/components/community/PostCard.vue'), 'utf8')

describe('PostCard', () => {
  it('renders the post title above the content summary', () => {
    const wrapper = mount(PostCard, {
      props: {
        post: makePost()
      }
    })

    expect(wrapper.find('.post-title').text()).toBe('一次有收获的前端面试复盘')
    expect(wrapper.find('.post-summary').text()).toBe('面试经验内容')
  })

  it('keeps very long titles readable without expanding the card height', () => {
    const longTitle = Array.from({ length: 30 }, () => 'Long title').join(' ')
    const wrapper = mount(PostCard, {
      props: {
        post: makePost({ title: longTitle })
      }
    })

    const title = wrapper.find('.post-title')
    expect(title.attributes('title')).toBe(longTitle)

    const componentSource = source()
    expect(componentSource).toMatch(/\.post-title\s*\{[\s\S]*?-webkit-line-clamp:\s*2/)
    expect(componentSource).toMatch(/\.post-title\s*\{[\s\S]*?overflow:\s*hidden/)
  })

  it('collapses long content by default and toggles without opening the detail page', async () => {
    const wrapper = mount(PostCard, {
      props: {
        post: makePost({
          content: '这是一段很长的社区帖子内容，'.repeat(24)
        })
      }
    })

    expect(wrapper.find('.post-summary').classes()).toContain('collapsed')
    const toggle = wrapper.find('.content-toggle')
    expect(toggle.exists()).toBe(true)
    expect(toggle.text()).toBe('展开')

    await toggle.trigger('click')

    expect(wrapper.emitted('click')).toBeUndefined()
    expect(wrapper.find('.post-summary').classes()).not.toContain('collapsed')
    expect(wrapper.find('.content-toggle').text()).toBe('收起')
  })

  it('renders shared interview report posts as a navigable report link card', () => {
    const wrapper = mount(PostCard, {
      props: {
        post: makePost({
          title: 'Frontend Engineer 面试报告',
          content: '这次面试整体表现不错',
          sharedInterviewSessionId: 'session-1'
        })
      }
    })

    const link = wrapper.find('.report-link-card')
    expect(link.exists()).toBe(true)
    expect(link.attributes('href')).toBe('/interview/report/session-1')
    expect(link.text()).toContain('查看完整面试报告')
    expect(link.text()).toContain('Frontend Engineer 面试报告')
  })

  it('uses a fallback title for old shared report posts without a stored title', () => {
    const wrapper = mount(PostCard, {
      props: {
        post: makePost({
          title: '',
          content: '我分享了一份面试报告。',
          sharedInterviewSessionId: 'legacy-session-1'
        })
      }
    })

    expect(wrapper.find('.post-title').text()).toBe('面试报告分享')
    expect(wrapper.find('.report-link-title').text()).toBe('面试报告分享')
  })

  it('renders a visible favorited pill state with a readable icon', () => {
    const wrapper = mount(PostCard, {
      props: {
        post: makePost({ favorited: true })
      }
    })

    const favoriteButton = wrapper.find('.action-btn.favorited')
    expect(favoriteButton.exists()).toBe(true)
    expect(favoriteButton.text()).toContain('收藏')
    expect(favoriteButton.find('.feature-icon.size-sm').exists()).toBe(true)
    expect(favoriteButton.find('.feature-icon.size-xs').exists()).toBe(false)
  })

  it('emits only favorite when the favorite action is clicked', async () => {
    const wrapper = mount(PostCard, {
      props: {
        post: makePost({ favorited: true })
      }
    })

    await wrapper.find('.action-btn.favorited').trigger('click')

    expect(wrapper.emitted('favorite')).toHaveLength(1)
    expect(wrapper.emitted('click')).toBeUndefined()
  })

  it('does not render the admin hide action for normal users', () => {
    const wrapper = mount(PostCard, {
      props: {
        post: makePost(),
        canAdminHide: false
      }
    })

    expect(wrapper.find('.admin-hide-btn').exists()).toBe(false)
  })

  it('renders and emits the admin hide action for administrators', async () => {
    const wrapper = mount(PostCard, {
      props: {
        post: makePost(),
        canAdminHide: true
      }
    })

    const button = wrapper.find('.admin-hide-btn')
    expect(button.exists()).toBe(true)
    expect(button.text()).toContain('下架')

    await button.trigger('click')

    expect(wrapper.emitted('admin-hide')).toHaveLength(1)
    expect(wrapper.emitted('click')).toBeUndefined()
  })

  it('renders and emits the admin ban action for administrators', async () => {
    const post = makePost({ userId: 'user-2' })
    const wrapper = mount(PostCard, {
      props: {
        post,
        canAdminBan: true
      }
    })

    const button = wrapper.find('.admin-ban-btn')
    expect(button.exists()).toBe(true)
    expect(button.text()).toContain('封禁')

    await button.trigger('click')

    expect(wrapper.emitted('admin-ban-user')[0]).toEqual([post])
    expect(wrapper.emitted('click')).toBeUndefined()
  })

  it('uses a stronger active favorite surface without transition-all', () => {
    const componentSource = source()

    expect(componentSource).toMatch(/\.action-btn\.favorited\s*\{[\s\S]*?background:/)
    expect(componentSource).toMatch(/\.action-btn\.favorited\s*\{[\s\S]*?border-color:/)
    expect(componentSource).toMatch(/\.action-btn\.favorited\s*\{[\s\S]*?box-shadow:/)
    expect(componentSource).toContain('@media (prefers-reduced-motion: reduce)')
    expect(componentSource).not.toContain('transition: all')
  })
})
