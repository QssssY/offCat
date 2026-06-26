<template>
  <!-- 报告页有入场 transform，弹窗挂到 body 可避免只出现遮罩、面板被定位到不可见区域。 -->
  <el-dialog
    :model-value="visible"
    title="分享面试报告到社区"
    width="600px"
    :close-on-click-modal="false"
    :append-to-body="true"
    @update:model-value="$emit('update:visible', $event)"
    @open="initContent"
  >
    <div class="share-report-dialog">
      <!-- 标题会作为社区帖子标题保存，用户可在分享前按语境微调。 -->
      <div class="editor-field">
        <label class="field-label">帖子标题</label>
        <input
          v-model="reportTitle"
          class="title-input"
          type="text"
          maxlength="120"
          placeholder="请输入社区帖子标题"
        />
      </div>

      <!-- 用户文案 -->
      <div class="editor-field">
        <label class="field-label">我的文案</label>
        <textarea
          v-model="userText"
          class="content-textarea"
          placeholder="写点你的面试心得、感想..."
          rows="4"
        />
      </div>

      <!-- 报告链接预览 -->
      <div class="editor-field">
        <label class="field-label">
          报告链接
          <span class="field-hint">（发布后其他用户可点击查看）</span>
        </label>
        <div class="report-preview report-link-preview">
          <div class="report-preview-title">{{ normalizedReportTitle || '面试报告分享' }}</div>
          <div class="report-preview-content">{{ reportLink }}</div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <span class="total-count" :class="{ 'char-over': totalLength > 2000 }">
          总字数：{{ totalLength }}/2000
        </span>
        <div class="footer-actions">
          <el-button @click="$emit('update:visible', false)">取消</el-button>
          <el-button
            type="primary"
            :disabled="!canSubmit"
            :loading="submitting"
            @click="handleSubmit"
          >
            发布到社区
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { createPost } from '@/api/community'

const props = defineProps({
  visible: { type: Boolean, default: false },
  sessionData: { type: Object, default: null }
})

const emit = defineEmits(['update:visible'])

const userText = ref('')
const reportTitle = ref('')
const reportLink = ref('')
const submitting = ref(false)

const fullContent = computed(() => {
  const user = userText.value.trim()
  if (user) return user
  return `我分享了一份${reportTitle.value || '模拟面试报告'}，点击下方链接查看完整报告。`
})

const totalLength = computed(() => fullContent.value.length)
const normalizedReportTitle = computed(() => reportTitle.value.trim())

const canSubmit = computed(() => {
  return normalizedReportTitle.value.length > 0 && fullContent.value.length > 0 && totalLength.value <= 2000 && !submitting.value
})

function initContent() {
  userText.value = ''
  reportTitle.value = generateReportTitle()
  reportLink.value = buildReportLink()
}

function generateReportTitle() {
  const role = props.sessionData?.jobRole || '模拟面试'
  return `${role} 面试报告`
}

function buildReportLink() {
  const id = props.sessionData?.sessionId || props.sessionData?.id || ''
  return id ? `/interview/report/${id}` : ''
}

async function handleSubmit() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    const res = await createPost({
      category: 'interview_exp',
      title: normalizedReportTitle.value,
      content: fullContent.value,
      images: [],
      sharedInterviewSessionId: props.sessionData?.sessionId || props.sessionData?.id || ''
    })
    const reviewStatus = res?.data?.reviewStatus
    ElMessage.success(reviewStatus === 'approved' ? '分享成功，已公开展示' : '已提交审核，通过后将在社区展示')
    emit('update:visible', false)
  } catch {
    ElMessage.error('分享失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.share-report-dialog {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.editor-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-title);
}

.field-hint {
  font-weight: 400;
  color: var(--text-muted);
  font-size: 13px;
}

/* 用户文案输入 */
.content-textarea {
  width: 100%;
  padding: 12px 14px;
  border: 2px solid var(--border-input);
  border-radius: 12px;
  font-size: 14px;
  color: var(--text-title);
  background: var(--bg-input);
  resize: vertical;
  min-height: 100px;
  font-family: inherit;
  line-height: 1.7;
  transition: border-color 0.3s, box-shadow 0.3s;
}

.content-textarea:focus {
  outline: none;
  border-color: var(--orange-main);
  box-shadow: 0 0 0 3px rgba(255, 140, 66, 0.12);
}

.content-textarea::placeholder {
  color: var(--text-placeholder);
}

/* 报告预览卡片 */
.report-preview {
  background: var(--orange-light-bg, #fffaf7);
  border: 1px solid var(--border-card, rgba(243, 216, 199, 0.5));
  border-radius: 12px;
  max-height: 240px;
  overflow-y: auto;
}

.report-preview-content {
  padding: 14px 16px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-title, #2f2f2f);
  white-space: pre-wrap;
  word-break: break-word;
}

.title-input {
  width: 100%;
  padding: 11px 14px;
  border: 2px solid var(--border-input);
  border-radius: 12px;
  font-size: 14px;
  color: var(--text-title);
  background: var(--bg-input);
  font-family: inherit;
  line-height: 1.5;
  transition: border-color 0.3s, box-shadow 0.3s;
}

.title-input:focus {
  outline: none;
  border-color: var(--orange-main);
  box-shadow: 0 0 0 3px rgba(255, 140, 66, 0.12);
}

.title-input::placeholder {
  color: var(--text-placeholder);
}

.report-link-preview {
  max-height: none;
  padding: 14px 16px;
}

.report-link-preview .report-preview-content {
  padding: 0;
  color: var(--orange-deep, #d9661e);
  font-weight: 600;
  white-space: normal;
}

.report-preview-title {
  margin-bottom: 6px;
  color: var(--text-title, #2f2f2f);
  font-size: 14px;
  font-weight: 700;
}

/* 底部操作栏 */
.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.footer-actions {
  display: flex;
  gap: 12px;
}

.total-count {
  font-size: 12px;
  color: var(--text-placeholder);
}

.total-count.char-over {
  color: #f56c6c;
}
</style>
