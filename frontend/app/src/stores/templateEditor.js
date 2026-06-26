import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 可新增的段落类型目录
const SECTION_TYPES = [
  { type: 'text', label: '自定义文本段', defaultTitle: '自定义段落' },
  { type: 'summary', label: '个人简介', defaultTitle: '个人简介' },
  { type: 'skills', label: '技能清单', defaultTitle: '专业技能' },
  { type: 'experience', label: '经历条目', defaultTitle: '经历' },
  { type: 'certifications', label: '证书资质', defaultTitle: '证书资质' },
  { type: 'awards', label: '获奖荣誉', defaultTitle: '获奖荣誉' },
  { type: 'languages', label: '语言能力', defaultTitle: '语言能力' },
  { type: 'interests', label: '兴趣爱好', defaultTitle: '兴趣爱好' }
]

// 默认的段落配置
const DEFAULT_SECTIONS = [
  { key: 'summary', title: '个人简介', visible: true, type: 'summary' },
  { key: 'skills', title: '专业技能', visible: true, type: 'skills' },
  { key: 'education', title: '教育经历', visible: true, type: 'experience' },
  { key: 'work', title: '工作经历', visible: true, type: 'experience' },
  { key: 'projects', title: '项目经历', visible: true, type: 'experience' }
]

export const useTemplateEditorStore = defineStore('templateEditor', () => {
  const templateId = ref('')
  const resumeData = ref(null)
  // 段落配置：控制哪些section可见、顺序如何
  const sectionsConfig = ref(structuredClone(DEFAULT_SECTIONS))

  // 获取段落类型目录（只读）
  const sectionTypes = SECTION_TYPES

  // 可新增的段落类型（所有类型都可重复添加）
  const availableSectionTypes = computed(() => SECTION_TYPES)

  function loadTemplate(id, data, customSectionsConfig) {
    templateId.value = id
    resumeData.value = structuredClone(data)
    sectionsConfig.value = customSectionsConfig
      ? structuredClone(customSectionsConfig)
      : structuredClone(DEFAULT_SECTIONS)
  }

  function updateBasic(field, value) {
    if (resumeData.value?.basic) {
      resumeData.value.basic[field] = value
    }
  }

  function updateSummary(value) {
    if (resumeData.value) {
      resumeData.value.summary = value
    }
  }

  function addSkill(skill) {
    if (resumeData.value && !resumeData.value.skills.includes(skill)) {
      resumeData.value.skills.push(skill)
    }
  }

  function removeSkill(index) {
    resumeData.value?.skills.splice(index, 1)
  }

  function addExperience(section, item) {
    if (resumeData.value?.[section]) {
      resumeData.value[section].push(item)
    }
  }

  function updateExperience(section, id, data) {
    const list = resumeData.value?.[section]
    if (!list) return
    const idx = list.findIndex(item => item.id === id)
    if (idx !== -1) {
      list[idx] = { ...list[idx], ...data }
    }
  }

  function removeExperience(section, id) {
    const list = resumeData.value?.[section]
    if (!list) return
    const idx = list.findIndex(item => item.id === id)
    if (idx !== -1) {
      list.splice(idx, 1)
    }
  }

  function addHighlight(section, id, text) {
    const list = resumeData.value?.[section]
    if (!list) return
    const item = list.find(item => item.id === id)
    if (item && item.highlights) {
      item.highlights.push(text)
    }
  }

  function removeHighlight(section, id, index) {
    const list = resumeData.value?.[section]
    if (!list) return
    const item = list.find(item => item.id === id)
    if (item?.highlights) {
      item.highlights.splice(index, 1)
    }
  }

  function updateHighlight(section, id, index, text) {
    const list = resumeData.value?.[section]
    if (!list) return
    const item = list.find(item => item.id === id)
    if (item?.highlights) {
      item.highlights[index] = text
    }
  }

  // ===== 段落管理 =====

  // 切换某section的可见性
  function toggleSection(key) {
    const sec = sectionsConfig.value.find(s => s.key === key)
    if (sec) sec.visible = !sec.visible
  }

  // 新增一个section
  function addSection(typeConfig, customTitle) {
    const uniqueKey = `${typeConfig.type}-${Date.now()}`
    const newSec = {
      key: uniqueKey,
      title: customTitle || typeConfig.defaultTitle || typeConfig.label,
      visible: true,
      type: typeConfig.type
    }
    sectionsConfig.value.push(newSec)

    // 为新section初始化对应的数据
    if (!resumeData.value) return
    switch (typeConfig.type) {
      case 'text':
        resumeData.value[uniqueKey] = ''
        break
      case 'certifications':
      case 'awards':
        resumeData.value[uniqueKey] = []
        break
      case 'languages':
      case 'interests':
        resumeData.value[uniqueKey] = []
        break
      case 'experience':
        resumeData.value[uniqueKey] = []
        break
      case 'summary':
        resumeData.value[uniqueKey] = ''
        break
      case 'skills':
        resumeData.value[uniqueKey] = []
        break
    }
  }

  // 移除一个section（从config中删除，清理对应数据）
  function removeSection(key) {
    const idx = sectionsConfig.value.findIndex(s => s.key === key)
    if (idx === -1) return
    sectionsConfig.value.splice(idx, 1)
    // 清理resumeData中对应的自定义数据（不删除内置字段）
    if (resumeData.value && !['summary', 'skills', 'education', 'work', 'projects'].includes(key)) {
      delete resumeData.value[key]
    }
  }

  // 调整section顺序
  function reorderSections(fromIdx, toIdx) {
    const arr = sectionsConfig.value
    if (fromIdx < 0 || fromIdx >= arr.length || toIdx < 0 || toIdx >= arr.length) return
    const [moved] = arr.splice(fromIdx, 1)
    arr.splice(toIdx, 0, moved)
  }

  // 自定义section标题
  function updateSectionTitle(key, newTitle) {
    const sec = sectionsConfig.value.find(s => s.key === key)
    if (sec) sec.title = newTitle
  }

  return {
    templateId,
    resumeData,
    sectionsConfig,
    sectionTypes,
    availableSectionTypes,
    loadTemplate,
    updateBasic,
    updateSummary,
    addSkill,
    removeSkill,
    addExperience,
    updateExperience,
    removeExperience,
    addHighlight,
    removeHighlight,
    updateHighlight,
    toggleSection,
    addSection,
    removeSection,
    reorderSections,
    updateSectionTitle
  }
})
