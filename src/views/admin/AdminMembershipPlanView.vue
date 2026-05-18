<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">会员套餐管理</h2>
        <p class="page-subtitle">管理会员套餐定价、额度配置和启用状态</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" class="refresh-btn" @click="loadPlans">刷新列表</el-button>
        <el-button v-if="selectedPlans.length > 0" type="success" @click="handleBatchEnable">批量启用</el-button>
        <el-button v-if="selectedPlans.length > 0" type="warning" @click="handleBatchDisable">批量禁用</el-button>
        <el-button v-if="selectedPlans.length > 0" type="danger" @click="handleBatchDelete">
          批量删除 ({{ selectedPlans.length }})
        </el-button>
        <el-button type="primary" @click="openCreateDialog" class="create-btn">
          <el-icon><Plus /></el-icon>
          新增套餐
        </el-button>
      </div>
    </div>

    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage" class="page-error" />

    <el-card shadow="never" class="table-card">
      <el-table
        :data="planList"
        v-loading="loading"
        border
        stripe
        class="admin-data-table plan-table"
        empty-text="暂无会员套餐"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="planName" label="套餐名称" min-width="170" show-overflow-tooltip>
          <template #header><div class="table-header">套餐名称</div></template>
        </el-table-column>
        <el-table-column prop="planCode" label="编码" min-width="150" show-overflow-tooltip>
          <template #header><div class="table-header">编码</div></template>
        </el-table-column>
        <el-table-column prop="priceAmount" label="价格" width="120" align="right">
          <template #header><div class="table-header">价格</div></template>
          <template #default="{ row }">¥{{ row.priceAmount }}</template>
        </el-table-column>
        <el-table-column prop="durationDays" label="有效期(天)" width="120" align="center">
          <template #header><div class="table-header">有效期(天)</div></template>
        </el-table-column>
        <el-table-column prop="resumeQuota" label="简历额度" width="120" align="center">
          <template #header><div class="table-header">简历额度</div></template>
        </el-table-column>
        <el-table-column prop="interviewQuota" label="面试额度" width="120" align="center">
          <template #header><div class="table-header">面试额度</div></template>
        </el-table-column>
        <el-table-column prop="statusDesc" label="状态" width="100" align="center">
          <template #header><div class="table-header">状态</div></template>
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="plain">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="90" align="center">
          <template #header><div class="table-header">排序</div></template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #header><div class="table-header">操作</div></template>
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" class="action-btn" @click="openEditDialog(row)">编辑</el-button>
              <el-button
                size="small"
                :type="row.status === 1 ? 'warning' : 'success'"
                plain
                class="action-btn"
                @click="handleToggleActive(row)"
              >
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-popconfirm title="确认删除此套餐？" @confirm="handleDelete(row)">
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑套餐' : '新增套餐'" width="560px" :close-on-click-modal="false">
      <el-form :model="form" label-width="100px" :rules="formRules" ref="formRef">
        <el-form-item label="套餐编码" prop="planCode">
          <el-input v-model="form.planCode" placeholder="如 vip_month" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="套餐名称" prop="planName">
          <el-input v-model="form.planName" placeholder="如 月卡VIP" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" placeholder="套餐描述" maxlength="255" show-word-limit />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="价格(¥)" prop="priceAmount" class="form-half">
            <el-input-number v-model="form.priceAmount" :precision="2" :min="0" :step="10" style="width: 100%" />
          </el-form-item>
          <el-form-item label="有效期(天)" prop="durationDays" class="form-half">
            <el-input-number v-model="form.durationDays" :min="1" :step="30" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="简历额度" prop="resumeQuota" class="form-half">
            <el-input-number v-model="form.resumeQuota" :min="0" :step="5" style="width: 100%" />
          </el-form-item>
          <el-form-item label="面试额度" prop="interviewQuota" class="form-half">
            <el-input-number v-model="form.interviewQuota" :min="0" :step="5" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :step="1" />
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
  getAdminMembershipPlans,
  createAdminMembershipPlan,
  updateAdminMembershipPlan,
  toggleAdminMembershipPlanActive,
  toggleAdminMembershipPlansBatchActive,
  deleteAdminMembershipPlan,
  deleteAdminMembershipPlansBatch
} from '@/api/admin/membership'
import { showAdminSuccess, showAdminError } from '@/utils/adminFeedback'

const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const planList = ref([])
const selectedPlans = ref([])
const currentPage = ref(1)
const size = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  planCode: '',
  planName: '',
  description: '',
  priceAmount: 29.90,
  durationDays: 30,
  resumeQuota: 10,
  interviewQuota: 10,
  sort: 0
})

const formRules = {
  planCode: [{ required: true, message: '请输入套餐编码', trigger: 'blur' }],
  planName: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
  priceAmount: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  durationDays: [{ required: true, message: '请输入有效期', trigger: 'blur' }],
  resumeQuota: [{ required: true, message: '请输入简历额度', trigger: 'blur' }],
  interviewQuota: [{ required: true, message: '请输入面试额度', trigger: 'blur' }]
}

const normalizeNumber = (value) => Number(value || 0)

const loadPlans = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getAdminMembershipPlans({ page: currentPage.value, size: size.value })
    const data = res?.data || {}
    planList.value = Array.isArray(data) ? data : (data.records || [])
    total.value = Array.isArray(data) ? data.length : normalizeNumber(data.total)
    selectedPlans.value = []
    const totalPage = Math.max(1, Math.ceil(total.value / size.value))
    if (total.value > 0 && currentPage.value > totalPage) {
      currentPage.value = totalPage
      await loadPlans()
    }
  } catch (e) {
    errorMessage.value = e?.message || '加载套餐列表失败'
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadPlans()
}

const handlePageSizeChange = (pageSize) => {
  size.value = pageSize
  currentPage.value = 1
  loadPlans()
}

const handleSelectionChange = (selection) => {
  selectedPlans.value = selection
}

const resetForm = () => {
  form.planCode = ''
  form.planName = ''
  form.description = ''
  form.priceAmount = 29.90
  form.durationDays = 30
  form.resumeQuota = 10
  form.interviewQuota = 10
  form.sort = 0
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
  form.planCode = row.planCode
  form.planName = row.planName
  form.description = row.description || ''
  form.priceAmount = row.priceAmount
  form.durationDays = row.durationDays
  form.resumeQuota = row.resumeQuota
  form.interviewQuota = row.interviewQuota
  form.sort = row.sort
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const data = { ...form }
    if (isEdit.value) {
      await updateAdminMembershipPlan({ id: editingId.value, ...data })
      showAdminSuccess('套餐更新成功')
    } else {
      await createAdminMembershipPlan(data)
      showAdminSuccess('套餐创建成功')
      currentPage.value = 1
    }
    dialogVisible.value = false
    await loadPlans()
  } catch (e) {
    showAdminError(e?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleToggleActive = async (row) => {
  try {
    const newStatus = row.status === 1 ? 0 : 1
    await toggleAdminMembershipPlanActive(row.id, newStatus)
    showAdminSuccess(newStatus === 1 ? '套餐已启用' : '套餐已禁用')
    await loadPlans()
  } catch (e) {
    showAdminError(e?.message || '操作失败')
  }
}

const handleDelete = async (row) => {
  try {
    await deleteAdminMembershipPlan(row.id)
    showAdminSuccess('套餐已删除')
    await loadPlans()
  } catch (e) {
    showAdminError(e?.message || '删除失败')
  }
}

const updateSelectedStatus = async (status) => {
  const ids = selectedPlans.value.map((item) => item.id)
  await toggleAdminMembershipPlansBatchActive(ids, status)
  showAdminSuccess(status === 1 ? '套餐批量启用成功' : '套餐批量禁用成功')
  await loadPlans()
}

const handleBatchEnable = async () => {
  try {
    await ElMessageBox.confirm(`确认启用选中的 ${selectedPlans.value.length} 个套餐？`, '批量启用确认', { type: 'warning' })
    await updateSelectedStatus(1)
  } catch (e) {
    if (e !== 'cancel') showAdminError(e?.message || '批量启用失败')
  }
}

const handleBatchDisable = async () => {
  try {
    await ElMessageBox.confirm(`确认禁用选中的 ${selectedPlans.value.length} 个套餐？`, '批量禁用确认', { type: 'warning' })
    await updateSelectedStatus(0)
  } catch (e) {
    if (e !== 'cancel') showAdminError(e?.message || '批量禁用失败')
  }
}

const handleBatchDelete = async () => {
  const ids = selectedPlans.value.map((item) => item.id)
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${ids.length} 个套餐？`, '批量删除确认', { type: 'warning' })
    await deleteAdminMembershipPlansBatch(ids)
    showAdminSuccess('套餐批量删除成功')
    await loadPlans()
  } catch (e) {
    if (e !== 'cancel') showAdminError(e?.message || '批量删除失败')
  }
}

onMounted(loadPlans)
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
.table-card {
  border: 1px solid rgba(217, 196, 170, 0.25);
  border-radius: 18px;
  box-shadow: 0 6px 20px rgba(143, 69, 27, 0.06);
}
.admin-data-table { width: 100%; min-width: 1240px; }
.admin-data-table :deep(.el-table__cell .cell) { white-space: nowrap; }
.table-header { font-weight: 700; color: #5a4030; }
.action-group { display: flex; align-items: center; justify-content: center; gap: 8px; flex-wrap: wrap; }
.action-btn { margin-left: 0; }
.pagination-wrap { width: 100%; margin-top: 16px; display: flex; justify-content: flex-end; }
.form-row { display: flex; gap: 16px; }
.form-half { flex: 1; }
@media (max-width: 768px) {
  .page-header { align-items: flex-start; flex-direction: column; }
  .header-actions { justify-content: flex-start; }
  .form-row { flex-direction: column; gap: 0; }
  .pagination-wrap { justify-content: flex-end; overflow-x: auto; }
}
</style>
