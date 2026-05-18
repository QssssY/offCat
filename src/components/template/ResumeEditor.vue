<template>
  <!-- 简历编辑器：动态渲染所有段落，支持增删排序 -->
  <div class="resume-editor" v-if="store.resumeData">
    <!-- 添加段落按钮：顶部 -->
    <div class="add-section-area">
      <el-dropdown trigger="click" @command="handleAddSection">
        <el-button type="primary" plain size="small" style="width: 100%">
          + 添加段落
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-for="t in store.availableSectionTypes"
              :key="t.type"
              :command="t"
            >{{ t.label }}</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 基础信息 -->
    <SectionBasicInfo
      :model-value="store.resumeData.basic"
      @update:model-value="updateBasic"
    />

    <!-- 动态渲染的段落列表 -->
    <div
      v-for="(sec, idx) in store.sectionsConfig"
      :key="sec.key"
      class="editor-section"
      :class="{ 'section-hidden': !sec.visible }"
    >
      <!-- 段落头部：标题 + 操作按钮 -->
      <div class="section-header" @click="toggleExpand(sec.key)">
        <div class="header-left">
          <span class="drag-handle" title="拖拽排序">⠿</span>
          <!-- 双击标题可重命名 -->
          <span
            v-if="editingKey !== sec.key"
            class="section-label"
            @dblclick.stop="startEditTitle(sec.key, sec.title)"
            title="双击重命名"
          >{{ sec.title }}</span>
          <el-input
            v-else
            ref="titleInputRef"
            v-model="editingTitle"
            size="small"
            class="title-edit-input"
            @blur="saveTitle(sec.key)"
            @keyup.enter="saveTitle(sec.key)"
            @keyup.escape="cancelEditTitle"
            @click.stop
          />
          <span v-if="!sec.visible" class="hidden-badge">已隐藏</span>
        </div>
        <div class="header-right">
          <!-- 上移 -->
          <el-button
            text
            size="small"
            :disabled="idx === 0"
            @click.stop="store.reorderSections(idx, idx - 1)"
            title="上移"
          >↑</el-button>
          <!-- 下移 -->
          <el-button
            text
            size="small"
            :disabled="idx === store.sectionsConfig.length - 1"
            @click.stop="store.reorderSections(idx, idx + 1)"
            title="下移"
          >↓</el-button>
          <!-- 显示/隐藏 -->
          <el-button
            text
            size="small"
            @click.stop="store.toggleSection(sec.key)"
            :title="sec.visible ? '隐藏此段落' : '显示此段落'"
          >{{ sec.visible ? '👁' : '👁‍🗨' }}</el-button>
          <!-- 删除 -->
          <el-button
            text
            type="danger"
            size="small"
            @click.stop="handleRemove(sec.key)"
            title="删除此段落"
          >✕</el-button>
          <!-- 展开/折叠 -->
          <span class="expand-icon">{{ expandedKeys.has(sec.key) ? '▼' : '▶' }}</span>
        </div>
      </div>

      <!-- 段落内容 -->
      <div v-show="expandedKeys.has(sec.key)" class="section-content">
        <!-- 个人简介 -->
        <SectionSummary
          v-if="sec.type === 'summary' && sec.key === 'summary'"
          :model-value="store.resumeData.summary"
          @update:model-value="store.updateSummary"
        />
        <el-input
          v-else-if="sec.type === 'summary'"
          type="textarea"
          :model-value="store.resumeData[sec.key] || ''"
          @update:model-value="store.resumeData[sec.key] = $event"
          :rows="4"
          placeholder="请输入内容"
          resize="vertical"
        />

        <!-- 技能清单 -->
        <SectionSkills
          v-else-if="sec.type === 'skills' && sec.key === 'skills'"
          :model-value="store.resumeData.skills"
          @update:model-value="updateSkills"
        />
        <SectionSkills
          v-else-if="sec.type === 'skills'"
          :model-value="store.resumeData[sec.key] || []"
          @update:model-value="store.resumeData[sec.key] = $event"
        />

        <!-- 教育经历 -->
        <SectionEducation
          v-else-if="sec.type === 'experience' && sec.key === 'education'"
          :model-value="store.resumeData.education"
          @add-item="addEducation"
          @remove-item="removeEducation"
          @update-item="updateEducation"
        />

        <!-- 工作经历 -->
        <SectionWork
          v-else-if="sec.type === 'experience' && sec.key === 'work'"
          :model-value="store.resumeData.work"
          @add-item="addWork"
          @remove-item="removeWork"
          @update-item="updateWork"
          @add-highlight="addWorkHighlight"
          @remove-highlight="removeWorkHighlight"
          @update-highlight="updateWorkHighlight"
        />

        <!-- 项目经历 -->
        <SectionProject
          v-else-if="sec.type === 'experience' && sec.key === 'projects'"
          :model-value="store.resumeData.projects"
          @add-item="addProject"
          @remove-item="removeProject"
          @update-item="updateProject"
          @add-highlight="addProjectHighlight"
          @remove-highlight="removeProjectHighlight"
          @update-highlight="updateProjectHighlight"
        />

        <!-- 自定义经历段落 -->
        <SectionWork
          v-else-if="sec.type === 'experience'"
          :model-value="store.resumeData[sec.key] || []"
          @add-item="() => addCustomExperience(sec.key)"
          @remove-item="(id) => store.removeExperience(sec.key, id)"
          @update-item="(id, data) => store.updateExperience(sec.key, id, data)"
          @add-highlight="(id) => store.addHighlight(sec.key, id, '')"
          @remove-highlight="(id, i) => store.removeHighlight(sec.key, id, i)"
          @update-highlight="(id, i, val) => store.updateHighlight(sec.key, id, i, val)"
        />

        <!-- 自定义文本段 -->
        <SectionText
          v-else-if="sec.type === 'text'"
          :model-value="store.resumeData[sec.key] || ''"
          @update:model-value="store.resumeData[sec.key] = $event"
        />

        <!-- 证书资质 -->
        <SectionCertifications
          v-else-if="sec.type === 'certifications'"
          :model-value="store.resumeData[sec.key] || []"
          @update:model-value="store.resumeData[sec.key] = $event"
          @add-item="addCertification(sec.key)"
          @remove-item="(id) => removeListItem(sec.key, id)"
        />

        <!-- 获奖荣誉 -->
        <SectionAwards
          v-else-if="sec.type === 'awards'"
          :model-value="store.resumeData[sec.key] || []"
          @update:model-value="store.resumeData[sec.key] = $event"
          @add-item="addAward(sec.key)"
          @remove-item="(id) => removeListItem(sec.key, id)"
        />

        <!-- 语言能力 -->
        <SectionLanguages
          v-else-if="sec.type === 'languages'"
          :model-value="store.resumeData[sec.key] || []"
          @update:model-value="store.resumeData[sec.key] = $event"
        />

        <!-- 兴趣爱好 -->
        <SectionInterests
          v-else-if="sec.type === 'interests'"
          :model-value="store.resumeData[sec.key] || []"
          @update:model-value="store.resumeData[sec.key] = $event"
        />
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useTemplateEditorStore } from '@/stores/templateEditor.js'
import SectionBasicInfo from './editor/SectionBasicInfo.vue'
import SectionSummary from './editor/SectionSummary.vue'
import SectionSkills from './editor/SectionSkills.vue'
import SectionEducation from './editor/SectionEducation.vue'
import SectionWork from './editor/SectionWork.vue'
import SectionProject from './editor/SectionProject.vue'
import SectionText from './editor/SectionText.vue'
import SectionCertifications from './editor/SectionCertifications.vue'
import SectionAwards from './editor/SectionAwards.vue'
import SectionLanguages from './editor/SectionLanguages.vue'
import SectionInterests from './editor/SectionInterests.vue'

const store = useTemplateEditorStore()

// 记录每个section的展开/折叠状态
const expandedKeys = ref(new Set(['summary', 'skills', 'education', 'work', 'projects']))

// 段落标题重命名
const editingKey = ref(null)
const editingTitle = ref('')
const titleInputRef = ref(null)

function startEditTitle(key, currentTitle) {
  editingKey.value = key
  editingTitle.value = currentTitle
  nextTick(() => titleInputRef.value?.focus())
}

function saveTitle(key) {
  const title = editingTitle.value.trim()
  if (title) {
    store.updateSectionTitle(key, title)
  }
  editingKey.value = null
}

function cancelEditTitle() {
  editingKey.value = null
}

function toggleExpand(key) {
  if (expandedKeys.value.has(key)) {
    expandedKeys.value.delete(key)
  } else {
    expandedKeys.value.add(key)
  }
}

// 基础信息
function updateBasic(val) {
  store.resumeData.basic = val
}

// 技能
function updateSkills(val) {
  store.resumeData.skills = val
}

// 删除段落（带确认）
async function handleRemove(key) {
  try {
    await ElMessageBox.confirm(
      '确定删除此段落吗？删除后可从"添加段落"重新添加。',
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    store.removeSection(key)
    expandedKeys.value.delete(key)
  } catch {
    // 用户取消，不做任何操作
  }
}

// 添加段落（弹出自定义标题输入框）
async function handleAddSection(typeConfig) {
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入段落标题：',
      '添加段落',
      {
        confirmButtonText: '添加',
        cancelButtonText: '取消',
        inputValue: typeConfig.defaultTitle || typeConfig.label,
        inputValidator: (val) => val && val.trim() ? true : '段落标题不能为空'
      }
    )
    const finalTitle = value.trim() || typeConfig.defaultTitle || typeConfig.label
    store.addSection(typeConfig, finalTitle)
    // 自动展开新添加的段落
    const newSec = store.sectionsConfig[store.sectionsConfig.length - 1]
    if (newSec) expandedKeys.value.add(newSec.key)
  } catch {
    // 用户取消，不做任何操作
  }
}

// 教育经历
function addEducation() {
  store.addExperience('education', {
    id: `edu-${Date.now()}`, school: '', degree: '', major: '',
    startDate: '', endDate: '', description: ''
  })
}
function removeEducation(id) { store.removeExperience('education', id) }
function updateEducation(id, data) { store.updateExperience('education', id, data) }

// 工作经历
function addWork() {
  store.addExperience('work', {
    id: `work-${Date.now()}`, company: '', position: '',
    startDate: '', endDate: '', highlights: []
  })
}
function removeWork(id) { store.removeExperience('work', id) }
function updateWork(id, data) { store.updateExperience('work', id, data) }
function addWorkHighlight(id) { store.addHighlight('work', id, '') }
function removeWorkHighlight(id, index) { store.removeHighlight('work', id, index) }
function updateWorkHighlight(id, index, val) { store.updateHighlight('work', id, index, val) }

// 项目经历
function addProject() {
  store.addExperience('projects', {
    id: `proj-${Date.now()}`, name: '', role: '',
    startDate: '', endDate: '', description: '', highlights: []
  })
}
function removeProject(id) { store.removeExperience('projects', id) }
function updateProject(id, data) { store.updateExperience('projects', id, data) }
function addProjectHighlight(id) { store.addHighlight('projects', id, '') }
function removeProjectHighlight(id, index) { store.removeHighlight('projects', id, index) }
function updateProjectHighlight(id, index, val) { store.updateHighlight('projects', id, index, val) }

// 自定义经历段落
function addCustomExperience(key) {
  store.addExperience(key, {
    id: `${key}-${Date.now()}`, company: '', position: '',
    startDate: '', endDate: '', highlights: []
  })
}

// 证书资质
function addCertification(key) {
  if (!store.resumeData[key]) store.resumeData[key] = []
  store.resumeData[key].push({ id: `cert-${Date.now()}`, name: '', issuer: '', date: '' })
}

// 获奖荣誉
function addAward(key) {
  if (!store.resumeData[key]) store.resumeData[key] = []
  store.resumeData[key].push({ id: `award-${Date.now()}`, name: '', issuer: '', date: '' })
}

// 通用列表项删除
function removeListItem(key, id) {
  const list = store.resumeData[key]
  if (!list) return
  const idx = list.findIndex(item => item.id === id)
  if (idx !== -1) list.splice(idx, 1)
}
</script>

<style scoped>
.resume-editor {
  padding: 16px;
  overflow-y: auto;
  height: 100%;
}

.editor-section {
  border: 1px solid var(--border-card);
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 12px;
}

.editor-section.section-hidden {
  opacity: 0.5;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: var(--bg-card-hover, #f5f7fa);
  cursor: pointer;
  user-select: none;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.drag-handle {
  color: var(--text-muted, #909399);
  cursor: grab;
  font-size: 14px;
}

.section-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-title, #303133);
}

.hidden-badge {
  font-size: 11px;
  color: var(--text-muted, #909399);
  background: #f0f0f0;
  padding: 1px 6px;
  border-radius: 4px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 2px;
}

.expand-icon {
  font-size: 11px;
  color: var(--text-muted, #909399);
  margin-left: 4px;
}

.section-content {
  background: var(--bg-card, #fff);
}

/* 隐藏内部section组件的重复边框 */
.section-content :deep(.section-summary),
.section-content :deep(.section-skills),
.section-content :deep(.section-education),
.section-content :deep(.section-work),
.section-content :deep(.section-project) {
  border: none;
  margin-bottom: 0;
}

.section-content :deep(.section-header) {
  display: none;
}

.add-section-area {
  margin-bottom: 12px;
  padding: 0 4px;
}

.title-edit-input {
  width: 140px;
}
</style>
