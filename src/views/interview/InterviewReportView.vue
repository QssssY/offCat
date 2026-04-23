<template>
  <div class="interview-report-view">
    <!-- 返回入口 - 页面级 -->
    <div class="page-back">
      <el-button link @click="goBack" class="back-btn">
        <el-icon><ArrowLeft /></el-icon>
        返回历史记录
      </el-button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-section">
      <div class="loading-content">
        <div class="loading-ring"></div>
        <div class="loading-text">AI 正在生成评价报告...</div>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-section">
      <div class="error-card">
        <div class="error-icon-wrap">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="#f56c6c" stroke-width="2" />
            <line x1="15" y1="9" x2="9" y2="15" stroke="#f56c6c" stroke-width="2" />
            <line x1="9" y1="9" x2="15" y2="15" stroke="#f56c6c" stroke-width="2" />
          </svg>
        </div>
        <div class="error-title">加载失败</div>
        <div class="error-desc">{{ error }}</div>
        <div class="error-actions">
          <el-button type="primary" @click="fetchSessionDetail">重试</el-button>
          <el-button @click="goBack">返回</el-button>
        </div>
      </div>
    </div>

    <!-- 未结束状态 -->
    <div v-else-if="!isEnded" class="empty-section">
      <div class="empty-card">
        <div class="empty-icon-wrap">
          <svg width="56" height="56" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="#F3D8C7" stroke-width="2" />
            <polyline points="12 6 12 12 16 14" stroke="#FF8C42" stroke-width="2" stroke-linecap="round" />
          </svg>
        </div>
        <div class="empty-title">面试尚未结束</div>
        <div class="empty-desc">
          当前面试结束后，可查看综合评分与完整评价报告
        </div>
        <div class="empty-actions">
          <el-button type="primary" @click="goToSession">返回会话</el-button>
          <el-button @click="goBack">返回</el-button>
        </div>
      </div>
    </div>

    <!-- 【修复】报告生成中状态（会话已结束但报告尚未生成） -->
    <div v-else-if="isReportGenerating" class="generating-section">
      <div class="generating-card">
        <div class="generating-icon-wrap">
          <div class="generating-spinner"></div>
        </div>
        <div class="generating-title">报告生成中</div>
        <div class="generating-desc">
          {{ generatingDesc }}
        </div>
        <div class="generating-progress">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
          </div>
          <div class="progress-text">请稍候，自动刷新中...</div>
        </div>
        <div class="generating-actions">
          <el-button type="primary" size="small" :loading="refreshingReport" @click="refreshReportNow">
            立即刷新
          </el-button>
          <el-button size="small" @click="goToSession">查看会话</el-button>
        </div>
      </div>
    </div>

    <!-- 报告内容 -->
    <div v-else-if="hasReport" class="report-content">
      <!-- Hero 报告总览区 -->
      <div class="hero-section">
        <div class="hero-main">
          <div class="hero-left">
            <div class="job-info">
              <span class="job-label">面试岗位</span>
              <span class="job-name">{{ sessionData?.jobRole || "-" }}</span>
            </div>
            <div class="score-display">
              <template v-if="displayScoreValue !== null">
                <span class="score-number">{{ displayScoreValue }}</span>
                <span class="score-unit">分</span>
              </template>
              <template v-else>
                <span class="score-number muted">--</span>
              </template>
            </div>
            <div
              class="level-badge"
              :class="levelBadgeClass"
              v-if="parsedReport?.level"
            >
              {{ parsedReport.level }}
            </div>
          </div>
          <div class="hero-right">
            <div class="ai-conclusion" v-if="parsedReport?.summary">
              <div class="conclusion-label">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M12 2a10 10 0 1 0 10 10" />
                  <path d="M12 6v6l4 2" />
                </svg>
                AI 诊断结论
              </div>
              <p class="conclusion-text">{{ parsedReport.summary }}</p>
            </div>
          </div>
        </div>
        <div class="hero-meta">
          <div class="meta-item">
            <span class="meta-label">面试状态</span>
            <span class="meta-value status-ended">{{
              statusDesc || "已结束"
            }}</span>
          </div>
          <div class="meta-divider"></div>
          <div class="meta-item">
            <span class="meta-label">面试难度</span>
            <span class="meta-value">{{
              difficultyDesc || difficultyFallback
            }}</span>
          </div>
          <div class="meta-divider"></div>
          <div class="meta-item">
            <span class="meta-label">面试模式</span>
            <span class="meta-value">{{
              interviewModeDesc || modeFallback
            }}</span>
          </div>
        </div>
      </div>

      <!-- KPI 指标区 -->
      <div v-if="hasReport" class="kpi-section">
        <div class="kpi-grid">
          <div class="kpi-card kpi-with-ring">
            <div class="kpi-label">综合评分</div>
            <div class="ring-wrapper">
              <svg width="80" height="80" viewBox="0 0 80 80" class="ring-svg">
                <circle
                  cx="40"
                  cy="40"
                  r="34"
                  fill="none"
                  stroke="#f3d8c7"
                  stroke-width="6"
                />
                <circle
                  cx="40"
                  cy="40"
                  r="34"
                  fill="none"
                  :stroke="getScoreColor(displayScoreValue)"
                  stroke-width="6"
                  stroke-linecap="round"
                  :stroke-dasharray="`${(displayScoreValue || 0) * 2.13} 213`"
                  transform="rotate(-90 40 40)"
                />
              </svg>
              <span class="ring-score">{{
                displayScoreValue !== null ? displayScoreValue : "--"
              }}</span>
            </div>
          </div>
          <div class="kpi-card kpi-simple">
            <div class="kpi-label">报告等级</div>
            <div class="kpi-number" :class="levelValueClass">
              {{ parsedReport?.level || "--" }}
            </div>
          </div>
          <div class="kpi-card kpi-simple">
            <div class="kpi-label">面试状态</div>
            <div class="kpi-number">{{ statusDesc || "已结束" }}</div>
          </div>
          <div class="kpi-card kpi-simple">
            <div class="kpi-label">面试难度</div>
            <div class="kpi-number">
              {{ difficultyDesc || difficultyFallback }}
            </div>
          </div>
        </div>
      </div>

      <!-- 无报告回退 -->
      <div v-if="!hasReport" class="report-empty-card">
        <div class="empty-icon-wrap">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
            <path
              d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"
              stroke="#c0c4cc"
              stroke-width="2"
            />
            <polyline
              points="14 2 14 8 20 8"
              stroke="#c0c4cc"
              stroke-width="2"
            />
          </svg>
        </div>
        <div class="empty-title">暂无评价报告</div>
        <div class="empty-desc">
          {{
            reportPolling
              ? "系统正在生成中，页面将自动刷新..."
              : "系统正在生成中，请稍后再来"
          }}
        </div>
        <el-button
          type="primary"
          plain
          size="small"
          :loading="refreshingReport"
          @click="refreshReportNow"
        >
          立即刷新
        </el-button>
      </div>

      <template v-else>
        <!-- 优势亮点 -->
        <div v-if="reportStrengths.length > 0" class="section-card">
          <div class="section-header">
            <div class="section-icon strength">
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                <polyline points="22 4 12 14.01 9 11.01" />
              </svg>
            </div>
            <h3 class="section-title">优势亮点</h3>
          </div>
          <div class="section-body">
            <div class="strength-grid">
              <div
                v-for="(item, idx) in reportStrengths"
                :key="idx"
                class="strength-item"
              >
                <div class="strength-check">
                  <svg
                    width="14"
                    height="14"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2.5"
                  >
                    <polyline points="20 6 9 17 4 12" />
                  </svg>
                </div>
                <span class="strength-text">{{ item }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 面试表现标签 -->
        <div v-if="reportPerformanceTags.length > 0" class="section-card">
          <div class="section-header">
            <div class="section-icon tag-icon">
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"
                />
                <line x1="7" y1="7" x2="7.01" y2="7" />
              </svg>
            </div>
            <h3 class="section-title">面试表现标签</h3>
          </div>
          <div class="section-body">
            <div class="tag-list">
              <el-tag
                v-for="(tag, idx) in reportPerformanceTags"
                :key="idx"
                type="warning"
                >{{ tag }}</el-tag
              >
            </div>
          </div>
        </div>

        <!-- 各维度评分 -->
        <div v-if="reportDimensions" class="section-card">
          <div class="section-header">
            <div class="section-icon dimension">
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <line x1="18" y1="20" x2="18" y2="10" />
                <line x1="12" y1="20" x2="12" y2="4" />
                <line x1="6" y1="20" x2="6" y2="14" />
              </svg>
            </div>
            <h3 class="section-title">各维度评分</h3>
          </div>
          <div class="section-body">
            <div class="score-grid">
              <div class="score-card">
                <div class="title">系统设计</div>
                <div class="ring-wrapper">
                  <svg
                    width="80"
                    height="80"
                    viewBox="0 0 80 80"
                    class="ring-svg"
                  >
                    <circle
                      cx="40"
                      cy="40"
                      r="34"
                      fill="none"
                      stroke="#f3d8c7"
                      stroke-width="6"
                    />
                    <circle
                      cx="40"
                      cy="40"
                      r="34"
                      fill="none"
                      :stroke="
                        getDimensionColor(
                          Number(parsedReport?.dimensions?.systemDesign)
                        )
                      "
                      stroke-width="6"
                      stroke-linecap="round"
                      :stroke-dasharray="`${
                        (Number(parsedReport?.dimensions?.systemDesign) || 0) *
                        2.13
                      } 213`"
                      transform="rotate(-90 40 40)"
                    />
                  </svg>
                  <span class="ring-score">{{
                    Math.round(
                      Number(parsedReport?.dimensions?.systemDesign)
                    ) || 0
                  }}</span>
                </div>
              </div>
              <div class="score-card">
                <div class="title">沟通表达</div>
                <div class="ring-wrapper">
                  <svg
                    width="80"
                    height="80"
                    viewBox="0 0 80 80"
                    class="ring-svg"
                  >
                    <circle
                      cx="40"
                      cy="40"
                      r="34"
                      fill="none"
                      stroke="#f3d8c7"
                      stroke-width="6"
                    />
                    <circle
                      cx="40"
                      cy="40"
                      r="34"
                      fill="none"
                      :stroke="
                        getDimensionColor(
                          Number(parsedReport?.dimensions?.communication)
                        )
                      "
                      stroke-width="6"
                      stroke-linecap="round"
                      :stroke-dasharray="`${
                        (Number(parsedReport?.dimensions?.communication) || 0) *
                        2.13
                      } 213`"
                      transform="rotate(-90 40 40)"
                    />
                  </svg>
                  <span class="ring-score">{{
                    Math.round(
                      Number(parsedReport?.dimensions?.communication)
                    ) || 0
                  }}</span>
                </div>
              </div>
              <div class="score-card">
                <div class="title">问题解决</div>
                <div class="ring-wrapper">
                  <svg
                    width="80"
                    height="80"
                    viewBox="0 0 80 80"
                    class="ring-svg"
                  >
                    <circle
                      cx="40"
                      cy="40"
                      r="34"
                      fill="none"
                      stroke="#f3d8c7"
                      stroke-width="6"
                    />
                    <circle
                      cx="40"
                      cy="40"
                      r="34"
                      fill="none"
                      :stroke="
                        getDimensionColor(
                          Number(parsedReport?.dimensions?.problemSolving)
                        )
                      "
                      stroke-width="6"
                      stroke-linecap="round"
                      :stroke-dasharray="`${
                        (Number(parsedReport?.dimensions?.problemSolving) ||
                          0) * 2.13
                      } 213`"
                      transform="rotate(-90 40 40)"
                    />
                  </svg>
                  <span class="ring-score">{{
                    Math.round(
                      Number(parsedReport?.dimensions?.problemSolving)
                    ) || 0
                  }}</span>
                </div>
              </div>
              <div class="score-card">
                <div class="title">技术深度</div>
                <div class="ring-wrapper">
                  <svg
                    width="80"
                    height="80"
                    viewBox="0 0 80 80"
                    class="ring-svg"
                  >
                    <circle
                      cx="40"
                      cy="40"
                      r="34"
                      fill="none"
                      stroke="#f3d8c7"
                      stroke-width="6"
                    />
                    <circle
                      cx="40"
                      cy="40"
                      r="34"
                      fill="none"
                      :stroke="
                        getDimensionColor(
                          Number(parsedReport?.dimensions?.technicalDepth)
                        )
                      "
                      stroke-width="6"
                      stroke-linecap="round"
                      :stroke-dasharray="`${
                        (Number(parsedReport?.dimensions?.technicalDepth) ||
                          0) * 2.13
                      } 213`"
                      transform="rotate(-90 40 40)"
                    />
                  </svg>
                  <span class="ring-score">{{
                    Math.round(
                      Number(parsedReport?.dimensions?.technicalDepth)
                    ) || 0
                  }}</span>
                </div>
              </div>
            </div>
            <div class="dimension-comments" v-if="dimensionComments.length > 0">
              <div
                v-for="comment in dimensionComments"
                :key="comment.key"
                class="dimension-comment"
              >
                <div class="comment-title">{{ comment.label }}</div>
                <div class="comment-text">{{ comment.text }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 待提升方向 -->
        <div v-if="improvementList.length > 0" class="section-card">
          <div class="section-header">
            <div class="section-icon improvement">
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
            </div>
            <h3 class="section-title">待提升方向</h3>
          </div>
          <div class="section-body">
            <div class="improvement-list">
              <div
                v-for="(item, idx) in improvementList"
                :key="idx"
                class="improvement-item"
              >
                <div class="improvement-dot"></div>
                <span class="improvement-text">{{ item }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 不足与建议 -->
        <div class="section-card">
          <div class="section-header">
            <div class="section-icon weakness">
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
            </div>
            <h3 class="section-title">不足与建议</h3>
          </div>
          <div class="section-body">
            <div class="action-list">
              <div
                v-for="(item, idx) in reportSuggestions"
                :key="'s-' + idx"
                class="action-item"
              >
                <div class="action-index">{{ idx + 1 }}</div>
                <div class="action-content">
                  <span class="action-text">{{ item }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 危险信号 -->
        <div v-if="reportRedFlags.length > 0" class="section-card">
          <div class="section-header">
            <div class="section-icon redflag">
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"
                />
                <line x1="4" y1="22" x2="4" y2="15" />
              </svg>
            </div>
            <h3 class="section-title">危险信号</h3>
          </div>
          <div class="section-body">
            <div class="weakness-list">
              <div
                v-for="(item, idx) in reportRedFlags"
                :key="idx"
                class="weakness-item"
              >
                <div class="weakness-dot" style="background: #f56c6c"></div>
                <span class="weakness-text">{{ item }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 关键问题 -->
        <div v-if="reportCriticalIssues.length > 0" class="section-card">
          <div class="section-header">
            <div class="section-icon critical">
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <polygon
                  points="7.86 2 16.86 2 22 7.86 22 16.86 16.86 22 7.86 22 2 16.86 2 7.86 7.86 2"
                />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
            </div>
            <h3 class="section-title">关键问题</h3>
          </div>
          <div class="section-body">
            <div class="weakness-list">
              <div
                v-for="(item, idx) in reportCriticalIssues"
                :key="idx"
                class="weakness-item"
              >
                <div class="weakness-dot" style="background: #f56c6c"></div>
                <span class="weakness-text">{{ item }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 缺失能力 -->
        <div v-if="reportMissingCompetencies.length > 0" class="section-card">
          <div class="section-header">
            <div class="section-icon missing">
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <circle cx="12" cy="12" r="10" />
                <line x1="8" y1="12" x2="16" y2="12" />
              </svg>
            </div>
            <h3 class="section-title">缺失能力</h3>
          </div>
          <div class="section-body">
            <div class="weakness-list">
              <div
                v-for="(item, idx) in reportMissingCompetencies"
                :key="idx"
                class="weakness-item"
              >
                <div class="weakness-dot" style="background: #e6a23c"></div>
                <span class="weakness-text">{{ item }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 拒绝原因 -->
        <div v-if="reportRejectionReasons.length > 0" class="section-card">
          <div class="section-header">
            <div class="section-icon rejection">
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <circle cx="12" cy="12" r="10" />
                <line x1="15" y1="9" x2="9" y2="15" />
                <line x1="9" y1="9" x2="15" y2="15" />
              </svg>
            </div>
            <h3 class="section-title">拒绝原因</h3>
          </div>
          <div class="section-body">
            <div class="weakness-list">
              <div
                v-for="(item, idx) in reportRejectionReasons"
                :key="idx"
                class="weakness-item"
              >
                <div class="weakness-dot" style="background: #f56c6c"></div>
                <span class="weakness-text">{{ item }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 逐题表现 -->
        <div v-if="reportQuestionPerformance.length > 0" class="section-card">
          <div class="section-header">
            <div class="section-icon question">
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"
                />
                <polyline points="14 2 14 8 20 8" />
                <line x1="16" y1="13" x2="8" y2="13" />
                <line x1="16" y1="17" x2="8" y2="17" />
              </svg>
            </div>
            <h3 class="section-title">逐题表现</h3>
          </div>
          <div class="section-body">
            <div
              v-for="(q, idx) in reportQuestionPerformance"
              :key="idx"
              class="question-item"
            >
              <div class="question-header">
                <span class="question-index">Q{{ idx + 1 }}</span>
                <span class="question-text">{{ q.question }}</span>
              </div>
              <div class="question-answer">{{ q.answer }}</div>
              <div class="question-footer">
                <div class="question-score">
                  <span class="qs-label">得分</span>
                  <div class="ring-wrapper" style="width: 44px; height: 44px">
                    <svg
                      width="44"
                      height="44"
                      viewBox="0 0 44 44"
                      class="ring-svg"
                    >
                      <circle
                        cx="22"
                        cy="22"
                        r="18"
                        fill="none"
                        stroke="#f3d8c7"
                        stroke-width="4"
                      />
                      <circle
                        cx="22"
                        cy="22"
                        r="18"
                        fill="none"
                        :stroke="getScoreColor(q.score)"
                        stroke-width="4"
                        stroke-linecap="round"
                        :stroke-dasharray="`${
                          (Number(q.score) || 0) * 1.13
                        } 113`"
                        transform="rotate(-90 22 22)"
                      />
                    </svg>
                    <span class="ring-score" style="font-size: 14px">{{
                      q.score
                    }}</span>
                  </div>
                </div>
                <el-tag
                  v-for="(tag, tidx) in q.knowledgeTags || []"
                  :key="tidx"
                  size="small"
                  >{{ tag }}</el-tag
                >
              </div>
              <div class="question-comment" v-if="q.comment">
                {{ q.comment }}
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 底部操作区 -->
      <div class="action-section">
        <div class="action-group">
          <el-button @click="goBack" class="action-btn secondary"
            >返回历史</el-button
          >
          <el-button @click="goToSession" class="action-btn secondary"
            >查看会话</el-button
          >
          <el-button
            type="primary"
            class="action-btn primary"
            @click="goToEntry"
          >
            再来一次
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ArrowLeft } from "@element-plus/icons-vue";
import { getInterviewSession } from "@/api/interview";
import { ElMessage } from "element-plus";

const route = useRoute();
const router = useRouter();
const sessionId = computed(() => route.params.sessionId);

const loading = ref(true);
const error = ref("");
const sessionData = ref(null);
const reportPolling = ref(false);
const refreshingReport = ref(false);
const reportPollRounds = ref(0);
const REPORT_POLL_INTERVAL_MS = 3000;
const REPORT_POLL_MAX_ROUNDS = 120;
let reportPollingTimer = null;

const isEnded = computed(() => sessionData.value?.status === 1);

// 新增：报告生成中状态区分（会话已结束但报告尚未生成）
// 修复目的：解决"会话已结束但报告仍在生成"的过渡态显示问题
const isReportGenerating = computed(() => {
  // 条件1：会话已结束（status === 1）
  // 条件2：报告尚未生成（hasReport 为 false）
  // 条件3：轮询仍在进行（reportPolling 为 true）
  return isEnded.value && !hasReport.value && reportPolling.value;
});

// 动态描述文案
const generatingDesc = computed(() => {
  const rounds = reportPollRounds.value;
  if (rounds <= 3) {
    return "AI 面试官正在撰写个性化评价报告，请稍候...";
  } else if (rounds <= 10) {
    return "报告生成中，预计还需等待几秒钟...";
  } else {
    return "报告生成时间较长，请耐心等待...";
  }
});

// 进度条百分比（模拟）
const progressPercent = computed(() => {
  const rounds = reportPollRounds.value;
  const max = REPORT_POLL_MAX_ROUNDS;
  const pct = Math.min(Math.round((rounds / max) * 100), 95);
  return pct;
});

const statusDesc = computed(() => sessionData.value?.statusDesc || "");

const difficultyDesc = computed(() => sessionData.value?.difficultyDesc || "");

const difficultyFallback = computed(() => {
  const d = sessionData.value?.difficulty;
  if (d === 1) return "初级";
  if (d === 2) return "中级";
  if (d === 3) return "高级";
  return "--";
});

const interviewModeDesc = computed(
  () => sessionData.value?.interviewModeDesc || ""
);

const modeFallback = computed(() => {
  const m = sessionData.value?.interviewMode;
  if (m === "stress") return "压力面试";
  if (m === "normal") return "普通面试";
  return "普通面试";
});

const displayScoreValue = computed(() => {
  const score = sessionData.value?.comprehensiveScore;
  return score === null || score === undefined ? null : Number(score);
});

const parsedReport = computed(() => {
  const report = sessionData.value?.evaluationReport;
  if (report === null || report === undefined || report === "") return null;
  if (typeof report === "string") {
    // 兼容后端可能返回的 markdown 包裹格式，避免“报告已生成但前端解析失败”。
    let trimmed = report.trim();
    if (trimmed.startsWith("```json")) {
      trimmed = trimmed.substring(7);
    } else if (trimmed.startsWith("```")) {
      trimmed = trimmed.substring(3);
    }
    const lastBacktick = trimmed.lastIndexOf("```");
    if (lastBacktick > 0) {
      trimmed = trimmed.substring(0, lastBacktick);
    }
    trimmed = trimmed.trim();
    if (!trimmed) return null;
    try {
      return JSON.parse(trimmed);
    } catch {
      return null;
    }
  }
  return report;
});

const hasReport = computed(() => {
  return parsedReport.value !== null;
});
const shouldPollReport = computed(() => isEnded.value && !hasReport.value);

const levelBadgeClass = computed(() => {
  const level = parsedReport.value?.level;
  if (level === "优秀" || level === "A") return "level-excellent";
  if (level === "良好" || level === "B") return "level-good";
  if (level === "一般" || level === "C") return "level-fair";
  return "level-poor";
});

const levelValueClass = computed(() => {
  return levelBadgeClass.value;
});

const reportStrengths = computed(() => {
  const val = parsedReport.value?.strengths;
  if (!val) return [];
  if (Array.isArray(val)) return val;
  return [];
});

const reportSuggestions = computed(() => {
  const val = parsedReport.value?.suggestions;
  if (!val) return [];
  if (Array.isArray(val)) return val;
  return [];
});

const reportImprovements = computed(() => {
  const val = parsedReport.value?.improvements;
  if (!val) return [];
  if (Array.isArray(val)) return val;
  return [];
});

const reportDimensions = computed(() => {
  const val = parsedReport.value?.dimensions;
  if (!val || typeof val !== "object") return null;
  return val;
});

const dimensionComments = computed(() => {
  const comments = [];
  const report = parsedReport.value;
  if (!report) return comments;
  if (report.communication?.comment)
    comments.push({
      key: "comm",
      label: "沟通表达",
      text: report.communication.comment,
    });
  if (report.problemSolving?.comment)
    comments.push({
      key: "prob",
      label: "问题解决",
      text: report.problemSolving.comment,
    });
  if (report.technicalDepth?.comment)
    comments.push({
      key: "tech",
      label: "技术深度",
      text: report.technicalDepth.comment,
    });
  if (report.systemDesign?.comment)
    comments.push({
      key: "sys",
      label: "系统设计",
      text: report.systemDesign.comment,
    });
  return comments;
});

const improvementList = computed(() => {
  const merged = [
    ...(parsedReport.value?.improvements || []),
    ...(parsedReport.value?.weaknesses || []),
  ];
  return [...new Set(merged)];
});

const reportWeaknesses = computed(() => {
  const val = parsedReport.value?.weaknesses;
  if (!val) return [];
  return Array.isArray(val) ? val : [];
});

const reportRedFlags = computed(() => {
  const val = parsedReport.value?.redFlags;
  if (!val) return [];
  return Array.isArray(val) ? val : [];
});

const reportCriticalIssues = computed(() => {
  const val = parsedReport.value?.criticalIssues;
  if (!val) return [];
  return Array.isArray(val) ? val : [];
});

const reportMissingCompetencies = computed(() => {
  const val = parsedReport.value?.missingCompetencies;
  if (!val) return [];
  return Array.isArray(val) ? val : [];
});

const reportRejectionReasons = computed(() => {
  const val = parsedReport.value?.rejectionReasons;
  if (!val) return [];
  return Array.isArray(val) ? val : [];
});

const reportPerformanceTags = computed(() => {
  const val = parsedReport.value?.interviewPerformanceTags;
  if (!val) return [];
  return Array.isArray(val) ? val : [];
});

const reportQuestionPerformance = computed(() => {
  const val = parsedReport.value?.questionPerformance;
  if (!val) return [];
  return Array.isArray(val) ? val : [];
});

const dimensionNameMap = {
  systemDesign: "系统设计",
  communication: "沟通表达",
  problemSolving: "问题解决",
  technicalDepth: "技术深度",
  codeQuality: "代码质量",
  architecture: "架构能力",
  experience: "实践经验",
};

const getDimensionColor = (score) => {
  if (score >= 80) return "#67C23A";
  if (score >= 60) return "#E6A23C";
  return "#F56C6C";
};

const getScoreColor = (score) => {
  if (score >= 80) return "#67C23A";
  if (score >= 60) return "#E6A23C";
  return "#F56C6C";
};

const fetchSessionDetail = async (options = {}) => {
  const { showLoading = true, silentError = false } = options;
  if (!sessionId.value) {
    if (!silentError) {
      error.value = "会话ID不存在";
    }
    if (showLoading) {
      loading.value = false;
    }
    return;
  }

  if (showLoading) {
    loading.value = true;
  }
  if (!silentError) {
    error.value = "";
  }

  try {
    const res = await getInterviewSession(sessionId.value);
    sessionData.value = res.data;
  } catch (err) {
    if (!silentError) {
      error.value = err.message || "获取评价报告失败，请稍后重试";
    }
  } finally {
    if (showLoading) {
      loading.value = false;
    }
  }
};

/**
 * 启动报告轮询。
 * 作用：报告尚未落库时自动刷新，用户留在报告页即可看到最终结果。
 */
const startReportPolling = () => {
  if (reportPollingTimer || !sessionId.value) return;
  reportPolling.value = true;
  reportPollRounds.value = 0;
  reportPollingTimer = setInterval(async () => {
    if (refreshingReport.value) return;
    refreshingReport.value = true;
    try {
      await fetchSessionDetail({ showLoading: false, silentError: true });
      reportPollRounds.value += 1;
      if (hasReport.value) {
        stopReportPolling();
        ElMessage.success("评价报告已生成");
        return;
      }
      if (reportPollRounds.value >= REPORT_POLL_MAX_ROUNDS) {
        stopReportPolling();
      }
    } finally {
      refreshingReport.value = false;
    }
  }, REPORT_POLL_INTERVAL_MS);
};

/**
 * 停止报告轮询，避免页面离开后继续请求。
 */
const stopReportPolling = () => {
  if (reportPollingTimer) {
    clearInterval(reportPollingTimer);
    reportPollingTimer = null;
  }
  reportPolling.value = false;
  reportPollRounds.value = 0;
};

const refreshReportNow = async () => {
  if (!sessionId.value) return;
  refreshingReport.value = true;
  try {
    await fetchSessionDetail({ showLoading: false, silentError: true });
  } finally {
    refreshingReport.value = false;
  }
};

const goBack = () => {
  router.push("/interview/history");
};

const goToSession = () => {
  if (!sessionId.value) return;
  router.push(`/interview/session/${sessionId.value}`);
};

const goToEntry = () => {
  router.push("/interview/entry");
};

onMounted(() => {
  fetchSessionDetail();
});

watch(
  shouldPollReport,
  (needPolling) => {
    if (needPolling) {
      startReportPolling();
      return;
    }
    stopReportPolling();
  },
  { immediate: true }
);

onUnmounted(() => {
  stopReportPolling();
});
</script>

<style scoped>
.interview-report-view {
  min-height: 100%;
  background: #f8f6f3;
  padding: 24px;
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
  width: 56px;
  height: 56px;
  border: 4px solid #f3d8c7;
  border-top-color: #ff8c42;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
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
   空状态
   ============================================ */
.empty-section {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

/* ============================================
   报告生成中状态（新增）
   修复目的：明确显示"会话已结束但报告仍在生成"的过渡态
   ============================================ */
.generating-section {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.generating-card {
  background: #fff;
  border-radius: 20px;
  padding: 48px;
  text-align: center;
  max-width: 420px;
  border: 1px solid rgba(243, 216, 199, 0.5);
  box-shadow: 0 4px 20px rgba(255, 140, 66, 0.08);
}

.generating-icon-wrap {
  margin-bottom: 24px;
}

.generating-spinner {
  width: 56px;
  height: 56px;
  margin: 0 auto;
  border: 4px solid #f3d8c7;
  border-top-color: #ff8c42;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.generating-title {
  font-size: 20px;
  font-weight: 600;
  color: #2f2f2f;
  margin-bottom: 8px;
}

.generating-desc {
  font-size: 14px;
  color: #666;
  margin-bottom: 24px;
  line-height: 1.6;
}

.generating-progress {
  margin-bottom: 24px;
}

.progress-bar {
  height: 6px;
  background: #f3d8c7;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff8c42 0%, #ffb380 100%);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: #999;
}

.generating-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.empty-card {
  background: #fff;
  border-radius: 20px;
  padding: 48px;
  text-align: center;
  max-width: 420px;
  border: 1px solid rgba(243, 216, 199, 0.5);
  box-shadow: 0 4px 20px rgba(255, 140, 66, 0.06);
}

.empty-icon-wrap {
  margin-bottom: 20px;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #2f2f2f;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: #909399;
  margin-bottom: 24px;
}

.empty-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* ============================================
   Hero 报告总览区
   ============================================ */
.hero-section {
  background: #fff;
  border-radius: 20px;
  padding: 28px 32px;
  margin-bottom: 20px;
  border: 1px solid rgba(243, 216, 199, 0.5);
  box-shadow: 0 4px 20px rgba(255, 140, 66, 0.06);
}

.hero-main {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.hero-left {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 160px;
}

.job-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.job-label {
  font-size: 12px;
  color: #999;
}

.job-name {
  font-size: 15px;
  font-weight: 600;
  color: #2f2f2f;
}

.score-display {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.score-number {
  font-size: 56px;
  font-weight: 700;
  color: #ff8c42;
  line-height: 1;
}

.score-number.muted {
  color: #c0c4cc;
}

.score-unit {
  font-size: 18px;
  color: #ff8c42;
  font-weight: 500;
}

.level-badge {
  font-size: 13px;
  font-weight: 600;
  padding: 5px 14px;
  border-radius: 20px;
  width: fit-content;
}

.level-excellent {
  background: #e8f5e9;
  color: #4caf50;
}

.level-good {
  background: #fff3e0;
  color: #ff9800;
}

.level-fair {
  background: #fff8e1;
  color: #ffc107;
}

.level-poor {
  background: #ffebee;
  color: #f44336;
}

.hero-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ai-conclusion {
  padding: 16px;
  background: #fafafa;
  border-radius: 12px;
  border-left: 4px solid #ff8c42;
}

.conclusion-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #ff8c42;
  font-weight: 500;
  margin-bottom: 8px;
}

.conclusion-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #2f2f2f;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  padding: 4px 12px;
  border-radius: 20px;
  background: #f0f9eb;
  color: #67c23a;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #67c23a;
}

/* ============================================
   页面级返回入口
   ============================================ */
.page-back {
  margin-bottom: 16px;
}

.back-btn {
  color: #909399;
  font-size: 13px;
  padding: 6px 0;
}

.back-btn:hover {
  color: #ff8c42;
}

/* ============================================
   Hero 报告总览区
   ============================================ */
.hero-section {
  background: #fff;
  border-radius: 20px;
  padding: 28px 32px;
  margin-bottom: 20px;
  border: 1px solid rgba(243, 216, 199, 0.5);
  box-shadow: 0 4px 20px rgba(255, 140, 66, 0.06);
}

.hero-main {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.hero-left {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 160px;
}

.job-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.job-label {
  font-size: 12px;
  color: #999;
}

.job-name {
  font-size: 15px;
  font-weight: 600;
  color: #2f2f2f;
}

.score-display {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.score-number {
  font-size: 56px;
  font-weight: 700;
  color: #ff8c42;
  line-height: 1;
}

.score-number.muted {
  color: #c0c4cc;
}

.score-unit {
  font-size: 18px;
  color: #ff8c42;
  font-weight: 500;
}

.level-badge {
  font-size: 13px;
  font-weight: 600;
  padding: 5px 14px;
  border-radius: 20px;
  width: fit-content;
}

.level-excellent {
  background: #e8f5e9;
  color: #4caf50;
}

.level-good {
  background: #fff3e0;
  color: #ff9800;
}

.level-fair {
  background: #fff8e1;
  color: #ffc107;
}

.level-poor {
  background: #ffebee;
  color: #f44336;
}

.hero-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ai-conclusion {
  padding: 16px;
  background: #fafafa;
  border-radius: 12px;
  border-left: 4px solid #ff8c42;
}

.conclusion-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #ff8c42;
  font-weight: 500;
  margin-bottom: 8px;
}

.conclusion-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #2f2f2f;
}

.hero-meta {
  display: flex;
  align-items: center;
  gap: 0;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(243, 216, 199, 0.3);
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 20px;
}

.meta-item:first-child {
  padding-left: 0;
}

.meta-label {
  font-size: 12px;
  color: #999;
}

.meta-value {
  font-size: 14px;
  font-weight: 600;
  color: #2f2f2f;
}

.meta-value.status-ended {
  color: #67c23a;
}

.meta-divider {
  width: 1px;
  height: 32px;
  background: rgba(243, 216, 199, 0.5);
}

/* ============================================
   KPI 指标区
   ============================================ */
.kpi-section {
  margin-bottom: 20px;
}

.kpi-grid {
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
  gap: 10px;
  border: 1px solid rgba(243, 216, 199, 0.5);
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.04);
}

.kpi-label {
  font-size: 13px;
  color: #666;
  text-align: center;
}

.kpi-number {
  font-size: 28px;
  font-weight: 700;
  color: #2f2f2f;
  line-height: 1;
}

.kpi-ring {
  display: flex;
  align-items: center;
  justify-content: center;
}

.kpi-ring svg {
  display: block;
}

.kpi-simple .kpi-value {
  color: #2f2f2f;
  line-height: 1;
}

.kpi-label {
  font-size: 13px;
  color: #666;
}

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

.score-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 16px;
}

.score-card {
  background: #fafafa;
  border-radius: 12px;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  border: 1px solid rgba(243, 216, 199, 0.3);
}

.score-card .title {
  font-size: 13px;
  color: #666;
  font-weight: 500;
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
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #fff8f3 0%, #fff 100%);
  border-bottom: 1px solid rgba(243, 216, 199, 0.3);
}

.section-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.section-icon.strength {
  background: #f0f9eb;
  color: #67c23a;
}

.section-icon.dimension {
  background: #ecf5ff;
  color: #409eff;
}

.section-icon.suggestion {
  background: #fff3e0;
  color: #f56c6c;
}

.section-icon.improvement {
  background: #fef0f0;
  color: #f56c6c;
}

.section-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #2f2f2f;
}

.section-body {
  padding: 20px;
}

/* ============================================
   优势亮点
   ============================================ */
.strength-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.strength-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  background: #f8faf7;
  border-radius: 10px;
  border: 1px solid #e8f5e9;
}

.strength-check {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #67c23a;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.strength-text {
  font-size: 13px;
  line-height: 1.6;
  color: #2f2f2f;
}

/* ============================================
   各维度评分
   ============================================ */
.dimensions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dimension-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dimension-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dimension-name {
  font-size: 13px;
  color: #666;
}

.dimension-value {
  font-size: 14px;
  font-weight: 600;
}

.dimension-bar {
  height: 8px;
  background: #f3d8c7;
  border-radius: 4px;
  overflow: hidden;
}

.dimension-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
}

/* ============================================
   改进建议（行动清单）
   ============================================ */
.action-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-item {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.action-index {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #fff3e0;
  color: #ff8c42;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.action-content {
  flex: 1;
  padding: 12px 14px;
  background: #fffaf7;
  border-radius: 10px;
  border: 1px solid rgba(243, 216, 199, 0.4);
}

.action-text {
  font-size: 13px;
  line-height: 1.6;
  color: #2f2f2f;
}

/* ============================================
   待提升方向
   ============================================ */
.improvement-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.improvement-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  background: #fef9f9;
  border-radius: 10px;
  border: 1px solid #fde2e2;
}

.improvement-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f56c6c;
  margin-top: 6px;
  flex-shrink: 0;
}

.improvement-text {
  font-size: 13px;
  line-height: 1.6;
  color: #2f2f2f;
  word-break: break-all;
  white-space: normal;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.section-icon.tag-icon {
  background: #fff3e0;
  color: #e6a23c;
}

.section-icon.weakness {
  background: #fef0f0;
  color: #f56c6c;
}

.section-icon.redflag {
  background: #fef0f0;
  color: #f56c6c;
}

.section-icon.critical {
  background: #fef0f0;
  color: #f56c6c;
}

.section-icon.missing {
  background: #fff8e1;
  color: #e6a23c;
}

.section-icon.rejection {
  background: #ffebee;
  color: #f44336;
}

.section-icon.question {
  background: #ecf5ff;
  color: #409eff;
}

.weakness-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.weakness-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  background: #fef9f9;
  border-radius: 10px;
  border: 1px solid #fde2e2;
}

.weakness-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f56c6c;
  margin-top: 6px;
  flex-shrink: 0;
}

.weakness-text {
  font-size: 13px;
  line-height: 1.6;
  color: #2f2f2f;
  word-break: break-all;
  white-space: normal;
}

.suggestion-group {
  margin-bottom: 16px;
}

.suggestion-group:last-child {
  margin-bottom: 0;
}

.suggestion-sub-header {
  font-size: 12px;
  font-weight: 600;
  color: #f56c6c;
  margin-bottom: 10px;
  padding-left: 4px;
}

.dimension-comments {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dimension-comment {
  padding: 12px 14px;
  background: #f8faf7;
  border-radius: 8px;
}

.comment-title {
  font-size: 12px;
  color: #67c23a;
  font-weight: 600;
  margin-bottom: 4px;
}

.comment-text {
  font-size: 13px;
  line-height: 1.6;
  color: #2f2f2f;
  word-break: break-all;
  white-space: normal;
}

.question-item {
  background: #fafafa;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  border: 1px solid rgba(243, 216, 199, 0.3);
}

.question-header {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.question-index {
  font-size: 12px;
  font-weight: 600;
  color: #ff8c42;
  flex-shrink: 0;
}

.question-text {
  font-size: 14px;
  font-weight: 600;
  color: #2f2f2f;
  word-break: break-all;
  white-space: normal;
}

.question-answer {
  font-size: 13px;
  line-height: 1.7;
  color: #666;
  padding: 10px 12px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
  word-break: break-all;
  white-space: normal;
}

.question-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.question-score {
  display: flex;
  align-items: center;
  gap: 8px;
}

.qs-label {
  font-size: 12px;
  color: #666;
}

.question-comment {
  margin-top: 10px;
  padding: 10px 12px;
  background: #fffaf7;
  border-radius: 8px;
  border-left: 3px solid #ff8c42;
  font-size: 13px;
  line-height: 1.6;
  color: #2f2f2f;
  word-break: break-all;
  white-space: normal;
}

/* ============================================
   无报告
   ============================================ */
.report-empty-card {
  background: #fff;
  border-radius: 16px;
  padding: 48px;
  text-align: center;
  margin-bottom: 20px;
  border: 1px solid rgba(243, 216, 199, 0.5);
}

.report-empty-card .empty-title {
  font-size: 16px;
  font-weight: 500;
  color: #606266;
  margin: 12px 0 6px;
}

.report-empty-card .empty-desc {
  font-size: 13px;
  color: #909399;
  margin: 0;
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
}

.action-btn {
  border-radius: 24px;
  padding: 10px 24px;
  font-size: 14px;
}

.action-btn.secondary {
  background: #fff;
  border-color: #f3d8c7;
  color: #666;
}

.action-btn.secondary:hover {
  border-color: #ff8c42;
  color: #ff8c42;
}

.action-btn.primary {
  background: linear-gradient(135deg, #ff8c42 0%, #ff7a30 100%);
  border: none;
  color: #fff;
  box-shadow: 0 4px 16px rgba(255, 140, 66, 0.3);
}

.action-btn.primary:hover {
  opacity: 0.9;
}

.section-card,
.error-card,
.hero-section,
.section-header,
.section-body,
.kpi-card,
.kpi-grid,
.report-empty-card {
  box-sizing: border-box;
  min-width: 0;
  overflow: hidden;
}

.value {
  word-break: break-all;
  white-space: normal;
  line-height: 1.6;
}

/* ============================================
   响应式
   ============================================ */
@media (max-width: 1023px) {
  .hero-main {
    flex-direction: column;
    gap: 20px;
  }

  .hero-left {
    flex-direction: row;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;
  }

  .score-display {
    order: 1;
  }

  .level-badge {
    order: 2;
  }

  .job-info {
    width: 100%;
    order: 0;
  }

  .kpi-grid {
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  }
}

@media (max-width: 768px) {
  .interview-report-view {
    padding: 16px;
  }

  .hero-section {
    padding: 20px;
  }

  .score-number {
    font-size: 44px;
  }

  .kpi-grid {
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
    gap: 12px;
  }

  .kpi-card {
    padding: 16px;
  }

  .kpi-number {
    font-size: 24px;
  }

  .kpi-ring svg {
    width: 52px;
    height: 52px;
  }

  .ring-wrapper svg {
    width: 60px;
    height: 60px;
  }

  .section-header {
    padding: 14px 16px;
  }

  .section-body {
    padding: 16px;
  }

  .strength-grid {
    grid-template-columns: 1fr;
  }

  .action-group {
    flex-direction: column;
    align-items: stretch;
  }

  .action-btn {
    width: 100%;
    min-height: 42px;
  }
}
</style>
