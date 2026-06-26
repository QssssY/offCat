<template>
  <div class="admin-consumption-log">
    <!-- 类型筛选 -->
    <div class="admin-filter-bar">
      <el-select v-model="activeType" placeholder="全部类型" clearable size="small" style="width: 140px" @change="onFilterChange">
        <el-option label="全部类型" value="" />
        <el-option label="模拟面试" value="INTERVIEW" />
        <el-option label="简历诊断" value="RESUME" />
        <el-option label="AI润色" value="POLISH" />
        <el-option label="JD匹配" value="JD_MATCH" />
        <el-option label="模板库" value="TEMPLATE" />
        <el-option label="Offer" value="OFFER" />
      </el-select>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" style="padding: 20px 0; text-align: center">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 空状态 -->
    <el-empty v-else-if="records.length === 0" description="暂无消费记录" />

    <!-- 记录表格 -->
    <el-table v-else :data="records" stripe size="small">
      <el-table-column prop="quotaTypeName" label="类型" width="90" />
      <el-table-column label="变动" width="70" align="center">
        <template #default="{ row }">
          <span :style="{ color: row.changeAmount < 0 ? 'var(--el-color-success)' : 'var(--el-color-danger)', fontWeight: 600 }">
            {{ row.changeAmount > 0 ? '-' + row.changeAmount : '+' + Math.abs(row.changeAmount) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="balanceAfter" label="余额" width="60" align="center">
        <template #default="{ row }">{{ row.balanceAfter ?? '-' }}</template>
      </el-table-column>
      <el-table-column prop="sourceName" label="来源" width="100" />
      <el-table-column prop="description" label="描述" min-width="140" show-overflow-tooltip />
      <el-table-column label="时间" width="150">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="admin-log-pagination">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        small
        @current-change="fetchLogs"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { getAdminConsumptionLog } from '@/api/admin/users'

defineOptions({ name: 'AdminConsumptionLog' })

const props = defineProps({
  userId: { type: [String, Number], required: true }
})

const activeType = ref('')
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)
const records = ref([])
const loading = ref(false)

/** 获取消费记录 */
const fetchLogs = async () => {
  if (!props.userId) return
  loading.value = true
  try {
    const params = { pageNum: currentPage.value, pageSize }
    if (activeType.value) params.quotaType = activeType.value
    const res = await getAdminConsumptionLog(props.userId, params)
    const data = res.data
    records.value = data?.list || []
    total.value = data?.total || 0
  } catch {
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 类型筛选切换 */
const onFilterChange = () => {
  currentPage.value = 1
  fetchLogs()
}

/** 时间格式化 */
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

onMounted(() => {
  fetchLogs()
})

// 管理员切换用户时，重新加载该用户的消费记录
watch(() => props.userId, () => {
  currentPage.value = 1
  fetchLogs()
})
</script>

<style scoped>
.admin-consumption-log {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.admin-filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.admin-log-pagination {
  display: flex;
  justify-content: center;
  padding-top: 8px;
}
</style>
