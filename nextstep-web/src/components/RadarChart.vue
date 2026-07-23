<script setup lang="ts">
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { RadarChart } from 'echarts/charts'
import { LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import type { PathScore } from '@/api/analysis'

use([CanvasRenderer, RadarChart, LegendComponent, TitleComponent, TooltipComponent])

const props = defineProps<{ paths: PathScore[] }>()

const colorMap: Record<string, string> = {
  PG: '#5470c6',
  CS: '#91cc75',
  EM: '#fac858'
}

const option = computed(() => {
  if (!props.paths.length) return {}
  const indicators = props.paths[0].dimensions.map(d => ({ name: d.name, max: 100 }))
  const series = props.paths.map(p => ({
    name: p.pathName,
    value: p.dimensions.map(d => d.score),
    areaStyle: { opacity: 0.15 },
    lineStyle: { width: 2 },
    itemStyle: { color: colorMap[p.path] }
  }))
  return {
    legend: { data: props.paths.map(p => p.pathName), bottom: 0 },
    tooltip: { trigger: 'item' },
    radar: {
      indicator: indicators,
      radius: '65%',
      splitNumber: 5,
      axisName: { color: '#606266', fontSize: 12 }
    },
    series: [{ type: 'radar', data: series }]
  }
})
</script>

<template>
  <v-chart v-if="paths.length" :option="option" autoresize class="w-full h-80" />
  <div v-else class="text-gray-400 text-center py-8">暂无评分数据</div>
</template>
