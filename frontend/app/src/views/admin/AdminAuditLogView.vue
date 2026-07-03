<template>
  <div class="admin-page">
    <section class="page-header">
      <div>
        <h2 class="page-title">审计日志</h2>
        <p class="page-subtitle">查看管理员对用户权益的变更操作记录</p>
      </div>
      <div class="header-actions">
        <el-button @click="handleExport" :disabled="logList.length === 0" class="export-btn">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
        <el-button :loading="loading" @click="loadAuditLogs" class="refresh-btn">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </section>

    <!-- 筛选区域 -->
    <section class="filter-card">
      <el-form :inline="true" class="filter-form" @submit.prevent="loadAuditLogs">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            :clearable="true"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="filters.operatorName" placeholder="操作人搜索" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="目标用户">
          <el-input v-model="filters.targetUsername" placeholder="目标用户搜索" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="变更类型">
          <el-select v-model="filters.roleChangeType" placeholder="全部" clearable style="width: 140px">
            <el-option label="升级会员" :value="1" />
            <el-option label="降级普通" :value="2" />
            <el-option label="设为管理员" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">筛选</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage" class="page-error" />

    <el-card shadow="never" class="table-card">
      <el-table :data="logList" v-loading="loading" border stripe class="admin-data-table audit-table" empty-text="暂无审计日志">
        <el-table-column prop="id" label="ID" min-width="180" show-overflow-tooltip>
          <template #header><div class="table-header">ID</div></template>
        </el-table-column>
        <el-table-column prop="username" label="目标用户" min-width="140" show-overflow-tooltip>
          <template #header><div class="table-header">目标用户</div></template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" min-width="120">
          <template #header><div class="table-header">操作人</div></template>
        </el-table-column>
        <el-table-column label="角色变更" min-width="210">
          <template #header><div class="table-header">角色变更</div></template>
          <template #default="{ row }">
            <span>{{ row.beforeRoleDesc }}</span>
            <el-icon color="#d35400"><ArrowRight /></el-icon>
            <span>{{ row.afterRoleDesc }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="afterMembershipPlanCode" label="套餐编码" min-width="140" show-overflow-tooltip>
          <template #header><div class="table-header">套餐编码</div></template>
        </el-table-column>
        <el-table-column label="有效期至" min-width="180" show-overflow-tooltip>
          <template #header><div class="table-header">有效期至</div></template>
          <template #default="{ row }">{{ formatDate(row.afterVipExpireTime) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="260" show-overflow-tooltip>
          <template #header><div class="table-header">备注</div></template>
        </el-table-column>
        <el-table-column label="操作时间" min-width="190" show-overflow-tooltip>
          <template #header><div class="table-header">操作时间</div></template>
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ArrowRight, Download, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getAdminAuditLogs } from '@/api/admin/auditLogs'
import { formatDate } from '@/utils/date'
import { exportToXlsx } from '@/utils/export'

const loading = ref(false)
const errorMessage = ref('')
const logList = ref([])
const currentPage = ref(1)
const size = ref(20)
const total = ref(0)

// 筛选状态
const filters = reactive({
  dateRange: null,
  operatorName: '',
  targetUsername: '',
  roleChangeType: ''
})

// 构建查询参数
const buildQuery = () => {
  const query = { page: currentPage.value, size: size.value }
  if (filters.operatorName) query.operatorName = filters.operatorName.trim()
  if (filters.targetUsername) query.targetUsername = filters.targetUsername.trim()
  if (filters.roleChangeType !== '' && filters.roleChangeType !== null) query.roleChangeType = filters.roleChangeType
  if (Array.isArray(filters.dateRange) && filters.dateRange.length === 2) {
    query.startDate = filters.dateRange[0]
    query.endDate = filters.dateRange[1]
  }
  return query
}

const loadAuditLogs = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getAdminAuditLogs(buildQuery())
    const data = res?.data || {}
    logList.value = data.records || []
    total.value = Number(data.total || 0)
  } catch (e) {
    errorMessage.value = e?.message || '加载审计日志失败'
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadAuditLogs()
}

const resetFilters = () => {
  filters.dateRange = null
  filters.operatorName = ''
  filters.targetUsername = ''
  filters.roleChangeType = ''
  currentPage.value = 1
  loadAuditLogs()
}

// XLSX 导出：等待按需加载和文件生成完成后再提示成功，避免首次导出时提示早于下载。
const handleExport = async () => {
  if (logList.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  const headers = ['ID', '目标用户', '操作人', '变更前角色', '变更后角色', '套餐编码', '有效期至', '备注', '操作时间']
  const rows = logList.value.map(row => [
    row.id,
    row.username,
    row.operatorName,
    row.beforeRoleDesc,
    row.afterRoleDesc,
    row.afterMembershipPlanCode || '',
    row.afterVipExpireTime || '',
    row.remark || '',
    formatDate(row.createTime)
  ])
  await exportToXlsx({
    headers,
    rows,
    filename: `审计日志_${new Date().toISOString().slice(0, 10)}`,
    sheetName: '审计日志'
  })
  ElMessage.success('导出成功')
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadAuditLogs()
}

const handlePageSizeChange = (pageSize) => {
  size.value = pageSize
  currentPage.value = 1
  loadAuditLogs()
}

onMounted(loadAuditLogs)
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
  box-shadow: 0 6px 20px rgba(143, 69, 27, 0.08);
}
.page-title {
  margin: 0; font-size: 24px; font-weight: 700;
  background: linear-gradient(135deg, #8f451b 0%, #d35400 100%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.page-subtitle { margin: 6px 0 0; color: #a08060; font-size: 14px; }
.header-actions { display: flex; gap: 10px; }
.export-btn {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(217, 196, 170, 0.4);
  border-radius: 12px;
  color: #8f451b;
  font-weight: 500;
  transition: all 0.3s;
}
.export-btn:hover { border-color: #e67e22; color: #d35400; }
.refresh-btn {
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%);
  border: none; border-radius: 12px; padding: 12px 24px; font-weight: 600;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 6px 20px rgba(230, 126, 34, 0.3); color: #fff;
}
.refresh-btn:hover { transform: translateY(-2px); box-shadow: 0 10px 28px rgba(230, 126, 34, 0.4); }

/* 筛选区域 */
.filter-card {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(230, 126, 34, 0.1);
  border-radius: 14px;
  padding: 16px 20px;
  box-shadow: 0 4px 16px rgba(143, 69, 27, 0.06);
}
.filter-form { display: flex; flex-wrap: wrap; gap: 0; align-items: center; }
.filter-form :deep(.el-form-item) { margin-bottom: 0; margin-right: 16px; }
.filter-form :deep(.el-form-item__label) { color: #8f572f; font-weight: 500; }
.filter-form :deep(.el-input__wrapper) { border-radius: 10px; }
.filter-form :deep(.el-select .el-input__wrapper) { border-radius: 10px; }

.page-error { margin-bottom: 2px; }
.table-card {
  border: 1px solid rgba(217, 196, 170, 0.25);
  border-radius: 18px;
  box-shadow: 0 6px 20px rgba(143, 69, 27, 0.06);
}

.admin-data-table {
  width: 100%;
  min-width: 1420px;
}

.admin-data-table :deep(.el-table__cell .cell) {
  white-space: nowrap;
}

.table-header {
  font-weight: 700;
  color: #5a4030;
}

.admin-data-table :deep(.el-table__cell .el-icon) {
  margin: 0 4px;
  vertical-align: -2px;
}
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
