<template>
  <div class="section-summary">
    <div class="section-header" @click="expanded = !expanded">
      <span class="section-label">个人简介</span>
      <span class="expand-icon">{{ expanded ? '▼' : '▶' }}</span>
    </div>
    <div v-show="expanded" class="section-content">
      <el-input
        type="textarea"
        :model-value="modelValue"
        @update:model-value="$emit('update:modelValue', $event)"
        :rows="4"
        placeholder="请输入个人简介，简要描述你的职业背景和核心优势"
        resize="vertical"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  modelValue: { type: String, default: '' }
})

defineEmits(['update:modelValue'])
const expanded = ref(true)
</script>

<style scoped>
.section-summary {
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
