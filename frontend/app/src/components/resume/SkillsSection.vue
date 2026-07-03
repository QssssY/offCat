<template>
  <div class="skills-section">
    <!-- 技能得分 -->
    <div v-if="hasScore" class="skills-score">
      <div class="score-circle" :style="scoreStyle">
        <span class="score-number">{{ score }}</span>
        <span class="score-unit">分</span>
      </div>
      <div class="score-info">
        <div class="score-label">技能得分</div>
        <div v-if="scoreComment" class="score-comment">{{ scoreComment }}</div>
        <div v-if="evaluation" class="score-evaluation">{{ evaluation }}</div>
      </div>
    </div>

    <!-- 技能标签 -->
    <div v-if="skills.length > 0" class="skills-tags-section">
      <div class="section-label">技能列表</div>
      <div class="skills-tags">
        <el-tag
          v-for="(skill, index) in skills"
          :key="index"
          :type="getTagType(index)"
          effect="light"
          class="skill-tag"
        >
          {{ skill }}
        </el-tag>
      </div>
    </div>

    <!-- 技术栈 -->
    <div v-if="techStack.length > 0" class="tech-stack-section">
      <div class="section-label">技术栈</div>
      <div class="tech-list">
        <div v-for="(tech, index) in techStack" :key="index" class="tech-item">
          <FeatureIcon name="success" size="xs" />
          <span>{{ tech }}</span>
        </div>
      </div>
    </div>

    <!-- 技能描述/建议 -->
    <div v-if="skillDescription" class="skill-description">
      <div class="section-label">技能评价</div>
      <div class="description-content">{{ skillDescription }}</div>
    </div>

    <!-- 空状态 -->
    <div v-if="!hasContent" class="empty-skills">
      <FeatureIcon name="empty-state" size="md" />
      <span>暂无技能信息</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import FeatureIcon from '@/components/common/FeatureIcon.vue'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({})
  }
})

// 技能得分
const score = computed(() => {
  const s = props.data?.score ?? props.data?.skillScore ?? props.data?.totalScore
  return typeof s === 'number' ? s : null
})

const hasScore = computed(() => score.value !== null)

const scoreComment = computed(() => {
  return props.data?.scoreComment ?? props.data?.comment ?? ''
})

// 技能列表
const skills = computed(() => {
  const data = props.data
  if (!data) return []

  // 可能的技能字段
  const possibleKeys = ['skills', 'skillList', 'skillNames', 'items']
  for (const key of possibleKeys) {
    if (Array.isArray(data[key])) {
      return data[key].filter(s => typeof s === 'string')
    }
  }

  // 如果是数组直接返回
  if (Array.isArray(data)) {
    return data.filter(s => typeof s === 'string')
  }

  return []
})

// 技术栈
const techStack = computed(() => {
  const ts = props.data?.techStack ?? props.data?.tech_stack ?? props.data?.stack
  if (Array.isArray(ts)) {
    return ts.filter(t => typeof t === 'string')
  }
  return []
})

// 技能描述
const skillDescription = computed(() => {
  return props.data?.description ?? props.data?.skillDescription ?? ''
})

// 评价段落
const evaluation = computed(() => {
  return props.data?.evaluation ?? ''
})

// 是否有内容
const hasContent = computed(() => {
  return hasScore.value ||
    skills.value.length > 0 ||
    techStack.value.length > 0 ||
    skillDescription.value
})

// 得分圆环样式
const scoreStyle = computed(() => {
  const s = score.value || 0
  const percentage = Math.min(Math.max(s, 0), 100)
  const color = percentage >= 80 ? '#67c23a' : percentage >= 60 ? '#e6a23c' : '#f56c6c'

  return {
    background: `conic-gradient(${color} ${percentage * 3.6}deg, #e4e7ed 0deg)`
  }
})

// 标签类型
const getTagType = (index) => {
  const types = ['primary', 'success', 'warning', 'info']
  return types[index % types.length]
}
</script>

<style scoped>
.skills-section {
  padding: 4px 0;
}

.skills-score {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e6dfd8;
}

.score-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  background: #efe9de;
  color: #141413;
}

.score-circle::before {
  content: '';
  position: absolute;
  inset: 5px;
  background: #faf9f5;
  border-radius: 50%;
}

.score-number {
  position: relative;
  font-size: 24px;
  font-weight: 700;
  color: #141413;
  line-height: 1;
}

.score-unit {
  position: relative;
  font-size: 12px;
  color: #6c6a64;
  margin-top: 2px;
}

.score-info {
  flex: 1;
}

.score-label {
  font-size: 14px;
  font-weight: 500;
  color: #141413;
  margin-bottom: 4px;
}

.score-comment {
  font-size: 13px;
  color: #3d3d3a;
  line-height: 1.6;
}

.score-evaluation {
  margin-top: 8px;
  font-size: 13px;
  color: #3d3d3a;
  line-height: 1.7;
  text-align: justify;
}

.skills-tags-section,
.tech-stack-section,
.skill-description {
  margin-bottom: 20px;
}

.section-label {
  font-size: 14px;
  font-weight: 500;
  color: #141413;
  margin-bottom: 12px;
}

.skills-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.skill-tag {
  font-size: 13px;
}

.tech-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tech-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #3d3d3a;
  padding: 4px 10px;
  background: #faf9f5;
  border-radius: 6px;
  border: 1px solid #e6dfd8;
}

.description-content {
  font-size: 14px;
  color: #3d3d3a;
  line-height: 1.8;
  white-space: pre-wrap;
}

.empty-skills {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 0;
  color: #6c6a64;
  font-size: 14px;
}

[data-theme="dark"] .skills-score {
  border-bottom-color: var(--border-card);
}

[data-theme="dark"] .score-circle {
  background: #3a3835;
  color: var(--text-title);
}

[data-theme="dark"] .score-circle::before {
  background: var(--bg-card);
}

[data-theme="dark"] .score-number {
  color: var(--text-title);
}

[data-theme="dark"] .score-unit {
  color: var(--text-muted);
}

[data-theme="dark"] .score-label {
  color: var(--text-title);
}

[data-theme="dark"] .score-comment {
  color: var(--text-body);
}

[data-theme="dark"] .score-evaluation {
  color: var(--text-body);
}

[data-theme="dark"] .section-label {
  color: var(--text-title);
}

[data-theme="dark"] .tech-item {
  color: var(--text-body);
  background: rgba(255,255,255,0.06);
  border-color: var(--border-card);
}

[data-theme="dark"] .description-content {
  color: var(--text-body);
}

[data-theme="dark"] .empty-skills {
  color: var(--text-muted);
}
</style>
