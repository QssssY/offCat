<template>
  <div class="template-editor-view">
    <!-- 顶部工具栏 -->
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <el-button text @click="goBack">
          ← 返回模板库
        </el-button>
        <span class="template-name" v-if="currentTemplate">{{ currentTemplate.name }}</span>
      </div>
      <div class="toolbar-right">
        <ExportToolbar
          :target-ref="previewRef"
          :file-name="currentTemplate?.name || '简历'"
          :template-css="templateStyle"
        />
      </div>
    </div>

    <!-- 编辑器主体 -->
    <div class="editor-body">
      <!-- 左侧编辑面板 -->
      <div class="editor-panel">
        <ResumeEditor />
      </div>

      <!-- 右侧预览 -->
      <div class="preview-panel">
        <div class="preview-paper-wrapper">
          <div class="preview-paper" ref="previewRef">
            <component :is="'style'" v-html="templateStyle" />
            <TemplateRenderer
              v-if="store.resumeData"
              :template-id="store.templateId"
              :resume-data="store.resumeData"
              :sections-config="store.sectionsConfig"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 移动端切换标签 -->
    <div class="mobile-tabs">
      <button
        class="tab-btn"
        :class="{ active: mobileTab === 'edit' }"
        @click="mobileTab = 'edit'"
      >编辑</button>
      <button
        class="tab-btn"
        :class="{ active: mobileTab === 'preview' }"
        @click="mobileTab = 'preview'"
      >预览</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTemplateEditorStore } from '@/stores/templateEditor.js'
import { templates } from '@/data/templates.js'
import TemplateRenderer from '@/components/template/TemplateRenderer.vue'
import ResumeEditor from '@/components/template/ResumeEditor.vue'
import ExportToolbar from '@/components/template/ExportToolbar.vue'

const route = useRoute()
const router = useRouter()
const store = useTemplateEditorStore()

const previewRef = ref(null)
const templateStyle = ref('')
const mobileTab = ref('edit')

const currentTemplate = computed(() => {
  return templates.find(t => t.id === route.params.templateId)
})

async function loadTemplateData() {
  const id = route.params.templateId
  if (!id) return

  try {
    // 动态加载内容数据
    const contentModule = await import(`@/data/contents/${id}.js`)
    store.loadTemplate(id, contentModule.default)

    // 动态加载样式
    const styleModule = await import(`@/data/styles/${id}.css?raw`)
    templateStyle.value = styleModule.default
  } catch (e) {
    console.error('加载模板失败:', e)
    router.push('/templates')
  }
}

function goBack() {
  router.push('/templates')
}

onMounted(() => {
  loadTemplateData()
})

watch(() => route.params.templateId, () => {
  loadTemplateData()
})
</script>

<style scoped>
.template-editor-view {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-page);
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: var(--bg-header);
  border-bottom: 1px solid var(--border-card);
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.template-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-title);
}

.editor-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.editor-panel {
  width: 40%;
  min-width: 320px;
  border-right: 1px solid var(--border-card);
  overflow-y: auto;
  background: var(--bg-card);
}

.preview-panel {
  flex: 1;
  overflow-y: auto;
  display: flex;
  justify-content: center;
  padding: 24px;
  background: #e5e7eb;
}

.preview-paper-wrapper {
  width: 100%;
  max-width: 680px;
}

.preview-paper {
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  min-height: 960px;
}

.mobile-tabs {
  display: none;
}

@media (max-width: 768px) {
  .editor-body {
    flex-direction: column;
  }

  .editor-panel {
    width: 100%;
    min-width: unset;
    border-right: none;
  }

  .preview-panel {
    width: 100%;
  }

  .mobile-tabs {
    display: flex;
    border-top: 1px solid var(--border-card);
    background: var(--bg-card);
  }

  .tab-btn {
    flex: 1;
    padding: 10px;
    border: none;
    background: none;
    font-size: 14px;
    cursor: pointer;
    color: var(--text-muted);
  }

  .tab-btn.active {
    color: var(--orange-main);
    font-weight: 600;
    border-bottom: 2px solid var(--orange-main);
  }
}
</style>
