<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">AI 引擎配置</h2>
        <p class="page-subtitle">维护 interview/resume 业务模型配置，支持启用切换和密钥脱敏展示</p>
        <p class="page-subtitle subtitle-tip">说明：首页「Offer 辅助」（薪资谈判模拟、谈薪话术）复用 interview 业务类型的激活配置，调整 interview 配置会同时影响模拟面试与 Offer 辅助。</p>
      </div>
      <div class="header-actions">
        <el-button :loading="tableLoading" class="refresh-btn" @click="fetchEngineList">刷新列表</el-button>
        <el-button
          v-if="selectedEngines.length > 0"
          type="danger"
          :loading="batchDeleteLoading"
          @click="handleBatchDelete"
        >
          批量删除 ({{ selectedEngines.length }})
        </el-button>
        <el-button
          v-if="selectedEngines.length > 0"
          type="warning"
          @click="handleBatchDisable"
        >
          批量禁用
        </el-button>
        <el-button
          v-if="selectedEngines.length > 0"
          type="success"
          @click="handleBatchEnable"
        >
          批量启用
        </el-button>
        <el-button @click="handleSelectAll">
          全部勾选
        </el-button>
        <el-button type="primary" @click="openCreateDialog" class="btn-primary">
          <el-icon><Edit /></el-icon>
          新增引擎配置
        </el-button>
      </div>
    </div>

    <div class="admin-section-switch" role="tablist" aria-label="AI 引擎配置页面分区">
      <button
        type="button"
        class="admin-section-tab"
        :class="{ active: activeAdminSection === 'engine-config' }"
        role="tab"
        data-admin-section="engine-config"
        :aria-selected="activeAdminSection === 'engine-config' ? 'true' : 'false'"
        @click="activeAdminSection = 'engine-config'"
      >
        引擎配置
      </button>
      <button
        type="button"
        class="admin-section-tab"
        :class="{ active: activeAdminSection === 'custom-ai-usage' }"
        role="tab"
        data-admin-section="custom-ai-usage"
        :aria-selected="activeAdminSection === 'custom-ai-usage' ? 'true' : 'false'"
        @click="activeAdminSection = 'custom-ai-usage'"
      >
        自定义 AI 用量
      </button>
      <button
        type="button"
        class="admin-section-tab"
        :class="{ active: activeAdminSection === 'system-tts-config' }"
        role="tab"
        data-admin-section="system-tts-config"
        :aria-selected="activeAdminSection === 'system-tts-config' ? 'true' : 'false'"
        @click="activeAdminSection = 'system-tts-config'"
      >
        系统 TTS 配置
      </button>
    </div>

    <div v-if="activeAdminSection === 'engine-config'" class="stats-grid">
      <button
        class="stats-card"
        :class="{ active: matchedQuickFilterKey === 'all' }"
        @click="applyQuickFilter('all')"
      >
        <span class="stats-label">全部配置</span>
        <strong class="stats-value">{{ engineStats.total }}</strong>
      </button>
      <button
        class="stats-card"
        :class="{ active: matchedQuickFilterKey === 'active' }"
        @click="applyQuickFilter('active')"
      >
        <span class="stats-label">启用配置</span>
        <strong class="stats-value">{{ engineStats.active }}</strong>
      </button>
      <button
        class="stats-card"
        :class="{ active: matchedQuickFilterKey === 'interview-active' }"
        @click="applyQuickFilter('interview-active')"
      >
        <span class="stats-label">面试生效</span>
        <strong class="stats-value">{{ engineStats.interviewActive }}</strong>
      </button>
      <button
        class="stats-card"
        :class="{ active: matchedQuickFilterKey === 'resume-active' }"
        @click="applyQuickFilter('resume-active')"
      >
        <span class="stats-label">简历生效</span>
        <strong class="stats-value">{{ engineStats.resumeActive }}</strong>
      </button>
      <button
        class="stats-card warning"
        :class="{ active: matchedQuickFilterKey === 'multi-active-risk' }"
        @click="applyQuickFilter('multi-active-risk')"
      >
        <span class="stats-label">生效冲突风险</span>
        <strong class="stats-value">{{ engineStats.multiActiveRisk }}</strong>
      </button>
    </div>

    <el-card v-if="activeAdminSection === 'custom-ai-usage'" shadow="never" class="custom-ai-limit-card">
      <div class="custom-ai-limit-copy">
        <span>用户自定义 AI 每日上限</span>
        <strong>{{ customAiDailyLimit || '--' }}</strong>
        <small>命中用户自带 API Key 时只扣此独立调用次数，不扣平台 AI 额度。</small>
      </div>
      <div class="custom-ai-limit-controls">
        <el-input-number
          v-model="customAiDailyLimitForm.limit"
          :min="1"
          :max="10000"
          :step="5"
          controls-position="right"
        />
        <el-button
          type="primary"
          :loading="customAiDailyLimitSaving"
          @click="handleCustomAiDailyLimitSave"
        >
          保存上限
        </el-button>
      </div>
    </el-card>

    <el-card v-if="activeAdminSection === 'custom-ai-usage'" shadow="never" class="custom-ai-usage-card">
      <div class="custom-ai-usage-header">
        <div>
          <span>用户自定义 AI 用量统计</span>
          <strong>{{ customAiUsageRangeTitle }}自定义 AI 调用</strong>
        </div>
        <div class="custom-ai-usage-controls">
          <el-radio-group
            v-model="customAiUsageRangePreset"
            size="small"
            aria-label="自定义 AI 用量统计快捷范围"
            @change="handleCustomAiUsageRangePresetChange"
          >
            <el-radio-button value="today">今天</el-radio-button>
            <el-radio-button value="last7">近 7 天</el-radio-button>
            <el-radio-button value="last30">近 30 天</el-radio-button>
            <el-radio-button value="custom">自定义</el-radio-button>
          </el-radio-group>
          <el-date-picker
            v-model="customAiUsageRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            :clearable="false"
            @change="handleCustomAiUsageRangeChange"
          />
          <el-button :loading="customAiUsageLoading" @click="fetchCustomAiUsageStats">刷新统计</el-button>
        </div>
      </div>
      <div class="custom-ai-usage-summary">
        <div>
          <span>总调用</span>
          <strong>{{ customAiUsageStats.totalCalls }}</strong>
        </div>
        <div>
          <span>配置用户</span>
          <strong>{{ customAiUsageStats.configuredUserCount }}</strong>
        </div>
        <div>
          <span>范围活跃</span>
          <strong>{{ customAiUsageStats.activeUserCount }}</strong>
        </div>
      </div>
      <div class="custom-ai-trend-section">
        <div class="custom-ai-trend-header">
          <div class="custom-ai-trend-copy">
            <span class="custom-ai-usage-section-title">用户自定义 AI 按日趋势</span>
            <small>{{ customAiUsageTrends.startDate }} 至 {{ customAiUsageTrends.endDate }}</small>
          </div>
          <div class="custom-ai-trend-summary" aria-label="用户自定义 AI 趋势摘要">
            <span>区间总调用 <strong>{{ customAiUsageTrends.totalCalls }}</strong></span>
            <span>活跃用户 <strong>{{ customAiUsageTrends.activeUserCount }}</strong></span>
          </div>
          <button
            type="button"
            class="custom-ai-trend-toggle"
            :aria-expanded="customAiTrendExpanded ? 'true' : 'false'"
            @click="customAiTrendExpanded = !customAiTrendExpanded"
          >
            {{ customAiTrendExpanded ? '收起趋势' : '展开趋势' }}
          </button>
        </div>
        <div v-if="customAiTrendExpanded" class="custom-ai-trend-panel">
          <div class="custom-ai-trend-controls">
            <el-radio-group
              v-model="customAiTrendPreset"
              size="small"
              @change="handleCustomAiTrendPresetChange"
            >
              <el-radio-button value="last7">近 7 天</el-radio-button>
              <el-radio-button value="last30">近 30 天</el-radio-button>
              <el-radio-button value="custom">自定义</el-radio-button>
            </el-radio-group>
            <el-date-picker
              v-model="customAiTrendRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :clearable="false"
              :disabled="customAiTrendPreset !== 'custom'"
              @change="handleCustomAiTrendRangeChange"
            />
            <el-button :loading="customAiTrendLoading" @click="fetchCustomAiUsageTrends">刷新趋势</el-button>
          </div>
          <div class="custom-ai-trend-chart" v-loading="customAiTrendLoading">
            <Line
              v-if="hasCustomAiTrendData"
              :data="customAiTrendChartData"
              :options="customAiTrendChartOptions"
              :height="220"
            />
            <div v-else class="custom-ai-trend-empty">暂无趋势数据</div>
          </div>
        </div>
      </div>
      <div class="custom-ai-usage-section">
        <span class="custom-ai-usage-section-title">功能分布</span>
        <div class="custom-ai-type-list">
          <span v-if="customAiUsageStats.typeStats.length === 0" class="custom-ai-empty-text">暂无调用</span>
          <span
            v-for="item in customAiUsageStats.typeStats"
            :key="item.usageType"
            class="custom-ai-type-chip"
          >
            {{ item.usageTypeDesc }} <strong>{{ item.callCount }}</strong>
          </span>
        </div>
      </div>
      <el-table
        :data="customAiUsageStats.userStats"
        v-loading="customAiUsageLoading"
        border
        size="small"
        class="custom-ai-usage-table"
        empty-text="所选范围暂无用户自定义 AI 调用"
      >
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column label="用户" min-width="150">
          <template #default="{ row }">
            <div class="custom-ai-user-cell">
              <strong>{{ row.nickname || row.username || '--' }}</strong>
              <span>{{ row.username || '--' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="totalCalls" label="总调用" width="90" align="center" />
        <el-table-column label="功能明细" min-width="260">
          <template #default="{ row }">
            <div class="custom-ai-type-list compact">
              <span
                v-for="item in row.typeStats"
                :key="`${row.userId}-${item.usageType}`"
                class="custom-ai-type-chip"
              >
                {{ item.usageTypeDesc }} <strong>{{ item.callCount }}</strong>
              </span>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="custom-ai-usage-footer" v-if="customAiUsageStats.totalUsers > 0">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :current-page="customAiUsagePagination.page"
          :page-size="customAiUsagePagination.pageSize"
          :total="customAiUsageStats.totalUsers"
          @current-change="handleCustomAiUsagePageChange"
        />
      </div>
    </el-card>

    <el-card v-if="activeAdminSection === 'system-tts-config'" shadow="never" class="system-tts-card" v-loading="systemTtsLoading">
      <div class="system-tts-header">
        <div>
          <span>系统级云端语音</span>
          <strong>系统 TTS 配置</strong>
          <small>未配置自定义 TTS 的用户会使用此配置进行语音面试播报；用户自定义配置优先。</small>
        </div>
        <el-switch
          v-model="systemTtsForm.enabled"
          active-text="启用"
          inactive-text="禁用"
        />
      </div>

      <el-form label-width="100px" class="system-tts-form">
        <div class="system-tts-grid">
          <el-form-item label="服务商">
            <el-select
              v-model="systemTtsForm.ttsProvider"
              placeholder="选择 TTS Provider"
              @change="handleSystemTtsProviderChange"
            >
              <el-option
                v-for="preset in systemTtsProviderPresets"
                :key="preset.value"
                :label="preset.label"
                :value="preset.value"
                :disabled="preset.disabled"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="Base URL">
            <el-input v-model.trim="systemTtsForm.baseUrl" maxlength="512" placeholder="https://api.example.com/v1" />
          </el-form-item>
          <el-form-item label="API Key">
            <el-input v-model="systemTtsForm.apiKey" type="password" show-password maxlength="1024" placeholder="留空或保留脱敏值表示复用已保存 Key" />
          </el-form-item>
          <el-form-item label="模型">
            <el-select
              v-if="systemTtsModelOptions.length > 0"
              v-model="systemTtsForm.model"
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入模型"
            >
              <el-option
                v-for="item in systemTtsModelOptions"
                :key="item.id"
                :label="item.name || item.id"
                :value="item.id"
              />
            </el-select>
            <el-input v-else v-model.trim="systemTtsForm.model" maxlength="128" placeholder="tts-1" />
          </el-form-item>
          <el-form-item label="音色">
            <el-select
              v-if="systemTtsVoiceOptions.length > 0"
              v-model="systemTtsForm.voiceId"
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入音色"
            >
              <el-option
                v-for="item in systemTtsVoiceOptions"
                :key="item.id"
                :label="item.name || item.id"
                :value="item.id"
              />
            </el-select>
            <el-input v-else v-model.trim="systemTtsForm.voiceId" maxlength="128" placeholder="alloy" />
          </el-form-item>
          <el-form-item label="端点路径">
            <el-input v-model.trim="systemTtsForm.endpointPath" maxlength="128" placeholder="/audio/speech" />
          </el-form-item>
        </div>
        <div v-if="systemTtsConnectivityResult" class="system-tts-result" :class="{ failed: !systemTtsConnectivityResult.success }">
          <span>{{ systemTtsConnectivityResult.message || (systemTtsConnectivityResult.success ? 'TTS 连通测试成功' : 'TTS 连通测试失败') }}</span>
          <small v-if="systemTtsConnectivityResult.latencyMs">{{ systemTtsConnectivityResult.latencyMs }}ms</small>
        </div>
        <div class="system-tts-actions">
          <el-button :loading="systemTtsSaving" type="primary" @click="handleSystemTtsSave">保存配置</el-button>
          <el-button :loading="systemTtsTesting" @click="handleSystemTtsConnectivityTest">测试连通性</el-button>
          <el-button :loading="systemTtsDiscovering" @click="handleSystemTtsDiscover">获取模型/音色</el-button>
          <el-button :loading="systemTtsPreviewing" @click="handleSystemTtsPreview">预览音色</el-button>
        </div>
      </el-form>
    </el-card>

    <div v-if="activeAdminSection === 'engine-config'" class="filter-bar">
      <!-- 引擎配置本地筛选：方便按业务类型和状态快速定位当前生效配置 -->
      <el-input
        v-model.trim="keyword"
        class="filter-item keyword"
        placeholder="按编码/名称/模型搜索"
        clearable
      >
        <template #prefix>
          <el-icon class="filter-icon"><Search /></el-icon>
        </template>
      </el-input>
      <el-select v-model="businessTypeFilter" class="filter-item" placeholder="按业务类型筛选" clearable>
        <el-option label="全部业务" value="all" />
        <el-option label="模拟面试" value="interview" />
        <el-option label="简历诊断" value="resume" />
      </el-select>
      <el-select v-model="providerFilter" class="filter-item" placeholder="按 Provider 筛选" clearable>
        <el-option label="全部 Provider" value="all" />
        <el-option
          v-for="provider in providerTypeOptions"
          :key="provider"
          :label="provider"
          :value="provider"
        />
      </el-select>
      <el-select v-model="statusFilter" class="filter-item" placeholder="按状态筛选" clearable>
        <el-option label="全部状态" value="all" />
        <el-option label="仅启用" value="active" />
        <el-option label="仅禁用" value="inactive" />
      </el-select>
      <el-select v-model="activeScopeFilter" class="filter-item" placeholder="按生效范围筛选" clearable>
        <el-option label="全部生效范围" value="all" />
        <el-option label="当前业务生效中" value="current-active" />
        <el-option label="当前业务未生效" value="not-current-active" />
      </el-select>
      <el-button class="reset-btn" @click="resetFilters">重置筛选</el-button>
    </div>

    <div v-if="activeAdminSection === 'engine-config'" class="filter-result">
      当前筛选结果：<span class="result-count">{{ filteredEngineList.length }}</span> / {{ engineList.length }} 条
    </div>
    <el-alert
      v-if="activeAdminSection === 'engine-config' && hasMultiActiveRisk"
      type="warning"
      :closable="false"
      class="risk-alert"
      title="检测到同业务存在多条启用配置，请尽快检查并完成收敛，避免运行时路由不确定。"
    />

    <el-card v-if="activeAdminSection === 'engine-config'" shadow="never" class="table-card">
      <el-table ref="engineTableRef"
        :data="pagedEngineList"
        v-loading="tableLoading"
        border
        stripe
        :empty-text="tableEmptyText"
        class="engine-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" align="center">
          <template #header>
            <div class="table-header">ID</div>
          </template>
        </el-table-column>
        <el-table-column prop="engineCode" label="引擎编码" min-width="140" align="center">
          <template #header>
            <div class="table-header">引擎编码</div>
          </template>
        </el-table-column>
        <el-table-column prop="engineName" label="引擎名称" min-width="140" align="center">
          <template #header>
            <div class="table-header">引擎名称</div>
          </template>
        </el-table-column>
        <el-table-column prop="providerType" label="Provider" min-width="120" align="center">
          <template #header>
            <div class="table-header">Provider</div>
          </template>
        </el-table-column>
        <el-table-column prop="businessTypeDesc" label="业务类型" min-width="120" align="center">
          <template #header>
            <div class="table-header">业务类型</div>
          </template>
          <template #default="{ row }">
            <div class="business-cell">
              <span>{{ row.businessTypeDesc }}</span>
              <el-tag
                v-if="isCurrentActiveEngine(row)"
                type="success"
                effect="plain"
                size="small"
              >
                当前生效
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="modelName" label="模型名" min-width="140" align="center">
          <template #header>
            <div class="table-header">模型名</div>
          </template>
        </el-table-column>
        <el-table-column label="多模态" width="110" align="center">
          <template #header>
            <div class="table-header">多模态</div>
          </template>
          <template #default="{ row }">
            <el-tag
              :type="row.supportsMultimodal === 1 ? 'success' : 'info'"
              effect="plain"
              size="small"
            >
              {{ row.supportsMultimodal === 1 ? '支持' : '不支持' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="思考模式" width="110" align="center">
          <template #header>
            <div class="table-header">思考模式</div>
          </template>
          <template #default="{ row }">
            <el-tag
              :type="row.thinkingMode === 'enabled' ? 'success' : row.thinkingMode === 'disabled' ? 'warning' : 'info'"
              effect="plain"
              size="small"
            >
              {{ row.thinkingMode === 'enabled' ? '开启' : row.thinkingMode === 'disabled' ? '关闭' : '不传' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="baseUrl" label="基础地址" min-width="180" show-overflow-tooltip align="center">
          <template #header>
            <div class="table-header">基础地址</div>
          </template>
        </el-table-column>
        <el-table-column prop="apiKey" label="API Key(脱敏)" min-width="140" align="center">
          <template #header>
            <div class="table-header">API Key(脱敏)</div>
          </template>
        </el-table-column>
        <el-table-column prop="temperature" label="温度" width="90" align="center">
          <template #header>
            <div class="table-header">温度</div>
          </template>
        </el-table-column>
        <el-table-column prop="maxTokens" label="MaxTokens" width="110" align="center">
          <template #header>
            <div class="table-header">MaxTokens</div>
          </template>
        </el-table-column>
        <el-table-column prop="timeoutMs" label="超时(ms)" width="100" align="center">
          <template #header>
            <div class="table-header">超时(ms)</div>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center">
          <template #header>
            <div class="table-header">排序</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #header>
            <div class="table-header">状态</div>
          </template>
          <template #default="{ row }">
            <el-tag
              :type="row.isActive === 1 ? 'success' : 'info'"
              effect="plain"
              size="small"
              class="status-tag"
            >
              {{ row.isActive === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #header>
            <div class="table-header">操作</div>
          </template>
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" @click="openEditDialog(row)" class="action-btn">
                编辑
              </el-button>
              <el-button
                size="small"
                @click="handleToggleActive(row)"
                class="action-btn"
                :loading="toggleLoadingIds.has(row.id)"
              >
                {{ row.isActive === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button
                size="small"
                type="danger"
                plain
                @click="handleDelete(row)"
                class="action-btn"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="filteredEngineList.length"
          :current-page="pagination.page"
          :page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEditMode ? '编辑 AI 引擎配置' : '新增 AI 引擎配置'"
      width="760px"
      destroy-on-close
      class="engine-dialog"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="引擎编码" prop="engineCode">
              <el-input v-model.trim="formData.engineCode" :disabled="submitLoading" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="引擎名称" prop="engineName">
              <el-input v-model.trim="formData.engineName" :disabled="submitLoading" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Provider" prop="providerType">
              <el-select
                v-model="formData.providerType"
                filterable
                allow-create
                default-first-option
                style="width: 100%"
                placeholder="请选择或输入 Provider"
                :disabled="submitLoading"
              >
                <el-option
                  v-for="provider in providerSuggestOptions"
                  :key="provider"
                  :label="provider"
                  :value="provider"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务类型" prop="businessType">
              <el-select v-model="formData.businessType" style="width: 100%" :disabled="submitLoading" clearable>
                <el-option value="interview" label="模拟面试(interview)" />
                <el-option value="resume" label="简历诊断(resume)" />
              </el-select>
              <div v-if="formData.businessType === 'interview'" class="field-tip">
                interview 配置同时服务首页「Offer 辅助」（薪资谈判模拟、谈薪话术）。
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="模型名" prop="modelName">
          <div class="model-fetch-row">
            <el-select
              v-model="formData.modelName"
              filterable
              allow-create
              default-first-option
              clearable
              style="width: 100%"
              placeholder="选择或输入模型名"
              :disabled="submitLoading"
            >
              <el-option
                v-for="model in modelOptions"
                :key="model.id"
                :label="model.name || model.id"
                :value="model.id"
              />
            </el-select>
            <el-button
              :loading="modelFetchLoading"
              :disabled="submitLoading || !formData.baseUrl || (!isEditMode && !formData.apiKey)"
              @click="handleModelFetch"
            >
              获取模型
            </el-button>
          </div>
        </el-form-item>

        <el-form-item v-if="formData.businessType === 'resume'" label="多模态">
          <el-switch
            v-model="resumeMultimodalEnabled"
            :disabled="submitLoading"
            inline-prompt
            active-text="支持"
            inactive-text="关闭"
          />
          <div class="field-tip">仅用于图片型 PDF 的「看图转文本」，不会替代原有文本型简历诊断链路。</div>
        </el-form-item>

        <el-form-item label="思考模式">
          <el-select v-model="formData.thinkingMode" :disabled="submitLoading" style="width: 100%">
            <el-option label="不传（使用模型默认行为）" value="none" />
            <el-option label="开启" value="enabled" />
            <el-option label="关闭" value="disabled" />
          </el-select>
          <div class="field-tip">控制是否向模型发送 thinking 参数。不支持的模型会自动忽略此配置。</div>
        </el-form-item>

        <el-form-item label="基础地址" prop="baseUrl">
          <div class="base-url-row">
            <el-input v-model.trim="formData.baseUrl" :disabled="submitLoading" />
            <el-button :disabled="submitLoading" @click="fillBaseUrlByProvider">
              按 Provider 填充
            </el-button>
          </div>
          <div class="field-tip">
            推荐地址：{{ providerBaseUrlSuggestion || '当前 Provider 暂无预设地址，请手动填写' }}
          </div>
        </el-form-item>

        <el-form-item label="API Key" prop="apiKey">
          <el-input
            v-model.trim="formData.apiKey"
            type="password"
            show-password
            :placeholder="isEditMode ? '留空表示不修改 API Key' : '请输入 API Key'"
            :disabled="submitLoading"
          />
        </el-form-item>

        <div class="connectivity-test-panel" aria-live="polite">
          <el-button
            type="primary"
            plain
            :loading="connectivityTestLoading"
            :disabled="submitLoading"
            @click="handleConnectivityTest"
          >
            <el-icon><Connection /></el-icon>
            测试连通性
          </el-button>
          <span class="field-tip">
            新增时使用当前输入的 API Key；编辑时未输入新 Key 则使用已保存密钥。
          </span>
          <el-alert
            v-if="connectivityTestResult"
            class="connectivity-test-result"
            :type="connectivityTestResult.success ? 'success' : 'error'"
            :closable="false"
            :title="connectivityTestResult.message"
            show-icon
          >
            <div class="connectivity-test-detail">
              <span v-if="connectivityTestResult.latencyMs !== null">
                耗时：{{ connectivityTestResult.latencyMs }}ms
              </span>
              <span v-if="connectivityTestResult.responsePreview">
                返回：{{ connectivityTestResult.responsePreview }}
              </span>
              <span v-if="connectivityTestResult.errorMessage">
                原因：{{ connectivityTestResult.errorMessage }}
              </span>
            </div>
          </el-alert>
        </div>

        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="温度" prop="temperature">
              <el-input-number
                v-model="formData.temperature"
                :min="0"
                :max="2"
                :step="0.1"
                :precision="1"
                :disabled="submitLoading"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="MaxTokens" prop="maxTokens">
              <el-input-number v-model="formData.maxTokens" :min="1" :disabled="submitLoading" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="超时(ms)" prop="timeoutMs">
              <el-input-number v-model="formData.timeoutMs" :min="1000" :step="500" :disabled="submitLoading" />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="field-tip">
          配置建议：{{ parameterSuggestionText }}
        </div>

        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="formData.sort" :min="0" :disabled="submitLoading" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="isActive">
              <el-radio-group v-model="formData.isActive" :disabled="submitLoading">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注">
          <el-input v-model.trim="formData.remark" :disabled="submitLoading" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false" class="dialog-btn">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm" class="dialog-btn primary">
          {{ isEditMode ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { Connection, Edit, Search } from '@element-plus/icons-vue'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  LineElement,
  PointElement,
  Filler
} from 'chart.js'
import {
  createAdminAiEngine,
  deleteAiEngine,
  deleteAiEngines,
  fetchAdminAiModels,
  getAdminAiEngines,
  getCustomAiDailyLimit,
  getCustomAiUsageTrends,
  getCustomAiUsageStats,
  testAdminAiEngineConnectivity,
  toggleAdminAiEngineActive,
  toggleAiEnginesBatchActive,
  updateAdminAiEngine,
  updateCustomAiDailyLimit
} from '@/api/admin/aiEngines'
import {
  discoverAdminTtsOptions,
  getAdminTtsConfig,
  previewAdminTtsVoice,
  saveAdminTtsConfig,
  testAdminTtsConnectivity
} from '@/api/admin/ttsConfig'
import {
  confirmAdminRiskAction,
  resolveAdminTableEmptyText,
  showAdminError,
  showAdminSuccess,
  showAdminWarning
} from '@/utils/adminFeedback'

// 注册自定义 AI 趋势折线图所需模块，避免引入额外图表依赖。
ChartJS.register(Title, Tooltip, Legend, CategoryScale, LinearScale, LineElement, PointElement, Filler)

// 表格数据：AI 引擎配置列表。
const engineList = ref([])
const tableLoading = ref(false)
// 页面主分区：默认展示引擎配置，统计与趋势放到独立分区，避免挤压配置主流程。
const activeAdminSection = ref('engine-config')
// 表格实例：用于全选操作
const engineTableRef = ref(null)
// 批量选择状态：用于批量删除操作
const selectedEngines = ref([])
// 批量删除加载状态
const batchDeleteLoading = ref(false)
const keyword = ref('')
const businessTypeFilter = ref('all')
const providerFilter = ref('all')
const statusFilter = ref('all')
const activeScopeFilter = ref('all')
const pagination = reactive({
  page: 1,
  pageSize: 10
})
const customAiDailyLimit = ref(0)
const customAiDailyLimitSaving = ref(false)
const customAiDailyLimitForm = reactive({
  limit: 50
})
const customAiUsageRangePreset = ref('last7')
// 功能分布和用户明细共用同一日期范围，避免两个统计区域口径不一致。
const customAiUsageRange = ref(buildRecentDateRange(7))
const customAiUsageLoading = ref(false)
const customAiUsagePagination = reactive({
  page: 1,
  pageSize: 5
})
const customAiUsageStats = ref(buildEmptyCustomAiUsageStats())
const customAiTrendPreset = ref('last7')
const customAiTrendRange = ref(buildRecentDateRange(7))
const customAiTrendLoading = ref(false)
const customAiUsageTrends = ref(buildEmptyCustomAiUsageTrends())
// 自定义 AI 用量已隔离到独立分区，切换进入该分区后默认展开趋势图。
const customAiTrendExpanded = ref(true)
const systemTtsLoading = ref(false)
const systemTtsSaving = ref(false)
const systemTtsTesting = ref(false)
const systemTtsDiscovering = ref(false)
const systemTtsPreviewing = ref(false)
const systemTtsConfigured = ref(false)
const systemTtsConnectivityResult = ref(null)
const systemTtsDiscoveryResult = ref(null)
let systemTtsPreviewAudio = null
let systemTtsPreviewObjectUrl = ''
const systemTtsForm = reactive({
  enabled: false,
  ttsProvider: 'openai',
  baseUrl: '',
  apiKey: '',
  model: '',
  voiceId: '',
  endpointPath: '/audio/speech'
})

// 弹窗编辑状态：复用一个表单完成新增和编辑。
const dialogVisible = ref(false)
const isEditMode = ref(false)
const submitLoading = ref(false)
const connectivityTestLoading = ref(false)
const connectivityTestResult = ref(null)
const modelFetchLoading = ref(false)
const modelOptions = ref([])
const formRef = ref(null)
/** 正在切换状态的行 ID 集合（per-row 锁，避免全局互斥） */
const toggleLoadingIds = ref(new Set())
const editOriginalPayload = ref(null)

// 表单字段：与后端 DTO 字段保持一致。
const formData = reactive({
  id: null,
  engineCode: '',
  engineName: '',
  providerType: '',
  businessType: 'interview',
  modelName: '',
  supportsMultimodal: 0,
  thinkingMode: 'none',
  baseUrl: '',
  apiKey: '',
  temperature: 1.0,
  maxTokens: 4096,
  timeoutMs: 30000,
  isActive: 1,
  sort: 0,
  remark: ''
})

/**
 * 用布尔开关承载表单展示，提交时再转换为后端约定的 0/1。
 */
const resumeMultimodalEnabled = computed({
  get: () => Number(formData.supportsMultimodal) === 1,
  set: (value) => {
    formData.supportsMultimodal = value ? 1 : 0
  }
})

// Provider 预设：用于提升表单填写效率，减少重复录入基础地址。
const providerBaseUrlPresetMap = {
  openai: 'https://api.openai.com/v1',
  doubao: 'https://ark.cn-beijing.volces.com/api/v3',
  kimi: 'https://api.moonshot.cn/v1',
  deepseek: 'https://api.deepseek.com/v1',
  qwen: 'https://dashscope.aliyuncs.com/compatible-mode/v1'
}

const systemTtsProviderPresets = [
  {
    value: 'openai',
    label: 'OpenAI',
    disabled: false,
    defaultBaseUrl: 'https://api.openai.com/v1',
    defaultModel: 'tts-1',
    defaultVoiceId: 'alloy',
    endpointPath: '/audio/speech',
    voices: [
      { id: 'alloy', name: 'Alloy' },
      { id: 'echo', name: 'Echo' },
      { id: 'fable', name: 'Fable' },
      { id: 'onyx', name: 'Onyx' },
      { id: 'nova', name: 'Nova' },
      { id: 'shimmer', name: 'Shimmer' }
    ]
  },
  {
    value: 'mimo',
    label: 'MiMo（小米）',
    disabled: false,
    defaultBaseUrl: 'https://api.xiaomimimo.com/v1',
    defaultModel: 'mimo-v2.5-tts',
    defaultVoiceId: 'mimo_default',
    endpointPath: '/chat/completions',
    voices: [
      { id: 'mimo_default', name: 'MiMo-默认' },
      { id: 'Mia', name: 'Mia' },
      { id: 'Chloe', name: 'Chloe' },
      { id: 'Milo', name: 'Milo' },
      { id: 'Dean', name: 'Dean' }
    ]
  },
  {
    value: 'edge',
    label: 'EdgeTTS',
    disabled: false,
    defaultBaseUrl: 'https://speech.platform.bing.com',
    defaultModel: 'edge-tts',
    defaultVoiceId: 'zh-CN-XiaoxiaoNeural',
    endpointPath: '/consumer/speech/synthesize/readaloud/edge/v1',
    voices: [
      { id: 'zh-CN-XiaoxiaoNeural', name: '晓晓（女声，普通话）' },
      { id: 'zh-CN-XiaoyiNeural', name: '晓伊（女声，普通话）' },
      { id: 'zh-CN-YunjianNeural', name: '云健（男声，普通话）' },
      { id: 'zh-CN-YunxiNeural', name: '云希（男声，普通话）' },
      { id: 'zh-CN-YunxiaNeural', name: '云夏（男声，普通话）' },
      { id: 'zh-CN-YunyangNeural', name: '云扬（男声，普通话）' },
      { id: 'zh-HK-HiuGaaiNeural', name: '晓佳（女声，粤语）' },
      { id: 'zh-HK-HiuMaanNeural', name: '晓曼（女声，粤语）' },
      { id: 'zh-HK-WanLungNeural', name: '云龙（男声，粤语）' },
      { id: 'zh-TW-HsiaoChenNeural', name: '晓臻（女声，台湾普通话）' },
      { id: 'zh-TW-HsiaoYuNeural', name: '晓雨（女声，台湾普通话）' },
      { id: 'zh-TW-YunJheNeural', name: '云哲（男声，台湾普通话）' }
    ]
  },
  {
    value: 'gemini',
    label: 'Gemini',
    disabled: false,
    defaultBaseUrl: 'https://generativelanguage.googleapis.com',
    defaultModel: 'gemini-2.5-flash-preview-tts',
    defaultVoiceId: 'Kore',
    endpointPath: '/v1beta/models/{model}:generateContent',
    voices: [
      { id: 'Kore', name: 'Kore' },
      { id: 'Puck', name: 'Puck' },
      { id: 'Charon', name: 'Charon' },
      { id: 'Fenrir', name: 'Fenrir' },
      { id: 'Aoede', name: 'Aoede' }
    ]
  },
  {
    value: 'minimax',
    label: 'MiniMax',
    disabled: false,
    defaultBaseUrl: 'https://api.minimax.chat',
    defaultModel: 'speech-02-turbo',
    defaultVoiceId: 'male-qn-qingse',
    endpointPath: '/v1/t2a_v2',
    voices: [
      { id: 'male-qn-qingse', name: '青涩男声' },
      { id: 'male-qn-jingying', name: '精英男声' },
      { id: 'female-shaonv', name: '少女女声' },
      { id: 'female-yujie', name: '御姐女声' },
      { id: 'presenter_male', name: '主持男声' },
      { id: 'presenter_female', name: '主持女声' }
    ]
  },
  {
    value: 'qwen',
    label: 'Qwen',
    disabled: false,
    defaultBaseUrl: 'https://dashscope.aliyuncs.com',
    defaultModel: 'qwen3-tts-flash',
    defaultVoiceId: 'Cherry',
    endpointPath: '/api/v1/services/aigc/multimodal-generation/generation',
    voices: [
      { id: 'Cherry', name: 'Cherry' },
      { id: 'Serena', name: 'Serena' },
      { id: 'Ethan', name: 'Ethan' },
      { id: 'Chelsie', name: 'Chelsie' }
    ]
  },
  {
    value: 'xai',
    label: 'xAI',
    disabled: false,
    defaultBaseUrl: 'https://api.x.ai',
    defaultModel: 'grok-tts',
    defaultVoiceId: 'Fritz-PlayAI',
    endpointPath: '/v1/tts',
    voices: [
      { id: 'Fritz-PlayAI', name: 'Fritz' },
      { id: 'Aiden-PlayAI', name: 'Aiden' },
      { id: 'Luna-PlayAI', name: 'Luna' }
    ]
  }
]

const systemTtsModelOptions = computed(() => {
  const models = systemTtsDiscoveryResult.value?.models
  return Array.isArray(models) ? models : []
})

const systemTtsVoiceOptions = computed(() => {
  const voices = systemTtsDiscoveryResult.value?.voices
  return Array.isArray(voices) ? voices : []
})

/**
 * 校验基础地址格式。
 * 作用：保证基础地址至少是 http(s) URL，避免明显错误配置入库。
 * @param {any} _rule
 * @param {string} value
 * @param {(error?: Error) => void} callback
 */
const validateBaseUrl = (_rule, value, callback) => {
  const text = String(value || '').trim()
  if (!text) {
    callback(new Error('请输入基础地址'))
    return
  }
  if (!/^https?:\/\//i.test(text)) {
    callback(new Error('基础地址必须以 http:// 或 https:// 开头'))
    return
  }
  callback()
}

// 表单校验：编辑时 API Key 可以留空，其它核心字段保持必填。
const formRules = {
  engineCode: [{ required: true, message: '请输入引擎编码', trigger: 'blur' }],
  engineName: [{ required: true, message: '请输入引擎名称', trigger: 'blur' }],
  providerType: [{ required: true, message: '请输入 Provider 类型', trigger: 'change' }],
  businessType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
  modelName: [{ required: true, message: '请输入模型名', trigger: 'blur' }],
  baseUrl: [{ validator: validateBaseUrl, trigger: 'blur' }],
  apiKey: [
    {
      validator: (_, value, callback) => {
        // 关键规则：新增时必须提供 API Key，编辑时允许留空表示不更新。
        if (!isEditMode.value && !value) {
          callback(new Error('新增时必须填写 API Key'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  temperature: [{ required: true, message: '请输入温度', trigger: 'change' }],
  maxTokens: [{ required: true, message: '请输入 MaxTokens', trigger: 'change' }],
  timeoutMs: [{ required: true, message: '请输入超时毫秒', trigger: 'change' }],
  isActive: [{ required: true, message: '请选择状态', trigger: 'change' }],
  sort: [{ required: true, message: '请输入排序', trigger: 'change' }]
}

/**
 * Provider 下拉选项。
 * 作用：从真实数据生成，减少硬编码维护成本。
 */
const providerTypeOptions = computed(() => {
  return Array.from(
    new Set(
      engineList.value
        .map((item) => String(item.providerType || '').trim())
        .filter(Boolean)
    )
  )
})

/**
 * Provider 建议选项。
 * 作用：合并系统预设与历史配置，兼顾规范输入与灵活扩展。
 */
const providerSuggestOptions = computed(() => {
  const presetProviders = Object.keys(providerBaseUrlPresetMap)
  return Array.from(new Set([...presetProviders, ...providerTypeOptions.value]))
})

/**
 * 当前 Provider 对应的推荐基础地址。
 * 作用：为管理员提供可直接复用的默认地址，减少手填错误。
 */
const providerBaseUrlSuggestion = computed(() => {
  const providerKey = String(formData.providerType || '').trim().toLowerCase()
  if (!providerKey) return ''
  return providerBaseUrlPresetMap[providerKey] || ''
})

/**
 * 参数建议文案。
 * 作用：按业务场景给出温度/超时配置参考，提高配置一致性。
 */
const parameterSuggestionText = computed(() => {
  if (formData.businessType === 'resume') {
    return '简历诊断建议温度 0.2-0.6，超时建议不低于 20000ms。'
  }
  return '模拟面试建议温度 0.6-1.0，超时建议不低于 30000ms。'
})

/**
 * 各业务当前生效引擎 ID 映射。
 * 作用：用于识别"当前生效项"并在列表中高亮展示。
 */
const activeEngineIdByBusiness = computed(() => {
  return engineList.value.reduce((map, item) => {
    if (Number(item.isActive) !== 1) return map
    const businessType = String(item.businessType || '')
    if (!businessType) return map
    if (!map[businessType]) {
      map[businessType] = item.id
    }
    return map
  }, {})
})

/**
 * 判断某一行是否是当前业务生效项。
 * @param {Record<string, any>} row
 * @returns {boolean}
 */
const isCurrentActiveEngine = (row) => {
  const businessType = String(row?.businessType || '')
  if (!businessType) return false
  return String(activeEngineIdByBusiness.value[businessType]) === String(row?.id)
}

/**
 * 获取某业务当前生效配置。
 * 作用：启停确认时提示"会替换谁"，让管理员明确影响范围。
 * @param {string} businessType
 * @returns {Record<string, any> | null}
 */
const getCurrentActiveEngineByBusiness = (businessType) => {
  const activeId = activeEngineIdByBusiness.value[String(businessType || '')]
  if (!activeId) return null
  return engineList.value.find((item) => String(item.id) === String(activeId)) || null
}

/**
 * 引擎列表筛选结果。
 * 作用：前端本地筛选用于提升后台配置操作效率，不影响后端数据真实性。
 */
const filteredEngineList = computed(() => {
  return engineList.value.filter((item) => {
    const matchesKeyword = !keyword.value
      || item.engineCode?.includes(keyword.value)
      || item.engineName?.includes(keyword.value)
      || item.modelName?.includes(keyword.value)

    const matchesBusinessType = businessTypeFilter.value === 'all'
      || item.businessType === businessTypeFilter.value

    const matchesProviderType = providerFilter.value === 'all'
      || String(item.providerType || '') === String(providerFilter.value)

    const matchesStatus = statusFilter.value === 'all'
      || (statusFilter.value === 'active' && item.isActive === 1)
      || (statusFilter.value === 'inactive' && item.isActive !== 1)

    const isCurrentActive = isCurrentActiveEngine(item)
    const matchesActiveScope = activeScopeFilter.value === 'all'
      || (activeScopeFilter.value === 'current-active' && isCurrentActive)
      || (activeScopeFilter.value === 'not-current-active' && !isCurrentActive)

    return matchesKeyword && matchesBusinessType && matchesProviderType && matchesStatus && matchesActiveScope
  })
})

/**
 * 当前分页后的数据。
 * 作用：提升大列表浏览效率，不改变后端接口契约。
 */
const pagedEngineList = computed(() => {
  const startIndex = (pagination.page - 1) * pagination.pageSize
  const endIndex = startIndex + pagination.pageSize
  return filteredEngineList.value.slice(startIndex, endIndex)
})

/**
 * 表格空状态文案。
 * 作用：统一"系统无数据"和"筛选后无结果"两种空状态表达。
 */
const tableEmptyText = computed(() => {
  return resolveAdminTableEmptyText(engineList.value.length, filteredEngineList.value.length)
})

/**
 * 列表统计摘要。
 * 作用：提供管理视角下的快速判断与筛选入口。
 */
const engineStats = computed(() => {
  return engineList.value.reduce(
    (summary, item) => {
      summary.total += 1
      if (Number(item.isActive) === 1) {
        summary.active += 1
      }
      if (String(item.businessType) === 'interview' && isCurrentActiveEngine(item)) {
        summary.interviewActive += 1
      }
      if (String(item.businessType) === 'resume' && isCurrentActiveEngine(item)) {
        summary.resumeActive += 1
      }
      return summary
    },
    {
      total: 0,
      active: 0,
      interviewActive: 0,
      resumeActive: 0,
      multiActiveRisk: calculateMultiActiveRisk(engineList.value)
    }
  )
})

/**
 * 当前快捷筛选命中键。
 * 作用：让统计卡片高亮状态与筛选条件一致。
 */
const matchedQuickFilterKey = computed(() => {
  if (businessTypeFilter.value === 'all'
    && statusFilter.value === 'active'
    && activeScopeFilter.value === 'not-current-active') {
    return 'multi-active-risk'
  }
  if (businessTypeFilter.value === 'all' && statusFilter.value === 'all' && activeScopeFilter.value === 'all') {
    return 'all'
  }
  if (businessTypeFilter.value === 'all' && statusFilter.value === 'active' && activeScopeFilter.value === 'all') {
    return 'active'
  }
  if (businessTypeFilter.value === 'interview' && statusFilter.value === 'all' && activeScopeFilter.value === 'current-active') {
    return 'interview-active'
  }
  if (businessTypeFilter.value === 'resume' && statusFilter.value === 'all' && activeScopeFilter.value === 'current-active') {
    return 'resume-active'
  }
  return 'custom'
})

/**
 * 是否存在"同业务多启用"风险。
 * 作用：即使后端做约束，也给前端显式风险观测位，便于排查历史脏数据。
 */
const hasMultiActiveRisk = computed(() => calculateMultiActiveRisk(engineList.value) > 0)

/**
 * 重置表单为默认值。
 */
const resetFormData = () => {
  formData.id = null
  formData.engineCode = ''
  formData.engineName = ''
  formData.providerType = ''
  formData.businessType = 'interview'
  formData.modelName = ''
  formData.supportsMultimodal = 0
  formData.baseUrl = ''
  formData.apiKey = ''
  formData.temperature = 1.0
  formData.maxTokens = 4096
  formData.timeoutMs = 30000
  formData.isActive = 1
  formData.sort = 0
  formData.remark = ''
  connectivityTestResult.value = null
  modelOptions.value = []
}

/**
 * 按 Provider 填充推荐基础地址。
 * 作用：减少重复录入并降低地址拼写错误概率。
 */
const fillBaseUrlByProvider = () => {
  if (!providerBaseUrlSuggestion.value) {
    showAdminWarning('当前 Provider 暂无预设地址，请手动填写')
    return
  }
  formData.baseUrl = providerBaseUrlSuggestion.value
  connectivityTestResult.value = null
}

/**
 * 构建连通测试请求。
 * 作用：测试当前表单值，不落库；编辑态 API Key 留空时由后端读取已保存密钥。
 */
const buildConnectivityTestPayload = () => {
  const payload = {
    id: isEditMode.value ? formData.id : null,
    providerType: String(formData.providerType || '').trim(),
    modelName: String(formData.modelName || '').trim(),
    baseUrl: String(formData.baseUrl || '').trim(),
    thinkingMode: formData.thinkingMode || 'none',
    temperature: formData.temperature,
    maxTokens: formData.maxTokens,
    timeoutMs: formData.timeoutMs
  }
  if (formData.apiKey) {
    payload.apiKey = String(formData.apiKey).trim()
  }
  return payload
}

const buildModelFetchPayload = () => {
  const payload = {
    id: isEditMode.value ? formData.id : undefined,
    providerType: String(formData.providerType || '').trim(),
    baseUrl: String(formData.baseUrl || '').trim(),
    timeoutMs: Number(formData.timeoutMs || 30000)
  }
  if (formData.apiKey) {
    payload.apiKey = String(formData.apiKey).trim()
  }
  return payload
}

const handleModelFetch = async () => {
  if (!formData.baseUrl || (!isEditMode.value && !formData.apiKey)) {
    showAdminWarning('请先填写基础地址和 API Key')
    return
  }

  modelFetchLoading.value = true
  try {
    const res = await fetchAdminAiModels(buildModelFetchPayload())
    const data = res?.data || {}
    if (!data.success) {
      throw new Error(data.errorMessage || data.message || '模型列表获取失败')
    }
    // 模型候选只作为辅助输入，不覆盖管理员已经手动填写的模型名。
    modelOptions.value = Array.isArray(data.models) ? data.models : []
    if (!formData.modelName && modelOptions.value[0]?.id) {
      formData.modelName = modelOptions.value[0].id
    }
    showAdminSuccess(data.message || '模型列表获取成功')
  } catch (error) {
    showAdminError(error, '模型列表获取失败，请手动输入模型名')
  } finally {
    modelFetchLoading.value = false
  }
}

/**
 * 测试当前 AI 引擎配置是否可连通。
 */
const handleSystemTtsProviderChange = (providerId) => {
  const preset = systemTtsProviderPresets.find((item) => item.value === providerId)
  if (!preset || preset.disabled) return
  systemTtsForm.ttsProvider = providerId
  systemTtsForm.baseUrl = preset.defaultBaseUrl
  if (providerId === 'edge') {
    systemTtsForm.apiKey = ''
  }
  systemTtsForm.model = preset.defaultModel
  systemTtsForm.voiceId = preset.defaultVoiceId
  systemTtsForm.endpointPath = preset.endpointPath
  systemTtsConnectivityResult.value = null
  // 服务商预设只回填候选项，不触发网络请求，避免切换 Provider 时产生隐式出网。
  systemTtsDiscoveryResult.value = {
    success: true,
    models: [{ id: preset.defaultModel, name: preset.defaultModel }],
    voices: preset.voices || [],
    ttsEndpointPath: preset.endpointPath
  }
}

const buildSystemTtsPayload = () => ({
  enabled: Boolean(systemTtsForm.enabled),
  ttsProvider: String(systemTtsForm.ttsProvider || '').trim(),
  baseUrl: String(systemTtsForm.baseUrl || '').trim(),
  apiKey: String(systemTtsForm.apiKey || '').trim(),
  model: String(systemTtsForm.model || '').trim(),
  voiceId: String(systemTtsForm.voiceId || '').trim(),
  endpointPath: String(systemTtsForm.endpointPath || '').trim()
})

const validateSystemTtsPayload = (payload, requireApiKey = false) => {
  const hasAnyConfigValue = Boolean(payload.baseUrl || payload.apiKey || payload.model || payload.voiceId)
  if (!payload.enabled && !hasAnyConfigValue) {
    return true
  }
  const ttsKeyRequired = payload.ttsProvider !== 'edge'
  if (!payload.baseUrl || !payload.model || !payload.voiceId) {
    showAdminWarning('请完整填写系统 TTS 地址、模型和音色')
    return false
  }
  if (ttsKeyRequired && requireApiKey && !payload.apiKey && !systemTtsConfigured.value) {
    showAdminWarning('首次配置系统 TTS 时必须填写 API Key')
    return false
  }
  return true
}

const applySystemTtsConfig = (config = {}) => {
  systemTtsConfigured.value = Boolean(config.configured)
  systemTtsForm.enabled = Boolean(config.enabled)
  systemTtsForm.ttsProvider = config.ttsProvider || 'openai'
  systemTtsForm.baseUrl = config.baseUrl || ''
  systemTtsForm.apiKey = config.apiKey || ''
  systemTtsForm.model = config.model || ''
  systemTtsForm.voiceId = config.voiceId || ''
  systemTtsForm.endpointPath = config.endpointPath || '/audio/speech'
  systemTtsConnectivityResult.value = null
}

const fetchSystemTtsConfig = async () => {
  systemTtsLoading.value = true
  try {
    const res = await getAdminTtsConfig()
    applySystemTtsConfig(res?.data || {})
  } catch (error) {
    showAdminError(error?.message || '加载系统 TTS 配置失败')
  } finally {
    systemTtsLoading.value = false
  }
}

const handleSystemTtsSave = async () => {
  const payload = buildSystemTtsPayload()
  if (!validateSystemTtsPayload(payload, true)) return
  systemTtsSaving.value = true
  try {
    const res = await saveAdminTtsConfig(payload)
    applySystemTtsConfig({ ...payload, ...(res?.data || {}) })
    showAdminSuccess('系统 TTS 配置已保存')
  } catch (error) {
    showAdminError(error?.message || '保存系统 TTS 配置失败')
  } finally {
    systemTtsSaving.value = false
  }
}

const handleSystemTtsConnectivityTest = async () => {
  const payload = buildSystemTtsPayload()
  if (!validateSystemTtsPayload(payload, true)) return
  systemTtsTesting.value = true
  systemTtsConnectivityResult.value = null
  try {
    const res = await testAdminTtsConnectivity(payload)
    systemTtsConnectivityResult.value = res?.data || { success: true, message: 'TTS 连通测试成功' }
    showAdminSuccess(systemTtsConnectivityResult.value.message || 'TTS 连通测试成功')
  } catch (error) {
    systemTtsConnectivityResult.value = {
      success: false,
      message: error?.message || 'TTS 连通测试失败'
    }
    showAdminError(systemTtsConnectivityResult.value.message)
  } finally {
    systemTtsTesting.value = false
  }
}

const handleSystemTtsDiscover = async () => {
  const payload = buildSystemTtsPayload()
  if (!payload.baseUrl) {
    showAdminWarning('请先填写系统 TTS 地址')
    return
  }
  if (payload.ttsProvider !== 'edge' && !payload.apiKey && !systemTtsConfigured.value) {
    showAdminWarning('首次获取模型/音色时必须填写 API Key')
    return
  }
  systemTtsDiscovering.value = true
  try {
    const res = await discoverAdminTtsOptions(payload)
    const data = res?.data || {}
    if (!data.success) {
      throw new Error(data.errorMessage || data.message || '模型和音色获取失败')
    }
    systemTtsDiscoveryResult.value = data
    if (data.ttsEndpointPath) {
      systemTtsForm.endpointPath = data.ttsEndpointPath
    }
    if (!systemTtsForm.model && systemTtsModelOptions.value[0]?.id) {
      systemTtsForm.model = systemTtsModelOptions.value[0].id
    }
    if (!systemTtsForm.voiceId && systemTtsVoiceOptions.value[0]?.id) {
      systemTtsForm.voiceId = systemTtsVoiceOptions.value[0].id
    }
    showAdminSuccess(data.message || 'TTS 模型和音色获取成功')
  } catch (error) {
    showAdminError(error?.message || 'TTS 模型和音色获取失败')
  } finally {
    systemTtsDiscovering.value = false
  }
}

const releaseSystemTtsPreviewAudio = () => {
  if (systemTtsPreviewAudio) {
    systemTtsPreviewAudio.pause()
    systemTtsPreviewAudio.onended = null
    systemTtsPreviewAudio.onerror = null
    systemTtsPreviewAudio = null
  }
  if (systemTtsPreviewObjectUrl) {
    URL.revokeObjectURL(systemTtsPreviewObjectUrl)
    systemTtsPreviewObjectUrl = ''
  }
}

const handleSystemTtsPreview = async () => {
  const payload = buildSystemTtsPayload()
  if (!validateSystemTtsPayload(payload, true)) return
  systemTtsPreviewing.value = true
  try {
    releaseSystemTtsPreviewAudio()
    const blob = await previewAdminTtsVoice(payload)
    systemTtsPreviewObjectUrl = URL.createObjectURL(blob)
    systemTtsPreviewAudio = new Audio(systemTtsPreviewObjectUrl)
    systemTtsPreviewAudio.onended = releaseSystemTtsPreviewAudio
    systemTtsPreviewAudio.onerror = () => {
      releaseSystemTtsPreviewAudio()
      showAdminError('系统 TTS 试音播放失败')
    }
    await systemTtsPreviewAudio.play()
    showAdminSuccess('系统 TTS 试音已开始播放')
  } catch (error) {
    releaseSystemTtsPreviewAudio()
    showAdminError(error?.message || '系统 TTS 试音失败')
  } finally {
    systemTtsPreviewing.value = false
  }
}

const handleConnectivityTest = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  connectivityTestLoading.value = true
  connectivityTestResult.value = null
  try {
    const res = await testAdminAiEngineConnectivity(buildConnectivityTestPayload())
    const result = res?.data || {}
    connectivityTestResult.value = {
      success: Boolean(result.success),
      message: result.message || (result.success ? '连通测试成功' : '连通测试失败'),
      latencyMs: result.latencyMs ?? null,
      responsePreview: result.responsePreview || '',
      errorMessage: result.errorMessage || ''
    }
    if (connectivityTestResult.value.success) {
      showAdminSuccess(connectivityTestResult.value.message)
    } else {
      showAdminError(connectivityTestResult.value.message)
    }
  } catch (error) {
    const message = error?.message || '连通测试失败'
    connectivityTestResult.value = {
      success: false,
      message,
      latencyMs: null,
      responsePreview: '',
      errorMessage: message
    }
    showAdminError(message)
  } finally {
    connectivityTestLoading.value = false
  }
}

/**
 * 检查引擎编码重复。
 * 作用：保存前前置提醒，降低接口报错后的回填成本。
 * @returns {Record<string, any> | null}
 */
const findDuplicateEngineCode = () => {
  const currentCode = String(formData.engineCode || '').trim().toLowerCase()
  if (!currentCode) return null
  return engineList.value.find((item) => {
    if (isEditMode.value && String(item.id) === String(formData.id)) return false
    return String(item.engineCode || '').trim().toLowerCase() === currentCode
  }) || null
}

/**
 * 检查同业务同模型重复。
 * 作用：避免同业务配置多条同模型记录，提升配置可维护性。
 * @returns {Record<string, any> | null}
 */
const findDuplicateBusinessModel = () => {
  const currentBusinessType = String(formData.businessType || '')
  const currentModelName = String(formData.modelName || '').trim().toLowerCase()
  if (!currentBusinessType || !currentModelName) return null
  return engineList.value.find((item) => {
    if (isEditMode.value && String(item.id) === String(formData.id)) return false
    return String(item.businessType || '') === currentBusinessType
      && String(item.modelName || '').trim().toLowerCase() === currentModelName
  }) || null
}

/**
 * 计算编辑改动字段。
 * 作用：保存成功后输出精确反馈，方便管理员复核本次改动范围。
 * @param {Record<string, any> | null} previousPayload
 * @param {Record<string, any>} nextPayload
 * @returns {string[]}
 */
const collectChangedFields = (previousPayload, nextPayload) => {
  if (!previousPayload) return []
  const changed = []
  if (String(previousPayload.engineCode) !== String(nextPayload.engineCode)) changed.push('引擎编码')
  if (String(previousPayload.engineName) !== String(nextPayload.engineName)) changed.push('引擎名称')
  if (String(previousPayload.providerType) !== String(nextPayload.providerType)) changed.push('Provider')
  if (String(previousPayload.businessType) !== String(nextPayload.businessType)) changed.push('业务类型')
  if (String(previousPayload.modelName) !== String(nextPayload.modelName)) changed.push('模型名')
  if (Number(previousPayload.supportsMultimodal) !== Number(nextPayload.supportsMultimodal)) changed.push('多模态')
  if (String(previousPayload.thinkingMode || 'none') !== String(nextPayload.thinkingMode || 'none')) changed.push('思考模式')
  if (String(previousPayload.baseUrl) !== String(nextPayload.baseUrl)) changed.push('基础地址')
  if (Number(previousPayload.temperature) !== Number(nextPayload.temperature)) changed.push('温度')
  if (Number(previousPayload.maxTokens) !== Number(nextPayload.maxTokens)) changed.push('MaxTokens')
  if (Number(previousPayload.timeoutMs) !== Number(nextPayload.timeoutMs)) changed.push('超时')
  if (Number(previousPayload.isActive) !== Number(nextPayload.isActive)) changed.push('状态')
  if (Number(previousPayload.sort) !== Number(nextPayload.sort)) changed.push('排序')
  if (String(previousPayload.remark || '') !== String(nextPayload.remark || '')) changed.push('备注')
  return changed
}

/**
 * 计算同业务多启用风险数量。
 * 作用：统计每个业务中启用配置数大于 1 的超额项数量。
 * @param {Array<Record<string, any>>} list
 * @returns {number}
 */
const calculateMultiActiveRisk = (list) => {
  const activeCountMap = list.reduce((map, item) => {
    if (Number(item.isActive) !== 1) return map
    const businessType = String(item.businessType || '')
    if (!businessType) return map
    map[businessType] = (map[businessType] || 0) + 1
    return map
  }, {})

  return Object.values(activeCountMap).reduce((riskCount, activeCount) => {
    if (Number(activeCount) <= 1) return riskCount
    return riskCount + (Number(activeCount) - 1)
  }, 0)
}

function formatDate(date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function buildEmptyCustomAiUsageStats() {
  const fallbackRange = customAiUsageRange?.value || buildRecentDateRange(7)
  return {
    date: fallbackRange[0] === fallbackRange[1] ? fallbackRange[0] : null,
    startDate: fallbackRange[0],
    endDate: fallbackRange[1],
    configuredUserCount: 0,
    activeUserCount: 0,
    totalCalls: 0,
    totalUsers: 0,
    page: 1,
    pageSize: customAiUsagePagination?.pageSize || 5,
    typeStats: [],
    userStats: []
  }
}

function normalizeCustomAiUsageStats(data) {
  const fallback = buildEmptyCustomAiUsageStats()
  return {
    ...fallback,
    ...data,
    startDate: data?.startDate || data?.date || fallback.startDate,
    endDate: data?.endDate || data?.date || fallback.endDate,
    configuredUserCount: Number(data?.configuredUserCount || 0),
    activeUserCount: Number(data?.activeUserCount || 0),
    totalCalls: Number(data?.totalCalls || 0),
    totalUsers: Number(data?.totalUsers || 0),
    page: Number(data?.page || customAiUsagePagination.page),
    pageSize: Number(data?.pageSize || customAiUsagePagination.pageSize),
    typeStats: Array.isArray(data?.typeStats) ? data.typeStats : [],
    userStats: Array.isArray(data?.userStats) ? data.userStats : []
  }
}

function getNormalizedCustomAiUsageRange() {
  if (Array.isArray(customAiUsageRange.value) && customAiUsageRange.value[0] && customAiUsageRange.value[1]) {
    return [customAiUsageRange.value[0], customAiUsageRange.value[1]]
  }
  return buildRecentDateRange(customAiUsageRangePreset.value === 'last30' ? 30 : 7)
}

function buildRecentDateRange(dayCount) {
  const end = new Date()
  const start = new Date(end)
  start.setDate(start.getDate() - (dayCount - 1))
  return [formatDate(start), formatDate(end)]
}

const customAiUsageRangeTitle = computed(() => {
  if (customAiUsageRangePreset.value === 'today') {
    return '今日'
  }
  if (customAiUsageRangePreset.value === 'last30') {
    return '近 30 天'
  }
  if (customAiUsageRangePreset.value === 'last7') {
    return '近 7 天'
  }
  const [startDate, endDate] = getNormalizedCustomAiUsageRange()
  return `${startDate} 至 ${endDate}`
})

function buildEmptyCustomAiUsageTrends() {
  const fallbackRange = customAiTrendRange?.value || buildRecentDateRange(7)
  return {
    startDate: fallbackRange[0],
    endDate: fallbackRange[1],
    totalCalls: 0,
    activeUserCount: 0,
    days: []
  }
}

function normalizeCustomAiUsageTrends(data) {
  return {
    ...buildEmptyCustomAiUsageTrends(),
    ...data,
    totalCalls: Number(data?.totalCalls || 0),
    activeUserCount: Number(data?.activeUserCount || 0),
    days: Array.isArray(data?.days)
      ? data.days.map(item => ({
          date: item?.date || '',
          totalCalls: Number(item?.totalCalls || 0),
          activeUserCount: Number(item?.activeUserCount || 0),
          typeStats: Array.isArray(item?.typeStats) ? item.typeStats : []
        }))
      : []
  }
}

function getNormalizedCustomAiTrendRange() {
  if (Array.isArray(customAiTrendRange.value) && customAiTrendRange.value[0] && customAiTrendRange.value[1]) {
    return [customAiTrendRange.value[0], customAiTrendRange.value[1]]
  }
  return buildRecentDateRange(customAiTrendPreset.value === 'last30' ? 30 : 7)
}

function formatTrendDateLabel(value) {
  if (!value) return ''
  const [, month, day] = String(value).split('-')
  return month && day ? `${month}/${day}` : value
}

const customAiTrendDays = computed(() => {
  const days = customAiUsageTrends.value?.days
  return Array.isArray(days) ? days : []
})

const hasCustomAiTrendData = computed(() => customAiTrendDays.value.some(item =>
  Number(item?.totalCalls || 0) > 0 || Number(item?.activeUserCount || 0) > 0
))

const customAiTrendChartData = computed(() => ({
  labels: customAiTrendDays.value.map(item => formatTrendDateLabel(item.date)),
  datasets: [
    {
      label: '总调用',
      data: customAiTrendDays.value.map(item => Number(item.totalCalls || 0)),
      borderColor: '#d35400',
      backgroundColor: 'rgba(211, 84, 0, 0.12)',
      pointBackgroundColor: '#d35400',
      pointRadius: 3,
      tension: 0.32,
      yAxisID: 'calls'
    },
    {
      label: '活跃用户',
      data: customAiTrendDays.value.map(item => Number(item.activeUserCount || 0)),
      borderColor: '#287c7a',
      backgroundColor: 'rgba(40, 124, 122, 0.12)',
      pointBackgroundColor: '#287c7a',
      pointRadius: 3,
      tension: 0.32,
      yAxisID: 'users'
    }
  ]
}))

const customAiTrendChartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  interaction: {
    mode: 'index',
    intersect: false
  },
  plugins: {
    legend: {
      position: 'bottom',
      labels: {
        boxWidth: 10,
        color: '#7a4c2a',
        usePointStyle: true
      }
    },
    tooltip: {
      callbacks: {
        label: (context) => `${context.dataset.label}: ${context.parsed.y || 0}`
      }
    }
  },
  scales: {
    x: {
      grid: {
        color: 'rgba(217, 196, 170, 0.18)'
      },
      ticks: {
        color: '#8f6f55'
      }
    },
    calls: {
      type: 'linear',
      position: 'left',
      beginAtZero: true,
      ticks: {
        precision: 0,
        color: '#8f6f55'
      },
      grid: {
        color: 'rgba(217, 196, 170, 0.22)'
      }
    },
    users: {
      type: 'linear',
      position: 'right',
      beginAtZero: true,
      ticks: {
        precision: 0,
        color: '#287c7a'
      },
      grid: {
        drawOnChartArea: false
      }
    }
  }
}))

/**
 * 加载 AI 引擎配置列表。
 */
const fetchEngineList = async () => {
  tableLoading.value = true
  try {
    const res = await getAdminAiEngines()
    engineList.value = Array.isArray(res?.data) ? res.data : []
  } catch (error) {
    showAdminError(error?.message || '加载 AI 引擎配置失败')
  } finally {
    tableLoading.value = false
  }
}

/**
 * 加载用户自定义 AI 每日调用上限。
 */
const fetchCustomAiDailyLimit = async () => {
  try {
    const res = await getCustomAiDailyLimit()
    const limit = Number(res?.data?.limit ?? 50)
    customAiDailyLimit.value = limit
    customAiDailyLimitForm.limit = limit
  } catch (error) {
    showAdminError(error?.message || '加载自定义 AI 每日上限失败')
  }
}

/**
 * 加载用户自定义 AI 用量统计。
 * 说明：该统计只展示用户自带 Key 的调用，不混入平台 AI 额度消耗。
 */
const fetchCustomAiUsageStats = async () => {
  const [startDate, endDate] = getNormalizedCustomAiUsageRange()
  customAiUsageRange.value = [startDate, endDate]
  customAiUsageLoading.value = true
  try {
    const res = await getCustomAiUsageStats({
      startDate,
      endDate,
      page: customAiUsagePagination.page,
      pageSize: customAiUsagePagination.pageSize
    })
    customAiUsageStats.value = normalizeCustomAiUsageStats(res?.data || {})
  } catch (error) {
    showAdminError(error?.message || '加载自定义 AI 用量统计失败')
  } finally {
    customAiUsageLoading.value = false
  }
}

const handleCustomAiUsageRangePresetChange = async (value) => {
  const nextPreset = value || customAiUsageRangePreset.value
  customAiUsageRangePreset.value = nextPreset
  if (nextPreset === 'today') {
    customAiUsageRange.value = buildRecentDateRange(1)
  } else if (nextPreset === 'last7') {
    customAiUsageRange.value = buildRecentDateRange(7)
  } else if (nextPreset === 'last30') {
    customAiUsageRange.value = buildRecentDateRange(30)
  } else if (!Array.isArray(customAiUsageRange.value) || customAiUsageRange.value.length !== 2) {
    customAiUsageRange.value = buildRecentDateRange(7)
  }
  // 日期范围变化后重置用户明细页码，避免继续请求旧范围下的页码。
  customAiUsagePagination.page = 1
  await fetchCustomAiUsageStats()
}

const handleCustomAiUsageRangeChange = async (value) => {
  if (!Array.isArray(value) || value.length !== 2 || !value[0] || !value[1]) {
    return
  }
  customAiUsageRangePreset.value = 'custom'
  customAiUsageRange.value = [value[0], value[1]]
  customAiUsagePagination.page = 1
  await fetchCustomAiUsageStats()
}

/**
 * 加载用户自定义 AI 按日趋势。
 * 说明：趋势筛选独立于功能分布范围筛选，避免切换图表范围时影响下方用户明细表格。
 */
const fetchCustomAiUsageTrends = async () => {
  const [startDate, endDate] = getNormalizedCustomAiTrendRange()
  customAiTrendRange.value = [startDate, endDate]
  customAiTrendLoading.value = true
  try {
    const res = await getCustomAiUsageTrends({ startDate, endDate })
    customAiUsageTrends.value = normalizeCustomAiUsageTrends(res?.data || {})
  } catch (error) {
    showAdminError(error?.message || '加载自定义 AI 趋势失败')
  } finally {
    customAiTrendLoading.value = false
  }
}

const handleCustomAiTrendPresetChange = async (value) => {
  const nextPreset = value || customAiTrendPreset.value
  customAiTrendPreset.value = nextPreset
  if (nextPreset === 'last7') {
    customAiTrendRange.value = buildRecentDateRange(7)
  } else if (nextPreset === 'last30') {
    customAiTrendRange.value = buildRecentDateRange(30)
  } else if (!Array.isArray(customAiTrendRange.value) || customAiTrendRange.value.length !== 2) {
    customAiTrendRange.value = buildRecentDateRange(7)
  }
  await fetchCustomAiUsageTrends()
}

const handleCustomAiTrendRangeChange = async (value) => {
  if (!Array.isArray(value) || value.length !== 2 || !value[0] || !value[1]) {
    return
  }
  customAiTrendPreset.value = 'custom'
  customAiTrendRange.value = [value[0], value[1]]
  await fetchCustomAiUsageTrends()
}

/**
 * 保存用户自定义 AI 每日调用上限。
 * 说明：该配置只影响用户自带 Key 的服务器调用次数，不改变平台额度规则。
 */
const handleCustomAiDailyLimitSave = async () => {
  const limit = Number(customAiDailyLimitForm.limit)
  if (!Number.isFinite(limit) || limit < 1 || limit > 10000) {
    showAdminWarning('每日上限必须在 1-10000 之间')
    return
  }
  customAiDailyLimitSaving.value = true
  try {
    const res = await updateCustomAiDailyLimit(limit)
    customAiDailyLimit.value = Number(res?.data?.limit ?? limit)
    customAiDailyLimitForm.limit = customAiDailyLimit.value
    showAdminSuccess('用户自定义 AI 每日上限已更新')
  } catch (error) {
    showAdminError(error?.message || '保存自定义 AI 每日上限失败')
  } finally {
    customAiDailyLimitSaving.value = false
  }
}

const handleCustomAiUsagePageChange = async (page) => {
  customAiUsagePagination.page = Number(page) || 1
  await fetchCustomAiUsageStats()
}

const openCreateDialog = () => {
  isEditMode.value = false
  editOriginalPayload.value = null
  resetFormData()
  dialogVisible.value = true
}

/**
 * 重置筛选条件。
 * 作用：快速恢复全量视图，便于跨业务排查配置。
 */
const resetFilters = () => {
  keyword.value = ''
  businessTypeFilter.value = 'all'
  providerFilter.value = 'all'
  statusFilter.value = 'all'
  activeScopeFilter.value = 'all'
}

/**
 * 统计卡片快捷筛选。
 * 作用：减少多次点选筛选项的操作成本。
 * @param {'all' | 'active' | 'interview-active' | 'resume-active' | 'multi-active-risk'} key
 */
const applyQuickFilter = (key) => {
  resetFilters()
  if (key === 'active') {
    statusFilter.value = 'active'
    return
  }
  if (key === 'interview-active') {
    businessTypeFilter.value = 'interview'
    activeScopeFilter.value = 'current-active'
    return
  }
  if (key === 'resume-active') {
    businessTypeFilter.value = 'resume'
    activeScopeFilter.value = 'current-active'
    return
  }
  if (key === 'multi-active-risk') {
    statusFilter.value = 'active'
    activeScopeFilter.value = 'not-current-active'
  }
}

/**
 * 页码变更处理。
 * @param {number} nextPage
 */
const handlePageChange = (nextPage) => {
  pagination.page = nextPage
}

/**
 * 分页大小变更处理。
 * @param {number} nextPageSize
 */
const handlePageSizeChange = (nextPageSize) => {
  pagination.pageSize = nextPageSize
  pagination.page = 1
}

/**
 * 打开编辑弹窗并回填行数据。
 * 说明：编辑态的 API Key 用空字符串初始化，避免前端显示后端脱敏值。
 * @param {Record<string, any>} row
 */
const openEditDialog = (row) => {
  isEditMode.value = true
  connectivityTestResult.value = null
  modelOptions.value = []
  editOriginalPayload.value = {
    engineCode: row.engineCode || '',
    engineName: row.engineName || '',
    providerType: row.providerType || '',
    businessType: row.businessType || 'interview',
    modelName: row.modelName || '',
    supportsMultimodal: Number(row.supportsMultimodal ?? 0),
    thinkingMode: row.thinkingMode || 'none',
    baseUrl: row.baseUrl || '',
    temperature: Number(row.temperature ?? 1.0),
    maxTokens: Number(row.maxTokens ?? 4096),
    timeoutMs: Number(row.timeoutMs ?? 30000),
    isActive: Number(row.isActive ?? 0),
    sort: Number(row.sort ?? 0),
    remark: row.remark || ''
  }
  formData.id = row.id
  formData.engineCode = row.engineCode || ''
  formData.engineName = row.engineName || ''
  formData.providerType = row.providerType || ''
  formData.businessType = row.businessType || 'interview'
  formData.modelName = row.modelName || ''
  formData.supportsMultimodal = Number(row.supportsMultimodal ?? 0)
  formData.thinkingMode = row.thinkingMode || 'none'
  formData.baseUrl = row.baseUrl || ''
  formData.apiKey = ''
  formData.temperature = Number(row.temperature ?? 1.0)
  formData.maxTokens = Number(row.maxTokens ?? 4096)
  formData.timeoutMs = Number(row.timeoutMs ?? 30000)
  formData.isActive = Number(row.isActive ?? 0)
  formData.sort = Number(row.sort ?? 0)
  formData.remark = row.remark || ''
  dialogVisible.value = true
}

/**
 * 提交新增/编辑表单。
 */
const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const basePayload = {
      engineCode: String(formData.engineCode || '').trim(),
      engineName: String(formData.engineName || '').trim(),
      providerType: String(formData.providerType || '').trim(),
      businessType: String(formData.businessType || '').trim(),
      modelName: String(formData.modelName || '').trim(),
      supportsMultimodal: formData.businessType === 'resume' ? formData.supportsMultimodal : 0,
      thinkingMode: formData.thinkingMode || 'none',
      baseUrl: String(formData.baseUrl || '').trim(),
      temperature: formData.temperature,
      maxTokens: formData.maxTokens,
      timeoutMs: formData.timeoutMs,
      isActive: formData.isActive,
      sort: formData.sort,
      remark: formData.remark ? String(formData.remark).trim() : null
    }

    const duplicateEngineCode = findDuplicateEngineCode()
    if (duplicateEngineCode) {
      await confirmAdminRiskAction({
        title: '编码重复预警',
        actionText: '继续保存重复引擎编码',
        targetName: duplicateEngineCode.engineCode,
        impactHint: '重复编码会增加配置路由歧义与维护成本，请确认这是有意配置。',
        type: 'warning'
      })
    }

    const duplicateBusinessModel = findDuplicateBusinessModel()
    if (duplicateBusinessModel) {
      await confirmAdminRiskAction({
        title: '模型重复预警',
        actionText: '继续保存重复业务模型',
        targetName: duplicateBusinessModel.modelName,
        impactHint: `业务「${duplicateBusinessModel.businessTypeDesc || duplicateBusinessModel.businessType}」已有同模型配置，可能导致维护混淆。`,
        type: 'warning'
      })
    }

    if (isEditMode.value) {
      const payload = {
        id: formData.id,
        ...basePayload
      }
      // 编辑态仅在用户输入新值时才提交 apiKey。
      if (formData.apiKey) {
        payload.apiKey = String(formData.apiKey).trim()
      }
      await updateAdminAiEngine(payload)
      const changedFields = collectChangedFields(editOriginalPayload.value, basePayload)
      showAdminSuccess(
        changedFields.length
          ? `AI 引擎配置修改成功（${changedFields.join('、')}）`
          : 'AI 引擎配置修改成功'
      )
    } else {
      await createAdminAiEngine({
        ...basePayload,
        apiKey: String(formData.apiKey).trim()
      })
      showAdminSuccess('AI 引擎配置新增成功')
    }

    dialogVisible.value = false
    await fetchEngineList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || '保存 AI 引擎配置失败')
    }
  } finally {
    submitLoading.value = false
  }
}

/**
 * 切换配置启用状态。
 * 说明：后端会保证同一 businessType 只有一个启用配置。
 * @param {Record<string, any>} row
 */
const handleToggleActive = async (row) => {
  if (toggleLoadingIds.value.has(row.id)) return

  const nextActive = row.isActive === 1 ? 0 : 1
  const actionText = nextActive === 1 ? '启用' : '禁用'

  // 已经是当前业务生效项时，重复点击"启用"不需要再发请求。
  if (nextActive === 1 && isCurrentActiveEngine(row)) {
    showAdminWarning(`配置「${row.engineName}」已经是${row.businessTypeDesc}当前生效项`)
    return
  }

  const currentActiveEngine = getCurrentActiveEngineByBusiness(row.businessType)
  const confirmLines = [`确认${actionText}配置「${row.engineName}」吗？`]

  if (nextActive === 1 && currentActiveEngine && String(currentActiveEngine.id) !== String(row.id)) {
    confirmLines.push(`该操作会将当前生效配置「${currentActiveEngine.engineName}」自动切换为禁用。`)
  }

  if (nextActive === 0 && isCurrentActiveEngine(row)) {
    confirmLines.push('禁用后该业务将暂时没有生效引擎，请确认已有替代配置。')
  }

  try {
    await confirmAdminRiskAction({
      title: `${actionText}确认`,
      actionText: `${actionText} AI 引擎配置`,
      targetName: row.engineName,
      // 将多行风险提示收敛为一句，保持统一模板同时保留影响范围说明。
      impactHint: confirmLines.slice(1).join('；') || '该操作会影响当前业务的模型路由与线上调用结果。',
      type: 'warning'
    })
    toggleLoadingIds.value.add(row.id)
    await toggleAdminAiEngineActive(row.id, nextActive)
    await fetchEngineList()

    if (nextActive === 1 && currentActiveEngine && String(currentActiveEngine.id) !== String(row.id)) {
      showAdminSuccess(`配置已启用，并替换生效项「${currentActiveEngine.engineName}」`)
      return
    }
    showAdminSuccess(`配置已${actionText}`)
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || `${actionText}配置失败`)
    }
  } finally {
    toggleLoadingIds.value.delete(row.id)
  }
}

/**
 * 处理表格选择变化。
 * @param {Array} selection 选中的行数据
 */
const handleSelectionChange = (selection) => {
  selectedEngines.value = selection
}

/**
 * 全部勾选当前筛选结果。
 */
const handleSelectAll = () => {
  if (engineTableRef.value) {
    engineTableRef.value.toggleAllSelection()
  }
}

/**
 * 删除单条 AI 引擎配置。
 * @param {Object} row 引擎配置行数据
 */
const handleDelete = async (row) => {
  try {
    await confirmAdminRiskAction({
      title: '删除确认',
      actionText: '删除 AI 引擎配置',
      targetName: row.engineName,
      impactHint: '删除后数据无法恢复，请确认是否继续。',
      type: 'error'
    })
    await deleteAiEngine(row.id)
    showAdminSuccess('AI 引擎配置删除成功')
    await fetchEngineList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || '删除 AI 引擎配置失败')
    }
  }
}

/**
 * 批量删除 AI 引擎配置。
 */
const handleBatchDelete = async () => {
  if (selectedEngines.value.length === 0) {
    showAdminWarning('请先选择要删除的引擎配置')
    return
  }

  try {
    await confirmAdminRiskAction({
      title: '批量删除确认',
      actionText: '删除选中的 AI 引擎配置',
      targetName: `${selectedEngines.value.length} 条数据`,
      impactHint: '删除后数据无法恢复，请确认是否继续。',
      type: 'error'
    })
    batchDeleteLoading.value = true
    const ids = selectedEngines.value.map(item => item.id)
    await deleteAiEngines(ids)
    showAdminSuccess(`成功删除 ${ids.length} 条引擎配置`)
    await fetchEngineList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || '批量删除失败')
    }
  } finally {
    batchDeleteLoading.value = false
  }
}

/**
 * 批量禁用 AI 引擎配置。
 */
const handleBatchDisable = async () => {
  if (selectedEngines.value.length === 0) {
    showAdminWarning('请先选择要禁用的引擎配置')
    return
  }

  try {
    await confirmAdminRiskAction({
      title: '批量禁用确认',
      actionText: '禁用选中的 AI 引擎配置',
      targetName: `${selectedEngines.value.length} 条数据`,
      impactHint: '禁用后将影响对应业务的模型路由。',
      type: 'warning'
    })
    const ids = selectedEngines.value.map(item => item.id)
    await toggleAiEnginesBatchActive(ids, 0)
    showAdminSuccess(`成功禁用 ${ids.length} 条引擎配置`)
    await fetchEngineList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || '批量禁用失败')
    }
  }
}

/**
 * 批量启用 AI 引擎配置。
 */
const handleBatchEnable = async () => {
  if (selectedEngines.value.length === 0) {
    showAdminWarning('请先选择要启用的引擎配置')
    return
  }

  try {
    await confirmAdminRiskAction({
      title: '批量启用确认',
      actionText: '启用选中的 AI 引擎配置',
      targetName: `${selectedEngines.value.length} 条数据`,
      impactHint: '启用后将作为对应业务的模型。',
      type: 'warning'
    })
    const ids = selectedEngines.value.map(item => item.id)
    await toggleAiEnginesBatchActive(ids, 1)
    showAdminSuccess(`成功启用 ${ids.length} 条引擎配置`)
    await fetchEngineList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || '批量启用失败')
    }
  }
}

onMounted(() => {
  fetchEngineList()
  fetchCustomAiDailyLimit()
  fetchCustomAiUsageStats()
  fetchCustomAiUsageTrends()
  fetchSystemTtsConfig()
})

// 离开系统 TTS 配置时释放试听音频，避免后台播放和 blob URL 泄漏
onBeforeUnmount(() => {
  releaseSystemTtsPreviewAudio()
})

/**
 * 监听筛选项变化：
 * 每次筛选变化都重置到第一页，避免落到空页。
 */
watch(
  () => [
    keyword.value,
    businessTypeFilter.value,
    providerFilter.value,
    statusFilter.value,
    activeScopeFilter.value
  ],
  () => {
    pagination.page = 1
  }
)

/**
 * 监听筛选结果长度：
 * 当数据减少导致页码越界时自动回退。
 */
watch(
  () => filteredEngineList.value.length,
  (total) => {
    const totalPage = Math.max(1, Math.ceil(total / pagination.pageSize))
    if (pagination.page > totalPage) {
      pagination.page = totalPage
    }
  }
)

/**
 * 表单关键配置变化后清空旧测试结果，避免管理员误读过期状态。
 */
watch(
  () => [
    formData.providerType,
    formData.modelName,
    formData.baseUrl,
    formData.apiKey,
    formData.thinkingMode,
    formData.temperature,
    formData.maxTokens,
    formData.timeoutMs
  ],
  () => {
    if (connectivityTestResult.value) {
      connectivityTestResult.value = null
    }
  }
)

watch(
  () => [
    formData.providerType,
    formData.baseUrl,
    formData.apiKey
  ],
  () => {
    modelOptions.value = []
  }
)
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: linear-gradient(135deg, #fffcf8 0%, #fff8f3 100%);
  border: 1px solid rgba(230, 126, 34, 0.1);
  border-radius: 14px;
  padding: 18px 22px;
  box-shadow: 0 4px 16px rgba(143, 69, 27, 0.06);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  background: linear-gradient(135deg, #8f451b 0%, #d35400 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #a08060;
}

.page-subtitle.subtitle-tip {
  color: #b58a5e;
  font-size: 12px;
  line-height: 1.6;
}

.btn-primary {
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%);
  border: none;
  border-radius: 12px;
  padding: 12px 24px;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 6px 20px rgba(230, 126, 34, 0.3);
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 28px rgba(230, 126, 34, 0.4);
}

.refresh-btn {
  border-radius: 10px;
  border-color: rgba(230, 126, 34, 0.25);
  color: #8f451b;
}

.refresh-btn:hover {
  border-color: #e67e22;
  color: #d35400;
  background: rgba(255, 140, 66, 0.06);
}

.admin-section-switch {
  display: inline-flex;
  align-self: flex-start;
  gap: 4px;
  padding: 4px;
  border: 1px solid rgba(217, 196, 170, 0.42);
  border-radius: 12px;
  background: rgba(255, 252, 248, 0.86);
}

.admin-section-tab {
  min-height: 34px;
  padding: 7px 16px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: #8f6f55;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.admin-section-tab.active {
  background: #fff;
  color: #d35400;
  box-shadow: 0 3px 10px rgba(143, 69, 27, 0.08);
}

.admin-section-tab:hover {
  color: #d35400;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.stats-card {
  border: 1px solid rgba(217, 196, 170, 0.25);
  border-radius: 14px;
  background: var(--bg-card);
  padding: 16px;
  text-align: left;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 4px 16px rgba(143, 69, 27, 0.05);
}

.stats-card:hover {
  border-color: rgba(230, 126, 34, 0.4);
  box-shadow: 0 8px 24px rgba(143, 69, 27, 0.12);
  transform: translateY(-3px);
}

.stats-card.active {
  border-color: #e67e22;
  background: linear-gradient(135deg, #fffcf8 0%, #fff8f3 100%);
  box-shadow: 0 6px 20px rgba(230, 126, 34, 0.15);
}

.stats-card.warning:not(.active) {
  border-color: rgba(245, 158, 66, 0.4);
}

.stats-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  color: #a08060;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stats-value {
  font-size: 26px;
  color: #5a4030;
  line-height: 1;
  font-weight: 700;
}

.custom-ai-limit-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 20px;
}

.custom-ai-limit-copy span,
.custom-ai-limit-copy strong,
.custom-ai-limit-copy small {
  display: block;
}

.custom-ai-limit-copy span {
  color: #a08060;
  font-size: 13px;
}

.custom-ai-limit-copy strong {
  margin-top: 4px;
  color: #5a4030;
  font-size: 26px;
  line-height: 1.1;
}

.custom-ai-limit-copy small {
  margin-top: 6px;
  color: #8f6f55;
  font-size: 12px;
  line-height: 1.5;
}

.custom-ai-limit-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.custom-ai-usage-card :deep(.el-card__body) {
  padding: 18px 20px;
}

.custom-ai-usage-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.custom-ai-usage-header span,
.custom-ai-usage-header strong {
  display: block;
}

.custom-ai-usage-header span {
  color: #a08060;
  font-size: 13px;
}

.custom-ai-usage-header strong {
  margin-top: 4px;
  color: #5a4030;
  font-size: 20px;
  line-height: 1.2;
}

.custom-ai-usage-controls {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
  flex-shrink: 0;
}

.custom-ai-usage-controls :deep(.el-date-editor) {
  min-width: 260px;
}

.custom-ai-usage-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.custom-ai-usage-summary > div {
  min-height: 72px;
  padding: 12px 14px;
  border: 1px solid rgba(217, 196, 170, 0.35);
  border-radius: 10px;
  background: rgba(255, 252, 248, 0.72);
}

.custom-ai-usage-summary span {
  display: block;
  color: #8f6f55;
  font-size: 12px;
}

.custom-ai-usage-summary strong {
  display: block;
  margin-top: 6px;
  color: #5a4030;
  font-size: 24px;
  line-height: 1;
}

.custom-ai-trend-section {
  margin: 0 0 14px;
  padding: 12px 0;
  border-top: 1px solid rgba(217, 196, 170, 0.38);
  border-bottom: 1px solid rgba(217, 196, 170, 0.38);
}

.custom-ai-trend-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.custom-ai-trend-copy {
  min-width: 180px;
}

.custom-ai-trend-header small {
  display: block;
  color: #a08060;
  font-size: 12px;
  line-height: 1.4;
}

.custom-ai-trend-summary {
  display: flex;
  justify-content: flex-end;
  flex: 1;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 180px;
}

.custom-ai-trend-summary span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 28px;
  padding: 4px 9px;
  border-radius: 8px;
  background: rgba(230, 126, 34, 0.08);
  color: #8f6f55;
  font-size: 12px;
  line-height: 1.2;
  white-space: nowrap;
}

.custom-ai-trend-summary strong {
  color: #5a4030;
  font-size: 14px;
  line-height: 1;
}

.custom-ai-trend-toggle {
  flex: 0 0 auto;
  min-height: 32px;
  padding: 6px 12px;
  border: 1px solid rgba(217, 196, 170, 0.58);
  border-radius: 8px;
  background: #fffaf4;
  color: #7a4c2a;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s ease, color 0.2s ease, background 0.2s ease;
}

.custom-ai-trend-toggle:hover {
  border-color: rgba(230, 126, 34, 0.72);
  background: rgba(230, 126, 34, 0.08);
  color: #d35400;
}

.custom-ai-trend-panel {
  margin-top: 12px;
}

/* 旧版趋势图默认占用整块首屏；当前仅在展开面板中保留图表高度。 */
.custom-ai-trend-controls {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.custom-ai-trend-chart {
  position: relative;
  min-height: 220px;
}

.custom-ai-trend-chart :deep(canvas) {
  max-height: 220px;
}

.custom-ai-trend-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  border: 1px dashed rgba(217, 196, 170, 0.48);
  border-radius: 8px;
  color: #a08060;
  font-size: 13px;
}

.custom-ai-usage-section {
  margin-bottom: 14px;
}

.custom-ai-usage-section-title {
  display: block;
  margin-bottom: 8px;
  color: #8f6f55;
  font-size: 12px;
  font-weight: 700;
}

.custom-ai-type-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 26px;
  align-items: center;
}

.custom-ai-type-list.compact {
  min-height: 0;
}

.custom-ai-type-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 26px;
  padding: 4px 9px;
  border-radius: 8px;
  background: rgba(230, 126, 34, 0.08);
  color: #7a4c2a;
  font-size: 12px;
  line-height: 1.2;
  white-space: nowrap;
}

.custom-ai-type-chip strong {
  color: #5a4030;
  font-weight: 700;
}

.custom-ai-empty-text {
  color: #a08060;
  font-size: 12px;
}

.custom-ai-usage-table {
  width: 100%;
}

.custom-ai-user-cell strong,
.custom-ai-user-cell span {
  display: block;
}

.custom-ai-user-cell strong {
  color: #5a4030;
  font-size: 13px;
  line-height: 1.3;
}

.custom-ai-user-cell span {
  margin-top: 2px;
  color: #a08060;
  font-size: 12px;
}

.custom-ai-usage-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.system-tts-card :deep(.el-card__body) {
  padding: 18px 20px;
}

.system-tts-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.system-tts-header span,
.system-tts-header strong,
.system-tts-header small {
  display: block;
}

.system-tts-header span {
  color: #a08060;
  font-size: 13px;
}

.system-tts-header strong {
  margin-top: 4px;
  color: #5a4030;
  font-size: 20px;
  line-height: 1.2;
}

.system-tts-header small {
  margin-top: 6px;
  color: #8f6f55;
  font-size: 12px;
  line-height: 1.5;
}

.system-tts-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px 14px;
}

.system-tts-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.system-tts-form :deep(.el-select) {
  width: 100%;
}

.system-tts-result {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 10px 14px;
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: 10px;
  background: rgba(16, 185, 129, 0.08);
  color: #047857;
  font-size: 13px;
}

.system-tts-result.failed {
  border-color: rgba(239, 68, 68, 0.2);
  background: rgba(239, 68, 68, 0.08);
  color: #b91c1c;
}

.system-tts-result small {
  margin-left: auto;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  opacity: 0.78;
}

.system-tts-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(230, 126, 34, 0.08);
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(143, 69, 27, 0.05);
}

.filter-item {
  width: 180px;
}

.filter-item :deep(.el-input__wrapper),
.filter-item :deep(.el-select__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(217, 196, 170, 0.4);
}

.filter-item :deep(.el-input__wrapper:hover),
.filter-item :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(230, 126, 34, 0.3);
}

.filter-item :deep(.el-input__wrapper.is-focus),
.filter-item :deep(.el-select__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(230, 126, 34, 0.15), 0 0 0 1px #e67e22;
}

.filter-item.keyword {
  width: 320px;
}

.reset-btn {
  border-radius: 10px;
}

.filter-result {
  margin-top: 0;
  font-size: 13px;
  color: #a08060;
  font-weight: 500;
}

.result-count {
  color: #d35400;
  font-weight: 600;
}

.risk-alert {
  margin-top: -6px;
}

.field-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #8e9aa6;
  line-height: 1.4;
}

.base-url-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.model-fetch-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.connectivity-test-panel {
  margin: -4px 0 18px 100px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.connectivity-test-result {
  width: 100%;
}

.connectivity-test-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  line-height: 1.5;
}

.filter-icon {
  color: var(--text-muted);
  font-size: 16px;
}

.table-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.engine-table :deep(.el-table__header-wrapper) {
  background: var(--bg-elevated);
}

.table-header {
  font-weight: 600;
  color: #2c3e50;
}

.engine-table :deep(.el-table__body tr:nth-child(even)) {
  background: var(--bg-elevated);
}

.engine-table :deep(.el-table__body tr:hover > td) {
  background: #fff5e6;
}

.status-tag {
  border-radius: 4px;
  font-weight: 500;
}

.business-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  color: #e67e22;
  font-size: 16px;
}

.action-btn:hover {
  color: #d35400;
  background: #fff5e6;
  border-radius: 6px;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.engine-dialog :deep(.el-dialog__header) {
  background: var(--bg-elevated);
  border-bottom: 1px solid var(--border-divider);
}

.engine-dialog :deep(.el-dialog__title) {
  font-weight: 600;
  color: #2c3e50;
}

.engine-dialog :deep(.el-form-item__label) {
  font-weight: 500;
  color: #34495e;
}

.dialog-btn {
  border-radius: 8px;
  padding: 10px 24px;
  font-weight: 500;
}

.dialog-btn.primary {
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%);
  border: none;
}

.dialog-btn.primary:hover {
  background: linear-gradient(135deg, #d35400 0%, #c0392b 100%);
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  .admin-section-switch {
    align-self: stretch;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .admin-section-tab {
    width: 100%;
  }

  .filter-item,
  .filter-item.keyword {
    width: 100%;
  }

  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .custom-ai-limit-card :deep(.el-card__body) {
    flex-direction: column;
    align-items: stretch;
  }

  .custom-ai-limit-controls {
    width: 100%;
    flex-wrap: wrap;
  }

  .custom-ai-usage-header,
  .custom-ai-usage-controls {
    align-items: stretch;
    flex-direction: column;
  }

  .custom-ai-usage-controls :deep(.el-date-editor) {
    width: 100%;
  }

  .custom-ai-trend-header,
  .custom-ai-trend-controls {
    align-items: stretch;
    flex-direction: column;
  }

  .custom-ai-trend-summary {
    justify-content: flex-start;
    width: 100%;
  }

  .custom-ai-trend-toggle {
    width: 100%;
  }

  .custom-ai-usage-summary {
    grid-template-columns: 1fr;
  }

  .system-tts-header,
  .system-tts-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .system-tts-grid {
    grid-template-columns: 1fr;
  }

  .custom-ai-usage-footer {
    justify-content: center;
  }

  .pagination-wrap {
    justify-content: center;
  }

  .base-url-row,
  .model-fetch-row {
    grid-template-columns: 1fr;
  }
}
</style>
