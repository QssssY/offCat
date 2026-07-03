<template>
  <span
    class="feature-icon"
    :class="[`size-${size}`, { 'feature-icon-halo': hasHalo }]"
    aria-hidden="true"
  >
    <picture>
      <source v-if="iconSources.webp" :srcset="iconSources.webp" type="image/webp" />
      <img
        :src="iconSources.png"
        :alt="resolvedLabel"
        :loading="resolvedLoading"
        decoding="async"
        :fetchpriority="resolvedFetchPriority"
      />
    </picture>
  </span>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import {
  getCriticalFeatureIconSource,
  getFeatureIconLabel,
  getFeatureIconSource,
  loadFeatureIconSource
} from '@/utils/featureIcons'

const props = defineProps({
  name: {
    type: String,
    required: true
  },
  label: {
    type: String,
    default: ''
  },
  size: {
    type: String,
    default: 'sm'
  },
  halo: {
    type: Boolean,
    default: false
  },
  critical: {
    type: Boolean,
    default: false
  },
  loading: {
    type: String,
    default: '',
    validator: (value) => ['', 'lazy', 'eager'].includes(value)
  },
  fetchPriority: {
    type: String,
    default: '',
    validator: (value) => ['', 'high', 'low', 'auto'].includes(value)
  }
})

const iconSources = ref(getFeatureIconSource(props.name))
const resolvedLabel = computed(() => props.label || getFeatureIconLabel(props.name))
const hasHalo = computed(() => props.halo)
const resolvedLoading = computed(() => props.loading || (props.critical ? 'eager' : 'lazy'))
// 关键首屏图标同步命中，非首屏图标延后异步加载，避免整套插画进入首屏包。
const resolvedFetchPriority = computed(() => props.fetchPriority || (props.critical ? 'high' : null))
let loadSequence = 0

watch(
  () => props.name,
  async (name) => {
    const sequence = ++loadSequence
    const criticalSource = getCriticalFeatureIconSource(name)

    if (criticalSource) {
      iconSources.value = criticalSource
      return
    }

    iconSources.value = getFeatureIconSource(name)
    const loadedSource = await loadFeatureIconSource(name)
    if (sequence === loadSequence) {
      iconSources.value = loadedSource
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.feature-icon {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  overflow: visible;
  line-height: 0;
  border-radius: 18px;
  transform-origin: center;
  transition: transform 180ms cubic-bezier(0.22, 1, 0.36, 1);
}

.feature-icon-halo::before {
  position: absolute;
  inset: -8px;
  z-index: 0;
  content: '';
  border: 1px solid rgba(255, 176, 122, 0.22);
  border-radius: 22px;
  background:
    radial-gradient(circle at 32% 24%, rgba(255, 255, 255, 0.58), transparent 42%),
    rgba(255, 247, 239, 0.42);
  box-shadow:
    0 10px 26px rgba(132, 75, 32, 0.09),
    inset 0 1px 0 rgba(255, 255, 255, 0.48);
  pointer-events: none;
}

.feature-icon-halo.size-xs::before {
  inset: -4px;
  border-radius: 14px;
}

.feature-icon-halo.size-sm::before {
  inset: -5px;
  border-radius: 16px;
}

.feature-icon-halo.size-md::before {
  inset: -6px;
  border-radius: 18px;
}

.feature-icon picture,
.feature-icon img {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
}

.feature-icon picture {
  display: block;
}

.feature-icon img {
  object-fit: contain;
  display: block;
}

.size-xs {
  width: 22px;
  height: 22px;
}

.size-sm {
  width: 28px;
  height: 28px;
}

.size-md {
  width: 40px;
  height: 40px;
}

.size-lg {
  width: 64px;
  height: 64px;
}

.size-xl {
  width: 88px;
  height: 88px;
}

:global(html[data-theme="dark"]) .feature-icon-halo::before {
  border-color: rgba(255, 176, 122, 0.18);
  background:
    radial-gradient(circle at 32% 24%, rgba(255, 176, 122, 0.16), transparent 44%),
    rgba(255, 140, 66, 0.08);
  box-shadow:
    0 10px 26px rgba(0, 0, 0, 0.22),
    inset 0 1px 0 rgba(255, 220, 190, 0.08);
}

@media (prefers-reduced-motion: reduce) {
  .feature-icon {
    transition-duration: 0.01ms;
  }
}
</style>
