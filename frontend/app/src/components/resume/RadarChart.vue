<template>
  <div class="radar-chart-wrapper">
    <Radar :data="chartData" :options="chartOptions" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Radar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  RadialLinearScale,
  PointElement,
  LineElement,
  Filler,
  Tooltip,
  Legend,
} from 'chart.js'
import { useThemeStore } from '@/stores/theme'

// 注册雷达图所需的 Chart.js 模块
ChartJS.register(RadialLinearScale, PointElement, LineElement, Filler, Tooltip, Legend)

const themeStore = useThemeStore()
const isDark = computed(() => themeStore.resolvedTheme === 'dark')

const props = defineProps({
  scores: {
    type: Object,
    required: true,
  },
  labels: {
    type: Array,
    default: () => ['基本信息', '岗位能力', '工作经验', '项目经历', '教育背景'],
  },
  keys: {
    type: Array,
    default: () => ['basicInfo', 'skill', 'work', 'project', 'education'],
  },
})

// 提取分数值（兼容纯数字和 {score, ...} 对象格式）
const getScoreValue = (val) => {
  if (val == null) return 0
  if (typeof val === 'number') return val
  if (typeof val === 'object' && val.score != null) return val.score
  return 0
}

// 图表数据
const chartData = computed(() => ({
  labels: props.labels,
  datasets: [
    {
      label: '评分',
      data: props.keys.map((key) => getScoreValue(props.scores[key])),
      backgroundColor: 'rgba(255, 140, 66, 0.15)',
      borderColor: '#FF8C42',
      borderWidth: 2,
      pointBackgroundColor: '#FF8C42',
      pointBorderColor: isDark.value ? '#1F1511' : '#fff',
      pointBorderWidth: 2,
      pointRadius: 5,
      pointHoverRadius: 7,
    },
  ],
}))

// 图表配置
const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        label: (ctx) => `${ctx.label}: ${ctx.raw}分`,
      },
    },
  },
  scales: {
    r: {
      min: 0,
      max: 100,
      ticks: {
        stepSize: 20,
        font: { size: 11 },
        color: isDark.value ? '#CAA189' : '#999',
        backdropColor: 'transparent',
      },
      pointLabels: {
        font: { size: 13, weight: '600' },
        color: isDark.value ? '#FFF3E8' : '#2f2f2f',
      },
      grid: {
        color: isDark.value ? 'rgba(255, 255, 255, 0.06)' : 'rgba(0, 0, 0, 0.06)',
      },
      angleLines: {
        color: isDark.value ? 'rgba(255, 255, 255, 0.06)' : 'rgba(0, 0, 0, 0.06)',
      },
    },
  },
}))
</script>

<style scoped>
.radar-chart-wrapper {
  width: 100%;
}
</style>
