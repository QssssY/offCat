<template>
  <div class="template-card" @click="$emit('use')">
    <div class="card-thumbnail">
      <TemplatePreviewImage
        :template-id="template.id"
        :color="template.colorAccent"
        :bg-color="previewBgColor"
      />
    </div>
    <div class="card-info">
      <div class="card-name">{{ template.name }}</div>
      <div class="card-desc">{{ template.description }}</div>
      <div class="card-tags">
        <span v-for="tag in template.tags" :key="tag" class="tag">{{ tag }}</span>
      </div>
    </div>
    <div class="card-actions">
      <el-button size="small" @click.stop="$emit('preview')">预览</el-button>
      <el-button type="primary" size="small" @click.stop="$emit('use')">使用模板</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import TemplatePreviewImage from './TemplatePreviewImage.vue'

const props = defineProps({
  template: { type: Object, required: true }
})

defineEmits(['use', 'preview'])

const darkPreviewTemplates = ['tech-dark', 'finance-classic', 'legal-authoritative']
const previewBgColor = computed(() => (
  darkPreviewTemplates.includes(props.template.id) ? '#111827' : '#ffffff'
))
</script>

<style scoped>
.template-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  contain: layout paint style;
}

.template-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hover);
}

.card-thumbnail {
  width: 100%;
  aspect-ratio: 210 / 297;
  overflow: hidden;
  background: #f8f8f8;
}

.card-info {
  padding: 14px 16px;
}

.card-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-title);
  margin-bottom: 4px;
}

.card-desc {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  background: var(--orange-light-bg);
  color: var(--orange-main);
}

.card-actions {
  padding: 0 16px 14px;
  display: flex;
  gap: 8px;
}
</style>
