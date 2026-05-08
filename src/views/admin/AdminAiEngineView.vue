<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">AI 引擎配置</h2>
        <p class="page-subtitle">维护 interview/resume 业务模型配置，支持启用切换和密钥脱敏展示</p>
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

    <div class="stats-grid">
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

    <div class="filter-bar">
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

    <div class="filter-result">
      当前筛选结果：<span class="result-count">{{ filteredEngineList.length }}</span> / {{ engineList.length }} 条
    </div>
    <el-alert
      v-if="hasMultiActiveRisk"
      type="warning"
      :closable="false"
      class="risk-alert"
      title="检测到同业务存在多条启用配置，请尽快检查并完成收敛，避免运行时路由不确定。"
    />

    <el-card shadow="never" class="table-card">
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
                :loading="toggleLoadingId === row.id"
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
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="模型名" prop="modelName">
          <el-input v-model.trim="formData.modelName" :disabled="submitLoading" />
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Edit, Search } from '@element-plus/icons-vue'
import {
  createAdminAiEngine,
  deleteAiEngine,
  deleteAiEngines,
  getAdminAiEngines,
  toggleAdminAiEngineActive,
  toggleAiEnginesBatchActive,
  updateAdminAiEngine
} from '@/api/admin/aiEngines'
import {
  confirmAdminRiskAction,
  resolveAdminTableEmptyText,
  showAdminError,
  showAdminSuccess,
  showAdminWarning
} from '@/utils/adminFeedback'

// 表格数据：AI 引擎配置列表。
const engineList = ref([])
const tableLoading = ref(false)
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

// 弹窗编辑状态：复用一个表单完成新增和编辑。
const dialogVisible = ref(false)
const isEditMode = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const toggleLoadingId = ref(null)
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
  return Number(activeEngineIdByBusiness.value[businessType]) === Number(row?.id)
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
  return engineList.value.find((item) => Number(item.id) === Number(activeId)) || null
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
    if (isEditMode.value && Number(item.id) === Number(formData.id)) return false
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
    if (isEditMode.value && Number(item.id) === Number(formData.id)) return false
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
  if (toggleLoadingId.value) return

  const nextActive = row.isActive === 1 ? 0 : 1
  const actionText = nextActive === 1 ? '启用' : '禁用'

  // 已经是当前业务生效项时，重复点击"启用"不需要再发请求。
  if (nextActive === 1 && isCurrentActiveEngine(row)) {
    showAdminWarning(`配置「${row.engineName}」已经是${row.businessTypeDesc}当前生效项`)
    return
  }

  const currentActiveEngine = getCurrentActiveEngineByBusiness(row.businessType)
  const confirmLines = [`确认${actionText}配置「${row.engineName}」吗？`]

  if (nextActive === 1 && currentActiveEngine && Number(currentActiveEngine.id) !== Number(row.id)) {
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
    toggleLoadingId.value = row.id
    await toggleAdminAiEngineActive(row.id, nextActive)
    await fetchEngineList()

    if (nextActive === 1 && currentActiveEngine && Number(currentActiveEngine.id) !== Number(row.id)) {
      showAdminSuccess(`配置已启用，并替换生效项「${currentActiveEngine.engineName}」`)
      return
    }
    showAdminSuccess(`配置已${actionText}`)
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || `${actionText}配置失败`)
    }
  } finally {
    toggleLoadingId.value = null
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

  .filter-item,
  .filter-item.keyword {
    width: 100%;
  }

  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .pagination-wrap {
    justify-content: center;
  }

  .base-url-row {
    grid-template-columns: 1fr;
  }
}
</style>
