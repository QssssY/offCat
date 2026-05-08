import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    // 统一告警阈值：基础 UI 库（Element Plus）体积较大且已独立成 vendor chunk，
    // 将阈值调到 1000kB，避免对已知可控依赖产生噪声告警。
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      output: {
        manualChunks(id) {
          // 管理端收尾：手动拆分第三方依赖，降低入口包体积并缓解构建告警。
          // 这样做可以避免单个 index chunk 过大，提升首屏缓存复用效率。
          if (!id.includes('node_modules')) return

          if (id.includes('chart.js') || id.includes('vue-chartjs')) {
            return 'chart-vendor'
          }
          if (id.includes('element-plus') || id.includes('@element-plus/icons-vue')) {
            return 'element-plus-vendor'
          }
          if (id.includes('axios')) {
            return 'axios-vendor'
          }
          if (
            id.includes('/vue/') ||
            id.includes('/vue-router/') ||
            id.includes('/pinia/')
          ) {
            return 'vue-vendor'
          }
        }
      }
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        timeout: 120000
      },
      '/auth': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
