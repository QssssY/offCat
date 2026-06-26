<template>
  <div class="admin-page">
    <section class="page-header">
      <div>
        <h2 class="page-title">问题反馈</h2>
        <p class="page-subtitle">接收用户提交的问题和建议，跟进处理状态与内部备注</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" class="refresh-btn" @click="loadFeedbackList">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button v-if="selectedRows.length > 0" type="danger" plain @click="handleBatchDelete">
          批量删除
        </el-button>
      </div>
    </section>

    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage" class="page-error" />

    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="反馈类型">
          <el-select v-model="filters.type" clearable class="filter-select" placeholder="全部类型">
            <el-option label="问题反馈" value="bug" />
            <el-option label="功能建议" value="suggestion" />
            <el-option label="体验问题" value="experience" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="filters.status" clearable class="filter-select" placeholder="全部状态">
            <el-option label="待处理" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已处理" :value="2" />
            <el-option label="已关闭" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="filters.userId" clearable class="filter-input" placeholder="输入用户ID" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">筛选</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        :data="feedbackList"
        v-loading="loading"
        border
        stripe
        class="admin-data-table feedback-table"
        empty-text="暂无问题反馈"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" min-width="180" show-overflow-tooltip>
          <template #header><div class="table-header">ID</div></template>
        </el-table-column>
        <el-table-column prop="username" label="提交用户" min-width="130" show-overflow-tooltip>
          <template #header><div class="table-header">提交用户</div></template>
        </el-table-column>
        <el-table-column prop="typeDesc" label="类型" width="110" align="center">
          <template #header><div class="table-header">类型</div></template>
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)" size="small" effect="plain">{{ row.typeDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip>
          <template #header><div class="table-header">标题</div></template>
        </el-table-column>
        <el-table-column prop="statusDesc" label="状态" width="110" align="center">
          <template #header><div class="table-header">状态</div></template>
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)" size="small" effect="plain">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handlerName" label="处理人" min-width="120" show-overflow-tooltip>
          <template #header><div class="table-header">处理人</div></template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="180" show-overflow-tooltip>
          <template #header><div class="table-header">提交时间</div></template>
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #header><div class="table-header">操作</div></template>
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" @click="openDetail(row)">查看</el-button>
              <el-button size="small" type="primary" plain @click="openHandleDialog(row)">处理</el-button>
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

    <el-dialog
      v-model="detailVisible"
      title="反馈详情"
      width="680px"
      class="feedback-detail-dialog"
      :close-on-click-modal="false"
    >
      <div v-if="currentItem" class="detail-view">
        <div class="detail-field"><label>标题</label><span>{{ currentItem.title }}</span></div>
        <div class="detail-field"><label>提交用户</label><span>{{ currentItem.username }}（{{ currentItem.userId }}）</span></div>
        <div class="detail-field"><label>类型</label><span>{{ currentItem.typeDesc }}</span></div>
        <div class="detail-field"><label>状态</label><span>{{ currentItem.statusDesc }}</span></div>
        <div class="detail-field"><label>联系方式</label><span>{{ currentItem.contact || '--' }}</span></div>
        <div class="detail-field"><label>处理人</label><span>{{ currentItem.handlerName || '--' }}</span></div>
        <div class="detail-field"><label>处理时间</label><span>{{ formatDate(currentItem.handledAt) }}</span></div>
        <div class="detail-scroll-sections">
          <div class="detail-field detail-content"><label>反馈内容</label><div class="content-box scroll-content-box feedback-content-box">{{ currentItem.content }}</div></div>
          <div class="detail-field detail-content"><label>处理备注</label><div class="content-box scroll-content-box remark-content-box">{{ currentItem.adminRemark || '暂无处理备注' }}</div></div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="openHandleDialog(currentItem)">处理</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="handleVisible" title="处理反馈" width="560px" :close-on-click-modal="false">
      <el-form ref="handleFormRef" :model="handleForm" :rules="handleRules" label-position="top">
        <el-form-item label="处理状态" prop="status">
          <el-select v-model="handleForm.status" class="full-width">
            <el-option label="待处理" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已处理" :value="2" />
            <el-option label="已关闭" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理备注" prop="adminRemark">
          <el-input v-model="handleForm.adminRemark" type="textarea" :rows="6" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleStatusSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { deleteAdminFeedbackBatch, getAdminFeedbackList, updateAdminFeedbackStatus } from '@/api/admin/feedback'
import { showAdminError, showAdminSuccess } from '@/utils/adminFeedback'
import { formatDate } from '@/utils/date'

const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const feedbackList = ref([])
const selectedRows = ref([])
const currentPage = ref(1)
const size = ref(20)
const total = ref(0)
const detailVisible = ref(false)
const handleVisible = ref(false)
const currentItem = ref(null)
const handleFormRef = ref(null)

const filters = reactive({ type: '', status: '', userId: '' })
const handleForm = reactive({ id: null, status: 0, adminRemark: '' })

const handleRules = {
  status: [{ required: true, message: '请选择处理状态', trigger: 'change' }],
  adminRemark: [{ max: 1000, message: '处理备注不能超过 1000 个字符', trigger: 'blur' }]
}

const buildQuery = () => {
  const query = { page: currentPage.value, size: size.value }
  if (filters.type) query.type = filters.type
  if (filters.status !== '') query.status = filters.status
  if (filters.userId) query.userId = filters.userId
  return query
}

const loadFeedbackList = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getAdminFeedbackList(buildQuery())
    const data = res?.data || {}
    feedbackList.value = data.records || []
    total.value = Number(data.total || 0)
    selectedRows.value = []
  } catch (e) {
    errorMessage.value = e?.message || '加载反馈列表失败'
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadFeedbackList()
}

const handleReset = () => {
  filters.type = ''
  filters.status = ''
  filters.userId = ''
  currentPage.value = 1
  loadFeedbackList()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadFeedbackList()
}

const handlePageSizeChange = (pageSize) => {
  size.value = pageSize
  currentPage.value = 1
  loadFeedbackList()
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const openDetail = (row) => {
  currentItem.value = row
  detailVisible.value = true
}

const openHandleDialog = (row) => {
  if (!row) return
  currentItem.value = row
  handleForm.id = row.id
  handleForm.status = row.status
  handleForm.adminRemark = row.adminRemark || ''
  handleVisible.value = true
}

const handleStatusSave = async () => {
  const valid = await handleFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await updateAdminFeedbackStatus(handleForm.id, {
      status: handleForm.status,
      adminRemark: handleForm.adminRemark
    })
    showAdminSuccess('反馈状态已更新')
    handleVisible.value = false
    detailVisible.value = false
    await loadFeedbackList()
  } catch (e) {
    showAdminError(e?.message || '保存处理状态失败')
  } finally {
    submitting.value = false
  }
}

const handleBatchDelete = async () => {
  const ids = selectedRows.value.map((item) => item.id)
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${ids.length} 条反馈记录？`, '批量删除确认', { type: 'warning' })
    await deleteAdminFeedbackBatch(ids)
    showAdminSuccess('反馈已批量删除')
    await loadFeedbackList()
  } catch (e) {
    if (e !== 'cancel') showAdminError(e?.message || '批量删除失败')
  }
}

const getTypeTag = (type) => {
  const map = { bug: 'danger', suggestion: 'success', experience: 'warning', other: 'info' }
  return map[type] || ''
}

const getStatusTag = (status) => {
  const map = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'info' }
  return map[status] || 'info'
}

onMounted(loadFeedbackList)
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
.header-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.refresh-btn {
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%);
  border: none; border-radius: 12px; padding: 12px 20px; font-weight: 600;
  box-shadow: 0 6px 20px rgba(230, 126, 34, 0.3); color: #fff;
}
.page-error { margin-bottom: 2px; }
.filter-card,
.table-card {
  border: 1px solid rgba(217, 196, 170, 0.25);
  border-radius: 18px;
  box-shadow: 0 6px 20px rgba(143, 69, 27, 0.06);
}
.filter-form { display: flex; flex-wrap: wrap; gap: 2px; }
.filter-select { width: 160px; }
.filter-input { width: 180px; }
.admin-data-table { width: 100%; min-width: 1320px; }
.admin-data-table :deep(.el-table__cell .cell) { white-space: nowrap; }
.table-header { font-weight: 700; color: #5a4030; }
.action-group { display: flex; align-items: center; justify-content: center; gap: 8px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
:global(.el-overlay:has(.feedback-detail-dialog)),
:global(.el-overlay-dialog:has(.feedback-detail-dialog)) {
  overflow: hidden !important;
}
:global(.el-overlay-dialog:has(.feedback-detail-dialog)) {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 0 44px;
  box-sizing: border-box;
}
.feedback-detail-dialog :deep(.el-dialog) {
  margin: 0 auto !important;
  max-height: calc(100vh - 64px);
  max-height: calc(100dvh - 64px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.feedback-detail-dialog :deep(.el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}
.feedback-detail-dialog :deep(.el-dialog__footer) {
  flex-shrink: 0;
}
.detail-view {
  max-height: calc(100vh - 180px);
  max-height: calc(100dvh - 180px);
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}
.detail-field { display: flex; gap: 12px; }
.detail-field label { width: 82px; font-weight: 600; color: #5a4030; flex-shrink: 0; }
.detail-field span { color: #333; overflow-wrap: anywhere; }
.detail-content { flex-direction: column; }
.detail-content label { margin-bottom: 4px; }
.detail-scroll-sections {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.content-box {
  background: #faf8f5; border: 1px solid #e8e0d8; border-radius: 8px;
  padding: 12px; white-space: pre-wrap; font-size: 14px; line-height: 1.6;
  overflow-wrap: anywhere;
}
.scroll-content-box {
  overflow-y: auto;
}
.feedback-content-box { max-height: clamp(140px, 26vh, 280px); }
.remark-content-box { max-height: clamp(88px, 15vh, 170px); }
.full-width { width: 100%; }
@media (max-width: 768px) {
  .page-header { align-items: flex-start; flex-direction: column; }
  .pagination-wrap { justify-content: flex-start; overflow-x: auto; }
  .filter-select,
  .filter-input { width: 100%; }
  :global(.el-overlay-dialog:has(.feedback-detail-dialog)) { padding: 16px 0 32px; }
  .feedback-detail-dialog :deep(.el-dialog) {
    max-height: calc(100vh - 48px);
    max-height: calc(100dvh - 48px);
  }
  .detail-view {
    max-height: calc(100vh - 180px);
    max-height: calc(100dvh - 180px);
  }
  .feedback-content-box { max-height: clamp(96px, 20vh, 180px); }
  .remark-content-box { max-height: clamp(72px, 10vh, 120px); }
}
</style>
