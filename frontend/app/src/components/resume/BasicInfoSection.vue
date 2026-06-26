<template>
  <div class="basic-info-section">
    <!-- 完整度得分 -->
    <div class="completeness-score">
      <div class="completeness-display">
        <div class="completeness-bar">
          <div
            class="completeness-fill"
            :style="{ width: completenessPercentage + '%' }"
            :class="completenessClass"
          />
        </div>
        <div class="completeness-value">{{ completenessScore }}%</div>
      </div>
      <div class="completeness-label">基础信息完整度</div>
    </div>

    <!-- 信息项列表 -->
    <div class="info-items">
      <div
        v-for="(item, index) in basicInfoItems"
        :key="index"
        class="info-item"
        :class="{ 'is-missing': !item.hasValue }"
      >
        <div class="item-icon">
          <el-icon :size="16" :color="item.hasValue ? '#409eff' : '#c0c4cc'">
            <component :is="item.icon" />
          </el-icon>
        </div>
        <div class="item-content">
          <div class="item-label">
            {{ item.label }}
            <span v-if="item.isOptional" class="optional-tag">(选填)</span>
          </div>
          <div class="item-value" :class="{ 'is-empty': !item.hasValue }">
            {{ item.displayValue }}
          </div>
        </div>
        <div class="item-status">
          <FeatureIcon v-if="item.hasValue" name="success" size="xs" />
          <FeatureIcon v-else name="error" size="xs" />
        </div>
      </div>
    </div>

    <!-- 评价 -->
    <div v-if="comment" class="basic-info-comment">
      <div class="comment-label">完整性评价</div>
      <div class="comment-content">{{ comment }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  User,
  Message,
  Phone,
  OfficeBuilding,
  Link,
  Location
} from '@element-plus/icons-vue'
import FeatureIcon from '@/components/common/FeatureIcon.vue'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({})
  }
})

/**
 * 【关键修复】
 * 从 props.data 中直接读取 basicInfoEvaluation 和 basicInfoDetails
 * 注意：props.data 现在是完整的 parsedResult 对象，不是只有 basicInfoEvaluation
 */

// 基础信息评价（用于完整度得分）
const basicInfoEvaluation = computed(() => {
  return props.data?.basicInfoEvaluation || {}
})

// 基础信息详情（用于展示具体字段值）
const basicInfoDetails = computed(() => {
  return props.data?.basicInfoDetails || {}
})

// 信息项配置（统一结构，直接从 basicInfoDetails 读取）
const infoConfig = [
  {
    key: 'name',
    label: '姓名',
    icon: User,
    isOptional: false
  },
  {
    key: 'email',
    label: '邮箱',
    icon: Message,
    isOptional: false
  },
  {
    key: 'phone',
    label: '电话',
    icon: Phone,
    isOptional: false
  },
  {
    key: 'location',
    label: '所在地',
    icon: Location,
    isOptional: false
  },
  {
    key: 'currentCompany',
    label: '当前公司',
    icon: OfficeBuilding,
    isOptional: true
  },
  {
    key: 'github',
    label: 'GitHub',
    icon: Link,
    isOptional: true
  },
  {
    key: 'blog',
    label: '博客/网站',
    icon: Link,
    isOptional: true
  }
]

/**
 * 【关键修复】
 * 统一构建基础信息项列表
 * 优先从 basicInfoDetails 读取具体值
 * 如果 basicInfoDetails 没有值，兼容旧数据从 basicInfoEvaluation 的 hasName 等判断
 */
const basicInfoItems = computed(() => {
  return infoConfig.map(config => {
    // 优先从 basicInfoDetails 读取具体值
    const detailValue = basicInfoDetails.value?.[config.key]
    const hasDetailValue = detailValue !== undefined && detailValue !== null && String(detailValue).trim() !== ''

    let hasValue = hasDetailValue
    let displayValue = '未填写'

    if (hasDetailValue) {
      // 有具体值，直接显示
      displayValue = String(detailValue).trim()
    } else {
      // 【兼容旧数据】如果 basicInfoDetails 没有值，检查 basicInfoEvaluation 的布尔字段
      const hasFieldKey = `has${config.key.charAt(0).toUpperCase()}${config.key.slice(1)}`
      const hasFieldValue = basicInfoEvaluation.value?.[hasFieldKey]
      if (hasFieldValue === true) {
        // 旧数据只有布尔标记，显示"已识别"
        hasValue = true
        displayValue = '已识别'
      }
    }

    return {
      ...config,
      hasValue,
      displayValue
    }
  })
})

/**
 * 【关键修复】
 * 完整度得分读取后端返回的真实值 basicInfoEvaluation.score
 * 不再自己计算，避免显示 0%
 */
const completenessScore = computed(() => {
  const score = basicInfoEvaluation.value?.score
  if (typeof score === 'number') {
    return score
  }
  // 兜底：如果后端没有返回 score，基于必填字段是否有值粗略计算
  const requiredFields = ['name', 'email', 'phone', 'location']
  let filledCount = 0
  for (const key of requiredFields) {
    const item = basicInfoItems.value.find(i => i.key === key)
    if (item?.hasValue) {
      filledCount++
    }
  }
  return Math.round((filledCount / requiredFields.length) * 100)
})

const hasCompletenessScore = computed(() => true)

const completenessPercentage = computed(() => {
  return Math.min(Math.max(completenessScore.value || 0, 0), 100)
})

const completenessClass = computed(() => {
  const p = completenessPercentage.value
  if (p >= 80) return 'completeness-high'
  if (p >= 60) return 'completeness-medium'
  return 'completeness-low'
})

// 评价
const comment = computed(() => {
  return basicInfoEvaluation.value?.comment ??
         basicInfoEvaluation.value?.completenessComment ??
         basicInfoEvaluation.value?.evaluation ??
         basicInfoEvaluation.value?.suggestions?.[0] ??
         ''
})
</script>

<style scoped>
.basic-info-section {
  padding: 8px 0;
}

.completeness-score {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-divider, #f0f0f0);
}

.completeness-display {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}

.completeness-bar {
  flex: 1;
  height: 12px;
  background-color: var(--border-divider, #e4e7ed);
  border-radius: 6px;
  overflow: hidden;
}

.completeness-fill {
  height: 100%;
  border-radius: 6px;
  transition: width 0.3s ease;
}

.completeness-high {
  background-color: var(--color-success, #67c23a);
}

.completeness-medium {
  background-color: var(--color-warning, #e6a23c);
}

.completeness-low {
  background-color: var(--color-danger, #f56c6c);
}

.completeness-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-title, #303133);
  min-width: 60px;
  text-align: right;
}

.completeness-label {
  font-size: 13px;
  color: var(--text-muted, #909399);
}

.info-items {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
  margin-bottom: 20px;
  box-sizing: border-box;
  width: 100%;
  overflow: hidden;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px;
  background-color: var(--bg-elevated, #f5f7fa);
  border-radius: 8px;
  transition: background-color 0.2s;
  min-width: 0;
  overflow: hidden;
  box-sizing: border-box;
  word-break: break-word;
}

.info-item:hover {
  background-color: var(--bg-card-hover, #e8ecf1);
}

.info-item.is-missing {
  opacity: 0.7;
}

.item-icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-card, #fff);
  border-radius: 4px;
}

.item-content {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.item-label {
  font-size: 12px;
  color: var(--text-muted, #909399);
  margin-bottom: 2px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.optional-tag {
  font-size: 11px;
  color: var(--text-muted, #909399);
  background-color: var(--border-divider, #f0f0f0);
  padding: 0 4px;
  border-radius: 2px;
}

.item-value {
  font-size: 14px;
  color: var(--text-title, #303133);
  font-weight: 500;
  white-space: normal;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
  word-break: break-all;
  line-height: 1.5;
}

.item-value.is-empty {
  color: var(--text-placeholder, #c0c4cc);
  font-weight: normal;
}

.item-status {
  flex-shrink: 0;
}

.basic-info-comment {
  padding-top: 16px;
  border-top: 1px solid var(--border-divider, #f0f0f0);
}

.comment-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-title, #303133);
  margin-bottom: 8px;
}

.comment-content {
  font-size: 14px;
  color: var(--text-body, #606266);
  line-height: 1.8;
  white-space: pre-wrap;
}

.empty-basic-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 0;
  color: var(--text-muted, #909399);
  font-size: 14px;
}
</style>
