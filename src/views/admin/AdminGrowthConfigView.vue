<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">成长中心配置</h2>
        <p class="page-subtitle">管理成长中心的成就规则、激励文案和里程碑配置</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" class="refresh-btn" @click="loadConfigs">刷新列表</el-button>
        <el-button v-if="selectedConfigs.length > 0" type="danger" @click="handleBatchDelete">
          批量删除 ({{ selectedConfigs.length }})
        </el-button>
        <el-button type="primary" @click="openCreateDialog" class="create-btn">
          <el-icon><Plus /></el-icon>
          新增配置
        </el-button>
      </div>
    </div>

    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage" class="page-error" />

    <el-card shadow="never" class="table-card">
      <div class="filter-bar">
        <el-select v-model="groupFilter" placeholder="配置分组" clearable @change="onGroupFilterChange" class="filter-select">
          <el-option label="全部" value="" />
          <el-option label="成就规则" value="achievement" />
          <el-option label="激励文案" value="encouragement" />
          <el-option label="里程碑" value="milestone" />
          <el-option label="默认" value="default" />
        </el-select>
      </div>

      <el-table
        :data="configList"
        v-loading="loading"
        border
        stripe
        class="admin-data-table growth-table"
        empty-text="暂无成长配置"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="configKey" label="配置键" min-width="220" show-overflow-tooltip>
          <template #header><div class="table-header">配置键</div></template>
        </el-table-column>
        <el-table-column prop="configValue" label="配置值" min-width="320" show-overflow-tooltip>
          <template #header><div class="table-header">配置值</div></template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip>
          <template #header><div class="table-header">说明</div></template>
        </el-table-column>
        <el-table-column prop="groupName" label="分组" width="120" align="center">
          <template #header><div class="table-header">分组</div></template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="90" align="center">
          <template #header><div class="table-header">排序</div></template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #header><div class="table-header">操作</div></template>
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" class="action-btn" @click="openEditDialog(row)">编辑</el-button>
              <el-popconfirm title="确认删除此配置？" @confirm="handleDelete(row)">
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑配置' : '新增配置'" width="600px" :close-on-click-modal="false">
      <el-form :model="form" label-width="90px" :rules="formRules" ref="formRef">
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="form.configKey" placeholder="如 achievement_first_interview" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="form.configValue" type="textarea" :rows="4" placeholder="配置值" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" placeholder="配置说明" maxlength="255" />
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="form.groupName" class="full-width">
            <el-option label="成就规则" value="achievement" />
            <el-option label="激励文案" value="encouragement" />
            <el-option label="里程碑" value="milestone" />
            <el-option label="默认" value="default" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
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
  getAdminGrowthConfigs,
  createAdminGrowthConfig,
  updateAdminGrowthConfig,
  deleteAdminGrowthConfig,
  deleteAdminGrowthConfigsBatch
} from '@/api/admin/growthConfig'
import { showAdminSuccess, showAdminError } from '@/utils/adminFeedback'

const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const configList = ref([])
const selectedConfigs = ref([])
const currentPage = ref(1)
const size = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const groupFilter = ref('')
const formRef = ref(null)

const form = reactive({
  configKey: '',
  configValue: '',
  description: '',
  groupName: 'default',
  sort: 0
})

const formRules = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }]
}

const normalizeNumber = (value) => Number(value || 0)

const onGroupFilterChange = () => {
  currentPage.value = 1
  loadConfigs()
}

const loadConfigs = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getAdminGrowthConfigs(groupFilter.value || undefined, { page: currentPage.value, size: size.value })
    const data = res?.data || {}
    configList.value = data.records || []
    total.value = normalizeNumber(data.total)
    selectedConfigs.value = []
    const totalPage = Math.max(1, Math.ceil(total.value / size.value))
    if (total.value > 0 && currentPage.value > totalPage) {
      currentPage.value = totalPage
      await loadConfigs()
    }
  } catch (e) {
    errorMessage.value = e?.message || '加载配置列表失败'
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadConfigs()
}

const handlePageSizeChange = (pageSize) => {
  size.value = pageSize
  currentPage.value = 1
  loadConfigs()
}

const handleSelectionChange = (selection) => {
  selectedConfigs.value = selection
}

const resetForm = () => {
  form.configKey = ''
  form.configValue = ''
  form.description = ''
  form.groupName = 'default'
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
  form.configKey = row.configKey
  form.configValue = row.configValue
  form.description = row.description || ''
  form.groupName = row.groupName
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
      await updateAdminGrowthConfig({ id: editingId.value, ...data })
      showAdminSuccess('配置更新成功')
    } else {
      await createAdminGrowthConfig(data)
      showAdminSuccess('配置创建成功')
      currentPage.value = 1
    }
    dialogVisible.value = false
    await loadConfigs()
  } catch (e) {
    showAdminError(e?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await deleteAdminGrowthConfig(row.id)
    showAdminSuccess('配置已删除')
    await loadConfigs()
  } catch (e) {
    showAdminError(e?.message || '删除失败')
  }
}

const handleBatchDelete = async () => {
  const ids = selectedConfigs.value.map((item) => item.id)
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${ids.length} 条配置？`, '批量删除确认', { type: 'warning' })
    await deleteAdminGrowthConfigsBatch(ids)
    showAdminSuccess('配置批量删除成功')
    await loadConfigs()
  } catch (e) {
    if (e !== 'cancel') showAdminError(e?.message || '批量删除失败')
  }
}

onMounted(loadConfigs)
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
.filter-bar { margin-bottom: 12px; display: flex; align-items: center; gap: 12px; }
.filter-select { width: 180px; }
.admin-data-table { width: 100%; min-width: 1120px; }
.admin-data-table :deep(.el-table__cell .cell) { white-space: nowrap; }
.table-header { font-weight: 700; color: #5a4030; }
.action-group { display: flex; align-items: center; justify-content: center; gap: 8px; flex-wrap: wrap; }
.action-btn { margin-left: 0; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.full-width { width: 100%; }
@media (max-width: 768px) {
  .page-header { align-items: flex-start; flex-direction: column; }
  .header-actions { justify-content: flex-start; }
  .pagination-wrap { justify-content: flex-start; overflow-x: auto; }
}
</style>
