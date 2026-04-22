<template>
  <div class="interview-entry-view">
    <!-- 页面标题区 -->
    <div class="page-header">
      <h1 class="page-title">模拟面试</h1>
      <p class="page-desc">配置面试参数，开始一场真实的模拟面试</p>
    </div>

    <!-- 准备就绪提示条 -->
    <div class="ready-bar">
      <div class="ready-icon">
        <el-icon :size="20"><CircleCheckFilled /></el-icon>
      </div>
      <span class="ready-text">准备就绪，随时可以开始面试</span>
    </div>

    <!-- 配置选项 -->
    <div class="config-section">
      <div class="config-card">
        <!-- 面试岗位下拉选择 -->
        <div class="config-item">
          <div class="config-label">面试岗位</div>
          <div class="config-control">
            <el-select
              v-model="selectedJob"
              placeholder="请选择面试岗位"
              size="large"
              class="job-select"
              :disabled="creating"
              popper-class="job-select-popper"
            >
              <template #empty>
                <div class="empty-options">暂无岗位数据</div>
              </template>
              <el-option
                v-for="job in jobOptions"
                :key="job.value"
                :label="job.label"
                :value="job.value"
              >
                <div class="job-option-content">
                  <span class="job-name">{{ job.label }}</span>
                  <span class="job-tag" :class="'tag-' + job.tagType">{{
                    job.tag
                  }}</span>
                </div>
              </el-option>
            </el-select>
          </div>
        </div>

        <!-- 难度级别选择 - pill 风格按钮组 -->
        <div class="config-item">
          <div class="config-label">难度级别</div>
          <div class="config-control pill-control">
            <div
              v-for="level in difficultyOptions"
              :key="level.value"
              class="pill-button"
              :class="{ active: selectedDifficulty === level.value }"
              @click="selectedDifficulty = level.value"
            >
              <span class="pill-label">{{ level.label }}</span>
              <span class="pill-hint">{{ level.hint }}</span>
            </div>
          </div>
        </div>

        <!-- 面试模式选择 - pill 风格按钮组 -->
        <div class="config-item">
          <div class="config-label">面试模式</div>
          <div class="config-control pill-control">
            <div
              v-for="mode in modeOptions"
              :key="mode.value"
              class="pill-button"
              :class="{ active: selectedMode === mode.value }"
              @click="selectedMode = mode.value"
            >
              <span class="pill-label">{{ mode.label }}</span>
              <span class="pill-hint">{{ mode.hint }}</span>
            </div>
          </div>
        </div>

        <!-- 开始面试按钮 -->
        <div class="start-section">
          <el-button
            type="primary"
            size="large"
            :disabled="!selectedJob || creating"
            :loading="creating"
            @click="handleStart"
          >
            {{ creating ? "创建面试中..." : "开始面试" }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 面试说明 -->
    <div class="info-section">
      <div class="info-card">
        <h3 class="info-title">面试说明</h3>
        <div class="info-list">
          <div class="info-item">
            <div class="info-number">1</div>
            <div class="info-text">AI 面试官会通过文字与您交流</div>
          </div>
          <div class="info-item">
            <div class="info-number">2</div>
            <div class="info-text">面试结束后会自动生成评估报告</div>
          </div>
          <div class="info-item">
            <div class="info-number">3</div>
            <div class="info-text">报告包含表现评分和改进建议</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { CircleCheckFilled } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import { createInterviewSession, getInterviewJobRoles } from "@/api/interview";

const router = useRouter();
const userStore = useUserStore();

// 岗位选项
// 作用：岗位选项必须统一从后台配置读取，不能继续在前端写死。
// 后端返回的是管理员维护的启用岗位列表，前端只负责渲染。
const jobOptions = ref([]);

// 难度级别选项 - 带辅助说明
const difficultyOptions = [
  { label: "初级", value: "primary", hint: "入门基础" },
  { label: "中级", value: "intermediate", hint: "项目实践" },
  { label: "高级", value: "advanced", hint: "深度能力" },
];

// 面试模式选项 - 带辅助说明
const modeOptions = [
  { label: "普通面试", value: "normal", hint: "标准流程" },
  { label: "压力面试", value: "stress", hint: "高压情境" },
];

// 难度级别映射：前端值 -> 后端期望值
const difficultyMap = {
  primary: 1,
  intermediate: 2,
  advanced: 3,
};

// 选中状态
const selectedJob = ref("");
const selectedDifficulty = ref("primary");
const selectedMode = ref("normal");
const creating = ref(false);

/**
 * 获取岗位选项
 *
 * 作用：
 * 让模拟面试入口页直接消费后台岗位配置，满足“岗位由管理员配置”的业务要求。
 * 返回的 tag / tagType 也来自后端，避免前端继续维护一套平行的静态配置。
 */
const fetchJobOptions = async () => {
  try {
    const res = await getInterviewJobRoles();
    const rawList = Array.isArray(res.data) ? res.data : [];
    jobOptions.value = rawList.map((item) => ({
      label: item.roleName,
      value: item.roleName,
      tag: item.interviewTag || "常规",
      tagType: item.tagType || "normal",
    }));
  } catch (err) {
    jobOptions.value = [];
    ElMessage.error(err.message || "获取岗位选项失败");
  }
};

// 开始面试 - 先创建会话，再跳转
const handleStart = async () => {
  if (!userStore.isLoggedIn()) {
    ElMessage.warning("请先登录");
    router.push("/login");
    return;
  }

  if (!selectedJob.value) {
    ElMessage.warning("请选择面试岗位");
    return;
  }

  creating.value = true;

  try {
    // 调用创建会话接口
    const res = await createInterviewSession({
      jobRole: selectedJob.value,
      difficulty: difficultyMap[selectedDifficulty.value],
      interviewMode: selectedMode.value,
    });

    // 从响应中提取 sessionId
    let sessionId = null;
    if (res.data) {
      sessionId = res.data.sessionId || res.data.id || res.data;
    } else if (res.sessionId) {
      sessionId = res.sessionId;
    } else if (typeof res === "string") {
      sessionId = res;
    }

    if (!sessionId) {
      throw new Error("创建会话失败，未获取到会话ID");
    }

    // 跳转到真实会话页
    router.push(`/interview/session/${sessionId}`);
  } catch (err) {
    console.error("创建面试会话失败:", err);
    ElMessage.error(err.message || "创建面试会话失败，请稍后重试");
  } finally {
    creating.value = false;
  }
};

onMounted(() => {
  fetchJobOptions();
});
</script>

<style scoped>
.interview-entry-view {
  min-height: 100%;
}

/* 页面标题区 */
.page-header {
  margin-bottom: 24px;
}

.page-title {
  margin: 0 0 6px 0;
  font-size: 24px;
  font-weight: 600;
  color: #2f2f2f;
}

.page-desc {
  margin: 0;
  font-size: 14px;
  color: #888888;
}

/* 准备就绪提示条 */
.ready-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #fff3e8 0%, #fff8f3 100%);
  border: 1px solid #ffd7bf;
  border-radius: 10px;
  margin-bottom: 24px;
}

.ready-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #ff8c42;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
}

.ready-text {
  font-size: 14px;
  color: #555555;
}

/* 配置选项区 */
.config-section {
  margin-bottom: 24px;
}

.config-card {
  background-color: #ffffff;
  border: 1px solid #f3d8c7;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.06);
}

.config-item {
  margin-bottom: 32px;
}

.config-item:last-of-type {
  margin-bottom: 36px;
}

.config-label {
  font-size: 15px;
  font-weight: 600;
  color: #2f2f2f;
  margin-bottom: 14px;
  letter-spacing: 0.5px;
}

.config-control {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

/* 岗位下拉选择器样式 */
.job-select {
  width: 360px;
}

.job-select :deep(.el-input__wrapper) {
  border-radius: 10px;
  border: 1px solid #f3d8c7;
  box-shadow: none;
  transition: all 0.2s;
  padding: 0 16px;
}

.job-select :deep(.el-input__wrapper:hover) {
  border-color: #ff8c42;
}

.job-select :deep(.el-input__wrapper.is-focus) {
  border-color: #ff8c42;
  box-shadow: 0 0 0 3px rgba(255, 140, 66, 0.1);
}

.job-select :deep(.el-input__inner) {
  color: #2f2f2f;
  font-size: 15px;
}

.job-select :deep(.el-input__inner::placeholder) {
  color: #888888;
}

/* 下拉面板样式 - 使选项更舒展 */
.job-select-popper {
  width: 380px !important;
  padding: 8px 0;
}

.job-select-popper .el-select-dropdown__item {
  height: 52px;
  padding: 0 20px;
  display: flex;
  align-items: center;
}

.job-select-popper .el-select-dropdown__item.hover,
.job-select-popper .el-select-dropdown__item:hover {
  background-color: #fff8f3;
}

.job-select-popper .el-select-dropdown__item.selected {
  color: #ff8c42;
  font-weight: 600;
  background-color: #fff8f3;
}

/* 下拉选项样式 */
.job-option-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 8px 0;
}

.job-name {
  font-size: 15px;
  color: #2f2f2f;
  font-weight: 500;
}

.job-tag {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  font-weight: 500;
  flex-shrink: 0;
  margin-left: 16px;
}

/* 岗位标签颜色 */
.tag-hot {
  background-color: #fff3e8;
  color: #ff8c42;
}

.tag-common {
  background-color: #f0f9eb;
  color: #67c23a;
}

.tag-competitive {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.tag-normal {
  background-color: #f5f7fa;
  color: #909399;
}

.tag-orange-highlight {
  background-color: rgba(255, 140, 66, 0.15);
  color: #e67a35;
}

.tag-blue-info {
  background-color: rgba(47, 125, 225, 0.12);
  color: #2f7de1;
}

.tag-green-success {
  background-color: rgba(48, 176, 111, 0.12);
  color: #2a9658;
}

.tag-red-alert {
  background-color: rgba(224, 84, 84, 0.12);
  color: #d64545;
}

.tag-purple-feature {
  background-color: rgba(123, 90, 217, 0.12);
  color: #6b4dc9;
}

.tag-gray-muted {
  background-color: rgba(143, 153, 167, 0.15);
  color: #6b7280;
}

.tag-outline {
  background-color: #ffffff;
  border: 1px solid #d9b49a;
  color: #9a5c33;
}

.tag-pill {
  background-color: rgba(255, 140, 66, 0.12);
  color: #b35f2b;
  border-radius: 999px;
}

.tag-default {
  background-color: #fdf1e6;
  color: #a05a2c;
}

.empty-options {
  padding: 20px;
  text-align: center;
  color: #888888;
  font-size: 14px;
}

/* Pill 风格按钮组 */
.pill-control {
  gap: 16px;
}

.pill-button {
  min-width: 96px;
  height: 44px;
  padding: 0 20px;
  border: 1px solid #f3d8c7;
  border-radius: 22px;
  background-color: #ffffff;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  transition: all 0.25s ease;
}

.pill-button:hover {
  border-color: #ff8c42;
  background-color: #fff8f3;
}

.pill-button.active {
  border-color: #ff8c42;
  background-color: #ff8c42;
  box-shadow: 0 4px 12px rgba(255, 140, 66, 0.3);
}

.pill-label {
  font-size: 14px;
  font-weight: 600;
  color: #555555;
  transition: color 0.25s ease;
}

.pill-hint {
  font-size: 11px;
  color: #888888;
  transition: color 0.25s ease;
}

.pill-button:hover .pill-label {
  color: #ff8c42;
}

.pill-button.active .pill-label {
  color: #ffffff;
}

.pill-button.active .pill-hint {
  color: rgba(255, 255, 255, 0.85);
}

/* 开始面试按钮 */
.start-section {
  display: flex;
  justify-content: center;
  padding-top: 8px;
}

.start-section .el-button {
  padding: 14px 56px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff8c42 0%, #e67a35 100%);
  border: none;
  box-shadow: 0 4px 14px rgba(255, 140, 66, 0.35);
  transition: all 0.25s ease;
}

.start-section .el-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 140, 66, 0.45);
}

.start-section .el-button:disabled {
  background: #f3d8c7;
  border-color: #f3d8c7;
  box-shadow: none;
  cursor: not-allowed;
}

/* 面试说明区 */
.info-section {
  margin-bottom: 24px;
}

.info-card {
  background-color: #ffffff;
  border: 1px solid #f3d8c7;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.06);
}

.info-title {
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 600;
  color: #2f2f2f;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.info-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff8c42 0%, #ffb07a 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  flex-shrink: 0;
}

.info-text {
  font-size: 14px;
  color: #555555;
}
</style>
