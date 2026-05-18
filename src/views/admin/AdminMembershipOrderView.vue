<template>
  <div class="admin-page">
    <section class="page-header">
      <div>
        <h2 class="page-title">订单管理</h2>
        <p class="page-subtitle">查看会员套餐购买订单记录</p>
      </div>
      <el-select v-model="statusFilter" placeholder="订单状态" clearable @change="handleStatusFilterChange" style="width: 140px">
        <el-option label="全部" value="" />
        <el-option label="已支付" value="PAID" />
        <el-option label="已创建" value="CREATED" />
      </el-select>
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
import { onMounted, ref } from 'vue'
import { getAdminMembershipOrders } from '@/api/admin/membership'
import { formatDate } from '@/utils/date'

const loading = ref(false)
const errorMessage = ref('')
const orderList = ref([])
const currentPage = ref(1)
const size = ref(20)
const total = ref(0)
const statusFilter = ref('')

const loadOrders = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getAdminMembershipOrders(statusFilter.value || undefined, { page: currentPage.value, size: size.value })
    const data = res?.data || {}
    orderList.value = data.records || []
    total.value = Number(data.total || 0)
  } catch (e) {
    errorMessage.value = e?.message || '加载订单列表失败'
  } finally {
    loading.value = false
  }
}

const handleStatusFilterChange = () => {
  currentPage.value = 1
  loadOrders()
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

onMounted(loadOrders)
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

