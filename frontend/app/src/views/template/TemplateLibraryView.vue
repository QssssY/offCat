<template>
  <div class="template-library">
    <div class="page-header">
      <h1 class="page-title">简历模板库</h1>
      <p class="page-desc">选择适合你的行业模板，快速创建专业简历</p>
    </div>

    <IndustryFilter
      :industries="industries"
      v-model="selectedIndustry"
    />

    <div class="template-grid" v-if="filteredTemplates.length">
      <TemplateCard
        v-for="tpl in filteredTemplates"
        :key="tpl.id"
        :template="tpl"
        @use="useTemplate(tpl.id)"
        @preview="previewTemplate(tpl)"
      />
    </div>

    <div v-else class="empty-state">
      <p>该行业暂无模板</p>
    </div>

    <TemplatePreviewDialog
      v-model:visible="previewVisible"
      :template="previewingTemplate"
      @use-template="useTemplate(previewingTemplate?.id)"
    />
  </div>
</template>

<script setup>
import { ref, computed, defineAsyncComponent } from 'vue'
import { useRouter } from 'vue-router'
import { industries } from '@/data/industries.js'
import { templates } from '@/data/templates.js'
import IndustryFilter from '@/components/template/IndustryFilter.vue'
import TemplateCard from '@/components/template/TemplateCard.vue'
import { prefetchTemplateEditorRoute } from '@/router/routeLoaders'

defineOptions({
  name: 'TemplateLibraryView'
})

const TemplatePreviewDialog = defineAsyncComponent(() =>
  import('@/components/template/TemplatePreviewDialog.vue')
)

const router = useRouter()
const selectedIndustry = ref('all')
const previewVisible = ref(false)
const previewingTemplate = ref(null)

const filteredTemplates = computed(() => {
  if (selectedIndustry.value === 'all') return templates
  return templates.filter(t => t.industry === selectedIndustry.value)
})

async function useTemplate(templateId) {
  if (!templateId) return
  previewVisible.value = false
  await prefetchTemplateEditorRoute(templateId).catch((error) => {
    console.debug('模板编辑页预取失败', error)
  })
  router.push(`/templates/editor/${templateId}`)
}

function previewTemplate(tpl) {
  previewingTemplate.value = tpl
  previewVisible.value = true
}
</script>

<style scoped>
.template-library {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-title);
  margin-bottom: 8px;
}

.page-desc {
  font-size: 15px;
  color: var(--text-muted);
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

@media (max-width: 1024px) {
  .template-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .template-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .template-grid {
    grid-template-columns: 1fr;
  }
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: var(--text-muted);
  font-size: 15px;
}
</style>
