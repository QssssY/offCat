<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">用户与权益管理</h2>
        <p class="page-subtitle">查看用户基础信息、会员权益状态，并支持管理员手工调整</p>
      </div>
      <div class="header-actions">
        <el-input
          v-model.trim="keyword"
          class="search-input"
          placeholder="按用户名或用户ID搜索"
          clearable
        >
          <template #prefix>
            <el-icon class="search-icon"><Search /></el-icon>
          </template>
        </el-input>
        <el-button :loading="tableLoading" class="refresh-btn" @click="fetchUserList">刷新列表</el-button>
        <el-button
          v-if="hasSelectedUsers"
          type="warning"
          @click="handleBatchDisable"
        >
          批量封禁
        </el-button>
        <el-button
          v-if="hasSelectedUsers"
          type="success"
          @click="handleBatchEnable"
        >
          批量解封
        </el-button>
        <el-button @click="handleSelectAll">
          全部勾选
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-select v-model="filterForm.role" class="filter-select" placeholder="角色筛选">
          <el-option label="全部角色" value="all" />
          <el-option label="普通用户" value="0" />
          <el-option label="会员用户" value="1" />
          <el-option label="管理员" value="9" />
        </el-select>
        <el-select v-model="filterForm.status" class="filter-select" placeholder="状态筛选">
          <el-option label="全部状态" value="all" />
          <el-option label="正常" value="1" />
          <el-option label="封禁" value="0" />
        </el-select>
        <el-select v-model="filterForm.vipState" class="filter-select" placeholder="会员状态筛选">
          <el-option label="全部会员状态" value="all" />
          <el-option label="会员有效" value="active" />
          <el-option label="会员过期" value="expired" />
          <el-option label="非会员" value="non-vip" />
        </el-select>
        <el-button class="reset-btn" @click="resetFilters">重置筛选</el-button>
        <el-button class="export-btn" @click="exportFilteredUsersCsv">导出当前筛选</el-button>
      </div>
      <div class="filter-result">
        当前筛选结果：<span class="filter-count">{{ filteredUsers.length }}</span> / {{ userList.length }} 人
      </div>
    </el-card>

    <div class="stats-grid">
      <button
        class="stats-card"
        :class="{ active: matchedQuickFilterKey === 'all' }"
        @click="applyQuickFilter('all')"
      >
        <span class="stats-label">全部用户</span>
        <strong class="stats-value">{{ userStats.total }}</strong>
      </button>
      <button
        class="stats-card"
        :class="{ active: matchedQuickFilterKey === 'status-enabled' }"
        @click="applyQuickFilter('status-enabled')"
      >
        <span class="stats-label">正常用户</span>
        <strong class="stats-value">{{ userStats.enabled }}</strong>
      </button>
      <button
        class="stats-card"
        :class="{ active: matchedQuickFilterKey === 'status-disabled' }"
        @click="applyQuickFilter('status-disabled')"
      >
        <span class="stats-label">封禁用户</span>
        <strong class="stats-value">{{ userStats.disabled }}</strong>
      </button>
      <button
        class="stats-card"
        :class="{ active: matchedQuickFilterKey === 'vip-active' }"
        @click="applyQuickFilter('vip-active')"
      >
        <span class="stats-label">会员有效</span>
        <strong class="stats-value">{{ userStats.vipActive }}</strong>
      </button>
      <button
        class="stats-card"
        :class="{ active: matchedQuickFilterKey === 'vip-expired' }"
        @click="applyQuickFilter('vip-expired')"
      >
        <span class="stats-label">会员过期</span>
        <strong class="stats-value">{{ userStats.vipExpired }}</strong>
      </button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table
        ref="userTableRef"
        :data="pagedUsers"
        v-loading="tableLoading"
        border
        stripe
        :empty-text="tableEmptyText"
        class="user-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="_userId" label="用户ID" min-width="180" align="center">
          <template #header>
            <div class="table-header">用户ID</div>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="150" align="center">
          <template #header>
            <div class="table-header">用户名</div>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" min-width="150" align="center">
          <template #header>
            <div class="table-header">昵称</div>
          </template>
          <template #default="{ row }">
            {{ row.nickname || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="roleDesc" label="角色" width="120" align="center">
          <template #header>
            <div class="table-header">角色</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #header>
            <div class="table-header">状态</div>
          </template>
          <template #default="{ row }">
            <el-tag
              :type="isEnabledUser(row) ? 'success' : 'danger'"
              effect="plain"
              size="small"
              class="status-tag"
            >
              {{ isEnabledUser(row) ? '正常' : '封禁' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="会员到期时间" min-width="180" align="center">
          <template #header>
            <div class="table-header">会员到期时间</div>
          </template>
          <template #default="{ row }">
            {{ formatDateTime(row.vipExpireTime) }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180" align="center">
          <template #header>
            <div class="table-header">创建时间</div>
          </template>
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="500" fixed="right" align="center">
          <template #header>
            <div class="table-header">操作</div>
          </template>
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" @click="openRightsDrawer(row)" class="action-btn view">
                权益详情
              </el-button>
              <el-button size="small" @click="openUserDataDrawer(row)" class="action-btn view">
                用户数据
              </el-button>
              <el-button size="small" @click="openEditDialog(row)" class="action-btn edit">
                编辑权益
              </el-button>
              <el-button size="small" @click="openQuotaDialog(row)" class="action-btn quota">
                调整额度
              </el-button>
              <el-button
                size="small"
                @click="handleToggleStatus(row)"
                class="action-btn"
              >
                {{ isEnabledUser(row) ? '禁用' : '启用' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="filteredUsers.length"
          :current-page="pagination.page"
          :page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>

    <el-drawer v-model="rightsDrawerVisible" title="用户详情" size="520px" class="rights-drawer">
      <el-tabs v-model="activeUserTab">
        <el-tab-pane label="权益详情" name="rights">
          <el-skeleton v-if="rightsLoading" :rows="8" animated />
          <el-descriptions v-else :column="1" border class="rights-descriptions">
            <el-descriptions-item label="用户ID">{{ rightsData.userId }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ rightsData.username }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ rightsData.nickname || '-' }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ rightsData.roleDesc }}</el-descriptions-item>
            <el-descriptions-item label="套餐编码">{{ rightsData.membershipPlanCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="会员到期">
              {{ formatDateTime(rightsData.vipExpireTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="会员有效">{{ rightsData.isVipActive ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="简历剩余额度">{{ rightsData.resumeQuota }}</el-descriptions-item>
            <el-descriptions-item label="面试剩余额度">{{ rightsData.interviewQuota }}</el-descriptions-item>
            <el-descriptions-item label="今日简历已用">{{ rightsData.dailyResumeUsed }}</el-descriptions-item>
            <el-descriptions-item label="今日面试已用">{{ rightsData.dailyInterviewUsed }}</el-descriptions-item>
            <el-descriptions-item label="累计简历已用">{{ rightsData.totalResumeUsed }}</el-descriptions-item>
            <el-descriptions-item label="累计面试已用">{{ rightsData.totalInterviewUsed }}</el-descriptions-item>
            <el-descriptions-item label="最近刷新日期">{{ rightsData.lastRefreshDate || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="面试记录" name="interviews">
          <el-skeleton v-if="userDataLoading" :rows="5" animated />
          <template v-else-if="userInterviews.length === 0">
            <el-empty description="暂无面试记录" />
          </template>
          <el-table v-else :data="userInterviews" stripe size="small">
            <el-table-column prop="jobRole" label="岗位" width="100" />
            <el-table-column prop="difficultyDesc" label="难度" width="60" />
            <el-table-column prop="statusDesc" label="状态" width="70">
              <template #default="{ row }">
                <el-tag :type="row.statusDesc === '已结束' ? 'success' : 'warning'" size="small">{{ row.statusDesc }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="comprehensiveScore" label="评分" width="60" />
            <el-table-column label="时间" width="160">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="简历诊断" name="resumeTasks">
          <el-skeleton v-if="userDataLoading" :rows="5" animated />
          <template v-else-if="userResumeTasks.length === 0">
            <el-empty description="暂无简历诊断记录" />
          </template>
          <el-table v-else :data="userResumeTasks" stripe size="small">
            <el-table-column prop="id" label="任务ID" width="80" />
            <el-table-column prop="statusDesc" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.statusDesc === '已完成' ? 'success' : row.statusDesc === '失败' ? 'danger' : 'warning'" size="small">{{ row.statusDesc }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="errorMsg" label="错误信息" min-width="150" show-overflow-tooltip />
            <el-table-column label="时间" width="160">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>

    <el-dialog
      v-model="quotaDialogVisible"
      title="调整用户额度"
      width="640px"
      destroy-on-close
      class="quota-dialog"
    >
      <el-skeleton v-if="quotaLoading" :rows="6" animated />
      <el-form
        v-else
        ref="quotaFormRef"
        :model="quotaForm"
        :rules="quotaRules"
        label-width="140px"
      >
        <el-alert type="info" :closable="false" class="quota-alert">
          <template #title>
            当前用户：{{ quotaForm.username || '-' }}（{{ quotaForm.userId || '-' }}）
          </template>
        </el-alert>

        <el-form-item label="累计面试已使用" prop="totalInterviewUsed">
          <el-input-number
            v-model="quotaForm.totalInterviewUsed"
            :min="0"
            :step="1"
            step-strictly
            :precision="0"
            controls-position="right"
            style="width: 100%"
            :disabled="quotaSaving"
          />
        </el-form-item>

        <el-form-item label="累计简历已使用" prop="totalResumeUsed">
          <el-input-number
            v-model="quotaForm.totalResumeUsed"
            :min="0"
            :step="1"
            step-strictly
            :precision="0"
            controls-position="right"
            style="width: 100%"
            :disabled="quotaSaving"
          />
        </el-form-item>

        <el-form-item label="今日面试已使用" prop="dailyInterviewUsed">
          <el-input-number
            v-model="quotaForm.dailyInterviewUsed"
            :min="0"
            :step="1"
            step-strictly
            :precision="0"
            controls-position="right"
            style="width: 100%"
            :disabled="quotaSaving"
          />
        </el-form-item>

        <el-form-item label="今日简历已使用" prop="dailyResumeUsed">
          <el-input-number
            v-model="quotaForm.dailyResumeUsed"
            :min="0"
            :step="1"
            step-strictly
            :precision="0"
            controls-position="right"
            style="width: 100%"
            :disabled="quotaSaving"
          />
        </el-form-item>

        <el-form-item label="面试剩余额度" prop="interviewQuota">
          <el-input-number
            v-model="quotaForm.interviewQuota"
            :min="0"
            :step="1"
            step-strictly
            :precision="0"
            controls-position="right"
            style="width: 100%"
            :disabled="quotaSaving"
          />
        </el-form-item>

        <el-form-item label="简历剩余额度" prop="resumeQuota">
          <el-input-number
            v-model="quotaForm.resumeQuota"
            :min="0"
            :step="1"
            step-strictly
            :precision="0"
            controls-position="right"
            style="width: 100%"
            :disabled="quotaSaving"
          />
        </el-form-item>

        <el-form-item label="最后刷新日期" prop="lastRefreshDate">
          <el-date-picker
            v-model="quotaForm.lastRefreshDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择最后刷新日期"
            style="width: 100%"
            :disabled="quotaSaving"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="quotaDialogVisible = false" class="dialog-btn">取消</el-button>
        <el-button
          type="primary"
          :loading="quotaSaving"
          :disabled="quotaLoading"
          @click="submitQuotaEdit"
          class="dialog-btn primary"
        >
          保存额度
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="editDialogVisible"
      title="编辑用户权益"
      width="620px"
      destroy-on-close
      class="edit-dialog"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="110px">
        <el-form-item label="目标角色" prop="role">
          <el-radio-group v-model="editForm.role" :disabled="editLoading">
            <el-radio :value="0">普通用户</el-radio>
            <el-radio :value="1">会员用户</el-radio>
            <el-radio :value="9">管理员</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="套餐编码">
          <el-select
            v-model="editForm.membershipPlanCode"
            clearable
            filterable
            style="width: 100%"
            placeholder="会员角色下可选择套餐"
            :disabled="editLoading || editForm.role !== 1"
          >
            <el-option
              v-for="plan in membershipPlans"
              :key="plan.planCode"
              :label="`${plan.planName || plan.planCode}（${plan.planCode}）`"
              :value="plan.planCode"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="会员到期时间">
          <el-date-picker
            v-model="editForm.vipExpireTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="请选择到期时间"
            style="width: 100%"
            :disabled="editLoading || editForm.role !== 1"
          />
        </el-form-item>

        <el-form-item label="修改备注">
          <el-input
            v-model.trim="editForm.remark"
            type="textarea"
            :rows="3"
            placeholder="可选，建议填写调整原因"
            :disabled="editLoading"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false" class="dialog-btn">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="submitEdit" class="dialog-btn primary">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import {
  getAdminUserRights,
  getAdminUserQuota,
  getAdminUsers,
  getMembershipPlansForAdmin,
  updateAdminUserRights,
  updateAdminUserQuota,
  updateAdminUserStatus,
  updateUsersBatchStatus,
  normalizeUserId
} from '@/api/admin/users'
import { getAdminUserInterviews, getAdminUserResumeTasks } from '@/api/admin/userData'
import {
  confirmAdminRiskAction,
  resolveAdminTableEmptyText,
  showAdminError,
  showAdminSuccess,
  showAdminWarning
} from '@/utils/adminFeedback'

// 用户列表基础状态：承载用户管理主表格。
const userList = ref([])
const tableLoading = ref(false)
// 表格实例：用于全选操作
const userTableRef = ref(null)
// 批量选择状态：用于批量操作
const selectedUsers = ref([])
const hasSelectedUsers = computed(() => selectedUsers.value && selectedUsers.value.length > 0)
const keyword = ref('')
const filterForm = reactive({
  role: 'all',
  status: 'all',
  vipState: 'all'
})
const pagination = reactive({
  page: 1,
  pageSize: 10
})

// 权益详情抽屉状态：点击“查看权益”后加载展示。
const rightsDrawerVisible = ref(false)
const rightsLoading = ref(false)
const rightsData = reactive({
  userId: '',
  username: '',
  roleDesc: '',
  membershipPlanCode: '',
  vipExpireTime: '',
  isVipActive: false,
  resumeQuota: 0,
  interviewQuota: 0,
  dailyResumeUsed: 0,
  dailyInterviewUsed: 0,
  totalResumeUsed: 0,
  totalInterviewUsed: 0,
  lastRefreshDate: ''
})

// 权益编辑状态：用于管理员手工调整 role/套餐/到期时间。
const selectedUserId = ref('')
const editDialogVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref(null)
const editForm = reactive({
  role: 0,
  membershipPlanCode: '',
  vipExpireTime: '',
  remark: ''
})

// 会员套餐选项：用于权益编辑时选择合法套餐编码。
const membershipPlans = ref([])

// 额度编辑状态：用于管理员修正累计与当日消耗值。
const quotaDialogVisible = ref(false)
const quotaLoading = ref(false)
const quotaSaving = ref(false)
const quotaFormRef = ref(null)
const quotaForm = reactive({
  userId: '',
  username: '',
  totalInterviewUsed: 0,
  totalResumeUsed: 0,
  dailyInterviewUsed: 0,
  dailyResumeUsed: 0,
  lastRefreshDate: ''
})

/**
 * 校验非负整数输入。
 * 作用：防止管理员误填负数或小数，避免提交后破坏额度统计口径。
 * @param {any} _rule
 * @param {number | string | null | undefined} value
 * @param {(error?: Error) => void} callback
 */
const validateNonNegativeInteger = (_rule, value, callback) => {
  const numericValue = Number(value)
  if (!Number.isFinite(numericValue)) {
    callback(new Error('请输入有效数字'))
    return
  }
  if (numericValue < 0 || !Number.isInteger(numericValue)) {
    callback(new Error('请输入大于等于 0 的整数'))
    return
  }
  callback()
}

const editRules = {
  role: [{ required: true, message: '请选择目标角色', trigger: 'change' }]
}

const quotaRules = {
  totalInterviewUsed: [{ validator: validateNonNegativeInteger, trigger: 'change' }],
  totalResumeUsed: [{ validator: validateNonNegativeInteger, trigger: 'change' }],
  dailyInterviewUsed: [{ validator: validateNonNegativeInteger, trigger: 'change' }],
  dailyResumeUsed: [{ validator: validateNonNegativeInteger, trigger: 'change' }]
}

/**
 * 从用户行中读取可用于接口调用的 userId。
 * 作用：统一走 `_userId` 字段，避免各处拼接时产生格式污染。
 * @param {Record<string, any>} row
 * @returns {string}
 */
const readUserId = (row) => {
  if (!row) return ''
  return row._userId || normalizeUserId(row.id)
}

/**
 * 判断用户是否为正常状态。
 * @param {Record<string, any>} row
 * @returns {boolean}
 */
const isEnabledUser = (row) => Number(row?.status) === 1

/**
 * 计算用户会员状态标签。
 * 作用：统一会员有效/过期判断口径，避免筛选与展示标准不一致。
 * @param {Record<string, any>} row
 * @returns {'active' | 'expired' | 'non-vip'}
 */
const getVipState = (row) => {
  if (Number(row?.role) !== 1) return 'non-vip'
  if (!row?.vipExpireTime) return 'expired'
  const expireDate = new Date(row.vipExpireTime)
  if (Number.isNaN(expireDate.getTime())) return 'expired'
  return expireDate.getTime() >= Date.now() ? 'active' : 'expired'
}

/**
 * 过滤后的用户列表。
 * 作用：支持关键词、角色、状态、会员状态的多维组合筛选。
 */
const filteredUsers = computed(() => {
  return userList.value.filter((item) => {
    const matchesKeyword = !keyword.value
      || item.username?.includes(keyword.value)
      || item._userId?.includes(keyword.value)

    const matchesRole = filterForm.role === 'all' || String(item.role) === filterForm.role
    const matchesStatus = filterForm.status === 'all' || String(item.status) === filterForm.status
    const matchesVipState = filterForm.vipState === 'all' || getVipState(item) === filterForm.vipState

    return matchesKeyword && matchesRole && matchesStatus && matchesVipState
  })
})

/**
 * 当前页用户列表。
 * 作用：在前端做轻量分页，避免当前后端接口改造成本。
 */
const pagedUsers = computed(() => {
  const startIndex = (pagination.page - 1) * pagination.pageSize
  const endIndex = startIndex + pagination.pageSize
  return filteredUsers.value.slice(startIndex, endIndex)
})

/**
 * 表格空状态文案。
 * 作用：统一“全量为空”和“筛选为空”展示语气，减少误判。
 */
const tableEmptyText = computed(() => {
  return resolveAdminTableEmptyText(userList.value.length, filteredUsers.value.length)
})

/**
 * 用户统计概览。
 * 作用：给管理员提供一眼可见的用户分布，快速定位目标群体。
 */
const userStats = computed(() => {
  return userList.value.reduce(
    (summary, item) => {
      summary.total += 1
      if (isEnabledUser(item)) {
        summary.enabled += 1
      } else {
        summary.disabled += 1
      }

      const vipState = getVipState(item)
      if (vipState === 'active') summary.vipActive += 1
      if (vipState === 'expired') summary.vipExpired += 1
      return summary
    },
    { total: 0, enabled: 0, disabled: 0, vipActive: 0, vipExpired: 0 }
  )
})

/**
 * 当前筛选对应的快捷筛选标识。
 * 作用：让统计卡片高亮状态与筛选条件保持一致。
 */
const matchedQuickFilterKey = computed(() => {
  if (filterForm.role === 'all' && filterForm.status === 'all' && filterForm.vipState === 'all') {
    return 'all'
  }
  if (filterForm.role === 'all' && filterForm.status === '1' && filterForm.vipState === 'all') {
    return 'status-enabled'
  }
  if (filterForm.role === 'all' && filterForm.status === '0' && filterForm.vipState === 'all') {
    return 'status-disabled'
  }
  if (filterForm.role === '1' && filterForm.status === 'all' && filterForm.vipState === 'active') {
    return 'vip-active'
  }
  if (filterForm.role === '1' && filterForm.status === 'all' && filterForm.vipState === 'expired') {
    return 'vip-expired'
  }
  return 'custom'
})

/**
 * 日期时间格式化。
 * @param {string | null} value
 * @returns {string}
 */
// 用户数据抽屉状态
const activeUserTab = ref('rights')
const currentDataUserId = ref('')
const userDataLoading = ref(false)
const userInterviews = ref([])
const userResumeTasks = ref([])

const openUserDataDrawer = async (row) => {
  const userId = readUserId(row)
  if (!userId) return
  currentDataUserId.value = userId
  activeUserTab.value = 'interviews'
  rightsDrawerVisible.value = true
  rightsLoading.value = false
  await Promise.all([fetchUserInterviews(userId), fetchUserResumeTasks(userId)])
}

const fetchUserInterviews = async (userId) => {
  userDataLoading.value = true
  try {
    const res = await getAdminUserInterviews(userId)
    userInterviews.value = res?.data?.records || []
  } catch { userInterviews.value = [] }
  finally { userDataLoading.value = false }
}

const fetchUserResumeTasks = async (userId) => {
  userDataLoading.value = true
  try {
    const res = await getAdminUserResumeTasks(userId)
    userResumeTasks.value = res?.data?.records || []
  } catch { userResumeTasks.value = [] }
  finally { userDataLoading.value = false }
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

/**
 * 将任意输入转换为非负整数。
 * 作用：接口返回空值时兜底为 0，避免数字组件显示异常。
 * @param {number | string | null | undefined} value
 * @returns {number}
 */
const toSafeNonNegativeInteger = (value) => {
  const numericValue = Number(value)
  if (!Number.isFinite(numericValue) || numericValue < 0) return 0
  return Math.floor(numericValue)
}

/**
 * 拉取用户列表。
 * 关键逻辑：在页面层为每行补充 `_userId`，后续接口一律使用该字符串值。
 */
const fetchUserList = async () => {
  tableLoading.value = true
  try {
    const res = await getAdminUsers()
    const list = Array.isArray(res?.data) ? res.data : []
    userList.value = list.map((item) => ({
      ...item,
      _userId: normalizeUserId(item.userId ?? item.id)
    }))
  } catch (error) {
    showAdminError(error?.message || '加载用户列表失败')
  } finally {
    tableLoading.value = false
  }
}

/**
 * 重置筛选条件。
 * 作用：快速回到全量视图，降低管理员多轮筛选后的恢复成本。
 */
const resetFilters = () => {
  keyword.value = ''
  filterForm.role = 'all'
  filterForm.status = 'all'
  filterForm.vipState = 'all'
}

/**
 * 应用统计卡片快捷筛选。
 * 作用：通过单击卡片快速套用高频筛选组合，减少重复操作。
 * @param {'all' | 'status-enabled' | 'status-disabled' | 'vip-active' | 'vip-expired'} key
 */
const applyQuickFilter = (key) => {
  if (key === 'all') {
    filterForm.role = 'all'
    filterForm.status = 'all'
    filterForm.vipState = 'all'
    return
  }

  if (key === 'status-enabled') {
    filterForm.role = 'all'
    filterForm.status = '1'
    filterForm.vipState = 'all'
    return
  }

  if (key === 'status-disabled') {
    filterForm.role = 'all'
    filterForm.status = '0'
    filterForm.vipState = 'all'
    return
  }

  if (key === 'vip-active') {
    filterForm.role = '1'
    filterForm.status = 'all'
    filterForm.vipState = 'active'
    return
  }

  if (key === 'vip-expired') {
    filterForm.role = '1'
    filterForm.status = 'all'
    filterForm.vipState = 'expired'
  }
}

/**
 * 分页页码变更回调。
 * @param {number} nextPage
 */
const handlePageChange = (nextPage) => {
  pagination.page = nextPage
}

/**
 * 分页条数变更回调。
 * @param {number} nextPageSize
 */
const handlePageSizeChange = (nextPageSize) => {
  pagination.pageSize = nextPageSize
  pagination.page = 1
}

/**
 * 转义 CSV 字段内容。
 * 作用：防止逗号、换行、双引号导致导出结构错乱。
 * @param {string | number | null | undefined} value
 * @returns {string}
 */
const escapeCsvCell = (value) => {
  const cellText = String(value ?? '')
  return `"${cellText.replaceAll('"', '""')}"`
}

/**
 * 导出当前筛选结果为 CSV。
 * 作用：让运营可直接下载并离线分析筛选后的用户集。
 */
const exportFilteredUsersCsv = () => {
  if (!filteredUsers.value.length) {
    showAdminWarning('当前筛选结果为空，无法导出')
    return
  }

  const headers = ['用户ID', '用户名', '角色', '状态', '会员到期时间', '会员状态', '创建时间']
  const rows = filteredUsers.value.map((item) => {
    const vipState = getVipState(item)
    return [
      item._userId || '',
      item.username || '',
      item.roleDesc || '',
      isEnabledUser(item) ? '正常' : '封禁',
      formatDateTime(item.vipExpireTime),
      vipState === 'active' ? '会员有效' : (vipState === 'expired' ? '会员过期' : '非会员'),
      formatDateTime(item.createTime)
    ]
  })

  const csvContent = [headers, ...rows]
    .map((row) => row.map((cell) => escapeCsvCell(cell)).join(','))
    .join('\n')

  const blob = new Blob([`\uFEFF${csvContent}`], { type: 'text/csv;charset=utf-8;' })
  const downloadLink = document.createElement('a')
  const now = new Date()
  const fileName = `admin_users_export_${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}.csv`

  downloadLink.href = URL.createObjectURL(blob)
  downloadLink.download = fileName
  document.body.appendChild(downloadLink)
  downloadLink.click()
  document.body.removeChild(downloadLink)
  URL.revokeObjectURL(downloadLink.href)
  showAdminSuccess('导出成功')
}

/**
 * 拉取会员套餐列表。
 */
const fetchMembershipPlans = async () => {
  try {
    const res = await getMembershipPlansForAdmin()
    membershipPlans.value = Array.isArray(res?.data) ? res.data : []
  } catch (error) {
    showAdminError(error?.message || '加载会员套餐失败')
  }
}

/**
 * 拉取并展示某个用户的权益详情。
 * @param {string} userId
 */
const fetchRightsByUserId = async (userId) => {
  rightsLoading.value = true
  try {
    const res = await getAdminUserRights(userId)
    Object.assign(rightsData, res?.data || {})
  } catch (error) {
    showAdminError(error?.message || '加载用户权益详情失败')
  } finally {
    rightsLoading.value = false
  }
}

const openRightsDrawer = async (row) => {
  const userId = readUserId(row)
  if (!userId) {
    showAdminError('用户ID无效，无法查询权益')
    return
  }
  rightsDrawerVisible.value = true
  await fetchRightsByUserId(userId)
}

/**
 * 打开额度编辑弹窗。
 * 关键逻辑：每次都从后端实时拉取额度，避免编辑基于旧缓存。
 * @param {Record<string, any>} row
 */
const openQuotaDialog = async (row) => {
  const userId = readUserId(row)
  if (!userId) {
    showAdminError('用户ID无效，无法调整额度')
    return
  }

  quotaDialogVisible.value = true
  quotaLoading.value = true

  try {
    const res = await getAdminUserQuota(userId)
    const data = res?.data || {}
    quotaForm.userId = userId
    quotaForm.username = data.username || row.username || ''
    quotaForm.totalInterviewUsed = toSafeNonNegativeInteger(data.totalInterviewUsed)
    quotaForm.totalResumeUsed = toSafeNonNegativeInteger(data.totalResumeUsed)
    quotaForm.dailyInterviewUsed = toSafeNonNegativeInteger(data.dailyInterviewUsed)
    quotaForm.dailyResumeUsed = toSafeNonNegativeInteger(data.dailyResumeUsed)
    quotaForm.interviewQuota = toSafeNonNegativeInteger(data.interviewQuota)
    quotaForm.resumeQuota = toSafeNonNegativeInteger(data.resumeQuota)
    quotaForm.lastRefreshDate = data.lastRefreshDate || ''
  } catch (error) {
    showAdminError(error?.message || '加载用户额度失败')
    quotaDialogVisible.value = false
  } finally {
    quotaLoading.value = false
  }
}

/**
 * 打开权益编辑弹窗。
 * 关键逻辑：先调用权益详情接口，确保编辑基于后端实时数据。
 * @param {Record<string, any>} row
 */
const openEditDialog = async (row) => {
  const userId = readUserId(row)
  if (!userId) {
    showAdminError('用户ID无效，无法编辑权益')
    return
  }

  selectedUserId.value = userId
  editDialogVisible.value = true
  editLoading.value = true

  try {
    const res = await getAdminUserRights(userId)
    const data = res?.data || {}
    editForm.role = Number(data.role ?? 0)
    editForm.membershipPlanCode = data.membershipPlanCode || ''
    editForm.vipExpireTime = data.vipExpireTime || ''
    editForm.remark = ''
  } catch (error) {
    showAdminError(error?.message || '加载用户权益数据失败')
  } finally {
    editLoading.value = false
  }
}

/**
 * 提交权益编辑。
 * 关键逻辑：
 * 1. 非会员角色时主动清空套餐与到期时间。
 * 2. userId 全流程使用字符串，避免超长 ID 精度丢失。
 */
const submitEdit = async () => {
  if (!selectedUserId.value || !editFormRef.value) return
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return

  editLoading.value = true
  try {
    const payload = {
      role: editForm.role,
      membershipPlanCode: editForm.role === 1 ? (editForm.membershipPlanCode || null) : null,
      vipExpireTime: editForm.role === 1 ? (editForm.vipExpireTime || null) : null,
      remark: editForm.remark || null
    }

    await updateAdminUserRights(selectedUserId.value, payload)
    showAdminSuccess('用户权益更新成功')
    editDialogVisible.value = false
    await fetchUserList()
    if (rightsDrawerVisible.value) {
      await fetchRightsByUserId(selectedUserId.value)
    }
  } catch (error) {
    showAdminError(error?.message || '更新用户权益失败')
  } finally {
    editLoading.value = false
  }
}

/**
 * 提交额度编辑。
 * 关键逻辑：统一提交非负整数字段，保持后端额度统计字段可预测。
 */
const submitQuotaEdit = async () => {
  if (!quotaForm.userId || !quotaFormRef.value) return

  const valid = await quotaFormRef.value.validate().catch(() => false)
  if (!valid) return

  quotaSaving.value = true
  try {
    const payload = {
      userId: quotaForm.userId,
      totalInterviewUsed: toSafeNonNegativeInteger(quotaForm.totalInterviewUsed),
      totalResumeUsed: toSafeNonNegativeInteger(quotaForm.totalResumeUsed),
      dailyInterviewUsed: toSafeNonNegativeInteger(quotaForm.dailyInterviewUsed),
      dailyResumeUsed: toSafeNonNegativeInteger(quotaForm.dailyResumeUsed),
      interviewQuota: toSafeNonNegativeInteger(quotaForm.interviewQuota),
      resumeQuota: toSafeNonNegativeInteger(quotaForm.resumeQuota),
      lastRefreshDate: quotaForm.lastRefreshDate || null
    }

    await updateAdminUserQuota(payload)
    showAdminSuccess('用户额度更新成功')
    quotaDialogVisible.value = false

    // 如果当前已打开权益抽屉且目标用户一致，提交后同步刷新展示数据。
    if (rightsDrawerVisible.value && rightsData.userId && String(rightsData.userId) === quotaForm.userId) {
      await fetchRightsByUserId(quotaForm.userId)
    }
  } catch (error) {
    showAdminError(error?.message || '更新用户额度失败')
  } finally {
    quotaSaving.value = false
  }
}

/**
 * 切换用户状态（正常/封禁）。
 * @param {Record<string, any>} row
 */
const handleToggleStatus = async (row) => {
  const userId = readUserId(row)
  if (!userId) {
    showAdminError('用户ID无效，无法更新状态')
    return
  }

  const nextStatus = isEnabledUser(row) ? 0 : 1
  const actionText = nextStatus === 1 ? '解封' : '封禁'
  try {
    await confirmAdminRiskAction({
      title: `${actionText}确认`,
      actionText: `${actionText}用户`,
      targetName: row.username,
      impactHint: nextStatus === 0
        ? '封禁后该用户将无法继续使用核心功能，请确认已完成风险评估。'
        : '解封后该用户将恢复访问能力，请确认账号状态已核验。',
      type: 'warning'
    })
    await updateAdminUserStatus(userId, nextStatus)
    showAdminSuccess(`用户已${actionText}`)
    await fetchUserList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || `${actionText}用户失败`)
    }
  }
}

/**
 * 处理表格选择变化。
 * @param {Array} selection 选中的行数据
 */
const handleSelectionChange = (selection) => {
  selectedUsers.value = selection
}

/**
 * 全部勾选当前筛选结果。
 */
const handleSelectAll = () => {
  if (userTableRef.value) {
    userTableRef.value.toggleAllSelection()
  }
}

/**
 * 批量封禁用户。
 */
const handleBatchDisable = async () => {
  if (!selectedUsers.value || selectedUsers.value.length === 0) {
    showAdminWarning('请先选择要封禁的用户')
    return
  }

  try {
    await confirmAdminRiskAction({
      title: '批量封禁确认',
      actionText: '封禁选中的用户',
      targetName: `${selectedUsers.value.length} 个用户`,
      impactHint: '封禁后这些用户将无法继续使用核心功能，请确认已完成风险评估。',
      type: 'warning'
    })
    const ids = selectedUsers.value.map(item => readUserId(item))
    await updateUsersBatchStatus(ids, 0)
    showAdminSuccess(`成功封禁 ${ids.length} 个用户`)
    await fetchUserList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || '批量封禁失败')
    }
  }
}

/**
 * 批量解封用户。
 */
const handleBatchEnable = async () => {
  if (selectedUsers.value.length === 0) {
    showAdminWarning('请先选择要解封的用户')
    return
  }

  try {
    await confirmAdminRiskAction({
      title: '批量解封确认',
      actionText: '解封选中的用户',
      targetName: `${selectedUsers.value.length} 个用户`,
      impactHint: '解封后这些用户将恢复访问能力。',
      type: 'warning'
    })
    const ids = selectedUsers.value.map(item => readUserId(item))
    await updateUsersBatchStatus(ids, 1)
    showAdminSuccess(`成功解封 ${ids.length} 个用户`)
    await fetchUserList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showAdminError(error?.message || '批量解封失败')
    }
  }
}

/**
 * 监听角色变更：
 * 当角色切到非会员时，前端立即清空会员字段，避免误提交旧值。
 */
watch(
  () => editForm.role,
  (roleValue) => {
    if (roleValue !== 1) {
      editForm.membershipPlanCode = ''
      editForm.vipExpireTime = ''
    }
  }
)

/**
 * 监听筛选条件变化：
 * 每次筛选条件变化都回到第一页，避免保留旧页码导致展示为空。
 */
watch(
  () => [keyword.value, filterForm.role, filterForm.status, filterForm.vipState],
  () => {
    pagination.page = 1
  }
)

/**
 * 监听筛选后数据量：
 * 当当前页超出最大页码时回退到最后一页，保证始终有可见数据。
 */
watch(
  () => filteredUsers.value.length,
  (total) => {
    const totalPage = Math.max(1, Math.ceil(total / pagination.pageSize))
    if (pagination.page > totalPage) {
      pagination.page = totalPage
    }
  }
)

onMounted(async () => {
  await Promise.all([fetchUserList(), fetchMembershipPlans()])
})
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: linear-gradient(135deg, #fffcf8 0%, #fff8f3 100%);
  border: 1px solid rgba(230, 126, 34, 0.1);
  border-radius: 14px;
  padding: 18px 22px;
  box-shadow: 0 4px 16px rgba(143, 69, 27, 0.06);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  background: linear-gradient(135deg, #8f451b 0%, #d35400 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #a08060;
}

.search-input {
  width: 280px;
}

.refresh-btn {
  background: linear-gradient(135deg, #fdf1e6 0%, #f6e0d0 100%);
  border: 1px solid rgba(230, 126, 34, 0.2);
  border-radius: 10px;
  color: #8f451b;
  font-weight: 500;
}

.refresh-btn:hover {
  background: linear-gradient(135deg, #fff8f3 0%, #f6e0d0 100%);
  border-color: #e67e22;
  color: #d35400;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px rgba(217, 196, 170, 0.4);
  transition: all 0.25s ease;
}

.search-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(230, 126, 34, 0.3);
}

.search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(230, 126, 34, 0.15), 0 0 0 1px #e67e22;
}

.search-icon {
  color: #b8967a;
  font-size: 16px;
}

.table-card {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 20px rgba(143, 69, 27, 0.08);
}

.filter-card {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 20px rgba(143, 69, 27, 0.06);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
}

.filter-select {
  width: 160px;
}

.filter-select :deep(.el-select__wrapper) {
  border-radius: 10px;
}

.reset-btn {
  border-radius: 10px;
  border-color: rgba(230, 126, 34, 0.25);
  color: #8f451b;
}

.reset-btn:hover {
  border-color: #e67e22;
  color: #d35400;
}

.export-btn {
  border-radius: 10px;
  color: #2f7de1;
  border-color: rgba(47, 125, 225, 0.3);
  background: rgba(47, 125, 225, 0.06);
  font-weight: 500;
}

.export-btn:hover {
  background: rgba(47, 125, 225, 0.1);
  border-color: #2f7de1;
  color: #2563eb;
}

.filter-result {
  margin-top: 14px;
  font-size: 13px;
  color: #a08060;
  font-weight: 500;
}

.filter-count {
  color: #d35400;
  font-weight: 700;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.stats-card {
  border: 1px solid rgba(217, 196, 170, 0.25);
  border-radius: 14px;
  background: var(--bg-card);
  padding: 16px;
  text-align: left;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 4px 16px rgba(143, 69, 27, 0.05);
}

.stats-card:hover {
  border-color: rgba(230, 126, 34, 0.4);
  box-shadow: 0 8px 24px rgba(143, 69, 27, 0.12);
  transform: translateY(-3px);
}

.stats-card.active {
  border-color: #e67e22;
  background: linear-gradient(135deg, #fffcf8 0%, #fff8f3 100%);
  box-shadow: 0 6px 20px rgba(230, 126, 34, 0.15);
}

.stats-label {
  display: block;
  font-size: 12px;
  color: #a08060;
  margin-bottom: 8px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stats-value {
  font-size: 26px;
  color: #5a4030;
  line-height: 1;
  font-weight: 700;
}

.user-table :deep(.el-table__header-wrapper) {
  background: linear-gradient(135deg, #fff8f3 0%, #fff3e8 100%);
}

.table-header {
  font-weight: 700;
  color: #5a4030;
}

.user-table :deep(.el-table__body tr:nth-child(even)) {
  background: #fdfcfb;
}

.user-table :deep(.el-table__body tr:hover > td) {
  background: rgba(255, 140, 66, 0.08);
}

.status-tag {
  border-radius: 4px;
  font-weight: 500;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}

.action-btn.view {
  color: #3498db;
}

.action-btn.view:hover {
  color: #2980b9;
  background: #ebf5fb;
  border-radius: 6px;
}

.action-btn.edit {
  color: #e67e22;
}

.action-btn.edit:hover {
  color: #d35400;
  background: #fff5e6;
  border-radius: 6px;
}

.action-btn.quota {
  color: #8e44ad;
}

.action-btn.quota:hover {
  color: #7d3c98;
  background: #f5eef8;
  border-radius: 6px;
}

.action-btn:hover {
  background: var(--bg-elevated);
  border-radius: 6px;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.rights-drawer :deep(.el-drawer__header) {
  background: var(--bg-elevated);
  border-bottom: 1px solid var(--border-divider);
  margin-bottom: 0;
}

.rights-drawer :deep(.el-drawer__title) {
  font-weight: 600;
  color: #2c3e50;
}

.rights-descriptions :deep(.el-descriptions__label) {
  font-weight: 500;
  color: #34495e;
  background: var(--bg-elevated);
}

.edit-dialog :deep(.el-dialog__header) {
  background: var(--bg-elevated);
  border-bottom: 1px solid var(--border-divider);
}

.quota-dialog :deep(.el-dialog__header) {
  background: var(--bg-elevated);
  border-bottom: 1px solid var(--border-divider);
}

.quota-alert {
  margin-bottom: 16px;
}

.edit-dialog :deep(.el-dialog__title) {
  font-weight: 600;
  color: #2c3e50;
}

.edit-dialog :deep(.el-form-item__label) {
  font-weight: 500;
  color: #34495e;
}

.dialog-btn {
  border-radius: 8px;
  padding: 10px 24px;
  font-weight: 500;
}

.dialog-btn.primary {
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%);
  border: none;
}

.dialog-btn.primary:hover {
  background: linear-gradient(135deg, #d35400 0%, #c0392b 100%);
}

@media (max-width: 1200px) {
  .filter-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  .search-input {
    width: 100%;
  }

  .filter-row {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .pagination-wrap {
    justify-content: center;
  }
}
</style>
