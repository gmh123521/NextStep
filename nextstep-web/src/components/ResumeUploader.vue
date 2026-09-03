<script setup lang="ts">
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'
import { resumeApi, type ResumeExtractResult, type ResumeExperienceItem } from '@/api/resume'
import { formatRequestError } from '@/utils/error'

const emit = defineEmits<{ (e: 'applied'): void }>()

const visible = ref(false)
const uploading = ref(false)
const applying = ref(false)
const result = ref<ResumeExtractResult | null>(null)
const fileName = ref('')

const stageMessage = ref('')
let stageTimer: ReturnType<typeof setInterval> | null = null

function startStageTimer() {
  const start = Date.now()
  stageMessage.value = '正在上传简历...'
  stageTimer = setInterval(() => {
    const elapsed = Date.now() - start
    if (elapsed < 1500) {
      stageMessage.value = '正在上传简历...'
    } else if (elapsed < 3000) {
      stageMessage.value = '正在解析 PDF 文本...'
    } else if (elapsed < 6000) {
      stageMessage.value = 'AI 正在分析教育背景...'
    } else if (elapsed < 11000) {
      stageMessage.value = 'AI 正在提取经历与奖项...'
    } else {
      stageMessage.value = '即将完成...'
    }
  }, 400)
}

function stopStageTimer() {
  if (stageTimer) { clearInterval(stageTimer); stageTimer = null }
  stageMessage.value = ''
}

const expTypeLabel: Record<string, string> = {
  INTERNSHIP: '实习', PROJECT: '项目', AWARD: '奖项',
  RESEARCH: '科研', PAPER: '论文', COMPETITION: '竞赛'
}

const schoolLevelLabel: Record<string, string> = {
  C9: 'C9 联盟', '985': '985 工程', '211': '211 工程',
  DOUBLE_FIRST: '双一流', REGULAR: '普通本科', COLLEGE: '专科'
}

const degreeLabel: Record<string, string> = {
  BACHELOR: '本科', MASTER: '硕士', DOCTOR: '博士'
}

const englishLabel: Record<string, string> = {
  CET4: '英语四级', CET6: '英语六级', TEM4: '英语专四', TEM8: '英语专八',
  IELTS: '雅思', TOEFL: '托福',
  JLPT_N1: '日语 N1', JLPT_N2: '日语 N2', JLPT_N3: '日语 N3',
  JLPT_N4: '日语 N4', JLPT_N5: '日语 N5',
  TOPIK1: '韩语 TOPIK Ⅰ', TOPIK2: '韩语 TOPIK Ⅱ',
  OTHER: '其他'
}

const gpaScaleLabel: Record<number, string> = { 4: '4 分制', 5: '5 分制', 100: '百分制' }

function formatField(key: string, value: any): string {
  if (value == null || value === '') return ''
  if (key === 'schoolLevel') return schoolLevelLabel[value] || value
  if (key === 'degreeType')  return degreeLabel[value] || value
  if (key === 'englishLevel')return englishLabel[value] || value
  if (key === 'gpaScale')    return gpaScaleLabel[value] || `${value}`
  if (key === 'gradeYear') {
    const m: Record<number, string> = {
      1: '大一', 2: '大二', 3: '大三', 4: '大四 / 应届本科',
      5: '研一 / 博一', 6: '研二 / 博二', 7: '研三 / 博三 / 应届硕博'
    }
    return m[value] || `${value}`
  }
  return String(value)
}

const fields = computed(() => result.value ? [
  { label: '当前院校', key: 'currentSchool' },
  { label: '院校层次', key: 'schoolLevel' },
  { label: '当前专业', key: 'currentMajor' },
  { label: '学历',     key: 'degreeType' },
  { label: '年级',     key: 'gradeYear' },
  { label: 'GPA',      key: 'gpa' },
  { label: 'GPA 制式', key: 'gpaScale' },
  { label: '语言等级', key: 'englishLevel' },
  { label: '语言分数', key: 'englishScore' }
] : [])

function open() {
  visible.value = true
  result.value = null
  fileName.value = ''
}

defineExpose({ open })

async function onFileChange(file: UploadFile) {
  if (!file.raw) return
  if (!file.name.toLowerCase().endsWith('.pdf')) {
    ElMessage.warning('仅支持 PDF 格式')
    return
  }
  uploading.value = true
  fileName.value = file.name
  startStageTimer()
  try {
    result.value = await resumeApi.parse(file.raw)
    ElMessage.success('AI 抽取完成，请检查并确认')
  } catch (e: any) {
    ElMessage.error('抽取失败：' + formatRequestError(e, '请稍后重试'))
  } finally {
    uploading.value = false
    stopStageTimer()
  }
}

function removeExperience(idx: number) {
  result.value?.experiences?.splice(idx, 1)
}

async function confirmApply() {
  if (!result.value) return
  await ElMessageBox.confirm(
    '将把上面识别到的内容合并到你的画像中（已存在的经历会自动跳过）。继续吗？',
    '确认应用',
    { confirmButtonText: '应用', cancelButtonText: '取消', type: 'warning' }
  ).catch(() => null).then(async ok => {
    if (ok === null) return
    applying.value = true
    try {
      const r = await resumeApi.apply(result.value!)
      const msg = r.skipped > 0
        ? `已应用：新增 ${r.inserted} 条经历，跳过 ${r.skipped} 条已存在`
        : `已应用：新增 ${r.inserted} 条经历`
      ElMessage.success(msg)
      visible.value = false
      emit('applied')
    } catch (e) {
      ElMessage.error('应用简历失败：' + formatRequestError(e, '请稍后重试'))
    } finally { applying.value = false }
  })
}
</script>

<template>
  <el-dialog v-model="visible" title="上传简历自动填充" width="min(720px, 95vw)" destroy-on-close>
    <div v-if="!result">
      <el-upload
        drag
        :auto-upload="false"
        :show-file-list="false"
        accept=".pdf"
        :on-change="onFileChange"
        v-loading="uploading"
        :element-loading-text="stageMessage || 'AI 正在识别简历内容...'"
      >
        <i class="i-ep-upload-filled text-4xl text-gray-400" />
        <div class="el-upload__text mt-2">将 PDF 简历拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="text-xs text-gray-500 mt-2">
            仅支持 PDF 文字版（不支持扫描件，最大 5MB）<br>
            手机/邮箱/身份证等隐私信息不会被读取
          </div>
        </template>
      </el-upload>
    </div>

    <div v-else class="space-y-4">
      <div class="text-sm text-gray-500">
        从 <span class="font-medium text-gray-700">{{ fileName }}</span> 识别到以下内容，请检查（不需要的可以删除经历项）：
      </div>

      <el-descriptions :column="2" border size="small">
        <el-descriptions-item v-for="f in fields" :key="f.key" :label="f.label">
          <span v-if="formatField(f.key, (result as any)[f.key])">
            {{ formatField(f.key, (result as any)[f.key]) }}
          </span>
          <span v-else class="text-gray-400">未识别</span>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="result.experiences?.length">
        <div class="text-sm font-semibold mb-2">识别到的经历（{{ result.experiences.length }} 条）</div>
        <div class="space-y-2">
          <div
            v-for="(exp, i) in result.experiences"
            :key="i"
            class="p-3 border border-gray-200 rounded flex justify-between gap-3"
          >
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <el-tag size="small">{{ expTypeLabel[exp.type || ''] || exp.type }}</el-tag>
                <span class="font-medium truncate">{{ exp.title }}</span>
                <span v-if="exp.role" class="text-xs text-gray-500">· {{ exp.role }}</span>
              </div>
              <div class="text-xs text-gray-400">
                {{ exp.startDate || '?' }} ~ {{ exp.endDate || '?' }}
              </div>
              <div v-if="exp.description" class="text-xs text-gray-600 mt-1 line-clamp-2">
                {{ exp.description }}
              </div>
            </div>
            <el-button text type="danger" size="small" @click="removeExperience(i)">删除</el-button>
          </div>
        </div>
      </div>

      <el-alert v-if="result.notes?.length" type="warning" :closable="false">
        <ul class="text-xs space-y-1 my-0">
          <li v-for="(n, i) in result.notes" :key="i">{{ n }}</li>
        </ul>
      </el-alert>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button v-if="result" @click="result = null; fileName = ''">重新上传</el-button>
      <el-button v-if="result" type="primary" :loading="applying" @click="confirmApply">应用到画像</el-button>
    </template>
  </el-dialog>
</template>
