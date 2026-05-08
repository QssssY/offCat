<template>
  <!-- 模板渲染器：根元素带 resume-tpl-{id} class，供外部CSS定位 -->
  <div class="template-renderer" :class="`resume-tpl-${templateId}`">
    <!-- 个人信息头部区域 -->
    <div class="resume-header">
      <div class="header-left">
        <!-- 姓名单独一行 -->
        <div class="name">{{ resumeData.basic.name }}</div>
        <div class="header-main">
          <!-- 职位徽章 -->
          <div class="title profile-badge">{{ resumeData.basic.title }}</div>
          <!-- 联系方式列表 -->
          <div class="meta-list">
            <span v-if="resumeData.basic.phone" class="meta-item">
              <span class="meta-icon">📞</span>{{ resumeData.basic.phone }}
            </span>
            <span v-if="resumeData.basic.email" class="meta-item">
              <span class="meta-icon">✉</span>{{ resumeData.basic.email }}
            </span>
            <span v-if="resumeData.basic.location" class="meta-item">
              <span class="meta-icon">📍</span>{{ resumeData.basic.location }}
            </span>
            <span v-if="resumeData.basic.website" class="meta-item">
              <span class="meta-icon">🔗</span>{{ resumeData.basic.website }}
            </span>
          </div>
        </div>
      </div>
      <!-- 证件照预留位置（始终显示，无照片时显示占位图标） -->
      <div class="header-photo">
        <img v-if="resumeData.basic.photo" :src="resumeData.basic.photo" alt="证件照" />
        <div v-else class="photo-placeholder">📷</div>
      </div>
    </div>

    <div class="resume-body">
      <!-- 动态渲染的段落列表 -->
      <template v-for="sec in activeSections">
        <div
          v-if="isSectionVisible(sec)"
          :key="sec.key"
          class="section"
          :class="`section-${sec.key}`"
        >
          <!-- section标题装饰 -->
          <div class="section-tab">
            <span class="section-tab-dot"></span>
            <span class="section-title">{{ sec.title }}</span>
          </div>
          <div class="section-line"></div>

          <!-- 个人简介 -->
          <div v-if="sec.type === 'summary'" class="summary-text">
            {{ getSectionData(sec) }}
          </div>

          <!-- 技能清单 -->
          <div v-else-if="sec.type === 'skills'" class="skills-list">
            <span
              v-for="(skill, i) in getSectionData(sec)"
              :key="i"
              class="skill-tag"
            >{{ skill }}</span>
          </div>

          <!-- 经历条目 -->
          <template v-else-if="sec.type === 'experience'">
            <div
              v-for="item in getSectionData(sec)"
              :key="item.id"
              class="experience-item"
            >
              <div class="exp-header">
                <span class="exp-company">{{ item.school || item.company || item.name }}</span>
                <span class="exp-date">{{ item.startDate }} - {{ item.endDate }}</span>
              </div>
              <div class="exp-position">{{ item.degree ? `${item.degree} · ${item.major}` : (item.position || item.role) }}</div>
              <div v-if="item.description" class="exp-desc">{{ item.description }}</div>
              <ul v-if="item.highlights?.length" class="exp-highlights">
                <li v-for="(h, i) in item.highlights" :key="i">{{ h }}</li>
              </ul>
            </div>
          </template>

          <!-- 自定义文本段 -->
          <div v-else-if="sec.type === 'text'" class="custom-text">
            {{ getSectionData(sec) }}
          </div>

          <!-- 证书资质 / 获奖荣誉 -->
          <template v-else-if="sec.type === 'certifications' || sec.type === 'awards'">
            <div
              v-for="item in getSectionData(sec)"
              :key="item.id"
              class="experience-item"
            >
              <div class="exp-header">
                <span class="exp-company">{{ item.name }}</span>
                <span class="exp-date">{{ item.date }}</span>
              </div>
              <div v-if="item.issuer" class="exp-desc">{{ item.issuer }}</div>
            </div>
          </template>

          <!-- 语言能力 / 兴趣爱好 -->
          <div v-else-if="sec.type === 'languages' || sec.type === 'interests'" class="skills-list">
            <span
              v-for="(item, i) in getSectionData(sec)"
              :key="i"
              class="skill-tag"
            >{{ item }}</span>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  templateId: { type: String, required: true },
  resumeData: { type: Object, required: true },
  // 可选：段落配置，不传则使用默认的5个段落
  sectionsConfig: { type: Array, default: null }
})

// 默认段落配置（兼容无sectionsConfig的场景，如缩略图预览）
const DEFAULT_SECTIONS = [
  { key: 'summary', title: '个人简介', visible: true, type: 'summary' },
  { key: 'skills', title: '专业技能', visible: true, type: 'skills' },
  { key: 'education', title: '教育经历', visible: true, type: 'experience' },
  { key: 'work', title: '工作经历', visible: true, type: 'experience' },
  { key: 'projects', title: '项目经历', visible: true, type: 'experience' }
]

// 实际使用的段落配置
const activeSections = computed(() => {
  return props.sectionsConfig || DEFAULT_SECTIONS
})

// 判断section是否有数据可显示
function isSectionVisible(sec) {
  const data = getSectionData(sec)
  if (data == null) return false
  if (typeof data === 'string') return data.trim().length > 0
  if (Array.isArray(data)) return data.length > 0
  return true
}

// 获取section对应的数据
function getSectionData(sec) {
  return props.resumeData[sec.key]
}
</script>

<style scoped>
/* 基础样式：各模板CSS会覆盖这些默认值 */
.template-renderer {
  background: #fff;
  color: #1f2937;
  font-size: 14px;
  line-height: 1.6;
  padding: 0;
}

/* 头部区域：flex 布局，左侧信息 + 右侧预留照片位 */
.resume-header {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.header-left {
  flex: 1;
  min-width: 0;
}

/* 姓名：全宽独占一行 */
.name {
  width: 100%;
}

/* 证件照预留位置：始终占据空间 */
.header-photo {
  flex-shrink: 0;
  width: 90px;
  height: 112px;
  border-radius: 4px;
  overflow: hidden;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-photo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.photo-placeholder {
  font-size: 28px;
  opacity: 0.3;
}

/* section-tab 默认隐藏（仅作为CSS钩子，各模板可启用） */
.section-tab {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-tab-dot {
  display: none;
}

.section-line {
  display: none;
}

/* 技能标签：支持长文本描述换行 */
.skills-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skill-tag {
  display: inline-block;
  white-space: normal;
  word-break: break-word;
  line-height: 1.5;
}

.exp-desc {
  font-size: 13px;
  color: #6b7280;
  margin-top: 4px;
}

/* 自定义文本段 */
.custom-text {
  font-size: 14px;
  color: #4B5563;
  line-height: 1.7;
  white-space: pre-wrap;
}

.meta-icon {
  display: none;
}
</style>
