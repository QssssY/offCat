import { ElMessage, ElMessageBox } from 'element-plus'

const ADMIN_ERROR_DEDUPE_WINDOW_MS = 1200
let lastAdminErrorMessage = ''
let lastAdminErrorShownAt = 0

/**
 * 管理端统一提示文案。
 * 作用：
 * 1. 集中维护跨页面共用的反馈文案，避免同义提示出现多种写法。
 * 2. 后续若要统一语气或多语言，只需修改一个位置。
 */
export const ADMIN_FEEDBACK_TEXT = {
  sessionExpired: '管理端会话已失效，请重新登录',
  forbidden: '当前账号无管理端访问权限，请联系管理员授权',
  notFound: '管理端接口不存在，请联系开发排查',
  serverError: '管理端服务异常，请稍后重试',
  networkError: '管理端网络异常，请检查连接',
  requestFailed: '管理端请求失败，请稍后重试',
  tableEmptyDefault: '暂无数据，请稍后刷新。',
  tableEmptyFiltered: '暂无匹配数据，请调整筛选条件后重试。'
}

/**
 * 管理端成功提示。
 * @param {string} message
 */
export const showAdminSuccess = (message) => {
  ElMessage.success(message || '操作成功')
}

/**
 * 管理端失败提示。
 * @param {string} message
 */
export const showAdminError = (message) => {
  const resolvedMessage = message || ADMIN_FEEDBACK_TEXT.requestFailed
  const now = Date.now()

  // 管理端请求层和页面 catch 可能在同一错误链路里先后触发，短时间内相同文案只展示一次。
  if (
    resolvedMessage === lastAdminErrorMessage
    && now - lastAdminErrorShownAt < ADMIN_ERROR_DEDUPE_WINDOW_MS
  ) {
    return
  }

  lastAdminErrorMessage = resolvedMessage
  lastAdminErrorShownAt = now
  ElMessage.error(resolvedMessage)
}

/**
 * 管理端警告提示。
 * @param {string} message
 */
export const showAdminWarning = (message) => {
  ElMessage.warning(message || '请确认后再继续')
}

/**
 * 统一生成状态码对应提示。
 * 作用：将状态码和展示文案解耦，减少各页面分支重复。
 * @param {number} status
 * @param {string | undefined} backendMessage
 * @returns {string}
 */
export const resolveAdminStatusErrorMessage = (status, backendMessage) => {
  if (backendMessage) return backendMessage
  if (status === 401) return ADMIN_FEEDBACK_TEXT.sessionExpired
  if (status === 403) return ADMIN_FEEDBACK_TEXT.forbidden
  if (status === 404) return ADMIN_FEEDBACK_TEXT.notFound
  if (status >= 500) return ADMIN_FEEDBACK_TEXT.serverError
  return ADMIN_FEEDBACK_TEXT.requestFailed
}

/**
 * 管理端风险确认弹窗。
 * 作用：统一“启停/变更/删除”等风险确认弹窗风格，减少页面重复代码。
 * @param {{title?: string, actionText: string, targetName?: string, impactHint?: string, type?: 'warning' | 'error' | 'info' | 'success'}} options
 * @returns {Promise<void>}
 */
export const confirmAdminRiskAction = async (options) => {
  const {
    title = '风险确认',
    actionText,
    targetName,
    impactHint,
    type = 'warning'
  } = options || {}

  const targetText = targetName ? `「${targetName}」` : '目标项'
  const lines = [`确认${actionText}${targetText}吗？`]
  if (impactHint) {
    lines.push(impactHint)
  }

  await ElMessageBox.confirm(lines.join('\n'), title, { type })
}

/**
 * 统一列表空状态文案。
 * 作用：
 * 1. 区分“系统暂无数据”和“筛选后无结果”两类场景，避免误导。
 * 2. 统一管理端列表页空状态语气，降低跨页面认知切换成本。
 * @param {number} totalCount
 * @param {number} filteredCount
 * @returns {string}
 */
export const resolveAdminTableEmptyText = (totalCount, filteredCount) => {
  if (Number(totalCount) <= 0) {
    return ADMIN_FEEDBACK_TEXT.tableEmptyDefault
  }
  if (Number(filteredCount) <= 0) {
    return ADMIN_FEEDBACK_TEXT.tableEmptyFiltered
  }
  return ADMIN_FEEDBACK_TEXT.tableEmptyDefault
}
