<template>
  <div class="consumption-log-panel">
    <!-- 类型筛选栏 -->
    <div class="filter-bar">
      <button
        v-for="opt in typeOptions"
        :key="opt.value"
        class="filter-btn"
        :class="{ active: activeType === opt.value }"
        @click="activeType = opt.value"
      >
        {{ opt.label }}
      </button>
    </div>

    <!-- 加载状态（首次加载） -->
    <div v-if="loading && records.length === 0" class="log-loading">
      <div class="loading-spinner"></div>
      <span>加载消费记录...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="records.length === 0 && !loading" class="log-empty">
      <FeatureIcon name="empty-state" size="sm" />
      <span>暂无消费记录</span>
    </div>

    <!-- 记录列表 -->
    <div v-if="records.length > 0" class="log-list">
      <div v-for="item in records" :key="item.id" class="log-item">
        <!-- 左侧：类型图标+名称 -->
        <div class="log-left">
          <el-icon class="log-type-icon" :style="{ color: typeColorMap[item.quotaType] || '#999' }" :size="28">
            <component :is="typeIconMap[item.quotaType] || Document" />
          </el-icon>
          <div class="log-detail">
            <div class="log-type-name">{{ item.quotaTypeName }}</div>
            <div class="log-meta">
              <span class="log-source">{{ item.sourceName }}</span>
              <span v-if="item.description" class="log-desc">· {{ item.description }}</span>
            </div>
          </div>
        </div>
        <!-- 右侧：变动数量+时间 -->
        <div class="log-right">
          <span class="log-amount" :class="item.changeAmount < 0 ? 'refund' : 'consume'">
            {{ item.changeAmount > 0 ? '-' + item.changeAmount : '+' + Math.abs(item.changeAmount) }}
          </span>
          <span class="log-time">{{ formatTime(item.createTime) }}</span>
        </div>
      </div>
    </div>

    <!-- 加载更多 / 到底提示 -->
    <div v-if="records.length > 0" class="log-footer">
      <span class="log-total">已加载 {{ records.length }} / {{ total }} 条</span>
      <button
        v-if="hasMore"
        class="load-more-btn"
        :disabled="loadingMore"
        @click="loadMore"
      >
        <div v-if="loadingMore" class="loading-spinner small"></div>
        {{ loadingMore ? '加载中...' : '加载更多' }}
      </button>
      <span v-else-if="records.length >= total" class="log-end">没有更多了</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, markRaw } from 'vue'
import { getConsumptionLog } from '@/api/quota'
import FeatureIcon from '@/components/common/FeatureIcon.vue'
import { Headset, Document, EditPen, Position, Files, Suitcase } from '@element-plus/icons-vue'

defineOptions({ name: 'ConsumptionLogPanel' })

/** 额度类型 → Element Plus 图标组件映射 */
const typeIconMap = {
  INTERVIEW: markRaw(Headset),
  RESUME: markRaw(Document),
  POLISH: markRaw(EditPen),
  JD_MATCH: markRaw(Position),
  TEMPLATE: markRaw(Files),
  OFFER: markRaw(Suitcase),
}

/** 额度类型 → 图标颜色映射（无底色，仅图标着色） */
const typeColorMap = {
  INTERVIEW: '#FF8C42',
  RESUME: '#E67A35',
  POLISH: '#5B8DEF',
  JD_MATCH: '#3ABAB4',
  TEMPLATE: '#7B68EE',
  OFFER: '#E667AF',
}

/** 额度类型筛选选项 */
const typeOptions = [
  { label: '全部', value: '' },
  { label: '模拟面试', value: 'INTERVIEW' },
  { label: '简历诊断', value: 'RESUME' },
  { label: 'AI润色', value: 'POLISH' },
  { label: 'JD匹配', value: 'JD_MATCH' },
  { label: '模板库', value: 'TEMPLATE' },
  { label: 'Offer', value: 'OFFER' },
]

/** 当前选中类型 */
const activeType = ref('')
/** 当前页码 */
const currentPage = ref(1)
/** 每页条数 */
const pageSize = 20
/** 总记录数 */
const total = ref(0)
/** 消费记录列表（追加模式） */
const records = ref([])
/** 首次加载状态 */
const loading = ref(false)
/** 加载更多状态 */
const loadingMore = ref(false)
/** 是否还有更多数据 */
const hasMore = computed(() => records.value.length < total.value)
/** 列表请求版本号：筛选切换会让旧响应失效，避免异步结果串入新列表 */
let listRequestVersion = 0
/** 追加请求版本号：筛选重载或再次追加会让旧追加响应失效 */
let loadMoreRequestVersion = 0

/** 首次获取消费记录 */
const fetchLogs = async () => {
  const requestVersion = ++listRequestVersion
  const requestType = activeType.value
  loadMoreRequestVersion++
  loading.value = true
  loadingMore.value = false
  try {
    const params = {
      pageNum: 1,
      pageSize,
    }
    if (requestType) {
      params.quotaType = requestType
    }
    const res = await getConsumptionLog(params)
    if (requestVersion !== listRequestVersion || requestType !== activeType.value) {
      return
    }
    const data = res.data
    records.value = data?.list || []
    total.value = data?.total || 0
    currentPage.value = 1
  } catch {
    if (requestVersion !== listRequestVersion || requestType !== activeType.value) {
      return
    }
    records.value = []
    total.value = 0
  } finally {
    if (requestVersion === listRequestVersion && requestType === activeType.value) {
      loading.value = false
    }
  }
}

/** 加载更多（追加到现有列表） */
const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return
  const requestVersion = listRequestVersion
  const requestType = activeType.value
  const loadRequestVersion = ++loadMoreRequestVersion
  loadingMore.value = true
  try {
    const nextPage = currentPage.value + 1
    const params = {
      pageNum: nextPage,
      pageSize,
    }
    if (requestType) {
      params.quotaType = requestType
    }
    const res = await getConsumptionLog(params)
    if (
      requestVersion !== listRequestVersion ||
      loadRequestVersion !== loadMoreRequestVersion ||
      requestType !== activeType.value
    ) {
      return
    }
    const data = res.data
    const newRecords = data?.list || []
    records.value = [...records.value, ...newRecords]
    total.value = data?.total || 0
    currentPage.value = nextPage
  } catch {
    // 追加失败时保持现有数据不变
  } finally {
    if (loadRequestVersion === loadMoreRequestVersion) {
      loadingMore.value = false
    }
  }
}

/** 类型切换时重置并重新加载 */
watch(activeType, () => {
  fetchLogs()
})

/** 时间格式化 */
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

onMounted(() => {
  fetchLogs()
})
</script>

<style scoped>
.consumption-log-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-btn {
  padding: 6px 14px;
  border: 1px solid var(--border-card);
  border-radius: 20px;
  background: var(--bg-card);
  font-size: 13px;
  color: var(--text-body);
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
}

.filter-btn:hover {
  border-color: var(--orange-main);
  color: var(--orange-main);
}

.filter-btn.active {
  background: var(--orange-main);
  border-color: var(--orange-main);
  color: #fff;
}

/* 加载状态 */
.log-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
  gap: 12px;
  color: var(--text-muted);
  font-size: 13px;
}

.loading-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid var(--border-divider);
  border-top-color: var(--orange-main);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 空状态 */
.log-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
  gap: 10px;
  color: var(--text-placeholder);
  font-size: 13px;
}

/* 记录列表 */
.log-list {
  display: flex;
  flex-direction: column;
}

.log-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid var(--bg-page);
  transition: background-color 0.12s ease;
}

.log-item:last-child {
  border-bottom: none;
}

.log-item:hover {
  background: var(--bg-card-hover);
}

/* 左侧 */
.log-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

/* 类型图标 — 无底色无边框，纯彩色图标 */
.log-type-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.log-detail {
  min-width: 0;
}

.log-type-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-title);
}

.log-meta {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-source {
  color: var(--text-muted);
}

.log-desc {
  color: var(--text-placeholder);
}

/* 右侧 */
.log-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  flex-shrink: 0;
  margin-left: 12px;
}

.log-amount {
  font-size: 15px;
  font-weight: 600;
}

.log-amount.consume {
  color: var(--color-danger);
}

.log-amount.refund {
  color: var(--color-success);
}

.log-time {
  font-size: 12px;
  color: var(--text-placeholder);
}

/* 底部：加载更多 / 到底提示 */
.log-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding-top: 12px;
}

.log-total {
  font-size: 12px;
  color: var(--text-muted);
}

.load-more-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 24px;
  border: 1px solid var(--border-card);
  border-radius: 20px;
  background: var(--bg-card);
  font-size: 13px;
  color: var(--text-body);
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
}

.load-more-btn:hover:not(:disabled) {
  border-color: var(--orange-main);
  color: var(--orange-main);
}

.load-more-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-spinner.small {
  width: 14px;
  height: 14px;
  border-width: 2px;
}

.log-end {
  font-size: 12px;
  color: var(--text-placeholder);
}

/* 响应式 */
@media (max-width: 767px) {
  .filter-bar {
    gap: 6px;
  }
  .filter-btn {
    padding: 5px 10px;
    font-size: 12px;
  }
  .log-item {
    padding: 12px 10px;
  }
  .log-type-icon {
    font-size: 22px !important;
  }
  .log-type-name {
    font-size: 13px;
  }
  .log-amount {
    font-size: 14px;
  }
}
</style>
