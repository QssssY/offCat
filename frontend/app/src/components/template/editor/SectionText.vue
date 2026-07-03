<template>
  <!-- 自定义文本段编辑器 -->
  <div class="section-text-editor">
    <el-input
      type="textarea"
      :model-value="modelValue"
      @update:model-value="$emit('update:modelValue', $event)"
      :rows="4"
      placeholder="请输入自定义段落内容"
      resize="vertical"
    />
  </div>
</template>

<script setup>
defineProps({
  modelValue: { type: String, default: '' }
})
defineEmits(['update:modelValue'])
</script>

<style scoped>
.section-text-editor {
  padding: 16px;
}
</style>
