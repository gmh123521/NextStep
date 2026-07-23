<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { plannerApi, type PlanView, type PlanTask } from '@/api/planner'

const route = useRoute()
const router = useRouter()

const path = ref<string>((route.query.path as string) || 'PG')
const plan = ref<PlanView | null>(null)
const loading = ref(false)
const generating = ref(false)

/** 战线长度自定义 */
const advancedOpen = ref(false)
const recommendedMonths = ref<number | null>(null)
const recommendReason = ref<string>('')
const customMonths = ref<number | null>(null)
const monthOptions = [3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 18, 24]

const pathOptions = [
  { v: 'PG', l: '考研' },
  { v: 'CS', l: '考公' },
  { v: 'EM', l: '就业' }
]

const subjectColor: Record<string, string> = {
  '数学': 'bg-blue-50 text-blue-600',
  '英语': 'bg-emerald-50 text-emerald-600',
  '政治': 'bg-rose-50 text-rose-600',
  '专业课': 'bg-violet-50 text-violet-600',
  '行测': 'bg-orange-50 text-orange-600',
  '申论': 'bg-amber-50 text-amber-600',
  '面试': 'bg-pink-50 text-pink-600',
  '简历': 'bg-cyan-50 text-cyan-600',
  '技术准备': 'bg-indigo-50 text-indigo-600',
  '笔试刷题': 'bg-purple-50 text-purple-600',
  '项目准备': 'bg-teal-50 text-teal-600',
  '面试演练': 'bg-fuchsia-50 text-fuchsia-600',
  '选校选岗': 'bg-sky-50 text-sky-600',
  '能力盘点': 'bg-lime-50 text-lime-600'
}

async function load() {
  loading.value = true
  try {
    plan.value = await plannerApi.get(path.value)
  } catch {
    plan.value = null
  } finally { loading.value = false }
  // 同时拉推荐战线（即使已有规划，重新生成时也会用到）
  loadRecommend()
}

async function loadRecommend() {
  try {
    const r = await plannerApi.recommend(path.value)
    recommendedMonths.value = r.months
    recommendReason.value = r.reason
    if (customMonths.value == null) customMonths.value = r.months
  } catch {}
}

async function generate() {
  generating.value = true
  ElMessage.info('AI 正在生成规划，约 30-60 秒...')
  try {
    // 用户没动过 → 走智能推断（不传 months）；改过 → 传自定义
    const months = (customMonths.value && customMonths.value !== recommendedMonths.value)
      ? customMonths.value : undefined
    plan.value = await plannerApi.generate(path.value, months)
    ElMessage.success('规划已生成')
  } catch {} finally { generating.value = false }
}

async function regenerate() {
  await ElMessageBox.confirm(
    '重新生成会覆盖当前规划，已勾选的进度也会清空。继续吗？',
    '重新生成',
    { confirmButtonText: '生成', cancelButtonText: '取消', type: 'warning' }
  ).then(generate).catch(() => {})
}

const exporting = ref(false)
async function exportPdf() {
  if (!plan.value) return
  exporting.value = true
  try {
    await plannerApi.exportPdf(path.value, plan.value.pathName)
    ElMessage.success('PDF 已下载')
  } catch (e: any) {
    ElMessage.error(e?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

async function onSwitchPath(v: string) {
  path.value = v
  router.replace({ query: { path: v } })
  recommendedMonths.value = null
  customMonths.value = null
  await load()
}

async function toggle(task: PlanTask) {
  const newVal = task.completed ? 0 : 1
  task.completed = newVal  // 乐观更新
  try {
    await plannerApi.toggleTask(task.id, newVal === 1)
    if (plan.value) {
      const done = plan.value.phases.flatMap(p => p.tasks).filter(t => t.completed === 1).length
      plan.value.doneTasks = done
      plan.value.progressPct = plan.value.totalTasks
        ? Math.round(100 * done / plan.value.totalTasks) : 0
    }
  } catch {
    task.completed = newVal === 1 ? 0 : 1  // 回滚
  }
}

onMounted(load)
</script>

<template>
  <div class="page space-y-4" v-loading="loading">

    <!-- 生成中的顶部提示条（不锁页面，用户能继续看旧规划）-->
    <transition name="el-fade-in-linear">
      <div v-if="generating" class="generating-banner">
        <el-icon class="is-loading mr-2"><Loading /></el-icon>
        <span>AI 正在生成规划，预计 30-60 秒...</span>
      </div>
    </transition>

    <!-- 顶部 hero -->
    <el-card>
      <div class="flex-between flex-wrap gap-4">
        <div class="flex-1 min-w-0">
          <h2 class="text-xl font-semibold mb-2">📋 我的备考/求职规划</h2>
          <el-radio-group :model-value="path" @change="onSwitchPath">
            <el-radio-button v-for="o in pathOptions" :key="o.v" :value="o.v">{{ o.l }}</el-radio-button>
          </el-radio-group>
        </div>
        <div class="flex items-center gap-3">
          <el-button v-if="plan" :loading="exporting" @click="exportPdf">📄 导出 PDF</el-button>
          <el-button v-if="plan" :loading="generating" @click="regenerate">重新生成</el-button>
          <el-button v-else type="primary" :loading="generating" @click="generate">
            生成 {{ pathOptions.find(o => o.v === path)?.l }}规划
          </el-button>
        </div>
      </div>

      <!-- 高级：自定义战线 -->
      <div class="mt-3">
        <el-link
          type="primary"
          :underline="false"
          @click="advancedOpen = !advancedOpen"
          class="!text-xs"
        >
          ⚙ 自定义战线长度（高级）
          <span class="ml-1">{{ advancedOpen ? '▲' : '▼' }}</span>
        </el-link>
        <div v-if="advancedOpen" class="mt-2 p-3 bg-gray-50 rounded text-sm">
          <div v-if="recommendReason" class="text-xs text-gray-500 mb-2">
            💡 AI 推荐：<span class="font-medium text-brand">{{ recommendedMonths }} 个月</span>
            <span class="ml-1">— {{ recommendReason }}</span>
          </div>
          <div class="flex items-center gap-2 flex-wrap">
            <span class="text-gray-700">战线长度</span>
            <el-select v-model="customMonths" class="!w-32" size="small">
              <el-option v-for="m in monthOptions" :key="m" :label="`${m} 个月`" :value="m" />
            </el-select>
            <span v-if="customMonths === recommendedMonths" class="text-xs text-gray-400">使用 AI 推荐</span>
            <span v-else-if="customMonths != null" class="text-xs text-orange-500">
              已自定义（覆盖 AI 推荐 {{ recommendedMonths }} 个月）
            </span>
          </div>
        </div>
      </div>

      <div v-if="plan" class="mt-4">
        <div class="flex-between mb-1 text-sm">
          <span class="text-gray-600">
            进度：{{ plan.doneTasks }} / {{ plan.totalTasks }} 个任务完成
            <span class="ml-2 text-gray-400">· 战线 {{ plan.totalMonths }} 个月</span>
          </span>
          <span class="font-medium text-brand">{{ plan.progressPct }}%</span>
        </div>
        <el-progress :percentage="plan.progressPct" :stroke-width="10" />
      </div>
    </el-card>

    <!-- 空状态 -->
    <el-card v-if="!plan && !loading && !generating" class="text-center py-12">
      <div class="text-gray-400 mb-3">还没生成 {{ pathOptions.find(o => o.v === path)?.l }}规划</div>
      <el-button type="primary" @click="generate">立即生成</el-button>
    </el-card>

    <!-- 目标 + 总策略 -->
    <el-card v-if="plan?.targetSummary || plan?.strategy">
      <template #header>
        <span class="font-semibold">🎯 总体策略</span>
        <span v-if="plan.targetSummary" class="text-sm text-gray-500 ml-3">{{ plan.targetSummary }}</span>
      </template>
      <p v-if="plan.strategy" class="text-sm text-gray-700 leading-relaxed whitespace-pre-wrap">
        {{ plan.strategy }}
      </p>
    </el-card>

    <!-- 风险预警 -->
    <el-alert v-if="plan?.riskAlerts?.length" type="warning" :closable="false">
      <template #title><span class="font-semibold">⚠️ 风险预警</span></template>
      <ul class="text-sm space-y-1 my-2">
        <li v-for="(r, i) in plan.riskAlerts" :key="i">{{ r }}</li>
      </ul>
    </el-alert>

    <!-- 阶段时间轴 -->
    <el-card v-if="plan?.phases?.length">
      <template #header><span class="font-semibold">📅 月度任务清单</span></template>
      <el-timeline>
        <el-timeline-item
          v-for="ph in plan.phases" :key="ph.phase"
          :timestamp="ph.phase" placement="top" type="primary" hollow
        >
          <div class="space-y-2 mb-3">
            <div
              v-for="t in ph.tasks" :key="t.id"
              class="flex items-start gap-3 p-3 border border-gray-200 rounded-lg hover:bg-gray-50 transition"
              :class="t.completed ? 'opacity-60' : ''"
            >
              <el-checkbox
                :model-value="t.completed === 1"
                @change="toggle(t)"
                size="large"
                class="!mt-0.5"
              />
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 flex-wrap">
                  <span v-if="t.subject"
                        class="text-xs px-2 py-0.5 rounded"
                        :class="subjectColor[t.subject] || 'bg-gray-100 text-gray-600'">
                    {{ t.subject }}
                  </span>
                  <span class="font-medium" :class="t.completed ? 'line-through text-gray-400' : ''">
                    {{ t.title }}
                  </span>
                </div>
                <p v-if="t.description"
                   class="text-sm text-gray-600 mt-1 leading-relaxed"
                   :class="t.completed ? 'line-through' : ''">
                  {{ t.description }}
                </p>
              </div>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<style scoped>
.generating-banner {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 16px;
  margin: -16px -16px 12px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.25);
}
.generating-banner .el-icon {
  font-size: 16px;
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}
</style>

