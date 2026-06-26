<template>
  <span
    class="notification-type-icon"
    :class="[`type-${meta.key}`, `size-${size}`, { 'notification-icon-halo': halo }]"
  >
    <FeatureIcon :name="meta.featureIcon" :label="meta.label" :size="iconSize" />
  </span>
</template>

<script setup>
import { computed } from 'vue'
import FeatureIcon from '@/components/common/FeatureIcon.vue'
import { getNotificationTypeMeta } from '@/utils/notificationMeta'

const props = defineProps({
  type: {
    type: String,
    default: ''
  },
  size: {
    type: String,
    default: 'md'
  },
  halo: {
    type: Boolean,
    default: false
  }
})

const meta = computed(() => getNotificationTypeMeta(props.type))
const iconSize = computed(() => (props.size === 'sm' ? 'sm' : 'md'))
</script>

<style scoped>
.notification-type-icon {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 16px;
  overflow: visible;
  transform-origin: center;
  transition:
    transform 180ms cubic-bezier(0.22, 1, 0.36, 1),
    filter 180ms cubic-bezier(0.22, 1, 0.36, 1);
}

.notification-icon-halo::before {
  position: absolute;
  inset: 2px;
  content: '';
  border: 1px solid rgba(255, 176, 122, 0.22);
  border-radius: inherit;
  background:
    radial-gradient(circle at 28% 22%, rgba(255, 255, 255, 0.5), transparent 42%),
    rgba(255, 247, 239, 0.36);
  box-shadow:
    0 8px 22px rgba(132, 75, 32, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.42);
  pointer-events: none;
}

.notification-type-icon :deep(.feature-icon) {
  position: relative;
  z-index: 1;
}

.size-sm {
  width: 34px;
  height: 34px;
}

.size-md {
  width: 44px;
  height: 44px;
}

:global(html[data-theme="dark"]) .notification-icon-halo::before {
  border-color: rgba(255, 176, 122, 0.18);
  background:
    radial-gradient(circle at 28% 22%, rgba(255, 176, 122, 0.14), transparent 44%),
    rgba(255, 140, 66, 0.08);
  box-shadow:
    0 8px 22px rgba(0, 0, 0, 0.22),
    inset 0 1px 0 rgba(255, 220, 190, 0.08);
}

@media (prefers-reduced-motion: reduce) {
  .notification-type-icon {
    transition-duration: 0.01ms;
  }
}
</style>
