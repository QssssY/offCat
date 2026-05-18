import request from '@/utils/request'
import { getToken, getTokenType } from '@/utils/auth'

/**
 * 查询当前用户通知列表（分页+筛选）
 * @param {Object} params - { page, size, readStatus, type }
 */
export function getNotifications(params) {
  return request({
    url: '/api/user/notifications',
    method: 'get',
    params
  })
}

/**
 * 获取当前用户未读通知数量
 */
export function getUnreadCount() {
  return request({
    url: '/api/user/notifications/unread-count',
    method: 'get'
  })
}

/**
 * 单条通知标记已读
 * @param {string|number} id - 通知ID
 */
export function markAsRead(id) {
  return request({
    url: `/api/user/notifications/${id}/read`,
    method: 'post'
  })
}

/**
 * 全部通知标记已读
 */
export function markAllAsRead() {
  return request({
    url: '/api/user/notifications/read-all',
    method: 'post'
  })
}

/**
 * 删除单条通知（逻辑删除）
 * @param {string|number} id - 通知ID
 */
export function deleteNotification(id) {
  return request({
    url: `/api/user/notifications/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除通知（逻辑删除）
 * @param {Array<string|number>} ids - 通知ID数组
 */
export function batchDeleteNotifications(ids) {
  return request({
    url: '/api/user/notifications/batch-delete',
    method: 'post',
    data: ids
  })
}

/**
 * 建立 SSE 通知推送连接（基于 fetch + ReadableStream）
 * 使用 fetch 代替 EventSource 以支持 Authorization header
 *
 * @param {Object} callbacks - 回调函数
 * @param {Function} callbacks.onNotification - 收到新通知时回调，参数为 { unreadCount, notification }
 * @param {Function} callbacks.onUnreadCount - 收到未读数更新时回调，参数为 { unreadCount }
 * @param {Function} callbacks.onError - 连接错误时回调
 * @returns {Object} 控制器 { abort() } 用于断开连接
 */
export function connectNotificationStream({ onNotification, onUnreadCount, onError }) {
  const controller = new AbortController();
  let reconnectTimer = null;
  /** 当前重连延迟（指数退避：5s → 10s → 20s → 40s → 60s 封顶） */
  let reconnectDelay = 5000;
  const MAX_RECONNECT_DELAY = 60000;
  /** 已连续重连次数 */
  let reconnectAttempts = 0;
  const MAX_RECONNECT_ATTEMPTS = 20;

  const connect = async () => {
    try {
      // 每次连接/重连时重新获取 token，避免 token 刷新后使用旧值
      const currentToken = getToken();
      const currentTokenType = getTokenType() || 'Bearer';
      const response = await fetch('/api/user/notifications/stream', {
        headers: {
          'Authorization': `${currentTokenType} ${currentToken}`,
          'Accept': 'text/event-stream'
        },
        signal: controller.signal
      });

      if (!response.ok) {
        throw new Error(`SSE 连接失败: ${response.status}`);
      }

      // 连接成功，重置重连计数和延迟
      reconnectAttempts = 0;
      reconnectDelay = 5000;

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop(); // 保留不完整的行

        let eventType = '';
        let eventData = '';

        for (const line of lines) {
          if (line.startsWith(':')) {
            // SSE 注释行（心跳），忽略
            continue;
          } else if (line.startsWith('event:')) {
            eventType = line.slice(6).trim();
          } else if (line.startsWith('data:')) {
            eventData = line.slice(5).trim();
          } else if (line === '' && eventData) {
            // 空行表示事件结束，处理数据（仅对 JSON 格式事件解析）
            if (eventType === 'notification' && onNotification) {
              try { onNotification(JSON.parse(eventData)); } catch (e) { console.error('[SSE] 解析 notification 失败:', e); }
            } else if (eventType === 'unread-count' && onUnreadCount) {
              try { onUnreadCount(JSON.parse(eventData)); } catch (e) { console.error('[SSE] 解析 unread-count 失败:', e); }
            }
            eventType = '';
            eventData = '';
          }
        }
      }
    } catch (e) {
      if (e.name !== 'AbortError') {
        console.error('[SSE] 连接错误:', e);
        // 指数退避自动重连（如果未被主动中断且未超过最大重试次数）
        if (!controller.signal.aborted && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
          reconnectAttempts++;
          const delay = reconnectDelay;
          // 下次延迟翻倍，封顶 60s
          reconnectDelay = Math.min(reconnectDelay * 2, MAX_RECONNECT_DELAY);
          reconnectTimer = setTimeout(() => {
            if (!controller.signal.aborted) {
              connect();
            }
          }, delay);
        } else if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
        }
        if (onError) onError(e);
      }
    }
  };

  connect();

  // 返回增强的控制器，abort 时同时清理重连定时器
  const originalAbort = controller.abort.bind(controller);
  controller.abort = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    originalAbort();
  };

  return controller;
}
