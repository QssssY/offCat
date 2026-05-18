<template>
  <div class="score-panel">
    <div
      v-for="dim in dimensions"
      :key="dim.key"
      class="score-item"
      :class="{ 'score-item--open': expandedKeys.has(dim.key) }"
    >
      <!-- 维度标题行：名称 + 得分 + 展开箭头 -->
      <div class="score-item-header" @click="toggle(dim.key)">
        <div class="score-item-left">
          <span class="score-item-label">{{ dim.label }}</span>
          <div class="score-bar-track">
            <div
              class="score-bar-fill"
              :style="{ width: dim.score + '%' }"
              :class="scoreLevelClass(dim.score)"
            ></div>
          </div>
        </div>
        <div class="score-item-right">
          <span class="score-item-value" :class="scoreLevelClass(dim.score)">{{ dim.score }}</span>
          <svg
            class="score-arrow"
            :class="{ 'score-arrow--open': expandedKeys.has(dim.key) }"
            width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
          >
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </div>
      </div>

      <!-- 展开区域：加分项 + 扣分项 -->
      <div v-if="expandedKeys.has(dim.key)" class="score-item-body">
        <!-- 加分项 -->
        <div v-if="dim.plus.length" class="score-detail-group">
          <div class="score-detail-title score-detail-title--plus">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            加分项
          </div>
          <ul class="score-detail-list">
            <li v-for="(item, i) in dim.plus" :key="`plus-${i}`" class="score-detail-item score-detail-item--plus">
              {{ item }}
            </li>
          </ul>
        </div>
        <!-- 扣分项 -->
        <div v-if="dim.minus.length" class="score-detail-group">
          <div class="score-detail-title score-detail-title--minus">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            扣分项
          </div>
          <ul class="score-detail-list">
            <li v-for="(item, i) in dim.minus" :key="`minus-${i}`" class="score-detail-item score-detail-item--minus">
              {{ item }}
            </li>
          </ul>
        </div>
        <!-- 无明细时的兜底 -->
        <div v-if="!dim.plus.length && !dim.minus.length" class="score-detail-empty">
          暂无明细数据
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive } from 'vue'

const props = defineProps({
  details: {
    type: Object,
    required: true,
    // { basicInfo: { score, plus:[], minus:[] }, skill: {...}, work: {...}, project: {...}, education: {...} }
  },
  dimensionConfig: {
    type: Array,
    default: () => [
      { key: 'basicInfo', label: '基本信息' },
      { key: 'skill', label: '岗位能力' },
      { key: 'work', label: '工作经验' },
      { key: 'project', label: '项目经历' },
      { key: 'education', label: '教育背景' },
    ],
  },
})

// 组装维度数据列表
const dimensions = computed(() =>
  props.dimensionConfig.map((cfg) => {
    const d = props.details?.[cfg.key] || {}
    return {
      key: cfg.key,
      label: cfg.label,
      score: d.score || 0,
      plus: Array.isArray(d.plus) ? d.plus : Array.isArray(d.strengths) ? d.strengths : [],
      minus: Array.isArray(d.minus) ? d.minus : Array.isArray(d.weaknesses) ? d.weaknesses : [],
    }
  })
)

// 默认展开得分最低的维度
const lowestKey = computed(() => {
  const sorted = [...dimensions.value].sort((a, b) => a.score - b.score)
  return sorted[0]?.key || props.dimensionConfig[0]?.key || 'basicInfo'
})

// 用 reactive 跟踪展开状态，初始展开得分最低项
const expandedKeys = reactive(new Set([lowestKey.value]))

const toggle = (key) => {
  if (expandedKeys.has(key)) {
    expandedKeys.delete(key)
  } else {
    expandedKeys.add(key)
  }
}

// 得分等级对应的 CSS 类
const scoreLevelClass = (score) => {
  if (score >= 80) return 'score-high'
  if (score >= 60) return 'score-medium'
  return 'score-low'
}
</script>

<style scoped>
.score-panel {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.score-item {
  border-radius: 8px;
  background: #faf9f5;
  overflow: hidden;
  transition: background 0.2s;
}

.score-item--open {
  background: #f5f0e8;
}

.score-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;
}

.score-item-header:hover {
  background: rgba(204,120,92,0.04);
}

.score-item-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.score-item-label {
  font-size: 13px;
  font-weight: 600;
  color: #141413;
  white-space: nowrap;
  min-width: 62px;
  flex-shrink: 0;
}

.score-bar-track {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: #e6dfd8;
  overflow: hidden;
}

.score-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.6s ease;
}

.score-bar-fill.score-high {
  background: #5db872;
}

.score-bar-fill.score-medium {
  background: #cc785c;
}

.score-bar-fill.score-low {
  background: #c64545;
}

.score-item-right {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: 12px;
}

.score-item-value {
  font-size: 16px;
  font-weight: 700;
  min-width: 28px;
  text-align: right;
}

.score-item-value.score-high {
  color: #5db872;
}

.score-item-value.score-medium {
  color: #cc785c;
}

.score-item-value.score-low {
  color: #c64545;
}

.score-arrow {
  color: #8e8b82;
  transition: transform 0.2s;
  flex-shrink: 0;
}

.score-arrow--open {
  transform: rotate(180deg);
}

.score-item-body {
  padding: 0 14px 12px;
}

.score-detail-group {
  margin-top: 8px;
}

.score-detail-title {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 4px;
}

.score-detail-title--plus {
  color: #5db872;
}

.score-detail-title--minus {
  color: #c64545;
}

.score-detail-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.score-detail-item {
  font-size: 12px;
  line-height: 1.6;
  padding: 2px 0 2px 16px;
  position: relative;
}

.score-detail-item--plus {
  color: #3d8a5a;
}

.score-detail-item--plus::before {
  content: '+';
  position: absolute;
  left: 2px;
  font-weight: 700;
  color: #5db872;
}

.score-detail-item--minus {
  color: #a94442;
}

.score-detail-item--minus::before {
  content: '−';
  position: absolute;
  left: 2px;
  font-weight: 700;
  color: #c64545;
}

.score-detail-empty {
  font-size: 12px;
  color: #8e8b82;
  padding: 4px 0;
}
</style>
