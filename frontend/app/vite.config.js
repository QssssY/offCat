import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import viteCompression from 'vite-plugin-compression'

/// <reference types="vitest" />
export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 按需引入：自动注册组件和对应 API
    AutoImport({
      resolvers: [ElementPlusResolver()]
    }),
    Components({
      resolvers: [ElementPlusResolver()]
    }),
    // 生产构建 gzip 压缩（>10KB 的文件生成 .gz）
    viteCompression({
      algorithm: 'gzip',
      threshold: 10240,
      deleteOriginFile: false
    })
  ],
  // 开发环境预优化后台高频重依赖，导出类依赖保持按需加载，避免首次进入路由时拉入无关重包。
  optimizeDeps: {
    include: [
      'chart.js',
      'vue-chartjs',
      'element-plus',
      '@element-plus/icons-vue',
      'naive-ui'
    ]
  },
  test: {
    environment: 'happy-dom',
    include: ['src/**/*.{test,spec}.{js,ts}'],
    server: {
      deps: {
        inline: ['element-plus']
      }
    }
  },
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
          if (id.includes('docx')) {
            return 'docx-vendor'
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
        changeOrigin: true
      },
      '/auth': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
