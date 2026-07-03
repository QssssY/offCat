<template>
  <div class="industry-filter">
    <button
      v-for="ind in industries"
      :key="ind.id"
      class="filter-tag"
      :class="{ active: modelValue === ind.id }"
      @click="$emit('update:modelValue', ind.id)"
    >
      {{ ind.name }}
    </button>
  </div>
</template>

<script setup>
defineProps({
  industries: { type: Array, required: true },
  modelValue: { type: String, default: 'all' }
})

defineEmits(['update:modelValue'])
</script>

<style scoped>
.industry-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 28px;
}

.filter-tag {
  padding: 8px 20px;
  border: 1px solid var(--border-card);
  border-radius: 20px;
  background: var(--bg-card);
  color: var(--text-body);
  font-size: 14px;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    color 0.2s ease,
    background-color 0.2s ease;
}

.filter-tag:hover {
  border-color: var(--orange-main);
  color: var(--orange-main);
}

.filter-tag.active {
  background: var(--orange-main);
  border-color: var(--orange-main);
  color: #fff;
}
</style>
