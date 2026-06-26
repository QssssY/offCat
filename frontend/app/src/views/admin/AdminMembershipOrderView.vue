<template>
  <div class="admin-page">
    <section class="page-header">
      <div>
        <h2 class="page-title">订单管理</h2>
        <p class="page-subtitle">查看会员套餐购买订单记录</p>
      </div>
      <div class="header-actions">
        <el-button @click="handleExport" :disabled="orderList.length === 0" class="export-btn">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </div>
    </section>

    <!-- 筛选区域 -->
    <section class="filter-card">
      <el-form :inline="true" class="filter-form" @submit.prevent="handleSearch">
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
        <el-form-item label="用户名">
          <el-input v-model="filters.username" placeholder="用户名搜索" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="filters.orderStatus" placeholder="全部" clearable style="width: 130px">
            <el-option label="已支付" value="PAID" />
            <el-option label="已创建" value="CREATED" />
          </el-select>
        </el-form-item>
        <el-form-item label="套餐">
          <el-select v-model="filters.planName" placeholder="全部套餐" clearable filterable style="width: 160px">
            <el-option
              v-for="plan in planOptions"
              :key="plan.value"
              :label="plan.label"
              :value="plan.value"
            />
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
      <el-table :data="orderList" v-loading="loading" border stripe class="admin-data-table order-table" empty-text="暂无订单记录">
        <el-table-column prop="orderNo" label="订单号" min-width="280" show-overflow-tooltip>
          <template #header><div class="table-header">订单号</div></template>
        </el-table-column>
        <el-table-column prop="username" label="用户" min-width="120">
          <template #header><div class="table-header">用户</div></template>
        </el-table-column>
        <el-table-column prop="planName" label="套餐" min-width="150" show-overflow-tooltip>
          <template #header><div class="table-header">套餐</div></template>
        </el-table-column>
        <el-table-column prop="orderAmount" label="金额" min-width="100" align="right">
          <template #header><div class="table-header">金额</div></template>
          <template #default="{ row }">¥{{ row.orderAmount }}</template>
        </el-table-column>
        <el-table-column prop="orderStatus" label="状态" min-width="100" align="center">
          <template #header><div class="table-header">状态</div></template>
          <template #default="{ row }">
            <el-tag :type="row.orderStatus === 'PAID' ? 'success' : 'info'" size="small" effect="plain">
              {{ row.orderStatus === 'PAID' ? '已支付' : '已创建' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="durationDays" label="天数" min-width="90" align="center">
          <template #header><div class="table-header">天数</div></template>
        </el-table-column>
        <el-table-column prop="grantedResumeQuota" label="简历额度" min-width="110" align="center">
          <template #header><div class="table-header">简历额度</div></template>
        </el-table-column>
        <el-table-column prop="grantedInterviewQuota" label="面试额度" min-width="110" align="center">
          <template #header><div class="table-header">面试额度</div></template>
        </el-table-column>
        <el-table-column label="支付时间" min-width="190" show-overflow-tooltip>
          <template #header><div class="table-header">支付时间</div></template>
          <template #default="{ row }">{{ formatDate(row.paidAt) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="190" show-overflow-tooltip>
          <template #header><div class="table-header">创建时间</div></template>
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
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getAdminMembershipOrders, getAdminMembershipPlans } from '@/api/admin/membership'
import { formatDate } from '@/utils/date'
import { exportToXlsx } from '@/utils/export'

const loading = ref(false)
const errorMessage = ref('')
const orderList = ref([])
const currentPage = ref(1)
const size = ref(20)
const total = ref(0)
const planOptions = ref([])

// 筛选状态
const filters = reactive({
  dateRange: null,
  username: '',
  orderStatus: '',
  planName: ''
})

// 加载套餐选项
const loadPlanOptions = async () => {
  try {
    const res = await getAdminMembershipPlans({ page: 1, size: 100 })
    planOptions.value = (res?.data?.records || []).map(p => ({
      label: p.planName,
      value: p.planName
    }))
  } catch {
    // 忽略套餐选项加载失败
  }
}

// 构建查询参数
const buildQuery = () => {
  const query = { page: currentPage.value, size: size.value }
  if (filters.orderStatus) query.orderStatus = filters.orderStatus
  if (filters.username) query.username = filters.username.trim()
  if (filters.planName) query.planName = filters.planName
  if (Array.isArray(filters.dateRange) && filters.dateRange.length === 2) {
    query.startDate = filters.dateRange[0]
    query.endDate = filters.dateRange[1]
  }
  return query
}

const loadOrders = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getAdminMembershipOrders(buildQuery())
    const data = res?.data || {}
    orderList.value = data.records || []
    total.value = Number(data.total || 0)
  } catch (e) {
    errorMessage.value = e?.message || '加载订单列表失败'
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadOrders()
}

const resetFilters = () => {
  filters.dateRange = null
  filters.username = ''
  filters.orderStatus = ''
  filters.planName = ''
  currentPage.value = 1
  loadOrders()
}

// XLSX 导出：等待按需加载和文件生成完成后再提示成功，避免首次导出时提示早于下载。
const handleExport = async () => {
  if (orderList.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  const headers = ['订单号', '用户', '套餐', '金额', '状态', '天数', '简历额度', '面试额度', '支付时间', '创建时间']
  const rows = orderList.value.map(row => [
    row.orderNo,
    row.username,
    row.planName,
    row.orderAmount != null ? `¥${row.orderAmount}` : '',
    row.orderStatus === 'PAID' ? '已支付' : '已创建',
    row.durationDays,
    row.grantedResumeQuota,
    row.grantedInterviewQuota,
    row.paidAt ? formatDate(row.paidAt) : '',
    formatDate(row.createTime)
  ])
  await exportToXlsx({
    headers,
    rows,
    filename: `订单管理_${new Date().toISOString().slice(0, 10)}`,
    sheetName: '订单管理'
  })
  ElMessage.success('导出成功')
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadOrders()
}

const handlePageSizeChange = (pageSize) => {
  size.value = pageSize
  currentPage.value = 1
  loadOrders()
}

onMounted(() => {
  loadOrders()
  loadPlanOptions()
})
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
  min-width: 1340px;
}

.admin-data-table :deep(.el-table__cell .cell) {
  white-space: nowrap;
}
.table-header {
  font-weight: 700;
  color: #5a4030;
}
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
