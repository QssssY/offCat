<template>
  <div class="notification-type-icon" :class="[`type-${meta.key}`, `size-${size}`]">
    <svg v-if="meta.icon === 'document'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <polyline points="14 2 14 8 20 8" />
    </svg>
    <svg v-else-if="meta.icon === 'edit'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M12 20h9" />
      <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z" />
    </svg>
    <svg v-else-if="meta.icon === 'user'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
    </svg>
    <svg v-else-if="meta.icon === 'alert'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <circle cx="12" cy="12" r="10" />
      <line x1="12" y1="8" x2="12" y2="12" />
      <line x1="12" y1="16" x2="12.01" y2="16" />
    </svg>
    <svg v-else-if="meta.icon === 'calendar'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M8 2v4" />
      <path d="M16 2v4" />
      <rect x="3" y="4" width="18" height="18" rx="2" />
      <path d="M3 10h18" />
    </svg>
    <svg v-else-if="meta.icon === 'refresh'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M21 12a9 9 0 0 1-9 9" />
      <path d="M3 12a9 9 0 0 1 9-9" />
      <path d="M21 3v6h-6" />
      <path d="M3 21v-6h6" />
    </svg>
    <svg v-else-if="meta.icon === 'tool'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.1-3.1a6 6 0 0 1-7.9 7.9l-5.6 5.6a2.1 2.1 0 0 1-3-3l5.6-5.6a6 6 0 0 1 7.9-7.9l-3.1 3.1z" />
    </svg>
    <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
      <path d="M13.73 21a2 2 0 0 1-3.46 0" />
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { getNotificationTypeMeta } from '@/utils/notificationMeta'

const props = defineProps({
  type: {
    type: String,
    default: ''
  },
  size: {
    type: String,
    default: 'md'
  }
})

const meta = computed(() => getNotificationTypeMeta(props.type))
</script>

<style scoped>
.notification-type-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 10px;
}

.notification-type-icon svg {
  width: 50%;
  height: 50%;
}

.size-sm {
  width: 32px;
  height: 32px;
}

.size-md {
  width: 40px;
  height: 40px;
}

.type-resume {
  background: rgba(255, 140, 66, 0.1);
  color: var(--orange-main);
}

.type-polish,
.type-update {
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
}

.type-interview {
  background: rgba(103, 194, 58, 0.1);
  color: var(--color-success);
}

.type-quota,
.type-maintenance {
  background: rgba(245, 108, 108, 0.1);
  color: var(--color-danger);
}

.type-system,
.type-unknown {
  background: rgba(144, 147, 153, 0.1);
  color: var(--text-muted);
}

.type-activity {
  background: rgba(230, 162, 60, 0.12);
  color: #e6a23c;
}
</style>
