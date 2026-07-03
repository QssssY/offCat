<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">版本更新日志</h2>
        <p class="page-subtitle">管理产品版本更新记录，发布后用户可在首页查看</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" class="refresh-btn" @click="loadVersionLogs">刷新列表</el-button>
        <el-button v-if="selectedVersions.length > 0" type="success" @click="handleBatchPublish">
          批量发布 ({{ selectedVersions.length }})
        </el-button>
        <el-button v-if="selectedVersions.length > 0" type="danger" @click="handleBatchDelete">
          批量删除
        </el-button>
        <el-button type="primary" @click="openCreateDialog" class="create-btn">
          <el-icon><Plus /></el-icon>
          新增版本
        </el-button>
      </div>
    </div>

    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage" class="page-error" />

    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-input
          v-model.trim="keyword"
          class="filter-item keyword"
          placeholder="按版本号、标题或内容搜索"
          clearable
          @keyup.enter="handleFilterChange"
          @clear="handleFilterChange"
        />
        <el-select v-model="filterForm.type" class="filter-item" placeholder="按类型筛选" @change="handleFilterChange">
          <el-option label="全部类型" value="all" />
          <el-option label="大版本" value="major" />
          <el-option label="小版本" value="minor" />
          <el-option label="修补" value="patch" />
        </el-select>
        <el-select v-model="filterForm.status" class="filter-item" placeholder="按状态筛选" @change="handleFilterChange">
          <el-option label="全部状态" value="all" />
          <el-option label="草稿" value="0" />
          <el-option label="已发布" value="1" />
        </el-select>
        <el-button class="filter-action-btn" @click="handleFilterChange">筛选</el-button>
        <el-button class="reset-btn" @click="resetFilters">重置筛选</el-button>
      </div>
      <div class="filter-result">
        当前筛选结果：<span class="result-count">{{ versionList.length }}</span> / {{ total }} 条
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        :data="versionList"
        v-loading="loading"
        border
        stripe
        class="admin-data-table version-table"
        empty-text="暂无版本日志"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="version" label="版本号" width="130" align="center">
          <template #header><div class="table-header">版本号</div></template>
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 'major' ? 'danger' : row.type === 'minor' ? 'warning' : ''" effect="plain">
              v{{ row.version }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip>
          <template #header><div class="table-header">标题</div></template>
        </el-table-column>
        <el-table-column prop="typeDesc" label="类型" width="100" align="center">
          <template #header><div class="table-header">类型</div></template>
        </el-table-column>
        <el-table-column prop="statusDesc" label="状态" width="100" align="center">
          <template #header><div class="table-header">状态</div></template>
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="plain">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" min-width="180" show-overflow-tooltip>
          <template #header><div class="table-header">发布时间</div></template>
          <template #default="{ row }">{{ formatDate(row.publishedAt) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180" show-overflow-tooltip>
          <template #header><div class="table-header">创建时间</div></template>
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #header><div class="table-header">操作</div></template>
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" class="action-btn" @click="openEditDialog(row)">编辑</el-button>
              <el-button
                v-if="row.status === 0"
                size="small"
                type="success"
                plain
                class="action-btn"
                @click="handlePublish(row)"
              >
                发布
              </el-button>
              <el-popconfirm title="确认删除此版本记录？" @confirm="handleDelete(row)">
                <template #reference>
                  <el-button size="small" type="danger" plain class="action-btn">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :current-page="currentPage"
          :page-size="size"
          :page-sizes="[10, 20, 50]"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑版本' : '新增版本'" width="640px" :close-on-click-modal="false">
      <el-form :model="form" label-width="80px" :rules="formRules" ref="formRef">
        <el-form-item label="版本号" prop="version">
          <el-input v-model="form.version" placeholder="如 2.1.0" maxlength="32" />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入版本标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="版本类型" prop="type">
          <el-select v-model="form.type" class="full-width">
            <el-option label="大版本" value="major" />
            <el-option label="小版本" value="minor" />
            <el-option label="修补" value="patch" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布方式">
          <el-radio-group v-model="form.publishMode">
            <el-radio value="draft">存为草稿</el-radio>
            <el-radio value="publish">立即发布</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="更新内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="请输入更新内容" maxlength="10000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ isEdit ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getAdminVersionLogs,
  createAdminVersionLog,
  updateAdminVersionLog,
  publishAdminVersionLog,
  publishAdminVersionLogsBatch,
  deleteAdminVersionLog,
  deleteAdminVersionLogsBatch
} from '@/api/admin/versionLogs'
import { showAdminSuccess, showAdminError } from '@/utils/adminFeedback'
import { formatDate } from '@/utils/date'

const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const versionList = ref([])
const selectedVersions = ref([])
const currentPage = ref(1)
const size = ref(20)
const total = ref(0)
const keyword = ref('')
const filterForm = reactive({
  type: 'all',
  status: 'all'
})
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  version: '',
  title: '',
  type: 'minor',
  publishMode: 'publish',
  content: ''
})

const formRules = {
  version: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
  title: [{ required: true, message: '请输入版本标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择版本类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入更新内容', trigger: 'blur' }]
}

const normalizeNumber = (value) => Number(value || 0)

const buildQueryParams = () => {
  const params = { page: currentPage.value, size: size.value }
  // 版本日志筛选交给后端处理，保证分页总数、当前页数据和批量操作基于同一查询口径。
  if (filterForm.type !== 'all') params.type = filterForm.type
  if (filterForm.status !== 'all') params.status = Number(filterForm.status)
  if (keyword.value) params.keyword = keyword.value
  return params
}

const loadVersionLogs = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getAdminVersionLogs(buildQueryParams())
    const data = res?.data || {}
    versionList.value = data.records || []
    total.value = normalizeNumber(data.total)
    selectedVersions.value = []
    const totalPage = Math.max(1, Math.ceil(total.value / size.value))
    if (total.value > 0 && currentPage.value > totalPage) {
      currentPage.value = totalPage
      await loadVersionLogs()
    }
  } catch (e) {
    errorMessage.value = e?.message || '加载版本日志失败'
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadVersionLogs()
}

const handlePageSizeChange = (pageSize) => {
  size.value = pageSize
  currentPage.value = 1
  loadVersionLogs()
}

const handleSelectionChange = (selection) => {
  selectedVersions.value = selection
}

const handleFilterChange = () => {
  currentPage.value = 1
  loadVersionLogs()
}

const resetFilters = () => {
  keyword.value = ''
  filterForm.type = 'all'
  filterForm.status = 'all'
  currentPage.value = 1
  loadVersionLogs()
}

const resetForm = () => {
  form.version = ''
  form.title = ''
  form.type = 'minor'
  form.publishMode = 'publish'
  form.content = ''
}

const openCreateDialog = () => {
  isEdit.value = false
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  editingId.value = row.id
  form.version = row.version
  form.title = row.title
  form.type = row.type
  form.content = row.content
  form.publishMode = row.status === 1 ? 'publish' : 'draft'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const data = {
      title: form.title,
      version: form.version,
      type: form.type,
      content: form.content,
      status: form.publishMode === 'publish' ? 1 : 0
    }
    if (isEdit.value) {
      await updateAdminVersionLog({ id: editingId.value, ...data })
      showAdminSuccess('版本日志更新成功')
    } else {
      await createAdminVersionLog(data)
      showAdminSuccess('版本日志创建成功')
      currentPage.value = 1
    }
    dialogVisible.value = false
    await loadVersionLogs()
  } catch (e) {
    showAdminError(e?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handlePublish = async (row) => {
  try {
    await publishAdminVersionLog(row.id)
    showAdminSuccess('版本日志已发布')
    await loadVersionLogs()
  } catch (e) {
    showAdminError(e?.message || '发布失败')
  }
}

const handleDelete = async (row) => {
  try {
    await deleteAdminVersionLog(row.id)
    showAdminSuccess('版本日志已删除')
    await loadVersionLogs()
  } catch (e) {
    showAdminError(e?.message || '删除失败')
  }
}

const handleBatchPublish = async () => {
  const ids = selectedVersions.value.filter((item) => item.status === 0).map((item) => item.id)
  if (ids.length === 0) {
    showAdminError('请选择草稿版本')
    return
  }
  try {
    await ElMessageBox.confirm(`确认发布选中的 ${ids.length} 条草稿版本？`, '批量发布确认', { type: 'warning' })
    await publishAdminVersionLogsBatch(ids)
    showAdminSuccess('版本日志批量发布成功')
    await loadVersionLogs()
  } catch (e) {
    if (e !== 'cancel') showAdminError(e?.message || '批量发布失败')
  }
}

const handleBatchDelete = async () => {
  const ids = selectedVersions.value.map((item) => item.id)
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${ids.length} 条版本日志？`, '批量删除确认', { type: 'warning' })
    await deleteAdminVersionLogsBatch(ids)
    showAdminSuccess('版本日志批量删除成功')
    await loadVersionLogs()
  } catch (e) {
    if (e !== 'cancel') showAdminError(e?.message || '批量删除失败')
  }
}

onMounted(loadVersionLogs)
</script>

<style scoped>
.admin-page { display: flex; flex-direction: column; gap: 18px; }
.page-header {
  background: linear-gradient(135deg, #fffcf8 0%, #fff8f3 100%);
  border: 1px solid rgba(230, 126, 34, 0.12);
  border-radius: 16px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  box-shadow: 0 6px 20px rgba(143, 69, 27, 0.08);
}
.page-title {
  margin: 0; font-size: 24px; font-weight: 700;
  background: linear-gradient(135deg, #8f451b 0%, #d35400 100%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.page-subtitle { margin: 6px 0 0; color: #a08060; font-size: 14px; }
.header-actions { display: flex; align-items: center; justify-content: flex-end; gap: 10px; flex-wrap: wrap; }
.create-btn,
.refresh-btn {
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%);
  border: none; border-radius: 12px; padding: 12px 20px; font-weight: 600;
  box-shadow: 0 6px 20px rgba(230, 126, 34, 0.3); color: #fff;
}
.page-error { margin-bottom: 2px; }
.filter-card {
  border: 1px solid rgba(217, 196, 170, 0.25);
  border-radius: 16px;
  box-shadow: 0 6px 20px rgba(143, 69, 27, 0.05);
}
.filter-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.filter-item { width: 170px; }
.filter-item.keyword { width: min(320px, 100%); }
.filter-action-btn,
.reset-btn { border-radius: 10px; font-weight: 600; }
.filter-result { margin-top: 12px; color: #8a6f56; font-size: 13px; }
.result-count { color: #8f451b; font-weight: 700; }
.table-card {
  border: 1px solid rgba(217, 196, 170, 0.25);
  border-radius: 18px;
  box-shadow: 0 6px 20px rgba(143, 69, 27, 0.06);
}
.admin-data-table { width: 100%; min-width: 1080px; }
.admin-data-table :deep(.el-table__cell .cell) { white-space: nowrap; }
.table-header { font-weight: 700; color: #5a4030; }
.action-group { display: flex; align-items: center; justify-content: center; gap: 8px; flex-wrap: wrap; }
.action-btn { margin-left: 0; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.full-width { width: 100%; }
@media (max-width: 768px) {
  .page-header { align-items: flex-start; flex-direction: column; }
  .header-actions { justify-content: flex-start; }
  .filter-row { align-items: stretch; flex-direction: column; }
  .filter-item, .filter-item.keyword { width: 100%; }
  .pagination-wrap { justify-content: flex-start; overflow-x: auto; }
}
</style>
