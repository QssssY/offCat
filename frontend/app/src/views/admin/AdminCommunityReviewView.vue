<template>
  <div class="admin-page community-review-page">
    <section class="page-header">
      <div>
        <h2 class="page-title">社区审核</h2>
        <p class="page-subtitle">处理待审帖子和评论，控制社区公开内容风险</p>
      </div>
      <el-button :loading="loading" class="refresh-btn" @click="loadCurrentList">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </section>

    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage" class="page-error" />

    <el-card shadow="never" class="filter-card">
      <el-tabs v-model="activeTab" class="review-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="帖子审核" name="posts" />
        <el-tab-pane label="评论审核" name="comments" />
      </el-tabs>

      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="审核状态">
          <el-select v-model="filters.reviewStatus" class="filter-select">
            <el-option label="全部" value="all" />
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已拒绝" value="rejected" />
            <el-option label="已隐藏" value="hidden" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="activeTab === 'posts'" label="板块">
          <el-select v-model="filters.category" clearable class="filter-select" placeholder="全部板块">
            <el-option label="面试经验" value="interview_exp" />
            <el-option label="内推广场" value="referral" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="filters.userId" clearable class="filter-input" placeholder="发布者用户ID" />
        </el-form-item>
        <el-form-item v-if="activeTab === 'comments'" label="帖子ID">
          <el-input v-model="filters.postId" clearable class="filter-input" placeholder="所属帖子ID" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" clearable class="keyword-input" placeholder="标题或正文关键词" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">筛选</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        v-if="activeTab === 'posts'"
        :data="rows"
        v-loading="loading"
        border
        stripe
        class="admin-data-table"
        empty-text="暂无帖子审核记录"
      >
        <el-table-column prop="id" label="帖子ID" min-width="180" show-overflow-tooltip />
        <el-table-column prop="authorName" label="发布者" min-width="130" show-overflow-tooltip />
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="category" label="板块" width="110">
          <template #default="{ row }">{{ formatCategory(row.category) }}</template>
        </el-table-column>
        <el-table-column prop="reviewStatus" label="审核状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.reviewStatus)" size="small" effect="plain">
              {{ statusText(row.reviewStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" @click="openDetail(row)">查看</el-button>
              <el-button size="small" type="success" plain @click="openReview(row, 'approved')">通过</el-button>
              <el-button size="small" type="danger" plain @click="openReview(row, 'rejected')">拒绝</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-table
        v-else
        :data="rows"
        v-loading="loading"
        border
        stripe
        class="admin-data-table"
        empty-text="暂无评论审核记录"
      >
        <el-table-column prop="id" label="评论ID" min-width="180" show-overflow-tooltip />
        <el-table-column prop="postId" label="帖子ID" min-width="180" show-overflow-tooltip />
        <el-table-column prop="authorName" label="评论者" min-width="130" show-overflow-tooltip />
        <el-table-column prop="postTitle" label="所属帖子" min-width="220" show-overflow-tooltip />
        <el-table-column prop="content" label="评论内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="reviewStatus" label="审核状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.reviewStatus)" size="small" effect="plain">
              {{ statusText(row.reviewStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" @click="openDetail(row)">查看</el-button>
              <el-button size="small" type="success" plain @click="openReview(row, 'approved')">通过</el-button>
              <el-button size="small" type="danger" plain @click="openReview(row, 'rejected')">拒绝</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > 0" class="pagination-wrap">
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

    <el-dialog v-model="detailVisible" title="社区内容详情" width="680px">
      <div v-if="currentItem" class="detail-view">
        <div class="detail-field"><label>ID</label><span>{{ currentItem.id }}</span></div>
        <div class="detail-field"><label>用户</label><span>{{ currentItem.authorName }}（{{ currentItem.userId }}）</span></div>
        <div v-if="activeTab === 'posts'" class="detail-field"><label>标题</label><span>{{ currentItem.title }}</span></div>
        <div v-else class="detail-field"><label>帖子</label><span>{{ currentItem.postTitle || currentItem.postId }}</span></div>
        <div class="detail-field"><label>状态</label><span>{{ statusText(currentItem.reviewStatus) }}</span></div>
        <div class="detail-content">
          <label>内容</label>
          <div class="content-box">{{ currentItem.content }}</div>
        </div>
        <div v-if="currentItem.reviewReason" class="detail-content">
          <label>审核原因</label>
          <div class="content-box">{{ currentItem.reviewReason }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="success" @click="openReview(currentItem, 'approved')">通过</el-button>
        <el-button type="danger" @click="openReview(currentItem, 'rejected')">拒绝</el-button>
        <el-button type="warning" plain @click="openReview(currentItem, 'hidden')">隐藏</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewVisible" title="审核处理" width="520px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="目标状态">
          <el-tag :type="statusTagType(reviewForm.reviewStatus)" effect="plain">
            {{ statusText(reviewForm.reviewStatus) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="审核原因">
          <el-input
            v-model="reviewForm.reviewReason"
            type="textarea"
            :rows="5"
            maxlength="255"
            show-word-limit
            :placeholder="reviewForm.reviewStatus === 'approved' ? '通过时可不填' : '请填写拒绝或隐藏原因'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReview">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import {
  getAdminCommunityComments,
  getAdminCommunityPosts,
  reviewAdminCommunityComment,
  reviewAdminCommunityPost
} from '@/api/admin/community'
import { showAdminError, showAdminSuccess } from '@/utils/adminFeedback'
import { formatDate } from '@/utils/date'

const activeTab = ref('posts')
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const rows = ref([])
const total = ref(0)
const currentPage = ref(1)
const size = ref(20)
const detailVisible = ref(false)
const reviewVisible = ref(false)
const currentItem = ref(null)

const filters = reactive({
  reviewStatus: 'pending',
  category: '',
  userId: '',
  postId: '',
  keyword: ''
})

const reviewForm = reactive({
  id: null,
  reviewStatus: 'approved',
  reviewReason: ''
})

const buildQuery = () => {
  const query = {
    page: currentPage.value,
    size: size.value,
    reviewStatus: filters.reviewStatus
  }
  if (filters.keyword) query.keyword = filters.keyword
  if (filters.userId) query.userId = filters.userId
  if (activeTab.value === 'posts' && filters.category) query.category = filters.category
  if (activeTab.value === 'comments' && filters.postId) query.postId = filters.postId
  return query
}

const loadCurrentList = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const loader = activeTab.value === 'posts' ? getAdminCommunityPosts : getAdminCommunityComments
    const res = await loader(buildQuery())
    const data = res?.data || {}
    rows.value = data.list || []
    total.value = Number(data.total || 0)
  } catch (error) {
    errorMessage.value = error?.message || '加载社区审核列表失败'
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  currentPage.value = 1
  loadCurrentList()
}

const handleSearch = () => {
  currentPage.value = 1
  loadCurrentList()
}

const handleReset = () => {
  filters.reviewStatus = 'pending'
  filters.category = ''
  filters.userId = ''
  filters.postId = ''
  filters.keyword = ''
  currentPage.value = 1
  loadCurrentList()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadCurrentList()
}

const handlePageSizeChange = (pageSize) => {
  size.value = pageSize
  currentPage.value = 1
  loadCurrentList()
}

const openDetail = (row) => {
  currentItem.value = row
  detailVisible.value = true
}

const openReview = (row, reviewStatus) => {
  if (!row) return
  currentItem.value = row
  reviewForm.id = row.id
  reviewForm.reviewStatus = reviewStatus
  reviewForm.reviewReason = reviewStatus === 'approved' ? '' : (row.reviewReason || '')
  reviewVisible.value = true
}

const submitReview = async () => {
  if (reviewForm.reviewStatus !== 'approved' && !reviewForm.reviewReason.trim()) {
    showAdminError('拒绝或隐藏内容时必须填写原因')
    return
  }
  submitting.value = true
  try {
    const payload = {
      reviewStatus: reviewForm.reviewStatus,
      reviewReason: reviewForm.reviewReason.trim()
    }
    if (activeTab.value === 'posts') {
      await reviewAdminCommunityPost(reviewForm.id, payload)
    } else {
      await reviewAdminCommunityComment(reviewForm.id, payload)
    }
    showAdminSuccess('审核状态已更新')
    reviewVisible.value = false
    detailVisible.value = false
    await loadCurrentList()
  } catch (error) {
    showAdminError(error?.message || '保存审核状态失败')
  } finally {
    submitting.value = false
  }
}

const statusText = (status) => {
  const map = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已拒绝',
    hidden: '已隐藏'
  }
  return map[status] || '未知'
}

const statusTagType = (status) => {
  const map = {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger',
    hidden: 'info'
  }
  return map[status] || 'info'
}

const formatCategory = (category) => {
  const map = {
    interview_exp: '面试经验',
    referral: '内推广场'
  }
  return map[category] || category || '--'
}

onMounted(loadCurrentList)
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-header {
  background: #fffaf5;
  border: 1px solid rgba(230, 126, 34, 0.14);
  border-radius: 16px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  box-shadow: 0 6px 20px rgba(143, 69, 27, 0.08);
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #8f451b;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #8d7058;
  font-size: 14px;
}

.refresh-btn {
  background: #d96c18;
  border: none;
  border-radius: 12px;
  padding: 12px 20px;
  font-weight: 600;
  box-shadow: 0 6px 20px rgba(217, 108, 24, 0.24);
  color: #fff;
}

.filter-card,
.table-card {
  border: 1px solid rgba(217, 196, 170, 0.25);
  border-radius: 18px;
  box-shadow: 0 6px 20px rgba(143, 69, 27, 0.06);
}

.review-tabs {
  margin-bottom: 12px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
}

.filter-select,
.filter-input {
  width: 160px;
}

.keyword-input {
  width: 220px;
}

.admin-data-table {
  width: 100%;
  min-width: 1180px;
}

.action-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.detail-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-field {
  display: flex;
  gap: 12px;
}

.detail-field label,
.detail-content label {
  width: 82px;
  font-weight: 600;
  color: #5a4030;
  flex-shrink: 0;
}

.detail-field span {
  color: #333;
  overflow-wrap: anywhere;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.content-box {
  background: #faf8f5;
  border: 1px solid #e8e0d8;
  border-radius: 8px;
  padding: 12px;
  white-space: pre-wrap;
  font-size: 14px;
  line-height: 1.6;
  overflow-wrap: anywhere;
  max-height: 260px;
  overflow-y: auto;
}

@media (max-width: 768px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .filter-select,
  .filter-input,
  .keyword-input {
    width: 100%;
  }

  .pagination-wrap {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
