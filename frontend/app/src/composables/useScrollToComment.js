import { nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getCommentDetail } from '@/api/community'

/**
 * 评论滚动定位 composable
 * 将 CommentSection 中的 scrollToTarget / highlightAndScroll / waitForImagesAndScroll / doScroll 提取为独立逻辑
 *
 * @param {Object} options
 * @param {import('vue').Ref<Object>} options.commentSectionRef - 评论区容器 ref
 * @param {import('vue').Ref<Array>} options.comments - 评论列表 ref
 * @param {Object} options.repliesMap - 回复列表 reactive map
 * @param {Set} options.expandedReplies - 已展开的回复集合
 * @param {Set} options.showAllReplies - 显示全部回复的集合
 * @param {Function} options.fetchReplies - 获取回复列表的函数 (parentId) => Promise
 * @param {string|number|null} options.scrollToParentId - 从 props 传入的父评论 ID
 * @param {string|number} options.postId - 帖子 ID
 */
export function useScrollToComment(options) {
  const {
    commentSectionRef,
    comments,
    repliesMap,
    expandedReplies,
    showAllReplies,
    fetchReplies,
    scrollToParentId,
    postId
  } = options

  const scrollToTarget = async (targetId) => {
    try {
      let found = comments.value.find(c => String(c.id) === String(targetId) && !c.parentCommentId)
      if (found) {
        await nextTick()
        highlightAndScroll(`comment-${targetId}`)
        return
      }

      let parentId = scrollToParentId
      if (!parentId) {
        const inList = comments.value.find(c => String(c.id) === String(targetId))
        if (inList && inList.parentCommentId) {
          parentId = inList.parentCommentId
        } else {
          const res = await getCommentDetail(postId, targetId)
          if (res.code === 200 && res.data) {
            parentId = res.data.parentCommentId
            if (!parentId) {
              if (!comments.value.find(c => String(c.id) === String(targetId))) {
                comments.value.push(res.data)
              }
              await nextTick()
              highlightAndScroll(`comment-${targetId}`)
              return
            }
          }
        }
      }

      if (parentId) {
        if (!comments.value.find(c => String(c.id) === String(parentId))) {
          const parentRes = await getCommentDetail(postId, parentId)
          if (parentRes.code === 200 && parentRes.data) {
            comments.value.push(parentRes.data)
          }
        }
        if (!repliesMap[parentId]) await fetchReplies(parentId)
        expandedReplies.add(parentId)
        showAllReplies.add(parentId)
        await nextTick()
        await new Promise(resolve => setTimeout(resolve, 400))
        highlightAndScroll(`comment-${targetId}`)
      }
    } catch (e) {
      console.error('获取目标评论失败:', e)
      ElMessage.warning('定位评论失败，可能已被删除')
    }
  }

  const highlightAndScroll = (elementId) => {
    const el = document.getElementById(elementId)
    if (el) {
      waitForImagesAndScroll(el)
      return
    }
    const container = commentSectionRef.value
    if (!container) return
    let done = false
    const finish = (target) => {
      if (done) return
      done = true
      observer.disconnect()
      waitForImagesAndScroll(target)
    }
    const observer = new MutationObserver(() => {
      const target = document.getElementById(elementId)
      if (target) finish(target)
    })
    observer.observe(container, { childList: true, subtree: true })
    const poll = () => {
      if (done) return
      const target = document.getElementById(elementId)
      if (target) { finish(target); return }
      requestAnimationFrame(poll)
    }
    requestAnimationFrame(poll)
    setTimeout(() => { if (!done) { done = true; observer.disconnect() } }, 5000)
  }

  const waitForImagesAndScroll = (el) => {
    const images = el.querySelectorAll('img')
    let pending = images.length
    let done = false
    const go = () => { if (!done) { done = true; doScroll(el) } }
    if (pending === 0) {
      go()
    } else {
      const timer = setTimeout(go, 5000)
      images.forEach(img => {
        if (done) return
        if (img.complete) {
          pending--
          if (pending === 0) { clearTimeout(timer); go() }
        } else {
          img.onload = img.onerror = () => {
            if (done) return
            pending--
            if (pending === 0) { clearTimeout(timer); go() }
          }
        }
      })
    }
  }

  const doScroll = (el) => {
    if (!el || !el.isConnected) return
    const imageGrid = el.querySelector('.image-grid')
    const scrollTarget = imageGrid || el
    requestAnimationFrame(() => {
      if (!el.isConnected) return
      const scrollContainer = document.querySelector('.layout-content')
      if (scrollContainer) {
        const containerRect = scrollContainer.getBoundingClientRect()
        const targetRect = scrollTarget.getBoundingClientRect()
        const scrollTop = scrollContainer.scrollTop + (targetRect.top - containerRect.top) - (containerRect.height / 2) + (targetRect.height / 2)
        scrollContainer.scrollTo({ top: Math.max(0, scrollTop), behavior: 'smooth' })
      } else {
        scrollTarget.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }
      el.classList.add('highlight-flash')
      setTimeout(() => el.classList.remove('highlight-flash'), 2000)
    })
  }

  return { scrollToTarget }
}
