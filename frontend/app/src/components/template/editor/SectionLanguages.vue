<template>
  <!-- 语言能力编辑器 -->
  <div class="section-langs-editor">
    <div class="tag-list">
      <el-tag
        v-for="(lang, i) in modelValue"
        :key="i"
        closable
        @close="removeAt(i)"
      >{{ lang }}</el-tag>
    </div>
    <div class="add-row">
      <el-input
        v-model="inputVal"
        size="small"
        placeholder="如：英语 CET-6"
        @keyup.enter="addLang"
      />
      <el-button size="small" type="primary" @click="addLang">添加</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  modelValue: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue'])
const inputVal = ref('')

function addLang() {
  const val = inputVal.value.trim()
  if (!val || props.modelValue.includes(val)) return
  emit('update:modelValue', [...props.modelValue, val])
  inputVal.value = ''
}

function removeAt(index) {
  const list = [...props.modelValue]
  list.splice(index, 1)
  emit('update:modelValue', list)
}
</script>

<style scoped>
.section-langs-editor {
  padding: 16px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.add-row {
  display: flex;
  gap: 8px;
}
</style>
