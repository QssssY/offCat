const criticalFeatureIconNames = [
  'home-dashboard',
  'resume-upload',
  'resume-analysis',
  'mock-interview',
  'template-library',
  'community-hub',
  'growth-center',
  'offer-assistant',
  'history-records',
  'menu',
  'dark-mode',
  'light-mode',
  'notification-center',
  'settings',
  'ai-loading',
  'loading',
  'ai-interviewer',
  'system-notifications',
  'user-profile',
  'account-security',
  'data-cleanup',
  'data-management',
  'feedback-center',
  'onboarding-task',
  'membership-credits',
  'membership-center',
  'growth-radar',
  'announcement'
]

const criticalPngIconModules = import.meta.glob([
  '../assets/feature-icons/png-fallback/old/home-dashboard.png',
  '../assets/feature-icons/png-fallback/old/resume-upload.png',
  '../assets/feature-icons/png-fallback/old/resume-analysis.png',
  '../assets/feature-icons/png-fallback/old/mock-interview.png',
  '../assets/feature-icons/png-fallback/old/template-library.png',
  '../assets/feature-icons/png-fallback/old/community-hub.png',
  '../assets/feature-icons/png-fallback/old/growth-center.png',
  '../assets/feature-icons/png-fallback/old/offer-assistant.png',
  '../assets/feature-icons/png-fallback/old/history-records.png',
  '../assets/feature-icons/png-fallback/old/settings.png',
  '../assets/feature-icons/png-fallback/old/system-notifications.png',
  '../assets/feature-icons/png-fallback/old/user-profile.png',
  '../assets/feature-icons/png-fallback/old/data-management.png',
  '../assets/feature-icons/png-fallback/old/membership-credits.png',
  '../assets/feature-icons/png-fallback/old/membership-center.png',
  '../assets/feature-icons/png-fallback/new/menu.png',
  '../assets/feature-icons/png-fallback/new/dark-mode.png',
  '../assets/feature-icons/png-fallback/new/light-mode.png',
  '../assets/feature-icons/png-fallback/new/notification-center.png',
  '../assets/feature-icons/png-fallback/new/ai-loading.png',
  '../assets/feature-icons/png-fallback/new/loading.png',
  '../assets/feature-icons/png-fallback/new/ai-interviewer.png',
  '../assets/feature-icons/png-fallback/new/account-security.png',
  '../assets/feature-icons/png-fallback/new/data-cleanup.png',
  '../assets/feature-icons/png-fallback/new/feedback-center.png',
  '../assets/feature-icons/png-fallback/new/onboarding-task.png',
  '../assets/feature-icons/png-fallback/new/growth-radar.png',
  '../assets/feature-icons/png-fallback/new/announcement.png'
], {
  eager: true,
  import: 'default'
})

const criticalWebpIconModules = import.meta.glob([
  '../assets/feature-icons/old/home-dashboard.webp',
  '../assets/feature-icons/old/resume-upload.webp',
  '../assets/feature-icons/old/resume-analysis.webp',
  '../assets/feature-icons/old/mock-interview.webp',
  '../assets/feature-icons/old/template-library.webp',
  '../assets/feature-icons/old/community-hub.webp',
  '../assets/feature-icons/old/growth-center.webp',
  '../assets/feature-icons/old/offer-assistant.webp',
  '../assets/feature-icons/old/history-records.webp',
  '../assets/feature-icons/old/settings.webp',
  '../assets/feature-icons/old/system-notifications.webp',
  '../assets/feature-icons/old/user-profile.webp',
  '../assets/feature-icons/old/data-management.webp',
  '../assets/feature-icons/old/membership-credits.webp',
  '../assets/feature-icons/old/membership-center.webp',
  '../assets/feature-icons/new/menu.webp',
  '../assets/feature-icons/new/dark-mode.webp',
  '../assets/feature-icons/new/light-mode.webp',
  '../assets/feature-icons/new/notification-center.webp',
  '../assets/feature-icons/new/ai-loading.webp',
  '../assets/feature-icons/new/loading.webp',
  '../assets/feature-icons/new/ai-interviewer.webp',
  '../assets/feature-icons/new/account-security.webp',
  '../assets/feature-icons/new/data-cleanup.webp',
  '../assets/feature-icons/new/feedback-center.webp',
  '../assets/feature-icons/new/onboarding-task.webp',
  '../assets/feature-icons/new/growth-radar.webp',
  '../assets/feature-icons/new/announcement.webp'
], {
  eager: true,
  import: 'default'
})

const pngIconModules = import.meta.glob([
  '../assets/feature-icons/png-fallback/{old,new}/*.png',
  '!../assets/feature-icons/png-fallback/old/home-dashboard.png',
  '!../assets/feature-icons/png-fallback/old/resume-upload.png',
  '!../assets/feature-icons/png-fallback/old/resume-analysis.png',
  '!../assets/feature-icons/png-fallback/old/mock-interview.png',
  '!../assets/feature-icons/png-fallback/old/template-library.png',
  '!../assets/feature-icons/png-fallback/old/community-hub.png',
  '!../assets/feature-icons/png-fallback/old/growth-center.png',
  '!../assets/feature-icons/png-fallback/old/offer-assistant.png',
  '!../assets/feature-icons/png-fallback/old/history-records.png',
  '!../assets/feature-icons/png-fallback/old/settings.png',
  '!../assets/feature-icons/png-fallback/old/system-notifications.png',
  '!../assets/feature-icons/png-fallback/old/user-profile.png',
  '!../assets/feature-icons/png-fallback/old/data-management.png',
  '!../assets/feature-icons/png-fallback/old/membership-credits.png',
  '!../assets/feature-icons/png-fallback/old/membership-center.png',
  '!../assets/feature-icons/png-fallback/new/menu.png',
  '!../assets/feature-icons/png-fallback/new/dark-mode.png',
  '!../assets/feature-icons/png-fallback/new/light-mode.png',
  '!../assets/feature-icons/png-fallback/new/notification-center.png',
  '!../assets/feature-icons/png-fallback/new/ai-loading.png',
  '!../assets/feature-icons/png-fallback/new/loading.png',
  '!../assets/feature-icons/png-fallback/new/ai-interviewer.png',
  '!../assets/feature-icons/png-fallback/new/account-security.png',
  '!../assets/feature-icons/png-fallback/new/data-cleanup.png',
  '!../assets/feature-icons/png-fallback/new/feedback-center.png',
  '!../assets/feature-icons/png-fallback/new/onboarding-task.png',
  '!../assets/feature-icons/png-fallback/new/growth-radar.png',
  '!../assets/feature-icons/png-fallback/new/announcement.png'
], {
  import: 'default'
})
const webpIconModules = import.meta.glob([
  '../assets/feature-icons/{old,new}/*.webp',
  '!../assets/feature-icons/old/home-dashboard.webp',
  '!../assets/feature-icons/old/resume-upload.webp',
  '!../assets/feature-icons/old/resume-analysis.webp',
  '!../assets/feature-icons/old/mock-interview.webp',
  '!../assets/feature-icons/old/template-library.webp',
  '!../assets/feature-icons/old/community-hub.webp',
  '!../assets/feature-icons/old/growth-center.webp',
  '!../assets/feature-icons/old/offer-assistant.webp',
  '!../assets/feature-icons/old/history-records.webp',
  '!../assets/feature-icons/old/settings.webp',
  '!../assets/feature-icons/old/system-notifications.webp',
  '!../assets/feature-icons/old/user-profile.webp',
  '!../assets/feature-icons/old/data-management.webp',
  '!../assets/feature-icons/old/membership-credits.webp',
  '!../assets/feature-icons/old/membership-center.webp',
  '!../assets/feature-icons/new/menu.webp',
  '!../assets/feature-icons/new/dark-mode.webp',
  '!../assets/feature-icons/new/light-mode.webp',
  '!../assets/feature-icons/new/notification-center.webp',
  '!../assets/feature-icons/new/ai-loading.webp',
  '!../assets/feature-icons/new/loading.webp',
  '!../assets/feature-icons/new/ai-interviewer.webp',
  '!../assets/feature-icons/new/account-security.webp',
  '!../assets/feature-icons/new/data-cleanup.webp',
  '!../assets/feature-icons/new/feedback-center.webp',
  '!../assets/feature-icons/new/onboarding-task.webp',
  '!../assets/feature-icons/new/growth-radar.webp',
  '!../assets/feature-icons/new/announcement.webp'
], {
  import: 'default'
})

const featureIconNames = [
  'home-dashboard',
  'resume-upload',
  'resume-analysis',
  'resume-optimization',
  'mock-interview',
  'interview-report',
  'history-records',
  'template-library',
  'community-hub',
  'growth-center',
  'offer-assistant',
  'settings',
  'resume-notifications',
  'resume-polish-notifications',
  'interview-notifications',
  'membership-credits',
  'system-notifications',
  'event-notifications',
  'user-profile',
  'membership-center',
  'beginner-guide',
  'data-management',
  'account-security',
  'ai-interviewer',
  'ai-loading',
  'announcement',
  'attachment',
  'back',
  'close',
  'collapse',
  'comment',
  'community-activity',
  'copy',
  'dark-mode',
  'data-cleanup',
  'delete',
  'download',
  'edit',
  'empty-state',
  'error',
  'exit-fullscreen',
  'expand',
  'favorite',
  'feedback-center',
  'fullscreen',
  'growth-milestone',
  'growth-radar',
  'image-upload',
  'interview-answer',
  'interview-end',
  'interview-feedback',
  'interview-pause',
  'interview-question',
  'interview-replay',
  'interview-start',
  'job-match-analysis',
  'light-mode',
  'liked',
  'loading',
  'mark-read',
  'menu',
  'message',
  'microphone-off',
  'microphone-on',
  'more',
  'next',
  'notification-center',
  'notification-settings',
  'offer-comparison',
  'onboarding-task',
  'password',
  'preview',
  'previous',
  'privacy',
  'processing',
  'profile-edit',
  'resume-editor',
  'resume-export',
  'resume-score',
  'retry',
  'salary-negotiation',
  'salary-script',
  'save',
  'search',
  'security-question',
  'share',
  'success',
  'template-editor',
  'unread',
  'upload-file',
  'version-log',
  'voice-interview',
  'voice-settings',
  'warning'
]

const iconFilenamePattern = /\/([^/]+)\.(png|webp)$/

const toIconMap = (modules) =>
  Object.entries(modules).reduce((icons, [path, src]) => {
    const match = path.match(iconFilenamePattern)
    if (match) {
      icons[match[1]] = src
    }
    return icons
  }, {})

const criticalPngFeatureIconsByName = toIconMap(criticalPngIconModules)
const criticalWebpFeatureIconsByName = toIconMap(criticalWebpIconModules)
const pngFeatureIconLoadersByName = toIconMap(pngIconModules)
const webpFeatureIconLoadersByName = toIconMap(webpIconModules)

export const featureIcons = Object.fromEntries(
  criticalFeatureIconNames.map((name) => [name, criticalPngFeatureIconsByName[name]])
)

export const criticalFeatureIconSources = Object.fromEntries(
  criticalFeatureIconNames.map((name) => [
    name,
    {
      png: criticalPngFeatureIconsByName[name],
      webp: criticalWebpFeatureIconsByName[name]
    }
  ])
)

export const featureIconSources = criticalFeatureIconSources

export const featureIconLabels = {
  'home-dashboard': '首页看板',
  'resume-upload': '简历上传',
  'resume-analysis': '简历分析',
  'resume-optimization': '简历优化',
  'mock-interview': '模拟面试',
  'interview-report': '面试报告',
  'history-records': '历史记录',
  'template-library': '模板库',
  'community-hub': '社区中心',
  'growth-center': '成长中心',
  'offer-assistant': 'Offer 辅助',
  settings: '设置',
  'resume-notifications': '简历通知',
  'resume-polish-notifications': '简历润色通知',
  'interview-notifications': '面试通知',
  'membership-credits': '会员与额度',
  'system-notifications': '系统通知',
  'event-notifications': '活动通知',
  'user-profile': '用户资料',
  'membership-center': '会员中心',
  'beginner-guide': '新手引导',
  'data-management': '数据管理',

  'account-security': '账号安全',
  'ai-interviewer': 'AI 面试官',
  'ai-loading': 'AI 加载',
  announcement: '公告',
  attachment: '附件',
  back: '返回',
  close: '关闭',
  collapse: '收起',
  comment: '评论',
  'community-activity': '社区动态',
  copy: '复制',
  'dark-mode': '深色模式',
  'data-cleanup': '数据清理',
  delete: '删除',
  download: '下载',
  edit: '编辑',
  'empty-state': '空状态',
  error: '错误',
  'exit-fullscreen': '退出全屏',
  expand: '展开',
  favorite: '收藏',
  'feedback-center': '反馈中心',
  fullscreen: '全屏',
  'growth-milestone': '成长里程碑',
  'growth-radar': '成长雷达',
  'image-upload': '图片上传',
  'interview-answer': '面试回答',
  'interview-end': '结束面试',
  'interview-feedback': '面试反馈',
  'interview-pause': '暂停面试',
  'interview-question': '面试问题',
  'interview-replay': '面试复盘',
  'interview-start': '开始面试',
  'job-match-analysis': '岗位匹配分析',
  'light-mode': '浅色模式',
  liked: '已点赞',
  loading: '加载中',
  'mark-read': '标记已读',
  menu: '菜单',
  message: '消息',
  'microphone-off': '麦克风关闭',
  'microphone-on': '麦克风开启',
  more: '更多',
  next: '下一步',
  'notification-center': '通知中心',
  'notification-settings': '通知设置',
  'offer-comparison': 'Offer 对比',
  'onboarding-task': '新手任务',
  password: '密码',
  preview: '预览',
  previous: '上一步',
  privacy: '隐私',
  processing: '处理中',
  'profile-edit': '资料编辑',
  'resume-editor': '简历编辑',
  'resume-export': '简历导出',
  'resume-score': '简历评分',
  retry: '重试',
  'salary-negotiation': '薪资谈判',
  'salary-script': '谈薪话术',
  save: '保存',
  search: '搜索',
  'security-question': '安全问题',
  share: '分享',
  success: '成功',
  'template-editor': '模板编辑',
  unread: '未读',
  'upload-file': '上传文件',
  'version-log': '版本日志',
  'voice-interview': '语音面试',
  'voice-settings': '语音设置',
  warning: '警告'
}

/**
 * 按业务语义读取本地裁剪图标。未命中时兜底到系统通知图标，
 * 保证旧图标和新增图标可以并存，同时避免页面出现破图。
 */
export function getFeatureIcon(name) {
  return featureIcons[name] || featureIcons['system-notifications']
}

export function getCriticalFeatureIconSource(name) {
  return criticalFeatureIconSources[name] || null
}

export function getFeatureIconSource(name) {
  return getCriticalFeatureIconSource(name) || criticalFeatureIconSources['system-notifications']
}

export async function loadFeatureIconSource(name) {
  const resolvedName = featureIconNames.includes(name) ? name : 'system-notifications'
  const webpLoader = webpFeatureIconLoadersByName[resolvedName]
  const pngLoader = pngFeatureIconLoadersByName[resolvedName]

  if (!webpLoader || !pngLoader) {
    return criticalFeatureIconSources['system-notifications']
  }

  const [webp, png] = await Promise.all([webpLoader(), pngLoader()])
  return { webp, png }
}

export function getFeatureIconLabel(name) {
  return featureIconLabels[name] || '功能图标'
}
