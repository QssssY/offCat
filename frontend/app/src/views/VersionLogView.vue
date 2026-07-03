<template>
  <main class="version-page">
    <section class="version-hero" aria-labelledby="version-page-title">
      <FeatureIcon name="version-log" size="lg" class="version-hero-icon" />
      <div class="hero-copy">
        <p class="eyebrow">公开更新</p>
        <h1 id="version-page-title">版本日志</h1>
        <p class="sub">查看最近发布的产品更新、修复与体验调整。</p>
      </div>

      <router-link to="/" class="home-link">
        <n-button secondary round class="home-button">
          <FeatureIcon name="back" size="xs" class="home-button-icon" />
          返回首页
        </n-button>
      </router-link>
    </section>

    <section v-if="loading" class="loading-panel" aria-label="版本日志加载中">
      <article v-for="item in 3" :key="item" class="skeleton-item">
        <n-skeleton text width="112px" />
        <n-skeleton text width="48%" />
        <n-skeleton text :repeat="2" />
      </article>
    </section>

    <section v-else-if="errorMessage" class="state-panel" aria-live="polite">
      <n-result status="error" title="版本日志加载失败" :description="errorMessage">
        <template #footer>
          <n-button type="primary" :loading="loading" @click="loadLogs">重新加载</n-button>
        </template>
      </n-result>
    </section>

    <section v-else-if="logs.length === 0" class="state-panel">
      <n-empty description="暂无版本日志">
        <template #icon>
          <FeatureIcon name="empty-state" size="lg" />
        </template>
        <template #extra>
          <p class="empty-copy">发布后的产品更新会展示在这里。</p>
        </template>
      </n-empty>
    </section>

    <section v-else class="version-feed" aria-label="版本更新列表">
      <article v-for="(log, index) in logs" :key="log.id" class="version-item">
        <div class="date-column" aria-hidden="true">
          <time class="version-date">{{ formatDate(log.publishedAt) }}</time>
          <span class="version-index">{{ formatIndex(index) }}</span>
        </div>

        <div class="version-card">
          <div class="version-card-header">
            <div class="version-meta">
              <n-tag
                size="small"
                round
                :bordered="false"
                :type="getVersionTagType(log.type)"
                class="version-tag"
              >
                v{{ log.version }}
              </n-tag>
              <span class="type-label">{{ getVersionTypeLabel(log.type) }}</span>
              <FeatureIcon name="announcement" size="xs" class="version-type-icon" />
            </div>
          </div>

          <h2 class="version-title" :title="log.title">{{ log.title }}</h2>
          <div
            class="version-content"
            :class="{ 'is-collapsed': isContentExpandable(log) && !isExpanded(log.id) }"
          >
            {{ log.content }}
          </div>
          <button
            v-if="isContentExpandable(log)"
            type="button"
            class="expand-button"
            @click="toggleExpanded(log.id)"
          >
            {{ isExpanded(log.id) ? '收起内容' : '展开全文' }}
          </button>
        </div>
      </article>
    </section>

    <div v-if="total > 0 && !loading && !errorMessage" class="pagination-wrap">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="pageSizeOptions"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElPagination } from 'element-plus'
import { NButton, NEmpty, NResult, NSkeleton, NTag } from 'naive-ui'
import { getPublicVersionLogsPage } from '@/api/publicVersionLog'
import FeatureIcon from '@/components/common/FeatureIcon.vue'

const logs = ref([])
const currentPage = ref(1)
const pageSize = ref(5)
const total = ref(0)
const loading = ref(false)
const errorMessage = ref('')
const expandedLogIds = ref(new Set())
const pageSizeOptions = [5, 10, 20]

const versionTypeMeta = {
  major: { tagType: 'error', label: '重要更新' },
  minor: { tagType: 'warning', label: '功能优化' },
  patch: { tagType: 'info', label: '修复调整' }
}

const getVersionTagType = (type) => versionTypeMeta[type]?.tagType || 'success'
const getVersionTypeLabel = (type) => versionTypeMeta[type]?.label || '产品更新'
const formatIndex = (index) => String(index + 1).padStart(2, '0')

const isContentExpandable = (log) => {
  const content = log?.content || ''
  return content.length > 160 || content.split(/\r?\n/).length > 4
}

const isExpanded = (id) => expandedLogIds.value.has(id)

const toggleExpanded = (id) => {
  const nextIds = new Set(expandedLogIds.value)
  if (nextIds.has(id)) {
    nextIds.delete(id)
  } else {
    nextIds.add(id)
  }
  expandedLogIds.value = nextIds
}

const formatDate = (value) => {
  if (!value) return '未发布'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const loadLogs = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    // 公开版本日志只读取已发布分页数据，不在前端派生额外筛选条件，避免改变后端契约。
    const res = await getPublicVersionLogsPage({ page: currentPage.value, size: pageSize.value })
    const data = res?.data || {}
    logs.value = data.records || []
    total.value = Number(data.total || 0)
    expandedLogIds.value = new Set()
  } catch (e) {
    logs.value = []
    total.value = 0
    errorMessage.value = e?.message || '请稍后重试。'
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadLogs()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadLogs()
}

onMounted(loadLogs)
</script>

<style scoped>
.version-page {
  --version-radius: 8px;
  --version-space-xs: 4px;
  --version-space-sm: 8px;
  --version-space-md: 12px;
  --version-space-lg: 16px;
  --version-space-xl: 24px;
  --version-space-2xl: 32px;
  --version-space-3xl: 48px;

  width: min(980px, calc(100% - 48px));
  margin: 0 auto;
  padding: 48px 0 80px;
}

.version-hero {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: var(--version-space-2xl);
  align-items: end;
  margin-bottom: var(--version-space-3xl);
}

.version-hero-icon {
  align-self: center;
}

.hero-copy {
  display: grid;
  gap: var(--version-space-md);
  max-width: 680px;
}

.eyebrow {
  margin: 0;
  color: var(--orange-main);
  font-size: 0.875rem;
  font-weight: 700;
  line-height: 1.4;
}

h1 {
  margin: 0;
  color: var(--text-title);
  font-size: clamp(2.375rem, 4.2vw, 3.5rem);
  line-height: 1.08;
  font-weight: 800;
  text-wrap: balance;
}

.sub {
  max-width: 48ch;
  margin: 0;
  color: var(--text-title);
  font-size: 1.0625rem;
  line-height: 1.7;
  opacity: 0.88;
  text-wrap: pretty;
}

.home-link {
  display: inline-flex;
  width: fit-content;
}

.home-button {
  --n-color: var(--bg-card) !important;
  --n-color-hover: var(--orange-light-bg) !important;
  --n-color-pressed: var(--orange-light-bg) !important;
  --n-text-color: var(--orange-deep) !important;
  --n-text-color-hover: var(--orange-deep) !important;
  --n-text-color-pressed: var(--orange-deep) !important;
  --n-border: 1px solid var(--orange-border) !important;
  --n-border-hover: 1px solid var(--orange-main) !important;
  --n-border-pressed: 1px solid var(--orange-main) !important;
  min-width: 104px;
  font-weight: 700;
}

.home-button-icon,
.version-type-icon {
  margin-right: 4px;
}

.loading-panel,
.version-feed {
  display: grid;
  gap: var(--version-space-md);
}

.skeleton-item {
  min-width: 0;
}

.version-item {
  display: grid;
  grid-template-columns: 116px minmax(0, 1fr);
  gap: var(--version-space-xl);
  min-width: 0;
}

.skeleton-item {
  padding: var(--version-space-xl);
  border: 1px solid var(--border-card);
  border-radius: var(--version-radius);
  background: var(--bg-card);
}

.date-column {
  display: grid;
  align-content: start;
  gap: var(--version-space-sm);
  padding-top: var(--version-space-lg);
}

.version-date {
  color: var(--text-body);
  font-size: 0.875rem;
  line-height: 1.4;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.version-index {
  color: color-mix(in srgb, var(--orange-main) 56%, var(--text-title));
  font-size: 2.75rem;
  line-height: 1;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.version-card {
  display: grid;
  gap: var(--version-space-md);
  padding: var(--version-space-xl);
  border: 1px solid var(--border-card);
  border-radius: var(--version-radius);
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
  min-width: 0;
}

.version-card-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--version-space-sm);
}

.version-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--version-space-sm);
}

.version-tag {
  font-weight: 700;
}

.type-label {
  color: var(--text-body);
  font-size: 0.8125rem;
  line-height: 1.4;
  font-weight: 600;
}

.version-title {
  margin: 0;
  color: var(--text-title);
  font-size: 1.25rem;
  line-height: 1.3;
  font-weight: 800;
  overflow-wrap: anywhere;
  white-space: normal;
  text-wrap: pretty;
}

.version-content {
  max-width: 72ch;
  margin: 0;
  color: var(--text-title);
  font-size: 1rem;
  line-height: 1.75;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  text-wrap: pretty;
  opacity: 0.92;
}

.version-content.is-collapsed {
  display: -webkit-box;
  max-height: 7em;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
}

.expand-button {
  width: fit-content;
  min-height: 32px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--orange-deep);
  cursor: pointer;
  font: inherit;
  font-size: 0.9375rem;
  font-weight: 700;
}

.expand-button:hover,
.expand-button:focus-visible {
  color: var(--orange-main);
  text-decoration: underline;
}

.expand-button:focus-visible {
  outline: 2px solid var(--orange-border);
  outline-offset: 3px;
  border-radius: 4px;
}

.state-panel {
  display: grid;
  place-items: center;
  min-height: 320px;
  padding: var(--version-space-3xl) var(--version-space-xl);
  border: 1px solid var(--border-card);
  border-radius: var(--version-radius);
  background: var(--bg-card);
}

.empty-copy {
  margin: var(--version-space-sm) 0 0;
  color: var(--text-muted);
  font-size: 0.9375rem;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--version-space-2xl);
  padding-top: var(--version-space-xl);
}

.pagination-wrap :deep(.el-pagination) {
  flex-wrap: wrap;
  justify-content: flex-end;
  row-gap: var(--version-space-sm);
}

@media (max-width: 767px) {
  .version-page {
    width: min(100% - 28px, 980px);
    padding: 32px 0 56px;
  }

  .version-hero {
    grid-template-columns: 1fr;
    gap: var(--version-space-lg);
    margin-bottom: var(--version-space-2xl);
  }

  h1 {
    font-size: 2.5rem;
  }

  .sub {
    font-size: 1rem;
  }

  .version-item {
    grid-template-columns: 1fr;
    gap: var(--version-space-sm);
  }

  .date-column {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    padding-top: 0;
  }

  .version-index {
    font-size: 1.875rem;
  }

  .version-card {
    padding: var(--version-space-lg);
  }

  .version-card-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .version-meta {
    width: 100%;
  }

  .version-title {
    font-size: 1.125rem;
  }

  .pagination-wrap {
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: var(--version-space-xs);
  }
}
</style>
