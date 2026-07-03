<template>
  <div class="section-basic-info">
    <div class="section-header" @click="expanded = !expanded">
      <span class="section-label">基本信息</span>
      <span class="expand-icon">{{ expanded ? '▼' : '▶' }}</span>
    </div>
    <div v-show="expanded" class="section-content">
      <!-- 照片上传 -->
      <div class="photo-upload-row">
        <div class="photo-preview" v-if="modelValue.photo">
          <img :src="modelValue.photo" alt="证件照" />
          <button class="photo-remove-btn" @click="update('photo', '')" title="移除照片">×</button>
        </div>
        <label class="photo-upload-btn" v-else>
          <input type="file" accept="image/png,image/jpeg,image/webp" class="photo-file-input" @change="onPhotoChange" />
          <span class="photo-upload-icon">📷</span>
          <span class="photo-upload-text">上传证件照</span>
        </label>
      </div>

      <el-form label-width="70px" size="default">
        <el-form-item label="姓名">
          <el-input :model-value="modelValue.name" @update:model-value="update('name', $event)" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="职位">
          <el-input :model-value="modelValue.title" @update:model-value="update('title', $event)" placeholder="请输入目标职位" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input :model-value="modelValue.phone" @update:model-value="update('phone', $event)" placeholder="请输入电话号码" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input :model-value="modelValue.email" @update:model-value="update('email', $event)" placeholder="请输入邮箱地址" />
        </el-form-item>
        <el-form-item label="所在地">
          <el-input :model-value="modelValue.location" @update:model-value="update('location', $event)" placeholder="请输入所在城市" />
        </el-form-item>
        <el-form-item label="网站">
          <el-input :model-value="modelValue.website" @update:model-value="update('website', $event)" placeholder="请输入个人网站" />
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { isResumePhotoFileTooLarge, RESUME_PHOTO_SIZE_LIMIT_TEXT } from '@/utils/resumePhoto'

const props = defineProps({
  modelValue: { type: Object, required: true }
})

const emit = defineEmits(['update:modelValue'])
const expanded = ref(true)

function update(field, value) {
  emit('update:modelValue', { ...props.modelValue, [field]: value })
}

function onPhotoChange(event) {
  const file = event.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    event.target.value = ''
    return
  }
  if (isResumePhotoFileTooLarge(file.size)) {
    ElMessage.warning(`照片文件大小不能超过 ${RESUME_PHOTO_SIZE_LIMIT_TEXT}`)
    event.target.value = ''
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    update('photo', typeof reader.result === 'string' ? reader.result : '')
    event.target.value = ''
  }
  reader.readAsDataURL(file)
}
</script>

<style scoped>
.section-basic-info {
  border: 1px solid var(--border-card);
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 12px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--bg-card-hover);
  cursor: pointer;
  user-select: none;
}

.section-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-title);
}

.expand-icon {
  font-size: 12px;
  color: var(--text-muted);
}

.section-content {
  padding: 16px;
  background: var(--bg-card);
}

.photo-upload-row {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.photo-preview {
  position: relative;
  width: 90px;
  height: 112px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid var(--border-card);
}

.photo-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.photo-remove-btn {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 14px;
  line-height: 20px;
  text-align: center;
  cursor: pointer;
  padding: 0;
}

.photo-upload-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 90px;
  height: 112px;
  border: 1.5px dashed var(--border-card);
  border-radius: 6px;
  cursor: pointer;
  transition: border-color 0.2s;
  background: var(--bg-card-hover);
}

.photo-upload-btn:hover {
  border-color: var(--resume-accent, #1b5b57);
}

.photo-file-input {
  display: none;
}

.photo-upload-icon {
  font-size: 22px;
  margin-bottom: 4px;
}

.photo-upload-text {
  font-size: 11px;
  color: var(--text-muted);
}
</style>
