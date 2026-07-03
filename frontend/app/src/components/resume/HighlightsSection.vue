<template>
  <div class="highlights-section">
    <div v-if="highlights.length > 0" class="highlights-list">
      <div
        v-for="(item, index) in highlights"
        :key="index"
        class="highlight-item"
      >
        <div class="highlight-icon">
          <FeatureIcon name="success" size="xs" />
        </div>
        <div class="highlight-content">
          <div v-if="item.title" class="highlight-title">{{ item.title }}</div>
          <div v-if="item.description" class="highlight-desc">{{ item.description }}</div>
          <div v-else-if="typeof item === 'string'" class="highlight-desc">{{ item }}</div>
        </div>
      </div>
    </div>
    <div v-else class="empty-highlights">
      <FeatureIcon name="empty-state" size="md" />
      <span>暂无亮点记录</span>
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

// 处理多种可能的数据结构
const highlights = computed(() => {
  const data = props.data
  if (!data) return []

  // 如果是数组，直接使用
  if (Array.isArray(data)) {
    return data.map(item => {
      if (typeof item === 'string') {
        return { title: '', description: item }
      }
      return item
    })
  }

  // 如果是对象，尝试提取亮点字段
  if (typeof data === 'object') {
    // 可能的字段名
    const possibleKeys = ['highlights', 'strengths', 'advantages', 'points', 'items']
    for (const key of possibleKeys) {
      if (Array.isArray(data[key])) {
        return data[key].map(item => {
          if (typeof item === 'string') {
            return { title: '', description: item }
          }
          return item
        })
      }
    }
  }

  return []
})
</script>

<style scoped>
.highlights-section {
  padding: 4px 0;
}

.highlights-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.highlight-item {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  background: #faf9f5;
  border-radius: 8px;
  border-left: 3px solid #cc785c;
}

.highlight-icon {
  flex-shrink: 0;
  padding-top: 2px;
}

.highlight-icon svg,
.highlight-icon .el-icon {
  color: #cc785c !important;
}

.highlight-content {
  flex: 1;
}

.highlight-title {
  font-size: 14px;
  font-weight: 500;
  color: #141413;
  margin-bottom: 4px;
}

.highlight-desc {
  font-size: 13px;
  color: #3d3d3a;
  line-height: 1.6;
}

.empty-highlights {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 0;
  color: #6c6a64;
  font-size: 14px;
}

[data-theme="dark"] .highlight-item {
  background: rgba(255,255,255,0.04);
  border-left-color: #d08a6a;
}

[data-theme="dark"] .highlight-icon svg,
[data-theme="dark"] .highlight-icon .el-icon {
  color: #d08a6a !important;
}

[data-theme="dark"] .highlight-title {
  color: var(--text-title);
}

[data-theme="dark"] .highlight-desc {
  color: var(--text-body);
}

[data-theme="dark"] .empty-highlights {
  color: var(--text-muted);
}
</style>
