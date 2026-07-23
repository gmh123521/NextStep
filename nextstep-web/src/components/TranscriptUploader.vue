<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { transcriptApi, type TranscriptExtractResult, type CourseItem } from '@/api/transcript'
import { compressImageIfNeeded } from '@/utils/imageCompress'

const emit = defineEmits<{ (e: 'applied'): void }>()

const visible = ref(false)
const uploading = ref(false)
const applying = ref(false)
const result = ref<TranscriptExtractResult | null>(null)
const fileName = ref('')
const syncProfileGpa = ref(true)

/** 上传过程的"分阶段进度提示" */
const stageMessage = ref('')
let stageTimer: ReturnType<typeof setInterval> | null = null

/** 经验值：阿里云 qwen3.6-plus 识别成绩单的速率
 *  - 启动 + 上传约 1.5 秒
 *  - 然后每 0.4 秒识别 1 门课
 *  - 最多估到 35 门后停在那里等真实结果
 */
function estimateCourseCount(elapsedMs: number): number {
  if (elapsedMs < 1500) return 0
  const n = Math.floor((elapsedMs - 1500) / 400)
  return Math.min(35, n)
}

function startStageTimer() {
  const start = Date.now()
  stageMessage.value = '正在上传文件...'
  stageTimer = setInterval(() => {
    const elapsed = Date.now() - start
    if (elapsed < 1500) {
      stageMessage.value = '正在上传文件...'
    } else if (elapsed < 4500) {
      stageMessage.value = 'AI 正在识别成绩单结构...'
    } else if (elapsed < 14000) {
      const n = estimateCourseCount(elapsed)
      stageMessage.value = `AI 已识别约 ${n} 门课程...`
    } else {
      stageMessage.value = '正在汇总学分与 GPA...'
    }
  }, 400)
}

function stopStageTimer() {
  if (stageTimer) { clearInterval(stageTimer); stageTimer = null }
  stageMessage.value = ''
}

const scaleLabel: Record<number, string> = { 4: '4 分制', 5: '5 分制', 100: '百分制' }

function gpaTo4(gpa: number | undefined | null, scale: number | undefined | null): number | null {
  if (gpa == null) return null
  const s = scale || 4
  let n: number
  if (s === 100) {
    if (gpa >= 90) n = 4.0
    else if (gpa >= 85) n = 3.7
    else if (gpa >= 82) n = 3.3
    else if (gpa >= 78) n = 3.0
    else if (gpa >= 75) n = 2.7
    else if (gpa >= 72) n = 2.3
    else if (gpa >= 68) n = 2.0
    else if (gpa >= 64) n = 1.5
    else if (gpa >= 60) n = 1.0
    else n = 0
  } else if (s === 5) {
    if (gpa >= 4.5) n = 4.0
    else if (gpa >= 4.0) n = 3.6
    else if (gpa >= 3.5) n = 3.2
    else if (gpa >= 3.0) n = 2.7
    else if (gpa >= 2.5) n = 2.0
    else n = 1.0
  } else {
    n = gpa
  }
  n = Math.max(0, Math.min(4, n))
  return Math.round(n * 100) / 100
}

function open() {
  visible.value = true
  result.value = null
  fileName.value = ''
  syncProfileGpa.value = true
}

defineExpose({ open })

async function onFileChange(file: { raw: File; name: string }) {
  if (!file.raw) return
  const lower = file.name.toLowerCase()
  if (!lower.endsWith('.pdf') && !lower.endsWith('.png') &&
      !lower.endsWith('.jpg') && !lower.endsWith('.jpeg') && !lower.endsWith('.webp')) {
    ElMessage.warning('支持 PDF / PNG / JPG / WEBP')
    return
  }
  if (file.raw.size > 8 * 1024 * 1024) {
    ElMessage.warning('文件过大（限 8MB）')
    return
  }
  uploading.value = true
  fileName.value = file.name
  startStageTimer()
  try {
    // 图片本地预压缩（PDF 不动），减少上传 + 阿里云推理时间
    const compressed = await compressImageIfNeeded(file.raw)
    if (compressed !== file.raw) {
      const savedKb = Math.round((file.raw.size - compressed.size) / 1024)
      console.info(`[transcript] 压缩后省 ${savedKb} KB`)
    }
    result.value = await transcriptApi.parse(compressed)
    ElMessage.success('AI 识别完成，请检查并确认')
  } catch (e: any) {
    ElMessage.error('识别失败：' + e.message)
  } finally {
    uploading.value = false
    stopStageTimer()
  }
}

function removeCourse(i: number) {
  result.value?.courses?.splice(i, 1)
}

async function confirmApply() {
  if (!result.value) return
  await ElMessageBox.confirm(
    '将把识别到的课程入库。已存在的课程会自动跳过。继续吗？',
    '确认应用',
    { confirmButtonText: '应用', cancelButtonText: '取消', type: 'warning' }
  ).catch(() => null).then(async ok => {
    if (ok === null) return
    applying.value = true
    try {
      const r = await transcriptApi.apply(result.value!, syncProfileGpa.value)
      let msg = `已应用：新增 ${r.inserted} 门课程`
      if (r.skipped > 0) msg += `，跳过 ${r.skipped} 门已存在`
      if (r.profileGpaUpdated) msg += `；GPA 已同步到画像`
      ElMessage.success(msg)
      visible.value = false
      emit('applied')
    } catch {} finally { applying.value = false }
  })
}
</script>

<template>
  <el-dialog v-model="visible" title="上传成绩单识别" width="min(820px, 95vw)" destroy-on-close>
    <div v-if="!result">
      <el-upload
        drag
        :auto-upload="false"
        :show-file-list="false"
        accept=".pdf,.png,.jpg,.jpeg,.webp"
        :on-change="onFileChange"
        v-loading="uploading"
        :element-loading-text="stageMessage || 'AI 正在识别成绩单...'"
      >
        <i class="i-ep-upload-filled text-4xl text-gray-400" />
        <div class="el-upload__text mt-2">将成绩单图片或 PDF 拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="text-xs text-gray-500 mt-2">
            支持 PDF / PNG / JPG / WEBP（最大 8MB）<br>
            推荐使用清晰的截图或导出的 PDF；扫描件也可识别
          </div>
        </template>
      </el-upload>
    </div>

    <div v-else class="space-y-4">
      <div class="text-sm text-gray-500">
        从 <span class="font-medium text-gray-700">{{ fileName }}</span> 识别到以下内容：
      </div>

      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="学生姓名">
          <span v-if="result.studentName">{{ result.studentName }}</span>
          <span v-else class="text-gray-400">未识别</span>
        </el-descriptions-item>
        <el-descriptions-item label="学号">
          <span v-if="result.studentId">{{ result.studentId }}</span>
          <span v-else class="text-gray-400">未识别</span>
        </el-descriptions-item>
        <el-descriptions-item label="学校">
          <span v-if="result.schoolName">{{ result.schoolName }}</span>
          <span v-else class="text-gray-400">未识别</span>
        </el-descriptions-item>
        <el-descriptions-item label="专业">
          <span v-if="result.majorName">{{ result.majorName }}</span>
          <span v-else class="text-gray-400">未识别</span>
        </el-descriptions-item>
        <el-descriptions-item label="计算 GPA">
          <div v-if="result.computedGpa != null">
            <div>
              <span class="font-semibold text-brand">
                {{ result.computedGpa }} / {{ scaleLabel[result.gpaScale || 100] || result.gpaScale }}
              </span>
              <span v-if="(result.gpaScale || 100) !== 4"
                    class="text-xs text-gray-500 ml-2">
                (4 分制约 {{ gpaTo4(result.computedGpa, result.gpaScale) }})
              </span>
            </div>
            <div v-if="result.officialGpaText" class="text-xs text-gray-400 mt-1">
              原文：{{ result.officialGpaText }}
            </div>
          </div>
          <span v-else class="text-gray-400">未计算</span>
        </el-descriptions-item>
        <el-descriptions-item label="总学分">
          <span v-if="result.totalCredit != null">{{ result.totalCredit }}</span>
          <span v-else class="text-gray-400">未识别</span>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="result.courses?.length">
        <div class="text-sm font-semibold mb-2">课程列表（{{ result.courses.length }} 门）</div>
        <el-table :data="result.courses" stripe size="small" max-height="320">
          <el-table-column prop="courseName" label="课程" min-width="180" show-overflow-tooltip />
          <el-table-column prop="credit" label="学分" width="80" />
          <el-table-column prop="score" label="成绩" width="80" />
          <el-table-column prop="gpa" label="GPA" width="80" />
          <el-table-column prop="semester" label="学期" width="120" />
          <el-table-column prop="category" label="类别" width="80" />
          <el-table-column label="操作" width="70" fixed="right">
            <template #default="{ $index }">
              <el-button text type="danger" size="small" @click="removeCourse($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-checkbox v-model="syncProfileGpa">
        同时把识别到的 GPA 同步到我的画像
        <span v-if="result.computedGpa != null" class="text-xs text-gray-400 ml-1">
          （{{ result.computedGpa }} / {{ scaleLabel[result.gpaScale || 100] }}）
        </span>
      </el-checkbox>

      <el-alert v-if="result.notes?.length" type="warning" :closable="false">
        <ul class="text-xs space-y-1 my-0">
          <li v-for="(n, i) in result.notes" :key="i">{{ n }}</li>
        </ul>
      </el-alert>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button v-if="result" @click="result = null; fileName = ''">重新上传</el-button>
      <el-button v-if="result" type="primary" :loading="applying" @click="confirmApply">应用</el-button>
    </template>
  </el-dialog>
</template>
