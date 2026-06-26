<template>
  <div class="section-education">
    <div class="section-header" @click="expanded = !expanded">
      <span class="section-label">教育经历</span>
      <span class="expand-icon">{{ expanded ? '▼' : '▶' }}</span>
    </div>
    <div v-show="expanded" class="section-content">
      <ExperienceItemEditor
        v-for="item in modelValue"
        :key="item.id"
        :item="item"
        :fields="eduFields"
        title-field="school"
        @update="$emit('update-item', item.id, $event)"
        @remove="$emit('remove-item', item.id)"
      />
      <el-button size="small" @click="$emit('add-item')">+ 添加教育经历</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ExperienceItemEditor from './ExperienceItemEditor.vue'

defineProps({
  modelValue: { type: Array, default: () => [] }
})

defineEmits(['update-item', 'remove-item', 'add-item'])

const expanded = ref(true)

const eduFields = [
  { key: 'school', label: '学校', type: 'text', placeholder: '请输入学校名称' },
  { key: 'degree', label: '学历', type: 'text', placeholder: '如：本科、硕士' },
  { key: 'major', label: '专业', type: 'text', placeholder: '请输入专业名称' },
  { key: 'startDate', label: '开始时间', type: 'text', placeholder: '如：2019-09' },
  { key: 'endDate', label: '结束时间', type: 'text', placeholder: '如：2023-06' },
  { key: 'description', label: '描述', type: 'textarea', placeholder: '可选，补充说明' }
]
</script>

<style scoped>
.section-education {
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
