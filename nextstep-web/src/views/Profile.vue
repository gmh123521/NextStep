<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { profileApi, type UserProfile } from '@/api/profile'
import ResumeUploader from '@/components/ResumeUploader.vue'
import TranscriptUploader from '@/components/TranscriptUploader.vue'
import ExperienceList from '@/components/ExperienceList.vue'

const resumeUploader = ref<InstanceType<typeof ResumeUploader> | null>(null)
const transcriptUploader = ref<InstanceType<typeof TranscriptUploader> | null>(null)
const experienceList = ref<InstanceType<typeof ExperienceList> | null>(null)

async function onResumeApplied() {
  await load()
  experienceList.value?.reload()
}

async function onTranscriptApplied() {
  await load()
}

const form = reactive<UserProfile>({})
const loading = ref(false)
const saving = ref(false)
const loaded = ref(false)

const levelOptions = [
  { v: 'C9',           l: 'C9 联盟' },
  { v: '985',          l: '985 工程' },
  { v: '211',          l: '211 工程' },
  { v: 'DOUBLE_FIRST', l: '双一流' },
  { v: 'REGULAR',      l: '普通本科' },
  { v: 'COLLEGE',      l: '专科' }
]

const degreeOptions = [
  { v: 'BACHELOR', l: '本科' },
  { v: 'MASTER',   l: '硕士' },
  { v: 'DOCTOR',   l: '博士' }
]

// 语种 + 各自的等级体系
type LanguageLevel = { v: string; l: string; max?: number; hint?: string }
type LanguageOption = { code: string; label: string; levels: LanguageLevel[] }
const languageOptions: LanguageOption[] = [
  {
    code: 'EN',
    label: '英语',
    levels: [
      { v: 'CET4',  l: '四级（CET-4）',  max: 710 },
      { v: 'CET6',  l: '六级（CET-6）',  max: 710 },
      { v: 'TEM4',  l: '专四（TEM-4）',  max: 100 },
      { v: 'TEM8',  l: '专八（TEM-8）',  max: 100 },
      { v: 'IELTS', l: '雅思（IELTS）',  max: 90, hint: '请输入 ×10 后的整数，例如 6.5 → 65' },
      { v: 'TOEFL', l: '托福（TOEFL）',  max: 120 }
    ]
  },
  {
    code: 'JP',
    label: '日语',
    levels: [
      { v: 'JLPT_N1', l: 'JLPT N1', max: 180 },
      { v: 'JLPT_N2', l: 'JLPT N2', max: 180 },
      { v: 'JLPT_N3', l: 'JLPT N3', max: 180 },
      { v: 'JLPT_N4', l: 'JLPT N4', max: 180 },
      { v: 'JLPT_N5', l: 'JLPT N5', max: 180 }
    ]
  },
  {
    code: 'KR',
    label: '韩语',
    levels: [
      { v: 'TOPIK1', l: 'TOPIK Ⅰ', max: 200 },
      { v: 'TOPIK2', l: 'TOPIK Ⅱ', max: 300 }
    ]
  },
  { code: 'OTHER', label: '其他', levels: [{ v: 'OTHER', l: '其他证书', max: undefined }] },
  { code: 'NONE',  label: '无 / 暂未通过', levels: [] }
]

const gpaScaleOptions = [
  { v: 4,   l: '4 分制' },
  { v: 5,   l: '5 分制' },
  { v: 100, l: '百分制' }
]

const pathOptions = [
  { v: 'PG', l: '考研' },
  { v: 'CS', l: '考公' },
  { v: 'EM', l: '就业' }
]

// 文案改成"问句"形式，含义自明
const stageOptions = [
  { v: 'IN_SCHOOL', l: '在校学习（还没开始正式备考/求职）' },
  { v: 'PREPARING', l: '正在全职备考（考研/考公）' },
  { v: 'JOB_HUNTING', l: '正在找工作（投简历 / 面试中）' },
  { v: 'GRADUATED', l: '已毕业，暂未就业' },
  { v: 'EMPLOYED',  l: '已就业' }
]

// 常见城市预设，支持自定义输入
const cityPresets = [
  '北京', '上海', '广州', '深圳', '杭州', '南京', '苏州', '成都',
  '武汉', '西安', '重庆', '天津', '长沙', '青岛', '厦门', '宁波', '济南', '合肥'
]

const gradeOptions = computed(() => {
  if (form.degreeType === 'DOCTOR') return [
    { v: 5, l: '博一' }, { v: 6, l: '博二' }, { v: 7, l: '博三 / 应届博士' }
  ]
  if (form.degreeType === 'MASTER') return [
    { v: 5, l: '研一' }, { v: 6, l: '研二' }, { v: 7, l: '研三 / 应届硕士' }
  ]
  return [
    { v: 1, l: '大一' }, { v: 2, l: '大二' }, { v: 3, l: '大三' }, { v: 4, l: '大四 / 应届本科' }
  ]
})

// 语种与对应等级（基于已存的 englishLevel 反推）
const selectedLanguage = ref<string>('')
const selectedLevelMeta = computed(() => {
  const lang = languageOptions.find(l => l.code === selectedLanguage.value)
  if (!lang) return undefined
  return lang.levels.find(lv => lv.v === form.englishLevel)
})
const levelOptionsForLang = computed(() => {
  return languageOptions.find(l => l.code === selectedLanguage.value)?.levels ?? []
})

// 偏好城市：tag 数组 ↔ 后端的逗号字符串
const cityArr = computed({
  get: () => form.preferredRegions ? form.preferredRegions.split(/[,，]/).map(s => s.trim()).filter(Boolean) : [],
  set: (v: string[]) => { form.preferredRegions = v.join(',') }
})

// 偏好行业：仅勾选 EM（就业）时收集
const industryArr = computed({
  get: () => form.preferredIndustries ? form.preferredIndustries.split(/[,，]/).map(s => s.trim()).filter(Boolean) : [],
  set: (v: string[]) => { form.preferredIndustries = v.join(',') }
})
const industryPresets = ['互联网/IT', '金融', '教育', '制造业', '咨询', '医疗', '游戏', '电商', '汽车', '半导体', '新能源', '生物医药']

// 学科门类
const majorCategoryOptions = [
  '哲学', '经济学', '法学', '教育学', '文学', '历史学',
  '理学', '工学', '农学', '医学', '管理学', '艺术学', '军事学'
]

// 专业 → 学科门类自动映射（关键词匹配）
const MAJOR_TO_CATEGORY: { keywords: string[]; category: string }[] = [
  { keywords: ['计算机', '软件', '人工智能', '电子', '通信', '自动化', '机械', '土木', '建筑', '材料', '化工', '能源', '生物工程', '航空', '车辆', '环境', '工业', '安全', '矿', '冶金', '纺织', '轻工', '光电', '集成电路'], category: '工学' },
  { keywords: ['数学', '物理', '化学', '天文', '地理', '大气', '海洋', '生物科学', '生态', '心理学', '统计学'], category: '理学' },
  { keywords: ['临床', '基础医学', '口腔', '中医', '中药', '药学', '护理', '预防医学', '医学', '法医'], category: '医学' },
  { keywords: ['经济', '金融', '财政', '税收', '保险', '国际贸易', '投资', '会计'], category: '经济学' },
  { keywords: ['工商管理', '市场营销', '旅游管理', '物流', '人力资源', '行政管理', '电子商务', '管理'], category: '管理学' },
  { keywords: ['法学', '法律', '社会学', '社会工作', '政治学'], category: '法学' },
  { keywords: ['教育', '学前', '小学教育', '体育'], category: '教育学' },
  { keywords: ['汉语言', '英语', '日语', '韩语', '法语', '德语', '俄语', '西班牙语', '阿拉伯语', '新闻', '广告', '广播', '编辑出版', '中文'], category: '文学' },
  { keywords: ['历史', '考古', '文物', '博物'], category: '历史学' },
  { keywords: ['农学', '园艺', '林学', '动物', '水产', '食品科学'], category: '农学' },
  { keywords: ['美术', '音乐', '舞蹈', '设计', '戏剧', '影视', '动画', '艺术'], category: '艺术学' },
  { keywords: ['哲学', '逻辑学', '宗教'], category: '哲学' }
]

function inferMajorCategory(major?: string): string | null {
  if (!major) return null
  for (const { keywords, category } of MAJOR_TO_CATEGORY) {
    for (const kw of keywords) {
      if (major.includes(kw)) return category
    }
  }
  return null
}

// 班级排名分档（用户更容易表达"前 X%"而非具体百分位）
const rankBands = [
  { v: 5,   l: '前 5%' },
  { v: 15,  l: '前 15%' },
  { v: 30,  l: '前 30%' },
  { v: 50,  l: '前 50%' },
  { v: 75,  l: '后 50%' },
  { v: 95,  l: '后 25%' }
]

const targetPathArr = computed({
  get: () => form.targetPaths ? form.targetPaths.split(',').filter(Boolean) : [],
  set: (v: string[]) => { form.targetPaths = v.join(',') }
})

const showBudget = computed(() => targetPathArr.value.includes('PG') || targetPathArr.value.includes('CS'))
const englishScoreMax = computed(() => selectedLevelMeta.value?.max ?? 999)
const englishScoreHint = computed(() => selectedLevelMeta.value?.hint)
const gpaMax = computed(() => form.gpaScale === 100 ? 100 : form.gpaScale === 5 ? 5 : 4)

/** WES 分段映射：当前制式 → 4 分制 */
function gpaToFour(v: number, scale: number): number {
  if (scale === 100) {
    if (v >= 90) return 4.0
    if (v >= 85) return 3.7
    if (v >= 82) return 3.3
    if (v >= 78) return 3.0
    if (v >= 75) return 2.7
    if (v >= 72) return 2.3
    if (v >= 68) return 2.0
    if (v >= 64) return 1.5
    if (v >= 60) return 1.0
    return 0
  }
  if (scale === 5) {
    if (v >= 4.5) return 4.0
    if (v >= 4.0) return 3.6
    if (v >= 3.5) return 3.2
    if (v >= 3.0) return 2.7
    if (v >= 2.5) return 2.0
    return 1.0
  }
  return v
}

/** 4 分制 → 百分制（取每档区间中点附近的代表值） */
function fourToPercent(v: number): number {
  if (v >= 4.0) return 92
  if (v >= 3.7) return 87
  if (v >= 3.3) return 83
  if (v >= 3.0) return 80
  if (v >= 2.7) return 76
  if (v >= 2.3) return 73
  if (v >= 2.0) return 70
  if (v >= 1.5) return 66
  if (v >= 1.0) return 62
  return 50
}

/** 4 分制 → 5 分制（取每档区间代表值） */
function fourToFive(v: number): number {
  if (v >= 4.0) return 4.7
  if (v >= 3.6) return 4.2
  if (v >= 3.2) return 3.7
  if (v >= 2.7) return 3.2
  if (v >= 2.0) return 2.7
  return 2.0
}

const gpaOutOfRange = computed(() => {
  if (form.gpa == null) return false
  const s = form.gpaScale || 4
  const maxVal = s === 100 ? 100 : s === 5 ? 5 : 4
  return Number(form.gpa) > maxVal
})

/** 当前 GPA 在三个制式下的等价值（用户原值始终不被改写，这里只是展示） */
const gpaInAllScales = computed(() => {
  if (form.gpa == null || gpaOutOfRange.value) return null
  const s = form.gpaScale || 4
  const v = Number(form.gpa)
  const fourScale = Math.max(0, Math.min(4, gpaToFour(v, s)))
  return {
    four:    s === 4   ? v : fourScale,
    five:    s === 5   ? v : fourToFive(fourScale),
    percent: s === 100 ? v : fourToPercent(fourScale)
  }
})

// 实时完整度（与后端 calcCompleteness 保持一致：核心字段 + 路径条件性字段）
const CORE_FIELDS: (keyof UserProfile)[] = [
  'currentSchool', 'schoolLevel', 'currentMajor', 'majorCategory', 'degreeType',
  'gradeYear', 'gpa', 'classRankPct', 'englishLevel',
  'targetPaths', 'preferredRegions',
  'riskAppetite'
]
const EM_FIELDS: (keyof UserProfile)[] = ['preferredIndustries', 'salaryExpectation']
const PGCS_FIELDS: (keyof UserProfile)[] = ['monthlyBudget']

const liveCompleteness = computed(() => {
  const denominator: (keyof UserProfile)[] = [...CORE_FIELDS]
  const paths = form.targetPaths || ''
  if (paths.includes('EM')) denominator.push(...EM_FIELDS)
  if (paths.includes('PG') || paths.includes('CS')) denominator.push(...PGCS_FIELDS)

  let filled = 0
  for (const k of denominator) {
    const v = form[k]
    if (v === null || v === undefined) continue
    if (typeof v === 'string' && !v.trim()) continue
    if (typeof v === 'number' && v === 0 && k !== 'monthlyBudget' && k !== 'salaryExpectation') continue
    filled++
  }
  return Math.round(100 * filled / denominator.length)
})

// 反应式：当 englishLevel 变化（如从后端加载），同步 selectedLanguage
watch(() => form.englishLevel, (newLevel) => {
  if (!newLevel) { selectedLanguage.value = ''; return }
  for (const lang of languageOptions) {
    if (lang.levels.some(lv => lv.v === newLevel)) {
      selectedLanguage.value = lang.code
      return
    }
  }
}, { immediate: false })

watch(() => form.degreeType, () => { if (loaded.value) form.gradeYear = undefined })
watch(selectedLanguage, () => { if (loaded.value) { form.englishLevel = undefined; form.englishScore = undefined } })
watch(() => form.englishLevel, () => { if (loaded.value) form.englishScore = undefined })

// 专业变化时尝试自动推断学科门类（仅在用户没手动选过时填充）
watch(() => form.currentMajor, (m) => {
  if (!loaded.value) return
  if (form.majorCategory) return  // 用户已经手动选过，不覆盖
  const inferred = inferMajorCategory(m)
  if (inferred) form.majorCategory = inferred
})

async function load() {
  loading.value = true
  try {
    const p = await profileApi.get()
    if (p) {
      // 派生字段（has_* / profileCompleteness）每次都用后端实时算的值覆盖，
      // 避免"删了经历但前端 form 残留 1"的问题
      ;(['hasInternship','hasResearch','hasCompetition','hasPaper'] as const).forEach(k => { form[k] = 0 })
      Object.assign(form, p)
    }
    if (!form.gpaScale) form.gpaScale = 4
    // 反推语种
    if (form.englishLevel) {
      for (const lang of languageOptions) {
        if (lang.levels.some(lv => lv.v === form.englishLevel)) {
          selectedLanguage.value = lang.code; break
        }
      }
    }
  } finally {
    loading.value = false
    nextTick(() => { loaded.value = true })
  }
}

async function save() {
  saving.value = true
  try {
    const saved = await profileApi.upsert(form)
    Object.assign(form, saved)
    ElMessage.success('已保存')
  } catch {} finally { saving.value = false }
}

function clearForm() {
  for (const k of Object.keys(form)) {
    (form as any)[k] = undefined
  }
  form.gpaScale = 4
  selectedLanguage.value = ''
}

async function handleClear() {
  try {
    await ElMessageBox.confirm(
      '将清空当前表单的所有内容（不影响已保存的数据，除非你再次点击"保存"）。继续吗？',
      '清空表单',
      { confirmButtonText: '清空', cancelButtonText: '取消', type: 'warning' }
    )
    clearForm()
    ElMessage.info('已清空')
  } catch { /* 用户取消 */ }
}

onMounted(load)
</script>

<template>
  <div class="page" v-loading="loading">
    <el-card>
      <template #header>
        <div class="flex-between flex-wrap gap-2">
          <span class="font-semibold">我的画像</span>
          <div class="flex items-center gap-3 flex-wrap">
            <el-button size="small" type="primary" plain @click="resumeUploader?.open()">
              上传简历自动填充
            </el-button>
            <el-button size="small" type="success" plain @click="transcriptUploader?.open()">
              上传成绩单识别
            </el-button>
            <div class="flex items-center gap-2 min-w-50">
              <span class="text-sm text-gray-500">完整度</span>
              <el-progress :percentage="liveCompleteness" :stroke-width="8" class="!w-32" />
            </div>
          </div>
        </div>
      </template>

      <el-form :model="form" label-position="top">
        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <h4 class="text-sm text-gray-500 mb-3 mt-2">📚 学业基础</h4>

            <el-form-item label="当前院校">
              <el-input v-model="form.currentSchool" placeholder="如：清华大学" />
            </el-form-item>

            <el-form-item label="院校层次">
              <el-select v-model="form.schoolLevel" placeholder="请选择" clearable class="w-full">
                <el-option v-for="o in levelOptions" :key="o.v" :label="o.l" :value="o.v" />
              </el-select>
            </el-form-item>

            <el-form-item label="当前专业">
              <el-input v-model="form.currentMajor" placeholder="如：计算机科学与技术" />
            </el-form-item>

            <el-form-item label="学科门类">
              <el-select v-model="form.majorCategory" placeholder="选择对应学科门类" clearable class="w-full">
                <el-option v-for="o in majorCategoryOptions" :key="o" :label="o" :value="o" />
              </el-select>
            </el-form-item>

            <el-form-item label="学历">
              <el-select v-model="form.degreeType" placeholder="请选择" clearable class="w-full">
                <el-option v-for="o in degreeOptions" :key="o.v" :label="o.l" :value="o.v" />
              </el-select>
            </el-form-item>

            <el-form-item label="年级">
              <el-select v-model="form.gradeYear" placeholder="请先选择学历" :disabled="!form.degreeType" clearable class="w-full">
                <el-option v-for="o in gradeOptions" :key="o.v" :label="o.l" :value="o.v" />
              </el-select>
            </el-form-item>

            <el-form-item label="GPA 制式">
              <el-radio-group v-model="form.gpaScale">
                <el-radio v-for="o in gpaScaleOptions" :key="o.v" :value="o.v">{{ o.l }}</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item :label="`GPA（满分 ${gpaMax}）`">
              <el-input-number
                v-model="form.gpa" :min="0" :max="gpaMax"
                :step="form.gpaScale === 100 ? 1 : 0.1"
                :precision="form.gpaScale === 100 ? 1 : 2"
                class="w-full"
              />
              <div v-if="gpaOutOfRange" class="text-xs text-red-500 mt-1">
                超出当前制式上限（满分 {{ gpaMax }}），请检查
              </div>
              <div v-else-if="gpaInAllScales" class="text-xs text-gray-400 mt-1 flex flex-wrap gap-x-3">
                <span v-if="(form.gpaScale || 4) !== 4">4 分制约 {{ gpaInAllScales.four.toFixed(2) }}</span>
                <span v-if="(form.gpaScale || 4) !== 5">5 分制约 {{ gpaInAllScales.five.toFixed(2) }}</span>
                <span v-if="(form.gpaScale || 4) !== 100">百分制约 {{ gpaInAllScales.percent.toFixed(0) }}</span>
                <span class="text-gray-300">· 评分按 4 分制对比</span>
              </div>
            </el-form-item>

            <el-form-item label="班级排名">
              <el-select v-model="form.classRankPct" placeholder="选最贴近的档位" clearable class="w-full">
                <el-option v-for="o in rankBands" :key="o.v" :label="o.l" :value="o.v" />
              </el-select>
            </el-form-item>

            <h4 class="text-sm text-gray-500 mb-3 mt-6">📝 自我描述（选填，让 AI 解读更精准）</h4>

            <el-form-item label="兴趣方向">
              <el-input v-model="form.interests" type="textarea" :rows="2"
                        placeholder="例如：对 AI / 后端架构 感兴趣；想做技术管理类工作..." />
            </el-form-item>

            <el-form-item label="自身优势">
              <el-input v-model="form.strengths" type="textarea" :rows="2"
                        placeholder="例如：算法基础扎实、项目经验丰富、表达能力好..." />
            </el-form-item>

            <el-form-item label="待补的短板">
              <el-input v-model="form.weaknesses" type="textarea" :rows="2"
                        placeholder="例如：英语口语弱、缺乏大厂实习、写作偏弱..." />
            </el-form-item>

            <el-form-item label="已记录的经历类型">
              <div class="flex flex-wrap gap-2 text-xs">
                <el-tag :type="form.hasInternship ? 'success' : 'info'" :effect="form.hasInternship ? 'dark' : 'plain'">
                  {{ form.hasInternship ? '✓ ' : '○ ' }}实习
                </el-tag>
                <el-tag :type="form.hasResearch ? 'success' : 'info'" :effect="form.hasResearch ? 'dark' : 'plain'">
                  {{ form.hasResearch ? '✓ ' : '○ ' }}科研
                </el-tag>
                <el-tag :type="form.hasCompetition ? 'success' : 'info'" :effect="form.hasCompetition ? 'dark' : 'plain'">
                  {{ form.hasCompetition ? '✓ ' : '○ ' }}竞赛
                </el-tag>
                <el-tag :type="form.hasPaper ? 'success' : 'info'" :effect="form.hasPaper ? 'dark' : 'plain'">
                  {{ form.hasPaper ? '✓ ' : '○ ' }}论文
                </el-tag>
              </div>
              <div class="text-xs text-gray-400 mt-2">
                来自下方"我的经历"列表（自动汇总，不在这里直接编辑）
              </div>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :md="12">
            <h4 class="text-sm text-gray-500 mb-3 mt-2">🎯 能力 / 偏好</h4>

            <el-form-item label="语种">
              <el-select v-model="selectedLanguage" placeholder="请选择主修语种" clearable class="w-full">
                <el-option v-for="l in languageOptions" :key="l.code" :label="l.label" :value="l.code" />
              </el-select>
            </el-form-item>

            <el-form-item
              v-if="selectedLanguage && levelOptionsForLang.length"
              label="语言等级"
            >
              <el-select v-model="form.englishLevel" placeholder="请选择" clearable class="w-full">
                <el-option v-for="lv in levelOptionsForLang" :key="lv.v" :label="lv.l" :value="lv.v" />
              </el-select>
            </el-form-item>

            <el-form-item
              v-if="form.englishLevel && selectedLevelMeta"
              :label="`分数${englishScoreMax !== 999 ? '（满分 ' + englishScoreMax + '）' : ''}`"
            >
              <el-input-number
                v-model="form.englishScore" :min="0"
                :max="englishScoreMax !== 999 ? englishScoreMax : undefined"
                class="w-full"
              />
              <div v-if="englishScoreHint" class="text-xs text-gray-400 mt-1">{{ englishScoreHint }}</div>
            </el-form-item>

            <el-form-item label="目标路径（可多选）">
              <el-checkbox-group v-model="targetPathArr">
                <el-checkbox v-for="p in pathOptions" :key="p.v" :value="p.v">{{ p.l }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <el-form-item label="偏好城市">
              <el-select
                v-model="cityArr" multiple filterable allow-create default-first-option
                placeholder="选择或输入城市后回车" class="w-full"
              >
                <el-option v-for="c in cityPresets" :key="c" :label="c" :value="c" />
              </el-select>
              <div class="text-xs text-gray-400 mt-1">选择内置城市或直接输入后回车，可多选</div>
            </el-form-item>

            <el-form-item v-if="targetPathArr.includes('EM')" label="期望月薪（元）">
              <el-input-number v-model="form.salaryExpectation" :min="0" :step="1000" class="w-full" />
            </el-form-item>

            <el-form-item v-if="targetPathArr.includes('EM')" label="偏好行业">
              <el-select
                v-model="industryArr" multiple filterable allow-create default-first-option
                placeholder="选择或输入行业后回车" class="w-full"
              >
                <el-option v-for="i in industryPresets" :key="i" :label="i" :value="i" />
              </el-select>
              <div class="text-xs text-gray-400 mt-1">用于推荐对口岗位</div>
            </el-form-item>

            <el-form-item label="风险偏好（1 保守 → 5 激进）">
              <el-slider v-model="form.riskAppetite" :min="1" :max="5" show-stops />
            </el-form-item>

            <el-form-item v-if="showBudget" label="每月可承受备考开销（元）">
              <el-input-number v-model="form.monthlyBudget" :min="0" :step="500" class="w-full" />
              <div class="text-xs text-gray-400 mt-1">备考期间每月房租 / 资料 / 课程等可投入预算</div>
            </el-form-item>

            <el-form-item label="目前所处阶段">
              <el-select v-model="form.currentStatus" placeholder="选择最贴近你现状的描述" clearable class="w-full">
                <el-option v-for="o in stageOptions" :key="o.v" :label="o.l" :value="o.v" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="flex justify-end gap-3 pt-4 border-t border-gray-100 mt-4">
          <el-button @click="handleClear">清空</el-button>
          <el-button @click="load">撤销修改</el-button>
          <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        </div>
      </el-form>
    </el-card>

    <ResumeUploader ref="resumeUploader" @applied="onResumeApplied" />
    <TranscriptUploader ref="transcriptUploader" @applied="onTranscriptApplied" />

    <ExperienceList ref="experienceList" @changed="load" />
  </div>
</template>
