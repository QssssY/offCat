<template>
  <div class="skill-tag-input">
    <div class="tags-list">
      <el-tag
        v-for="(skill, index) in modelValue"
        :key="index"
        closable
        @close="removeSkill(index)"
      >
        {{ skill }}
      </el-tag>
    </div>
    <div class="input-row">
      <el-input
        v-model="inputValue"
        size="small"
        placeholder="输入技能名称"
        @keyup.enter="addSkill"
      />
      <el-button size="small" @click="addSkill" :disabled="!inputValue.trim()">添加</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  modelValue: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:modelValue'])
const inputValue = ref('')

function addSkill() {
  const skill = inputValue.value.trim()
  if (!skill) return
  if (props.modelValue.includes(skill)) return
  emit('update:modelValue', [...props.modelValue, skill])
  inputValue.value = ''
}

function removeSkill(index) {
  const newList = [...props.modelValue]
  newList.splice(index, 1)
  emit('update:modelValue', newList)
}
</script>

<style scoped>
.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.input-row {
  display: flex;
  gap: 8px;
}
</style>
