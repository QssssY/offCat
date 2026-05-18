<template>
  <div class="notification-page">
    <div class="page-back">
      <el-button link @click="goHome" class="back-btn">
        <el-icon><ArrowLeft /></el-icon>
        返回首页
      </el-button>
    </div>

    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">消息通知</h1>
        <span class="unread-badge" v-if="unreadCount > 0">{{ unreadCount }} 条未读</span>
      </div>
      <el-button
        v-if="unreadCount > 0"
        type="primary"
        plain
        size="small"
        @click="handleMarkAllRead"
        :loading="markAllLoading"
      >
        全部已读
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select v-model="filterType" placeholder="通知类型" clearable size="small" @change="handleFilterChange">
        <el-option label="全部类型" value="" />
        <el-option label="简历诊断" value="resume" />
        <el-option label="AI 润色" value="polish" />
        <el-option label="模拟面试" value="interview" />
        <el-option label="额度提醒" value="quota" />
        <el-option label="系统通知" value="system" />
        <el-option label="活动公告" value="activity" />
        <el-option label="版本公告" value="update" />
        <el-option label="维护公告" value="maintenance" />
      </el-select>
      <el-select v-model="filterReadStatus" placeholder="已读状态" clearable size="small" @change="handleFilterChange">
        <el-option label="全部状态" value="" />
        <el-option label="未读" :value="0" />
        <el-option label="已读" :value="1" />
      </el-select>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <span class="loading-spinner"></span>
      <span>加载中...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="notifications.length === 0" class="empty-state">
      <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
        <path d="M13.73 21a2 2 0 0 1-3.46 0" />
      </svg>
      <p class="empty-text">暂无消息通知</p>
      <p class="empty-desc">当有新的诊断结果、润色报告或面试反馈时，会在这里通知你</p>
    </div>

    <!-- 有通知时的内容区 -->
    <template v-else>
      <!-- 批量操作栏 -->
      <div class="batch-bar">
        <el-checkbox
          :model-value="isAllSelected"
          :indeterminate="isIndeterminate"
          @change="handleToggleSelectAll"
        >
          全选
        </el-checkbox>
        <el-button
          v-if="selectedIds.length > 0"
          type="danger"
          size="small"
          :loading="deleteLoading"
          @click="handleBatchDelete"
        >
          批量删除 ({{ selectedIds.length }})
        </el-button>
      </div>

      <!-- 通知列表 -->
      <div class="notification-list">
        <div
          v-for="item in notifications"
          :key="item.id"
          class="notification-item"
          :class="{ unread: item.readStatus === 0, selected: selectedIds.includes(item.id) }"
          @click="handleItemClick(item)"
        >
          <!-- 复选框 -->
          <el-checkbox
            :model-value="selectedIds.includes(item.id)"
            @click.stop
            @change="handleToggleSelect(item.id)"
            class="item-checkbox"
          />

          <!-- 类型图标 -->
          <NotificationTypeIcon class="item-icon" :type="item.type" size="md" />

          <!-- 内容区 -->
          <div class="item-content">
            <div class="item-header">
              <span class="item-title">{{ item.title }}</span>
              <el-tag :type="getNotificationTypeMeta(item.type).tagType" size="small" effect="plain">
                {{ getNotificationTypeMeta(item.type).label }}
              </el-tag>
            </div>
            <p class="item-text">{{ item.content }}</p>
            <span class="item-time">{{ formatNotificationTime(item.createTime) }}</span>
          </div>

          <!-- 未读标记 -->
          <div v-if="item.readStatus === 0" class="unread-dot"></div>

          <!-- 删除按钮 -->
          <el-button
            class="item-delete-btn"
            type="danger"
            :icon="Delete"
            size="small"
            circle
            plain
            @click.stop="handleDelete(item.id)"
          />
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          :page-sizes="[5, 10, 20]"
          :layout="isMobileLayout ? 'prev, pager, next' : 'total, sizes, prev, pager, next'"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </template>

    <el-dialog
      v-model="announcementDialogVisible"
      class="announcement-dialog"
      :show-close="true"
      :append-to-body="true"
    >
      <template #header>
        <div class="announcement-dialog-header" v-if="selectedAnnouncement">
          <NotificationTypeIcon :type="selectedAnnouncement.type" size="sm" />
          <div class="announcement-dialog-title-block">
            <div class="announcement-dialog-title">{{ selectedAnnouncement.title }}</div>
            <div class="announcement-dialog-meta">
              <el-tag :type="getNotificationTypeMeta(selectedAnnouncement.type).tagType" size="small" effect="plain">
                {{ getNotificationTypeMeta(selectedAnnouncement.type).label }}
              </el-tag>
              <span>{{ formatNotificationTime(selectedAnnouncement.createTime) }}</span>
            </div>
          </div>
        </div>
      </template>
      <div class="announcement-dialog-content" v-if="selectedAnnouncement">
        {{ selectedAnnouncement.content }}
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Delete } from '@element-plus/icons-vue'
import { getNotifications, getUnreadCount, markAsRead, markAllAsRead, deleteNotification, batchDeleteNotifications } from '@/api/notification'
import NotificationTypeIcon from '@/components/notification/NotificationTypeIcon.vue'
import { formatNotificationTime, getNotificationTypeMeta, isAdminAnnouncementType } from '@/utils/notificationMeta'
import { getSettingsPreferences } from '@/utils/settingsPreferences'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

// 列表数据
const notifications = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(5)
const total = ref(0)
const unreadCount = ref(0)

// 筛选条件
const settingsPreferences = getSettingsPreferences()
const filterType = ref(settingsPreferences.notificationDefaultType || '')
const filterReadStatus = ref(settingsPreferences.notificationDefaultUnreadOnly ? 0 : '')

// 全部已读按钮加载状态
const markAllLoading = ref(false)

// 响应式分页布局
const isMobileLayout = ref(false)

const updateLayout = () => {
  isMobileLayout.value = window.innerWidth < 768
}

// 多选与批量删除
const selectedIds = ref([])
const deleteLoading = ref(false)
const announcementDialogVisible = ref(false)
const selectedAnnouncement = ref(null)

/** 全选状态：当前页所有项均被选中 */
const isAllSelected = computed(() =>
  notifications.value.length > 0 && notifications.value.every(item => selectedIds.value.includes(item.id))
)

/** 半选状态：当前页部分选中但非全选 */
const isIndeterminate = computed(() => {
  const count = notifications.value.filter(item => selectedIds.value.includes(item.id)).length
  return count > 0 && count < notifications.value.length
})

/**
 * 获取通知列表
 */
const fetchNotifications = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      size: pageSize.value
    }
    if (filterType.value) params.type = filterType.value
    if (filterReadStatus.value !== '' && filterReadStatus.value !== null) params.readStatus = filterReadStatus.value

    const res = await getNotifications(params)
    if (res.code === 200) {
      notifications.value = res.data.records || []
      total.value = Number(res.data.total) || 0
      unreadCount.value = Number(res.data.unreadCount) || 0
      selectedIds.value = []
    }
  } catch (e) {
    console.error('获取通知列表失败', e)
  } finally {
    loading.value = false
  }
}

/**
 * 获取未读数量
 */
const fetchUnreadCount = async () => {
  try {
    const res = await getUnreadCount()
    if (res.code === 200) {
      unreadCount.value = Number(res.data.unreadCount) || 0
    }
  } catch (e) {
    console.error('获取未读数量失败', e)
  }
}

/**
 * 筛选条件变化
 */
const handleFilterChange = () => {
  currentPage.value = 1
  selectedIds.value = []
  fetchNotifications()  // 无需 await，loading 状态已阻止重复操作
}

/**
 * 切换单条选中状态
 */
const handleToggleSelect = (id) => {
  selectedIds.value = selectedIds.value.includes(id)
    ? selectedIds.value.filter(itemId => itemId !== id)
    : [...selectedIds.value, id]
}

/**
 * 全选/取消全选当前页
 */
const handleToggleSelectAll = (checked) => {
  if (checked) {
    selectedIds.value = notifications.value.map(item => item.id)
  } else {
    selectedIds.value = []
  }
}

/**
 * 删除单条通知
 */
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除这条通知吗？', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteNotification(id)
    ElMessage.success('已删除')
    // 如果当前页删完了且不在第一页，回到上一页
    if (notifications.value.length === 1 && currentPage.value > 1) {
      currentPage.value--
    }
    await fetchNotifications()
    fetchUnreadCount()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      console.error('删除通知失败', e)
    }
  }
}

/**
 * 批量删除选中的通知
 */
const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedIds.value.length} 条通知吗？`,
      '批量删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    deleteLoading.value = true
    const deleteCount = selectedIds.value.length
    // 在清空前判断：当前页是否会被全部删完
    const willEmptyPage = notifications.value.length === deleteCount
    await batchDeleteNotifications(selectedIds.value)
    ElMessage.success(`已删除 ${deleteCount} 条通知`)
    selectedIds.value = []
    // 如果当前页删完了且不在第一页，回到上一页
    if (willEmptyPage && currentPage.value > 1) {
      currentPage.value--
    }
    await fetchNotifications()
    fetchUnreadCount()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      console.error('批量删除失败', e)
    }
  } finally {
    deleteLoading.value = false
  }
}

/**
 * 分页大小切换
 */
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchNotifications()
}

/**
 * 分页切换
 */
const handlePageChange = (page) => {
  currentPage.value = page
  fetchNotifications()
}

const updateNotificationReadState = (id, readStatus, readTime) => {
  notifications.value = notifications.value.map(item =>
    item.id === id ? { ...item, readStatus, readTime } : item
  )
}

const markNotificationReadOptimistically = (item) => {
  if (item.readStatus !== 0) return

  const readTime = new Date().toISOString()
  updateNotificationReadState(item.id, 1, readTime)
  unreadCount.value = Math.max(0, unreadCount.value - 1)

  markAsRead(item.id).catch((e) => {
    console.error('标记已读失败，回滚状态', e)
    updateNotificationReadState(item.id, 0, item.readTime || null)
    unreadCount.value += 1
  })
}

/**
 * 点击单条通知，标记已读并跳转
 */
const handleItemClick = async (item) => {
  markNotificationReadOptimistically(item)

  if (isAdminAnnouncementType(item.type) && item.broadcastId) {
    selectedAnnouncement.value = item
    announcementDialogVisible.value = true
    return
  }

  // 根据业务类型跳转
  if (item.bizType === 'resume_diagnosis' && item.bizId) {
    router.push(`/resume/result/${item.bizId}`)
  } else if (item.bizType === 'resume_polish' && item.bizId) {
    router.push(`/resume/result/${item.bizId}`)
  } else if (item.bizType === 'mock_interview' && item.bizId) {
    router.push(`/interview/report/${item.bizId}`)
  }
}

/**
 * 全部已读
 */
const handleMarkAllRead = async () => {
  markAllLoading.value = true
  try {
    await markAllAsRead()
    unreadCount.value = 0
    const readTime = new Date().toISOString()
    notifications.value = notifications.value.map(item => (
      item.readStatus === 0 ? { ...item, readStatus: 1, readTime } : item
    ))
    ElMessage.success('已全部标记为已读')
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理，此处不再重复
    console.error('全部已读操作失败', e)
  } finally {
    markAllLoading.value = false
  }
}

onMounted(() => {
  updateLayout()
  window.addEventListener('resize', updateLayout)
  fetchNotifications()
})

onUnmounted(() => {
  window.removeEventListener('resize', updateLayout)
})
</script>

<style scoped>
.notification-page {
  max-width: 800px;
  width: 100%;
  margin: 0 auto;
  padding: 24px 16px;
  box-sizing: border-box;
}

/* 返回按钮 */
.page-back {
  margin-bottom: 16px;
}

.back-btn {
  color: var(--text-muted, #909399);
}

/* 页面标题区 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-title, #1a1a1a);
  margin: 0;
}

.unread-badge {
  font-size: 13px;
  color: var(--orange-main);
  background: rgba(255, 140, 66, 0.1);
  padding: 2px 10px;
  border-radius: 12px;
  font-weight: 500;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.filter-bar .el-select {
  width: 140px;
}

/* 加载状态 */
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 60px 0;
  color: var(--text-muted, #999);
  font-size: 14px;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid var(--border-divider, #e0e0e0);
  border-top-color: var(--orange-main);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}


/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
}

.empty-icon {
  width: 64px;
  height: 64px;
  color: var(--text-placeholder, #ddd);
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  color: var(--text-muted, #999);
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 13px;
  color: var(--text-placeholder, #bbb);
  margin: 0;
  text-align: center;
}

/* 通知列表 */
.notification-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 批量操作栏 */
.batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-card, #f0f0f0);
  border-radius: 10px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-card, #f0f0f0);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.notification-item:hover {
  border-color: var(--orange-light-bg, #ffecd9);
  box-shadow: 0 2px 8px rgba(255, 140, 66, 0.08);
}

.notification-item.unread {
  background: var(--bg-page, #fffbf8);
  border-color: var(--orange-border, #ffe0c4);
}

.notification-item.selected {
  border-color: var(--orange-main);
  background: rgba(255, 140, 66, 0.04);
}

.item-checkbox {
  flex-shrink: 0;
}

/* 删除按钮：默认隐藏，hover 显示 */
.item-delete-btn {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.notification-item:hover .item-delete-btn {
  opacity: 1;
}

/* 类型图标 */
.item-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.item-icon svg {
  width: 20px;
  height: 20px;
}

.item-icon.type-resume {
  background: rgba(255, 140, 66, 0.1);
  color: var(--orange-main);
}

.item-icon.type-polish {
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
}

.item-icon.type-interview {
  background: rgba(103, 194, 58, 0.1);
  color: var(--color-success);
}

.item-icon.type-quota {
  background: rgba(245, 108, 108, 0.1);
  color: var(--color-danger);
}

.item-icon.type-system {
  background: rgba(144, 147, 153, 0.1);
  color: var(--text-muted);
}

.item-icon.type-activity {
  background: rgba(230, 162, 60, 0.12);
  color: #e6a23c;
}

.item-icon.type-update {
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
}

.item-icon.type-maintenance {
  background: rgba(245, 108, 108, 0.1);
  color: var(--color-danger);
}

/* 内容区 */
.item-content {
  flex: 1;
  min-width: 0;
}

.item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.item-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-title, #1a1a1a);
}

.item-text {
  font-size: 13px;
  color: var(--text-body, #666);
  margin: 0 0 6px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-time {
  font-size: 12px;
  color: var(--text-muted, #aaa);
}

/* 未读点 */
.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--orange-main);
  flex-shrink: 0;
  margin-top: 6px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* 响应式 */
@media (max-width: 767px) {
  .notification-page {
    padding: 12px;
  }

  .page-title {
    font-size: 18px;
  }

  .page-header {
    flex-wrap: wrap;
    gap: 12px;
  }

  .header-left {
    flex-wrap: wrap;
    gap: 8px;
  }

  .filter-bar {
    flex-direction: column;
  }

  .filter-bar .el-select {
    width: 100%;
  }

  .notification-item {
    padding: 12px;
    overflow: hidden;
  }

  .item-header {
    flex-wrap: wrap;
    gap: 4px;
    min-width: 0;
  }

  .item-title {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
    min-width: 0;
  }

  .item-delete-btn {
    opacity: 1;
  }

  .batch-bar {
    flex-wrap: wrap;
    gap: 8px;
  }

  .pagination-wrapper {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    padding-bottom: 4px;
  }

}
</style>

<style>
/* 公告弹窗 — 全局样式，因 append-to-body 需处理 teleport 的元素 */
.announcement-dialog {
  --el-dialog-width: 560px;
}

.announcement-dialog .el-dialog {
  border-radius: 12px;
  background: var(--bg-card);
}

.announcement-dialog .el-dialog__header {
  padding: 22px 24px 14px;
  margin-right: 38px;
}

.announcement-dialog .el-dialog__body {
  padding: 0 24px 24px;
}

.announcement-dialog-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.announcement-dialog-title-block {
  min-width: 0;
}

.announcement-dialog-title {
  color: var(--text-title);
  font-size: 18px;
  line-height: 1.4;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.announcement-dialog-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
  color: var(--text-muted);
  font-size: 12px;
}

.announcement-dialog-content {
  max-height: 52vh;
  overflow-y: auto;
  padding: 18px;
  border: 1px solid var(--border-card);
  border-radius: 8px;
  background: var(--bg-page);
  color: var(--text-body);
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

@media (max-width: 767px) {
  .announcement-dialog {
    --el-dialog-width: calc(100vw - 32px);
  }

  .announcement-dialog-header {
    flex-direction: column;
    gap: 8px;
  }

  .announcement-dialog .el-dialog__header {
    padding: 16px 16px 10px;
    margin-right: 32px;
  }

  .announcement-dialog .el-dialog__body {
    padding: 0 16px 16px;
  }

  .announcement-dialog-title {
    font-size: 16px;
  }

  .announcement-dialog-content {
    padding: 12px;
    font-size: 13px;
    max-height: 45vh;
  }
}

@media (max-width: 480px) {
  .announcement-dialog {
    --el-dialog-width: 100vw;
  }

  .announcement-dialog .el-dialog {
    border-radius: 0;
  }

  .announcement-dialog .el-dialog__header {
    padding: 14px 14px 10px;
    margin-right: 24px;
  }

  .announcement-dialog-title {
    font-size: 15px;
  }

  .announcement-dialog-content {
    padding: 10px;
    font-size: 12px;
    max-height: 40vh;
  }
}
</style>
