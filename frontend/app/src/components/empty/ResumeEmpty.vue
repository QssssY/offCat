<template>
  <div class="empty-illustration" :style="illustrationStyle">
    <FeatureIcon name="resume-score" size="xl" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import FeatureIcon from '@/components/common/FeatureIcon.vue'

const props = defineProps({
  size: {
    type: [Number, String],
    default: 120
  },
  bgColor: {
    type: String,
    default: '#FFF8F3'
  }
})

const illustrationStyle = computed(() => ({
  width: typeof props.size === 'number' ? `${props.size}px` : props.size,
  height: typeof props.size === 'number' ? `${props.size}px` : props.size,
  background: props.bgColor
}))
</script>

<style scoped>
.empty-illustration {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}
</style>
