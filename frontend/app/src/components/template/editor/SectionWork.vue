<template>
  <div class="section-work">
    <div class="section-header" @click="expanded = !expanded">
      <span class="section-label">工作经历</span>
      <span class="expand-icon">{{ expanded ? '▼' : '▶' }}</span>
    </div>
    <div v-show="expanded" class="section-content">
      <ExperienceItemEditor
        v-for="item in modelValue"
        :key="item.id"
        :item="item"
        :fields="workFields"
        title-field="company"
        :show-highlights="true"
        @update="$emit('update-item', item.id, $event)"
        @remove="$emit('remove-item', item.id)"
        @add-highlight="$emit('add-highlight', item.id)"
        @remove-highlight="(idx) => $emit('remove-highlight', item.id, idx)"
        @update-highlight="(idx, val) => $emit('update-highlight', item.id, idx, val)"
      />
      <el-button size="small" @click="$emit('add-item')">+ 添加工作经历</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ExperienceItemEditor from './ExperienceItemEditor.vue'

defineProps({
  modelValue: { type: Array, default: () => [] }
})

defineEmits(['update-item', 'remove-item', 'add-item', 'add-highlight', 'remove-highlight', 'update-highlight'])

const expanded = ref(true)

const workFields = [
  { key: 'company', label: '公司', type: 'text', placeholder: '请输入公司名称' },
  { key: 'position', label: '职位', type: 'text', placeholder: '请输入职位名称' },
  { key: 'startDate', label: '开始时间', type: 'text', placeholder: '如：2021-03' },
  { key: 'endDate', label: '结束时间', type: 'text', placeholder: '如：至今' }
]
</script>

<style scoped>
.section-work {
  border: 1px solid var(--border-card);
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 12px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--bg-card-hover);
  cursor: pointer;
  user-select: none;
}

.section-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-title);
}

.expand-icon {
  font-size: 12px;
  color: var(--text-muted);
}

.section-content {
  padding: 16px;
  background: var(--bg-card);
}
</style>
