<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="460px"
    :close-on-click-modal="false"
    append-to-body
    class="admin-user-ban-dialog"
    @update:model-value="$emit('update:modelValue', $event)"
    @closed="resetForm"
  >
    <div class="ban-target" v-if="targetName">
      <span class="ban-target-label">目标用户</span>
      <strong>{{ targetName }}</strong>
    </div>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
      <el-form-item label="封禁时长" prop="duration">
        <el-radio-group v-model="form.duration" :disabled="saving">
          <el-radio-button
            v-for="option in ADMIN_BAN_DURATION_OPTIONS"
            :key="option.value"
            :label="option.value"
          >
            {{ option.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="封禁原因" prop="reason">
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="4"
          maxlength="200"
          show-word-limit
          placeholder="请输入 200 字以内的封禁原因"
          :disabled="saving"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="saving" @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="danger" :loading="saving" @click="submit">确认封禁</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import {
  ADMIN_BAN_DURATION_OPTIONS,
  normalizeAdminBanReason,
  validateAdminBanReason
} from '@/utils/adminUserBan'

defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  targetName: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    default: '封禁用户'
  },
  saving: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'submit'])

const formRef = ref(null)
const form = reactive({
  duration: '7d',
  reason: ''
})

const rules = {
  duration: [{ required: true, message: '请选择封禁时长', trigger: 'change' }],
  reason: [{
    validator: (_rule, value, callback) => {
      const result = validateAdminBanReason(value)
      if (result === true) callback()
      else callback(new Error(result))
    },
    trigger: 'blur'
  }]
}

const resetForm = () => {
  form.duration = '7d'
  form.reason = ''
  formRef.value?.clearValidate?.()
}

const submit = async () => {
  const valid = await formRef.value?.validate?.().catch(() => false)
  if (!valid) return
  emit('submit', {
    duration: form.duration,
    reason: normalizeAdminBanReason(form.reason)
  })
}
</script>

<style scoped>
.ban-target {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
  padding: 10px 12px;
  border: 1px solid rgba(245, 108, 108, 0.18);
  border-radius: 8px;
  background: rgba(245, 108, 108, 0.06);
  color: var(--text-body);
}

.ban-target-label {
  color: var(--text-muted);
  font-size: 13px;
}

.admin-user-ban-dialog :deep(.el-radio-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
