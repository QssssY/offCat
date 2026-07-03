<template>
  <!-- 兴趣爱好编辑器 -->
  <div class="section-interests-editor">
    <div class="tag-list">
      <el-tag
        v-for="(item, i) in modelValue"
        :key="i"
        closable
        @close="removeAt(i)"
      >{{ item }}</el-tag>
    </div>
    <div class="add-row">
      <el-input
        v-model="inputVal"
        size="small"
        placeholder="如：阅读、跑步"
        @keyup.enter="addItem"
      />
      <el-button size="small" type="primary" @click="addItem">添加</el-button>
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

function addItem() {
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
.section-interests-editor {
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
