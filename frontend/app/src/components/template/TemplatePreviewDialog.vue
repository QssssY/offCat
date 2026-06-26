<template>
  <el-dialog
    :model-value="visible"
    :title="template?.name || '模板预览'"
    width="700px"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div class="preview-container" v-if="template">
      <div class="preview-paper">
        <component :is="'style'" v-if="templateStyle" v-html="templateStyle" />
        <TemplateRenderer
          :template-id="template.id"
          :resume-data="sampleData"
        />
      </div>
    </div>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">关闭</el-button>
      <el-button type="primary" @click="$emit('use-template')">使用此模板</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import TemplateRenderer from './TemplateRenderer.vue'
import { defaultResumeData } from '@/data/contents/_default.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  template: { type: Object, default: null }
})

defineEmits(['update:visible', 'use-template'])

const sampleData = defaultResumeData
const templateStyle = ref('')
let loadSequence = 0

async function loadTemplateStyle(templateId) {
  const sequence = ++loadSequence

  if (!templateId) {
    templateStyle.value = ''
    return
  }

  try {
    const styleModule = await import(`@/data/styles/${templateId}.css?raw`)
    if (sequence === loadSequence) {
      templateStyle.value = styleModule.default
    }
  } catch (error) {
    if (sequence === loadSequence) {
      templateStyle.value = ''
    }
    console.error('加载模板预览样式失败:', error)
  }
}

watch(
  () => [props.visible, props.template?.id],
  ([visible, templateId]) => {
    if (visible) {
      loadTemplateStyle(templateId)
      return
    }

    templateStyle.value = ''
  },
  { immediate: true }
)
</script>

<style scoped>
.preview-container {
  max-height: 70vh;
  overflow-y: auto;
  display: flex;
  justify-content: center;
}

.preview-paper {
  width: 100%;
  max-width: 600px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
</style>
