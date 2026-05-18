<template>
  <div class="admin-page">
    <section class="page-header">
      <div>
        <h2 class="page-title">审计日志</h2>
        <p class="page-subtitle">查看管理员对用户权益的变更操作记录</p>
      </div>
      <el-button :loading="loading" @click="loadAuditLogs" class="refresh-btn">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
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
import { onMounted, ref } from 'vue'
import { ArrowRight, Refresh } from '@element-plus/icons-vue'
import { getAdminAuditLogs } from '@/api/admin/auditLogs'
import { formatDate } from '@/utils/date'

const loading = ref(false)
const errorMessage = ref('')
const logList = ref([])
const currentPage = ref(1)
const size = ref(20)
const total = ref(0)

const loadAuditLogs = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getAdminAuditLogs({ page: currentPage.value, size: size.value })
    const data = res?.data || {}
    logList.value = data.records || []
    total.value = Number(data.total || 0)
  } catch (e) {
    errorMessage.value = e?.message || '加载审计日志失败'
  } finally {
    loading.value = false
  }
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
.refresh-btn {
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%);
  border: none; border-radius: 12px; padding: 12px 24px; font-weight: 600;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 6px 20px rgba(230, 126, 34, 0.3); color: #fff;
}
.refresh-btn:hover { transform: translateY(-2px); box-shadow: 0 10px 28px rgba(230, 126, 34, 0.4); }
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

