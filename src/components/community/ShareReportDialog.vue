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

      <!-- 报告预览 -->
      <div class="editor-field">
        <label class="field-label">
          报告内容
          <span class="field-hint">（将附在文案之后）</span>
        </label>
        <div class="report-preview">
          <div class="report-preview-content">{{ reportSummary }}</div>
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
const reportSummary = ref('')
const submitting = ref(false)

const fullContent = computed(() => {
  const user = userText.value.trim()
  const report = reportSummary.value.trim()
  if (user && report) return user + '\n\n' + report
  if (user) return user
  return report
})

const totalLength = computed(() => fullContent.value.length)

const canSubmit = computed(() => {
  return fullContent.value.length > 0 && totalLength.value <= 2000 && !submitting.value
})

function parseReport(raw) {
  if (!raw) return null
  if (typeof raw === 'object') return raw
  let trimmed = raw.trim()
  if (trimmed.startsWith('```json')) trimmed = trimmed.slice(7)
  else if (trimmed.startsWith('```')) trimmed = trimmed.slice(3)
  const lastBacktick = trimmed.lastIndexOf('```')
  if (lastBacktick > 0) trimmed = trimmed.slice(0, lastBacktick)
  trimmed = trimmed.trim()
  if (!trimmed) return null
  try { return JSON.parse(trimmed) } catch { return null }
}

function generateSummary() {
  const s = props.sessionData
  if (!s) return ''

  const report = parseReport(s.evaluationReport)
  const parts = []

  parts.push('📊 模拟面试报告')
  parts.push('')

  const meta = []
  if (s.jobRole) meta.push(`岗位：${s.jobRole}`)
  if (s.difficultyDesc) meta.push(`难度：${s.difficultyDesc}`)
  else if (s.difficulty) {
    const dMap = { 1: '初级', 2: '中级', 3: '高级' }
    meta.push(`难度：${dMap[s.difficulty] || '--'}`)
  }
  if (s.comprehensiveScore != null) meta.push(`综合评分：${s.comprehensiveScore}分`)
  if (meta.length) parts.push(meta.join(' | '))
  parts.push('')

  if (report) {
    const strengths = report.strengths || []
    if (strengths.length) {
      parts.push('✨ 优势亮点：')
      strengths.forEach(v => parts.push(`• ${v}`))
      parts.push('')
    }

    const weaknesses = [...(report.weaknesses || []), ...(report.missingCompetencies || [])]
    if (weaknesses.length) {
      parts.push('⚠️ 不足之处：')
      weaknesses.forEach(v => parts.push(`• ${v}`))
      parts.push('')
    }

    const suggestions = [...(report.improvementSuggestions || []), ...(report.suggestions || [])]
    if (suggestions.length) {
      parts.push('💡 改进建议：')
      suggestions.forEach(v => parts.push(`• ${v}`))
      parts.push('')
    }

    const dims = [
      { key: 'jobMatch', label: '岗位匹配' },
      { key: 'technicalDepth', label: '技术深度' },
      { key: 'communication', label: '沟通表达' },
      { key: 'problemSolving', label: '问题解决' },
      { key: 'pressureResistance', label: '抗压表现' }
    ]
    const dimScores = dims
      .map(d => {
        const val = report[d.key]
        return val && val.score != null ? `${d.label} ${val.score}` : null
      })
      .filter(Boolean)
    if (dimScores.length) {
      parts.push('📈 维度评分：')
      parts.push(dimScores.join(' | '))
      parts.push('')
    }

    if (report.summary) {
      parts.push('💬 AI 评估总结：')
      parts.push(report.summary)
    }
  }

  return parts.join('\n').trim()
}

function initContent() {
  userText.value = ''
  reportSummary.value = generateSummary()
}

async function handleSubmit() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    await createPost({
      category: 'interview_exp',
      content: fullContent.value,
      images: []
    })
    ElMessage.success('分享成功')
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
