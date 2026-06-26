/**
 * 社区模块公共工具函数
 */

import { ref } from 'vue'

// 共享的当前时间 ref，每分钟更新一次，避免每个 formatTime 调用都创建 new Date()
const _now = ref(Date.now())
let _timerStarted = false

/**
 * 启动全局定时器，每 60s 刷新 _now。
 *
 * 设计决策：
 * - setInterval 不清理：此模块是 SPA 全局 singleton，生命周期与页面一致，无需 onUnmounted 清理。
 * - 60s 间隔：平衡实时性（"刚刚"→"1分钟前" 过渡平滑）与性能（避免高频定时器）。
 * - _timerStarted 防重复：formatTime 在多处组件中被调用，确保只创建一个定时器。
 */
function _startTimer() {
  if (_timerStarted) return
  _timerStarted = true
  setInterval(() => { _now.value = Date.now() }, 60000)
}

/**
 * 格式化时间为相对时间（使用共享 now ref，每分钟自动刷新）
 * @param {string|Date} time
 * @returns {string}
 */
export function formatTime(time) {
  _startTimer()
  if (!time) return ''
  const date = new Date(time)
  const diff = _now.value - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}天前`
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${month}-${day}`
}

/**
 * 板块 key 转中文标签
 * @param {string} cat
 * @returns {string}
 */
export function categoryLabel(cat) {
  const map = { interview_exp: '面试经验', referral: '内推信息' }
  return map[cat] || cat
}
