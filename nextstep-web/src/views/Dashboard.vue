<script setup lang="ts">
import { profileApi, type UserProfile } from '@/api/profile'
import { analysisApi, aiApi, type AnalysisResult } from '@/api/analysis'
import { streamExplain, type ExplainController } from '@/api/aiStream'
import { reportApi } from '@/api/report'
import { ElMessage } from 'element-plus'
import RadarChart from '@/components/RadarChart.vue'
import ChatPanel from '@/components/ChatPanel.vue'

const chatPanel = ref<InstanceType<typeof ChatPanel> | null>(null)

async function onProfileChanged() {
  await loadProfile()
  await loadScore()
}

const router = useRouter()
const profile = ref<UserProfile | null>(null)
const result = ref<AnalysisResult | null>(null)
const loading = ref(false)
const scoring = ref(false)
const exporting = ref(false)

const explanation = ref('')
const explaining = ref(false)
let explainCtl: ExplainController | null = null

const colorMap: Record<string, string> = {
  PG: 'from-blue-400 to-blue-600',
  CS: 'from-emerald-400 to-emerald-600',
  EM: 'from-amber-400 to-amber-600'
}

const tagColor: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  '稳': 'success', '中': 'warning', '冲': 'danger', '保': 'info',
  '卷': 'danger', '推荐': 'success', '可选': 'info'
}

async function loadProfile() {
  try { profile.value = await profileApi.get() } catch {}
}

async function loadScore() {
  if (!profile.value) return
  scoring.value = true
  try {
    result.value = await analysisApi.score()
  } catch {} finally { scoring.value = false }
}

async function loadCachedExplain() {
  try {
    const cached = await aiApi.explainCache()
    if (cached) explanation.value = cached
  } catch {}
}

async function exportReport() {
  exporting.value = true
  try {
    await reportApi.exportPdf()
    ElMessage.success('综合报告已下载')
  } catch (e: any) {
    ElMessage.error(e?.message || '综合报告导出失败')
  } finally {
    exporting.value = false
  }
}

function startExplain() {
  if (explaining.value) return
  explanation.value = ''
  explaining.value = true
  explainCtl = streamExplain(
    (chunk) => { explanation.value += chunk },
    () => { explaining.value = false },
    (err) => {
      explaining.value = false
      ElMessage.error('AI 解读失败：' + err.message)
    }
  )
}

function stopExplain() {
  explainCtl?.abort()
  explaining.value = false
}

function viewRecommendation(type: string) {
  if (type === 'school') router.push('/school')
  else if (type === 'gov') router.push('/gov')
  else if (type === 'job') router.push('/job')
}

onBeforeUnmount(() => { explainCtl?.abort() })

onMounted(async () => {
  loading.value = true
  await loadProfile()
  loading.value = false
  if (profile.value) {
    loadScore()
    loadCachedExplain()
  }
})
</script>

<template>
  <div class="page space-y-6" v-loading="loading">
    <!-- 顶部 hero -->
    <el-card>
      <div class="flex-between flex-wrap gap-4">
        <div>
          <h2 class="text-xl font-semibold mb-1">欢迎回来 👋</h2>
          <p class="text-sm text-gray-500">
            画像完整度
            <span class="font-medium text-brand">{{ profile?.profileCompleteness ?? 0 }}%</span>
            <span v-if="!profile" class="ml-2 text-gray-400">— 还没创建画像，先去填一下</span>
          </p>
        </div>
        <div class="flex flex-wrap justify-end gap-2">
          <el-button v-if="profile" :loading="exporting" @click="exportReport">
            <i class="i-ep-document mr-1" />导出综合报告
          </el-button>
          <el-button v-if="profile" plain @click="chatPanel?.open()">
            🗣 AI 帮我补画像
          </el-button>
          <el-button v-if="profile" :loading="scoring" @click="loadScore">重新评估</el-button>
          <el-button type="primary" @click="router.push('/profile')">
            {{ profile ? '更新画像' : '创建画像' }}
          </el-button>
        </div>
      </div>
      <el-progress :percentage="profile?.profileCompleteness ?? 0" :stroke-width="10" class="mt-4" />
    </el-card>

    <!-- 推荐路径 banner + AI 解读 -->
    <el-card v-if="result?.topPath" class="!bg-gradient-to-r from-indigo-500 to-purple-500 text-white">
      <div class="flex-between flex-wrap gap-3">
        <div class="flex-1 min-w-0">
          <div class="text-sm opacity-80">为你推荐的主线路径</div>
          <div class="text-2xl font-bold mt-1">
            {{ result.paths.find(p => p.path === result?.topPath)?.pathName }}
          </div>
          <div class="text-sm mt-2 opacity-95">{{ result.topPathReason }}</div>
        </div>
        <div class="text-right">
          <div class="text-5xl font-bold opacity-90">
            {{ result.paths.find(p => p.path === result?.topPath)?.overall }}
            <span class="text-base align-top opacity-80">分</span>
          </div>
          <el-button
            class="!mt-2 !bg-white/20 !border-white/30 !text-white hover:!bg-white/30"
            size="small"
            @click="explaining ? stopExplain() : startExplain()"
          >
            <i class="i-ep-magic-stick mr-1" />
            {{ explaining ? '停止' : explanation ? '重新解读' : 'AI 学长解读' }}
          </el-button>
          <el-button
            class="!mt-2 !ml-2 !bg-white !text-indigo-600 hover:!bg-white/90"
            size="small"
            @click="router.push({ path: '/plan', query: { path: result?.topPath } })"
          >
            📋 我的规划
          </el-button>
        </div>
      </div>

      <!-- AI 流式解读区 -->
      <div
        v-if="explanation || explaining"
        class="mt-4 p-4 bg-white/15 rounded backdrop-blur text-sm leading-relaxed whitespace-pre-wrap"
      >
        <span>{{ explanation }}</span>
        <span v-if="explaining" class="inline-block w-2 h-4 ml-1 bg-white/80 animate-pulse align-middle"></span>
      </div>
    </el-card>

    <!-- 路径综合分卡片 -->
    <div v-if="result" class="grid gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
      <div
        v-for="p in result.paths"
        :key="p.path"
        class="rounded-lg p-5 text-white bg-gradient-to-br shadow-sm"
        :class="colorMap[p.path]"
      >
        <div class="flex-between mb-3">
          <span class="text-lg font-semibold">{{ p.pathName }}</span>
          <el-tag v-if="p.path === result.topPath" type="warning" effect="dark" round>主线</el-tag>
        </div>
        <div class="text-4xl font-bold mb-2">
          {{ p.overall }}<span class="text-base ml-1 opacity-80">/ 100</span>
        </div>
        <div class="text-xs opacity-90 flex flex-wrap gap-x-3 gap-y-1">
          <span v-for="d in p.dimensions" :key="d.name">
            {{ d.name }} {{ d.score }}
          </span>
        </div>
      </div>
    </div>

    <!-- 雷达图 + 建议 -->
    <div v-if="result" class="grid gap-4 grid-cols-1 lg:grid-cols-2">
      <el-card>
        <template #header><span class="font-semibold">能力维度对比</span></template>
        <RadarChart :paths="result.paths" />
      </el-card>

      <el-card>
        <template #header><span class="font-semibold">个性化建议</span></template>
        <div class="space-y-4">
          <div v-for="p in result.paths" :key="p.path">
            <div class="font-medium mb-2 flex items-center gap-2">
              <el-tag size="small">{{ p.pathName }}</el-tag>
              <span class="text-sm text-gray-600">综合 {{ p.overall }}</span>
            </div>
            <ul class="space-y-1 text-sm text-gray-700">
              <li v-for="(a, i) in p.advice" :key="i" class="flex gap-2">
                <span class="text-brand">•</span><span>{{ a }}</span>
              </li>
              <li v-if="!p.advice.length" class="text-gray-400 text-xs">暂无特殊建议</li>
            </ul>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 推荐列表 -->
    <el-card v-if="result">
      <template #header><span class="font-semibold">为你推荐</span></template>
      <el-tabs>
        <el-tab-pane v-for="p in result.paths" :key="p.path" :label="p.pathName">
          <div class="grid gap-3 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
            <div
              v-for="r in p.recommendations"
              :key="r.refId"
              class="border border-gray-200 rounded p-4 card-hover"
              @click="viewRecommendation(r.type)"
            >
              <div class="flex-between mb-2">
                <span class="font-medium">{{ r.title }}</span>
                <el-tag size="small" :type="tagColor[r.tag] || 'info'">{{ r.tag }}</el-tag>
              </div>
              <div class="text-xs text-gray-500 mb-2 line-clamp-2">{{ r.subtitle }}</div>
              <el-progress :percentage="r.matchScore" :stroke-width="6" :show-text="false" />
              <div class="text-xs text-gray-400 mt-1 text-right">匹配 {{ r.matchScore }}</div>
            </div>
            <div v-if="!p.recommendations.length" class="text-gray-400 text-sm col-span-full text-center py-4">
              暂无推荐
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 三大路径入口（无评分时显示）-->
    <div v-if="!result" class="grid gap-4 grid-cols-1 md:grid-cols-3">
      <div class="card-hover rounded-lg p-6 text-white bg-gradient-to-br from-blue-400 to-blue-600" @click="router.push('/school')">
        <div class="text-lg font-semibold mb-2">考研路径</div>
        <div class="text-sm opacity-90">院校 / 专业 / 历年分数线 / 上岸率</div>
      </div>
      <div class="card-hover rounded-lg p-6 text-white bg-gradient-to-br from-emerald-400 to-emerald-600" @click="router.push('/gov')">
        <div class="text-lg font-semibold mb-2">考公路径</div>
        <div class="text-sm opacity-90">国考 / 省考 / 招录 / 进面线</div>
      </div>
      <div class="card-hover rounded-lg p-6 text-white bg-gradient-to-br from-amber-400 to-amber-600" @click="router.push('/job')">
        <div class="text-lg font-semibold mb-2">就业路径</div>
        <div class="text-sm opacity-90">行业 / 岗位 / 薪资行情</div>
      </div>
    </div>

    <ChatPanel ref="chatPanel" @profile-changed="onProfileChanged" />
  </div>
</template>
