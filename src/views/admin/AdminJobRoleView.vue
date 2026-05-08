<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">岗位配置管理</h2>
        <p class="page-subtitle">维护模拟面试岗位列表，供用户端岗位下拉与 Prompt 绑定使用</p>
      </div>
      <div class="header-actions">
        <el-button :loading="tableLoading" class="refresh-btn" @click="fetchJobRoleList">刷新列表</el-button>
        <el-button
          v-if="selectedJobRoles.length > 0"
          type="danger"
          :loading="batchDeleteLoading"
          @click="handleBatchDelete"
        >
          批量删除 ({{ selectedJobRoles.length }})
        </el-button>
        <el-button
          v-if="selectedJobRoles.length > 0"
          type="warning"
          @click="handleBatchDisable"
        >
          批量禁用
        </el-button>
        <el-button
          v-if="selectedJobRoles.length > 0"
          type="success"
          @click="handleBatchEnable"
        >
          批量启用
        </el-button>
        <el-button @click="handleSelectAll">
          全部勾选
        </el-button>
        <el-button type="primary" @click="openCreateDialog">新增岗位</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input
        v-model.trim="keyword"
        class="filter-item keyword"
        placeholder="按岗位编码/名称搜索"
        clearable
        :prefix-icon="Search"
      />
      <el-select v-model="statusFilter" class="filter-item" placeholder="按状态筛选">
        <el-option label="全部状态" value="all" />
        <el-option label="仅启用" value="active" />
        <el-option label="仅禁用" value="inactive" />
      </el-select>
      <el-button class="reset-btn" @click="resetFilters">重置筛选</el-button>
    </div>

    <div class="filter-result">
      当前筛选结果：<span class="result-count">{{ filteredJobRoleList.length }}</span> / {{ jobRoleList.length }} 条
    </div>

    <el-card shadow="never" class="table-card">
      <el-table ref="jobRoleTableRef" :data="pagedJobRoleList" v-loading="tableLoading" border :empty-text="tableEmptyText" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="100" />
        <el-table-column prop="roleCode" label="岗位编码" min-width="140" />
        <el-table-column prop="roleName" label="岗位名称" min-width="160" />
        <el-table-column label="展示标签" min-width="220">
          <template #default="{ row }">
            <div class="tag-list">
              <el-tag
                v-for="tag in parseInterviewTags(row.interviewTag)"
                :key="`${row.id}-${tag}`"
                :effect="resolveTemplateConfig(row.tagType).effect"
                :class="['custom-preview-tag', resolveTemplateConfig(row.tagType).className]"
              >
                {{ tag }}
              </el-tag>
              <span v-if="!parseInterviewTags(row.interviewTag).length" class="empty-tip">-</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="标签样式" min-width="150">
          <template #default="{ row }">
            {{ resolveTemplateConfig(row.tagType).label }}
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isActive === 1 ? 'success' : 'info'">
              {{ row.isActive === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button type="primary" size="small" round @click="openEditDialog(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-button
                size="small"
                :type="row.isActive === 1 ? 'warning' : 'success'"
                round
                @click="handleToggleActive(row)"
              >
                {{ row.isActive === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button
                type="danger"
                size="small"
                round
                @click="handleDelete(row)"
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
          :total="filteredJobRoleList.length"
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
      :title="isEditMode ? '编辑岗位' : '新增岗位'"
      width="720px"
      :close-on-click-modal="false"
      destroy-on-close
      :show-close="true"
      class="fixed-dialog"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="108px">
        <el-form-item label="岗位编码" prop="roleCode">
          <el-input
            id="roleCode"
            v-model.trim="formData.roleCode"
            placeholder="例如：frontend"
            :disabled="submitLoading"
          />
        </el-form-item>
        <el-form-item label="岗位名称" prop="roleName">
          <el-input
            id="roleName"
            v-model.trim="formData.roleName"
            placeholder="例如：前端开发工程师"
            :disabled="submitLoading"
          />
        </el-form-item>

        <el-form-item label="展示标签">
          <div class="tag-input-section">
            <el-select
              id="interviewTagList"
              v-model="formData.interviewTagList"
              multiple
              allow-create
              filterable
              default-first-option
              collapse-tags
              collapse-tags-tooltip
              clearable
              style="width: 100%"
              placeholder="请选择或输入展示标签"
              :disabled="submitLoading"
            >
              <el-option
                v-for="tag in interviewTagOptions"
                :key="tag.value"
                :label="tag.label"
                :value="tag.value"
              />
            </el-select>
            <div class="field-tip">支持从默认标签库快速选择，也支持手动补充新标签</div>
          </div>
        </el-form-item>

        <el-form-item label="标签实时预览">
          <div class="tag-list">
            <el-tag
              v-for="tag in normalizedFormInterviewTags"
              :key="tag"
              :effect="currentTemplateConfig.effect"
              :class="['custom-preview-tag', currentTemplateConfig.className]"
            >
              {{ tag }}
            </el-tag>
            <span v-if="!normalizedFormInterviewTags.length" class="empty-tip">未选择展示标签</span>
          </div>
        </el-form-item>

        <el-form-item label="标签样式模板" prop="tagType">
          <!-- 样式模板：只允许预设模板选择，避免自由文本导致展示异常 -->
          <div class="template-grid">
            <div
              v-for="template in tagStyleTemplateOptions"
              :key="template.value"
              :class="['template-item', { 'is-active': formData.tagType === template.value }]"
              @click="!submitLoading && (formData.tagType = template.value)"
            >
              <div class="template-item-header">
                <span class="template-item-title">{{ template.label }}</span>
                <el-icon v-if="formData.tagType === template.value" class="template-check-icon">
                  <Check />
                </el-icon>
              </div>
              <div class="template-item-preview">
                <el-tag
                  :effect="template.effect"
                  :class="['custom-preview-tag', template.className]"
                >
                  {{ normalizedFormInterviewTags[0] || '示例' }}
                </el-tag>
                <span class="template-item-label">风格</span>
              </div>
            </div>
          </div>
          <div class="field-tip">请选择预设样式模板，用户端将按该模板统一展示岗位标签</div>
        </el-form-item>

        <el-form-item label="排序" prop="sort">
          <el-input-number id="sort" v-model="formData.sort" :min="0" :disabled="submitLoading" />
        </el-form-item>
        <el-form-item v-if="isEditMode" label="状态">
          <el-radio-group id="isActive" v-model="formData.isActive" :disabled="submitLoading">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
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
import { Check, Search, Edit } from '@element-plus/icons-vue'
import {
  createAdminJobRole,
  deleteJobRole,
  deleteJobRoles,
  getAdminJobRoles,
  toggleAdminJobRoleActive,
  toggleJobRolesBatchActive,
  updateAdminJobRole
} from '@/api/admin/jobRoles'
import {
  confirmAdminRiskAction,
  resolveAdminTableEmptyText,
  showAdminError,
  showAdminSuccess
} from '@/utils/adminFeedback'

// 表格主数据：岗位配置列表。
const jobRoleList = ref([])
const tableLoading = ref(false)
// 表格实例：用于全选操作
const jobRoleTableRef = ref(null)
// 批量选择状态：用于批量删除操作
const selectedJobRoles = ref([])
// 批量删除加载状态
const batchDeleteLoading = ref(false)
const keyword = ref('')
const statusFilter = ref('all')
const pagination = reactive({
  page: 1,
  pageSize: 10
})

// 展示标签候选库：覆盖常见招聘场景，并支持后续继续扩充。
const interviewTagOptions = [
  { label: '热门', value: '热门' },
  { label: '推荐', value: '推荐' },
  { label: '急招', value: '急招' },
  { label: '高薪', value: '高薪' },
  { label: '远程', value: '远程' },
  { label: '校招', value: '校招' },
  { label: '社招', value: '社招' },
  { label: '实习', value: '实习' },
  { label: '全职', value: '全职' },
  { label: '兼职', value: '兼职' },
  { label: '双休', value: '双休' },
  { label: '五险一金', value: '五险一金' },
  { label: '大厂', value: '大厂' },
  { label: '初级', value: '初级' },
  { label: '中级', value: '中级' },
  { label: '高级', value: '高级' },
  { label: '专家', value: '专家' },
  { label: 'AI方向', value: 'AI方向' },
  { label: '前端', value: '前端' },
  { label: '后端', value: '后端' },
  { label: 'Java', value: 'Java' },
  { label: 'Python', value: 'Python' },
  { label: '数据', value: '数据' },
  { label: '算法', value: '算法' },
  { label: '产品', value: '产品' },
  { label: '运营', value: '运营' }
]

// 标签样式模板：前端建立“模板值 -> 展示效果”映射，避免自由文本造成样式失控。
const tagStyleTemplateOptions = [
  { value: 'default', label: '默认', effect: 'light', className: 'tag-style-default' },
  { value: 'orange-highlight', label: '橙色高亮', effect: 'dark', className: 'tag-style-orange' },
  { value: 'blue-info', label: '蓝色信息', effect: 'dark', className: 'tag-style-blue' },
  { value: 'green-success', label: '绿色成功', effect: 'dark', className: 'tag-style-green' },
  { value: 'red-alert', label: '红色提醒', effect: 'dark', className: 'tag-style-red' },
  { value: 'purple-feature', label: '紫色特色', effect: 'dark', className: 'tag-style-purple' },
  { value: 'gray-muted', label: '灰色弱化', effect: 'dark', className: 'tag-style-gray' },
  { value: 'outline', label: '描边风格', effect: 'plain', className: 'tag-style-outline' },
  { value: 'pill', label: '胶囊风格', effect: 'light', className: 'tag-style-pill' },
  { value: 'pink-rose', label: '粉色柔和', effect: 'light', className: 'tag-style-pink' }
]

// 历史值兼容映射：兼容老数据中的 tagType 值，避免编辑旧数据时样式丢失。
const legacyTagStyleAliasMap = {
  hot: 'orange-highlight',
  common: 'default',
  info: 'blue-info',
  success: 'green-success',
  warning: 'orange-highlight',
  danger: 'red-alert'
}

// 弹窗与提交状态：控制新增/编辑流程。
const dialogVisible = ref(false)
const submitLoading = ref(false)
const isEditMode = ref(false)
const formRef = ref(null)

// 表单核心字段：与后端 DTO 字段一一对应，tagType 存模板值。
const formData = reactive({
  id: null,
  roleCode: '',
  roleName: '',
  interviewTagList: [],
  tagType: 'default',
  sort: 0,
  isActive: 1
})

// 表单校验：岗位编码、岗位名称、标签样式模板和排序为必填项。
const formRules = {
  roleCode: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  tagType: [{ required: true, message: '请选择标签样式模板', trigger: 'change' }],
  sort: [{ required: true, message: '请设置排序值', trigger: 'change' }]
}

/**
 * 标准化标签数组。
 * 作用：去空值、去重、去首尾空格，避免提交无效标签。
 */
const normalizeTagList = (tags) => {
  if (!Array.isArray(tags)) return []
  return Array.from(new Set(tags.map((tag) => String(tag || '').trim()).filter(Boolean)))
}

/**
 * 将后端存储的 interviewTag 字符串解析为标签数组。
 * 说明：后端字段是字符串，本轮按“逗号分隔标签”做兼容扩展。
 */
const parseInterviewTags = (interviewTag) => {
  if (!interviewTag) return []
  return normalizeTagList(String(interviewTag).split(','))
}

/**
 * 归一化样式模板值。
 * 作用：兼容旧值并回落到 default，保证表单和预览可控。
 */
const normalizeTagTemplateValue = (rawTagType) => {
  const value = String(rawTagType || '').trim()
  const directHit = tagStyleTemplateOptions.find((item) => item.value === value)
  if (directHit) return directHit.value
  const alias = legacyTagStyleAliasMap[value]
  if (alias) return alias
  return 'default'
}

/**
 * 根据模板值读取样式配置。
 * 作用：列表展示、即时预览、用户端预览共用同一套样式逻辑。
 */
const resolveTemplateConfig = (tagTypeValue) => {
  const normalized = normalizeTagTemplateValue(tagTypeValue)
  const target = tagStyleTemplateOptions.find((item) => item.value === normalized)
  return target || tagStyleTemplateOptions[0]
}

/**
 * 表单中的标准化标签数组。
 * 作用：统一预览与提交数据源，避免显示和提交不一致。
 */
const normalizedFormInterviewTags = computed(() => normalizeTagList(formData.interviewTagList))

/**
 * 当前选中模板配置。
 * 作用：统一驱动“标签实时预览”和“用户端展示预览区”。
 */
const currentTemplateConfig = computed(() => resolveTemplateConfig(formData.tagType))

/**
 * 预览用岗位名称。
 * 作用：名称为空时给出占位，避免预览区空白。
 */
const previewRoleName = computed(() => {
  return formData.roleName?.trim() || '岗位名称未填写'
})

/**
 * 预览用标签数组。
 * 作用：将最终提交前的标签结果直接用于预览。
 */
const previewTags = computed(() => normalizedFormInterviewTags.value)

/**
 * 表格筛选结果。
 * 作用：将关键词与状态筛选统一在前端计算，不改变后端接口契约。
 */
const filteredJobRoleList = computed(() => {
  return jobRoleList.value.filter((item) => {
    const matchesKeyword = !keyword.value
      || item.roleCode?.includes(keyword.value)
      || item.roleName?.includes(keyword.value)
    const matchesStatus = statusFilter.value === 'all'
      || (statusFilter.value === 'active' && item.isActive === 1)
      || (statusFilter.value === 'inactive' && item.isActive !== 1)
    return matchesKeyword && matchesStatus
  })
})

/**
 * 分页后的岗位列表。
 * 作用：统一管理端列表浏览体验，避免岗位配置条目过多时可读性下降。
 */
const pagedJobRoleList = computed(() => {
  const startIndex = (pagination.page - 1) * pagination.pageSize
  const endIndex = startIndex + pagination.pageSize
  return filteredJobRoleList.value.slice(startIndex, endIndex)
})

/**
 * 表格空状态文案。
 * 作用：统一“列表无数据”和“筛选无命中”提示文案，提升跨页一致性。
 */
const tableEmptyText = computed(() => {
  return resolveAdminTableEmptyText(jobRoleList.value.length, filteredJobRoleList.value.length)
})

/**
 * 重置表单到默认状态。
 * 作用：避免上一次编辑残留数据影响下一次新增。
 */
const resetFormData = () => {
  formData.id = null
  formData.roleCode = ''
  formData.roleName = ''
  formData.interviewTagList = []
  formData.tagType = 'default'
  formData.sort = 0
  formData.isActive = 1
}

/**
 * 加载岗位配置列表。
 * 作用：作为页面初始数据和新增/编辑后的刷新入口。
 */
const fetchJobRoleList = async () => {
  tableLoading.value = true
  try {
    const res = await getAdminJobRoles()
    jobRoleList.value = Array.isArray(res?.data) ? res.data : []
  } catch (error) {
    showAdminError(error?.message || '加载岗位配置失败')
  } finally {
    tableLoading.value = false
  }
}

/**
 * 重置筛选条件。
 * 作用：快速回到全量列表，减少多轮筛选后的操作负担。
 */
const resetFilters = () => {
  keyword.value = ''
  statusFilter.value = 'all'
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
 * 打开新增弹窗。
 */
const openCreateDialog = () => {
  isEditMode.value = false
  resetFormData()
  dialogVisible.value = true
}

/**
 * 打开编辑弹窗并回填当前行数据。
 */
const openEditDialog = (row) => {
  isEditMode.value = true
  formData.id = row.id
  formData.roleCode = row.roleCode || ''
  formData.roleName = row.roleName || ''
  formData.interviewTagList = parseInterviewTags(row.interviewTag)
  formData.tagType = normalizeTagTemplateValue(row.tagType)
  formData.sort = Number(row.sort ?? 0)
  formData.isActive = Number(row.isActive ?? 0)
  dialogVisible.value = true
}

/**
 * 提交新增/编辑表单。
 * 关键点：
 * 1. 展示标签按逗号拼接存储，兼容后端原有字符串字段。
 * 2. 标签样式仅提交预设模板值，禁止自由文本导致不可控展示。
 */
const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload = {
      roleCode: formData.roleCode,
      roleName: formData.roleName,
      interviewTag: normalizedFormInterviewTags.value.join(',') || null,
      tagType: normalizeTagTemplateValue(formData.tagType),
      sort: Number(formData.sort)
    }

    if (isEditMode.value) {
      await updateAdminJobRole({
        id: formData.id,
        ...payload,
        isActive: formData.isActive
      })
      showAdminSuccess('岗位修改成功')
    } else {
      await createAdminJobRole(payload)
      showAdminSuccess('岗位新增成功')
    }

    dialogVisible.value = false
    await fetchJobRoleList()
  } catch (error) {
    showAdminError(error?.message || '保存岗位失败')
  } finally {
    submitLoading.value = false
  }
}

/**
 * 切换岗位启用状态。
 * 关键点：明确二次确认，防止误操作导致用户端岗位列表变化。
 */
const handleToggleActive = async (row) => {
  const nextActive = row.isActive === 1 ? 0 : 1
  const actionText = nextActive === 1 ? '启用' : '禁用'

  try {
    await confirmAdminRiskAction({
      title: `${actionText}确认`,
      actionText: `${actionText}岗位`,
      targetName: row.roleName,
      impactHint: '该操作会影响用户端岗位下拉与 Prompt 绑定项可见性。',
      type: 'warning'
    })
    await toggleAdminJobRoleActive(row.id, nextActive)
    showAdminSuccess(`岗位已${actionText}`)
    await fetchJobRoleList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || `${actionText}岗位失败`)
    }
  }
}

/**
 * 处理表格选择变化。
 * @param {Array} selection 选中的行数据
 */
const handleSelectionChange = (selection) => {
  selectedJobRoles.value = selection
}

/**
 * 全部勾选当前筛选结果。
 */
const handleSelectAll = () => {
  if (jobRoleTableRef.value) {
    jobRoleTableRef.value.toggleAllSelection()
  }
}

/**
 * 删除单条岗位配置。
 * @param {Object} row 岗位配置行数据
 */
const handleDelete = async (row) => {
  try {
    await confirmAdminRiskAction({
      title: '删除确认',
      actionText: '删除岗位',
      targetName: row.roleName,
      impactHint: '删除后数据无法恢复，请确认是否继续。',
      type: 'error'
    })
    await deleteJobRole(row.id)
    showAdminSuccess('岗位删除成功')
    await fetchJobRoleList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || '删除岗位失败')
    }
  }
}

/**
 * 批量删除岗位配置。
 */
const handleBatchDelete = async () => {
  if (selectedJobRoles.value.length === 0) {
    showAdminWarning('请先选择要删除的岗位')
    return
  }

  try {
    await confirmAdminRiskAction({
      title: '批量删除确认',
      actionText: '删除选中的岗位',
      targetName: `${selectedJobRoles.value.length} 条数据`,
      impactHint: '删除后数据无法恢复，请确认是否继续。',
      type: 'error'
    })
    batchDeleteLoading.value = true
    const ids = selectedJobRoles.value.map(item => item.id)
    await deleteJobRoles(ids)
    showAdminSuccess(`成功删除 ${ids.length} 条岗位配置`)
    await fetchJobRoleList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || '批量删除失败')
    }
  } finally {
    batchDeleteLoading.value = false
  }
}

/**
 * 批量禁用岗位配置。
 */
const handleBatchDisable = async () => {
  if (selectedJobRoles.value.length === 0) {
    showAdminWarning('请先选择要禁用的岗位')
    return
  }

  try {
    await confirmAdminRiskAction({
      title: '批量禁用确认',
      actionText: '禁用选中的岗位',
      targetName: `${selectedJobRoles.value.length} 条数据`,
      impactHint: '禁用后用户端将无法看到这些岗位。',
      type: 'warning'
    })
    const ids = selectedJobRoles.value.map(item => item.id)
    await toggleJobRolesBatchActive(ids, 0)
    showAdminSuccess(`成功禁用 ${ids.length} 条岗位配置`)
    await fetchJobRoleList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || '批量禁用失败')
    }
  }
}

/**
 * 批量启用岗位配置。
 */
const handleBatchEnable = async () => {
  if (selectedJobRoles.value.length === 0) {
    showAdminWarning('请先选择要启用的岗位')
    return
  }

  try {
    await confirmAdminRiskAction({
      title: '批量启用确认',
      actionText: '启用选中的岗位',
      targetName: `${selectedJobRoles.value.length} 条数据`,
      impactHint: '启用后用户端将可以看到这些岗位。',
      type: 'warning'
    })
    const ids = selectedJobRoles.value.map(item => item.id)
    await toggleJobRolesBatchActive(ids, 1)
    showAdminSuccess(`成功启用 ${ids.length} 条岗位配置`)
    await fetchJobRoleList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || '批量启用失败')
    }
  }
}

onMounted(() => {
  fetchJobRoleList()
})

/**
 * 监听筛选条件变化：
 * 条件变化后自动回到第一页，避免分页停留导致空页误判。
 */
watch(
  () => [keyword.value, statusFilter.value],
  () => {
    pagination.page = 1
  }
)

/**
 * 监听筛选结果长度：
 * 当结果减少导致页码越界时，自动回退到最后一页。
 */
watch(
  () => filteredJobRoleList.value.length,
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
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
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
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #8f451b 0%, #d35400 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #a08060;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(230, 126, 34, 0.08);
  border-radius: 12px;
  padding: 14px 18px;
}

.filter-item {
  width: 180px;
}

.filter-item.keyword {
  width: 300px;
}

.refresh-btn {
  background: linear-gradient(135deg, #fdf1e6 0%, #f6e0d0 100%);
  border: 1px solid rgba(230, 126, 34, 0.2);
  border-radius: 10px;
  color: #8f451b;
  font-weight: 500;
}

.refresh-btn:hover {
  background: linear-gradient(135deg, #fff8f3 0%, #f6e0d0 100%);
  border-color: #e67e22;
  color: #d35400;
}

.reset-btn {
  border-radius: 10px;
  border-color: #e8cbb5;
  color: #8f451b;
}

.filter-result {
  font-size: 13px;
  color: #a08060;
  font-weight: 500;
}

.result-count {
  color: #d35400;
  font-weight: 700;
}

.field-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #b8967a;
  font-weight: 500;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-tip {
  color: #b89d8a;
  font-size: 13px;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  width: 100%;
}

.template-item {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px;
  border: 2px solid rgba(217, 196, 170, 0.3);
  border-radius: 10px;
  background: linear-gradient(135deg, #fffcf8 0%, #fff8f3 100%);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.template-item:hover {
  border-color: rgba(230, 126, 34, 0.4);
  background: #fffcf8;
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(143, 69, 27, 0.12);
}

.template-item.is-active {
  border-color: #e67e22;
  background: linear-gradient(135deg, #fff8f3 0%, #fef3e8 100%);
  box-shadow: 0 6px 20px rgba(230, 126, 34, 0.2);
}

.template-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.template-item-title {
  font-size: 12px;
  font-weight: 600;
  color: #8f451b;
}

.template-check-icon {
  color: #e67e22;
  font-size: 14px;
}

.template-item-preview {
  display: flex;
  align-items: center;
  gap: 6px;
}

.template-item-label {
  display: none;
}

.custom-preview-tag {
  border-radius: 8px;
  transition: transform 0.2s ease;
}

.custom-preview-tag:hover {
  transform: scale(1.05);
}

.tag-style-default {
  --el-tag-bg-color: #fdf1e6;
  --el-tag-border-color: #f6c89f;
  --el-tag-text-color: #a05a2c;
}

.tag-style-orange {
  --el-tag-bg-color: rgba(255, 140, 66, 0.15);
  --el-tag-border-color: rgba(255, 140, 66, 0.3);
  --el-tag-text-color: #e67a35;
}

.tag-style-blue {
  --el-tag-bg-color: rgba(47, 125, 225, 0.12);
  --el-tag-border-color: rgba(47, 125, 225, 0.25);
  --el-tag-text-color: #2f7de1;
}

.tag-style-green {
  --el-tag-bg-color: rgba(48, 176, 111, 0.12);
  --el-tag-border-color: rgba(48, 176, 111, 0.25);
  --el-tag-text-color: #2a9658;
}

.tag-style-red {
  --el-tag-bg-color: rgba(224, 84, 84, 0.12);
  --el-tag-border-color: rgba(224, 84, 84, 0.25);
  --el-tag-text-color: #d64545;
}

.tag-style-purple {
  --el-tag-bg-color: rgba(123, 90, 217, 0.12);
  --el-tag-border-color: rgba(123, 90, 217, 0.25);
  --el-tag-text-color: #6b4dc9;
}

.tag-style-gray {
  --el-tag-bg-color: rgba(143, 153, 167, 0.15);
  --el-tag-border-color: rgba(143, 153, 167, 0.25);
  --el-tag-text-color: #6b7280;
}

.tag-style-outline {
  --el-tag-bg-color: rgba(255, 255, 255, 0.8);
  --el-tag-border-color: rgba(217, 180, 154, 0.6);
  --el-tag-text-color: #9a5c33;
}

.tag-style-pill {
  --el-tag-bg-color: rgba(255, 140, 66, 0.12);
  --el-tag-border-color: rgba(255, 140, 66, 0.25);
  --el-tag-text-color: #b35f2b;
  border-radius: 999px;
}

.tag-style-pink {
  --el-tag-bg-color: rgba(236, 113, 147, 0.12);
  --el-tag-border-color: rgba(236, 113, 147, 0.25);
  --el-tag-text-color: #d64575;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.table-card {
  border-radius: 14px;
  border: 1px solid rgba(217, 196, 170, 0.25);
  box-shadow: 0 4px 20px rgba(143, 69, 27, 0.06);
  background: var(--bg-card);
}

.pagination-wrap {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-table) {
  --el-table-border-color: rgba(217, 196, 170, 0.3);
  --el-table-header-bg-color: #fffcf8;
  --el-table-tr-bg-color: var(--bg-card);
}

:deep(.el-table th.el-table__cell) {
  background: linear-gradient(135deg, #fff8f3 0%, #fff3e8 100%);
  color: #8f451b;
  font-weight: 700;
  font-size: 13px;
}

:deep(.el-table td.el-table__cell) {
  padding: 14px 0;
}

:deep(.el-table .el-table__empty-text) {
  color: #c4a888;
}

:deep(.el-tag) {
  border-radius: 8px;
}

:deep(.el-dialog) {
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(143, 69, 27, 0.2);
}

:deep(.el-dialog__header) {
  background: linear-gradient(135deg, #fffcf8 0%, #fff3e8 100%);
  padding: 22px 26px;
  margin: 0;
  border-bottom: 1px solid rgba(230, 126, 34, 0.1);
}

:deep(.el-dialog__title) {
  color: #5a4030;
  font-size: 20px;
  font-weight: 700;
}

:deep(.el-dialog__body) {
  padding: 24px;
}

:deep(.el-dialog__footer) {
  padding: 18px 26px;
  border-top: 1px solid rgba(230, 126, 34, 0.1);
  background: linear-gradient(135deg, #fffcf8 0%, #fff8f3 100%);
}

:deep(.el-form-item__label) {
  color: #5a4030;
  font-weight: 600;
  font-size: 14px;
}

:deep(.el-form-item) {
  margin-bottom: 14px;
}

:deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px rgba(217, 196, 170, 0.4) inset;
  transition: all 0.25s ease;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(230, 126, 34, 0.3) inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(230, 126, 34, 0.15), 0 0 0 1px #e67e22 inset;
}

:deep(.el-input__inner) {
  color: #5a4030;
}

:deep(.el-select .el-select__wrapper) {
  border-radius: 12px;
}

:deep(.el-radio-group) {
  display: flex;
  gap: 14px;
}

:deep(.el-radio__label) {
  color: #5a4030;
  font-weight: 500;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%);
  border: none;
  border-radius: 12px;
  font-weight: 600;
  box-shadow: 0 4px 16px rgba(230, 126, 34, 0.25);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #d56a15 0%, #c0392b 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(230, 126, 34, 0.35);
}

:deep(.el-button--default) {
  border-radius: 12px;
  border-color: rgba(230, 126, 34, 0.25);
  color: #8f451b;
  font-weight: 500;
}

:deep(.el-button--default:hover) {
  border-color: #e67e22;
  color: #d35400;
  background: rgba(255, 140, 66, 0.06);
}

:deep(.el-button--default:hover) {
  border-color: #e67e22;
  color: #e67e22;
}

@media (max-width: 960px) {
  .template-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  .template-grid {
    grid-template-columns: 1fr;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-item,
  .filter-item.keyword {
    width: 100%;
  }

  .pagination-wrap {
    justify-content: center;
  }
}

:global(.el-overlay:has(.fixed-dialog)) {
  overflow: hidden !important;
}

.fixed-dialog :deep(.el-dialog) {
  position: relative !important;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.fixed-dialog :deep(.el-dialog__wrapper) {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.fixed-dialog :deep(.el-dialog__header) {
  flex-shrink: 0;
  padding: 12px 20px;
  margin: 0;
  background: var(--bg-elevated);
  border-bottom: 1px solid var(--border-divider);
}

.fixed-dialog :deep(.el-dialog__body) {
  flex: 1;
  padding: 16px 20px;
}

.fixed-dialog :deep(.el-dialog__footer) {
  flex-shrink: 0;
  padding: 12px 20px;
  background: var(--bg-elevated);
  border-top: 1px solid var(--border-divider);
}

.dialog-btn {
  border-radius: 8px;
  padding: 9px 20px;
  font-weight: 500;
}

.dialog-btn.primary {
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%);
  border: none;
}

.dialog-btn.primary:hover {
  background: linear-gradient(135deg, #d35400 0%, #c0392b 100%);
}
</style>
