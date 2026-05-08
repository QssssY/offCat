<template>
  <div class="preview-image" :style="{ '--accent': color, '--bg': bgColor }">
    <!-- Gradient header variant -->
    <template v-if="headerStyle === 'gradient'">
      <div class="preview-header-gradient" :style="gradientHeaderStyle">
        <div class="preview-deco-circle"></div>
        <div class="preview-header-row">
          <div class="preview-avatar-circle" :style="{ borderColor: 'rgba(255,255,255,0.3)' }"></div>
          <div class="preview-header-lines">
            <div class="preview-line name-line-white"></div>
            <div class="preview-line title-line-white"></div>
          </div>
        </div>
        <div class="preview-meta-white">
          <span class="meta-dot-white"></span>
          <span class="meta-dot-white"></span>
          <span class="meta-dot-white"></span>
        </div>
      </div>
    </template>

    <!-- Centered header variant -->
    <template v-else-if="headerStyle === 'centered'">
      <div class="preview-header-centered" :style="centeredHeaderStyle">
        <div class="preview-line name-line-centered" :style="{ background: 'rgba(255,255,255,0.85)' }"></div>
        <div class="preview-line title-line-centered" :style="{ background: secondaryColor }"></div>
        <div class="preview-meta-centered">
          <span class="meta-dot-white"></span>
          <span class="meta-dot-white"></span>
          <span class="meta-dot-white"></span>
        </div>
        <div class="centered-border" :style="{ background: secondaryColor }"></div>
      </div>
    </template>

    <!-- Clean header variant (default) -->
    <template v-else>
      <div class="preview-header-clean">
        <div class="preview-avatar-clean" :style="{ borderColor: color }"></div>
        <div class="preview-header-lines">
          <div class="preview-line name-line" :style="{ background: color }"></div>
          <div class="preview-line title-line"></div>
          <div class="preview-meta">
            <span class="meta-dot"></span>
            <span class="meta-dot"></span>
            <span class="meta-dot"></span>
          </div>
        </div>
      </div>
    </template>

    <!-- Body sections (shared across all variants) -->
    <div class="preview-body">
      <div class="preview-section" v-for="i in 3" :key="i">
        <div class="section-heading" :style="{ background: color }"></div>
        <div class="section-divider" :style="{ background: color }"></div>
        <div class="preview-line long"></div>
        <div class="preview-line medium"></div>
        <div class="preview-line long"></div>
        <div v-if="i === 1" class="preview-tags">
          <span class="preview-tag" :style="{ borderColor: color }" v-for="j in 4" :key="j"></span>
        </div>
        <template v-if="i >= 2">
          <div class="preview-exp">
            <div class="preview-line short" :style="{ background: `${color}33` }"></div>
            <div class="preview-line long"></div>
            <div class="preview-line medium"></div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  color: { type: String, default: '#3B82F6' },
  bgColor: { type: String, default: '#ffffff' },
  templateId: { type: String, default: '' }
})

const gradientTemplates = [
  'tech-modern', 'tech-dark', 'design-creative', 'marketing-vibrant',
  'education-warm', 'finance-gold', 'medical-professional', 'marketing-bold',
  'manufacture-precision'
]
const centeredTemplates = ['finance-classic', 'legal-authoritative']

const headerStyle = computed(() => {
  if (centeredTemplates.includes(props.templateId)) return 'centered'
  if (gradientTemplates.includes(props.templateId)) return 'gradient'
  return 'clean'
})

const isDark = computed(() =>
  ['tech-dark', 'finance-classic', 'legal-authoritative'].includes(props.templateId)
)

const secondaryColor = computed(() => {
  if (props.templateId === 'finance-classic') return '#D4AF37'
  return props.color
})

function darkenColor(hex, amount = 0.2) {
  const num = parseInt(hex.replace('#', ''), 16)
  const r = Math.max(0, (num >> 16) - Math.round(255 * amount))
  const g = Math.max(0, ((num >> 8) & 0x00FF) - Math.round(255 * amount))
  const b = Math.max(0, (num & 0x0000FF) - Math.round(255 * amount))
  return `#${(r << 16 | g << 8 | b).toString(16).padStart(6, '0')}`
}

const gradientHeaderStyle = computed(() => {
  const dark = darkenColor(props.color, 0.15)
  return { background: `linear-gradient(135deg, ${props.color} 0%, ${dark} 100%)` }
})

const centeredHeaderStyle = computed(() => {
  const bg = props.templateId === 'finance-classic' ? '#1E3A5F' : '#111827'
  return { background: bg }
})
</script>

<style scoped>
.preview-image {
  width: 100%;
  height: 100%;
  background: var(--bg);
  padding: 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

/* ===== Clean header (default) ===== */
.preview-header-clean {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 12px 8px;
  border-bottom: 2px solid var(--accent);
}

.preview-avatar-clean {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  flex-shrink: 0;
  border: 2px solid;
  background: transparent;
}

/* ===== Gradient header ===== */
.preview-header-gradient {
  padding: 10px 12px 8px;
  position: relative;
  overflow: hidden;
}

.preview-deco-circle {
  position: absolute;
  top: -10px;
  right: -8px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.preview-header-row {
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
  z-index: 1;
}

.preview-avatar-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  flex-shrink: 0;
  border: 2px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.15);
}

.name-line-white {
  width: 50%;
  height: 10px;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.85);
}

.title-line-white {
  width: 65%;
  height: 6px;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.4);
}

.preview-meta-white {
  display: flex;
  gap: 6px;
  margin-top: 4px;
  position: relative;
  z-index: 1;
}

.meta-dot-white {
  width: 30px;
  height: 4px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.3);
}

/* ===== Centered header ===== */
.preview-header-centered {
  padding: 12px 12px 8px;
  text-align: center;
  position: relative;
}

.preview-header-lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.name-line-centered {
  width: 45%;
  height: 10px;
  border-radius: 3px;
  margin: 0 auto;
}

.title-line-centered {
  width: 35%;
  height: 8px;
  border-radius: 999px;
  margin: 0 auto;
  opacity: 0.8;
}

.preview-meta-centered {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-top: 4px;
}

.centered-border {
  height: 3px;
  margin-top: 6px;
  border-radius: 1px;
}

/* ===== Shared header lines ===== */
.name-line {
  width: 50%;
  height: 10px;
  border-radius: 3px;
  opacity: 0.8;
}

.title-line {
  width: 70%;
  height: 6px;
  border-radius: 3px;
  background: #d1d5db;
}

.preview-meta {
  display: flex;
  gap: 6px;
  margin-top: 2px;
}

.meta-dot {
  width: 36px;
  height: 4px;
  border-radius: 2px;
  background: #e5e7eb;
}

/* ===== Body ===== */
.preview-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 12px 12px;
}

.preview-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.section-heading {
  width: 28%;
  height: 8px;
  border-radius: 2px;
  opacity: 0.75;
}

.section-divider {
  width: 100%;
  height: 2px;
  border-radius: 1px;
  opacity: 0.3;
  margin-bottom: 2px;
}

.preview-line {
  height: 5px;
  border-radius: 2px;
  background: #e5e7eb;
}

.preview-line.long {
  width: 100%;
}

.preview-line.medium {
  width: 75%;
}

.preview-line.short {
  width: 40%;
  height: 6px;
  border-radius: 2px;
}

.preview-tags {
  display: flex;
  gap: 4px;
  margin-top: 2px;
}

.preview-tag {
  width: 28px;
  height: 14px;
  border-radius: 8px;
  border: 1px solid;
  opacity: 0.5;
}

.preview-exp {
  display: flex;
  flex-direction: column;
  gap: 3px;
  margin-top: 2px;
}
</style>
