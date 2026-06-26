<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">通知公告管理</h2>
        <p class="page-subtitle">创建和管理系统通知公告，支持按用户群体定向发送</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" class="refresh-btn" @click="loadNotifications">刷新列表</el-button>
        <el-button
          v-if="selectedNotifications.length > 0"
          type="success"
          @click="handleBatchPublish"
        >
          批量发布 ({{ selectedNotifications.length }})
        </el-button>
        <el-button
          v-if="selectedNotifications.length > 0"
          type="danger"
          @click="handleBatchDelete"
        >
          批量删除
        </el-button>
        <el-button type="primary" @click="openCreateDialog" class="create-btn">
          <el-icon><Plus /></el-icon>
          创建公告
        </el-button>
      </div>
    </div>

    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage" class="page-error" />

    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-input
          v-model.trim="keyword"
          class="filter-item keyword"
          placeholder="按标题或内容搜索"
          clearable
          @keyup.enter="handleFilterChange"
          @clear="handleFilterChange"
        />
        <el-select v-model="filterForm.type" class="filter-item" placeholder="按类型筛选" @change="handleFilterChange">
          <el-option label="全部类型" value="all" />
          <el-option label="系统公告" value="system" />
          <el-option label="活动通知" value="activity" />
          <el-option label="版本更新" value="update" />
          <el-option label="维护通知" value="maintenance" />
        </el-select>
        <el-select v-model="filterForm.status" class="filter-item" placeholder="按状态筛选" @change="handleFilterChange">
          <el-option label="全部状态" value="all" />
          <el-option label="草稿" value="0" />
          <el-option label="已发布" value="1" />
        </el-select>
        <el-select v-model="filterForm.targetType" class="filter-item" placeholder="按目标用户筛选" @change="handleFilterChange">
          <el-option label="全部目标用户" value="all" />
          <el-option label="全部用户" value="all-users" />
          <el-option label="VIP用户" value="vip" />
          <el-option label="普通用户" value="normal" />
        </el-select>
        <el-button class="filter-action-btn" @click="handleFilterChange">筛选</el-button>
        <el-button class="reset-btn" @click="resetFilters">重置筛选</el-button>
      </div>
      <div class="filter-result">
        当前筛选结果：<span class="result-count">{{ notificationList.length }}</span> / {{ total }} 条
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        :data="notificationList"
        v-loading="loading"
        border
        stripe
        class="admin-data-table notification-table"
        empty-text="暂无通知公告"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="90" align="center">
          <template #header><div class="table-header">ID</div></template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip>
          <template #header><div class="table-header">标题</div></template>
        </el-table-column>
        <el-table-column prop="typeDesc" label="类型" width="120" align="center">
          <template #header><div class="table-header">类型</div></template>
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)" size="small" effect="plain">{{ row.typeDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetTypeDesc" label="目标用户" width="130" align="center">
          <template #header><div class="table-header">目标用户</div></template>
        </el-table-column>
        <el-table-column prop="statusDesc" label="状态" width="100" align="center">
          <template #header><div class="table-header">状态</div></template>
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="plain">
              {{ row.statusDesc }}
            </el-tag>
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
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #header><div class="table-header">操作</div></template>
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" class="action-btn" @click="viewDetail(row)">查看</el-button>
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
              <el-popconfirm title="确认删除此公告？" @confirm="handleDelete(row)">
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

    <el-dialog v-model="dialogVisible" :title="isDetail ? '公告详情' : '创建公告'" width="640px" :close-on-click-modal="false">
      <template v-if="isDetail && currentItem">
        <div class="detail-view">
          <div class="detail-field"><label>标题</label><span>{{ currentItem.title }}</span></div>
          <div class="detail-field"><label>类型</label><span>{{ currentItem.typeDesc }}</span></div>
          <div class="detail-field"><label>目标用户</label><span>{{ currentItem.targetTypeDesc }}</span></div>
          <div class="detail-field"><label>状态</label><span>{{ currentItem.statusDesc }}</span></div>
          <div class="detail-field"><label>发布时间</label><span>{{ formatDate(currentItem.publishedAt) }}</span></div>
          <div class="detail-field detail-content"><label>内容</label><div class="content-box">{{ currentItem.content }}</div></div>
        </div>
      </template>
      <template v-else>
        <el-form :model="form" label-width="80px" :rules="formRules" ref="formRef">
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item label="类型" prop="type">
            <el-select v-model="form.type" class="full-width">
              <el-option label="系统公告" value="system" />
              <el-option label="活动通知" value="activity" />
              <el-option label="版本更新" value="update" />
              <el-option label="维护通知" value="maintenance" />
            </el-select>
          </el-form-item>
          <el-form-item label="目标用户" prop="targetType">
            <el-select v-model="form.targetType" class="full-width">
              <el-option label="全部用户" value="all" />
              <el-option label="VIP用户" value="vip" />
              <el-option label="普通用户" value="normal" />
            </el-select>
          </el-form-item>
          <el-form-item label="发布方式">
            <el-radio-group v-model="form.publishMode">
              <el-radio value="draft">存为草稿</el-radio>
              <el-radio value="publish">立即发布</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="内容" prop="content">
            <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入公告内容" maxlength="5000" show-word-limit />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button v-if="!isDetail" type="primary" :loading="submitting" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getAdminNotifications,
  createAdminNotification,
  publishAdminNotification,
  publishAdminNotificationsBatch,
  deleteAdminNotification,
  deleteAdminNotificationsBatch
} from '@/api/admin/notifications'
import { showAdminSuccess, showAdminError } from '@/utils/adminFeedback'
import { formatDate } from '@/utils/date'

const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const notificationList = ref([])
const selectedNotifications = ref([])
const currentPage = ref(1)
const size = ref(20)
const total = ref(0)
const keyword = ref('')
const filterForm = reactive({
  type: 'all',
  status: 'all',
  targetType: 'all'
})
const dialogVisible = ref(false)
const isDetail = ref(false)
const currentItem = ref(null)
const formRef = ref(null)

const form = reactive({
  title: '',
  type: 'system',
  targetType: 'all',
  publishMode: 'publish',
  content: ''
})

const formRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择公告类型', trigger: 'change' }],
  targetType: [{ required: true, message: '请选择目标用户', trigger: 'change' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

const normalizeNumber = (value) => Number(value || 0)

const getTypeTag = (type) => {
  const map = { system: '', activity: 'warning', update: 'primary', maintenance: 'danger' }
  return map[type] || ''
}

const buildQueryParams = () => {
  const params = { page: currentPage.value, size: size.value }
  if (filterForm.type !== 'all') params.type = filterForm.type
  if (filterForm.status !== 'all') params.status = Number(filterForm.status)
  if (filterForm.targetType !== 'all') {
    params.targetType = filterForm.targetType === 'all-users' ? 'all' : filterForm.targetType
  }
  if (keyword.value) params.keyword = keyword.value
  return params
}

const loadNotifications = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getAdminNotifications(buildQueryParams())
    const data = res?.data || {}
    notificationList.value = data.records || []
    total.value = normalizeNumber(data.total)
    selectedNotifications.value = []
    const totalPage = Math.max(1, Math.ceil(total.value / size.value))
    if (total.value > 0 && currentPage.value > totalPage) {
      currentPage.value = totalPage
      await loadNotifications()
    }
  } catch (e) {
    errorMessage.value = e?.message || '加载公告列表失败'
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadNotifications()
}

const handlePageSizeChange = (pageSize) => {
  size.value = pageSize
  currentPage.value = 1
  loadNotifications()
}

const handleSelectionChange = (selection) => {
  selectedNotifications.value = selection
}

const handleFilterChange = () => {
  currentPage.value = 1
  loadNotifications()
}

const resetFilters = () => {
  keyword.value = ''
  filterForm.type = 'all'
  filterForm.status = 'all'
  filterForm.targetType = 'all'
  currentPage.value = 1
  loadNotifications()
}

const openCreateDialog = () => {
  isDetail.value = false
  currentItem.value = null
  form.title = ''
  form.type = 'system'
  form.targetType = 'all'
  form.publishMode = 'publish'
  form.content = ''
  dialogVisible.value = true
}

const viewDetail = (item) => {
  isDetail.value = true
  currentItem.value = item
  dialogVisible.value = true
}

const handleCreate = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await createAdminNotification({
      title: form.title,
      content: form.content,
      type: form.type,
      targetType: form.targetType,
      status: form.publishMode === 'publish' ? 1 : 0
    })
    showAdminSuccess('公告创建成功')
    dialogVisible.value = false
    currentPage.value = 1
    await loadNotifications()
  } catch (e) {
    showAdminError(e?.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

const handlePublish = async (row) => {
  try {
    await publishAdminNotification(row.id)
    showAdminSuccess('公告已发布')
    await loadNotifications()
  } catch (e) {
    showAdminError(e?.message || '发布失败')
  }
}

const handleDelete = async (row) => {
  try {
    await deleteAdminNotification(row.id)
    showAdminSuccess('公告已删除')
    await loadNotifications()
  } catch (e) {
    showAdminError(e?.message || '删除失败')
  }
}

const handleBatchPublish = async () => {
  const ids = selectedNotifications.value.filter((item) => item.status === 0).map((item) => item.id)
  if (ids.length === 0) {
    showAdminError('请选择草稿公告')
    return
  }
  try {
    await ElMessageBox.confirm(`确认发布选中的 ${ids.length} 条草稿公告？`, '批量发布确认', { type: 'warning' })
    await publishAdminNotificationsBatch(ids)
    showAdminSuccess('公告批量发布成功')
    await loadNotifications()
  } catch (e) {
    if (e !== 'cancel') showAdminError(e?.message || '批量发布失败')
  }
}

const handleBatchDelete = async () => {
  const ids = selectedNotifications.value.map((item) => item.id)
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${ids.length} 条公告？`, '批量删除确认', { type: 'warning' })
    await deleteAdminNotificationsBatch(ids)
    showAdminSuccess('公告批量删除成功')
    await loadNotifications()
  } catch (e) {
    if (e !== 'cancel') showAdminError(e?.message || '批量删除失败')
  }
}

onMounted(loadNotifications)
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
.filter-item.keyword { width: min(280px, 100%); }
.filter-action-btn,
.reset-btn { border-radius: 10px; font-weight: 600; }
.filter-result { margin-top: 12px; color: #8a6f56; font-size: 13px; }
.result-count { color: #8f451b; font-weight: 700; }
.table-card {
  border: 1px solid rgba(217, 196, 170, 0.25);
  border-radius: 18px;
  box-shadow: 0 6px 20px rgba(143, 69, 27, 0.06);
}
.admin-data-table { width: 100%; min-width: 1180px; }
.admin-data-table :deep(.el-table__cell .cell) { white-space: nowrap; }
.table-header { font-weight: 700; color: #5a4030; }
.action-group { display: flex; align-items: center; justify-content: center; gap: 8px; flex-wrap: wrap; }
.action-btn { margin-left: 0; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.detail-view { display: flex; flex-direction: column; gap: 14px; }
.detail-field { display: flex; gap: 12px; }
.detail-field label { width: 80px; font-weight: 600; color: #5a4030; flex-shrink: 0; }
.detail-field span { color: #333; }
.detail-content { flex-direction: column; }
.detail-content label { margin-bottom: 4px; }
.content-box {
  background: #faf8f5; border: 1px solid #e8e0d8; border-radius: 8px;
  padding: 12px; white-space: pre-wrap; font-size: 14px; line-height: 1.6;
}
.full-width { width: 100%; }
@media (max-width: 768px) {
  .page-header { align-items: flex-start; flex-direction: column; }
  .header-actions { justify-content: flex-start; }
  .filter-row { align-items: stretch; flex-direction: column; }
  .filter-item, .filter-item.keyword { width: 100%; }
  .pagination-wrap { justify-content: flex-start; overflow-x: auto; }
}
</style>
