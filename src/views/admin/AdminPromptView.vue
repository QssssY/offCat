<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">Prompt 管理</h2>
        <p class="page-subtitle">维护面试/简历 Prompt 模板，并与岗位配置联动</p>
      </div>
      <div class="header-actions">
        <el-button :loading="tableLoading" class="refresh-btn" @click="fetchPromptList">
          刷新列表
        </el-button>
        <el-button
          v-if="selectedPrompts.length > 0"
          type="danger"
          :loading="batchDeleteLoading"
          @click="handleBatchDelete"
        >
          批量删除 ({{ selectedPrompts.length }})
        </el-button>
        <el-button
          v-if="selectedPrompts.length > 0"
          type="warning"
          @click="handleBatchDisable"
        >
          批量禁用
        </el-button>
        <el-button
          v-if="selectedPrompts.length > 0"
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
          新增 Prompt
        </el-button>
      </div>
    </div>

    <div class="stats-grid">
      <button
        class="stats-card"
        :class="{ active: matchedQuickFilterKey === 'all' }"
        @click="applyQuickFilter('all')"
      >
        <span class="stats-label">全部模板</span>
        <strong class="stats-value">{{ promptStats.total }}</strong>
      </button>
      <button
        class="stats-card"
        :class="{ active: matchedQuickFilterKey === 'active' }"
        @click="applyQuickFilter('active')"
      >
        <span class="stats-label">启用模板</span>
        <strong class="stats-value">{{ promptStats.active }}</strong>
      </button>
      <button
        class="stats-card"
        :class="{ active: matchedQuickFilterKey === 'interview' }"
        @click="applyQuickFilter('interview')"
      >
        <span class="stats-label">面试场景</span>
        <strong class="stats-value">{{ promptStats.interview }}</strong>
      </button>
      <button
        class="stats-card"
        :class="{ active: matchedQuickFilterKey === 'resume' }"
        @click="applyQuickFilter('resume')"
      >
        <span class="stats-label">简历场景</span>
        <strong class="stats-value">{{ promptStats.resume }}</strong>
      </button>
      <button
        class="stats-card warning"
        :class="{ active: matchedQuickFilterKey === 'linkage-abnormal' }"
        @click="applyQuickFilter('linkage-abnormal')"
      >
        <span class="stats-label">岗位联动异常</span>
        <strong class="stats-value">{{ promptStats.linkageAbnormal }}</strong>
      </button>
    </div>

    <div class="filter-bar">
      <!-- Prompt 列表本地筛选：降低配置量上来后的查找成本 -->
      <el-input
        v-model.trim="keyword"
        class="filter-item keyword"
        placeholder="按岗位编码/名称/内容搜索"
        clearable
        :prefix-icon="Search"
      />
      <el-select v-model="scenarioFilter" class="filter-item" placeholder="按场景筛选" clearable>
        <el-option label="全部场景" value="all" />
        <el-option label="面试系统设定" :value="1" />
        <el-option label="简历诊断设定" :value="2" />
      </el-select>
      <el-select v-model="jobRoleFilter" class="filter-item" placeholder="按岗位筛选" clearable>
        <el-option label="全部岗位" value="all" />
        <el-option
          v-for="role in jobRoleOptions"
          :key="role.id"
          :label="`${role.roleName}（${role.roleCode}）`"
          :value="role.roleCode"
        />
      </el-select>
      <el-select v-model="difficultyFilter" class="filter-item" placeholder="按难度筛选" clearable>
        <el-option label="全部难度" value="all" />
        <el-option :value="1" label="初级" />
        <el-option :value="2" label="中级" />
        <el-option :value="3" label="高级" />
      </el-select>
      <el-select v-model="statusFilter" class="filter-item" placeholder="按状态筛选" clearable>
        <el-option label="全部状态" value="all" />
        <el-option label="仅启用" value="active" />
        <el-option label="仅禁用" value="inactive" />
      </el-select>
      <el-select v-model="linkageFilter" class="filter-item" placeholder="按联动状态筛选" clearable>
        <el-option label="全部联动状态" value="all" />
        <el-option label="岗位联动异常（禁用或不存在）" value="abnormal" />
        <el-option label="岗位联动正常" value="linked-active" />
        <el-option label="岗位已禁用" value="linked-inactive" />
        <el-option label="岗位不存在" value="unlinked" />
      </el-select>
      <el-button class="reset-btn" @click="resetFilters">重置筛选</el-button>
    </div>

    <div class="filter-result">
      当前筛选结果：<span class="result-count">{{ filteredPromptList.length }}</span> / {{ promptList.length }} 条
    </div>

    <el-card shadow="never" class="table-card">
      <el-table ref="promptTableRef"
        :data="pagedPromptList"
        v-loading="tableLoading"
        border
        stripe
        :empty-text="tableEmptyText"
        class="prompt-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" align="center">
          <template #header>
            <div class="table-header">ID</div>
          </template>
        </el-table-column>
        <el-table-column prop="scenarioTypeDesc" label="场景类型" min-width="140" align="center">
          <template #header>
            <div class="table-header">场景类型</div>
          </template>
        </el-table-column>
        <el-table-column prop="jobRoleCode" label="岗位编码" min-width="140" align="center">
          <template #header>
            <div class="table-header">岗位编码</div>
          </template>
        </el-table-column>
        <el-table-column prop="jobRoleName" label="岗位名称" min-width="160" align="center">
          <template #header>
            <div class="table-header">岗位名称</div>
          </template>
          <template #default="{ row }">
            <div class="role-cell">
              <span>{{ row.jobRoleName || '-' }}</span>
              <el-tag
                v-if="resolveJobRoleLinkageStatus(row.jobRoleCode) === 'linked-inactive'"
                type="warning"
                effect="plain"
                size="small"
              >
                岗位已禁用
              </el-tag>
              <el-tag
                v-if="resolveJobRoleLinkageStatus(row.jobRoleCode) === 'unlinked'"
                type="danger"
                effect="plain"
                size="small"
              >
                岗位不存在
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="difficultyDesc" label="难度" width="100" align="center">
          <template #header>
            <div class="table-header">难度</div>
          </template>
        </el-table-column>
        <el-table-column label="Prompt 内容" min-width="320">
          <template #header>
            <div class="table-header">Prompt 内容</div>
          </template>
          <template #default="{ row }">
            <el-tooltip placement="top" :content="row.promptContent" class="content-tooltip">
              <div class="prompt-preview">{{ row.promptContent }}</div>
            </el-tooltip>
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
              <el-button
                size="small"
                @click="openEditDialog(row)"
                class="action-btn"
              >
                编辑
              </el-button>
              <el-button
                size="small"
                @click="handleToggleActive(row)"
                class="action-btn"
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
          :total="filteredPromptList.length"
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
      :title="isEditMode ? '编辑 Prompt' : '新增 Prompt'"
      width="760px"
      destroy-on-close
      class="prompt-dialog"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="场景类型" prop="scenarioType">
          <el-select v-model="formData.scenarioType" style="width: 100%" :disabled="submitLoading" clearable>
            <el-option :value="1" label="面试系统设定" />
            <el-option :value="2" label="简历诊断设定" />
          </el-select>
        </el-form-item>

        <el-form-item label="岗位" prop="jobRoleCode">
          <div class="job-role-select-header">
            <span class="field-tip">岗位列表默认仅显示启用项，避免误绑到已下线岗位</span>
            <el-switch
              v-model="onlyActiveJobRoleOption"
              inline-prompt
              active-text="仅启用"
              inactive-text="全部"
              :disabled="submitLoading"
            />
          </div>
          <el-select
            v-model="formData.jobRoleCode"
            filterable
            style="width: 100%"
            placeholder="请选择岗位配置"
            :disabled="submitLoading"
            clearable
          >
            <el-option
              v-for="role in selectableJobRoleOptions"
              :key="`${role.id || 'virtual'}-${role.roleCode}`"
              :label="buildJobRoleOptionLabel(role)"
              :value="role.roleCode"
            />
          </el-select>
          <div class="field-tip" :class="{ warning: selectedJobRoleLinkageStatus !== 'linked-active' }">
            当前岗位联动状态：{{ selectedJobRoleLinkageText }}
          </div>
        </el-form-item>

        <el-form-item label="难度" prop="difficulty">
          <el-select v-model="formData.difficulty" style="width: 100%" :disabled="submitLoading" clearable>
            <el-option :value="1" label="初级" />
            <el-option :value="2" label="中级" />
            <el-option :value="3" label="高级" />
          </el-select>
        </el-form-item>

        <el-form-item label="Prompt" prop="promptContent">
          <el-input
            v-model="formData.promptContent"
            type="textarea"
            :rows="10"
            placeholder="请输入 Prompt 模板内容"
            :disabled="submitLoading"
            class="prompt-input"
          />
          <div class="prompt-meta">
            <span class="field-tip">
              已输入 {{ promptContentLength }} 字，建议不少于 20 字以保证上下文完整
            </span>
            <el-button
              text
              type="primary"
              :disabled="submitLoading"
              @click="fillPromptTemplateByScenario"
            >
              一键填充场景模板
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false" class="dialog-btn">取消</el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="submitForm"
          class="dialog-btn primary"
        >
          {{ isEditMode ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Edit, Search } from '@element-plus/icons-vue'
import { getAdminJobRoles } from '@/api/admin/jobRoles'
import {
  createAdminPrompt,
  deletePrompt,
  deletePrompts,
  getAdminPrompts,
  toggleAdminPromptActive,
  togglePromptsBatchActive,
  updateAdminPrompt
} from '@/api/admin/prompts'
import {
  confirmAdminRiskAction,
  resolveAdminTableEmptyText,
  showAdminError,
  showAdminSuccess
} from '@/utils/adminFeedback'

// 列表数据：Prompt 模板主表格。
const promptList = ref([])
const tableLoading = ref(false)
// 表格实例：用于全选操作
const promptTableRef = ref(null)
// 批量选择状态：用于批量删除操作
const selectedPrompts = ref([])
// 批量删除加载状态
const batchDeleteLoading = ref(false)
const keyword = ref('')
const scenarioFilter = ref('all')
const jobRoleFilter = ref('all')
const difficultyFilter = ref('all')
const statusFilter = ref('all')
const linkageFilter = ref('all')
const pagination = reactive({
  page: 1,
  pageSize: 10
})

// 岗位选项：用于创建/编辑 Prompt 时做合法岗位选择。
const jobRoleOptions = ref([])

/**
 * 岗位索引映射。
 * 作用：以岗位编码快速判断 Prompt 与岗位配置是否仍然有效联动。
 */
const jobRoleMetaMap = computed(() => {
  return jobRoleOptions.value.reduce((map, roleItem) => {
    const roleCode = String(roleItem.roleCode || '')
    if (!roleCode) return map
    map[roleCode] = {
      roleName: roleItem.roleName || '',
      isActive: Number(roleItem.isActive) === 1
    }
    return map
  }, {})
})

// 弹窗提交状态：控制新增/编辑流程。
const dialogVisible = ref(false)
const isEditMode = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const onlyActiveJobRoleOption = ref(true)
const editOriginalPayload = ref(null)

// 表单字段：严格对应后端 Prompt DTO 字段。
const formData = reactive({
  id: null,
  scenarioType: 1,
  jobRoleCode: '',
  difficulty: 1,
  promptContent: ''
})

/**
 * 校验 Prompt 内容质量。
 * 作用：避免仅填极短文本导致线上模板可用性不足。
 * @param {any} _rule
 * @param {string} value
 * @param {(error?: Error) => void} callback
 */
const validatePromptContent = (_rule, value, callback) => {
  const text = String(value || '').trim()
  if (!text) {
    callback(new Error('请输入 Prompt 内容'))
    return
  }
  if (text.length < 20) {
    callback(new Error('Prompt 内容至少需要 20 个字符'))
    return
  }
  callback()
}

const formRules = {
  scenarioType: [{ required: true, message: '请选择场景类型', trigger: 'change' }],
  jobRoleCode: [{ required: true, message: '请选择岗位', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  promptContent: [{ validator: validatePromptContent, trigger: 'blur' }]
}

/**
 * Prompt 列表筛选结果。
 * 作用：按场景、状态和关键词进行本地过滤，提升配置管理效率。
 */
const filteredPromptList = computed(() => {
  return promptList.value.filter((item) => {
    const matchesKeyword = !keyword.value
      || item.jobRoleCode?.includes(keyword.value)
      || item.jobRoleName?.includes(keyword.value)
      || item.promptContent?.includes(keyword.value)

    const matchesScenario = scenarioFilter.value === 'all'
      || Number(item.scenarioType) === Number(scenarioFilter.value)

    const matchesJobRole = jobRoleFilter.value === 'all'
      || String(item.jobRoleCode) === String(jobRoleFilter.value)

    const matchesDifficulty = difficultyFilter.value === 'all'
      || Number(item.difficulty) === Number(difficultyFilter.value)

    const matchesStatus = statusFilter.value === 'all'
      || (statusFilter.value === 'active' && item.isActive === 1)
      || (statusFilter.value === 'inactive' && item.isActive !== 1)

    const linkageStatus = resolveJobRoleLinkageStatus(item.jobRoleCode)
    const matchesLinkage = linkageFilter.value === 'all'
      || (linkageFilter.value === 'abnormal' && linkageStatus !== 'linked-active')
      || linkageStatus === linkageFilter.value

    return matchesKeyword
      && matchesScenario
      && matchesJobRole
      && matchesDifficulty
      && matchesStatus
      && matchesLinkage
  })
})

/**
 * 当前分页数据。
 * 作用：在不改后端接口的前提下提升大列表浏览效率。
 */
const pagedPromptList = computed(() => {
  const startIndex = (pagination.page - 1) * pagination.pageSize
  const endIndex = startIndex + pagination.pageSize
  return filteredPromptList.value.slice(startIndex, endIndex)
})

/**
 * 表格空状态文案。
 * 作用：统一“无数据”和“筛选无结果”表达，避免用户误判接口异常。
 */
const tableEmptyText = computed(() => {
  return resolveAdminTableEmptyText(promptList.value.length, filteredPromptList.value.length)
})

/**
 * Prompt 列表统计结果。
 * 作用：在页面顶部展示关键分布，并驱动快捷筛选入口。
 */
const promptStats = computed(() => {
  return promptList.value.reduce(
    (summary, item) => {
      summary.total += 1
      if (item.isActive === 1) {
        summary.active += 1
      } else {
        summary.inactive += 1
      }
      if (Number(item.scenarioType) === 1) summary.interview += 1
      if (Number(item.scenarioType) === 2) summary.resume += 1
      if (resolveJobRoleLinkageStatus(item.jobRoleCode) !== 'linked-active') {
        summary.linkageAbnormal += 1
      }
      return summary
    },
    { total: 0, active: 0, inactive: 0, interview: 0, resume: 0, linkageAbnormal: 0 }
  )
})

/**
 * 当前快捷筛选匹配状态。
 * 作用：让统计卡片高亮与筛选条件保持一致，避免视觉误导。
 */
const matchedQuickFilterKey = computed(() => {
  if (scenarioFilter.value === 'all' && statusFilter.value === 'all' && linkageFilter.value === 'all') {
    return 'all'
  }
  if (scenarioFilter.value === 'all' && statusFilter.value === 'active' && linkageFilter.value === 'all') {
    return 'active'
  }
  if (scenarioFilter.value === 1 && statusFilter.value === 'all' && linkageFilter.value === 'all') {
    return 'interview'
  }
  if (scenarioFilter.value === 2 && statusFilter.value === 'all' && linkageFilter.value === 'all') {
    return 'resume'
  }
  if (scenarioFilter.value === 'all' && statusFilter.value === 'all' && linkageFilter.value === 'abnormal') {
    return 'linkage-abnormal'
  }
  return 'custom'
})

/**
 * Prompt 内容长度。
 * 作用：实时给出输入反馈，帮助管理员控制模板完整度。
 */
const promptContentLength = computed(() => String(formData.promptContent || '').trim().length)

/**
 * 表单岗位候选列表。
 * 作用：
 * 1. 默认仅展示启用岗位，降低误选风险。
 * 2. 编辑历史数据时保留当前已选岗位，避免原值丢失。
 */
const selectableJobRoleOptions = computed(() => {
  const selectedCode = String(formData.jobRoleCode || '')
  const baseOptions = onlyActiveJobRoleOption.value
    ? jobRoleOptions.value.filter((item) => Number(item.isActive) === 1)
    : jobRoleOptions.value

  const hasSelectedInOptions = baseOptions.some((item) => String(item.roleCode) === selectedCode)
  if (!selectedCode || hasSelectedInOptions) {
    return baseOptions
  }

  const selectedMeta = jobRoleMetaMap.value[selectedCode]
  if (selectedMeta) {
    return [
      ...baseOptions,
      {
        id: `selected-${selectedCode}`,
        roleCode: selectedCode,
        roleName: selectedMeta.roleName,
        isActive: selectedMeta.isActive ? 1 : 0
      }
    ]
  }

  // 兼容历史脏数据：岗位编码已不存在时，补一个只读语义选项防止编辑时值丢失。
  return [
    ...baseOptions,
    {
      id: `missing-${selectedCode}`,
      roleCode: selectedCode,
      roleName: '历史岗位（已删除）',
      isActive: 0,
      isVirtualMissing: true
    }
  ]
})

/**
 * 当前表单岗位联动状态描述。
 * 作用：在编辑区显式提示当前岗位是否可用，减少误保存风险。
 */
const selectedJobRoleLinkageStatus = computed(() => resolveJobRoleLinkageStatus(formData.jobRoleCode))
const selectedJobRoleLinkageText = computed(() => {
  if (selectedJobRoleLinkageStatus.value === 'linked-active') return '岗位联动正常'
  if (selectedJobRoleLinkageStatus.value === 'linked-inactive') return '岗位已禁用（可编辑历史模板）'
  return '岗位不存在（建议切换到有效岗位）'
})

/**
 * 重置表单，避免新增/编辑状态互相污染。
 */
const resetFormData = () => {
  formData.id = null
  formData.scenarioType = 1
  formData.jobRoleCode = ''
  formData.difficulty = 1
  formData.promptContent = ''
}

/**
 * 加载 Prompt 列表。
 */
const fetchPromptList = async () => {
  tableLoading.value = true
  try {
    const res = await getAdminPrompts()
    promptList.value = Array.isArray(res?.data) ? res.data : []
  } catch (error) {
    showAdminError(error?.message || '加载 Prompt 列表失败')
  } finally {
    tableLoading.value = false
  }
}

/**
 * 加载岗位配置选项。
 * 作用：确保 Prompt 的岗位字段必须来源于岗位配置表。
 */
const fetchJobRoleOptions = async () => {
  try {
    const res = await getAdminJobRoles()
    jobRoleOptions.value = Array.isArray(res?.data) ? res.data : []
  } catch (error) {
    showAdminError(error?.message || '加载岗位选项失败')
  }
}

/**
 * 构建岗位下拉展示文案。
 * 作用：将“启用/禁用/缺失”状态直接体现在选项文案上，提高可读性。
 * @param {Record<string, any>} role
 * @returns {string}
 */
const buildJobRoleOptionLabel = (role) => {
  const roleName = role?.roleName || '-'
  const roleCode = role?.roleCode || '-'
  if (role?.isVirtualMissing) {
    return `${roleName}（${roleCode}，已删除）`
  }
  if (Number(role?.isActive) !== 1) {
    return `${roleName}（${roleCode}，已禁用）`
  }
  return `${roleName}（${roleCode}）`
}

/**
 * 生成场景模板骨架。
 * 作用：提供可编辑的基础模板，降低管理员从零撰写成本。
 * @returns {string}
 */
const buildScenarioTemplate = () => {
  if (Number(formData.scenarioType) === 1) {
    return [
      '你是一名专业技术面试官，请围绕岗位核心能力进行提问。',
      `面向岗位：${formData.jobRoleCode || '请先选择岗位'}`,
      `目标难度：${formData.difficulty === 3 ? '高级' : (formData.difficulty === 2 ? '中级' : '初级')}`,
      '输出要求：',
      '1. 问题聚焦真实业务场景；',
      '2. 追问候选人的技术细节与权衡思路；',
      '3. 回答后给出简短评价并进入下一问。'
    ].join('\n')
  }

  return [
    '你是一名资深简历诊断顾问，请从岗位匹配度角度输出建议。',
    `面向岗位：${formData.jobRoleCode || '请先选择岗位'}`,
    `目标难度：${formData.difficulty === 3 ? '高级' : (formData.difficulty === 2 ? '中级' : '初级')}`,
    '输出要求：',
    '1. 先总结简历优势；',
    '2. 明确指出与岗位不匹配的问题；',
    '3. 给出可执行的优化建议与改写示例。'
  ].join('\n')
}

/**
 * 一键填充场景模板。
 * 作用：快速生成可用初稿，并在覆盖已有内容前做确认。
 */
const fillPromptTemplateByScenario = async () => {
  const templateText = buildScenarioTemplate()
  const hasText = String(formData.promptContent || '').trim().length > 0
  if (!hasText) {
    formData.promptContent = templateText
    return
  }

  try {
    // 统一风险确认：覆盖已有内容属于高影响操作，显式提示覆盖后果。
    await confirmAdminRiskAction({
      title: '覆盖确认',
      actionText: '覆盖 Prompt 内容',
      targetName: '当前编辑内容',
      impactHint: '覆盖后将丢失当前输入文本，建议先复制备份。',
      type: 'warning'
    })
    formData.promptContent = templateText
  } catch (error) {
    // 用户取消覆盖时不提示错误，保持当前输入内容不变。
  }
}

/**
 * 检查是否存在同场景/同岗位/同难度重复模板。
 * 作用：保存前给出预警，避免重复配置导致模板维护混乱。
 * @returns {Record<string, any> | null}
 */
const findDuplicatePrompt = () => {
  return promptList.value.find((item) => {
    if (isEditMode.value && String(item.id) === String(formData.id)) return false
    return Number(item.scenarioType) === Number(formData.scenarioType)
      && String(item.jobRoleCode) === String(formData.jobRoleCode)
      && Number(item.difficulty) === Number(formData.difficulty)
  }) || null
}

/**
 * 计算编辑改动字段列表。
 * 作用：保存成功后给出明确反馈，帮助管理员确认本次变更范围。
 * @param {Record<string, any>} previousPayload
 * @param {Record<string, any>} nextPayload
 * @returns {string[]}
 */
const collectChangedFields = (previousPayload, nextPayload) => {
  const changedFields = []
  if (Number(previousPayload?.scenarioType) !== Number(nextPayload?.scenarioType)) changedFields.push('场景类型')
  if (String(previousPayload?.jobRoleCode) !== String(nextPayload?.jobRoleCode)) changedFields.push('岗位')
  if (Number(previousPayload?.difficulty) !== Number(nextPayload?.difficulty)) changedFields.push('难度')
  if (String(previousPayload?.promptContent || '').trim() !== String(nextPayload?.promptContent || '').trim()) {
    changedFields.push('Prompt 内容')
  }
  return changedFields
}

/**
 * 判断 Prompt 与岗位配置的联动状态。
 * 作用：
 * 1. 快速识别岗位是否已禁用或已删除。
 * 2. 为筛选、统计和状态标签提供统一口径。
 * @param {string | null | undefined} jobRoleCode
 * @returns {'linked-active' | 'linked-inactive' | 'unlinked'}
 */
const resolveJobRoleLinkageStatus = (jobRoleCode) => {
  const roleCode = String(jobRoleCode || '')
  if (!roleCode) return 'unlinked'
  const roleMeta = jobRoleMetaMap.value[roleCode]
  if (!roleMeta) return 'unlinked'
  return roleMeta.isActive ? 'linked-active' : 'linked-inactive'
}

/**
 * 重置筛选条件。
 * 作用：快速回到全量视图，减少配置排查时的操作成本。
 */
const resetFilters = () => {
  keyword.value = ''
  scenarioFilter.value = 'all'
  jobRoleFilter.value = 'all'
  difficultyFilter.value = 'all'
  statusFilter.value = 'all'
  linkageFilter.value = 'all'
}

/**
 * 统计卡片快捷筛选。
 * 作用：通过单击卡片快速定位常见管理视角，提高配置排查效率。
 * @param {'all' | 'active' | 'interview' | 'resume' | 'linkage-abnormal'} key
 */
const applyQuickFilter = (key) => {
  scenarioFilter.value = 'all'
  statusFilter.value = 'all'
  linkageFilter.value = 'all'

  if (key === 'active') {
    statusFilter.value = 'active'
    return
  }
  if (key === 'interview') {
    scenarioFilter.value = 1
    return
  }
  if (key === 'resume') {
    scenarioFilter.value = 2
    return
  }
  if (key === 'linkage-abnormal') {
    linkageFilter.value = 'abnormal'
  }
}

/**
 * 翻页处理。
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

const openCreateDialog = () => {
  isEditMode.value = false
  editOriginalPayload.value = null
  onlyActiveJobRoleOption.value = true
  resetFormData()
  dialogVisible.value = true
}

/**
 * 打开编辑弹窗并填充行数据。
 * @param {Record<string, any>} row
 */
const openEditDialog = (row) => {
  isEditMode.value = true
  editOriginalPayload.value = {
    scenarioType: Number(row.scenarioType ?? 1),
    jobRoleCode: row.jobRoleCode || '',
    difficulty: Number(row.difficulty ?? 1),
    promptContent: String(row.promptContent || '').trim()
  }
  formData.id = row.id
  formData.scenarioType = Number(row.scenarioType ?? 1)
  formData.jobRoleCode = row.jobRoleCode || ''
  formData.difficulty = Number(row.difficulty ?? 1)
  formData.promptContent = row.promptContent || ''
  // 如果历史模板绑定了已禁用岗位，自动放开下拉过滤，保证可见可改。
  onlyActiveJobRoleOption.value = resolveJobRoleLinkageStatus(row.jobRoleCode) !== 'linked-active'
  dialogVisible.value = true
}

/**
 * 提交 Prompt 表单。
 * 关键逻辑：创建和编辑共用同一个表单，按 isEditMode 决定接口。
 */
const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload = {
      scenarioType: formData.scenarioType,
      jobRoleCode: formData.jobRoleCode,
      difficulty: formData.difficulty,
      promptContent: String(formData.promptContent || '').trim()
    }

    const duplicatePrompt = findDuplicatePrompt()
    if (duplicatePrompt) {
      await confirmAdminRiskAction({
        title: '重复配置预警',
        actionText: '继续保存重复 Prompt',
        targetName: `${duplicatePrompt.jobRoleName || duplicatePrompt.jobRoleCode || '目标岗位'} / ${duplicatePrompt.difficultyDesc || '目标难度'}`,
        impactHint: '重复模板会增加维护与排障成本，请确认这是有意配置。',
        type: 'warning'
      })
    }

    if (isEditMode.value) {
      await updateAdminPrompt({
        id: formData.id,
        ...payload
      })
      const changedFields = collectChangedFields(editOriginalPayload.value, payload)
      showAdminSuccess(changedFields.length ? `Prompt 修改成功（${changedFields.join('、')}）` : 'Prompt 修改成功')
    } else {
      await createAdminPrompt(payload)
      showAdminSuccess('Prompt 新增成功')
    }

    dialogVisible.value = false
    await fetchPromptList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || '保存 Prompt 失败')
    }
  } finally {
    submitLoading.value = false
  }
}

/**
 * 切换 Prompt 启用状态。
 * @param {Record<string, any>} row
 */
const handleToggleActive = async (row) => {
  const nextActive = row.isActive === 1 ? 0 : 1
  const actionText = nextActive === 1 ? '启用' : '禁用'

  try {
    await confirmAdminRiskAction({
      title: `${actionText}确认`,
      actionText: `${actionText} Prompt`,
      targetName: row.jobRoleName || row.jobRoleCode || '目标模板',
      impactHint: '该操作会直接影响对应场景的实时模板生效状态。',
      type: 'warning'
    })
    await toggleAdminPromptActive(row.id, nextActive)
    showAdminSuccess(`Prompt 已${actionText}`)
    await fetchPromptList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || `${actionText} Prompt 失败`)
    }
  }
}

/**
 * 处理表格选择变化。
 * @param {Array} selection 选中的行数据
 */
const handleSelectionChange = (selection) => {
  selectedPrompts.value = selection
}

/**
 * 全部勾选当前筛选结果。
 */
const handleSelectAll = () => {
  if (promptTableRef.value) {
    promptTableRef.value.toggleAllSelection()
  }
}

/**
 * 删除单条 Prompt 模板。
 * @param {Object} row Prompt 行数据
 */
const handleDelete = async (row) => {
  try {
    await confirmAdminRiskAction({
      title: '删除确认',
      actionText: '删除 Prompt',
      targetName: row.jobRoleName || row.jobRoleCode || '目标模板',
      impactHint: '删除后数据无法恢复，请确认是否继续。',
      type: 'error'
    })
    await deletePrompt(row.id)
    showAdminSuccess('Prompt 删除成功')
    await fetchPromptList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || '删除 Prompt 失败')
    }
  }
}

/**
 * 批量删除 Prompt 模板。
 */
const handleBatchDelete = async () => {
  if (selectedPrompts.value.length === 0) {
    showAdminWarning('请先选择要删除的 Prompt')
    return
  }

  try {
    await confirmAdminRiskAction({
      title: '批量删除确认',
      actionText: '删除选中的 Prompt',
      targetName: `${selectedPrompts.value.length} 条数据`,
      impactHint: '删除后数据无法恢复，请确认是否继续。',
      type: 'error'
    })
    batchDeleteLoading.value = true
    const ids = selectedPrompts.value.map(item => item.id)
    await deletePrompts(ids)
    showAdminSuccess(`成功删除 ${ids.length} 条 Prompt`)
    await fetchPromptList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || '批量删除失败')
    }
  } finally {
    batchDeleteLoading.value = false
  }
}

/**
 * 批量禁用 Prompt 模板。
 */
const handleBatchDisable = async () => {
  if (selectedPrompts.value.length === 0) {
    showAdminWarning('请先选择要禁用的 Prompt')
    return
  }

  try {
    await confirmAdminRiskAction({
      title: '批量禁用确认',
      actionText: '禁用选中的 Prompt',
      targetName: `${selectedPrompts.value.length} 条数据`,
      impactHint: '禁用后对应场景将使用其他启用的模板。',
      type: 'warning'
    })
    const ids = selectedPrompts.value.map(item => item.id)
    await togglePromptsBatchActive(ids, 0)
    showAdminSuccess(`成功禁用 ${ids.length} 条 Prompt`)
    await fetchPromptList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || '批量禁用失败')
    }
  }
}

/**
 * 批量启用 Prompt 模板。
 */
const handleBatchEnable = async () => {
  if (selectedPrompts.value.length === 0) {
    showAdminWarning('请先选择要启用的 Prompt')
    return
  }

  try {
    await confirmAdminRiskAction({
      title: '批量启用确认',
      actionText: '启用选中的 Prompt',
      targetName: `${selectedPrompts.value.length} 条数据`,
      impactHint: '启用后将作为对应场景的模板。',
      type: 'warning'
    })
    const ids = selectedPrompts.value.map(item => item.id)
    await togglePromptsBatchActive(ids, 1)
    showAdminSuccess(`成功启用 ${ids.length} 条 Prompt`)
    await fetchPromptList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || '批量启用失败')
    }
  }
}

/**
 * 监听筛选项变化：
 * 每次条件变更都回到第一页，避免停留到无数据页。
 */
watch(
  () => [
    keyword.value,
    scenarioFilter.value,
    jobRoleFilter.value,
    difficultyFilter.value,
    statusFilter.value,
    linkageFilter.value
  ],
  () => {
    pagination.page = 1
  }
)

/**
 * 监听筛选结果长度：
 * 当结果减少导致页码越界时，自动回退到最后一页。
 */
watch(
  () => filteredPromptList.value.length,
  (total) => {
    const totalPage = Math.max(1, Math.ceil(total / pagination.pageSize))
    if (pagination.page > totalPage) {
      pagination.page = totalPage
    }
  }
)

onMounted(async () => {
  await Promise.all([fetchPromptList(), fetchJobRoleOptions()])
})
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

.btn-icon {
  margin-right: 6px;
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

.job-role-select-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.filter-result {
  margin-top: 0;
  font-size: 13px;
  color: #a08060;
  font-weight: 500;
}

.result-count {
  color: #d35400;
  font-weight: 700;
}

.table-card {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 20px rgba(143, 69, 27, 0.06);
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.prompt-table :deep(.el-table__header-wrapper) {
  background: var(--bg-elevated);
}

.table-header {
  font-weight: 600;
  color: #2c3e50;
}

.prompt-table :deep(.el-table__body tr:nth-child(even)) {
  background: var(--bg-elevated);
}

.prompt-table :deep(.el-table__body tr:hover > td) {
  background: #fff5e6;
}

.prompt-preview {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 320px;
}

.role-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.field-tip.warning {
  color: #d35400;
}

.content-tooltip :deep(.el-tooltip__popper) {
  background: #2c3e50;
}

.status-tag {
  border-radius: 4px;
  font-weight: 500;
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

.prompt-dialog :deep(.el-dialog__header) {
  background: var(--bg-elevated);
  border-bottom: 1px solid var(--border-divider);
}

.prompt-dialog :deep(.el-dialog__title) {
  font-weight: 600;
  color: #2c3e50;
}

.prompt-dialog :deep(.el-form-item__label) {
  font-weight: 500;
  color: #34495e;
}

.prompt-input :deep(.el-textarea__inner) {
  border-radius: 8px;
  min-height: 200px;
}

.prompt-meta {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
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

  .job-role-select-header,
  .prompt-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .pagination-wrap {
    justify-content: center;
  }
}
</style>
