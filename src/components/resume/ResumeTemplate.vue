<template>
  <div ref="resumeRef" :class="['resume-template', `resume-template--${mode}`]">
    <div v-if="isPreview" class="editor-toolbar">
      <button type="button" class="editor-tool" :disabled="!canUndo" @mousedown.prevent @click="undoTemplateChange">
        上一步
      </button>
      <button type="button" class="editor-tool" :disabled="!canRedo" @mousedown.prevent @click="redoTemplateChange">
        下一步
      </button>
      <span class="toolbar-separator"></span>
      <button type="button" class="editor-tool" :disabled="!hasActiveEditableTarget" @mousedown.prevent @click="toggleBold">
        B
      </button>
      <button type="button" class="editor-tool" :disabled="!hasActiveEditableTarget" @mousedown.prevent @click="decreaseFontSize">
        A-
      </button>
      <button type="button" class="editor-tool" :disabled="!hasActiveEditableTarget" @mousedown.prevent @click="increaseFontSize">
        A+
      </button>
      <span class="toolbar-separator"></span>
      <button type="button" class="editor-tool" @mousedown.prevent @click="toggleLabelStyleAtCurrent">标签样式</button>
      <button type="button" class="editor-tool" @mousedown.prevent @click="toggleSectionTitleAtCurrent">章节标题</button>
      <button type="button" class="editor-tool" :disabled="!hasActiveEditableTarget" @mousedown.prevent @click="toggleBulletAtCurrent">列表样式</button>
      <button type="button" class="editor-tool" :disabled="!hasActiveEditableTarget" @mousedown.prevent @click="deleteCurrentTarget">
        删除段落
      </button>
      <button type="button" class="editor-tool" :disabled="!hasActiveEditableTarget" @mousedown.prevent @click="resetCurrentStyle">
        重置
      </button>
    </div>

    <article class="resume-paper">
      <section class="resume-section resume-section--profile">
        <div class="resume-section-head">
          <div class="section-tab">
            <span class="section-tab-mark"></span>
            <input
              v-model="header.sectionTitle"
              class="section-title-input resume-inline-input"
              data-export-display="inline"
              :readonly="!isPreview"
              placeholder="个人信息"
            />
          </div>
          <div class="section-line"></div>
        </div>

        <div class="profile-card">
          <div class="profile-main">
            <div class="profile-name-row">
              <ResumeInlineRichEditor
                :ref="(instance) => setHeaderFieldRef(header.name.id, instance)"
                :field="header.name"
                :mode="mode"
                placeholder="请输入姓名"
                :class="['profile-name-input', { 'is-active': isActiveHeaderField(header.name.id) }]"
                @focus="activateHeaderField(header.name.id, 'name')"
                @update-html="updateHeaderFieldHtml"
                @request-remove-empty="handleHeaderFieldEmptyDelete"
              />
            </div>

            <div class="profile-meta-grid">
              <ResumeInlineRichEditor
                :ref="(instance) => setHeaderFieldRef(header.jobTarget.id, instance)"
                :field="header.jobTarget"
                :mode="mode"
                placeholder="请输入求职方向"
                :class="['profile-target-input', 'profile-meta-item--wide', { 'is-active': isActiveHeaderField(header.jobTarget.id) }]"
                @focus="activateHeaderField(header.jobTarget.id, 'job_target')"
                @update-html="updateHeaderFieldHtml"
                @request-remove-empty="handleHeaderFieldEmptyDelete"
              />

              <div
                v-for="item in header.metaItems"
                :key="item.id"
                :class="['profile-meta-card', { 'is-active': isActiveHeaderField(item.id) }]"
                :draggable="isPreview"
                @dragstart="handleMetaDragStart(item.id, $event)"
                @dragover.prevent="handleMetaDragOver($event)"
                @drop.prevent="handleMetaDrop(item.id)"
                @dragend="resetMetaDragState"
              >
                <button v-if="isPreview" type="button" class="drag-handle drag-handle--meta" draggable="true" @mousedown.stop>
                  ⋮⋮
                </button>
                <ResumeInlineRichEditor
                  :ref="(instance) => setHeaderFieldRef(item.id, instance)"
                  :field="item"
                  :mode="mode"
                  placeholder="请输入联系方式"
                  class="profile-meta-input"
                  @focus="activateHeaderField(item.id, 'meta')"
                  @update-html="updateHeaderFieldHtml"
                  @request-remove-empty="handleHeaderFieldEmptyDelete"
                />
                <button v-if="isPreview" type="button" class="meta-remove-btn editor-ghost-btn" @click="removeMetaItem(item.id)">
                  删除
                </button>
              </div>
            </div>

            <div v-if="isPreview" class="profile-meta-tools">
              <button type="button" class="editor-ghost-btn" @click="addMetaItem">新增信息项</button>
              <button type="button" class="editor-ghost-btn" @click="addSummaryLine">新增补充说明</button>
            </div>

            <div v-if="header.summaryLines.length" class="profile-summary">
              <div
                v-for="item in header.summaryLines"
                :key="item.id"
                :class="['profile-summary-item', { 'is-active': isActiveHeaderField(item.id) }]"
              >
                <ResumeInlineRichEditor
                  :ref="(instance) => setHeaderFieldRef(item.id, instance)"
                  :field="item"
                  :mode="mode"
                  multiline
                  placeholder="请输入补充说明"
                  class="profile-summary-input"
                  @focus="activateHeaderField(item.id, 'summary')"
                  @update-html="updateHeaderFieldHtml"
                  @request-remove-empty="handleHeaderFieldEmptyDelete"
                />
                <button v-if="isPreview" type="button" class="summary-remove-btn editor-ghost-btn" @click="removeSummaryLine(item.id)">
                  删除
                </button>
              </div>
            </div>
          </div>

          <div class="profile-photo">
            <input
              v-if="isPreview"
              ref="photoInputRef"
              class="photo-input"
              type="file"
              accept="image/png,image/jpeg,image/webp"
              @change="handlePhotoChange"
            />
            <button
              v-if="isPreview"
              type="button"
              class="photo-frame photo-frame--button"
              @click="triggerPhotoUpload"
            >
              <img v-if="photoDataUrl" :src="photoDataUrl" alt="简历照片" class="photo-image" />
              <span v-else class="photo-placeholder">点击上传照片</span>
            </button>
            <div v-else class="photo-frame">
              <img v-if="photoDataUrl" :src="photoDataUrl" alt="简历照片" class="photo-image" />
              <span v-else class="photo-placeholder">照片预留区</span>
            </div>

            <div v-if="isPreview" class="photo-actions">
              <button type="button" class="photo-action" @click="triggerPhotoUpload">
                {{ photoDataUrl ? '更换照片' : '上传照片' }}
              </button>
              <button v-if="photoDataUrl" type="button" class="photo-action photo-action--ghost" @click="clearPhoto">
                清空照片
              </button>
            </div>
            <p class="photo-tip">{{ photoDataUrl ? '导出时将保留当前照片' : '照片预留区' }}</p>
          </div>
        </div>
      </section>

      <main class="resume-main">
        <section
          v-for="section in sections"
          :key="section.id"
          :class="['resume-section', `resume-section--${section.key}`]"
        >
          <div class="resume-section-head">
            <div class="section-tab">
              <span class="section-tab-mark"></span>
              <input
                v-model="section.title"
                class="section-title-input resume-inline-input"
                data-export-display="inline"
                :readonly="!isPreview"
              />
            </div>
            <div class="section-line"></div>
          </div>

          <div class="resume-section-body">
            <div
              v-for="block in section.blocks"
              :key="block.id"
              :class="['resume-block-shell', { 'is-active': activeBlockId === block.id }]"
            >
              <div
                v-if="isPreview"
                class="block-drop-indicator"
                :class="{ 'is-visible': isDragOver(section.id, block.id, 'before') }"
                @dragover.prevent="setDragOver(section.id, block.id, 'before', $event)"
                @dragleave="clearDragOver"
                @drop.prevent="handleBlockDrop(section.id, block.id, 'before')"
              ></div>

              <div class="resume-block" :data-block-id="block.id" @click="activateBlock(block.id)">
                <button
                  v-if="isPreview"
                  type="button"
                  class="drag-handle drag-handle--block"
                  draggable="true"
                  @dragstart="handleBlockDragStart(block.id, $event)"
                  @dragend="resetDragState"
                  @mousedown.stop
                >
                  ⋮⋮
                </button>

                <template v-if="isRichTextBlock(block)">
                  <div :class="resolveTextBlockClass(block)">
                    <ResumeRichBlockEditor
                      :ref="(instance) => setRichBlockRef(block.id, instance)"
                      :block="block"
                      :mode="mode"
                      @focus="activateBlock"
                      @update-html="updateBlockHtml"
                      @request-insert-after="insertTextBlockAfter"
                      @request-remove-empty="removeEmptyBlock"
                    />
                  </div>
                </template>

                <div
                  v-else-if="block.type === 'banner_title'"
                  class="resume-section-head resume-section-head--block"
                  :style="buildBlockInlineStyle(block)"
                >
                  <div class="section-tab">
                    <span class="section-tab-mark"></span>
                    <input
                      v-model="block.title"
                      class="section-title-input resume-inline-input"
                      :style="buildBlockInlineStyle(block)"
                      data-export-display="inline"
                      :readonly="!isPreview"
                      @focus="activateBlock(block.id)"
                      @keydown.enter.prevent="insertTextBlockAfter(block.id)"
                    />
                  </div>
                  <div class="section-line"></div>
                </div>

                <div v-else-if="block.type === 'label'" class="label-line" :style="buildBlockInlineStyle(block)">
                  <input
                    v-model="block.label"
                    class="label-key-input resume-inline-input"
                    data-export-display="inline"
                    :readonly="!isPreview"
                    @focus="activateBlock(block.id)"
                    @keydown.enter.prevent="insertTextBlockAfter(block.id)"
                  />
                  <input
                    v-model="block.value"
                    class="label-value-input resume-inline-input"
                    data-export-display="inline"
                    :readonly="!isPreview"
                    @focus="activateBlock(block.id)"
                    @keydown.enter.prevent="insertTextBlockAfter(block.id)"
                  />
                </div>

                <div
                  v-else-if="block.type === 'row'"
                  :class="['entry-row', `entry-row--${block.rowKind || 'default'}`]"
                  :style="buildBlockInlineStyle(block)"
                >
                  <input
                    v-for="(item, itemIndex) in block.items"
                    :key="item.id"
                    v-model="item.value"
                    :class="['entry-cell-input', `entry-cell--${resolveCellRole(itemIndex, block.items.length)}`, 'resume-inline-input']"
                    data-export-display="inline"
                    :readonly="!isPreview"
                    @focus="activateBlock(block.id)"
                    @keydown.enter.prevent="insertTextBlockAfter(block.id)"
                  />
                </div>
              </div>

              <div
                v-if="isPreview"
                class="block-drop-indicator"
                :class="{ 'is-visible': isDragOver(section.id, block.id, 'after') }"
                @dragover.prevent="setDragOver(section.id, block.id, 'after', $event)"
                @dragleave="clearDragOver"
                @drop.prevent="handleBlockDrop(section.id, block.id, 'after')"
              ></div>
            </div>

            <div
              v-if="isPreview"
              class="section-drop-tail"
              :class="{ 'is-visible': isDragOver(section.id, null, 'end') }"
              @dragover.prevent="setDragOver(section.id, null, 'end', $event)"
              @dragleave="clearDragOver"
              @drop.prevent="handleBlockDrop(section.id, null, 'end')"
            >
              拖到这里放到本节末尾
            </div>
          </div>
        </section>
      </main>
    </article>
  </div>
</template>

<script setup>
import DOMPurify from 'dompurify'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import ResumeInlineRichEditor from './ResumeInlineRichEditor.vue'
import ResumeRichBlockEditor from './ResumeRichBlockEditor.vue'
import {
  buildResumeTemplateModel,
  createEmptyLabelBlock,
  createEmptyTextBlock,
} from './resumeTemplateParser'

const props = defineProps({
  text: {
    type: String,
    default: '',
  },
  mode: {
    type: String,
    default: 'preview',
    validator: (value) => ['preview', 'print'].includes(value),
  },
})

const isPreview = computed(() => props.mode === 'preview')
const resumeRef = ref(null)
const photoInputRef = ref(null)
const photoDataUrl = ref('')
const header = ref(createEmptyHeaderModel())
const sections = ref([])
const activeTarget = ref(createEmptyActiveTarget())
const draggingBlockId = ref('')
const metaDraggingId = ref('')
const dragOverState = ref(createEmptyDragState())
const historyState = ref({
  past: [],
  future: [],
})
const suspendHistory = ref(false)
const historyTimer = ref(null)
const richBlockRefs = new Map()
const headerFieldRefs = new Map()

function createEmptyActiveTarget() {
  return {
    type: '',
    id: '',
    fieldKind: '',
  }
}

function createEmptyDragState() {
  return {
    sectionId: '',
    blockId: '',
    position: '',
  }
}

function createStyleModel() {
  return {
    fontSize: null,
    fontWeight: null,
  }
}

function createEmptyHeaderModel() {
  return {
    sectionTitle: '个人信息',
    name: createHeaderField({ kind: 'name', placeholder: '请输入姓名' }),
    jobTarget: createHeaderField({ kind: 'job_target', placeholder: '请输入求职方向' }),
    metaItems: [],
    summaryLines: [],
  }
}

function cloneModel(value) {
  return JSON.parse(JSON.stringify(value))
}

function generateClientId(prefix) {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

function escapeHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function wrapTextAsHtml(value) {
  return `<p>${escapeHtml(value).replace(/\n/g, '<br>')}</p>`
}

function createHeaderField({
  id,
  kind,
  html = '<p></p>',
  style = null,
  placeholder = '',
}) {
  return {
    id: id || generateClientId(`header_${kind}`),
    kind,
    html,
    style: {
      ...createStyleModel(),
      ...(style || {}),
    },
    placeholder,
  }
}

function normalizeHeaderField(source, kind, placeholder) {
  if (source?.html !== undefined) {
    return createHeaderField({
      id: source.id,
      kind: source.kind || kind,
      html: source.html || '<p></p>',
      style: source.style,
      placeholder: source.placeholder || placeholder,
    })
  }

  const rawText = typeof source === 'string' ? source : source?.value || ''
  return createHeaderField({
    id: source?.id,
    kind,
    html: rawText ? wrapTextAsHtml(rawText) : '<p></p>',
    style: source?.style,
    placeholder,
  })
}

function normalizeHeaderModel(source) {
  return {
    sectionTitle: source?.sectionTitle || '个人信息',
    name: normalizeHeaderField(source?.name, 'name', '请输入姓名'),
    jobTarget: normalizeHeaderField(source?.jobTarget, 'job_target', '请输入求职方向'),
    metaItems: (source?.metaItems || []).map((item) => normalizeHeaderField(item, 'meta', '请输入联系方式')),
    summaryLines: (source?.summaryLines || []).map((item) => normalizeHeaderField(item, 'summary', '请输入补充说明')),
  }
}

function normalizeSectionsModel(sourceSections) {
  return (sourceSections || []).map((section) => ({
    ...section,
    blocks: (section.blocks || []).map((block) => ({
      ...block,
      style: {
        ...createStyleModel(),
        ...(block.style || {}),
      },
      items: block.items ? block.items.map((item) => ({ ...item })) : undefined,
    })),
  }))
}

function createTextBlockFromText(text, variant = '', style = null) {
  return {
    id: generateClientId('resume_block_text'),
    type: 'text',
    variant,
    html: wrapTextAsHtml(text || ''),
    style: {
      ...createStyleModel(),
      ...(style || {}),
    },
  }
}

/**
 * 胶囊章节标题块属于前端模板编辑器专用块类型。
 * 它只影响当前预览与导出效果，不修改后端 AI 返回的纯文本结构。
 */
function createBannerTitleBlock(title = '章节标题', style = null) {
  return {
    id: generateClientId('resume_block_banner'),
    type: 'banner_title',
    title,
    style: {
      ...createStyleModel(),
      ...(style || {}),
    },
  }
}

function createLabelBlock(label = '标签：', value = '请输入内容', style = null) {
  return {
    id: generateClientId('resume_block_label'),
    type: 'label',
    label,
    value,
    style: {
      ...createStyleModel(),
      ...(style || {}),
    },
  }
}

const activeBlockId = computed(() => {
  return activeTarget.value.type === 'block' ? activeTarget.value.id : ''
})

const hasActiveEditableTarget = computed(() => {
  return !!activeTarget.value.id
})

const canUndo = computed(() => {
  return historyState.value.past.length > 1
})

const canRedo = computed(() => {
  return historyState.value.future.length > 0
})

function setRichBlockRef(blockId, instance) {
  if (instance) {
    richBlockRefs.set(blockId, instance)
    return
  }
  richBlockRefs.delete(blockId)
}

function setHeaderFieldRef(fieldId, instance) {
  if (instance) {
    headerFieldRefs.set(fieldId, instance)
    return
  }
  headerFieldRefs.delete(fieldId)
}

function resolveCellRole(index, total) {
  if (total === 1 || index === 0) {
    return 'left'
  }
  if (index === total - 1) {
    return 'right'
  }
  return 'middle'
}

function isRichTextBlock(block) {
  return block.type === 'text' || block.type === 'section_title'
}

function resolveTextBlockClass(block) {
  return [
    block.type === 'section_title' ? 'subsection-line' : 'text-line',
    block.variant ? `text-line--${block.variant}` : '',
  ]
}

function buildBlockInlineStyle(block) {
  return {
    fontSize: block.style?.fontSize ? `${block.style.fontSize}px` : undefined,
    fontWeight: block.style?.fontWeight || undefined,
  }
}

function activateBlock(blockId) {
  activeTarget.value = {
    type: 'block',
    id: blockId,
    fieldKind: '',
  }
}

function activateHeaderField(fieldId, fieldKind) {
  activeTarget.value = {
    type: 'header_field',
    id: fieldId,
    fieldKind,
  }
}

function isActiveHeaderField(fieldId) {
  return activeTarget.value.type === 'header_field' && activeTarget.value.id === fieldId
}

function findBlockLocation(blockId) {
  for (let sectionIndex = 0; sectionIndex < sections.value.length; sectionIndex += 1) {
    const section = sections.value[sectionIndex]
    const blockIndex = section.blocks.findIndex((block) => block.id === blockId)
    if (blockIndex !== -1) {
      return {
        sectionIndex,
        section,
        blockIndex,
        block: section.blocks[blockIndex],
      }
    }
  }
  return null
}

function findHeaderFieldById(fieldId) {
  if (header.value.name.id === fieldId) {
    return {
      type: 'direct',
      kind: 'name',
      field: header.value.name,
      parent: header.value,
      key: 'name',
    }
  }

  if (header.value.jobTarget.id === fieldId) {
    return {
      type: 'direct',
      kind: 'job_target',
      field: header.value.jobTarget,
      parent: header.value,
      key: 'jobTarget',
    }
  }

  const metaIndex = header.value.metaItems.findIndex((item) => item.id === fieldId)
  if (metaIndex !== -1) {
    return {
      type: 'array',
      kind: 'meta',
      field: header.value.metaItems[metaIndex],
      parent: header.value.metaItems,
      index: metaIndex,
    }
  }

  const summaryIndex = header.value.summaryLines.findIndex((item) => item.id === fieldId)
  if (summaryIndex !== -1) {
    return {
      type: 'array',
      kind: 'summary',
      field: header.value.summaryLines[summaryIndex],
      parent: header.value.summaryLines,
      index: summaryIndex,
    }
  }

  return null
}

function getActiveBlock() {
  return activeTarget.value.type === 'block' ? findBlockLocation(activeTarget.value.id)?.block || null : null
}

/** 根据 blockId 查找所属 section 的 key */
function findSectionKeyByBlockId(blockId) {
  for (const section of sections.value) {
    if (section.blocks.some(b => b.id === blockId)) {
      return section.key
    }
  }
  return ''
}

function getActiveHeaderFieldLocation() {
  return activeTarget.value.type === 'header_field' ? findHeaderFieldById(activeTarget.value.id) : null
}

async function focusBlock(blockId) {
  activateBlock(blockId)
  await nextTick()

  const richEditor = richBlockRefs.get(blockId)
  if (richEditor?.focusEditor) {
    richEditor.focusEditor()
    return
  }

  const plainInput = resumeRef.value?.querySelector(`[data-block-id="${blockId}"] input, [data-block-id="${blockId}"] textarea`)
  plainInput?.focus()
}

async function focusHeaderField(fieldId, fieldKind) {
  activateHeaderField(fieldId, fieldKind)
  await nextTick()
  headerFieldRefs.get(fieldId)?.focusEditor?.()
}

function blurCurrentTarget() {
  if (activeTarget.value.type === 'header_field') {
    headerFieldRefs.get(activeTarget.value.id)?.blurEditor?.()
    return
  }

  if (activeTarget.value.type === 'block') {
    richBlockRefs.get(activeTarget.value.id)?.blurEditor?.()
    const plainInput = resumeRef.value?.querySelector(
      `[data-block-id="${activeTarget.value.id}"] input, [data-block-id="${activeTarget.value.id}"] textarea`,
    )
    plainInput?.blur()
  }
}

/**
 * 点击模板外空白区域时，需要同时清理正文块和头部字段的激活态，
 * 避免导出前仍残留工具栏关联的焦点样式。
 */
function clearActiveState() {
  blurCurrentTarget()
  activeTarget.value = createEmptyActiveTarget()
}

function handleDocumentPointerDown(event) {
  if (!resumeRef.value?.contains(event.target)) {
    clearActiveState()
  }
}

/**
 * 简历富文本允许保留基础排版标签和内联样式，但必须先净化再做 DOM 解析，
 * 避免历史数据或外部导入内容把脚本、事件属性等危险节点带进编辑器链路。
 */
function sanitizeRichTextHtml(html) {
  return DOMPurify.sanitize(String(html || ''), {
    ALLOWED_TAGS: ['p', 'br', 'strong', 'b', 'em', 'i', 'u', 'span', 'div', 'ul', 'ol', 'li'],
    ALLOWED_ATTR: ['style'],
    FORBID_TAGS: ['script', 'iframe', 'object', 'embed'],
  })
}

function stripHtmlToText(html) {
  if (!html) {
    return ''
  }

  const wrapper = document.createElement('div')
  wrapper.innerHTML = sanitizeRichTextHtml(html)
  wrapper.querySelectorAll('br').forEach((node) => {
    node.replaceWith('\n')
  })
  return wrapper.textContent?.replace(/\u00a0/g, ' ').trim() || ''
}

function readHeaderFieldText(field) {
  return stripHtmlToText(field?.html || '')
}

function isSingleLineRichText(block) {
  const text = stripHtmlToText(block?.html || '')
  if (!text) {
    return false
  }
  return !text.includes('\n')
}

function parseLabelText(text) {
  const normalized = String(text || '').trim()
  const match = normalized.match(/^([^:：]{1,20}[:：])\s*(.+)$/)
  if (match) {
    return {
      label: match[1],
      value: match[2],
    }
  }

  return {
    label: '标签：',
    value: normalized,
  }
}

function buildLabelPlainText(block) {
  return `${block.label || ''}${block.value || ''}`.trim()
}

function sanitizeIncomingResumeText(text) {
  return String(text || '')
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .replace(/^\s*(AI润色简历|个人简历|求职简历|简历)\s*\n/u, '')
}

function applyTemplateText(text) {
  const model = cloneModel(buildResumeTemplateModel(sanitizeIncomingResumeText(text)))
  suspendHistory.value = true
  header.value = normalizeHeaderModel(model.header)
  sections.value = normalizeSectionsModel(model.sections)
  photoDataUrl.value = ''
  activeTarget.value = createEmptyActiveTarget()
  resetDragState()
  resetMetaDragState()
  suspendHistory.value = false
  initializeHistory()
}

function createSnapshot() {
  return {
    header: cloneModel(header.value),
    sections: cloneModel(sections.value),
    photoDataUrl: photoDataUrl.value,
    activeTarget: cloneModel(activeTarget.value),
  }
}

function createSnapshotRecord() {
  const snapshot = createSnapshot()
  return {
    signature: JSON.stringify(snapshot),
    snapshot,
  }
}

function initializeHistory() {
  historyState.value = {
    past: [createSnapshotRecord()],
    future: [],
  }
}

/**
 * 结构性操作立即入历史栈，文本输入走节流快照。
 * 这样既能保证撤销/重做覆盖整份模板，又不会因为每个字符输入都生成一条历史记录。
 */
function recordHistoryNow() {
  if (!isPreview.value || suspendHistory.value) {
    return
  }

  if (historyTimer.value) {
    clearTimeout(historyTimer.value)
    historyTimer.value = null
  }

  const record = createSnapshotRecord()
  const lastRecord = historyState.value.past[historyState.value.past.length - 1]
  if (lastRecord?.signature === record.signature) {
    return
  }

  historyState.value.past.push(record)
  if (historyState.value.past.length > 80) {
    historyState.value.past.shift()
  }
  historyState.value.future = []
}

function queueHistorySnapshot() {
  if (!isPreview.value || suspendHistory.value) {
    return
  }

  if (historyTimer.value) {
    clearTimeout(historyTimer.value)
  }

  historyTimer.value = setTimeout(() => {
    historyTimer.value = null
    recordHistoryNow()
  }, 280)
}

async function restoreSnapshot(snapshot) {
  suspendHistory.value = true
  header.value = normalizeHeaderModel(snapshot.header)
  sections.value = normalizeSectionsModel(snapshot.sections)
  photoDataUrl.value = snapshot.photoDataUrl || ''
  activeTarget.value = snapshot.activeTarget || createEmptyActiveTarget()
  resetDragState()
  resetMetaDragState()
  await nextTick()

  if (activeTarget.value.type === 'header_field') {
    const location = findHeaderFieldById(activeTarget.value.id)
    if (location) {
      headerFieldRefs.get(location.field.id)?.focusEditor?.()
    }
  } else if (activeTarget.value.type === 'block') {
    const location = findBlockLocation(activeTarget.value.id)
    if (location) {
      const richEditor = richBlockRefs.get(location.block.id)
      if (richEditor?.focusEditor) {
        richEditor.focusEditor()
      } else {
        const plainInput = resumeRef.value?.querySelector(
          `[data-block-id="${location.block.id}"] input, [data-block-id="${location.block.id}"] textarea`,
        )
        plainInput?.focus()
      }
    }
  }

  suspendHistory.value = false
}

async function undoTemplateChange() {
  if (!isPreview.value) {
    return
  }

  recordHistoryNow()
  if (historyState.value.past.length <= 1) {
    return
  }

  const current = historyState.value.past.pop()
  historyState.value.future.push(current)
  await restoreSnapshot(historyState.value.past[historyState.value.past.length - 1].snapshot)
}

async function redoTemplateChange() {
  if (!isPreview.value || !historyState.value.future.length) {
    return
  }

  recordHistoryNow()
  const nextRecord = historyState.value.future.pop()
  historyState.value.past.push(nextRecord)
  await restoreSnapshot(nextRecord.snapshot)
}

watch(
  () => props.text,
  (nextText) => {
    applyTemplateText(nextText)
  },
  { immediate: true },
)

watch([header, sections, photoDataUrl], () => {
  queueHistorySnapshot()
}, { deep: true })

onMounted(() => {
  document.addEventListener('mousedown', handleDocumentPointerDown)
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleDocumentPointerDown)
  if (historyTimer.value) {
    clearTimeout(historyTimer.value)
  }
})

function updateBlockHtml({ id, html }) {
  const location = findBlockLocation(id)
  if (!location) {
    return
  }
  location.block.html = html
}

function updateHeaderFieldHtml({ id, html }) {
  const location = findHeaderFieldById(id)
  if (!location) {
    return
  }
  location.field.html = html
}

async function insertBlockAfter(currentBlockId, nextBlock) {
  const location = findBlockLocation(currentBlockId)
  if (!location) {
    const firstSection = sections.value[0]
    if (!firstSection) {
      return
    }
    firstSection.blocks.push(nextBlock)
    await focusBlock(nextBlock.id)
    recordHistoryNow()
    return
  }

  location.section.blocks.splice(location.blockIndex + 1, 0, nextBlock)
  await focusBlock(nextBlock.id)
  recordHistoryNow()
}

/**
 * 正文回车仍然保持“在当前块下方新建段落”的 Word 风格，
 * 新块插入后立即切换焦点，避免出现用户看见新增但无法继续输入的问题。
 */
async function insertTextBlockAfter(currentBlockId) {
  const sectionKey = findSectionKeyByBlockId(currentBlockId)
  const noBulletSections = ['education', 'profile']
  const variant = noBulletSections.includes(sectionKey) ? '' : 'bullet'
  await insertBlockAfter(currentBlockId, createTextBlockFromText('', variant))
}

async function replaceBlock(blockId, nextBlock) {
  const location = findBlockLocation(blockId)
  if (!location) {
    return
  }

  location.section.blocks.splice(location.blockIndex, 1, nextBlock)
  await focusBlock(nextBlock.id)
  recordHistoryNow()
}

function canToggleBlockToLabel(block) {
  return (block.type === 'text' || block.type === 'section_title') && isSingleLineRichText(block)
}

function canToggleBlockToBanner(block) {
  return (block.type === 'text' || block.type === 'section_title') && isSingleLineRichText(block)
}

async function toggleLabelStyleAtCurrent() {
  const activeBlock = getActiveBlock()

  if (activeBlock?.type === 'label') {
    await replaceBlock(activeBlock.id, createTextBlockFromText(buildLabelPlainText(activeBlock), '', activeBlock.style))
    return
  }

  if (activeBlock && canToggleBlockToLabel(activeBlock)) {
    const parsed = parseLabelText(stripHtmlToText(activeBlock.html))
    await replaceBlock(activeBlock.id, createLabelBlock(parsed.label, parsed.value, activeBlock.style))
    return
  }

  const fallbackId = activeBlockId.value || sections.value[0]?.blocks[sections.value[0].blocks.length - 1]?.id
  if (!fallbackId) {
    return
  }
  await insertBlockAfter(fallbackId, createEmptyLabelBlock())
}

async function toggleSectionTitleAtCurrent() {
  const activeBlock = getActiveBlock()

  if (activeBlock?.type === 'banner_title') {
    await replaceBlock(activeBlock.id, createTextBlockFromText(activeBlock.title || '', 'heading', activeBlock.style))
    return
  }

  if (activeBlock && canToggleBlockToBanner(activeBlock)) {
    const title = stripHtmlToText(activeBlock.html)
    await replaceBlock(activeBlock.id, createBannerTitleBlock(title || '章节标题', activeBlock.style))
    return
  }

  const fallbackId = activeBlockId.value || sections.value[0]?.blocks[sections.value[0].blocks.length - 1]?.id
  if (!fallbackId) {
    return
  }
  await insertBlockAfter(fallbackId, createBannerTitleBlock())
}

/** 切换当前段落的 bullet（小圆点）样式 */
async function toggleBulletAtCurrent() {
  const activeBlock = getActiveBlock()
  if (!activeBlock || activeBlock.type !== 'text') return
  const newVariant = activeBlock.variant === 'bullet' ? '' : 'bullet'
  await replaceBlock(activeBlock.id, createTextBlockFromText(
    stripHtmlToText(activeBlock.html), newVariant, activeBlock.style
  ))
}

function removeBlockById(blockId) {
  const location = findBlockLocation(blockId)
  if (!location) {
    return null
  }

  location.section.blocks.splice(location.blockIndex, 1)
  return {
    nextBlock:
      location.section.blocks[location.blockIndex] ||
      location.section.blocks[location.blockIndex - 1] ||
      null,
  }
}

async function removeBlockAndFocus(blockId) {
  const result = removeBlockById(blockId)
  if (!result) {
    return
  }

  if (result.nextBlock) {
    await focusBlock(result.nextBlock.id)
  } else {
    clearActiveState()
  }

  recordHistoryNow()
}

async function removeEmptyBlock(blockId) {
  await removeBlockAndFocus(blockId)
}

async function handleHeaderFieldEmptyDelete(fieldId) {
  const location = findHeaderFieldById(fieldId)
  if (!location) {
    return
  }

  if (location.kind === 'meta') {
    await removeMetaItem(fieldId)
    return
  }

  if (location.kind === 'summary') {
    await removeSummaryLine(fieldId)
  }
}

async function deleteCurrentTarget() {
  if (!hasActiveEditableTarget.value) {
    ElMessage.warning('请先选择要删除的内容')
    return
  }

  if (activeTarget.value.type === 'block') {
    await removeBlockAndFocus(activeTarget.value.id)
    return
  }

  const location = getActiveHeaderFieldLocation()
  if (!location) {
    return
  }

  if (location.kind === 'meta') {
    await removeMetaItem(location.field.id)
    return
  }

  if (location.kind === 'summary') {
    await removeSummaryLine(location.field.id)
    return
  }

  location.field.html = '<p></p>'
  await focusHeaderField(location.field.id, location.kind)
  recordHistoryNow()
}

function getHeaderFieldBaseSize(field) {
  switch (field.kind) {
    case 'name':
      return 38
    case 'job_target':
      return 13
    case 'meta':
      return 14
    case 'summary':
      return 14
    default:
      return 14
  }
}

function getHeaderFieldBaseWeight(field) {
  switch (field.kind) {
    case 'name':
      return 800
    case 'job_target':
      return 600
    default:
      return 400
  }
}

function getBlockBaseSize(block) {
  if (block.type === 'banner_title') {
    return 17
  }
  if (block.type === 'section_title' || block.variant === 'heading') {
    return 15
  }
  return 14
}

function getBlockBaseWeight(block) {
  if (block.type === 'banner_title' || block.type === 'section_title' || block.variant === 'heading') {
    return 700
  }
  return 400
}

function toggleFieldBold(field) {
  const currentWeight = Number(field.style?.fontWeight || getHeaderFieldBaseWeight(field)) || 400
  field.style.fontWeight = currentWeight >= 600 ? '400' : '700'
}

function adjustFieldFontSize(field, delta) {
  const current = Number.parseFloat(field.style?.fontSize || getHeaderFieldBaseSize(field)) || getHeaderFieldBaseSize(field)
  field.style.fontSize = Math.min(48, Math.max(12, current + delta))
}

function toggleBlockBold(block) {
  const currentWeight = Number(block.style?.fontWeight || getBlockBaseWeight(block)) || 400
  block.style.fontWeight = currentWeight >= 600 ? '400' : '700'
}

function adjustBlockFontSize(block, delta) {
  const current = Number.parseFloat(block.style?.fontSize || getBlockBaseSize(block)) || getBlockBaseSize(block)
  block.style.fontSize = Math.min(48, Math.max(12, current + delta))
}

function toggleBold() {
  if (!hasActiveEditableTarget.value) {
    ElMessage.warning('请先选择要编辑的内容')
    return
  }

  if (activeTarget.value.type === 'header_field') {
    const location = getActiveHeaderFieldLocation()
    if (!location) {
      return
    }

    if (headerFieldRefs.get(location.field.id)?.toggleBoldSelection?.()) {
      recordHistoryNow()
      return
    }

    toggleFieldBold(location.field)
    recordHistoryNow()
    return
  }

  const block = getActiveBlock()
  if (!block) {
    return
  }

  if (isRichTextBlock(block) && richBlockRefs.get(block.id)?.toggleBoldSelection?.()) {
    recordHistoryNow()
    return
  }

  toggleBlockBold(block)
  recordHistoryNow()
}

function updateCurrentFontSize(delta) {
  if (!hasActiveEditableTarget.value) {
    ElMessage.warning('请先选择要编辑的内容')
    return
  }

  if (activeTarget.value.type === 'header_field') {
    const location = getActiveHeaderFieldLocation()
    if (!location) {
      return
    }

    if (headerFieldRefs.get(location.field.id)?.adjustSelectionFontSize?.(delta)) {
      recordHistoryNow()
      return
    }

    adjustFieldFontSize(location.field, delta)
    recordHistoryNow()
    return
  }

  const block = getActiveBlock()
  if (!block) {
    return
  }

  if (isRichTextBlock(block) && richBlockRefs.get(block.id)?.adjustSelectionFontSize?.(delta)) {
    recordHistoryNow()
    return
  }

  adjustBlockFontSize(block, delta)
  recordHistoryNow()
}

function increaseFontSize() {
  updateCurrentFontSize(1)
}

function decreaseFontSize() {
  updateCurrentFontSize(-1)
}

function resetCurrentStyle() {
  if (!hasActiveEditableTarget.value) {
    ElMessage.warning('请先选择要编辑的内容')
    return
  }

  if (activeTarget.value.type === 'header_field') {
    const location = getActiveHeaderFieldLocation()
    if (!location) {
      return
    }

    if (headerFieldRefs.get(location.field.id)?.resetSelectionStyle?.()) {
      recordHistoryNow()
      return
    }

    location.field.style = createStyleModel()
    recordHistoryNow()
    return
  }

  const block = getActiveBlock()
  if (!block) {
    return
  }

  if (isRichTextBlock(block) && richBlockRefs.get(block.id)?.resetSelectionStyle?.()) {
    recordHistoryNow()
    return
  }

  block.style = createStyleModel()
  recordHistoryNow()
}

function resetDragState() {
  draggingBlockId.value = ''
  dragOverState.value = createEmptyDragState()
}

function resetMetaDragState() {
  metaDraggingId.value = ''
}

function handleBlockDragStart(blockId, event) {
  draggingBlockId.value = blockId
  activateBlock(blockId)
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', blockId)
  }
}

function setDragOver(sectionId, blockId, position, event) {
  if (event?.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }

  dragOverState.value = {
    sectionId,
    blockId: blockId || '',
    position,
  }
}

function clearDragOver() {
  dragOverState.value = createEmptyDragState()
}

function isDragOver(sectionId, blockId, position) {
  return (
    dragOverState.value.sectionId === sectionId &&
    dragOverState.value.blockId === (blockId || '') &&
    dragOverState.value.position === position
  )
}

/**
 * 正文拖拽继续遵循文档流重排，只改变块顺序和章节归属。
 * 这里显式写入 dataTransfer，并在 drop 后统一清理拖拽态，保证 HTML5 DnD 真正生效。
 */
async function handleBlockDrop(sectionId, blockId, position) {
  const draggedId = draggingBlockId.value
  if (!draggedId) {
    return
  }

  if (blockId && draggedId === blockId) {
    resetDragState()
    return
  }

  const fromLocation = findBlockLocation(draggedId)
  const toSectionIndex = sections.value.findIndex((section) => section.id === sectionId)
  if (!fromLocation || toSectionIndex === -1) {
    resetDragState()
    return
  }

  const draggedBlock = fromLocation.section.blocks.splice(fromLocation.blockIndex, 1)[0]
  const targetSection = sections.value[toSectionIndex]
  let insertIndex = targetSection.blocks.length

  if (blockId) {
    const targetIndex = targetSection.blocks.findIndex((item) => item.id === blockId)
    insertIndex = position === 'before' ? targetIndex : targetIndex + 1
  }

  if (fromLocation.section.id === sectionId && fromLocation.blockIndex < insertIndex) {
    insertIndex -= 1
  }

  targetSection.blocks.splice(insertIndex, 0, draggedBlock)
  resetDragState()
  await focusBlock(draggedBlock.id)
  recordHistoryNow()
}

function handleMetaDragStart(itemId, event) {
  metaDraggingId.value = itemId
  activateHeaderField(itemId, 'meta')
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', itemId)
  }
}

function handleMetaDragOver(event) {
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

async function handleMetaDrop(targetId) {
  const fromIndex = header.value.metaItems.findIndex((item) => item.id === metaDraggingId.value)
  const toIndex = header.value.metaItems.findIndex((item) => item.id === targetId)
  if (fromIndex === -1 || toIndex === -1 || fromIndex === toIndex) {
    resetMetaDragState()
    return
  }

  const [moved] = header.value.metaItems.splice(fromIndex, 1)
  header.value.metaItems.splice(toIndex, 0, moved)
  resetMetaDragState()
  await focusHeaderField(moved.id, 'meta')
  recordHistoryNow()
}

async function addMetaItem() {
  const field = createHeaderField({
    kind: 'meta',
    placeholder: '请输入联系方式',
  })
  header.value.metaItems.push(field)
  await focusHeaderField(field.id, 'meta')
  recordHistoryNow()
}

async function removeMetaItem(itemId) {
  const itemIndex = header.value.metaItems.findIndex((item) => item.id === itemId)
  if (itemIndex === -1) {
    return
  }

  header.value.metaItems.splice(itemIndex, 1)
  const nextItem = header.value.metaItems[itemIndex] || header.value.metaItems[itemIndex - 1]
  if (nextItem) {
    await focusHeaderField(nextItem.id, 'meta')
  } else {
    clearActiveState()
  }
  recordHistoryNow()
}

async function addSummaryLine() {
  const field = createHeaderField({
    kind: 'summary',
    placeholder: '请输入补充说明',
  })
  header.value.summaryLines.push(field)
  await focusHeaderField(field.id, 'summary')
  recordHistoryNow()
}

async function removeSummaryLine(itemId) {
  const itemIndex = header.value.summaryLines.findIndex((item) => item.id === itemId)
  if (itemIndex === -1) {
    return
  }

  header.value.summaryLines.splice(itemIndex, 1)
  const nextItem = header.value.summaryLines[itemIndex] || header.value.summaryLines[itemIndex - 1]
  if (nextItem) {
    await focusHeaderField(nextItem.id, 'summary')
  } else {
    clearActiveState()
  }
  recordHistoryNow()
}

function handlePhotoChange(event) {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }

  if (!file.type.startsWith('image/')) {
    event.target.value = ''
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('照片文件大小不能超过 5MB')
    event.target.value = ''
    return
  }

  const reader = new FileReader()
  reader.onload = () => {
    photoDataUrl.value = typeof reader.result === 'string' ? reader.result : ''
    event.target.value = ''
    recordHistoryNow()
  }
  reader.readAsDataURL(file)
}

function triggerPhotoUpload() {
  photoInputRef.value?.click()
}

function clearPhoto() {
  photoDataUrl.value = ''
  if (photoInputRef.value) {
    photoInputRef.value.value = ''
  }
  recordHistoryNow()
}

function getResumePlainText() {
  const lines = []

  if (header.value.sectionTitle?.trim()) {
    lines.push(header.value.sectionTitle.trim())
  }

  const name = readHeaderFieldText(header.value.name)
  if (name) {
    lines.push(name)
  }

  const jobTarget = readHeaderFieldText(header.value.jobTarget)
  if (jobTarget) {
    lines.push(jobTarget)
  }

  const profileMeta = header.value.metaItems.map((item) => readHeaderFieldText(item)).filter(Boolean)
  if (profileMeta.length) {
    lines.push(profileMeta.join(' | '))
  }

  header.value.summaryLines
    .map((item) => readHeaderFieldText(item))
    .filter(Boolean)
    .forEach((item) => lines.push(item))

  sections.value.forEach((section) => {
    if (section.title?.trim()) {
      lines.push('')
      lines.push(section.title.trim())
    }

    section.blocks.forEach((block) => {
      if (block.type === 'banner_title') {
        const title = block.title?.trim() || ''
        if (title) {
          lines.push('')
          lines.push(title)
        }
        return
      }

      if (block.type === 'row') {
        const rowText = block.items.map((item) => item.value.trim()).filter(Boolean).join(' | ')
        if (rowText) {
          lines.push(rowText)
        }
        return
      }

      if (block.type === 'label') {
        const labelText = buildLabelPlainText(block)
        if (labelText) {
          lines.push(labelText)
        }
        return
      }

      const text = stripHtmlToText(block.html)
      if (!text) {
        return
      }

      if (block.variant === 'bullet') {
        lines.push(`- ${text}`)
        return
      }

      if (block.type === 'section_title') {
        lines.push('')
      }

      lines.push(text)
    })
  })

  return lines.join('\n').trim()
}

function getResumeName() {
  return readHeaderFieldText(header.value.name)
}

function sanitizeRichTextClone(rootNode) {
  rootNode.querySelectorAll('[contenteditable]').forEach((node) => {
    node.removeAttribute('contenteditable')
    node.removeAttribute('role')
    node.removeAttribute('tabindex')
  })

  rootNode.querySelectorAll('.ProseMirror-focused, .has-focus, .is-active, .is-focused').forEach((node) => {
    node.classList.remove('ProseMirror-focused', 'has-focus', 'is-active', 'is-focused')
  })
}

/**
 * Vue 单文件组件启用 scoped 后，新增的静态导出节点也必须复制作用域属性，
 * 否则导出节点无法命中当前组件样式，最终出现预览和导出不一致。
 */
function copyScopedAttributes(sourceNode, targetNode) {
  Array.from(sourceNode.attributes).forEach((attribute) => {
    if (attribute.name.startsWith('data-v-')) {
      targetNode.setAttribute(attribute.name, attribute.value)
    }
  })
}

function createStaticFieldNode(fieldNode) {
  const nextNode = fieldNode.ownerDocument.createElement('div')
  copyScopedAttributes(fieldNode, nextNode)
  nextNode.className = `${fieldNode.className} export-static-field`.trim()
  nextNode.classList.remove('resume-inline-input', 'resume-textarea-input')
  if (fieldNode.getAttribute('style')) {
    nextNode.setAttribute('style', fieldNode.getAttribute('style'))
  }
  nextNode.textContent = fieldNode.value || fieldNode.placeholder || ''
  return nextNode
}

function replaceFormFieldWithStaticText(rootNode) {
  rootNode.querySelectorAll('input, textarea').forEach((fieldNode) => {
    fieldNode.replaceWith(createStaticFieldNode(fieldNode))
  })
}

/**
 * 预览态照片区域用 button 承载上传交互。
 * 导出时需要保留视觉样式，但不能把原生按钮语义带进截图，否则会污染最终结果。
 */
function replacePhotoFrameButtonWithStaticNode(rootNode) {
  rootNode.querySelectorAll('.photo-frame--button').forEach((buttonNode) => {
    const nextNode = buttonNode.ownerDocument.createElement('div')
    copyScopedAttributes(buttonNode, nextNode)
    nextNode.className = buttonNode.className
    buttonNode.childNodes.forEach((childNode) => {
      nextNode.appendChild(childNode.cloneNode(true))
    })
    buttonNode.replaceWith(nextNode)
  })
}

/**
 * html2canvas 对部分渐变和重复纹理背景的解析不稳定。
 * 导出前统一降级为近似纯色，优先保证预览和 PDF/图片下载链路稳定。
 */
function applyExportSafeBackgrounds(rootNode) {
  rootNode.querySelectorAll('.section-tab').forEach((node) => {
    node.style.backgroundImage = 'none'
    node.style.backgroundColor = '#edf4f2'
  })

  rootNode.querySelectorAll('.section-line').forEach((node) => {
    node.style.backgroundImage = 'none'
    node.style.backgroundColor = '#d8ddd8'
  })

  rootNode.querySelectorAll('.photo-frame, .photo-placeholder').forEach((node) => {
    node.style.backgroundImage = 'none'
    node.style.backgroundColor = '#f3f6f5'
  })
}

/**
 * 导出时统一基于当前模板克隆出只读节点，
 * 去掉工具栏、拖拽手柄、上传控件、占位提示和焦点态，保证下载内容与用户最终编辑结果一致。
 */
function buildExportElement() {
  clearActiveState()

  if (!resumeRef.value) {
    return null
  }

  const clone = resumeRef.value.cloneNode(true)
  clone.classList.remove('resume-template--preview')
  clone.classList.add('resume-template--print')
  clone.querySelector('.editor-toolbar')?.remove()
  clone
    .querySelectorAll(
      '.drag-handle, .editor-ghost-btn, .profile-meta-tools, .photo-input, .photo-actions, .block-drop-indicator, .section-drop-tail, .inline-rich-placeholder',
    )
    .forEach((node) => node.remove())

  sanitizeRichTextClone(clone)
  replaceFormFieldWithStaticText(clone)
  replacePhotoFrameButtonWithStaticNode(clone)
  applyExportSafeBackgrounds(clone)
  clone.querySelector('.photo-tip')?.remove()

  return clone
}

defineExpose({
  getResumePlainText,
  getResumeName,
  buildExportElement,
})
</script>

<style scoped>
.resume-template {
  --resume-accent: #1b5b57;
  --resume-accent-rgb: 27, 91, 87;
  --resume-accent-soft: #e8f0ee;
  --resume-accent-soft-rgb: 232, 240, 238;
  --resume-gold: #b18757;
  --resume-gold-rgb: 177, 135, 87;
  --resume-text: #1f2933;
  --resume-muted: #52606d;
  --resume-line: #d6ddd8;
  --resume-line-rgb: 214, 221, 216;
  --resume-focus: rgba(var(--resume-accent-rgb), 0.10);
  --resume-focus-border: rgba(var(--resume-accent-rgb), 0.18);
  width: 100%;
  box-sizing: border-box;
}

.resume-template--preview {
  max-width: 980px;
  margin: 0 auto;
}

.resume-template--print {
  width: 190mm;
  margin: 0 auto;
}

.editor-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
  padding: 10px 12px;
  background: var(--bg-page, rgba(255, 248, 243, 0.96));
  border: 1px solid var(--border-card, rgba(243, 216, 199, 0.92));
  border-radius: 14px;
  position: sticky;
  top: 12px;
  z-index: 8;
  backdrop-filter: blur(10px);
}

.editor-tool,
.editor-ghost-btn {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid rgba(var(--resume-accent-rgb), 0.18);
  border-radius: 999px;
  background: var(--bg-card, #fff);
  color: var(--resume-accent);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.editor-tool:hover,
.editor-ghost-btn:hover {
  border-color: rgba(var(--resume-accent-rgb), 0.48);
  box-shadow: 0 6px 14px rgba(var(--resume-accent-rgb), 0.1);
  transform: translateY(-1px);
}

.editor-tool:disabled {
  opacity: 0.42;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.toolbar-separator {
  width: 1px;
  height: 24px;
  background: rgba(var(--resume-accent-rgb), 0.2);
  margin: 0 4px;
  align-self: center;
}

.resume-paper {
  box-sizing: border-box;
  background: var(--bg-card, #fff);
  color: var(--resume-text);
  border: 1px solid var(--resume-line, #d7dfda);
}

.resume-template--preview .resume-paper {
  padding: 28px 34px 34px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.resume-template--print .resume-paper {
  padding: 8mm 10mm 8mm;
  border: none;
  box-shadow: none;
}

.resume-main {
  margin-top: 14px;
}

.resume-template--print .resume-main {
  margin-top: 10px;
}

.resume-section + .resume-section {
  margin-top: 30px;
}

.resume-template--print .resume-section + .resume-section {
  margin-top: 20px;
}

.resume-section-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}

.resume-template--print .resume-section-head {
  margin-bottom: 14px;
}

.section-tab {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 5px 14px 5px 10px;
  background: linear-gradient(90deg, rgba(var(--resume-accent-rgb), 0.14), rgba(var(--resume-accent-rgb), 0.05));
  border-left: 3px solid var(--resume-gold);
  border-radius: 0 16px 16px 0;
}

.section-tab-mark {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--resume-gold);
  flex-shrink: 0;
}

.section-title-input,
.resume-inline-input,
.resume-textarea-input {
  width: 100%;
  box-sizing: border-box;
  border: none;
  background: transparent;
  color: inherit;
  outline: none;
  padding: 0;
  font: inherit;
}

.section-title-input {
  min-width: 80px;
  margin: 0;
  font-size: 17px;
  line-height: 1.2;
  font-weight: 700;
  color: var(--resume-accent);
  letter-spacing: 0.08em;
  white-space: nowrap;
}

.section-line {
  flex: 1;
  min-width: 0;
  height: 1px;
  background: linear-gradient(90deg, rgba(var(--resume-gold-rgb), 0.55), rgba(var(--resume-line-rgb), 0.65));
}

.profile-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 118px;
  gap: 24px;
  align-items: start;
  padding: 6px 0 10px;
}

.resume-template--print .profile-card {
  padding: 4px 0 6px;
  gap: 16px;
}

.profile-main {
  min-width: 0;
}

.profile-name-row {
  display: flex;
  align-items: baseline;
  gap: 14px;
  flex-wrap: wrap;
}

.profile-name-input {
  display: block;
  min-width: 0;
  margin: 0;
  font-size: 38px;
  line-height: 1.06;
  font-weight: 800;
  color: var(--resume-text, #143f45);
  letter-spacing: 0.06em;
}

.profile-target-input {
  display: block;
  width: 100%;
  box-sizing: border-box;
  min-height: 28px;
  margin-left: -10px;
  padding: 0 12px 0 22px;
  border-radius: 999px;
  background: rgba(var(--resume-gold-rgb), 0.12);
  color: var(--resume-gold);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
}

.profile-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 18px;
  margin-top: 14px;
}

.profile-meta-item--wide {
  grid-column: 1 / -1;
}

.profile-meta-card {
  position: relative;
  display: flex;
  align-items: center;
  min-width: 0;
}

.profile-meta-input {
  width: 100%;
  min-width: 0;
  font-size: 14px;
  line-height: 1.72;
  color: var(--resume-muted);
  word-break: break-word;
}

.profile-meta-tools {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.profile-summary {
  margin-top: 12px;
  padding: 10px 12px;
  border-left: 2px solid rgba(var(--resume-accent-rgb), 0.22);
  background: rgba(var(--resume-accent-soft-rgb), 0.32);
}

.profile-summary-item {
  position: relative;
}

.profile-summary-item + .profile-summary-item {
  margin-top: 10px;
}

.profile-summary-input {
  min-height: 44px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--resume-text);
  white-space: pre-wrap;
}

.profile-photo {
  position: relative;
  justify-self: end;
  width: 118px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.photo-input {
  display: none;
}

.photo-frame {
  display: block;
  width: 118px;
  height: 146px;
  box-sizing: border-box;
  border: 1.5px dashed rgba(var(--resume-accent-rgb), 0.45);
  background:
    linear-gradient(135deg, rgba(var(--resume-accent-soft-rgb), 0.68), rgba(255, 255, 255, 0.96)),
    repeating-linear-gradient(
      -45deg,
      rgba(var(--resume-gold-rgb), 0.08),
      rgba(var(--resume-gold-rgb), 0.08) 10px,
      transparent 10px,
      transparent 20px
    );
  overflow: hidden;
}

.photo-frame--button {
  padding: 0;
  border-left-width: 1.5px;
  cursor: pointer;
  appearance: none;
}

.photo-image,
.photo-placeholder {
  width: 100%;
  height: 100%;
}

.photo-image {
  display: block;
  object-fit: cover;
}

.photo-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 14px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--resume-muted);
  text-align: center;
}

.photo-actions {
  position: absolute;
  left: 50%;
  bottom: 34px;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: calc(100% - 8px);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease;
}

.photo-action {
  min-height: 28px;
  border: 1px solid rgba(var(--resume-accent-rgb), 0.24);
  border-radius: 999px;
  background: rgba(var(--resume-accent-soft-rgb), 0.62);
  color: var(--resume-accent);
  font-size: 12px;
  line-height: 1.2;
  cursor: pointer;
}

.photo-action--ghost {
  background: var(--bg-card, #fff);
  color: var(--resume-muted);
}

.photo-tip {
  margin: 0;
  font-size: 12px;
  color: var(--resume-muted);
  line-height: 1.4;
}

.resume-section-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.resume-block-shell {
  position: relative;
}

.resume-block-shell.is-active .resume-block {
  outline: 2px dashed var(--resume-accent);
  outline-offset: 2px;
  border-radius: 6px;
  background: rgba(var(--resume-accent-rgb), 0.04);
}

.resume-block {
  position: relative;
  min-width: 0;
}

.drag-handle {
  position: absolute;
  left: -22px;
  top: 3px;
  border: none;
  background: transparent;
  color: var(--resume-muted);
  cursor: grab;
  font-size: 14px;
  line-height: 1;
  padding: 0;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.drag-handle--meta {
  position: absolute;
  left: -22px;
  top: 50%;
  transform: translateY(-50%);
  flex-shrink: 0;
}

.meta-remove-btn,
.summary-remove-btn {
  position: absolute;
  right: -12px;
  top: 50%;
  transform: translateY(-50%);
  min-height: 32px;
  padding: 0 10px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.summary-remove-btn {
  top: 10px;
  transform: none;
}

.drag-handle:active {
  cursor: grabbing;
}

.profile-meta-card:hover .drag-handle--meta,
.profile-meta-card:focus-within .drag-handle--meta,
.profile-meta-card:hover .meta-remove-btn,
.profile-meta-card:focus-within .meta-remove-btn,
.profile-summary-item:hover .summary-remove-btn,
.profile-summary-item:focus-within .summary-remove-btn {
  opacity: 1;
}

.resume-block-shell:hover .drag-handle--block,
.resume-block-shell:focus-within .drag-handle--block {
  opacity: 1;
}

.profile-photo:hover .photo-actions,
.profile-photo:focus-within .photo-actions {
  opacity: 1;
  pointer-events: auto;
}

.resume-template--preview .photo-tip {
  display: none;
}

.entry-row {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr) minmax(118px, 0.85fr);
  gap: 16px;
  align-items: baseline;
}

.entry-row--education {
  grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr) minmax(72px, 0.7fr) minmax(118px, 0.9fr);
}

.entry-cell-input,
.entry-cell--left,
.entry-cell--middle,
.entry-cell--right {
  min-width: 0;
  font-size: 14px;
  line-height: 1.72;
  color: var(--resume-text);
  word-break: break-word;
}

.entry-cell--left {
  font-weight: 700;
}

.entry-cell--middle {
  font-weight: 600;
  color: var(--resume-text);
}

.entry-cell--right {
  text-align: right;
  color: var(--resume-muted);
}

.entry-row--education .entry-cell--right {
  white-space: nowrap;
}

.label-line,
.text-line,
.subsection-line {
  margin: 0;
  font-size: 14px;
  line-height: 1.82;
  color: var(--resume-text);
  white-space: pre-wrap;
  word-break: break-word;
}

.subsection-line {
  font-size: 15px;
  font-weight: 700;
  color: var(--resume-text);
}

.text-line--heading {
  font-size: 15px;
  font-weight: 700;
  color: var(--resume-text);
}

.text-line--bullet {
  position: relative;
  padding-left: 14px;
}

.text-line--bullet::before {
  content: '•';
  position: absolute;
  left: 0;
  top: 0;
  color: var(--resume-gold);
}

.label-key-input {
  display: inline-block;
  width: auto;
  margin-right: 6px;
  font-weight: 700;
  color: var(--resume-text);
}

.label-value-input {
  color: var(--resume-muted);
}

.block-drop-indicator,
.section-drop-tail {
  height: 10px;
  border-radius: 999px;
  background: transparent;
  transition: background-color 0.2s ease;
}

.block-drop-indicator.is-visible,
.section-drop-tail.is-visible {
  background: rgba(var(--resume-accent-rgb), 0.18);
}

.section-drop-tail {
  font-size: 12px;
  line-height: 10px;
  color: var(--resume-muted);
  text-align: center;
  padding: 6px 0;
  margin-top: 2px;
}

.resume-inline-input:focus,
.resume-textarea-input:focus,
.section-title-input:focus,
.profile-name-input.is-focused,
.profile-target-input.is-focused,
.profile-meta-input.is-focused,
.profile-summary-input.is-focused {
  background: var(--resume-focus);
  box-shadow: 0 0 0 2px var(--resume-focus-border);
  border-radius: 6px;
}

.profile-name-input.is-active,
.profile-target-input.is-active,
.profile-meta-card.is-active,
.profile-summary-item.is-active {
  border-radius: 6px;
  background: rgba(var(--resume-accent-rgb), 0.04);
  box-shadow: 0 0 0 2px rgba(var(--resume-accent-rgb), 0.12);
}

.profile-name-input.is-active,
.profile-target-input.is-active {
  padding-inline: 4px;
}

.profile-target-input.is-active {
  padding-left: 26px;
  padding-right: 16px;
}

.profile-target-input :deep(.inline-rich-placeholder) {
  left: 22px;
  top: 50%;
  transform: translateY(-50%);
}

.resume-template--print .resume-inline-input,
.resume-template--print .resume-textarea-input,
.resume-template--print .section-title-input {
  background: transparent !important;
  box-shadow: none !important;
  pointer-events: none;
  caret-color: transparent;
  resize: none;
  appearance: none;
  -webkit-appearance: none;
}

.export-static-field {
  width: 100%;
  box-sizing: border-box;
  white-space: pre-wrap;
  word-break: break-word;
}

.resume-template--print .profile-target-input {
  display: flex;
  align-items: center;
  min-height: 28px;
  padding: 4px 18px 4px 28px;
  background: rgba(177, 135, 87, 0.16);
  color: #7a5631;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.35;
}

.resume-template--print .profile-meta-card,
.resume-template--print .profile-summary-item {
  background: transparent;
  box-shadow: none;
}

.resume-template--print .photo-frame {
  border: none;
  background: var(--bg-elevated, #f3f6f5);
}

.resume-template--print .photo-placeholder {
  background: var(--bg-elevated, #f3f6f5);
}

@media (max-width: 767px) {
  .editor-toolbar {
    position: static;
  }

  .resume-template--preview .resume-paper {
    padding: 20px 16px 24px;
  }

  .profile-card {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .profile-photo {
    justify-self: start;
  }

  .photo-actions {
    width: 100%;
    max-width: 118px;
  }

  .profile-name-input {
    font-size: 31px;
  }

  .profile-meta-grid {
    grid-template-columns: 1fr;
  }

  .entry-row {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .entry-cell--right {
    text-align: left;
  }
}
</style>

<style>
/* ===== ResumeTemplate 暗黑模式适配 =====
   仅影响预览模式，导出(print)模式不受影响 */
[data-theme="dark"] .resume-template:not(.resume-template--print) {
  --resume-accent: #4ecdc4;
  --resume-accent-rgb: 78, 205, 196;
  --resume-accent-soft: #1a3a38;
  --resume-accent-soft-rgb: 26, 58, 56;
  --resume-gold: #d4a76a;
  --resume-gold-rgb: 212, 167, 106;
  --resume-text: #e2e8f0;
  --resume-muted: #94a3b8;
  --resume-line: #334155;
  --resume-line-rgb: 51, 65, 85;
  --resume-focus: rgba(var(--resume-accent-rgb), 0.15);
  --resume-focus-border: rgba(var(--resume-accent-rgb), 0.25);
}

/* ===== ResumeTemplate 导出(print)模式强制亮色 =====
   确保 html2canvas 截图时不论父文档是否暗黑模式，
   导出内容始终使用亮色。!important 防止任何 CSS 优先级覆盖。 */
.resume-template--print {
  --resume-text: #1f2933 !important;
  --resume-muted: #52606d !important;
  --resume-accent: #1b5b57 !important;
  --resume-accent-rgb: 27, 91, 87 !important;
  --resume-accent-soft: #e8f0ee !important;
  --resume-accent-soft-rgb: 232, 240, 238 !important;
  --resume-gold: #b18757 !important;
  --resume-gold-rgb: 177, 135, 87 !important;
  --resume-line: #d6ddd8 !important;
  --resume-line-rgb: 214, 221, 216 !important;
  --resume-focus: rgba(27, 91, 87, 0.10) !important;
  --resume-focus-border: rgba(27, 91, 87, 0.18) !important;
  --bg-card: #ffffff !important;
  --bg-elevated: #fafafa !important;
  --bg-page: #fff8f3 !important;
  --border-card: #f3d8c7 !important;
  --text-body: #555555 !important;
}
</style>
