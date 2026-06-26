/**
 * 错误码到用户友好提示的映射表。
 * 后端返回业务错误码时，前端优先使用此映射展示结构化提示。
 * 未命中映射的错误码回退展示后端原始 message。
 */

export const ERROR_MESSAGES = {
  // === 简历模块 (2xxx) ===
  2001: {
    title: '上传文件不能为空',
    description: '请选择一个简历文件后重新上传'
  },
  2002: {
    title: '文件格式不支持',
    description: '建议使用 PDF 格式的简历文件'
  },
  2003: {
    title: '文件过大',
    description: '请压缩文件后重试'
  },
  2004: {
    title: '简历解析失败',
    description: '文件可能已损坏或内容为空，请尝试重新上传'
  },
  2005: {
    title: '今日诊断次数已用完',
    description: '升级会员可获取更多诊断次数，或明天再试'
  },
  2006: {
    title: '文件路径异常',
    description: '请重新上传简历文件'
  },
  2007: {
    title: '简历诊断任务不存在',
    description: '该任务可能已被删除或未创建成功'
  },
  2008: {
    title: '无权访问该简历诊断任务',
    description: '只能查看自己的诊断任务'
  },
  2009: {
    title: '文件保存失败',
    description: '请稍后重试，如持续失败请联系客服'
  },
  2010: {
    title: '简历文件清理失败',
    description: '记录已处理，但部分文件清理未完成'
  },
  2011: {
    title: '该任务不可重试',
    description: '只有失败的诊断任务才能重试'
  },
  2012: {
    title: '重试时效已过',
    description: '失败超过 24 小时的任务请重新上传简历'
  },
  2013: {
    title: '今日AI润色次数已用完',
    description: '明天可继续使用，或升级会员获取更多次数'
  },
  2014: {
    title: '该简历已使用过AI润色',
    description: '每份简历只能润色一次'
  },
  2015: {
    title: '今日JD匹配次数已用完',
    description: '明天可继续使用'
  },
  2016: {
    title: '今日模板使用次数已用完',
    description: '明天可继续使用'
  },

  // === 面试模块 (3xxx) ===
  3001: {
    title: '今日模拟面试次数已用完',
    description: '升级会员可获取更多面试次数，或明天再试'
  },
  3002: {
    title: '面试会话不存在',
    description: '该会话可能已过期或未创建成功'
  },
  3003: {
    title: '无权访问该面试会话',
    description: '只能查看自己的面试会话'
  },
  3004: {
    title: '面试会话已结束',
    description: '该会话已结束，无法继续发送消息'
  },
  3005: {
    title: 'AI 生成超时',
    description: '请稍后重试'
  },

  // === AI 服务 (4xxx) ===
  4001: {
    title: 'AI 服务暂时不可用',
    description: '请稍后重试'
  },
  4002: {
    title: 'AI 返回结果为空',
    description: '请稍后重试'
  },
  4003: {
    title: 'AI 响应解析失败',
    description: '请稍后重试'
  },
  4004: {
    title: 'AI 调用配额不足',
    description: '请联系管理员'
  },

  // === 会员与支付 (5xxx) ===
  5001: {
    title: '会员套餐不存在或已停用',
    description: '请刷新页面后重试'
  },
  5002: {
    title: '账号已被禁用',
    description: '如需帮助请联系客服'
  },
  5003: {
    title: '用户不存在',
    description: '请确认账号信息后重试'
  },
  5004: {
    title: '用户未登录',
    description: '请先登录后继续操作'
  },
  5005: {
    title: '该功能为会员专属',
    description: '升级会员即可解锁全部功能'
  },
  5006: {
    title: '今日Offer辅助次数已用完',
    description: '明天可继续使用'
  },
  5007: {
    title: '功能使用次数已达上限',
    description: '升级会员获取更多使用次数'
  },
  5008: {
    title: '无法降级套餐',
    description: '已订阅更高级别套餐，仅支持续费或升级'
  },

  // === 管理端 (6xxx) ===
  6001: {
    title: '配置不存在',
    description: '该配置可能已被删除'
  },
  6002: {
    title: '编码已存在',
    description: '请使用不同的编码'
  },
  6003: {
    title: '批量操作数量超限',
    description: '请减少操作数量后重试'
  },
  6004: {
    title: 'Prompt 不存在',
    description: '该 Prompt 可能已被删除'
  },
  6005: {
    title: 'AI 引擎配置不存在',
    description: '该引擎配置可能已被删除'
  },
  6006: {
    title: '岗位配置不存在',
    description: '该岗位配置可能已被删除'
  }
}

/**
 * 根据错误码获取用户友好提示。
 * 优先匹配精确码，未命中则回退到后端原始 message。
 *
 * @param {number|string} code 后端返回的业务错误码
 * @param {string} fallbackMessage 后端返回的原始 message，作为兜底
 * @returns {{ title: string, description: string } | null}
 */
export function getErrorMessage(code, fallbackMessage) {
  const mapped = ERROR_MESSAGES[code]
  if (mapped) {
    return mapped
  }
  if (fallbackMessage) {
    return { title: fallbackMessage, description: '' }
  }
  return null
}
