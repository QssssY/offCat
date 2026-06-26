<template>
  <el-dialog
    v-model="visible"
    title=""
    width="420px"
    :close-on-click-modal="true"
    :close-on-press-escape="true"
    align-center
    class="vip-upgrade-dialog"
    @close="onClose"
  >
    <div class="vip-upgrade-content">
      <div class="upgrade-icon">
        <el-icon :size="48"><Lock /></el-icon>
      </div>
      <h3 class="upgrade-title">升级会员解锁更多功能</h3>
      <p class="upgrade-desc">该功能为会员专属，升级会员即可享受以下权益：</p>
      <ul class="benefit-list">
        <li>AI 简历润色（每份简历 1 次）</li>
        <li>JD 岗位匹配分析（每日 3 次）</li>
        <li>简历模板库（每日 5 次使用）</li>
        <li>Offer 薪资谈判辅助（每日 3 次）</li>
        <li>更多简历诊断与模拟面试次数</li>
      </ul>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="onClose">稍后再说</el-button>
        <el-button type="primary" @click="goUpgrade">立即升级</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Lock } from '@element-plus/icons-vue'

const router = useRouter()
const visible = ref(false)

const onShowUpgrade = () => {
  visible.value = true
}

const onClose = () => {
  visible.value = false
}

const goUpgrade = () => {
  visible.value = false
  router.push('/membership')
}

onMounted(() => {
  window.addEventListener('show-vip-upgrade', onShowUpgrade)
})

onUnmounted(() => {
  window.removeEventListener('show-vip-upgrade', onShowUpgrade)
})
</script>

<style scoped>
.vip-upgrade-content {
  text-align: center;
  padding: 10px 0;
}
.upgrade-icon {
  margin-bottom: 16px;
  color: var(--el-color-warning);
}
.upgrade-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 8px;
}
.upgrade-desc {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin: 0 0 16px;
}
.benefit-list {
  list-style: none;
  padding: 0;
  margin: 0;
  text-align: left;
  display: inline-block;
}
.benefit-list li {
  padding: 4px 0;
  font-size: 14px;
  color: var(--el-text-color-regular);
}
.benefit-list li::before {
  content: '\2714\fe0f ';
  margin-right: 6px;
}
.dialog-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
