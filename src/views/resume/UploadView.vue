<template>
  <div class="resume-upload-view">
    <div v-if="needLogin" class="login-prompt">
      <el-alert title="请先登录" type="warning" :closable="false" show-icon>
        <template #default>
          <span>使用简历诊断功能前需要先登录账号。</span>
          <el-button type="primary" size="small" style="margin-left: 12px" @click="goToLogin">
            去登录
          </el-button>
        </template>
      </el-alert>
    </div>

    <div class="page-header">
      <h1 class="page-title">简历诊断</h1>
      <p class="page-desc">上传 PDF 简历后，系统会自动提取内容并生成诊断结果。</p>
    </div>

    <div class="upload-section">
      <div class="upload-card">
        <el-upload
          class="upload-area"
          drag
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleFileChange"
          :accept="acceptedFormats"
        >
          <div v-if="!selectedFile" class="upload-placeholder">
            <div class="upload-icon">
              <el-icon :size="48"><Upload /></el-icon>
            </div>
            <div class="upload-text">
              <div class="upload-title">点击或拖拽文件到此处上传</div>
              <div class="upload-hint">
                支持文本型 PDF 与图片型/扫描型 PDF，文件大小不超过 10MB
              </div>
            </div>
          </div>
          <div v-else class="upload-selected">
            <div class="file-info">
              <el-icon class="file-icon" :size="32"><Document /></el-icon>
              <div class="file-detail">
                <div class="file-name">{{ selectedFile.name }}</div>
                <div class="file-size">{{ formatFileSize(selectedFile.size) }}</div>
              </div>
            </div>
            <el-button link type="danger" @click.stop="clearFile">更换文件</el-button>
          </div>
        </el-upload>

        <div v-if="fileError" class="file-error">
          <el-icon><WarningFilled /></el-icon>
          <span>{{ fileError }}</span>
        </div>

        <div class="file-requirements">
          <div class="req-title">文件要求</div>
          <ul class="req-list">
            <li>文件格式：仅支持 <strong>PDF</strong></li>
            <li>文件大小：不超过 10MB</li>
            <li>支持文本型 PDF，也支持图片型/扫描型 PDF</li>
          </ul>
          <div class="req-warning">
            <el-icon><WarningFilled /></el-icon>
            <span>图片型/扫描型 PDF 需要额外识别步骤，处理时间可能更长。</span>
          </div>
        </div>

        <div class="submit-section">
          <el-button
            type="primary"
            size="large"
            :disabled="!selectedFile || fileError || submitting"
            :loading="submitting"
            :class="{ 'btn-loading': submitting }"
            @click="handleSubmit"
          >
            <span v-if="submitting" class="btn-text-loading">AI 正在分析中...</span>
            <span v-else>{{ buttonText }}</span>
          </el-button>
        </div>

        <div v-if="submitError" class="submit-error">
          <div class="error-icon">
            <el-icon :size="32" color="#f56c6c"><CircleClose /></el-icon>
          </div>
          <div class="error-content">
            <div class="error-title">提交失败</div>
            <div class="error-desc">{{ submitError }}</div>
            <el-button type="primary" @click="retrySubmit">重新提交</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="info-section">
      <div class="info-card">
        <h3 class="info-title">诊断完成后你将看到</h3>
        <div class="info-list">
          <div class="info-item">
            <div class="info-dot"></div>
            <div class="info-text">简历基础信息完整度分析</div>
          </div>
          <div class="info-item">
            <div class="info-dot"></div>
            <div class="info-text">技能匹配度评估</div>
          </div>
          <div class="info-item">
            <div class="info-dot"></div>
            <div class="info-text">经历描述优化建议</div>
          </div>
          <div class="info-item">
            <div class="info-dot"></div>
            <div class="info-text">整体评分与改进方向</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CircleClose, Document, Upload, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadResume } from '@/api/resume'
import { isLoggedIn } from '@/utils/auth'

const router = useRouter()

const acceptedFormats = '.pdf'
const maxFileSize = 10 * 1024 * 1024

const needLogin = computed(() => !isLoggedIn())
const selectedFile = ref(null)
const fileError = ref('')
const submitting = ref(false)
const submitError = ref('')

const buttonText = computed(() => (submitting.value ? '提交中...' : '开始诊断'))

const formatFileSize = (bytes) => {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

/**
 * 上传页仅保留基础文件校验。
 * 文本型还是图片型 PDF 统一交给后端混合解析链路处理。
 */
const validateFile = (file) => {
  if (!file) {
    fileError.value = '请选择文件'
    return false
  }

  const extension = String(file.name || '').split('.').pop()?.toLowerCase()
  if (extension !== 'pdf') {
    fileError.value = '仅支持 PDF 格式文件'
    return false
  }

  if (file.size > maxFileSize) {
    fileError.value = '文件大小不能超过 10MB'
    return false
  }

  fileError.value = ''
  return true
}

const handleFileChange = (file) => {
  const rawFile = file?.raw
  if (validateFile(rawFile)) {
    selectedFile.value = rawFile
    submitError.value = ''
  }
}

const clearFile = () => {
  selectedFile.value = null
  fileError.value = ''
}

const goToLogin = () => {
  router.push({
    path: '/login',
    query: { redirect: '/resume/upload' }
  })
}

const handleSubmit = async () => {
  if (!isLoggedIn()) {
    try {
      await ElMessageBox.confirm(
        '使用简历诊断功能前需要先登录，是否前往登录页？',
        '提示',
        {
          confirmButtonText: '去登录',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      goToLogin()
    } catch {
      // 用户主动取消时不再额外提示。
    }
    return
  }

  if (!selectedFile.value || fileError.value) {
    return
  }

  submitting.value = true
  submitError.value = ''

  try {
    const res = await uploadResume(selectedFile.value)
    const taskId = String(res.data || '')

    ElMessage({
      message: '简历已提交，正在诊断中...',
      type: 'success',
      duration: 2000,
      showClose: true
    })

    if (!taskId) {
      submitError.value = '任务创建成功，但未获取到任务 ID'
      return
    }

    await router.push(`/resume/result/${taskId}`)
  } catch (err) {
    console.error('上传失败:', err)
    submitError.value = err?.message || '提交失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

const retrySubmit = () => {
  submitError.value = ''
  handleSubmit()
}
</script>

<style scoped>
.resume-upload-view {
  min-height: 100%;
}

.login-prompt {
  margin-bottom: 24px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  margin: 0 0 6px 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-title);
}

.page-desc {
  margin: 0;
  font-size: 14px;
  color: var(--text-muted);
}

.upload-section {
  margin-bottom: 24px;
}

.upload-card {
  background-color: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.06);
}

.upload-area {
  margin-bottom: 20px;
}

.upload-area :deep(.el-upload-dragger) {
  width: 100%;
  border: 2px dashed var(--orange-border);
  border-radius: 12px;
  background-color: var(--bg-page);
  padding: 40px 20px;
  transition: all 0.25s ease;
}

.upload-area :deep(.el-upload-dragger:hover) {
  border-color: var(--orange-main);
  background-color: var(--orange-light-bg);
}

.upload-placeholder {
  text-align: center;
}

.upload-icon {
  margin-bottom: 12px;
  color: var(--orange-main);
}

.upload-title {
  font-size: 15px;
  color: var(--text-body);
  margin-bottom: 6px;
}

.upload-hint {
  font-size: 13px;
  color: var(--text-muted);
}

.upload-selected {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-icon {
  color: var(--orange-main);
}

.file-detail {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.file-name {
  font-size: 14px;
  color: var(--text-body);
}

.file-size {
  font-size: 12px;
  color: var(--text-muted);
}

.file-error {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  background-color: #fef0f0;
  border: 1px solid var(--orange-border);
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 13px;
  color: var(--color-danger);
}

.file-requirements {
  margin-bottom: 24px;
}

.req-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-body);
  margin-bottom: 10px;
}

.req-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.req-list li {
  font-size: 13px;
  color: var(--text-muted);
  padding: 4px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.req-list li::before {
  content: '';
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background-color: var(--orange-main);
}

.req-warning {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding: 8px 12px;
  background-color: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 6px;
  font-size: 13px;
  color: var(--color-danger);
}

.submit-section {
  margin-bottom: 20px;
}

.submit-section :deep(.btn-loading) {
  position: relative;
  overflow: hidden;
}

.submit-section :deep(.btn-loading::after) {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, transparent, #fff, transparent);
  animation: loading-bar 1.5s ease-in-out infinite;
}

@keyframes loading-bar {
  0% {
    transform: translateX(-100%);
  }

  100% {
    transform: translateX(100%);
  }
}

.submit-error {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  background-color: #fef0f0;
  border: 1px solid var(--orange-border);
  border-radius: 8px;
}

.error-content {
  flex: 1;
}

.error-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-danger);
  margin-bottom: 6px;
}

.error-desc {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 12px;
}

.info-section {
  margin-bottom: 24px;
}

.info-card {
  background-color: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.06);
}

.info-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-title);
}

.info-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.info-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: var(--orange-main);
  flex-shrink: 0;
}

.info-text {
  font-size: 14px;
  color: var(--text-body);
}

@media (max-width: 768px) {
  .page-title {
    font-size: 20px;
  }

  .upload-card {
    padding: 24px 20px;
  }

  .info-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 18px;
  }

  .page-desc {
    font-size: 13px;
  }

  .upload-card {
    padding: 20px 16px;
  }

  .upload-area :deep(.el-upload-dragger) {
    padding: 24px 16px;
  }

  .upload-title {
    font-size: 14px;
  }
}
</style>
