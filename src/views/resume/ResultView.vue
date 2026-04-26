<template>
  <div class="resume-result-view">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-section">
      <div class="loading-content">
        <div class="loading-ring"></div>
        <div class="loading-text">AI 正在分析你的简历...</div>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-section">
      <div class="error-card">
        <div class="error-icon-wrap">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="#f56c6c" stroke-width="2"/>
            <line x1="15" y1="9" x2="9" y2="15" stroke="#f56c6c" stroke-width="2"/>
            <line x1="9" y1="9" x2="15" y2="15" stroke="#f56c6c" stroke-width="2"/>
          </svg>
        </div>
        <div class="error-title">加载失败</div>
        <div class="error-desc">{{ error }}</div>
        <div class="error-actions">
          <el-button type="primary" @click="fetchTaskDetail">重试</el-button>
          <el-button @click="goToUpload">返回上传</el-button>
        </div>
      </div>
    </div>

    <!-- 任务内容 -->
    <div v-else-if="task" class="result-content">
      <!-- Hero 诊断总览区 -->
      <div class="hero-section" :class="`hero-${task.status}`">
        <div class="hero-left">
          <div class="score-display">
            <template v-if="isCompleted && parsedResult?.overallEvaluation">
              <div class="ring-wrapper">
                <svg width="80" height="80" viewBox="0 0 80 80" class="ring-svg">
                  <circle cx="40" cy="40" r="34" fill="none" stroke="#f3d8c7" stroke-width="6"/>
                  <circle
                    cx="40" cy="40" r="34"
                    fill="none"
                    stroke="#FF8C42"
                    stroke-width="6"
                    stroke-linecap="round"
                    :stroke-dasharray="`${(parsedResult.overallEvaluation.totalScore || 0) * 2.13} 213`"
                    transform="rotate(-90 40 40)"
                  />
                </svg>
                <span class="ring-score">{{ parsedResult.overallEvaluation.totalScore || 0 }}</span>
              </div>
            </template>
            <template v-else-if="isProcessing">
              <div class="ring-wrapper">
                <svg width="80" height="80" viewBox="0 0 80 80" class="ring-svg">
                  <circle cx="40" cy="40" r="34" fill="none" stroke="#f3d8c7" stroke-width="6"/>
                </svg>
                <span class="ring-score muted">--</span>
              </div>
            </template>
            <template v-else>
              <div class="ring-wrapper">
                <svg width="80" height="80" viewBox="0 0 80 80" class="ring-svg">
                  <circle cx="40" cy="40" r="34" fill="none" stroke="#f3d8c7" stroke-width="6"/>
                </svg>
                <span class="ring-score muted">{{ task.status === 3 ? '--' : '0' }}</span>
              </div>
            </template>
          </div>
          <div class="level-badge" :class="levelClass" v-if="isCompleted && parsedResult?.overallEvaluation">
            {{ levelText }}
          </div>
        </div>
        <div class="hero-right">
          <div class="ai-summary" v-if="isCompleted && parsedResult?.overallEvaluation?.summary">
            <div class="summary-label">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2a10 10 0 1 0 10 10"/>
                <path d="M12 6v6l4 2"/>
              </svg>
              AI 总评
            </div>
            <p class="summary-text">{{ parsedResult.overallEvaluation.summary }}</p>
          </div>
          <div class="ai-summary" v-else-if="isProcessing">
            <p class="summary-text processing">AI 正在深度分析你的简历，请稍候...</p>
          </div>
          <div class="status-row">
            <span class="status-badge" :class="`status-${task.status}`">
              <span class="status-dot"></span>
              {{ statusText }}
            </span>
            <span class="update-time" v-if="task.updateTime">
              {{ formatTime(task.updateTime) }}
            </span>
          </div>
          <div v-if="isProcessing" class="refresh-hint">
            <el-button size="small" :loading="refreshing" @click="fetchTaskDetail">
              {{ refreshing ? '刷新中...' : '刷新状态' }}
            </el-button>
          </div>
        </div>
      </div>

      <!-- KPI 指标仪表盘（仅完成时显示） -->
      <div v-if="isCompleted && parsedResult" class="kpi-section">
        <div class="score-grid">
          <!-- 综合评分 -->
          <div class="card kpi-card">
            <div class="card-label">综合评分</div>
            <div class="ring-wrapper">
              <svg width="72" height="72" viewBox="0 0 72 72" class="ring-svg">
                <circle cx="36" cy="36" r="30" fill="none" stroke="#f3d8c7" stroke-width="5"/>
                <circle
                  cx="36" cy="36" r="30"
                  fill="none"
                  stroke="#FF8C42"
                  stroke-width="5"
                  stroke-linecap="round"
                  :stroke-dasharray="`${(parsedResult.overallEvaluation?.totalScore || 0) * 1.88} 188`"
                  transform="rotate(-90 36 36)"
                />
              </svg>
              <span class="ring-score">{{ parsedResult.overallEvaluation?.totalScore || 0 }}</span>
            </div>
          </div>
          <!-- 信息完整度 -->
          <div class="card kpi-card">
            <div class="card-label">信息完整度</div>
            <div class="ring-wrapper">
              <svg width="72" height="72" viewBox="0 0 72 72" class="ring-svg">
                <circle cx="36" cy="36" r="30" fill="none" stroke="#f3d8c7" stroke-width="5"/>
                <circle
                  cx="36" cy="36" r="30"
                  fill="none"
                  stroke="#67C23A"
                  stroke-width="5"
                  stroke-linecap="round"
                  :stroke-dasharray="`${(basicInfoEvaluation?.score || 0) * 1.88} 188`"
                  transform="rotate(-90 36 36)"
                />
              </svg>
              <span class="ring-score">{{ basicInfoEvaluation?.score || 0 }}</span>
            </div>
          </div>
          <!-- 技能得分 -->
          <div class="card kpi-card">
            <div class="card-label">技能得分</div>
            <div class="ring-wrapper">
              <svg width="72" height="72" viewBox="0 0 72 72" class="ring-svg">
                <circle cx="36" cy="36" r="30" fill="none" stroke="#f3d8c7" stroke-width="5"/>
                <circle
                  cx="36" cy="36" r="30"
                  fill="none"
                  stroke="#409EFF"
                  stroke-width="5"
                  stroke-linecap="round"
                  :stroke-dasharray="`${(skillScore || 0) * 1.88} 188`"
                  transform="rotate(-90 36 36)"
                />
              </svg>
              <span class="ring-score">{{ skillScore || 0 }}</span>
            </div>
          </div>
          <!-- 经验得分 -->
          <div class="card kpi-card">
            <div class="card-label">经验得分</div>
            <div class="ring-wrapper">
              <svg width="72" height="72" viewBox="0 0 72 72" class="ring-svg">
                <circle cx="36" cy="36" r="30" fill="none" stroke="#f3d8c7" stroke-width="5"/>
                <circle
                  cx="36" cy="36" r="30"
                  fill="none"
                  stroke="#A855F7"
                  stroke-width="5"
                  stroke-linecap="round"
                  :stroke-dasharray="`${(experienceScore || 0) * 1.88} 188`"
                  transform="rotate(-90 36 36)"
                />
              </svg>
              <span class="ring-score">{{ experienceScore || 0 }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 诊断结果结构化展示（仅完成时显示） -->
      <template v-if="isCompleted && parsedResult">
        <!-- 技能情况 -->
        <div class="section-card" v-if="parsedResult.skillEvaluation">
          <div class="section-header">
            <div class="section-icon skill">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>
              </svg>
            </div>
            <h3 class="section-title">技能情况</h3>
          </div>
          <div class="section-body">
            <SkillsSection :data="parsedResult.skillEvaluation" />
          </div>
        </div>

        <!-- 亮点概览 -->
        <div class="section-card" v-if="parsedResult.highlights?.length">
          <div class="section-header">
            <div class="section-icon highlight">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
              </svg>
            </div>
            <h3 class="section-title">亮点确认</h3>
          </div>
          <div class="section-body">
            <HighlightsSection :data="parsedResult.highlights" />
          </div>
        </div>

        <!-- 基础信息完整度 -->
        <div class="section-card" v-if="basicInfoDetails">
          <div class="section-header">
            <div class="section-icon basic">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
            </div>
            <h3 class="section-title">基础信息完整度</h3>
          </div>
          <div class="section-body">
            <!-- 得分展示 -->
            <div class="item-row score-row" v-if="basicInfoEvaluation?.score">
              <span class="item-label">完整度得分</span>
              <span class="item-value">{{ basicInfoEvaluation.score }}分</span>
            </div>
            <!-- 五项填写状态 -->
            <div class="basic-items-grid" v-if="basicInfoDetails">
              <div class="basic-item" v-if="basicInfoDetails.name || basicInfoDetails.hasName !== undefined">
                <span class="label">姓名</span>
                <span :class="basicInfoDetails.name || basicInfoDetails.hasName ? 'value success' : 'value warning'">
                  {{ basicInfoDetails.name ? '已填写' : (basicInfoDetails.hasName ? '已填写' : '未填写') }}
                </span>
              </div>
              <div class="basic-item" v-if="basicInfoDetails.email || basicInfoDetails.hasEmail !== undefined">
                <span class="label">邮箱</span>
                <span :class="basicInfoDetails.email || basicInfoDetails.hasEmail ? 'value success' : 'value warning'">
                  {{ basicInfoDetails.email ? '已填写' : (basicInfoDetails.hasEmail ? '已填写' : '未填写') }}
                </span>
              </div>
              <div class="basic-item" v-if="basicInfoDetails.phone || basicInfoDetails.hasPhone !== undefined">
                <span class="label">电话</span>
                <span :class="basicInfoDetails.phone || basicInfoDetails.hasPhone ? 'value success' : 'value warning'">
                  {{ basicInfoDetails.phone ? '已填写' : (basicInfoDetails.hasPhone ? '已填写' : '未填写') }}
                </span>
              </div>
              <div class="basic-item" v-if="basicInfoDetails.github || basicInfoDetails.hasGithub !== undefined">
                <span class="label">GitHub</span>
                <span :class="basicInfoDetails.github || basicInfoDetails.hasGithub ? 'value success' : 'value warning'">
                  {{ basicInfoDetails.github ? '已填写' : (basicInfoDetails.hasGithub ? '已填写' : '未填写') }}
                </span>
              </div>
              <div class="basic-item" v-if="basicInfoDetails.blog || basicInfoDetails.hasBlog !== undefined">
                <span class="label">博客</span>
                <span :class="basicInfoDetails.blog || basicInfoDetails.hasBlog ? 'value success' : 'value warning'">
                  {{ basicInfoDetails.blog ? '已填写' : (basicInfoDetails.hasBlog ? '已填写' : '未填写') }}
                </span>
              </div>
            </div>
            <!-- 建议 -->
            <div class="suggestions-list" v-if="basicInfoEvaluation?.suggestions?.length">
              <div class="suggestion-item" v-for="(s, i) in basicInfoEvaluation.suggestions" :key="i">
                {{ s }}
              </div>
            </div>
          </div>
        </div>

        <!-- 工作与项目经验 -->
        <div class="section-card" v-if="workExperienceData">
          <div class="section-header">
            <div class="section-icon experience">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="7" width="20" height="14" rx="2" ry="2"/>
                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
              </svg>
            </div>
            <h3 class="section-title">工作与项目经验</h3>
          </div>
          <div class="section-body">
            <WorkExperienceSection :data="workExperienceData" />
          </div>
        </div>

        <!-- 优化建议 -->
        <div class="section-card" v-if="parsedResult.optimizationSuggestions?.length">
          <div class="section-header">
            <div class="section-icon optimization">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
              </svg>
            </div>
            <h3 class="section-title">优化建议</h3>
          </div>
          <div class="section-body">
            <div class="suggestions-list">
              <div class="suggestion-item" v-for="(item, idx) in parsedResult.optimizationSuggestions" :key="idx">
                <span class="suggestion-index">{{ idx + 1 }}</span>
                <span class="suggestion-text">{{ item }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 总体评价 -->
        <div class="section-card" v-if="parsedResult.overallEvaluation">
          <div class="section-header">
            <div class="section-icon overall">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"/>
                <path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"/>
                <path d="M4 22h16"/>
                <path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22"/>
                <path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22"/>
                <path d="M18 2H6v7a6 6 0 0 0 12 0V2Z"/>
              </svg>
            </div>
            <h3 class="section-title">AI 总体评价</h3>
          </div>
          <div class="section-body">
            <OverallEvaluation :data="parsedResult.overallEvaluation" />
          </div>
        </div>
      </template>

      <!-- 原始结果回退显示 -->
      <div v-else-if="isCompleted && task.diagnosisResult" class="section-card fallback">
        <div class="section-header">
          <h3 class="section-title">诊断结果（原始数据）</h3>
          <el-tag type="warning" size="small">未解析</el-tag>
        </div>
        <div class="section-body">
          <pre class="result-pre">{{ formatRawResult(task.diagnosisResult) }}</pre>
        </div>
      </div>

      <!-- 失败提示区 -->
      <div v-if="isFailed" class="failed-section">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
          <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" stroke="#f56c6c" stroke-width="2"/>
          <line x1="12" y1="9" x2="12" y2="13" stroke="#f56c6c" stroke-width="2"/>
          <line x1="12" y1="17" x2="12.01" y2="17" stroke="#f56c6c" stroke-width="2"/>
        </svg>
        <div class="failed-title">诊断失败</div>
        <div class="failed-desc">{{ task.errorMsg || '请稍后重试' }}</div>
      </div>

      <!-- 底部操作区 -->
      <div v-if="isCompleted" class="section-card job-match-card">
        <div class="section-header">
          <div class="section-icon overall">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 3v18h18" />
              <path d="M7 14l3-3 3 2 4-5" />
            </svg>
          </div>
          <h3 class="section-title">岗位匹配分析</h3>
        </div>
        <div class="section-body">
          <div class="job-match-intro">
            <p class="job-match-desc">输入目标岗位 JD，查看当前简历与岗位要求的匹配度、缺口与优化方向。</p>
            <el-button type="primary" plain class="job-match-entry-btn" @click="toggleJobMatchPanel">
              {{ jobMatchVisible ? '收起岗位匹配分析' : '岗位匹配分析' }}
            </el-button>
          </div>

          <div v-if="jobMatchVisible" class="job-match-panel">
            <el-input
              v-model="jobDescriptionText"
              type="textarea"
              :rows="8"
              resize="vertical"
              maxlength="5000"
              show-word-limit
              placeholder="请粘贴岗位 JD 文本，本轮仅支持手动输入。"
            />

            <div class="job-match-actions">
              <el-button type="primary" :loading="jobMatchLoading" @click="submitJobMatchAnalysis">
                {{ jobMatchLoading ? '分析中...' : '开始分析' }}
              </el-button>
              <el-button :loading="polishLoading" @click="triggerAiPolishPlaceholder">
                {{ polishLoading ? '润色中...' : '去 AI 润色' }}
              </el-button>
            </div>

            <div v-if="jobMatchResult" class="job-match-result">
              <div class="job-match-score-card">
                <div class="job-match-score-label">匹配度评分</div>
                <div class="job-match-score-value">{{ jobMatchResult.matchScore ?? 0 }}</div>
              </div>

              <div class="job-match-result-grid">
                <div class="job-match-result-block">
                  <div class="job-match-block-title">已匹配关键词</div>
                  <div v-if="jobMatchResult.matchedKeywords?.length" class="job-match-tag-list">
                    <span
                      v-for="keyword in jobMatchResult.matchedKeywords"
                      :key="`matched-${keyword}`"
                      class="job-match-tag matched"
                    >
                      {{ keyword }}
                    </span>
                  </div>
                  <div v-else class="job-match-empty">暂无已匹配关键词</div>
                </div>

                <div class="job-match-result-block">
                  <div class="job-match-block-title">缺失关键词或缺失能力项</div>
                  <div v-if="jobMatchResult.missingKeywords?.length" class="job-match-tag-list">
                    <span
                      v-for="keyword in jobMatchResult.missingKeywords"
                      :key="`missing-${keyword}`"
                      class="job-match-tag missing"
                    >
                      {{ keyword }}
                    </span>
                  </div>
                  <div v-else class="job-match-empty">当前未识别到明显缺口</div>
                </div>
              </div>

              <div class="job-match-suggestions">
                <div class="job-match-block-title">优化建议</div>
                <div v-if="jobMatchResult.suggestions?.length" class="suggestions-list">
                  <div
                    v-for="(item, idx) in jobMatchResult.suggestions"
                    :key="`job-suggestion-${idx}`"
                    class="suggestion-item"
                  >
                    <span class="suggestion-index">{{ idx + 1 }}</span>
                    <span class="suggestion-text">{{ item }}</span>
                  </div>
                </div>
                <div v-else class="job-match-empty">暂无优化建议</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="isCompleted" ref="polishSectionRef" class="section-card polish-card">
        <div class="section-header">
          <div class="section-icon optimization">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 20h9" />
              <path d="M16.5 3.5a2.121 2.121 0 1 1 3 3L7 19l-4 1 1-4z" />
            </svg>
          </div>
          <h3 class="section-title">AI 简历润色</h3>
        </div>
        <div class="section-body">
          <div v-if="polishResult" class="polish-result">
            <div class="polish-meta">
              <el-tag type="success">{{ polishResult.sourceType || '仅基于简历' }}</el-tag>
              <span class="polish-time" v-if="polishResult.createTime">
                {{ formatTime(polishResult.createTime) }}
              </span>
            </div>

            <div class="polish-content-block">
              <div class="polish-block-header">
                <div class="job-match-block-title">润色后的简历内容</div>
                <el-button size="small" @click="copyPolishedResume">复制内容</el-button>
              </div>
              <pre class="polish-content-pre">{{ polishResult.polishedResumeText }}</pre>
            </div>

            <div class="polish-content-block">
              <div class="job-match-block-title">修改说明 / 优化说明</div>
              <div v-if="polishResult.modificationNotes?.length" class="suggestions-list">
                <div
                  v-for="(item, idx) in polishResult.modificationNotes"
                  :key="`polish-note-${idx}`"
                  class="suggestion-item"
                >
                  <span class="suggestion-index">{{ idx + 1 }}</span>
                  <span class="suggestion-text">{{ item }}</span>
                </div>
              </div>
              <div v-else class="job-match-empty">暂无修改说明</div>
            </div>
          </div>
          <div v-else class="job-match-empty">
            请在上方“岗位匹配分析”区点击“去 AI 润色”生成结果。
          </div>
        </div>
      </div>

      <div class="action-section">
        <div class="action-group">
          <el-button @click="goToHome" class="action-btn secondary">返回首页</el-button>
          <el-button @click="goToUpload" class="action-btn secondary">继续上传</el-button>
          <el-button
            v-if="isCompleted"
            type="primary"
            class="action-btn primary"
            @click="goToInterview"
          >
            进入模拟面试
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { analyzeResumeJobMatch, analyzeResumePolish, getResumeTask } from '@/api/resume'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

import OverallEvaluation from '@/components/resume/OverallEvaluation.vue'
import HighlightsSection from '@/components/resume/HighlightsSection.vue'
import SkillsSection from '@/components/resume/SkillsSection.vue'
import BasicInfoSection from '@/components/resume/BasicInfoSection.vue'
import WorkExperienceSection from '@/components/resume/WorkExperienceSection.vue'
import OptimizationSection from '@/components/resume/OptimizationSection.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(true)
const refreshing = ref(false)
const error = ref('')
const task = ref(null)
const pollTimer = ref(null)
const hasRefreshedUserInfo = ref(false)
const jobMatchVisible = ref(false)
const jobDescriptionText = ref('')
const jobMatchLoading = ref(false)
const jobMatchResult = ref(null)
const polishLoading = ref(false)
const polishResult = ref(null)
const polishSectionRef = ref(null)

const taskId = computed(() => route.params.taskId)

const isPending = computed(() => task.value?.status === 0)
const isProcessing = computed(() => task.value?.status === 1)
const isCompleted = computed(() => task.value?.status === 2)
const isFailed = computed(() => task.value?.status === 3)

const statusText = computed(() => {
  switch (task.value?.status) {
    case 0: return '排队中'
    case 1: return '分析中'
    case 2: return '已完成'
    case 3: return '已失败'
    default: return '未知'
  }
})

const levelClass = computed(() => {
  const score = parsedResult.value?.overallEvaluation?.totalScore || 0
  if (score >= 80) return 'level-excellent'
  if (score >= 60) return 'level-good'
  if (score >= 40) return 'level-fair'
  return 'level-poor'
})

const levelText = computed(() => {
  const score = parsedResult.value?.overallEvaluation?.totalScore || 0
  if (score >= 80) return '优秀'
  if (score >= 60) return '良好'
  if (score >= 40) return '一般'
  return '待提升'
})

const parseDiagnosisResult = (raw) => {
  if (!raw) return {}
  if (typeof raw === 'object') return raw
  if (typeof raw === 'string') {
    try {
      return JSON.parse(raw) || {}
    } catch (e) {
      return {}
    }
  }
  return {}
}

const parsedDiagnosisResult = computed(() => {
  return parseDiagnosisResult(task.value?.diagnosisResult)
})

const basicInfoEvaluation = computed(() => {
  return parsedDiagnosisResult.value?.basicInfoEvaluation || {}
})

const basicInfoDetails = computed(() => {
  return parsedDiagnosisResult.value?.basicInfoDetails || parsedDiagnosisResult.value?.basicInfoEvaluation || {}
})

const skillScore = computed(() => {
  return parsedDiagnosisResult.value?.skillEvaluation?.score || 0
})

const experienceScore = computed(() => {
  const workScore = parsedDiagnosisResult.value?.workExperienceEvaluation?.score || 0
  const projectScore = parsedDiagnosisResult.value?.projectExperienceEvaluation?.score || 0
  return Math.round((workScore + projectScore) / 2)
})

const parsedResult = computed(() => {
  if (!task.value?.diagnosisResult) return null
  try {
    const result = parsedDiagnosisResult.value
    return {
      overallEvaluation: result.overallEvaluation || result.overall || {},
      highlights: result.highlights || result.strengths || [],
      skillEvaluation: result.skillEvaluation || result.skills || {},
      basicInfoEvaluation: basicInfoEvaluation.value,
      basicInfoDetails: basicInfoDetails.value,
      workExperienceEvaluation: result.workExperienceEvaluation || result.experience || {},
      projectExperienceEvaluation: result.projectExperienceEvaluation || result.projects || {},
      optimizationSuggestions: result.optimizationSuggestions || result.suggestions || []
    }
  } catch (e) {
    return null
  }
})

const workExperienceData = computed(() => {
  if (!parsedResult.value) return {}
  return {
    workScore: parsedResult.value.workExperienceEvaluation?.score,
    projectScore: parsedResult.value.projectExperienceEvaluation?.score,
    workExperiences: parsedResult.value.workExperienceEvaluation?.experiences ||
      parsedResult.value.workExperienceEvaluation?.items,
    projectExperiences: parsedResult.value.projectExperienceEvaluation?.projects ||
      parsedResult.value.projectExperienceEvaluation?.items,
    issues: [
      ...(parsedResult.value.workExperienceEvaluation?.issues || []),
      ...(parsedResult.value.projectExperienceEvaluation?.issues || [])
    ],
    suggestions: [
      ...(parsedResult.value.workExperienceEvaluation?.suggestions || []),
      ...(parsedResult.value.projectExperienceEvaluation?.suggestions || [])
    ]
  }
})

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatRawResult = (result) => {
  if (!result) return ''
  try {
    if (typeof result === 'string') {
      return JSON.stringify(JSON.parse(result), null, 2)
    }
    return JSON.stringify(result, null, 2)
  } catch (e) {
    return String(result)
  }
}

const fetchTaskDetail = async () => {
  if (!taskId.value) {
    error.value = '任务ID不存在'
    loading.value = false
    return
  }

  if (refreshing.value) return
  if (!loading.value) {
    refreshing.value = true
  }

  error.value = ''

  try {
    const res = await getResumeTask(taskId.value)
    const previousStatus = task.value?.status
    task.value = res.data
    if (task.value?.latestJobMatchAnalysis) {
      jobMatchResult.value = task.value.latestJobMatchAnalysis
    }
    if (task.value?.latestPolishResult) {
      polishResult.value = task.value.latestPolishResult
    }
    loading.value = false
    refreshing.value = false

    if (isCompleted.value && previousStatus !== 2 && !hasRefreshedUserInfo.value) {
      hasRefreshedUserInfo.value = true
      await userStore.fetchUserInfo()
      ElMessage.success('简历诊断已完成')
    }
  } catch (err) {
    error.value = err.message || '获取任务详情失败，请稍后重试'
    loading.value = false
    refreshing.value = false
  }
}

const startPolling = () => {
  if (pollTimer.value) clearInterval(pollTimer.value)
  pollTimer.value = setInterval(() => {
    if (isProcessing.value || isPending.value) {
      fetchTaskDetail()
    } else {
      stopPolling()
    }
  }, 3000)
}

const stopPolling = () => {
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
}

const goToHome = () => router.push('/')
const goToUpload = () => router.push('/resume/upload')
const goToInterview = () => {
  if (taskId.value) {
    router.push(`/interview/entry?resumeTaskId=${taskId.value}`)
  }
}

const toggleJobMatchPanel = () => {
  jobMatchVisible.value = !jobMatchVisible.value
}

// 润色完成后先直接更新本地页面状态，再补一次任务详情同步，保证用户无需刷新页面即可看到结果
const applyLatestPolishResult = async (latestPolishResult) => {
  if (!latestPolishResult) {
    return
  }
  polishResult.value = latestPolishResult
  if (task.value) {
    task.value = {
      ...task.value,
      latestPolishResult
    }
  }
  await nextTick()
  polishSectionRef.value?.scrollIntoView({
    behavior: 'smooth',
    block: 'start'
  })
}

// AI 润色属于长耗时操作，若请求超时但后端已落库，则主动回查最新结果并立即展示
const recoverLatestPolishResultAfterTimeout = async () => {
  for (let retryIndex = 0; retryIndex < 3; retryIndex += 1) {
    await new Promise((resolve) => setTimeout(resolve, retryIndex === 0 ? 1200 : 2000))
    try {
      const res = await getResumeTask(taskId.value)
      const latestTask = res?.data || res
      if (latestTask) {
        task.value = latestTask
      }
      if (latestTask?.latestPolishResult) {
        await applyLatestPolishResult(latestTask.latestPolishResult)
        return true
      }
    } catch (error) {
      console.error('[AI 简历润色] 超时后回查失败:', error)
    }
  }
  return false
}

const triggerAiPolishPlaceholder = async () => {
  if (!task.value?.taskId) {
    ElMessage.error('当前简历任务不存在')
    return
  }
  polishLoading.value = true
  try {
    const res = await analyzeResumePolish({
      resumeTaskId: task.value.taskId,
      resumeText: task.value.resumeText || '',
      jdText: jobDescriptionText.value.trim() || undefined
    })
    const latestPolishResult = res?.data || res
    await applyLatestPolishResult(latestPolishResult)
    fetchTaskDetail()
    ElMessage.success('AI 简历润色完成')
  } catch (err) {
    if (err?.code === 'ECONNABORTED' || String(err?.message || '').includes('timeout')) {
      const recovered = await recoverLatestPolishResultAfterTimeout()
      if (recovered) {
        ElMessage.success('AI 简历润色已完成，结果已同步展示')
        return
      }
      ElMessage.warning('AI 润色请求超时，正在等待后端完成，请稍后刷新查看结果')
      return
    }
    console.error('[AI 简历润色] 执行失败:', err)
    ElMessage.error(err?.message || 'AI 简历润色失败，请稍后重试')
  } finally {
    polishLoading.value = false
  }
}

const submitJobMatchAnalysis = async () => {
  if (!task.value?.taskId) {
    ElMessage.error('当前简历任务不存在')
    return
  }
  if (!jobDescriptionText.value.trim()) {
    ElMessage.warning('请先输入岗位 JD 文本')
    return
  }

  jobMatchLoading.value = true
  try {
    const res = await analyzeResumeJobMatch({
      resumeTaskId: task.value.taskId,
      resumeText: task.value.resumeText || '',
      jdText: jobDescriptionText.value.trim()
    })
    jobMatchResult.value = res.data
    if (task.value) {
      task.value.latestJobMatchAnalysis = res.data
    }
    ElMessage.success('岗位匹配分析完成')
  } catch (err) {
    console.error('[岗位匹配分析] 执行失败:', err)
  } finally {
    jobMatchLoading.value = false
  }
}

onMounted(() => {
  fetchTaskDetail()
})

onUnmounted(() => {
  stopPolling()
})

const unwatch = watch(isProcessing, (newVal) => {
  if (newVal || isPending.value) {
    startPolling()
  } else {
    stopPolling()
  }
}, { immediate: true })

watch(task, (newTask) => {
  if (newTask?.latestJobMatchAnalysis) {
    jobMatchResult.value = newTask.latestJobMatchAnalysis
  }
  if (newTask?.latestPolishResult) {
    polishResult.value = newTask.latestPolishResult
  }
}, { deep: true })

const copyPolishedResume = async () => {
  if (!polishResult.value?.polishedResumeText) {
    ElMessage.warning('暂无可复制的润色内容')
    return
  }
  try {
    await navigator.clipboard.writeText(polishResult.value.polishedResumeText)
    ElMessage.success('润色内容已复制')
  } catch (err) {
    console.error('[AI 简历润色] 复制失败:', err)
    ElMessage.error('复制失败，请稍后重试')
  }
}

onUnmounted(() => {
  unwatch()
})
</script>

<style scoped>
.resume-result-view {
  min-height: 100%;
  background: #F8F6F3;
  padding: 24px;
  box-sizing: border-box;
}

/* ============================================
   加载状态
   ============================================ */
.loading-section {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.loading-ring {
  width: 64px;
  height: 64px;
  border: 4px solid #f3d8c7;
  border-top-color: #FF8C42;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  font-size: 15px;
  color: #666;
}

/* ============================================
   错误状态
   ============================================ */
.error-section {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.error-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  text-align: center;
  max-width: 400px;
  border: 1px solid #f3d8c7;
  box-sizing: border-box;
  overflow: hidden;
}

.error-icon-wrap {
  margin-bottom: 20px;
}

.error-title {
  font-size: 18px;
  font-weight: 600;
  color: #2f2f2f;
  margin-bottom: 8px;
}

.error-desc {
  font-size: 14px;
  color: #666;
  margin-bottom: 24px;
}

.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* ============================================
   Hero 诊断总览区
   ============================================ */
.hero-section {
  background: #fff;
  border-radius: 20px;
  padding: 28px 32px;
  margin-bottom: 20px;
  border: 1px solid rgba(243, 216, 199, 0.5);
  box-shadow: 0 4px 20px rgba(255, 140, 66, 0.06);
  box-sizing: border-box;
  overflow: hidden;
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.hero-section.hero-1 {
  background: linear-gradient(135deg, #fff8f3 0%, #fff 100%);
}

.hero-section.hero-3 {
  background: linear-gradient(135deg, #fff0f0 0%, #fff 100%);
}

.hero-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  min-width: 120px;
}

.score-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.level-badge {
  font-size: 13px;
  font-weight: 600;
  padding: 5px 14px;
  border-radius: 20px;
  width: fit-content;
}

.level-excellent {
  background: #E8F5E9;
  color: #4CAF50;
}

.level-good {
  background: #FFF3E0;
  color: #FF9800;
}

.level-fair {
  background: #FFF8E1;
  color: #FFC107;
}

.level-poor {
  background: #FFEBEE;
  color: #F44336;
}

.hero-right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ai-summary {
  padding: 16px;
  background: #fafafa;
  border-radius: 12px;
  border-left: 4px solid #FF8C42;
  box-sizing: border-box;
  overflow: hidden;
}

.summary-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #FF8C42;
  font-weight: 500;
  margin-bottom: 8px;
}

.summary-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #2f2f2f;
  word-break: break-all;
  white-space: normal;
}

.summary-text.processing {
  color: #909399;
  font-style: italic;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  padding: 4px 12px;
  border-radius: 20px;
}

.status-badge .status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status-0 {
  background: #fdf6ec;
  color: #e6a23c;
}

.status-0 .status-dot {
  background: #e6a23c;
}

.status-1 {
  background: #ecf5ff;
  color: #409eff;
}

.status-1 .status-dot {
  background: #409eff;
  animation: pulse 1.5s ease-in-out infinite;
}

.status-2 {
  background: #f0f9eb;
  color: #67c23a;
}

.status-2 .status-dot {
  background: #67c23a;
}

.status-3 {
  background: #fef0f0;
  color: #f56c6c;
}

.status-3 .status-dot {
  background: #f56c6c;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.update-time {
  font-size: 13px;
  color: #999;
}

.refresh-hint {
  margin-top: 8px;
}

/* ============================================
   圆环核心样式
   ============================================ */
.ring-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.ring-svg {
  display: block;
}

.ring-score {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 22px;
  font-weight: 700;
  color: #2f2f2f;
  pointer-events: none;
  white-space: nowrap;
}

.ring-score.muted {
  color: #c0c4cc;
}

/* ============================================
   KPI 指标仪表盘
   ============================================ */
.kpi-section {
  margin-bottom: 20px;
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 16px;
}

.kpi-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  border: 1px solid rgba(243, 216, 199, 0.5);
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.04);
  box-sizing: border-box;
  overflow: hidden;
}

.card-label {
  font-size: 13px;
  color: #666;
  text-align: center;
}

/* ============================================
   通用卡片区块
   ============================================ */
.section-card {
  background: #fff;
  border-radius: 16px;
  margin-bottom: 16px;
  border: 1px solid rgba(243, 216, 199, 0.5);
  overflow: hidden;
  box-sizing: border-box;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #fff8f3 0%, #fff 100%);
  border-bottom: 1px solid rgba(243, 216, 199, 0.3);
  box-sizing: border-box;
  overflow: hidden;
}

.section-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.section-icon.overall {
  background: #f0f9eb;
  color: #67c23a;
}

.section-icon.highlight {
  background: #fff3e0;
  color: #ff9800;
}

.section-icon.skill {
  background: #ecf5ff;
  color: #409eff;
}

.section-icon.basic {
  background: #f3e8ff;
  color: #a855f7;
}

.section-icon.experience {
  background: #fff7ed;
  color: #f97316;
}

.section-icon.optimization {
  background: #f0f9eb;
  color: #22c55e;
}

.section-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #2f2f2f;
}

.section-body {
  padding: 20px;
  box-sizing: border-box;
  overflow: hidden;
}

/* ============================================
   基础信息完整度
   ============================================ */
.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  box-sizing: border-box;
  min-width: 0;
}

.item-row:last-child {
  border-bottom: none;
}

.item-label {
  font-size: 13px;
  color: #666;
}

.item-value {
  font-size: 13px;
  color: #2f2f2f;
  font-weight: 500;
  word-break: break-all;
  white-space: normal;
  line-height: 1.6;
  text-align: right;
}

.basic-items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.basic-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  box-sizing: border-box;
  min-width: 0;
  overflow: hidden;
}

.basic-item .label {
  font-size: 13px;
  color: #666;
  flex-shrink: 0;
}

.basic-item .value {
  font-size: 13px;
  font-weight: 600;
  word-break: break-all;
  white-space: normal;
  line-height: 1.4;
  text-align: right;
  min-width: 0;
}

.success {
  color: #67c23a;
}

.warning {
  color: #e6a23c;
}

.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}

.suggestion-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  box-sizing: border-box;
  min-width: 0;
  overflow: hidden;
}

.suggestion-index {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #FF8C42;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.suggestion-text {
  font-size: 13px;
  color: #2f2f2f;
  line-height: 1.6;
  word-break: break-all;
  white-space: normal;
}

/* ============================================
   失败状态
   ============================================ */
.failed-section {
  background: #fff;
  border-radius: 16px;
  padding: 48px;
  text-align: center;
  margin-bottom: 20px;
  border: 1px solid #fde2e2;
  box-sizing: border-box;
  overflow: hidden;
}

.failed-title {
  font-size: 18px;
  font-weight: 600;
  color: #f56c6c;
  margin: 16px 0 8px;
}

.failed-desc {
  font-size: 14px;
  color: #666;
}

/* ============================================
   回退显示
   ============================================ */
.result-pre {
  font-size: 13px;
  color: #606266;
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  line-height: 1.7;
  box-sizing: border-box;
}

.job-match-card {
  margin-top: 20px;
}

.job-match-intro {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.job-match-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #666;
}

.job-match-entry-btn {
  border-color: #FF8C42;
  color: #FF8C42;
}

.job-match-panel {
  margin-top: 20px;
}

.job-match-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.job-match-result {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.job-match-score-card {
  padding: 18px 20px;
  border-radius: 14px;
  background: linear-gradient(135deg, #fff8f3 0%, #fff 100%);
  border: 1px solid rgba(243, 216, 199, 0.7);
}

.job-match-score-label {
  font-size: 13px;
  color: #666;
}

.job-match-score-value {
  margin-top: 8px;
  font-size: 34px;
  line-height: 1;
  font-weight: 700;
  color: #FF8C42;
}

.job-match-result-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.job-match-result-block,
.job-match-suggestions {
  padding: 18px 20px;
  border-radius: 14px;
  background: #f9fafb;
}

.job-match-block-title {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #2f2f2f;
}

.job-match-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.job-match-tag {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
}

.job-match-tag.matched {
  background: #edf9f1;
  color: #2f9b5d;
}

.job-match-tag.missing {
  background: #fff1f0;
  color: #d94841;
}

.job-match-empty {
  font-size: 13px;
  line-height: 1.7;
  color: #909399;
}

.polish-card {
  margin-top: 20px;
}

.polish-result {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.polish-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.polish-time {
  font-size: 13px;
  color: #909399;
}

.polish-content-block {
  padding: 18px 20px;
  border-radius: 14px;
  background: #f9fafb;
}

.polish-block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.polish-content-pre {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: #2f2f2f;
  white-space: pre-wrap;
  word-break: break-word;
}

/* ============================================
   底部操作区
   ============================================ */
.action-section {
  margin-top: 24px;
  padding: 24px 0;
}

.action-group {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.action-btn {
  border-radius: 24px;
  padding: 10px 24px;
  font-size: 14px;
  min-height: 42px;
  box-sizing: border-box;
}

.action-btn.secondary {
  background: #fff;
  border-color: #f3d8c7;
  color: #666;
}

.action-btn.secondary:hover {
  border-color: #FF8C42;
  color: #FF8C42;
}

.action-btn.primary {
  background: linear-gradient(135deg, #FF8C42 0%, #FF7A30 100%);
  border: none;
  color: #fff;
  box-shadow: 0 4px 16px rgba(255, 140, 66, 0.3);
}

.action-btn.primary:hover {
  opacity: 0.9;
}

/* ============================================
   响应式
   ============================================ */
@media (max-width: 1023px) {
  .hero-section {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 24px;
  }

  .hero-left {
    flex-direction: row;
    gap: 20px;
  }
}

@media (max-width: 767px) {
  .resume-result-view {
    padding: 16px;
  }

  .hero-section {
    padding: 20px;
  }

  .ring-score {
    font-size: 18px;
  }

  .score-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .kpi-card {
    padding: 16px 12px;
  }

  .section-header {
    padding: 14px 16px;
  }

  .section-body {
    padding: 16px;
  }

  .basic-items-grid {
    grid-template-columns: 1fr;
  }

  .job-match-result-grid {
    grid-template-columns: 1fr;
  }

  .action-group {
    flex-direction: column;
    align-items: stretch;
  }

  .action-btn {
    width: 100%;
  }
}
</style>
