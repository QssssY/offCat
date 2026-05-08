<template>
  <div class="resume-result-view">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-section">
      <AiLoadingState
        title="AI 正在分析你的简历..."
        :stages="resumeStages"
        :currentStageIndex="0"
        :showElapsedTime="true"
        noCard
      />
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
      <!-- 等待/处理中状态：使用 AiLoadingState 组件 -->
      <div v-if="isPending || isProcessing" class="loading-section">
        <AiLoadingState
          :title="isPending ? '任务排队中...' : 'AI 正在分析你的简历...'"
          :stages="resumeStages"
          :currentStageIndex="currentStageIndex"
          :messages="resumeLoadingMessages"
          :showElapsedTime="true"
          :showRefreshButton="true"
          :refreshLoading="refreshing"
          noCard
          @refresh="fetchTaskDetail"
        />
        <div class="loading-nav-actions">
          <el-button size="small" @click="goToHome">返回首页</el-button>
          <el-button size="small" @click="goToUpload">继续上传</el-button>
        </div>
      </div>

      <!-- Hero 诊断总览区（仅完成/失败时显示） -->
      <div v-if="isCompleted || isFailed" class="hero-section" :class="`hero-${task.status}`">
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
          <div class="status-row">
            <span class="status-badge" :class="`status-${task.status}`">
              <span class="status-dot"></span>
              {{ statusText }}
            </span>
            <span class="update-time" v-if="task.updateTime">
              {{ formatTime(task.updateTime) }}
            </span>
          </div>
          <div v-if="parseModeLabel || task.parseMessage" class="parse-meta">
            <el-tag v-if="parseModeLabel" size="small" effect="plain" class="parse-tag">
              解析来源：{{ parseModeLabel }}
            </el-tag>
            <span v-if="task.parseMessage" class="parse-message">{{ task.parseMessage }}</span>
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

      <!-- 五维能力雷达图 + 得分明细（仅完成时显示） -->
      <div v-if="isCompleted && parsedResult" class="section-card">
        <div class="section-header">
          <div class="section-icon radar">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
            </svg>
          </div>
          <h3 class="section-title">五维能力分析</h3>
        </div>
        <div class="section-body radar-layout">
          <div class="radar-left">
            <RadarChart :scores="radarScores" />
          </div>
          <div class="radar-right">
            <RadarScorePanel :details="radarScoreDetails" />
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
            <div class="score-with-evaluation" v-if="basicInfoEvaluation?.score">
              <div class="score-row-left">
                <span class="item-label">完整度得分</span>
                <span class="item-value">{{ basicInfoEvaluation.score }}分</span>
              </div>
              <div v-if="basicInfoEvaluation?.evaluation" class="score-row-right">{{ basicInfoEvaluation.evaluation }}</div>
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

              <div v-if="jobMatchResult.analysisSummary" class="job-match-summary">
                {{ jobMatchResult.analysisSummary }}
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
                <div class="polish-actions">
                  <el-button size="small" @click="copyPolishedResume">复制内容</el-button>
                  <el-button size="small" type="primary" :loading="pdfExporting" @click="exportResumePdf">导出 PDF</el-button>
                  <el-button size="small" :loading="imageExporting" @click="exportResumeImage">导出图片</el-button>
                </div>
              </div>
              <div class="polish-edit-hint">
                支持直接编辑文案、切换标题样式、调整段落顺序；可导出为 PDF 或图片格式。
              </div>
              <div v-if="polishResult?.polishedResumeText" class="polish-preview-shell">
                <ResumeTemplate ref="resumeTemplateRef" :text="polishResult.polishedResumeText" mode="preview" />
              </div>
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
          <el-button v-if="!isPending && !isProcessing" @click="goToHome" class="action-btn secondary">返回首页</el-button>
          <el-button v-if="!isPending && !isProcessing" @click="goToUpload" class="action-btn secondary">继续上传</el-button>
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
import { ref, computed, nextTick, onMounted, onUnmounted, watch, defineAsyncComponent } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { analyzeResumeJobMatch, analyzeResumePolish, getResumeTask } from '@/api/resume'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

import AiLoadingState from '@/components/common/AiLoadingState.vue'
import OverallEvaluation from '@/components/resume/OverallEvaluation.vue'
import HighlightsSection from '@/components/resume/HighlightsSection.vue'
import SkillsSection from '@/components/resume/SkillsSection.vue'
import WorkExperienceSection from '@/components/resume/WorkExperienceSection.vue'
import RadarChart from '@/components/resume/RadarChart.vue'
import RadarScorePanel from '@/components/resume/RadarScorePanel.vue'
const ResumeTemplate = defineAsyncComponent(() => import('@/components/resume/ResumeTemplate.vue'))

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(true)
const refreshing = ref(false)
const error = ref('')
const task = ref(null)
const pollTimer = ref(null)
const pollRequestInFlight = ref(false)
const hasRefreshedUserInfo = ref(false)
const jobMatchVisible = ref(false)
const jobDescriptionText = ref('')
const jobMatchLoading = ref(false)
const jobMatchResult = ref(null)
const polishLoading = ref(false)
const polishResult = ref(null)
const polishSectionRef = ref(null)
const pdfExporting = ref(false)
const imageExporting = ref(false)
const resumeTemplateRef = ref(null)

const taskId = computed(() => route.params.taskId)

const isPending = computed(() => task.value?.status === 0)
const isProcessing = computed(() => task.value?.status === 1)
const isCompleted = computed(() => task.value?.status === 2)
const isFailed = computed(() => task.value?.status === 3)
const PENDING_POLL_INTERVAL = 5000
const PROCESSING_POLL_INTERVAL = 7000

const statusText = computed(() => {
  if (task.value?.statusDesc) {
    return task.value.statusDesc
  }
  switch (task.value?.status) {
    case 0: return '排队中'
    case 1: return '分析中'
    case 2: return '已完成'
    case 3: return '已失败'
    default: return '未知'
  }
})

/**
 * 结果页将后端解析模式转为更适合展示的中文标签。
 */
const parseModeLabel = computed(() => {
  switch (task.value?.parseMode) {
    case 'TEXT':
      return '文本直提'
    case 'MULTIMODAL':
      return '多模态识别'
    case 'OCR':
      return 'OCR 识别'
    case 'MIXED':
      return '混合解析'
    default:
      return ''
  }
})

const processingSummaryText = computed(() => {
  if (isPending.value) {
    return '当前任务正在排队，系统会自动刷新状态。'
  }
  if (isProcessing.value) {
    return '系统正在提取简历文本并进行 AI 分析，请稍候。'
  }
  return ''
})

// ---- AiLoadingState 相关 ----
/** 简历诊断阶段定义 */
const resumeStages = [
  { key: 'parsing', label: '提取简历文本' },
  { key: 'analyzing', label: 'AI 深度分析' },
  { key: 'generating', label: '生成诊断报告' }
]

/** 当前阶段索引：排队中=0，分析中=1 */
const currentStageIndex = computed(() => {
  if (isPending.value) return 0
  return 1
})

/** 轮播鼓励文案 */
const resumeLoadingMessages = [
  '正在逐行解析你的简历内容...',
  'AI 正在评估你的技能匹配度...',
  '正在分析工作经历的含金量...',
  '正在对比行业基准数据...',
  '快要完成了，再坚持一下...'
]

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
      educationEvaluation: result.educationEvaluation || {},
      optimizationSuggestions: result.optimizationSuggestions || result.suggestions || []
    }
  } catch (e) {
    return null
  }
})

// 五维雷达图数据：基本信息、岗位能力、工作经验、项目经历、教育背景
const radarScores = computed(() => {
  const result = parsedDiagnosisResult.value
  if (!result) return {}
  return {
    basicInfo: result.basicInfoEvaluation?.score || 0,
    skill: result.skillEvaluation?.score || 0,
    work: result.workExperienceEvaluation?.score || 0,
    project: result.projectExperienceEvaluation?.score || 0,
    education: result.educationEvaluation?.score || computeEducationFallback(result),
  }
})

// 教育评分兜底：当AI未返回educationEvaluation时，按权重反推
const computeEducationFallback = (result) => {
  const totalScore = result.overallEvaluation?.totalScore || 0
  const basic = result.basicInfoEvaluation?.score || 0
  const skill = result.skillEvaluation?.score || 0
  const work = result.workExperienceEvaluation?.score || 0
  const project = result.projectExperienceEvaluation?.score || 0
  const weightedSum = basic * 0.1 + skill * 0.15 + work * 0.25 + project * 0.4
  const eduScore = Math.round((totalScore - weightedSum) / 0.1)
  return Math.max(0, Math.min(100, eduScore))
}

// 雷达图得分明细：直接使用 AI 返回的 strengths（加分项）和 weaknesses（扣分项）
const radarScoreDetails = computed(() => {
  const r = parsedDiagnosisResult.value
  if (!r) return {}

  const extract = (evalObj, scoreKey) => ({
    score: radarScores.value[scoreKey],
    plus: evalObj?.strengths || [],
    minus: evalObj?.weaknesses || [],
  })

  return {
    basicInfo: extract(r.basicInfoEvaluation, 'basicInfo'),
    skill: extract(r.skillEvaluation, 'skill'),
    work: extract(r.workExperienceEvaluation, 'work'),
    project: extract(r.projectExperienceEvaluation, 'project'),
    education: extract(r.educationEvaluation, 'education'),
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
    workEvaluation: parsedResult.value.workExperienceEvaluation?.evaluation || '',
    projectEvaluation: parsedResult.value.projectExperienceEvaluation?.evaluation || '',
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

const fetchTaskDetail = async (options = {}) => {
  const { silent = false } = options
  if (!taskId.value) {
    error.value = '任务ID不存在'
    loading.value = false
    refreshing.value = false
    return
  }

  // 轮询和手动刷新共用一个请求锁，避免慢接口叠加触发多次详情查询。
  if (pollRequestInFlight.value) {
    return
  }

  if (!loading.value && !silent) {
    refreshing.value = true
  }
  pollRequestInFlight.value = true
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

    if (task.value?.status === 2 && previousStatus !== 2 && !hasRefreshedUserInfo.value) {
      hasRefreshedUserInfo.value = true
      await userStore.fetchUserInfo()
      ElMessage.success('简历诊断已完成')
    }
  } catch (err) {
    error.value = err.message || '获取任务详情失败，请稍后重试'
  } finally {
    loading.value = false
    refreshing.value = false
    pollRequestInFlight.value = false
  }
}

const startPolling = () => {
  stopPolling()
  scheduleNextPoll()
}

// 轮询改为递归 setTimeout，只在上一轮完成后再安排下一轮，避免持续打满慢接口。
const scheduleNextPoll = () => {
  if (!isPending.value && !isProcessing.value) {
    stopPolling()
    return
  }

  const nextInterval = isPending.value ? PENDING_POLL_INTERVAL : PROCESSING_POLL_INTERVAL
  pollTimer.value = setTimeout(async () => {
    await fetchTaskDetail({ silent: true })
    scheduleNextPoll()
  }, nextInterval)
}

const stopPolling = () => {
  if (pollTimer.value) {
    clearTimeout(pollTimer.value)
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
  fetchTaskDetail({ silent: true })
})

onUnmounted(() => {
  stopPolling()
  // 恢复页面滚动
  document.body.style.overflow = ''
  document.documentElement.style.overflow = ''
})

const unwatch = watch(() => task.value?.status, (newStatus) => {
  if (newStatus === 0 || newStatus === 1) {
    startPolling()
  } else {
    stopPolling()
  }
}, { immediate: true })

// 加载/处理中时锁定页面滚动，结果出来后恢复
watch([loading, isPending, isProcessing], ([l, p, pr]) => {
  const locked = l || p || pr
  document.body.style.overflow = locked ? 'hidden' : ''
  document.documentElement.style.overflow = locked ? 'hidden' : ''
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
  // 优先复制模板中已渲染的真实文本，否则回退到 AI 原始结果
  const editedText =
    resumeTemplateRef.value?.getResumePlainText?.() ||
    polishResult.value?.polishedResumeText ||
    ''
  if (!editedText) {
    ElMessage.warning('暂无可复制的内容')
    return
  }
  try {
    await navigator.clipboard.writeText(editedText)
    ElMessage.success('已复制到剪贴板')
  } catch (err) {
    console.error('[AI 润色] 复制失败:', err)
    ElMessage.error('复制失败，请手动选择复制')
  }
}


const getExportFilename = () => {
  // 优先使用简历中的姓名作为文件名，否则取第一行文本
  const resumeName = resumeTemplateRef.value?.getResumeName?.()?.trim()
  if (resumeName) {
    return resumeName
  }

  const firstLine = (resumeTemplateRef.value?.getResumePlainText?.() || polishResult.value?.polishedResumeText || '')
    .split('\n')
    .map((line) => line.trim())
    .find(Boolean)
  return firstLine || 'resume'
}

// 公共截图函数：将简历模板克隆到离屏容器，用 html2canvas 截图为高分辨率 canvas。
// 使用 buildExportElement 获取已清理非导出元素的克隆节点，确保截图干净。
// 公共截图函数：导出前先等待模板状态稳定，再用只读导出节点生成高分辨率 canvas。
const captureResumeCanvas = async () => {
  await nextTick()
  const exportEl = resumeTemplateRef.value?.buildExportElement?.()
  if (!exportEl) {
    return null
  }

  // 挂载到离屏容器，宽度设为 190mm（与 print 模式一致），白色背景
  const mountNode = document.createElement('div')
  mountNode.style.position = 'fixed'
  mountNode.style.left = '-10000px'
  mountNode.style.top = '0'
  mountNode.style.width = '190mm'
  mountNode.style.background = '#ffffff'
  mountNode.style.zIndex = '-1'
  mountNode.appendChild(exportEl)
  document.body.appendChild(mountNode)

  try {
    await new Promise((resolve) => requestAnimationFrame(() => resolve()))
    const html2canvas = (await import('html2canvas')).default
    // scale=3 保证高分辨率输出，dpi 300 适合打印质量
    const canvas = await html2canvas(exportEl, {
      scale: 3,
      useCORS: true,
      backgroundColor: '#ffffff',
      dpi: 300,
      logging: false,
    })
    return canvas
  } finally {
    document.body.removeChild(mountNode)
  }
}

// 导出 PDF：先用 html2canvas 截图为高分辨率图片，再嵌入 jsPDF。
// 直接截图嵌入可避免 html2pdf.js 分页解析导致的布局变形问题。
const exportResumePdf = async () => {
  if (!resumeTemplateRef.value?.buildExportElement) {
    ElMessage.warning('暂无可导出的润色内容')
    return
  }

  pdfExporting.value = true

  try {
    const canvas = await captureResumeCanvas()
    if (!canvas) {
      ElMessage.warning('暂无可导出的润色内容')
      return
    }

    const { default: jsPDF } = await import('jspdf')
    const filename = `${getExportFilename()}.pdf`

    // A4 竖版尺寸（mm）
    const pageW = 210
    const pageH = 297
    // PDF 不额外加边距，简历模板自身已有 10mm 内边距，避免双重边距导致空白过大
    const margin = 0
    const contentW = pageW - margin * 2
    const contentH = pageH - margin * 2

    // 将 canvas 转为 JPEG 数据，质量 0.95 平衡清晰度与文件大小
    const imgData = canvas.toDataURL('image/jpeg', 0.95)
    // 计算 canvas 的宽高比，按 A4 内容区宽度等比缩放
    const imgRatio = canvas.width / canvas.height
    let renderW = contentW
    let renderH = renderW / imgRatio

    // 若缩放后高度超出内容区，按高度适配
    if (renderH > contentH) {
      renderH = contentH
      renderW = renderH * imgRatio
    }

    // 水平居中偏移
    const offsetX = margin + (contentW - renderW) / 2

    const pdf = new jsPDF({
      unit: 'mm',
      format: 'a4',
      orientation: 'portrait',
    })

    pdf.addImage(imgData, 'JPEG', offsetX, margin, renderW, renderH)
    pdf.save(filename)

    ElMessage.success('PDF 已导出')
  } catch (err) {
    console.error('[PDF导出] 失败:', err)
    ElMessage.error('PDF 导出失败，请稍后重试')
  } finally {
    pdfExporting.value = false
  }
}

// 导出简历图片：复用截图 canvas，输出为 PNG 文件下载。
const exportResumeImage = async () => {
  if (!resumeTemplateRef.value?.buildExportElement) {
    ElMessage.warning('暂无可导出的润色内容')
    return
  }

  imageExporting.value = true

  try {
    const canvas = await captureResumeCanvas()
    if (!canvas) {
      ElMessage.warning('暂无可导出的润色内容')
      return
    }

    const filename = `${getExportFilename()}.png`

    // canvas.toBlob 触发 PNG 下载
    canvas.toBlob((blob) => {
      if (!blob) {
        ElMessage.error('图片生成失败')
        return
      }
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      link.click()
      URL.revokeObjectURL(url)
    }, 'image/png')

    ElMessage.success('简历图片已导出')
  } catch (err) {
    console.error('[图片导出] 失败:', err)
    ElMessage.error('图片导出失败，请稍后重试')
  } finally {
    imageExporting.value = false
  }
}

onUnmounted(() => {
  unwatch()
})
</script>

<style scoped>
.resume-result-view {
  min-height: 100%;
  background: var(--bg-page);
  padding: 24px;
  box-sizing: border-box;
}

/* ============================================
   加载状态
   ============================================ */
.loading-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 48px);
  gap: 20px;
}

.loading-nav-actions {
  display: flex;
  gap: 12px;
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
  background: var(--bg-card);
  border-radius: 16px;
  padding: 40px;
  text-align: center;
  max-width: 400px;
  border: 1px solid var(--border-card);
  box-sizing: border-box;
  overflow: hidden;
}

.error-icon-wrap {
  margin-bottom: 20px;
}

.error-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-title);
  margin-bottom: 8px;
}

.error-desc {
  font-size: 14px;
  color: var(--text-body);
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
  background: var(--bg-card);
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
  background: var(--bg-elevated);
  border-radius: 12px;
  border-left: 4px solid var(--orange-main);
  box-sizing: border-box;
  overflow: hidden;
}

.summary-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--orange-main);
  font-weight: 500;
  margin-bottom: 8px;
}

.summary-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-title);
  word-break: break-all;
  white-space: pre-wrap;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.parse-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.parse-tag {
  border-color: rgba(255, 140, 66, 0.35);
  color: var(--orange-main);
}

.parse-message {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-body);
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
  color: var(--text-muted);
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
  color: var(--text-title);
  pointer-events: none;
  white-space: nowrap;
}

.ring-score.muted {
  color: var(--text-placeholder);
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
  background: var(--bg-card);
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
  color: var(--text-body);
  text-align: center;
}

/* ============================================
   通用卡片区块
   ============================================ */
.section-card {
  background: var(--bg-card);
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

.section-icon.radar {
  background: #fff7ed;
  color: #f97316;
}

.section-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-title);
}

.section-body {
  padding: 20px;
  box-sizing: border-box;
  overflow: hidden;
}

/* 雷达图左右布局：左侧图表居中，右侧得分明细 */
.radar-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  align-items: center;
  padding: 12px 20px;
}

.radar-left {
  display: flex;
  align-items: center;
  justify-content: center;
}

.radar-right {
  min-width: 0;
}

@media (max-width: 767px) {
  .radar-layout {
    grid-template-columns: 1fr;
    gap: 16px;
    align-items: start;
  }
}

/* ============================================
   基础信息完整度
   ============================================ */
.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-divider);
  box-sizing: border-box;
  min-width: 0;
}

.item-row:last-child {
  border-bottom: none;
}

.item-label {
  font-size: 13px;
  color: var(--text-body);
}

.item-value {
  font-size: 13px;
  color: var(--text-title);
  font-weight: 500;
  word-break: break-all;
  white-space: normal;
  line-height: 1.6;
  text-align: right;
}

/* 得分 + 评价段落左右布局 */
.score-with-evaluation {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-divider);
}

.score-row-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  min-width: 80px;
}

.score-row-left .item-label {
  font-size: 12px;
  color: var(--text-muted);
}

.score-row-left .item-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--orange-main);
  text-align: center;
}

.score-row-right {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: var(--text-body);
  line-height: 1.7;
  text-align: justify;
  padding-top: 4px;
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
  background: var(--bg-elevated);
  border-radius: 8px;
  box-sizing: border-box;
  min-width: 0;
  overflow: hidden;
}

.basic-item .label {
  font-size: 13px;
  color: var(--text-body);
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
  background: var(--bg-elevated);
  border-radius: 8px;
  box-sizing: border-box;
  min-width: 0;
  overflow: hidden;
}

.suggestion-index {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--orange-main);
  color: var(--bg-card);
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.suggestion-text {
  font-size: 13px;
  color: var(--text-title);
  line-height: 1.6;
  word-break: break-all;
  white-space: normal;
}

/* ============================================
   失败状态
   ============================================ */
.failed-section {
  background: var(--bg-card);
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
  color: var(--text-body);
}

/* ============================================
   回退显示
   ============================================ */
.result-pre {
  font-size: 13px;
  color: var(--text-body);
  background: var(--bg-elevated);
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
  color: var(--text-body);
}

.job-match-entry-btn {
  border-color: var(--orange-main);
  color: var(--orange-main);
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
  color: var(--text-body);
}

.job-match-score-value {
  margin-top: 8px;
  font-size: 34px;
  line-height: 1;
  font-weight: 700;
  color: var(--orange-main);
}

.job-match-summary {
  padding: 14px 18px;
  border-radius: 12px;
  background: var(--bg-elevated);
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-body);
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
  background: var(--bg-elevated);
}

.job-match-block-title {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-title);
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
  background: #eef6f0;
  color: #3d7a5a;
  font-weight: 600;
}

.job-match-tag.missing {
  background: #fce4ec;
  color: #b71c1c;
  font-weight: 600;
}

.job-match-empty {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-muted);
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
  color: var(--text-muted);
}

.polish-content-block {
  padding: 18px 20px;
  border-radius: 14px;
  background: var(--bg-elevated);
}

.polish-block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.polish-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.polish-preview-shell {
  margin-top: 6px;
}

.polish-edit-hint {
  margin-bottom: 12px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--text-body);
}

.polish-content-pre {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-title);
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
  background: var(--bg-card);
  border-color: var(--border-card);
  color: var(--text-body);
}

.action-btn.secondary:hover {
  border-color: var(--orange-main);
  color: var(--orange-main);
}

.action-btn.primary {
  background: linear-gradient(135deg, #FF8C42 0%, #FF7A30 100%);
  border: none;
  color: var(--bg-card);
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

/* ===== 暗色模式适配 ===== */
.level-excellent {
  background: rgba(76, 175, 80, 0.15);
  color: #81c784;
}

.level-good {
  background: rgba(255, 152, 0, 0.15);
  color: #ffb74d;
}

.level-fair {
  background: rgba(255, 193, 7, 0.15);
  color: #ffd54f;
}

.level-poor {
  background: rgba(244, 67, 54, 0.15);
  color: #ef9a9a;
}

.status-0 {
  background: rgba(230, 162, 60, 0.15);
  color: #f0c060;
}

.status-1 {
  background: rgba(64, 158, 255, 0.15);
  color: #79bbff;
}

.status-2 {
  background: rgba(103, 194, 58, 0.15);
  color: #95d06a;
}

.status-3 {
  background: rgba(245, 108, 108, 0.15);
  color: #f89898;
}

.section-header {
  background: linear-gradient(135deg, var(--bg-elevated) 0%, var(--bg-card) 100%);
}

.section-icon.overall {
  background: rgba(103, 194, 58, 0.15);
  color: #81c784;
}

.section-icon.highlight {
  background: rgba(255, 152, 0, 0.15);
  color: #ffb74d;
}

.section-icon.skill {
  background: rgba(64, 158, 255, 0.15);
  color: #79bbff;
}

.section-icon.basic {
  background: rgba(168, 85, 247, 0.15);
  color: #c084fc;
}

.section-icon.experience {
  background: rgba(249, 115, 22, 0.15);
  color: #fb923c;
}

.section-icon.optimization {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}

.section-icon.radar {
  background: rgba(249, 115, 22, 0.15);
  color: #fb923c;
}

.kpi-card {
  border-color: var(--border-card);
}

.section-card {
  border-color: var(--border-card);
}

.job-match-tag.matched {
  background: rgba(47, 155, 93, 0.15);
  color: #6ee7a0;
}

.job-match-tag.missing {
  background: rgba(217, 72, 65, 0.15);
  color: #f87171;
}
</style>
