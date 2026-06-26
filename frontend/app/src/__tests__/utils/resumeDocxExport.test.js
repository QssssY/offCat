import { describe, it, expect, vi, beforeEach } from 'vitest'
import { parseHtmlToDocxRuns, convertBlockToParagraphs, exportResumeToDocx } from '@/utils/resumeDocxExport'

// 构造一份完整的测试用结构化简历模型
function buildTestModel() {
  return {
    header: {
      sectionTitle: '个人信息',
      name: { id: '1', kind: 'name', html: '<p>张三</p>', style: {} },
      jobTarget: { id: '2', kind: 'job_target', html: '<p>前端开发工程师</p>', style: {} },
      metaItems: [
        { id: '3', kind: 'meta', html: '<p>138-0000-0000</p>', style: {} },
        { id: '4', kind: 'meta', html: '<p>zhangsan@email.com</p>', style: {} },
      ],
      summaryLines: [
        { id: '5', kind: 'summary', html: '<p>3年前端开发经验，熟悉 Vue 和 React</p>', style: {} },
      ],
    },
    sections: [
      {
        id: 's1', key: 'experience', title: '工作经历',
        blocks: [
          { id: 'b1', type: 'row', rowKind: 'default', items: [{ id: 'i1', value: 'ABC 公司' }, { id: 'i2', value: '前端工程师' }, { id: 'i3', value: '2021.06 - 至今' }], style: {} },
          { id: 'b2', type: 'text', variant: 'bullet', html: '<p>负责公司核心产品的前端开发</p>', style: {} },
          { id: 'b3', type: 'text', variant: 'bullet', html: '<p>优化页面性能，<strong>首屏加载时间减少 40%</strong></p>', style: {} },
        ],
      },
      {
        id: 's2', key: 'skill', title: '专业技能',
        blocks: [
          { id: 'b4', type: 'label', label: '前端框架：', value: 'Vue 3、React、Angular', style: {} },
          { id: 'b5', type: 'text', variant: '', html: '<p>熟悉 TypeScript、Webpack、Vite 等工具链</p>', style: {} },
        ],
      },
    ],
  }
}

describe('resumeDocxExport', () => {
  describe('parseHtmlToDocxRuns', () => {
    it('应将纯文本 HTML 转为单个 TextRun 配置', () => {
      const runs = parseHtmlToDocxRuns('<p>Hello World</p>')
      const textRuns = runs.filter(r => r.text)
      expect(textRuns.length).toBeGreaterThanOrEqual(1)
      expect(textRuns.some(r => r.text.includes('Hello World'))).toBe(true)
      expect(textRuns[0].bold).toBeFalsy()
    })

    it('应识别 <strong> 标签为粗体', () => {
      const runs = parseHtmlToDocxRuns('<p>普通<strong>加粗</strong>文本</p>')
      const boldRun = runs.find(r => r.bold && r.text?.includes('加粗'))
      expect(boldRun).toBeDefined()
    })

    it('应识别 <b> 标签为粗体', () => {
      const runs = parseHtmlToDocxRuns('<p><b>加粗</b></p>')
      const boldRun = runs.find(r => r.bold && r.text?.includes('加粗'))
      expect(boldRun).toBeDefined()
    })

    it('应识别 <em> 标签为斜体', () => {
      const runs = parseHtmlToDocxRuns('<p><em>斜体</em></p>')
      const italicRun = runs.find(r => r.italics && r.text?.includes('斜体'))
      expect(italicRun).toBeDefined()
    })

    it('应处理嵌套的粗体和斜体标签', () => {
      const runs = parseHtmlToDocxRuns('<p><strong><em>粗斜</em></strong></p>')
      const bothRun = runs.find(r => r.bold && r.italics && r.text?.includes('粗斜'))
      expect(bothRun).toBeDefined()
    })

    it('应将 <br> 转为 break 标记', () => {
      const runs = parseHtmlToDocxRuns('<p>第一行<br>第二行</p>')
      const breakRun = runs.find(r => r.break)
      expect(breakRun).toBeDefined()
    })

    it('空 HTML 应返回包含空文本的数组', () => {
      const runs = parseHtmlToDocxRuns('')
      expect(runs.length).toBe(1)
      expect(runs[0].text).toBe('')
    })

    it('null 输入应返回包含空文本的数组', () => {
      const runs = parseHtmlToDocxRuns(null)
      expect(runs.length).toBe(1)
      expect(runs[0].text).toBe('')
    })

    it('should sanitize unsafe HTML before parsing detached DOM', () => {
      const runs = parseHtmlToDocxRuns('<p>Safe<strong>Bold</strong></p><script>alert("x")</script><img src=x onerror="alert(1)">')
      const text = runs.map(r => r.text || '').join('')

      expect(text).toContain('Safe')
      expect(text).toContain('Bold')
      expect(text).not.toContain('alert')
    })
  })

  describe('convertBlockToParagraphs', () => {
    it('应将普通 text block 转为一个段落', () => {
      const paragraphs = convertBlockToParagraphs({
        id: '1', type: 'text', variant: '', html: '<p>普通文本</p>', style: {},
      })
      expect(paragraphs).toHaveLength(1)
      expect(paragraphs[0].children).toBeDefined()
      expect(paragraphs[0].bullet).toBeUndefined()
    })

    it('应将 bullet variant 转为带项目符号的段落', () => {
      const paragraphs = convertBlockToParagraphs({
        id: '1', type: 'text', variant: 'bullet', html: '<p>列表项</p>', style: {},
      })
      expect(paragraphs).toHaveLength(1)
      expect(paragraphs[0].bullet).toEqual({ level: 0 })
    })

    it('应将 heading variant 转为加粗段落', () => {
      const paragraphs = convertBlockToParagraphs({
        id: '1', type: 'text', variant: 'heading', html: '<p>子标题</p>', style: {},
      })
      expect(paragraphs).toHaveLength(1)
      const boldRuns = paragraphs[0].children.filter(r => r.bold)
      expect(boldRuns.length).toBeGreaterThan(0)
    })

    it('应将 row block 转为用 | 分隔的单行段落', () => {
      const paragraphs = convertBlockToParagraphs({
        id: '1', type: 'row', rowKind: 'default',
        items: [{ id: 'a', value: 'ABC 公司' }, { id: 'b', value: '工程师' }, { id: 'c', value: '2021-2023' }],
        style: {},
      })
      expect(paragraphs).toHaveLength(1)
      expect(paragraphs[0].children[0].text).toContain('ABC 公司')
      expect(paragraphs[0].children[0].text).toContain('|')
    })

    it('应将 label block 转为加粗标签 + 普通值的段落', () => {
      const paragraphs = convertBlockToParagraphs({
        id: '1', type: 'label', label: '技能：', value: 'Vue、React', style: {},
      })
      expect(paragraphs).toHaveLength(1)
      expect(paragraphs[0].children[0].bold).toBe(true)
      expect(paragraphs[0].children[0].text).toContain('技能：')
      expect(paragraphs[0].children[1].text).toBe('Vue、React')
    })

    it('应将 banner_title 转为 Heading2 段落', () => {
      const paragraphs = convertBlockToParagraphs({
        id: '1', type: 'banner_title', title: '工作经历', style: {},
      })
      expect(paragraphs).toHaveLength(1)
      expect(paragraphs[0]._type).toBe('heading')
      expect(paragraphs[0].level).toBe('HEADING_2')
    })

    it('应将 section_title 转为 Heading3 段落', () => {
      const paragraphs = convertBlockToParagraphs({
        id: '1', type: 'section_title', html: '<p>项目经历</p>', style: {},
      })
      expect(paragraphs).toHaveLength(1)
      expect(paragraphs[0]._type).toBe('heading')
      expect(paragraphs[0].level).toBe('HEADING_3')
    })

    it('空 row items 应返回空数组', () => {
      const paragraphs = convertBlockToParagraphs({
        id: '1', type: 'row', items: [{ id: 'a', value: '' }, { id: 'b', value: '  ' }], style: {},
      })
      expect(paragraphs).toHaveLength(0)
    })

    it('null block 应返回空数组', () => {
      expect(convertBlockToParagraphs(null)).toHaveLength(0)
    })

    it('未知 block type 应返回空数组', () => {
      expect(convertBlockToParagraphs({ type: 'unknown_type' })).toHaveLength(0)
    })
  })

  describe('exportResumeToDocx', () => {
    beforeEach(() => {
      // Mock 浏览器下载相关 API
      vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:mock-url')
      vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})

      const mockLink = { href: '', download: '', click: vi.fn() }
      vi.spyOn(document, 'createElement').mockImplementation(tag => {
        if (tag === 'a') return mockLink
        return document.constructor.prototype.createElement.call(document, tag)
      })
    })

    it('空 JSON 字符串应抛出中文错误', async () => {
      await expect(exportResumeToDocx('', 'test')).rejects.toThrow('缺少结构化简历数据')
    })

    it('空对象 JSON 应抛出中文错误', async () => {
      await expect(exportResumeToDocx('{}', 'test')).rejects.toThrow('缺少结构化简历数据')
    })

    it('无效 JSON 应抛出格式异常错误', async () => {
      await expect(exportResumeToDocx('{invalid}', 'test')).rejects.toThrow('简历数据格式异常')
    })

    it('空模型（无 header 无 sections）应抛出内容为空错误', async () => {
      await expect(exportResumeToDocx('{"other": true}', 'test')).rejects.toThrow('简历内容为空')
    })

    it('完整模型应成功生成 Blob 并触发下载', async () => {
      const model = buildTestModel()
      const jsonString = JSON.stringify(model)

      // 还原 createElement mock，让 parseHtmlToDocxRuns 正常工作
      vi.restoreAllMocks()

      const clickFn = vi.fn()
      vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:mock-url')
      vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
      vi.spyOn(document, 'createElement').mockImplementation(function (tag) {
        if (tag === 'a') return { href: '', download: '', click: clickFn }
        return Document.prototype.createElement.call(document, tag)
      })

      await exportResumeToDocx(jsonString, '张三')

      expect(URL.createObjectURL).toHaveBeenCalled()
      expect(clickFn).toHaveBeenCalled()
      expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-url')
    })

    it('download click throws should still revoke object URL', async () => {
      const model = buildTestModel()
      const jsonString = JSON.stringify(model)

      vi.restoreAllMocks()

      const clickError = new Error('download blocked')
      vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:mock-url')
      vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
      vi.spyOn(document, 'createElement').mockImplementation(function (tag) {
        if (tag === 'a') return { href: '', download: '', click: vi.fn(() => { throw clickError }) }
        return Document.prototype.createElement.call(document, tag)
      })

      await expect(exportResumeToDocx(jsonString, 'resume')).rejects.toThrow('download blocked')

      expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-url')
    })
  })
})
