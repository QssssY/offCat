<template>
  <!-- 证书资质编辑器 -->
  <div class="section-certs-editor">
    <div
      v-for="item in modelValue"
      :key="item.id"
      class="cert-item"
    >
      <div class="cert-fields">
        <el-input
          :model-value="item.name"
          @update:model-value="updateItem(item.id, 'name', $event)"
          placeholder="证书名称"
          size="small"
        />
        <el-input
          :model-value="item.issuer"
          @update:model-value="updateItem(item.id, 'issuer', $event)"
          placeholder="颁发机构"
          size="small"
        />
        <el-input
          :model-value="item.date"
          @update:model-value="updateItem(item.id, 'date', $event)"
          placeholder="获得时间"
          size="small"
        />
      </div>
      <el-button
        text
        type="danger"
        size="small"
        @click="$emit('remove-item', item.id)"
      >删除</el-button>
    </div>
    <el-button
      text
      type="primary"
      size="small"
      @click="$emit('add-item')"
    >+ 添加证书</el-button>
  </div>
</template>

<script setup>
const props = defineProps({
  modelValue: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue', 'add-item', 'remove-item'])

function updateItem(id, field, value) {
  const list = [...props.modelValue]
  const idx = list.findIndex(item => item.id === id)
  if (idx !== -1) {
    list[idx] = { ...list[idx], [field]: value }
    emit('update:modelValue', list)
  }
}
</script>

<style scoped>
.section-certs-editor {
  padding: 16px;
}

.cert-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 10px;
}

.cert-fields {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
</style>
