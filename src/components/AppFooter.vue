<template>
  <footer class="app-footer">
    <div class="footer-content">
      <div class="footer-left">
        <span class="footer-brand">AI Resume</span>
        <span class="footer-divider">|</span>
        <span class="footer-copyright">&copy; {{ currentYear }} All rights reserved.</span>
      </div>
      <div class="footer-right">
        <a href="mailto:1795845968@qq.com" class="footer-link">联系我们</a>
        <span class="footer-dot">&middot;</span>
        <a href="#" class="footer-link" @click.prevent>隐私政策</a>
        <span class="footer-dot">&middot;</span>
        <a href="#" class="footer-link" @click.prevent>服务条款</a>
      </div>
    </div>
  </footer>
</template>

<script setup>
const currentYear = new Date().getFullYear();
</script>

<style scoped>
.app-footer {
  background: var(--bg-card, #ffffff);
  border-top: 1px solid var(--border-card, rgba(243, 216, 199, 0.5));
  color: var(--text-muted, #909399);
  padding: 16px 24px;
  font-size: 12px;
  flex-shrink: 0;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.footer-brand {
  font-weight: 600;
  color: var(--text-title, #303133);
  font-size: 13px;
}

.footer-divider {
  color: var(--border-card, #e4e7ed);
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.footer-link {
  color: var(--text-muted, #909399);
  text-decoration: none;
  transition: color 0.2s;
}

.footer-link:hover {
  color: var(--orange-main, #ff8c42);
}

.footer-dot {
  color: var(--border-card, #e4e7ed);
}

/* 移动端：单列居中 */
@media (max-width: 768px) {
  .footer-content {
    flex-direction: column;
    text-align: center;
    gap: 6px;
  }
}
</style>
