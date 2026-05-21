import { describe, it, expect } from 'vitest'
import { ERROR_MESSAGES, getErrorMessage } from '@/utils/errorMessages'

describe('errorMessages', () => {
  describe('ERROR_MESSAGES 映射表', () => {
    it('应包含简历模块 2xxx 错误码', () => {
      expect(ERROR_MESSAGES[2001]).toBeDefined()
      expect(ERROR_MESSAGES[2005]).toBeDefined()
      expect(ERROR_MESSAGES[2010]).toBeDefined()
    })

    it('应包含面试模块 3xxx 错误码', () => {
      expect(ERROR_MESSAGES[3001]).toBeDefined()
      expect(ERROR_MESSAGES[3005]).toBeDefined()
    })

    it('应包含 AI 服务 4xxx 错误码', () => {
      expect(ERROR_MESSAGES[4001]).toBeDefined()
      expect(ERROR_MESSAGES[4004]).toBeDefined()
    })

    it('应包含会员模块 5xxx 错误码', () => {
      expect(ERROR_MESSAGES[5001]).toBeDefined()
      expect(ERROR_MESSAGES[5004]).toBeDefined()
    })

    it('应包含管理端 6xxx 错误码', () => {
      expect(ERROR_MESSAGES[6001]).toBeDefined()
      expect(ERROR_MESSAGES[6006]).toBeDefined()
    })

    it('每个映射项应有 title 和 description', () => {
      for (const [code, mapping] of Object.entries(ERROR_MESSAGES)) {
        expect(mapping).toHaveProperty('title')
        expect(mapping.title).toBeTruthy()
        expect(mapping).toHaveProperty('description')
      }
    })
  })

  describe('getErrorMessage', () => {
    it('已知错误码返回映射提示', () => {
      const result = getErrorMessage(2005, 'fallback')
      expect(result).not.toBeNull()
      expect(result.title).toBe('今日诊断次数已用完')
    })

    it('文件格式错误提示应只引导上传 PDF', () => {
      const result = getErrorMessage(2002, 'fallback')
      expect(result).not.toBeNull()
      expect(result.description).toBe('建议使用 PDF 格式的简历文件')
    })

    it('未知错误码回退到后端 message', () => {
      const result = getErrorMessage(9999, '后端返回的消息')
      expect(result).not.toBeNull()
      expect(result.title).toBe('后端返回的消息')
    })

    it('未知错误码且无 fallback 返回 null', () => {
      const result = getErrorMessage(9999, '')
      expect(result).toBeNull()
    })

    it('null code 返回 null 或 fallback', () => {
      const result = getErrorMessage(null, '兜底消息')
      expect(result.title).toBe('兜底消息')
    })

    it('数值型和字符串型 code 都能匹配', () => {
      const numericResult = getErrorMessage(2001, '')
      const stringResult = getErrorMessage('2001', '')
      expect(numericResult.title).toBe(stringResult.title)
    })
  })
})
