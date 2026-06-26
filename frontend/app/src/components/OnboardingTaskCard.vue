<template>
  <div class="onboarding-task-card">
    <div class="task-card-header">
      <div class="header-left">
        <div class="header-icon">
          <FeatureIcon name="onboarding-task" size="sm" />
        </div>
        <div class="header-text">
          <div class="header-title">快速上手</div>
          <div class="header-progress">{{ completedCount }} / {{ totalCount }} 已完成</div>
        </div>
      </div>
      <div class="progress-ring">
        <svg viewBox="0 0 36 36">
          <circle class="ring-bg" cx="18" cy="18" r="15.5" fill="none" stroke-width="3" />
          <circle
            class="ring-fill"
            cx="18" cy="18" r="15.5"
            fill="none" stroke-width="3"
            :stroke-dasharray="circumference"
            :stroke-dashoffset="dashOffset"
          />
        </svg>
        <span class="ring-text">{{ progressPercent }}%</span>
      </div>
    </div>

    <div class="task-list">
      <div
        v-for="item in tasks"
        :key="item.taskKey"
        class="task-item"
        :class="{ completed: item.completed }"
      >
        <div class="task-left">
          <div class="task-check" :class="{ done: item.completed }">
            <FeatureIcon v-if="item.completed" name="success" size="xs" />
            <span v-else class="check-empty"></span>
          </div>
          <div class="task-info">
            <div class="task-label">{{ item.taskLabel }}</div>
            <div class="task-desc">{{ item.taskDesc }}</div>
          </div>
        </div>
        <router-link v-if="!item.completed" :to="item.actionUrl" class="task-action">
          去完成
          <FeatureIcon :name="getTaskIcon(item)" size="xs" />
        </router-link>
        <span v-else class="task-done-label">已完成</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import FeatureIcon from '@/components/common/FeatureIcon.vue'

const props = defineProps({
  tasks: { type: Array, default: () => [] },
  completedCount: { type: Number, default: 0 },
  totalCount: { type: Number, default: 4 }
})

const circumference = 2 * Math.PI * 15.5

const getTaskIcon = (item) => {
  const text = `${item.taskKey || ''} ${item.taskLabel || ''} ${item.taskDesc || ''} ${item.actionUrl || ''}`.toLowerCase()
  if (text.includes('resume') || text.includes('简历')) return 'resume-upload'
  if (text.includes('interview') || text.includes('面试')) return 'ai-interviewer'
  if (text.includes('template') || text.includes('模板')) return 'template-editor'
  if (text.includes('growth') || text.includes('成长')) return 'growth-milestone'
  return 'onboarding-task'
}
const progressPercent = computed(() => {
  if (props.totalCount <= 0) return 0
  return Math.round((props.completedCount / props.totalCount) * 100)
})
const dashOffset = computed(() => {
  if (props.totalCount <= 0) return circumference
  return circumference * (1 - props.completedCount / props.totalCount)
})
</script>

<style scoped>
.onboarding-task-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 16px rgba(255, 140, 66, 0.08);
  border: 1px solid var(--border-card);
}

.task-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--bg-page);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--orange-light-bg) 0%, rgba(255, 140, 66, 0.08) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--orange-main);
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-title);
}

.header-progress {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 2px;
}

.progress-ring {
  position: relative;
  width: 44px;
  height: 44px;
}

.progress-ring svg {
  width: 44px;
  height: 44px;
  transform: rotate(-90deg);
}

.ring-bg {
  stroke: var(--bg-page);
}

.ring-fill {
  stroke: var(--orange-main);
  stroke-linecap: round;
  transition: stroke-dashoffset 0.4s ease;
}

.ring-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 11px;
  font-weight: 700;
  color: var(--orange-main);
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-radius: 10px;
  transition: background 0.15s ease;
}

.task-item:hover {
  background: var(--bg-card-hover);
}

.task-item.completed {
  opacity: 0.7;
}

.task-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

.task-check {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.task-check.done {
  background: var(--orange-main);
  color: #ffffff;
}

.task-check.done svg {
  width: 14px;
  height: 14px;
}

.check-empty {
  display: block;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 2px solid var(--border-card);
}

.task-info {
  min-width: 0;
}

.task-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-title);
}

.task-item.completed .task-label {
  text-decoration: line-through;
  color: var(--text-muted);
}

.task-desc {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.task-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
  color: var(--orange-main);
  text-decoration: none;
  padding: 6px 14px;
  border-radius: 8px;
  background: var(--orange-light-bg);
  transition: background-color 0.15s ease, color 0.15s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.task-action:hover {
  background: rgba(255, 140, 66, 0.15);
}

.task-done-label {
  font-size: 13px;
  color: var(--text-muted);
  flex-shrink: 0;
}

@media (max-width: 767px) {
  .onboarding-task-card {
    padding: 16px;
  }
  .task-item {
    padding: 10px 8px;
  }
  .task-desc {
    display: none;
  }
}
</style>
