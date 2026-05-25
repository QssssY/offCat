<template>
  <div
    class="preview-image"
    :class="previewClasses"
    :style="previewStyle"
    :data-template-id="templateId"
  >
    <aside v-if="preview.layout === 'sidebar'" class="preview-sidebar">
      <div v-if="preview.photoPlacement === 'sidebar'" class="preview-photo sidebar-photo"></div>
      <div class="sidebar-line wide"></div>
      <div class="sidebar-line"></div>
      <div class="sidebar-block">
        <span v-for="i in 4" :key="`side-${i}`"></span>
      </div>
    </aside>

    <div class="preview-page">
      <header class="preview-header">
        <div
          v-if="preview.photoPlacement !== 'none' && preview.layout !== 'sidebar'"
          class="preview-photo"
        ></div>
        <div class="preview-title-stack">
          <div class="name-line"></div>
          <div class="role-line"></div>
          <div class="meta-row">
            <span v-for="i in 3" :key="`meta-${i}`"></span>
          </div>
        </div>
      </header>

      <main class="preview-body">
        <section v-for="i in 3" :key="`section-${i}`" class="preview-section">
          <div class="section-title">
            <span class="section-dot"></span>
            <span class="section-label"></span>
          </div>
          <div class="section-divider"></div>
          <div class="line long"></div>
          <div class="line medium"></div>
          <div class="line long"></div>
          <div v-if="i === 1" class="tag-row">
            <span v-for="j in 4" :key="`tag-${j}`"></span>
          </div>
          <div v-if="i > 1" class="compact-item">
            <span></span>
            <div class="line short"></div>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { getTemplatePreviewMeta } from '@/data/templatePreviewMeta'

const props = defineProps({
  color: { type: String, default: '#3B82F6' },
  bgColor: { type: String, default: '#ffffff' },
  templateId: { type: String, default: '' }
})

const preview = computed(() => getTemplatePreviewMeta(props.templateId, {
  color: props.color,
  bgColor: props.bgColor
}))

const previewClasses = computed(() => [
  `layout-${preview.value.layout}`,
  `header-${preview.value.headerStyle}`,
  `section-${preview.value.sectionStyle}`,
  `photo-${preview.value.photoPlacement}`,
  { 'is-dark': preview.value.dark }
])

const previewStyle = computed(() => ({
  '--preview-accent': preview.value.accent,
  '--preview-accent-soft': preview.value.accentSoft,
  '--preview-bg': preview.value.background,
  '--preview-surface': preview.value.surface,
  '--preview-text': preview.value.textTone,
  '--preview-role': preview.value.role,
  '--preview-muted': preview.value.muted,
  '--preview-line': preview.value.line,
  '--preview-line-strong': preview.value.lineStrong,
  '--preview-border': preview.value.border,
  '--preview-divider': preview.value.divider,
  '--preview-tag-bg': preview.value.tagBg,
  '--preview-tag-border': preview.value.tagBorder,
  '--preview-pill-bg': preview.value.pillBg,
  '--preview-pill-border': preview.value.pillBorder,
  '--preview-photo-bg': preview.value.photoBg,
  '--preview-gradient-end': preview.value.gradientEnd,
  '--preview-dark-glow': preview.value.darkGlow,
  '--preview-dark-surface': preview.value.darkSurface,
  '--preview-dark-line': preview.value.darkLine,
  '--preview-dark-border': preview.value.darkBorder
}))
</script>

<style scoped>
.preview-image {
  width: 100%;
  height: 100%;
  display: flex;
  overflow: hidden;
  background: var(--preview-bg);
  color: var(--preview-text);
  box-sizing: border-box;
}

.preview-page {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--preview-bg);
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 12px 9px;
  background: var(--preview-surface);
  border-bottom: 2px solid var(--preview-border);
}

.preview-title-stack {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.preview-photo {
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  border-radius: 50%;
  border: 2px solid var(--preview-accent);
  background:
    linear-gradient(135deg, var(--preview-photo-bg), transparent),
    var(--preview-surface);
}

.name-line,
.role-line,
.meta-row span,
.sidebar-line,
.sidebar-block span,
.line,
.section-label,
.tag-row span,
.compact-item span {
  display: block;
  border-radius: 999px;
}

.name-line {
  width: 52%;
  height: 10px;
  background: var(--preview-accent);
}

.role-line {
  width: 68%;
  height: 6px;
  background: var(--preview-role);
}

.meta-row {
  display: flex;
  gap: 6px;
}

.meta-row span {
  width: 34px;
  height: 4px;
  background: var(--preview-muted);
}

.preview-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 9px 12px 12px;
}

.preview-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 5px;
}

.section-dot {
  width: 7px;
  height: 7px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: var(--preview-accent);
}

.section-label {
  width: 30%;
  height: 8px;
  background: var(--preview-accent);
}

.section-divider {
  width: 100%;
  height: 2px;
  border-radius: 999px;
  background: var(--preview-divider);
}

.line {
  height: 5px;
  background: var(--preview-line);
}

.line.long {
  width: 100%;
}

.line.medium {
  width: 76%;
}

.line.short {
  width: 42%;
  height: 6px;
  background: var(--preview-line-strong);
}

.tag-row {
  display: flex;
  gap: 4px;
  margin-top: 2px;
}

.tag-row span {
  width: 28px;
  height: 13px;
  border: 1px solid var(--preview-tag-border);
  background: var(--preview-tag-bg);
}

.compact-item {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 2px;
}

.compact-item span {
  width: 7px;
  height: 7px;
  background: var(--preview-accent);
}

.header-gradient .preview-header {
  color: #fff;
  background:
    radial-gradient(circle at 88% 0%, rgba(255, 255, 255, 0.22), transparent 28%),
    linear-gradient(135deg, var(--preview-accent), var(--preview-gradient-end));
  border-bottom: 0;
}

.header-gradient .preview-photo {
  border-color: rgba(255, 255, 255, 0.42);
  background: rgba(255, 255, 255, 0.18);
}

.header-gradient .name-line,
.header-gradient .role-line,
.header-gradient .meta-row span {
  background: rgba(255, 255, 255, 0.86);
}

.header-gradient .role-line,
.header-gradient .meta-row span {
  opacity: 0.58;
}

.header-centered .preview-header {
  display: block;
  text-align: center;
  background: var(--preview-accent);
  padding-top: 13px;
  border-bottom: 4px solid var(--preview-accent-soft);
}

.header-centered .name-line,
.header-centered .role-line {
  margin-inline: auto;
  background: rgba(255, 255, 255, 0.9);
}

.header-centered .role-line {
  width: 38%;
  background: var(--preview-accent-soft);
}

.header-centered .meta-row {
  justify-content: center;
}

.header-centered .meta-row span {
  background: rgba(255, 255, 255, 0.42);
}

.header-minimal .preview-header {
  align-items: flex-start;
  background: transparent;
  border-bottom: 1px solid var(--preview-border);
}

.header-minimal.photo-right .preview-header {
  flex-direction: row-reverse;
}

.header-minimal .preview-photo {
  border-radius: 6px;
  background: var(--preview-photo-bg);
}

.header-soft .preview-header {
  margin: 10px 10px 0;
  border: 1px solid var(--preview-divider);
  border-radius: 8px;
  background: var(--preview-surface);
}

.header-clean-band .preview-header {
  border-left: 8px solid var(--preview-accent);
}

.layout-sidebar .preview-page {
  background: var(--preview-bg);
}

.preview-sidebar {
  width: 34%;
  max-width: 72px;
  padding: 13px 10px;
  background: var(--preview-accent);
  box-sizing: border-box;
}

.sidebar-photo {
  width: 30px;
  height: 30px;
  margin-bottom: 12px;
  border-color: rgba(255, 255, 255, 0.54);
  background: rgba(255, 255, 255, 0.2);
}

.sidebar-line {
  width: 64%;
  height: 6px;
  margin-bottom: 6px;
  background: rgba(255, 255, 255, 0.72);
}

.sidebar-line.wide {
  width: 86%;
  height: 9px;
}

.sidebar-block {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin-top: 14px;
}

.sidebar-block span {
  width: 100%;
  height: 5px;
  background: rgba(255, 255, 255, 0.38);
}

.layout-sidebar .preview-header {
  border-bottom: 0;
  background: transparent;
}

.layout-dark-page {
  background:
    radial-gradient(circle at 88% 8%, var(--preview-dark-glow), transparent 28%),
    var(--preview-bg);
}

.layout-dark-page .preview-page,
.layout-dark-page .preview-body {
  background: transparent;
}

.is-dark .preview-header,
.is-dark .preview-section {
  background: var(--preview-dark-surface);
}

.is-dark .preview-header {
  margin: 10px 10px 0;
  border: 1px solid var(--preview-dark-border);
  border-radius: 8px;
}

.is-dark .line {
  background: var(--preview-dark-line);
}

.is-dark .preview-section {
  padding: 7px;
  border: 1px solid var(--preview-dark-border);
  border-radius: 8px;
}

.layout-soft-card .preview-section {
  padding: 7px;
  border-radius: 8px;
  background: var(--preview-surface);
}

.section-pill-dot .section-label {
  width: 32%;
  height: 11px;
  background: var(--preview-pill-bg);
  border: 1px solid var(--preview-pill-border);
}

.section-pill-dot .section-divider {
  display: none;
}

.section-icon-dot .section-label {
  background: var(--preview-line);
}

.section-classic-line .section-dot {
  display: none;
}

.section-classic-line .section-label {
  width: 34%;
  border-radius: 2px;
}

.section-dark-line .section-divider {
  background: var(--preview-tag-border);
}
</style>
