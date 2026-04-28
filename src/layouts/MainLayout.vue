<template>
  <div class="main-layout">
    <AppHeader />
    <main class="layout-main">
      <section class="layout-content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup>
import AppHeader from "@/components/AppHeader.vue";
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  background-color: var(--bg-page, #fff8f3);
  display: flex;
  flex-direction: column;
}

.layout-main {
  padding-top: 60px;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.layout-content {
  flex: 1;
  min-height: 0;
  padding: 24px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .layout-content {
    padding: 16px;
  }
}

@media (max-width: 480px) {
  .layout-content {
    padding: 12px;
  }
}
</style>
