<template>
  <div class="template-card" @click="$emit('use')">
    <div class="card-thumbnail">
      <div class="thumbnail-container" ref="containerRef">
        <div
          class="thumbnail-wrapper"
          :style="{ transform: `scale(${scaleRatio})` }"
        >
          <!-- 注入模板专属CSS，确保缩略图与编辑器预览一致 -->
          <component :is="'style'" v-if="templateStyle" v-html="templateStyle" />
          <TemplateRenderer
            :template-id="template.id"
            :resume-data="sampleData"
          />
        </div>
      </div>
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
import { ref, onMounted, onUnmounted } from 'vue'
import TemplateRenderer from './TemplateRenderer.vue'
import { defaultResumeData } from '@/data/contents/_default.js'

const props = defineProps({
  template: { type: Object, required: true }
})

defineEmits(['use', 'preview'])

const sampleData = defaultResumeData
const containerRef = ref(null)
const scaleRatio = ref(0.2)
const templateStyle = ref('')

// 动态加载模板CSS，与TemplateEditorView使用相同的import方式
async function loadTemplateStyle(id) {
  try {
    const styleModule = await import(`@/data/styles/${id}.css?raw`)
    templateStyle.value = styleModule.default
  } catch (e) {
    templateStyle.value = ''
  }
}

let resizeObserver = null

onMounted(() => {
  // 加载当前模板的CSS
  loadTemplateStyle(props.template.id)

  if (!containerRef.value) return

  const updateScale = () => {
    const container = containerRef.value
    if (!container) return
    const containerWidth = container.clientWidth
    scaleRatio.value = containerWidth / 800
  }

  resizeObserver = new ResizeObserver(updateScale)
  resizeObserver.observe(containerRef.value)
  updateScale()
})

onUnmounted(() => {
  resizeObserver?.disconnect()
})
</script>

<style scoped>
.template-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
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

.thumbnail-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.thumbnail-wrapper {
  width: 800px;
  transform-origin: top left;
  pointer-events: none;
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
