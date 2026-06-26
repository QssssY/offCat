<template>
  <div class="experience-item-editor">
    <div class="item-header">
      <div class="item-title">{{ item[titleField] || '未命名' }}</div>
      <el-button type="danger" text size="small" @click="$emit('remove')">删除</el-button>
    </div>
    <el-form label-width="80px" size="small">
      <el-form-item v-for="field in fields" :key="field.key" :label="field.label">
        <el-input
          v-if="field.type === 'text'"
          :model-value="item[field.key]"
          @update:model-value="update(field.key, $event)"
          :placeholder="field.placeholder"
        />
        <el-input
          v-else-if="field.type === 'textarea'"
          type="textarea"
          :model-value="item[field.key]"
          @update:model-value="update(field.key, $event)"
          :rows="2"
          :placeholder="field.placeholder"
          resize="vertical"
        />
      </el-form-item>
    </el-form>

    <!-- 亮点列表 -->
    <div v-if="showHighlights" class="highlights-section">
      <div class="highlights-label">亮点/成果</div>
      <div v-for="(h, index) in item.highlights" :key="index" class="highlight-row">
        <el-input
          size="small"
          :model-value="h"
          @update:model-value="$emit('update-highlight', index, $event)"
          placeholder="描述一项成果或亮点"
        />
        <el-button text size="small" @click="$emit('remove-highlight', index)">×</el-button>
      </div>
      <el-button size="small" text @click="$emit('add-highlight')">+ 添加亮点</el-button>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  item: { type: Object, required: true },
  fields: { type: Array, required: true },
  titleField: { type: String, default: 'name' },
  showHighlights: { type: Boolean, default: false }
})

const emit = defineEmits(['update', 'remove', 'add-highlight', 'remove-highlight', 'update-highlight'])

function update(key, value) {
  emit('update', { ...props.item, [key]: value })
}
</script>

<style scoped>
.experience-item-editor {
  border: 1px solid var(--border-card);
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 10px;
  background: var(--bg-card);
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.item-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-title);
}

.highlights-section {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--border-card);
}

.highlights-label {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.highlight-row {
  display: flex;
  gap: 6px;
  margin-bottom: 6px;
  align-items: center;
}
</style>
