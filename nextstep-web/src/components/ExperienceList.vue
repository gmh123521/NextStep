<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { experienceApi, type UserExperience } from '@/api/experience'

const emit = defineEmits<{ (e: 'changed'): void }>()

const list = ref<UserExperience[]>([])
const loading = ref(false)
const expandedIds = reactive(new Set<number>())

function toggleExpand(id: number) {
  if (expandedIds.has(id)) expandedIds.delete(id)
  else expandedIds.add(id)
}

function isLongText(text?: string) {
  return text && text.length > 120
}

const TYPE_META: Record<string, { label: string; icon: string; color: string }> = {
  INTERNSHIP:  { label: '实习', icon: 'i-ep-briefcase',     color: 'bg-blue-50 text-blue-600' },
  PROJECT:     { label: '项目', icon: 'i-ep-folder-opened', color: 'bg-emerald-50 text-emerald-600' },
  AWARD:       { label: '奖项', icon: 'i-ep-trophy',        color: 'bg-amber-50 text-amber-600' },
  RESEARCH:    { label: '科研', icon: 'i-ep-magic-stick',   color: 'bg-violet-50 text-violet-600' },
  PAPER:       { label: '论文', icon: 'i-ep-document',      color: 'bg-rose-50 text-rose-600' },
  COMPETITION: { label: '竞赛', icon: 'i-ep-medal',         color: 'bg-orange-50 text-orange-600' }
}

const SOURCE_LABEL: Record<string, string> = {
  RESUME: '简历导入', MANUAL: '手动添加', CHAT: 'AI 对话'
}

// 按 type 分组
const grouped = computed(() => {
  const g: Record<string, UserExperience[]> = {}
  for (const e of list.value) {
    const k = e.type || 'OTHER'
    ;(g[k] = g[k] || []).push(e)
  }
  return g
})

async function load() {
  loading.value = true
  try {
    list.value = await experienceApi.list()
  } finally { loading.value = false }
}

async function handleDelete(exp: UserExperience) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${exp.title}」吗？此操作不可恢复。`,
      '删除经历',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await experienceApi.remove(exp.id)
    ElMessage.success('已删除')
    await load()
    emit('changed')  // 通知父组件刷新画像（has_* 派生字段会变）
  } catch { /* 取消 */ }
}

defineExpose({ reload: load })
onMounted(load)
</script>

<template>
  <el-card v-loading="loading">
    <template #header>
      <div class="flex-between">
        <span class="font-semibold">我的经历 <span class="text-gray-400 text-sm font-normal">({{ list.length }})</span></span>
        <span class="text-xs text-gray-400">通过简历上传或 AI 对话自动收集</span>
      </div>
    </template>

    <div v-if="!list.length" class="text-center py-8 text-gray-400 text-sm">
      还没有经历记录，点上方"上传简历自动填充"即可一键导入
    </div>

    <div v-else class="space-y-5">
      <div v-for="(items, type) in grouped" :key="type">
        <div class="flex items-center gap-2 mb-2">
          <i :class="TYPE_META[type]?.icon" class="text-lg" />
          <span class="font-medium">{{ TYPE_META[type]?.label || type }}</span>
          <span class="text-xs text-gray-400">{{ items.length }} 条</span>
        </div>

        <div class="space-y-2">
          <div
            v-for="exp in items"
            :key="exp.id"
            class="p-3 border border-gray-200 rounded-lg hover:bg-gray-50 transition flex items-start gap-3"
          >
            <div :class="TYPE_META[type]?.color" class="px-2 py-1 rounded text-xs flex-shrink-0">
              {{ TYPE_META[type]?.label }}
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex flex-wrap items-baseline gap-2">
                <span class="font-medium">{{ exp.title }}</span>
                <span v-if="exp.role" class="text-sm text-gray-500">· {{ exp.role }}</span>
              </div>
              <div class="text-xs text-gray-400 mt-1">
                <span v-if="exp.startDate || exp.endDate">
                  {{ exp.startDate || '?' }} ~ {{ exp.endDate || '?' }}
                </span>
                <span class="mx-2">·</span>
                <span>{{ SOURCE_LABEL[exp.source] || exp.source }}</span>
              </div>
              <div v-if="exp.description" class="text-sm text-gray-600 mt-1">
                <span :class="{ 'line-clamp-2': !expandedIds.has(exp.id) && isLongText(exp.description) }">{{ exp.description }}</span>
                <button
                  v-if="isLongText(exp.description)"
                  class="text-blue-500 text-xs ml-1 hover:underline flex-shrink-0"
                  @click="toggleExpand(exp.id)"
                >{{ expandedIds.has(exp.id) ? '收起' : '展开' }}</button>
              </div>
            </div>
            <el-button text type="danger" size="small" class="flex-shrink-0" @click="handleDelete(exp)">
              删除
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </el-card>
</template>
