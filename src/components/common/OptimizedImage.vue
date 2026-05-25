<template>
  <picture :class="['optimized-picture', pictureClass]">
    <source v-if="sources.webp" :srcset="sources.webp" type="image/webp" />
    <img
      :src="sources.png"
      :alt="alt"
      :class="imgClass"
      :loading="loading"
      decoding="async"
      :fetchpriority="fetchPriority || null"
      @error="$emit('error', $event)"
    />
  </picture>
</template>

<script setup>
defineEmits(['error'])

defineProps({
  sources: {
    type: Object,
    required: true
  },
  alt: {
    type: String,
    default: ''
  },
  imgClass: {
    type: [String, Array, Object],
    default: ''
  },
  pictureClass: {
    type: [String, Array, Object],
    default: ''
  },
  loading: {
    type: String,
    default: 'lazy',
    validator: (value) => ['lazy', 'eager'].includes(value)
  },
  fetchPriority: {
    type: String,
    default: '',
    validator: (value) => ['', 'high', 'low', 'auto'].includes(value)
  }
})
</script>

<style scoped>
.optimized-picture {
  display: contents;
}
</style>
