// 缩略图只承载列表识别，不加载完整模板 CSS；这里用真实模板的核心 token 做轻量对齐。
const previewMeta = {
  'tech-modern': {
    accent: '#3B82F6',
    accentSoft: '#DBEAFE',
    background: '#FFFFFF',
    surface: '#F8FAFC',
    textTone: '#0F172A',
    layout: 'top-band',
    headerStyle: 'gradient',
    sectionStyle: 'bar',
    photoPlacement: 'left',
    dark: false
  },
  'tech-minimal': {
    accent: '#5B7A2E',
    accentSoft: '#E4EBD6',
    background: '#F4F7EE',
    surface: '#FFFFFF',
    textTone: '#2F3A23',
    layout: 'minimal-photo',
    headerStyle: 'minimal',
    sectionStyle: 'pill-dot',
    photoPlacement: 'right',
    dark: false
  },
  'tech-dark': {
    accent: '#818CF8',
    accentSoft: '#312E81',
    background: '#0F172A',
    surface: '#1E293B',
    textTone: '#E0E7FF',
    layout: 'dark-page',
    headerStyle: 'gradient',
    sectionStyle: 'dark-line',
    photoPlacement: 'left',
    dark: true
  },
  'finance-classic': {
    accent: '#1E3A5F',
    accentSoft: '#D4AF37',
    background: '#FFFFFF',
    surface: '#F8FAFC',
    textTone: '#102033',
    layout: 'centered-authority',
    headerStyle: 'centered',
    sectionStyle: 'classic-line',
    photoPlacement: 'none',
    dark: false
  },
  'finance-gold': {
    accent: '#B8860B',
    accentSoft: '#F8EBC3',
    background: '#FFFFFF',
    surface: '#FFFBEB',
    textTone: '#33260A',
    layout: 'centered-authority',
    headerStyle: 'centered',
    sectionStyle: 'classic-line',
    photoPlacement: 'none',
    dark: false
  },
  'education-warm': {
    accent: '#059669',
    accentSoft: '#D1FAE5',
    background: '#FFFFFF',
    surface: '#F0FDF4',
    textTone: '#10372B',
    layout: 'soft-card',
    headerStyle: 'soft',
    sectionStyle: 'pill-dot',
    photoPlacement: 'left',
    dark: false
  },
  'education-clean': {
    accent: '#1B3A5C',
    accentSoft: '#DBEAFE',
    background: '#FFFFFF',
    surface: '#F8FAFC',
    textTone: '#102033',
    layout: 'sidebar',
    headerStyle: 'sidebar',
    sectionStyle: 'icon-dot',
    photoPlacement: 'sidebar',
    dark: false
  },
  'medical-professional': {
    accent: '#0EA5E9',
    accentSoft: '#E0F2FE',
    background: '#FFFFFF',
    surface: '#F8FAFC',
    textTone: '#0F2D3D',
    layout: 'top-band',
    headerStyle: 'gradient',
    sectionStyle: 'bar',
    photoPlacement: 'left',
    dark: false
  },
  'medical-soft': {
    accent: '#06B6D4',
    accentSoft: '#CFFAFE',
    background: '#FFFFFF',
    surface: '#ECFEFF',
    textTone: '#12343B',
    layout: 'soft-card',
    headerStyle: 'soft',
    sectionStyle: 'pill-dot',
    photoPlacement: 'left',
    dark: false
  },
  'manufacture-precision': {
    accent: '#475569',
    accentSoft: '#E2E8F0',
    background: '#FFFFFF',
    surface: '#F8FAFC',
    textTone: '#111827',
    layout: 'top-band',
    headerStyle: 'clean-band',
    sectionStyle: 'bar',
    photoPlacement: 'left',
    dark: false
  },
  'manufacture-industrial': {
    accent: '#78716C',
    accentSoft: '#E7E5E4',
    background: '#FAFAF9',
    surface: '#F5F5F4',
    textTone: '#292524',
    layout: 'sidebar',
    headerStyle: 'sidebar',
    sectionStyle: 'classic-line',
    photoPlacement: 'sidebar',
    dark: false
  },
  'marketing-vibrant': {
    accent: '#F59E0B',
    accentSoft: '#FEF3C7',
    background: '#FFFFFF',
    surface: '#FFFBEB',
    textTone: '#3B2603',
    layout: 'top-band',
    headerStyle: 'gradient',
    sectionStyle: 'bar',
    photoPlacement: 'left',
    dark: false
  },
  'marketing-bold': {
    accent: '#EF4444',
    accentSoft: '#FEE2E2',
    background: '#FFFFFF',
    surface: '#FFF1F2',
    textTone: '#3F1212',
    layout: 'top-band',
    headerStyle: 'gradient',
    sectionStyle: 'bar',
    photoPlacement: 'left',
    dark: false
  },
  'design-creative': {
    accent: '#8B5CF6',
    accentSoft: '#EDE9FE',
    background: '#FFFFFF',
    surface: '#F5F3FF',
    textTone: '#281B46',
    layout: 'top-band',
    headerStyle: 'gradient',
    sectionStyle: 'pill-dot',
    photoPlacement: 'left',
    dark: false
  },
  'design-elegant': {
    accent: '#6D28D9',
    accentSoft: '#EDE9FE',
    background: '#FFFFFF',
    surface: '#FAF5FF',
    textTone: '#2E1B4D',
    layout: 'minimal-photo',
    headerStyle: 'minimal',
    sectionStyle: 'classic-line',
    photoPlacement: 'right',
    dark: false
  },
  'legal-authoritative': {
    accent: '#111827',
    accentSoft: '#D1D5DB',
    background: '#FFFFFF',
    surface: '#F3F4F6',
    textTone: '#111827',
    layout: 'centered-authority',
    headerStyle: 'centered',
    sectionStyle: 'classic-line',
    photoPlacement: 'none',
    dark: false
  },
  'legal-refined': {
    accent: '#374151',
    accentSoft: '#E5E7EB',
    background: '#FFFFFF',
    surface: '#F9FAFB',
    textTone: '#111827',
    layout: 'minimal-photo',
    headerStyle: 'minimal',
    sectionStyle: 'classic-line',
    photoPlacement: 'none',
    dark: false
  }
}

export const templatePreviewMeta = previewMeta

function hexToRgb(hex) {
  const normalized = hex.replace('#', '')

  return {
    r: parseInt(normalized.slice(0, 2), 16),
    g: parseInt(normalized.slice(2, 4), 16),
    b: parseInt(normalized.slice(4, 6), 16)
  }
}

function rgbToHex({ r, g, b }) {
  return `#${[r, g, b].map((value) => (
    Math.round(value).toString(16).padStart(2, '0')
  )).join('')}`
}

function mixHex(source, target, targetRatio) {
  const sourceRgb = hexToRgb(source)
  const targetRgb = hexToRgb(target)
  const sourceRatio = 1 - targetRatio

  return rgbToHex({
    r: sourceRgb.r * sourceRatio + targetRgb.r * targetRatio,
    g: sourceRgb.g * sourceRatio + targetRgb.g * targetRatio,
    b: sourceRgb.b * sourceRatio + targetRgb.b * targetRatio
  })
}

function rgba(hex, alpha) {
  const { r, g, b } = hexToRgb(hex)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

// 缩略图运行时不依赖 color-mix，提前生成稳定颜色变量，避免兼容性导致灰屏。
function withPreviewTokens(meta) {
  return {
    ...meta,
    role: meta.role || mixHex(meta.accent, '#FFFFFF', 0.72),
    muted: meta.muted || mixHex(meta.textTone, '#FFFFFF', 0.82),
    line: meta.line || mixHex(meta.textTone, '#FFFFFF', 0.86),
    lineStrong: meta.lineStrong || mixHex(meta.accent, '#FFFFFF', 0.68),
    border: meta.border || rgba(meta.accent, 0.36),
    divider: meta.divider || rgba(meta.accent, 0.28),
    tagBg: meta.tagBg || mixHex(meta.accent, '#FFFFFF', 0.88),
    tagBorder: meta.tagBorder || rgba(meta.accent, 0.68),
    pillBg: meta.pillBg || mixHex(meta.accent, '#FFFFFF', 0.82),
    pillBorder: meta.pillBorder || rgba(meta.accent, 0.42),
    photoBg: meta.photoBg || mixHex(meta.accent, '#FFFFFF', 0.76),
    gradientEnd: meta.gradientEnd || mixHex(meta.accent, '#000000', 0.24),
    darkGlow: meta.darkGlow || rgba(meta.accent, 0.32),
    darkSurface: meta.darkSurface || rgba(meta.surface, 0.8),
    darkLine: meta.darkLine || rgba(meta.textTone, 0.28),
    darkBorder: meta.darkBorder || rgba(meta.accent, 0.34)
  }
}

// 新模板漏配时仍保留兼容兜底，但正式模板必须由单测约束为全量配置。
export function getTemplatePreviewMeta(templateId, fallback = {}) {
  const meta = previewMeta[templateId]

  if (!meta) {
    return withPreviewTokens({
      accent: fallback.color || '#3B82F6',
      accentSoft: fallback.accentSoft || '#DBEAFE',
      background: fallback.bgColor || '#FFFFFF',
      surface: fallback.surface || '#F8FAFC',
      textTone: fallback.textTone || '#0F172A',
      layout: fallback.layout || 'top-band',
      headerStyle: fallback.headerStyle || 'clean-band',
      sectionStyle: fallback.sectionStyle || 'bar',
      photoPlacement: fallback.photoPlacement || 'left',
      dark: Boolean(fallback.dark)
    })
  }

  return withPreviewTokens(meta)
}
