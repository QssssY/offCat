import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import ResumeTemplate from '@/components/resume/ResumeTemplate.vue'

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
  },
}))

const editorStubs = {
  ResumeInlineRichEditor: {
    name: 'ResumeInlineRichEditor',
    props: ['field'],
    template: '<span class="inline-rich-editor-stub" v-html="field.html"></span>',
  },
  ResumeRichBlockEditor: {
    name: 'ResumeRichBlockEditor',
    props: ['block'],
    template: '<div class="rich-block-editor-stub" v-html="block.html"></div>',
  },
}

describe('ResumeTemplate', () => {
  it('renders parsed text after initializing editable history', () => {
    const wrapper = mount(ResumeTemplate, {
      props: {
        text: [
          '个人信息',
          '姓名:李浩然',
          '求职意向:软件测试工程师',
          '',
          '教育背景',
          '北京信息科技大学 | 计算机科学与技术 | 本科 | 2020-2024',
        ].join('\n'),
      },
      global: {
        stubs: editorStubs,
      },
    })

    expect(wrapper.find('.resume-paper').exists()).toBe(true)
    expect(wrapper.text()).toContain('李浩然')
    expect(wrapper.text()).toContain('软件测试工程师')
  })
})
