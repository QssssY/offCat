<template>
  <div class="line-chart-wrapper">
    <Line :data="chartData" :options="chartOptions" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Tooltip,
  Legend,
  Filler
} from 'chart.js'
import { useThemeStore } from '@/stores/theme'

// 注册折线图所需的 Chart.js 模块
ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend, Filler)

const themeStore = useThemeStore()
const isDark = computed(() => themeStore.resolvedTheme === 'dark')

const props = defineProps({
  /** X轴标签数组，如 ["04/01", "04/15", "05/01"] */
  labels: {
    type: Array,
    required: true
  },
  /** 数据集数组，每项包含 label, data, borderColor, backgroundColor */
  datasets: {
    type: Array,
    required: true
  },
  /** 是否显示图例（多条折线时需要） */
  showLegend: {
    type: Boolean,
    default: false
  }
})

// 图表数据
const chartData = computed(() => ({
  labels: props.labels,
  datasets: props.datasets.map(ds => ({
    label: ds.label || '分数',
    data: ds.data,
    borderColor: ds.borderColor || '#FF8C42',
    backgroundColor: ds.backgroundColor || 'rgba(255, 140, 66, 0.1)',
    borderWidth: 2,
    pointBackgroundColor: ds.borderColor || '#FF8C42',
    pointBorderColor: isDark.value ? '#1F1511' : '#fff',
    pointBorderWidth: 2,
    pointRadius: 4,
    pointHoverRadius: 6,
    fill: true,
    tension: 0.3
  }))
}))

// 图表配置
const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: props.showLegend,
      position: 'bottom',
      labels: {
        boxWidth: 12,
        boxHeight: 12,
        padding: 16,
        usePointStyle: true,
        pointStyle: 'circle',
        font: { size: 12 }
      }
    },
    tooltip: {
      callbacks: {
        label: (ctx) => `${ctx.raw}分`
      }
    }
  },
  scales: {
    x: {
      grid: { color: isDark.value ? 'rgba(255, 255, 255, 0.06)' : 'rgba(0, 0, 0, 0.04)' },
      ticks: { font: { size: 11 }, color: isDark.value ? '#CAA189' : '#999' }
    },
    y: {
      min: 0,
      max: 100,
      ticks: { stepSize: 20, font: { size: 11 }, color: isDark.value ? '#CAA189' : '#999' },
      grid: { color: isDark.value ? 'rgba(255, 255, 255, 0.08)' : 'rgba(0, 0, 0, 0.06)' }
    }
  }
}))
</script>

<style scoped>
.line-chart-wrapper {
  width: 100%;
}
</style>
