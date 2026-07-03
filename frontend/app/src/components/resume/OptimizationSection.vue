<template>
  <div class="optimization-section">
    <!-- 优先级建议 -->
    <div v-if="prioritySuggestions.length > 0" class="suggestions-group priority">
      <div class="group-header">
        <FeatureIcon name="warning" size="xs" />
        <span>优先改进</span>
      </div>
      <div class="suggestions-list">
        <div
          v-for="(item, index) in prioritySuggestions"
          :key="index"
          class="suggestion-card priority"
        >
          <div class="card-header">
            <span class="card-number">{{ index + 1 }}</span>
            <span class="card-title">{{ item.title || '改进建议' }}</span>
          </div>
          <div v-if="item.description" class="card-body">
            {{ item.description }}
          </div>
          <div v-if="item.actionItems && item.actionItems.length" class="action-items">
            <div
              v-for="(action, idx) in item.actionItems"
              :key="idx"
              class="action-item"
            >
              <FeatureIcon name="next" size="xs" />
              <span>{{ action }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 一般建议 -->
    <div v-if="generalSuggestions.length > 0" class="suggestions-group general">
      <div class="group-header">
        <FeatureIcon name="resume-optimization" size="xs" />
        <span>优化建议</span>
      </div>
      <div class="suggestions-list">
        <div
          v-for="(item, index) in generalSuggestions"
          :key="index"
          class="suggestion-card general"
        >
          <div class="card-header">
            <span class="card-number">{{ index + 1 }}</span>
            <span class="card-title">{{ item.title || '优化建议' }}</span>
          </div>
          <div v-if="item.description" class="card-body">
            {{ item.description }}
          </div>
        </div>
      </div>
    </div>

    <!-- 可执行操作 -->
    <div v-if="actionableItems.length > 0" class="actionable-section">
      <div class="section-title">
        <FeatureIcon name="success" size="xs" />
        <span>可执行修改</span>
      </div>
      <div class="actionable-list">
        <div
          v-for="(item, index) in actionableItems"
          :key="index"
          class="actionable-item"
        >
          <el-checkbox :model-value="false" disabled>
            <span class="item-text">{{ item }}</span>
          </el-checkbox>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!hasContent" class="empty-optimization">
      <FeatureIcon name="empty-state" size="sm" />
      <span>暂无优化建议</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import FeatureIcon from '@/components/common/FeatureIcon.vue'

const props = defineProps({
  data: {
    type: [Array, Object],
    default: () => []
  }
})

// 处理数据为数组格式
const suggestionsData = computed(() => {
  const data = props.data
  if (!data) return []

  if (Array.isArray(data)) {
    return data.map(item => {
      if (typeof item === 'string') {
        return { title: item, description: '', priority: 'general' }
      }
      return item
    })
  }

  if (typeof data === 'object') {
    // 可能的字段名
    const possibleKeys = ['suggestions', 'recommendations', 'improvements', 'items']
    for (const key of possibleKeys) {
      if (Array.isArray(data[key])) {
        return data[key].map(item => {
          if (typeof item === 'string') {
            return { title: item, description: '', priority: 'general' }
          }
          return item
        })
      }
    }

    // 如果没有找到数组，将整个对象作为单个建议
    return [{
      title: data.title || '优化建议',
      description: data.description || data.content || '',
      priority: data.priority || 'general'
    }]
  }

  return []
})

// 优先建议
const prioritySuggestions = computed(() => {
  return suggestionsData.value.filter(item =>
    item.priority === 'high' ||
    item.priority === 'important' ||
    item.important === true
  )
})

// 一般建议
const generalSuggestions = computed(() => {
  return suggestionsData.value.filter(item =>
    item.priority !== 'high' &&
    item.priority !== 'important' &&
    item.important !== true
  )
})

// 可执行操作
const actionableItems = computed(() => {
  const items = []
  suggestionsData.value.forEach(suggestion => {
    if (suggestion.actionItems && Array.isArray(suggestion.actionItems)) {
      items.push(...suggestion.actionItems)
    }
    if (suggestion.actions && Array.isArray(suggestion.actions)) {
      items.push(...suggestion.actions)
    }
  })
  return items
})

// 是否有内容
const hasContent = computed(() => {
  return suggestionsData.value.length > 0 || actionableItems.value.length > 0
})
</script>

<style scoped>
.optimization-section {
  padding: 8px 0;
}

.suggestions-group {
  margin-bottom: 24px;
}

.suggestions-group:last-child {
  margin-bottom: 0;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-divider, #f0f0f0);
  font-size: 15px;
  font-weight: 500;
  color: var(--text-title, #303133);
}

.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.suggestion-card {
  background-color: var(--bg-elevated, #f5f7fa);
  border-radius: 4px;
  padding: 16px;
  border-left: 3px solid var(--text-muted, #909399);
}

.suggestion-card.priority {
  background-color: #fef0f0;
  border-left-color: var(--color-danger, #f56c6c);
}

.suggestion-card.general {
  background-color: #ecf5ff;
  border-left-color: var(--color-info, #409eff);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.card-number {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-body, #606266);
}

.suggestion-card.priority .card-number {
  background-color: rgba(245, 108, 108, 0.2);
  color: var(--color-danger, #f56c6c);
}

.suggestion-card.general .card-number {
  background-color: rgba(64, 158, 255, 0.2);
  color: var(--color-info, #409eff);
}

.card-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-title, #303133);
}

.card-body {
  font-size: 13px;
  color: var(--text-body, #606266);
  line-height: 1.6;
  margin-bottom: 8px;
}

.action-items {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--border-divider, #dcdfe6);
}

.action-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-body, #606266);
}

.actionable-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--border-divider, #f0f0f0);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-title, #303133);
  margin-bottom: 12px;
}

.actionable-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.actionable-item {
  padding: 10px 12px;
  background-color: #f0f9eb;
  border-radius: 4px;
  border-left: 3px solid var(--color-success, #67c23a);
}

.item-text {
  font-size: 13px;
  color: var(--text-body, #606266);
}

.empty-optimization {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 0;
  color: var(--text-muted, #909399);
  font-size: 14px;
}
</style>
