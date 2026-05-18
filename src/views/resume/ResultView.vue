<template>
  <div class="resume-result-view">
    <div class="page-back">
      <n-button quaternary size="tiny" @click="goToHistory" class="back-btn">
        <svg width="16" height="16" viewBox="0 0 256 256" fill="none" stroke="currentColor" stroke-width="24"><polyline points="160 208 80 128 160 48"/></svg>
        返回历史记录
      </n-button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-section">
      <AiLoadingState
        title="AI 正在分析你的简历..."
        :stages="resumeStages"
        :currentStageIndex="0"
        :showElapsedTime="true"
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
          <n-button type="primary" @click="fetchTaskDetail">重试</n-button>
          <n-button ghost @click="goToUpload">返回上传</n-button>
        </div>
      </div>
    </div>

    <!-- 任务内容 -->
    <div v-else-if="task" class="result-content">
      <!-- 轮询超时提示 -->
      <div v-if="pollTimeout" class="poll-timeout-section">
        <div class="poll-timeout-card">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="timeout-icon">
            <circle cx="12" cy="12" r="10" />
            <polyline points="12 6 12 12 16 14" />
          </svg>
          <h3 class="timeout-title">诊断处理时间较长</h3>
          <p class="timeout-desc">系统仍在处理你的简历，你可以稍后在历史记录中查看结果。</p>
          <div class="timeout-actions">
            <n-button type="primary" @click="router.push('/resume/history')">查看历史记录</n-button>
            <n-button ghost @click="startPolling">继续等待</n-button>
          </div>
        </div>
      </div>

      <!-- 等待/处理中状态：使用 AiLoadingState 组件 -->
      <div v-else-if="isPending || isProcessing" class="generating-section">
        <AiLoadingState
          :title="isPending ? '任务排队中...' : 'AI 正在分析你的简历...'"
          :stages="resumeStages"
          :currentStageIndex="currentStageIndex"
          :messages="resumeLoadingMessages"
          :showElapsedTime="true"
          :showRefreshButton="true"
          :refreshLoading="refreshing"
          @refresh="fetchTaskDetail"
        >
          <template #actions>
            <n-button size="small" ghost @click="goToHome">返回首页</n-button>
            <n-button size="small" ghost @click="goToUpload">继续上传</n-button>
          </template>
        </AiLoadingState>
      </div>

      <!-- Hero 诊断总览区（仅完成/失败时显示） -->
      <div v-if="isCompleted || isFailed" class="hero-section" :class="`hero-${task.status}`">
        <div class="hero-glow"></div>
        <div class="hero-body">
          <div class="hero-left">
            <template v-if="isCompleted && parsedResult?.overallEvaluation">
              <div class="ring-wrapper hero-ring">
                <svg width="88" height="88" viewBox="0 0 88 88" class="ring-svg">
                  <circle cx="44" cy="44" r="37" fill="none" stroke="rgba(255,255,255,0.2)" stroke-width="5"/>
                  <circle
                    cx="44" cy="44" r="37"
                    fill="none"
                    stroke="#ffffff"
                    stroke-width="5"
                    stroke-linecap="round"
                    :stroke-dasharray="`${(parsedResult.overallEvaluation.totalScore || 0) * 2.32} 232`"
                    transform="rotate(-90 44 44)"
                  />
                </svg>
                <span class="ring-score hero-score">{{ parsedResult.overallEvaluation.totalScore || 0 }}</span>
              </div>
            </template>
            <template v-else>
              <div class="ring-wrapper hero-ring">
                <svg width="88" height="88" viewBox="0 0 88 88" class="ring-svg">
                  <circle cx="44" cy="44" r="37" fill="none" stroke="rgba(255,255,255,0.2)" stroke-width="5"/>
                </svg>
                <span class="ring-score hero-score muted">{{ task.status === 3 ? '--' : '0' }}</span>
              </div>
            </template>
            <div class="level-badge" :class="levelClass" v-if="isCompleted && parsedResult?.overallEvaluation">
              {{ levelText }}
            </div>
          </div>
          <div class="hero-right">
            <div class="status-row">
              <span class="claude-badge" :class="`badge-${task.status}`">
                <span class="claude-badge-bar"></span>
                {{ statusText }}
              </span>
              <span class="update-time" v-if="task.updateTime">
                {{ formatTime(task.updateTime) }}
              </span>
            </div>
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
            <div v-if="parseModeLabel || task.parseMessage" class="parse-meta">
              <span class="parse-mode-pill">{{ parseModeLabel }}</span>
              <span v-if="task.parseMessage" class="parse-message">{{ task.parseMessage }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- KPI 指标仪表盘（仅完成时显示） -->
      <div v-if="isCompleted && parsedResult" class="kpi-section">
        <div class="kpi-header">
          <h3 class="kpi-title">评分概览</h3>
        </div>
        <div class="kpi-grid">
          <div class="kpi-cell">
            <span class="kpi-label">工作经验 <span class="kpi-weight">占评分40%</span></span>
            <div class="kpi-value-wrap">
              <span class="kpi-value coral">{{ workScore || 0 }}</span>
              <span class="kpi-unit">分</span>
            </div>
          </div>
          <div class="kpi-cell">
            <span class="kpi-label">项目经验 <span class="kpi-weight">占评分30%</span></span>
            <div class="kpi-value-wrap">
              <span class="kpi-value purple">{{ projectScore || 0 }}</span>
              <span class="kpi-unit">分</span>
            </div>
          </div>
          <div class="kpi-cell">
            <span class="kpi-label">核心技能 <span class="kpi-weight">占评分15%</span></span>
            <div class="kpi-value-wrap">
              <span class="kpi-value blue">{{ skillScore || 0 }}</span>
              <span class="kpi-unit">分</span>
            </div>
          </div>
          <div class="kpi-cell">
            <span class="kpi-label">基础信息 <span class="kpi-weight">占评分5%</span></span>
            <div class="kpi-value-wrap">
              <span class="kpi-value green">{{ basicInfoEvaluation?.score || 0 }}</span>
              <span class="kpi-unit">分</span>
            </div>
          </div>
          <div class="kpi-cell">
            <span class="kpi-label">学历匹配 <span class="kpi-weight">占评分5%</span></span>
            <div class="kpi-value-wrap">
              <span class="kpi-value teal">{{ educationScore || 0 }}</span>
              <span class="kpi-unit">分</span>
            </div>
          </div>
          <div class="kpi-cell">
            <span class="kpi-label">个人定位 <span class="kpi-weight">占评分5%</span></span>
            <div class="kpi-value-wrap">
              <span class="kpi-value gray">{{ positioningScore || 0 }}</span>
              <span class="kpi-unit">分</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 评分参考表 -->
      <div v-if="isCompleted && parsedResult" class="section-card grade-ref-card">
        <div class="section-header">
          <h3 class="section-title">评分参考</h3>
        </div>
        <div class="section-body">
          <div class="grade-table">
            <div v-for="g in gradeScale" :key="g.level" class="grade-row" :class="{ active: resumeLevel === g.level }">
              <span class="grade-level" :class="'grade-' + g.level.toLowerCase()">{{ g.level }}</span>
              <span class="grade-range">{{ g.range }}</span>
              <span class="grade-label">{{ g.label }}</span>
              <span class="grade-stage">{{ g.stage }}</span>
              <span v-if="resumeLevel === g.level" class="grade-marker">你在此</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 五维能力雷达图 + 得分明细（仅完成时显示） -->
      <div v-if="isCompleted && parsedResult" class="section-card">
        <div class="section-header">
          <div class="section-icon radar">
            <svg width="18" height="18" viewBox="0 0 256 256" fill="none" stroke="currentColor" stroke-width="24" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="128" cy="128" r="96"/>
              <polyline points="128 56 128 120 168 156"/>
            </svg>
          </div>
          <h3 class="section-title">五维能力分析</h3>
        </div>
        <div class="section-body radar-layout">
          <div class="radar-left">
            <RadarChart :scores="radarScores" :labels="radarLabels" :keys="radarKeys" />
          </div>
          <div class="radar-right">
            <RadarScorePanel :details="radarScoreDetails" :dimensionConfig="radarDimensionConfig" />
          </div>
        </div>
      </div>

      <!-- 诊断结果结构化展示（仅完成时显示） -->
      <template v-if="isCompleted && parsedResult">
        <!-- 技能情况 -->
        <div class="section-card" v-if="parsedResult.skillEvaluation">
          <div class="section-header">
            <div class="section-icon skill">
            <svg width="18" height="18" viewBox="0 0 256 256" fill="none" stroke="currentColor" stroke-width="24" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="128" cy="128" r="96"/>
              <path d="M128 56v0a28 28 0 0 1 28 28v0a28 28 0 0 1-28 28"/>
              <path d="M128 172v0a28 28 0 0 0 28 28v0a28 28 0 0 0 28-28v0"/>
              <path d="M128 84v0a28 28 0 0 0-28 28v0"/>
              <path d="M128 172v0a28 28 0 0 1-28 28v0a28 28 0 0 1-28-28v0"/>
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
            <svg width="18" height="18" viewBox="0 0 256 256" fill="none" stroke="currentColor" stroke-width="24" stroke-linecap="round" stroke-linejoin="round">
              <path d="M128 24l28 60 64 8-48 44 16 64-60-32-60 32 16-64-48-44 64-8z"/>
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
            <svg width="18" height="18" viewBox="0 0 256 256" fill="none" stroke="currentColor" stroke-width="24" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="128" cy="112" r="40"/>
              <path d="M208 216a80 80 0 0 0-160 0"/>
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
            <svg width="18" height="18" viewBox="0 0 256 256" fill="none" stroke="currentColor" stroke-width="24" stroke-linecap="round" stroke-linejoin="round">
              <rect x="24" y="80" width="208" height="136" rx="16"/>
              <path d="M168 216v-32a40 40 0 0 0-80 0v32"/>
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
            <svg width="18" height="18" viewBox="0 0 256 256" fill="none" stroke="currentColor" stroke-width="24" stroke-linecap="round" stroke-linejoin="round">
              <path d="M96 208H40a8 8 0 0 1-8-8V56a8 8 0 0 1 8-8h96l32 32v48"/>
              <path d="M168 176l24-24 24 24"/>
              <path d="M192 152v48a8 8 0 0 0 8 8h32"/>
              <path d="M152 224h80"/>
            </svg>
          </div>
          <h3 class="section-title">优化建议</h3>
          </div>
          <div class="section-body">
            <div class="suggestions-list">
              <div class="suggestion-item" v-for="(item, idx) in parsedResult.optimizationSuggestions" :key="idx">
                <span class="suggestion-bar"></span>
                <span class="suggestion-text">{{ item }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 个人定位 -->
        <div class="section-card" v-if="positioningEval.score !== undefined">
          <div class="section-header">
            <div class="section-icon positioning">
              <svg width="18" height="18" viewBox="0 0 256 256" fill="none" stroke="currentColor" stroke-width="24" stroke-linecap="round" stroke-linejoin="round">
                <path d="M48 40h160a8 8 0 0 1 8 8v160a8 8 0 0 1-8 8H48a8 8 0 0 1-8-8V48a8 8 0 0 1 8-8z"/>
                <path d="M72 88h112"/>
                <path d="M72 128h96"/>
                <path d="M72 168h64"/>
              </svg>
            </div>
            <h3 class="section-title">个人定位</h3>
          </div>
          <div class="section-body">
            <div class="score-with-evaluation">
              <div class="score-row-left">
                <span class="item-label">个人定位得分</span>
                <span class="item-value">{{ positioningEval.score }}分</span>
              </div>
              <div v-if="positioningEval.evaluation" class="score-row-right">{{ positioningEval.evaluation }}</div>
            </div>
            <div class="detail-group" v-if="positioningEval.strengths?.length">
              <div class="section-sub-title">加分项</div>
              <div class="tag-list">
                <span class="tag tag-strength" v-for="(item, i) in positioningEval.strengths" :key="i">{{ item }}</span>
              </div>
            </div>
            <div class="detail-group" v-if="positioningEval.weaknesses?.length">
              <div class="section-sub-title">扣分项</div>
              <div class="tag-list">
                <span class="tag tag-weakness" v-for="(item, i) in positioningEval.weaknesses" :key="i">{{ item }}</span>
              </div>
            </div>
            <div class="suggestions-list" v-if="positioningEval.suggestions?.length">
              <div class="suggestion-item" v-for="(s, i) in positioningEval.suggestions" :key="i">
                <span class="suggestion-bar"></span>
                <span class="suggestion-text">{{ s }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 总体评价 -->
        <div class="section-card" v-if="parsedResult.overallEvaluation">
          <div class="section-header">
            <div class="section-icon overall">
            <svg width="18" height="18" viewBox="0 0 256 256" fill="none" stroke="currentColor" stroke-width="24" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="128" cy="120" r="64"/>
              <path d="M128 72v48l24 24"/>
              <path d="M176 192l16 40-64-24-64 24 16-40"/>
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
          <span class="parse-mode-pill warn">未解析</span>
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
            <svg width="18" height="18" viewBox="0 0 256 256" fill="none" stroke="currentColor" stroke-width="24" stroke-linecap="round" stroke-linejoin="round">
              <rect x="40" y="168" width="40" height="48"/>
              <rect x="108" y="112" width="40" height="104"/>
              <rect x="176" y="56" width="40" height="160"/>
              <line x1="216" y1="24" x2="176" y2="56"/>
            </svg>
          </div>
          <h3 class="section-title">岗位匹配分析</h3>
        </div>
        <div class="section-body">
          <div class="job-match-intro">
            <p class="job-match-desc">输入目标岗位 JD，查看当前简历与岗位要求的匹配度、缺口与优化方向。</p>
            <n-button secondary class="job-match-entry-btn" @click="toggleJobMatchPanel">
              {{ jobMatchVisible ? '收起岗位匹配分析' : '岗位匹配分析' }}
            </n-button>
          </div>

          <div v-if="jobMatchVisible" class="job-match-panel">
            <n-input
              v-model:value="jobDescriptionText"
              type="textarea"
              :autosize="{ minRows: 6, maxRows: 12 }"
              :maxlength="5000"
              show-count
              placeholder="请粘贴岗位 JD 文本，本轮仅支持手动输入。"
            />

            <div class="job-match-actions">
              <n-button type="primary" :loading="jobMatchLoading" @click="submitJobMatchAnalysis">
                {{ jobMatchLoading ? '分析中...' : '开始分析' }}
              </n-button>
              <n-button secondary :loading="polishLoading" @click="triggerAiPolishPlaceholder">
                {{ polishLoading ? '润色中...' : '去 AI 润色' }}
              </n-button>
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
                      <span class="suggestion-bar"></span>
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
            <svg width="18" height="18" viewBox="0 0 256 256" fill="none" stroke="currentColor" stroke-width="24" stroke-linecap="round" stroke-linejoin="round">
              <path d="M200 216H40a8 8 0 0 1-8-8V56a8 8 0 0 1 8-8h96l32 32v128"/>
              <path d="M168 176l24-24 24 24"/>
              <path d="M192 152v48a8 8 0 0 0 8 8h32"/>
            </svg>
          </div>
          <h3 class="section-title">AI 简历润色</h3>
        </div>
        <div class="section-body">
          <div v-if="polishResult" class="polish-result">
            <div class="polish-meta">
              <span class="parse-mode-pill">{{ polishResult.sourceType || '仅基于简历' }}</span>
              <span class="polish-time" v-if="polishResult.createTime">
                {{ formatTime(polishResult.createTime) }}
              </span>
            </div>

            <div class="polish-content-block">
              <div class="polish-block-header">
                <div class="job-match-block-title">润色后的简历内容</div>
                <div class="polish-actions">
                  <n-button size="small" ghost @click="copyPolishedResume">复制内容</n-button>
                  <n-button size="small" type="primary" :loading="pdfExporting" @click="exportResumePdf">导出 PDF</n-button>
                  <n-button size="small" ghost :loading="imageExporting" @click="exportResumeImage">导出图片</n-button>
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
                    <span class="suggestion-bar"></span>
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
          <n-button v-if="!isPending && !isProcessing" @click="goToHome" class="action-btn secondary">返回首页</n-button>
          <n-button v-if="!isPending && !isProcessing" @click="goToUpload" class="action-btn secondary">继续上传</n-button>
          <n-button
            v-if="isCompleted"
            class="action-btn claude-primary"
            @click="goToInterview"
          >
            进入模拟面试
          </n-button>
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
import { NButton, NInput, useMessage } from 'naive-ui'
const message = useMessage()

import AiLoadingState from '@/components/common/AiLoadingState.vue'
import OverallEvaluation from '@/components/resume/OverallEvaluation.vue'
import HighlightsSection from '@/components/resume/HighlightsSection.vue'
import SkillsSection from '@/components/resume/SkillsSection.vue'
import WorkExperienceSection from '@/components/resume/WorkExperienceSection.vue'
import RadarChart from '@/components/resume/RadarChart.vue'
import RadarScorePanel from '@/components/resume/RadarScorePanel.vue'
import { createResumePdfImagePages } from '@/utils/resumePdfPagination'
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
const POLL_MAX_ROUNDS = 90 // 最多轮询 90 轮，约 10 分钟
let pollRounds = 0
const pollTimeout = ref(false)

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
  if (score >= 90) return 'level-excellent'
  if (score >= 75) return 'level-good'
  if (score >= 60) return 'level-fair'
  return 'level-poor'
})

const levelText = computed(() => {
  const score = parsedResult.value?.overallEvaluation?.totalScore || 0
  if (score >= 90) return '顶尖'
  if (score >= 75) return '优秀'
  if (score >= 60) return '合格'
  return '偏弱'
})

const resumeLevel = computed(() => {
  return parsedResult.value?.overallEvaluation?.level || ''
})

const gradeScale = [
  { level: 'S', range: '90-100', label: '顶尖', stage: '竞争力极强，岗位匹配度高，具备突出亮点和量化成果' },
  { level: 'A', range: '75-89', label: '优秀', stage: '竞争力良好，经历扎实，有明确量化成果和专业度' },
  { level: 'B', range: '60-74', label: '合格', stage: '基础达标，但缺乏亮点或量化成果，需提升关键经历' },
  { level: 'C', range: '40-59', label: '偏弱', stage: '简历问题较多，经历描述空洞、结构混乱或信息缺失' },
  { level: 'D', range: '<40', label: '问题严重', stage: '简历存在重大缺陷，需从零系统梳理经历和技能' },
]

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

const workScore = computed(() => {
  return parsedDiagnosisResult.value?.workExperienceEvaluation?.score || 0
})

const projectScore = computed(() => {
  return parsedDiagnosisResult.value?.projectExperienceEvaluation?.score || 0
})

const educationScore = computed(() => {
  const result = parsedDiagnosisResult.value
  return result?.educationEvaluation?.score || computeEducationFallback(result) || 0
})

const positioningScore = computed(() => {
  return parsedDiagnosisResult.value?.positioningEvaluation?.score || 0
})

const positioningEval = computed(() => {
  return parsedDiagnosisResult.value?.positioningEvaluation || {}
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
      positioningEvaluation: result.positioningEvaluation || {},
      optimizationSuggestions: result.optimizationSuggestions || result.suggestions || []
    }
  } catch (e) {
    return null
  }
})

// 六维雷达图数据
const radarScores = computed(() => {
  const result = parsedDiagnosisResult.value
  if (!result) return {}
  return {
    basicInfo: result.basicInfoEvaluation?.score || 0,
    skill: result.skillEvaluation?.score || 0,
    work: result.workExperienceEvaluation?.score || 0,
    project: result.projectExperienceEvaluation?.score || 0,
    education: result.educationEvaluation?.score || computeEducationFallback(result),
    positioning: result.positioningEvaluation?.score || 0,
  }
})

// 教育评分兜底：当AI未返回educationEvaluation时，按权重反推
const computeEducationFallback = (result) => {
  if (!result) return 0
  const totalScore = result.overallEvaluation?.totalScore || 0
  const basic = result.basicInfoEvaluation?.score || 0
  const skill = result.skillEvaluation?.score || 0
  const work = result.workExperienceEvaluation?.score || 0
  const project = result.projectExperienceEvaluation?.score || 0
  const positioning = result.positioningEvaluation?.score || 0
  const weightedSum = basic * 0.05 + skill * 0.15 + work * 0.40 + project * 0.30 + positioning * 0.05
  const eduScore = Math.round((totalScore - weightedSum) / 0.05)
  return Math.max(0, Math.min(100, eduScore))
}

// 雷达图得分明细：直接使用 AI 返回的 strengths（加分项）和 weaknesses（扣分项）
const radarKeys = ['basicInfo', 'skill', 'work', 'project', 'education', 'positioning']

const radarLabels = ['基本信息', '岗位能力', '工作经验', '项目经历', '教育背景', '个人定位']

const radarDimensionConfig = [
  { key: 'basicInfo', label: '基本信息' },
  { key: 'skill', label: '岗位能力' },
  { key: 'work', label: '工作经验' },
  { key: 'project', label: '项目经历' },
  { key: 'education', label: '教育背景' },
  { key: 'positioning', label: '个人定位' },
]

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
    positioning: extract(r.positioningEvaluation, 'positioning'),
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
      message.success('简历诊断已完成')
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
  pollRounds = 0
  pollTimeout.value = false
  scheduleNextPoll()
}

// 轮询改为递归 setTimeout，只在上一轮完成后再安排下一轮，避免持续打满慢接口。
const scheduleNextPoll = () => {
  if (!isPending.value && !isProcessing.value) {
    stopPolling()
    return
  }

  // 超过最大轮询次数时停止轮询，提示用户稍后查看
  if (pollRounds >= POLL_MAX_ROUNDS) {
    stopPolling()
    pollTimeout.value = true
    return
  }
  pollRounds++

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
const goToHistory = () => router.push('/resume/history')
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
    message.error('当前简历任务不存在')
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
    message.success('AI 简历润色完成')
  } catch (err) {
    if (err?.code === 'ECONNABORTED' || String(err?.message || '').includes('timeout')) {
      const recovered = await recoverLatestPolishResultAfterTimeout()
      if (recovered) {
        message.success('AI 简历润色已完成，结果已同步展示')
        return
      }
      message.warning('AI 润色请求超时，正在等待后端完成，请稍后刷新查看结果')
      return
    }
    console.error('[AI 简历润色] 执行失败:', err)
    message.error(err?.message || 'AI 简历润色失败，请稍后重试')
  } finally {
    polishLoading.value = false
  }
}

const submitJobMatchAnalysis = async () => {
  if (!task.value?.taskId) {
    message.error('当前简历任务不存在')
    return
  }
  if (!jobDescriptionText.value.trim()) {
    message.warning('请先输入岗位 JD 文本')
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
    message.success('岗位匹配分析完成')
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
    message.warning('暂无可复制的内容')
    return
  }
  try {
    await navigator.clipboard.writeText(editedText)
    message.success('已复制到剪贴板')
  } catch (err) {
    console.error('[AI 润色] 复制失败:', err)
    message.error('复制失败，请手动选择复制')
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
    message.warning('暂无可导出的润色内容')
    return
  }

  pdfExporting.value = true

  try {
    const canvas = await captureResumeCanvas()
    if (!canvas) {
      message.warning('暂无可导出的润色内容')
      return
    }

    const { default: jsPDF } = await import('jspdf')
    const filename = `${getExportFilename()}.pdf`

    // 将 canvas 转为 JPEG 数据，质量 0.95 平衡清晰度与文件大小
    const imgData = canvas.toDataURL('image/jpeg', 0.95)
    const pdf = new jsPDF({
      unit: 'mm',
      format: 'a4',
      orientation: 'portrait',
    })

    // PDF 不额外加边距，简历模板自身已有内边距；按页面宽度铺满后分页，避免长图被压窄。
    const pages = createResumePdfImagePages({
      canvasWidth: canvas.width,
      canvasHeight: canvas.height,
      margin: 0,
    })
    pages.forEach((page) => {
      if (page.addPage) {
        pdf.addPage()
      }
      pdf.addImage(imgData, 'JPEG', page.x, page.y, page.width, page.height)
    })
    pdf.save(filename)

    message.success('PDF 已导出')
  } catch (err) {
    console.error('[PDF导出] 失败:', err)
    message.error('PDF 导出失败，请稍后重试')
  } finally {
    pdfExporting.value = false
  }
}

// 导出简历图片：复用截图 canvas，输出为 PNG 文件下载。
const exportResumeImage = async () => {
  if (!resumeTemplateRef.value?.buildExportElement) {
    message.warning('暂无可导出的润色内容')
    return
  }

  imageExporting.value = true

  try {
    const canvas = await captureResumeCanvas()
    if (!canvas) {
      message.warning('暂无可导出的润色内容')
      return
    }

    const filename = `${getExportFilename()}.png`

    // canvas.toBlob 触发 PNG 下载
    canvas.toBlob((blob) => {
      if (!blob) {
        message.error('图片生成失败')
        return
      }
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      link.click()
      URL.revokeObjectURL(url)
    }, 'image/png')

    message.success('简历图片已导出')
  } catch (err) {
    console.error('[图片导出] 失败:', err)
    message.error('图片导出失败，请稍后重试')
  } finally {
    imageExporting.value = false
  }
}

onUnmounted(() => {
  unwatch()
})
</script>

<style scoped>
/* ============================================
   4pt 间距系统
   ============================================ */
.resume-result-view {
  --space-xs: 8px;
  --space-sm: 12px;
  --space-md: 16px;
  --space-lg: 24px;
  --space-xl: 32px;
  --space-xxl: 48px;

  min-height: 100%;
  background: var(--bg-page);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif;
  padding: var(--space-lg);
  box-sizing: border-box;
  font-family: var(--font-body);
  color: var(--text-body);
  max-width: 960px;
  margin: 0 auto;
}

/* ============================================
   轮询超时提示
   ============================================ */
.poll-timeout-section {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 48px);
}

.poll-timeout-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 48px 40px;
  text-align: center;
  max-width: 440px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-card);
}

.timeout-icon {
  width: 48px;
  height: 48px;
  color: #e6a23c;
  margin-bottom: 16px;
}

.timeout-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-title, #303133);
  margin: 0 0 8px;
}

.timeout-desc {
  font-size: 14px;
  color: var(--text-muted, #909399);
  margin: 0 0 24px;
  line-height: 1.6;
}

.timeout-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* ============================================
   顶部返回栏
   ============================================ */
.page-back {
  margin-bottom: 16px;
}

.back-btn {
  color: var(--text-muted, #909399);
}

/* ============================================
   加载状态
   ============================================ */
.loading-section,
.generating-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 420px;
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
  padding: var(--space-xl);
  text-align: center;
  max-width: 400px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-card);
  box-sizing: border-box;
  overflow: hidden;
}

.error-icon-wrap {
  margin-bottom: 20px;
}

.error-title {
  font-size: 1.25rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--text-title);
  margin-bottom: 8px;
}

.error-desc {
  font-size: 0.875rem;
  color: var(--text-body);
  margin-bottom: 24px;
  line-height: 1.6;
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
  position: relative;
  border-radius: 12px;
  margin-bottom: 24px;
  overflow: hidden;
}

.hero-glow {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: radial-gradient(ellipse 80% 60% at 30% 20%, rgba(255,255,255,0.15) 0%, transparent 70%);
  z-index: 0;
}

.hero-section.hero-2 {
  background: linear-gradient(135deg, var(--orange-main) 0%, var(--orange-deep) 100%);
  color: #fff;
}

.hero-section.hero-3 {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  color: var(--text-body);
}

.hero-section.hero-3 .ring-svg circle {
  stroke: var(--border-card) !important;
}

.hero-section.hero-3 .claude-badge {
  color: var(--text-body);
}

.hero-section.hero-3 .update-time {
  color: var(--text-muted);
}

.hero-section.hero-3 .parse-mode-pill {
  background: var(--orange-light-bg);
  color: var(--orange-deep);
}

.hero-section.hero-3 .parse-message {
  color: var(--text-muted);
}

.hero-section.hero-3 .parse-message {
  color: var(--text-muted);
}

.hero-section.hero-1 .claude-badge {
  color: var(--text-body);
}

.hero-section.hero-1 .update-time {
  color: var(--text-muted);
}

.hero-section.hero-1 .parse-mode-pill {
  background: var(--orange-light-bg);
  color: var(--orange-deep);
}

.hero-section.hero-1 .parse-message {
  color: var(--text-muted);
}

.hero-section.hero-1 {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  color: var(--text-body);
}

.hero-section.hero-1 .ring-svg circle {
  stroke: var(--border-card) !important;
}

.hero-section.hero-1 .claude-badge {
  color: var(--text-body);
}

.hero-body {
  padding: 40px 44px;
  display: flex;
  gap: 40px;
  align-items: flex-start;
  position: relative;
  z-index: 1;
}

.hero-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.hero-ring .ring-score {
  font-size: 28px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: #fff;
}

.hero-ring .ring-score.muted {
  color: var(--text-muted);
}

/* Level Badge - Claude badge-pill 风格 */
.level-badge {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.02em;
  padding: 4px 14px;
  border-radius: 9999px;
  background: rgba(255,255,255,0.9);
  color: var(--orange-deep);
  width: fit-content;
}

.level-excellent {
  background: rgba(255,255,255,0.9);
  color: var(--color-success);
}

.level-good {
  background: rgba(255,255,255,0.9);
  color: var(--orange-deep);
}

.level-fair {
  background: rgba(255,255,255,0.9);
  color: var(--color-warning);
}

.level-poor {
  background: rgba(255,255,255,0.9);
  color: var(--color-danger);
}

.hero-right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* Claude 状态 Badge - 左侧色条 + 无背景 */
.claude-badge {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  font-weight: 500;
  color: rgba(255,255,255,0.85);
  line-height: 1.4;
}

.claude-badge-bar {
  width: 3px;
  height: 16px;
  border-radius: 2px;
  flex-shrink: 0;
}

.badge-0 .claude-badge-bar { background: var(--color-warning); }
.badge-1 .claude-badge-bar { background: #79bbff; animation: claude-pulse 1.5s ease-in-out infinite; }
.badge-2 .claude-badge-bar { background: var(--color-success); }
.badge-3 .claude-badge-bar { background: var(--color-danger); }

@keyframes claude-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* Hero 中的 AI 总评 */
.ai-summary {
  padding: 20px 24px;
  background: rgba(255,255,255,0.1);
  border-radius: 12px;
  backdrop-filter: blur(8px);
}

.summary-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  color: rgba(255,255,255,0.7);
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.summary-text {
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
  color: #fff;
  word-break: break-word;
  white-space: pre-wrap;
  text-wrap: pretty;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.update-time {
  font-size: 12px;
  color: rgba(255,255,255,0.5);
}

.parse-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.parse-mode-pill {
  display: inline-flex;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.03em;
  padding: 2px 10px;
  border-radius: 9999px;
  background: rgba(255,255,255,0.15);
  color: rgba(255,255,255,0.8);
}

.parse-message {
  font-size: 12px;
  line-height: 1.6;
  color: rgba(255,255,255,0.6);
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
  font-variant-numeric: tabular-nums;
  color: var(--text-title);
  pointer-events: none;
  white-space: nowrap;
}

.ring-score.muted {
  color: var(--text-muted);
}

/* ============================================
   评分参考表
   ============================================ */
.grade-ref-card {
  overflow: visible;
}

.grade-table {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.grade-row {
  display: grid;
  grid-template-columns: 40px 72px 96px 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 10px;
  background: var(--orange-light-bg);
  border: 1px solid transparent;
  transition: all 0.2s;
}

.grade-row.active {
  border-color: var(--orange-main);
  background: #fff5ed;
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.12);
}

.grade-level {
  font-size: 18px;
  font-weight: 800;
  text-align: center;
}

.grade-s { color: var(--color-success); }
.grade-a { color: #409eff; }
.grade-b { color: #e6a23c; }
.grade-c { color: var(--text-muted); }
.grade-d { color: var(--color-danger); }

.grade-range {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-title);
}

.grade-label {
  font-size: 13px;
  color: var(--text-body);
}

.grade-stage {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.5;
}

.grade-marker {
  font-size: 12px;
  font-weight: 700;
  color: var(--orange-main);
  background: rgba(255, 140, 66, 0.12);
  padding: 2px 10px;
  border-radius: 999px;
  white-space: nowrap;
}

/* ============================================
   KPI 指标仪表盘 - Bento 风格
   ============================================ */
.kpi-section {
  margin-bottom: var(--space-lg);
}

.kpi-header {
  margin-bottom: var(--space-sm);
}

.kpi-title {
  margin: 0;
  font-size: 0.8125rem;
  font-weight: 500;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.kpi-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-sm);
}

.kpi-cell {
  background: var(--bg-card);
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  border-radius: 12px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-card);
}

.kpi-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.kpi-weight {
  font-size: 11px;
  font-weight: 400;
  color: var(--text-muted);
  opacity: 0.65;
}

.kpi-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-muted);
  letter-spacing: 0.02em;
}

.kpi-value-wrap {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.kpi-value {
  font-size: 32px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1;
  color: var(--text-title);
}

.kpi-value.coral { color: var(--orange-main); }
.kpi-value.green { color: var(--color-success); }
.kpi-value.blue { color: #5b8ec9; }
.kpi-value.purple { color: #9b7bc8; }
.kpi-value.teal { color: #4a9e9e; }
.kpi-value.gray { color: var(--text-muted); }

.kpi-unit {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-muted);
}

/* ============================================
   通用卡片区块 - Claude 纯色分层风格
   ============================================ */
.section-card {
  background: var(--bg-card);
  border-radius: 12px;
  margin-bottom: 20px;
  overflow: hidden;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-card);
  transition: box-shadow 0.2s;
}

.section-card:hover {
  box-shadow: var(--shadow-hover);
}

.section-card:last-child {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: 14px 24px;
  background: linear-gradient(135deg, var(--orange-light-bg) 0%, var(--bg-card) 100%);
  border-bottom: 1px solid var(--border-card);
}

.section-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: transparent;
  color: var(--orange-main);
}

.section-icon svg {
  stroke: var(--orange-main);
}

.section-icon.overall svg,
.section-icon.highlight svg,
.section-icon.skill svg,
.section-icon.basic svg,
.section-icon.experience svg,
.section-icon.optimization svg,
.section-icon.radar svg {
  stroke: var(--orange-main);
}

.section-icon.skill svg { stroke: #5b8ec9; }
.section-icon.basic svg { stroke: #9b7bc8; }
.section-icon.experience svg { stroke: #c8965b; }
.section-icon.optimization svg { stroke: var(--color-success); }
.section-icon.overall svg { stroke: var(--color-success); }
.section-icon.highlight svg { stroke: #c8a05b; }

.section-title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.3px;
  color: var(--text-title);
  text-wrap: balance;
}

.section-body {
  padding: 24px;
  padding-bottom: 28px;
}

/* 雷达图左右布局 */
.radar-layout {
  display: grid;
  grid-template-columns: 1.3fr 0.7fr;
  gap: 28px;
  align-items: center;
  padding: 8px 24px 16px;
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
  border-bottom: 1px solid var(--border-card);
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

.score-with-evaluation {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-card);
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
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: var(--space-sm);
}

.basic-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-card);
}

.basic-item:last-child {
  border-bottom: none;
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
}

.success {
  color: var(--color-success);
}

.warning {
  color: var(--color-warning);
}

/* ============================================
   标签列表 - 加分项/扣分项 标签风格
   ============================================ */
.section-sub-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-body);
  margin-bottom: 8px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
  white-space: normal;
}

.tag-strength {
  background: rgba(34, 197, 94, 0.1);
  color: var(--color-success);
  border: 1px solid rgba(34, 197, 94, 0.2);
}

.tag-weakness {
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-danger);
  border: 1px solid rgba(239, 68, 68, 0.2);
}

/* ============================================
   建议列表 - 左竖线风格
   ============================================ */
.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: var(--space-sm);
}

.suggestion-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-card);
}

.suggestion-item:last-child {
  border-bottom: none;
}

.suggestion-bar {
  width: 3px;
  min-height: 20px;
  border-radius: 2px;
  background: var(--orange-main);
  flex-shrink: 0;
  margin-top: 4px;
}

.suggestion-text {
  font-size: 14px;
  color: var(--text-body);
  line-height: 1.7;
  word-break: break-word;
  white-space: normal;
  flex: 1;
  text-wrap: pretty;
}

/* ============================================
   失败状态
   ============================================ */
.failed-section {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 48px 36px;
  text-align: center;
  margin-bottom: 20px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-card);
}

.failed-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-danger);
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
  background: var(--orange-light-bg);
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  line-height: 1.7;
}

/* ============================================
   岗位匹配分析
   ============================================ */
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
  border-color: var(--orange-main) !important;
  color: var(--orange-main) !important;
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
  border-radius: 12px;
  background: linear-gradient(135deg, var(--orange-light-bg) 0%, var(--bg-card) 100%);
  border: 1px solid var(--orange-border);
}

.job-match-score-label {
  font-size: 13px;
  color: var(--text-muted);
}

.job-match-score-value {
  margin-top: 8px;
  font-size: 36px;
  line-height: 1;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--orange-main);
}

.job-match-summary {
  padding: 14px 18px;
  border-radius: 12px;
  background: var(--orange-light-bg);
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
  border-radius: 12px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-card);
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
  gap: 8px;
}

.job-match-tag {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  line-height: 28px;
}

.job-match-tag.matched {
  background: rgba(93,184,114,0.12);
  color: #3d8a5a;
}

.job-match-tag.missing {
  background: rgba(198,69,69,0.1);
  color: #b53a3a;
}

.job-match-empty {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-muted);
}

/* ============================================
   AI 简历润色
   ============================================ */
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
  border-radius: 12px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-card);
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

/* ============================================
   底部操作区 - Claude 纯色按钮
   ============================================ */
.action-section {
  margin-top: var(--space-xl);
  padding: var(--space-xl) 0;
  border-top: 1px solid var(--border-card);
}

.action-group {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.action-btn {
  border-radius: 6px;
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 500;
  min-height: 40px;
}

.action-btn.secondary {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  color: var(--text-body);
}

.action-btn.secondary:hover {
  background: var(--orange-light-bg);
  border-color: var(--orange-main);
  color: var(--orange-main);
}

.action-btn.claude-primary {
  background: linear-gradient(135deg, var(--orange-main) 0%, var(--orange-deep) 100%);
  border: none;
  color: #fff;
  box-shadow: 0 4px 16px rgba(255, 140, 66, 0.3);
}

.action-btn.claude-primary:hover {
  opacity: 0.9;
  box-shadow: 0 6px 20px rgba(255, 140, 66, 0.4);
}

/* ============================================
   响应式
   ============================================ */
@media (max-width: 1023px) {
  .hero-body {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 28px;
  }

  .hero-left {
    flex-direction: row;
    gap: 20px;
  }

  .status-row {
    justify-content: center;
  }

  .parse-meta {
    justify-content: center;
  }
}

@media (max-width: 767px) {
  .resume-result-view {
    padding: 12px;
  }

  .hero-body {
    padding: 20px;
    gap: 24px;
  }

  .hero-left {
    gap: 12px;
  }

  .ring-score {
    font-size: 18px;
  }

  .kpi-grid {
    grid-template-columns: 1fr 1fr;
  }

  .kpi-cell {
    padding: 14px 16px;
  }

  .kpi-value {
    font-size: 24px;
  }

  .section-card {
    overflow: visible;
  }

  .section-header {
    padding: 12px 14px;
  }

  .section-body {
    padding: 14px;
  }

  .score-with-evaluation {
    flex-direction: column;
    gap: 12px;
  }

  .score-row-left {
    flex-direction: row;
    gap: 8px;
    min-width: auto;
  }

  .score-row-left .item-value {
    font-size: 24px;
  }

  .grade-table {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    margin: 0 -14px;
    padding: 0 14px;
  }

  .grade-row {
    grid-template-columns: 32px 60px 80px 1fr auto;
    gap: 8px;
    padding: 8px 10px;
  }

  .grade-level {
    font-size: 15px;
  }

  .grade-range {
    font-size: 12px;
  }

  .grade-label {
    font-size: 12px;
  }

  .grade-stage {
    font-size: 11px;
  }

  .basic-items-grid {
    grid-template-columns: 1fr;
  }

  .radar-layout {
    padding: 4px 14px 12px;
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

  .result-content {
    overflow-x: hidden;
  }
}

/* ===== 暗色模式适配 ===== */
[data-theme="dark"] .hero-section.hero-2 {
  background: linear-gradient(135deg, #8a4a2a 0%, #5a2d1a 100%);
}

[data-theme="dark"] .hero-section.hero-3,
[data-theme="dark"] .hero-section.hero-1 {
  background: var(--bg-card);
}

[data-theme="dark"] .hero-ring .ring-score {
  color: var(--text-title);
}

[data-theme="dark"] .ai-summary {
  background: var(--orange-light-bg);
}

[data-theme="dark"] .summary-text {
  color: var(--text-title);
}

[data-theme="dark"] .claude-badge {
  color: var(--text-body);
}

[data-theme="dark"] .update-time {
  color: var(--text-muted);
}

[data-theme="dark"] .parse-mode-pill {
  background: rgba(255,255,255,0.1);
  color: var(--text-body);
}

[data-theme="dark"] .level-badge {
  background: rgba(255,255,255,0.08);
  color: var(--text-title);
}

[data-theme="dark"] .section-card {
  background: var(--bg-card);
}

[data-theme="dark"] .section-header {
  background: var(--orange-light-bg);
  border-bottom-color: var(--border-card);
}

[data-theme="dark"] .kpi-cell {
  background: var(--bg-card);
}

[data-theme="dark"] .kpi-grid {
  background: var(--border-card);
}

[data-theme="dark"] .basic-items-grid {
  background: var(--border-card);
}

[data-theme="dark"] .basic-item {
  background: var(--bg-card);
}

[data-theme="dark"] .job-match-score-card {
  background: var(--orange-light-bg);
}

[data-theme="dark"] .job-match-result-block,
[data-theme="dark"] .job-match-suggestions,
[data-theme="dark"] .job-match-summary,
[data-theme="dark"] .polish-content-block {
  background: var(--orange-light-bg);
}

[data-theme="dark"] .job-match-tag.matched {
  background: rgba(93,184,114,0.15);
  color: #7dd899;
}

[data-theme="dark"] .job-match-tag.missing {
  background: rgba(198,69,69,0.15);
  color: #e08080;
}

[data-theme="dark"] .grade-row.active {
  background: rgba(255, 140, 66, 0.1);
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.06);
}

[data-theme="dark"] .grade-marker {
  background: rgba(255, 140, 66, 0.22);
}

[data-theme="dark"] .hero-glow {
  background: radial-gradient(ellipse 80% 60% at 30% 20%, rgba(0,0,0,0.25) 0%, transparent 70%);
}

[data-theme="dark"] .parse-message {
  color: var(--text-muted);
}

[data-theme="dark"] .tag-strength {
  background: rgba(34, 197, 94, 0.08);
  border-color: rgba(34, 197, 94, 0.15);
}

[data-theme="dark"] .tag-weakness {
  background: rgba(239, 68, 68, 0.08);
  border-color: rgba(239, 68, 68, 0.15);
}

/* ============================================
   入场动效 - Stagger Fade In
   ============================================ */
@keyframes claude-fade-up {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.result-content > * {
  animation: claude-fade-up 0.5s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.result-content > *:nth-child(1) { animation-delay: 0s; }
.result-content > *:nth-child(2) { animation-delay: 0.08s; }
.result-content > *:nth-child(3) { animation-delay: 0.16s; }
.result-content > *:nth-child(4) { animation-delay: 0.24s; }
.result-content > *:nth-child(5) { animation-delay: 0.32s; }
.result-content > *:nth-child(6) { animation-delay: 0.40s; }
.result-content > *:nth-child(7) { animation-delay: 0.48s; }
.result-content > *:nth-child(8) { animation-delay: 0.56s; }
.result-content > *:nth-child(9) { animation-delay: 0.64s; }

@media (prefers-reduced-motion: reduce) {
  .result-content > * {
    animation: none;
  }
}
</style>

<style>
[data-theme="dark"] .job-match-panel .n-input {
  --n-color: var(--bg-card) !important;
  --n-text-color: var(--text-body) !important;
  --n-border: 1px solid var(--border-card) !important;
  --n-caret-color: var(--orange-main) !important;
}

[data-theme="dark"] .job-match-panel .n-input.n-input--textarea .n-input__textarea {
  background-color: var(--bg-card) !important;
  color: var(--text-body) !important;
}

[data-theme="dark"] .job-match-actions .n-button.n-button--secondary {
  --n-color: rgba(255, 140, 66, 0.06) !important;
  --n-text-color: var(--orange-main) !important;
  --n-border: 1px solid var(--orange-main) !important;
  --n-border-hover: 1px solid var(--orange-main) !important;
}

[data-theme="dark"] .job-match-actions .n-button.n-button--secondary:hover {
  --n-color: rgba(255, 140, 66, 0.15) !important;
}

[data-theme="dark"] .polish-actions .n-button.n-button--ghost {
  --n-text-color: var(--orange-main) !important;
  --n-border: 1px solid var(--orange-main) !important;
  --n-border-hover: 1px solid var(--orange-main) !important;
}

[data-theme="dark"] .polish-actions .n-button.n-button--ghost:hover {
  --n-color: rgba(255, 140, 66, 0.15) !important;
}

[data-theme="dark"] .polish-actions .n-button.n-button--primary-type {
  --n-color: var(--orange-main) !important;
  --n-text-color: #fff !important;
  --n-border: 1px solid var(--orange-main) !important;
  --n-border-hover: 1px solid var(--orange-deep) !important;
}

[data-theme="dark"] .polish-actions .n-button.n-button--primary-type:hover {
  --n-color: var(--orange-deep) !important;
}
</style>
