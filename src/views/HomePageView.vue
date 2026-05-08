<template>
  <div class="landing-page">
    <!-- Section 1: Hero 区域 -->
    <section class="hero-section">
      <!-- 背景装饰元素 -->
      <div class="hero-bg">
        <div class="bg-gradient"></div>
        <div class="bg-dots"></div>
      </div>

      <div class="hero-content">
        <div class="hero-badge">
          <svg
            class="badge-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path
              d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
            />
          </svg>
          <span>AI 驱动的职场智能平台</span>
        </div>

        <h1 class="hero-title">智能模拟面试<br />与简历诊断系统</h1>

        <p class="hero-subtitle">提升你的求职竞争力，让每一次面试都有准备</p>

        <div class="hero-actions">
          <el-button
            type="primary"
            size="large"
            class="cta-btn primary-btn"
            @click="handleResume"
          >
            <svg
              class="btn-icon"
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
            开始简历诊断
          </el-button>
          <el-button
            size="large"
            class="cta-btn secondary-btn"
            @click="handleInterview"
          >
            <svg
              class="btn-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
              <path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
            开始模拟面试
          </el-button>
        </div>

        <div class="hero-stats">
          <div class="stat-item">
            <span v-if="statsLoading" class="stat-skeleton"><el-skeleton-item variant="h1" style="width: 60px; height: 32px;" /></span>
            <span v-else class="stat-number">{{ formatCount(stats.userCount) }}</span>
            <span class="stat-text">用户使用</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span v-if="statsLoading" class="stat-skeleton"><el-skeleton-item variant="h1" style="width: 60px; height: 32px;" /></span>
            <span v-else class="stat-number">{{ formatCount(stats.diagnosisCount) }}</span>
            <span class="stat-text">简历诊断</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span v-if="statsLoading" class="stat-skeleton"><el-skeleton-item variant="h1" style="width: 60px; height: 32px;" /></span>
            <span v-else class="stat-number">{{ formatCount(stats.interviewCount) }}</span>
            <span class="stat-text">模拟面试</span>
          </div>
        </div>
      </div>

      <!-- Hero 右侧装饰 -->
      <div class="hero-visual">
        <div class="visual-card resume-visual">
          <div class="visual-icon">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"
              />
              <polyline points="14 2 14 8 20 8" />
            </svg>
          </div>
          <span>简历诊断</span>
        </div>
        <div class="visual-card interview-visual">
          <div class="visual-icon">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
            </svg>
          </div>
          <span>模拟面试</span>
        </div>
      </div>
    </section>

    <!-- Section 2: 功能介绍区 -->
    <section class="features-section">
      <div class="section-header">
        <span class="section-tag">核心功能</span>
        <h2 class="section-title">专业 AI 服务</h2>
        <p class="section-desc">助力求职之路，让每一次面试都有准备</p>
      </div>

      <div class="features-grid">
        <!-- 简历诊断卡片 -->
        <div class="feature-card" @click="handleResume">
          <div class="card-glow"></div>
          <div class="feature-icon-wrap">
            <div class="feature-icon">
              <svg
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
                <line x1="10" y1="9" x2="8" y2="9" />
              </svg>
            </div>
          </div>
          <h3 class="feature-title">简历诊断</h3>
          <p class="feature-desc">
            智能分析简历内容，精准定位问题，提供专业优化建议
          </p>
          <div class="feature-footer">
            <span class="feature-link">
              立即体验
              <svg
                class="arrow-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M5 12h14M12 5l7 7-7 7" />
              </svg>
            </span>
          </div>
        </div>

        <!-- 模拟面试卡片 -->
        <div class="feature-card" @click="handleInterview">
          <div class="card-glow"></div>
          <div class="feature-icon-wrap">
            <div class="feature-icon">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                <path d="M16 3.13a4 4 0 0 1 0 7.75" />
              </svg>
            </div>
          </div>
          <h3 class="feature-title">模拟面试</h3>
          <p class="feature-desc">
            AI 模拟真实面试场景，实时评估表现，助力面试技巧提升
          </p>
          <div class="feature-footer">
            <span class="feature-link">
              立即体验
              <svg
                class="arrow-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M5 12h14M12 5l7 7-7 7" />
              </svg>
            </span>
          </div>
        </div>
      </div>
    </section>

    <!-- Section 3: 特性说明区 -->
    <section class="highlights-section">
      <div class="highlights-grid">
        <div class="highlight-item">
          <div class="highlight-icon">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
              />
            </svg>
          </div>
          <div class="highlight-content">
            <h4 class="highlight-title">AI 智能分析</h4>
            <p class="highlight-desc">深度学习算法，精准识别简历与面试问题</p>
          </div>
        </div>

        <div class="highlight-item">
          <div class="highlight-icon">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
            </svg>
          </div>
          <div class="highlight-content">
            <h4 class="highlight-title">实时流式对话</h4>
            <p class="highlight-desc">
              流畅的 AI 对话体验，如同与真人面试官交流
            </p>
          </div>
        </div>

        <div class="highlight-item">
          <div class="highlight-icon">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
              <line x1="3" y1="9" x2="21" y2="9" />
              <line x1="9" y1="21" x2="9" y2="9" />
            </svg>
          </div>
          <div class="highlight-content">
            <h4 class="highlight-title">多岗位覆盖</h4>
            <p class="highlight-desc">支持技术、产品、运营等多种岗位模拟</p>
          </div>
        </div>

        <div class="highlight-item">
          <div class="highlight-icon">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
          </div>
          <div class="highlight-content">
            <h4 class="highlight-title">随时随地</h4>
            <p class="highlight-desc">24 小时在线服务，碎片时间高效利用</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { isLoggedIn } from "@/utils/auth";
import { getPublicStats } from "@/api/stats";

const router = useRouter();

// 平台统计数据
const statsLoading = ref(true);
const stats = ref({
  userCount: 0,
  diagnosisCount: 0,
  interviewCount: 0,
});

// 格式化数字：超过 1000 显示为 x,xxx+ 格式
const formatCount = (num) => {
  if (!num || num <= 0) return "0";
  if (num >= 10000) return `${(num / 10000).toFixed(1).replace(/\.0$/, "")}万+`;
  if (num >= 1000) return `${Math.floor(num / 1000)},${String(num % 1000).padStart(3, "0")}+`;
  return `${num}+`;
};

onMounted(async () => {
  try {
    const res = await getPublicStats();
    if (res.data) {
      stats.value = res.data;
    }
  } catch {
    // 获取失败时保持默认值 0，不影响页面展示
  } finally {
    statsLoading.value = false;
  }
});

// 跳转简历诊断
const handleResume = () => {
  if (isLoggedIn()) {
    router.push("/resume/upload");
  } else {
    router.push("/login");
  }
};

// 跳转模拟面试
const handleInterview = () => {
  if (isLoggedIn()) {
    router.push("/interview/entry");
  } else {
    router.push("/login");
  }
};
</script>

<style scoped>
/* 页面基础样式 */
.landing-page {
  min-height: calc(100vh - 60px);
  background-color: var(--bg-page);
  overflow-x: hidden;
}

/* ============================================
   Section 1: Hero 区域 - 增强设计感
   ============================================ */
.hero-section {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1200px;
  margin: 0 auto;
  padding: 80px 48px 100px;
  gap: 48px;
}

/* 背景装饰 */
.hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  overflow: hidden;
}

.bg-gradient {
  position: absolute;
  width: 600px;
  height: 600px;
  background: radial-gradient(
    circle,
    rgba(255, 140, 66, 0.15) 0%,
    transparent 70%
  );
  top: -200px;
  right: -100px;
}

.bg-dots {
  position: absolute;
  width: 100%;
  height: 100%;
  background-image: radial-gradient(
    circle,
    rgba(255, 140, 66, 0.1) 1px,
    transparent 1px
  );
  background-size: 24px 24px;
}

/* Hero 内容区 */
.hero-content {
  flex: 1;
  max-width: 560px;
  position: relative;
  z-index: 1;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: linear-gradient(
    135deg,
    rgba(255, 140, 66, 0.1) 0%,
    rgba(255, 140, 66, 0.05) 100%
  );
  border: 1px solid rgba(255, 140, 66, 0.2);
  border-radius: 24px;
  margin-bottom: 24px;
}

.badge-icon {
  width: 16px;
  height: 16px;
  color: var(--orange-main);
}

.hero-badge span {
  font-size: 13px;
  color: var(--orange-main);
  font-weight: 500;
}

.hero-title {
  font-size: 52px;
  font-weight: 700;
  color: var(--text-title);
  line-height: 1.15;
  margin: 0 0 20px 0;
  letter-spacing: -0.02em;
}

.hero-subtitle {
  font-size: 18px;
  color: var(--text-body);
  margin: 0 0 36px 0;
  line-height: 1.6;
}

/* CTA 按钮组 */
.hero-actions {
  display: flex;
  gap: 16px;
  margin-bottom: 48px;
}

.cta-btn {
  padding: 14px 28px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  transition: all 0.25s ease;
}

.btn-icon {
  width: 18px;
  height: 18px;
}

.primary-btn {
  background: linear-gradient(135deg, #ff8c42 0%, #e67a35 100%);
  border: none;
  color: var(--bg-card);
  box-shadow: 0 4px 16px rgba(255, 140, 66, 0.35);
}

.primary-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(255, 140, 66, 0.45);
}

.secondary-btn {
  background: var(--bg-card);
  border: 2px solid var(--orange-main);
  color: var(--orange-main);
}

.secondary-btn:hover {
  background: rgba(255, 140, 66, 0.05);
  transform: translateY(-2px);
}

/* 数据统计 */
.hero-stats {
  display: flex;
  align-items: center;
  gap: 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-number {
  font-size: 24px;
  font-weight: 700;
  color: var(--orange-main);
}

.stat-text {
  font-size: 13px;
  color: var(--text-muted);
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: var(--border-divider);
}

/* Hero 右侧装饰卡片 */
.hero-visual {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 280px;
}

.visual-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  transition: all 0.25s ease;
}

.visual-card:hover {
  transform: translateX(-8px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
}

.resume-visual {
  margin-left: 20px;
}

.interview-visual {
  margin-left: 40px;
}

.visual-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff8c42 0%, #ffb07a 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.visual-icon svg {
  width: 24px;
  height: 24px;
  color: var(--bg-card);
}

.visual-card span {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-title);
}

/* ============================================
   Section 2: 功能介绍区
   ============================================ */
.features-section {
  max-width: 1100px;
  margin: 0 auto;
  padding: 60px 24px 80px;
}

.section-header {
  text-align: center;
  margin-bottom: 48px;
}

.section-tag {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  color: var(--orange-main);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 12px;
}

.section-title {
  font-size: 36px;
  font-weight: 700;
  color: var(--text-title);
  margin: 0 0 12px 0;
}

.section-desc {
  font-size: 16px;
  color: var(--text-body);
  margin: 0;
}

/* 功能卡片网格 */
.features-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

/* 功能卡片样式 */
.feature-card {
  position: relative;
  background: var(--bg-card);
  border-radius: 20px;
  padding: 40px 36px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid var(--border-divider);
  overflow: hidden;
}

/* 卡片顶部渐变装饰条 */
.feature-card::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #ff8c42, #ffb07a);
  opacity: 0;
  transition: opacity 0.3s ease;
}

/* 卡片悬停光效 */
.card-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(
    circle,
    rgba(255, 140, 66, 0.08) 0%,
    transparent 50%
  );
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.feature-card:hover {
  border-color: rgba(255, 140, 66, 0.3);
  transform: translateY(-6px);
  box-shadow: 0 16px 48px rgba(255, 140, 66, 0.12);
}

.feature-card:hover::before {
  opacity: 1;
}

.feature-card:hover .card-glow {
  opacity: 1;
}

.feature-icon-wrap {
  margin-bottom: 28px;
}

.feature-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(135deg, #ff8c42 0%, #ffb07a 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(255, 140, 66, 0.3);
}

.feature-icon svg {
  width: 30px;
  height: 30px;
  color: var(--bg-card);
}

.feature-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-title);
  margin: 0 0 14px 0;
}

.feature-desc {
  font-size: 15px;
  color: var(--text-body);
  line-height: 1.7;
  margin: 0 0 28px 0;
}

.feature-footer {
  display: flex;
  align-items: center;
}

.feature-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--orange-main);
  transition: gap 0.2s ease;
}

.feature-card:hover .feature-link {
  gap: 12px;
}

.arrow-icon {
  width: 16px;
  height: 16px;
  transition: transform 0.2s ease;
}

.feature-card:hover .arrow-icon {
  transform: translateX(4px);
}

/* ============================================
   Section 3: 特性说明区
   ============================================ */
.highlights-section {
  max-width: 1100px;
  margin: 0 auto;
  padding: 40px 24px 100px;
}

.highlights-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.highlight-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 24px;
  background: var(--bg-card);
  border-radius: 16px;
  border: 1px solid var(--border-divider);
  transition: all 0.25s ease;
}

.highlight-item:hover {
  border-color: rgba(255, 140, 66, 0.3);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.highlight-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(
    135deg,
    rgba(255, 140, 66, 0.1) 0%,
    rgba(255, 140, 66, 0.05) 100%
  );
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.highlight-icon svg {
  width: 20px;
  height: 20px;
  color: var(--orange-main);
}

.highlight-content {
  flex: 1;
  min-width: 0;
}

.highlight-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-title);
  margin: 0 0 6px 0;
}

.highlight-desc {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0;
  line-height: 1.5;
}

/* ============================================
   响应式设计
   ============================================ */
@media (max-width: 1024px) {
  .hero-section {
    flex-direction: column;
    text-align: center;
    padding: 60px 24px 80px;
  }

  .hero-content {
    max-width: 100%;
  }

  .hero-badge {
    margin: 0 auto 24px;
  }

  .hero-actions {
    justify-content: center;
  }

  .hero-stats {
    justify-content: center;
  }

  .hero-visual {
    flex-direction: row;
    max-width: 100%;
    justify-content: center;
  }

  .resume-visual,
  .interview-visual {
    margin-left: 0;
  }

  .highlights-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 36px;
  }

  .hero-subtitle {
    font-size: 16px;
  }

  .hero-actions {
    flex-direction: column;
    align-items: center;
  }

  .cta-btn {
    width: 100%;
    max-width: 280px;
    justify-content: center;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .highlights-grid {
    grid-template-columns: 1fr;
  }

  .highlight-item {
    padding: 20px;
  }

  .hero-visual {
    display: none;
  }
}
</style>
