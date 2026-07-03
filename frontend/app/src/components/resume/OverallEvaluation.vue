<template>
  <div class="overall-evaluation">
    <div class="score-section">
      <div class="score-left">
        <div class="score-display">
          <div class="score-value">{{ score }}</div>
          <div class="score-label">综合得分</div>
        </div>
        <div class="grade-section">
          <div class="grade-badge" :class="gradeClass">{{ grade }}</div>
          <div class="grade-label">综合等级</div>
        </div>
      </div>
    </div>
    <div v-if="summary" class="summary-section">
      <div class="summary-label">总体评价</div>
      <div class="summary-content">{{ summary }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({
  data: {
    type: Object,
    default: () => ({})
  }
})

// 提取数据，处理多种可能的字段名
const score = computed(() => {
  const s = props.data?.score ?? props.data?.totalScore ?? props.data?.total_score
  return typeof s === 'number' ? s : '-'
})

const grade = computed(() => {
  const g = props.data?.grade ?? props.data?.level ?? props.data?.rating
  return g || '-'
})

const summary = computed(() => {
  return props.data?.summary ?? props.data?.overallComment ?? props.data?.comment ?? ''
})

// 等级样式类
const gradeClass = computed(() => {
  const g = String(grade.value || '').trim().toLowerCase()
  const gradeLetter = g.match(/^[sabcd](?=\b|\s|-|_|$)/)?.[0]
  if (gradeLetter === 's' || gradeLetter === 'a' || g.includes('优') || g.includes('excellent')) return 'grade-excellent'
  if (gradeLetter === 'b' || g.includes('良') || g.includes('good')) return 'grade-good'
  if (gradeLetter === 'c' || g.includes('中') || g.includes('average')) return 'grade-average'
  if (gradeLetter === 'd' || g.includes('差') || g.includes('poor')) return 'grade-poor'
  return 'grade-default'
})
</script>

<style scoped>
.overall-evaluation {
  padding: 8px 0;
}

.score-section {
  display: flex;
  align-items: center;
  gap: 40px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-divider, #f0f0f0);
}

.score-left {
  display: flex;
  gap: 40px;
  flex-shrink: 0;
}

.score-display {
  text-align: center;
  min-width: 100px;
}

.score-value {
  font-size: 44px;
  font-weight: 700;
  color: #141413;
  line-height: 1;
  margin-bottom: 8px;
}

.score-label {
  font-size: 12px;
  color: #6c6a64;
  letter-spacing: 0.03em;
  text-transform: uppercase;
}

.grade-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.grade-badge {
  padding: 6px 20px;
  border-radius: 9999px;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}

.grade-excellent {
  background: rgba(93,184,114,0.12);
  color: #3d8a5a;
}

.grade-good {
  background: rgba(204,120,92,0.12);
  color: #a9583e;
}

.grade-average {
  background: rgba(212,160,23,0.12);
  color: #b58a14;
}

.grade-poor {
  background: rgba(198,69,69,0.1);
  color: #b53a3a;
}

.grade-default {
  background: #efe9de;
  color: #6c6a64;
}

.grade-label {
  font-size: 12px;
  color: #6c6a64;
  letter-spacing: 0.03em;
}

.summary-section {
  padding-top: 8px;
}

.summary-label {
  font-size: 14px;
  font-weight: 500;
  color: #141413;
  margin-bottom: 10px;
}

.summary-content {
  font-size: 14px;
  color: #3d3d3a;
  line-height: 1.8;
  white-space: pre-wrap;
}

[data-theme="dark"] .score-section {
  border-bottom-color: var(--border-card);
}

[data-theme="dark"] .score-value {
  color: var(--text-title);
}

[data-theme="dark"] .score-label {
  color: var(--text-muted);
}

[data-theme="dark"] .grade-excellent {
  background: rgba(93,184,114,0.15);
  color: #5db872;
}

[data-theme="dark"] .grade-good {
  background: rgba(204,120,92,0.15);
  color: #d08a6a;
}

[data-theme="dark"] .grade-average {
  background: rgba(212,160,23,0.15);
  color: #d4a017;
}

[data-theme="dark"] .grade-poor {
  background: rgba(198,69,69,0.15);
  color: #e06060;
}

[data-theme="dark"] .grade-default {
  background: rgba(255,255,255,0.06);
  color: var(--text-muted);
}

[data-theme="dark"] .grade-label {
  color: var(--text-muted);
}

[data-theme="dark"] .summary-label {
  color: var(--text-title);
}

[data-theme="dark"] .summary-content {
  color: var(--text-body);
}
</style>
