<template>
  <div class="section-project">
    <div class="section-header" @click="expanded = !expanded">
      <span class="section-label">项目经历</span>
      <span class="expand-icon">{{ expanded ? '▼' : '▶' }}</span>
    </div>
    <div v-show="expanded" class="section-content">
      <ExperienceItemEditor
        v-for="item in modelValue"
        :key="item.id"
        :item="item"
        :fields="projectFields"
        title-field="name"
        :show-highlights="true"
        @update="$emit('update-item', item.id, $event)"
        @remove="$emit('remove-item', item.id)"
        @add-highlight="$emit('add-highlight', item.id)"
        @remove-highlight="(idx) => $emit('remove-highlight', item.id, idx)"
        @update-highlight="(idx, val) => $emit('update-highlight', item.id, idx, val)"
      />
      <el-button size="small" @click="$emit('add-item')">+ 添加项目经历</el-button>
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

const projectFields = [
  { key: 'name', label: '项目名称', type: 'text', placeholder: '请输入项目名称' },
  { key: 'role', label: '角色', type: 'text', placeholder: '如：前端负责人' },
  { key: 'startDate', label: '开始时间', type: 'text', placeholder: '如：2023-01' },
  { key: 'endDate', label: '结束时间', type: 'text', placeholder: '如：2023-06' },
  { key: 'description', label: '项目描述', type: 'textarea', placeholder: '简要描述项目内容' }
]
</script>

<style scoped>
.section-project {
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
