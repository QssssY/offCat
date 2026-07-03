<template>
  <div class="post-editor">
    <!-- 板块选择 -->
    <div class="editor-field">
      <label class="field-label">发布板块</label>
      <div class="category-selector">
        <button
          class="cat-option"
          :class="{ active: form.category === 'interview_exp' }"
          @click="form.category = 'interview_exp'"
        >
          <FeatureIcon name="interview-replay" size="sm" />
          面试经验分享
          <!-- 【选中对勾】激活时显示的圆形对勾标记 -->
          <span v-if="form.category === 'interview_exp'" class="cat-check">
            <FeatureIcon name="success" size="xs" />
          </span>
        </button>
        <button
          class="cat-option"
          :class="{ active: form.category === 'referral' }"
          @click="form.category = 'referral'"
        >
          <FeatureIcon name="offer-comparison" size="sm" />
          内推广场
          <!-- 【选中对勾】激活时显示的圆形对勾标记 -->
          <span v-if="form.category === 'referral'" class="cat-check">
            <FeatureIcon name="success" size="xs" />
          </span>
        </button>
      </div>
    </div>

    <!-- 脱敏提示（面试经验板块） -->
    <div v-if="form.category === 'interview_exp'" class="privacy-tip">
      <FeatureIcon name="privacy" size="sm" class="tip-icon" />
      <div class="tip-content">
        <strong>隐私保护提醒</strong>
        <p>请对以下信息进行脱敏处理：公司名称（可用"某互联网公司"代替）、面试官姓名、个人联系方式、具体薪资数字等。保护您和他人的隐私。</p>
      </div>
    </div>

    <!-- 内容输入 -->
    <div class="editor-field">
      <label class="field-label">帖子标题</label>
      <input
        v-model="form.title"
        class="title-input"
        type="text"
        maxlength="120"
        :placeholder="form.category === 'interview_exp'
          ? '例如：一次 Java 后端一面的复盘'
          : '例如：深圳前端岗位内推，偏 Vue 方向'"
      />
    </div>

    <!-- 内容输入 -->
    <div class="editor-field">
      <label class="field-label">帖子内容</label>
      <textarea
        v-model="form.content"
        class="content-textarea"
        :placeholder="form.category === 'interview_exp'
          ? '分享你的面试经历，例如：面试流程、考察重点、注意事项等...'
          : '发布内推信息，例如：公司名称、岗位要求、投递方式等...'"
        rows="6"
        maxlength="2000"
      />
      <div class="textarea-footer">
        <span class="char-count">{{ form.content.length }}/2000</span>
      </div>
    </div>

    <!-- 图片上传 -->
    <div class="editor-field">
      <label class="field-label">上传图片 <span class="field-hint">（选填，最多9张）</span></label>
      <div class="image-upload-area">
        <div
          v-for="(img, index) in form.images"
          :key="index"
          class="upload-preview"
        >
          <img :src="img" />
          <button class="remove-btn" @click="removeImage(index)">
            <FeatureIcon name="close" size="xs" />
          </button>
        </div>
        <label
          v-if="form.images.length < 9"
          class="upload-trigger"
          :class="{ uploading: imageUploading }"
        >
          <input
            type="file"
            accept="image/*"
            multiple
            hidden
            @change="handleImageUpload"
          />
          <FeatureIcon v-if="!imageUploading" name="image-upload" size="sm" />
          <div v-else class="upload-loading">
            <div class="loading-spinner-sm"></div>
          </div>
          <span v-if="!imageUploading">添加图片</span>
        </label>
      </div>
    </div>

    <!-- 提交按钮 -->
    <div class="editor-actions">
      <el-button size="large" @click="$emit('cancel')">取消</el-button>
      <el-button
        type="primary"
        size="large"
        :disabled="!canSubmit"
        :loading="submitting"
        @click="handleSubmit"
      >
        发布帖子
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { createPost, uploadPostImage } from '@/api/community'
import FeatureIcon from '@/components/common/FeatureIcon.vue'

const emit = defineEmits(['published', 'cancel'])

const form = ref({
  category: 'interview_exp',
  title: '',
  content: '',
  images: []
})

const imageUploading = ref(false)
const submitting = ref(false)

const canSubmit = computed(() => {
  return form.value.title.trim().length > 0 && form.value.content.trim().length > 0 && !submitting.value
})

const handleImageUpload = async (e) => {
  const files = Array.from(e.target.files)
  if (!files.length) return

  const remaining = 9 - form.value.images.length
  const toUpload = files.slice(0, remaining)

  imageUploading.value = true
  try {
    const validFiles = toUpload.filter(file => {
      if (file.size > 2 * 1024 * 1024) {
        ElMessage.warning('单张图片不能超过2MB')
        return false
      }
      return true
    })

    if (validFiles.length > 0) {
      const results = await Promise.allSettled(
        validFiles.map(file => uploadPostImage(file))
      )
      results.forEach(result => {
        if (result.status === 'fulfilled' && result.value?.code === 200 && result.value.data?.url) {
          form.value.images.push(result.value.data.url)
        }
      })
    }
  } catch (err) {
    ElMessage.error('图片上传失败')
  } finally {
    imageUploading.value = false
    e.target.value = ''
  }
}

const removeImage = (index) => {
  form.value.images.splice(index, 1)
}

const handleSubmit = async () => {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    const res = await createPost({
      category: form.value.category,
      title: form.value.title.trim(),
      content: form.value.content.trim(),
      images: form.value.images
    })
    const reviewStatus = res?.data?.reviewStatus
    ElMessage.success(reviewStatus === 'approved' ? '发布成功，已公开展示' : '已提交审核，通过后将在社区展示')
    emit('published')
  } catch (err) {
    console.error('发布失败:', err)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
/* ===== 发帖编辑器样式（UI美化版 v2） ===== */

.post-editor {
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
  letter-spacing: 0.2px;
}

.field-hint {
  font-weight: 400;
  color: var(--text-muted);
  font-size: 13px;
}

/* 【板块选择】双选项卡片式选择器 */
.category-selector {
  display: flex;
  gap: 12px;
}

.cat-option {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 16px;
  border: 2px solid var(--border-input);
  border-radius: 12px;
  background: var(--bg-card);
  color: var(--text-body);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s cubic-bezier(0.4, 0, 0.2, 1), color 0.3s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.3s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.cat-option:hover {
  border-color: var(--orange-main);
  color: var(--orange-main);
}

/* 【板块激活态】品牌色边框 + 柔和背景 + 发光效果 */
.cat-option.active {
  border-color: var(--orange-main);
  background: var(--orange-light-bg);
  color: var(--orange-main);
  box-shadow: 0 0 0 3px rgba(255, 140, 66, 0.1);
}

.cat-option svg {
  width: 18px;
  height: 18px;
}

/* 【选中对勾】右上角圆形对勾标记 */
.cat-check {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--orange-main);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(255, 140, 66, 0.4);
  animation: check-pop 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.cat-check svg {
  width: 12px;
  height: 12px;
  color: #fff;
}

/* 【对勾弹出动画】从小到大弹出 */
@keyframes check-pop {
  from {
    transform: scale(0);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

/* 【脱敏提示】带左侧橙色色条的提示卡片 */
.privacy-tip {
  display: flex;
  gap: 10px;
  padding: 14px 16px;
  background: linear-gradient(135deg, var(--orange-light-bg) 0%, rgba(255, 140, 66, 0.05) 100%);
  border: 1px solid var(--orange-border);
  border-left: 3px solid var(--orange-main);
  border-radius: 4px 12px 12px 4px;
  align-items: flex-start;
}

.tip-icon {
  width: 20px;
  height: 20px;
  color: var(--orange-main);
  flex-shrink: 0;
  margin-top: 1px;
}

.tip-content {
  flex: 1;
}

.tip-content strong {
  display: block;
  font-size: 13px;
  color: var(--orange-deep);
  margin-bottom: 4px;
}

.tip-content p {
  font-size: 12px;
  color: var(--text-muted);
  margin: 0;
  line-height: 1.6;
}

/* 【内容输入】聚焦时品牌色边框 + 光晕 */
.content-textarea {
  width: 100%;
  padding: 12px 14px;
  border: 2px solid var(--border-input);
  border-radius: 12px;
  font-size: 14px;
  color: var(--text-title);
  background: var(--bg-input);
  resize: vertical;
  min-height: 140px;
  font-family: inherit;
  line-height: 1.7;
  transition: border-color 0.3s, box-shadow 0.3s;
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

.content-textarea:focus {
  outline: none;
  border-color: var(--orange-main);
  box-shadow: 0 0 0 3px rgba(255, 140, 66, 0.12);
}

.content-textarea::placeholder {
  color: var(--text-placeholder);
}

.textarea-footer {
  display: flex;
  justify-content: flex-end;
}

.char-count {
  font-size: 12px;
  color: var(--text-placeholder);
}

/* 【图片上传】虚线边框触发器，悬停时品牌色高亮 */
.image-upload-area {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.upload-preview {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--border-divider);
  transition: border-color 0.2s ease;
}

.upload-preview:hover {
  border-color: var(--orange-border);
}

.upload-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  border: none;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.remove-btn:hover {
  background: rgba(0, 0, 0, 0.7);
}

.remove-btn svg {
  width: 12px;
  height: 12px;
}

.upload-trigger {
  width: 80px;
  height: 80px;
  border: 2px dashed var(--border-input);
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  color: var(--text-placeholder);
  font-size: 11px;
  transition: background-color 0.3s cubic-bezier(0.4, 0, 0.2, 1), color 0.3s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.upload-trigger:hover {
  border-color: var(--orange-main);
  color: var(--orange-main);
  background: var(--orange-light-bg);
}

.upload-trigger.uploading {
  pointer-events: none;
  opacity: 0.6;
}

.upload-trigger svg {
  width: 20px;
  height: 20px;
}

.upload-loading {
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-spinner-sm {
  width: 18px;
  height: 18px;
  border: 2px solid var(--border-divider);
  border-top-color: var(--orange-main);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 【提交按钮】底部操作栏 */
.editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-divider);
}
</style>
