<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, type AdminStats, type AdminUser, type CrawlerJob, type GovPostRecord, type JobPositionRecord, type SchoolRecord } from '@/api/admin'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const activeTab = ref('overview')
const loading = ref(false)
const stats = ref<AdminStats | null>(null)
const users = ref<AdminUser[]>([])
const userTotal = ref(0)
const userFilters = reactive({ keyword: '', status: undefined as number | undefined, role: '' })
const userPage = ref(1)
const userPageSize = ref(10)
const sources = ref<string[]>([])
const crawlerJobs = ref<CrawlerJob[]>([])
const crawlerTotal = ref(0)
const crawlerSource = ref('')
const crawlerPage = ref(1)
const crawlerPageSize = ref(10)
const dataType = ref<'schools' | 'gov-posts' | 'job-positions'>('schools')
const dataLoading = ref(false)
const schools = ref<SchoolRecord[]>([])
const schoolTotal = ref(0)
const govPosts = ref<GovPostRecord[]>([])
const govTotal = ref(0)
const jobPositions = ref<JobPositionRecord[]>([])
const jobTotal = ref(0)
const dataPage = ref(1)
const dataPageSize = ref(10)
const dataKeyword = ref('')
const dataRows = computed<any[]>(() => dataType.value === 'schools' ? schools.value : dataType.value === 'gov-posts' ? govPosts.value : jobPositions.value)
const dataTotal = computed(() => dataType.value === 'schools' ? schoolTotal.value : dataType.value === 'gov-posts' ? govTotal.value : jobTotal.value)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogType = ref(dataType.value)
const editingId = ref<number | undefined>()
const form = reactive<Record<string, any>>({})

const statCards = computed(() => stats.value ? [
  { label: '用户总数', value: stats.value.totalUsers, icon: 'i-ep-user', color: 'text-blue-500' },
  { label: '正常用户', value: stats.value.activeUsers, icon: 'i-ep-circle-check', color: 'text-emerald-500' },
  { label: '管理员', value: stats.value.adminUsers, icon: 'i-ep-setting', color: 'text-purple-500' },
  { label: '禁用用户', value: stats.value.disabledUsers, icon: 'i-ep-circle-close', color: 'text-red-500' },
  { label: '院校数据', value: stats.value.totalSchools, icon: 'i-ep-school', color: 'text-indigo-500' },
  { label: '考公岗位', value: stats.value.totalGovPosts, icon: 'i-ep-document', color: 'text-amber-500' },
  { label: '就业岗位', value: stats.value.totalJobPositions, icon: 'i-ep-briefcase', color: 'text-cyan-500' },
  { label: '薪资统计', value: stats.value.totalSalaryStats, icon: 'i-ep-data-analysis', color: 'text-pink-500' }
] : [] )

async function loadOverview() {
  stats.value = await adminApi.overview()
}

async function loadUsers() {
  loading.value = true
  try {
    const result = await adminApi.users({ pageNum: userPage.value, pageSize: userPageSize.value, ...userFilters })
    users.value = result.records
    userTotal.value = result.total
  } finally { loading.value = false }
}

async function changeUserStatus(row: any) {
  try {
    if (row.status === 0) await ElMessageBox.confirm(`确认禁用用户“${row.username}”吗？`, '操作确认')
    row.status === 0 ? await adminApi.disableUser(row.id) : await adminApi.enableUser(row.id)
    ElMessage.success(row.status === 0 ? '用户已禁用' : '用户已启用')
    await loadUsers()
  } catch (error) { if (error !== 'cancel' && error !== 'close') throw error }
}

async function changeUserRole(row: any) {
  const role = row.role === 'ADMIN' ? 'USER' : 'ADMIN'
  try {
    await ElMessageBox.confirm(`确认将“${row.username}”设为${role === 'ADMIN' ? '管理员' : '普通用户'}吗？`, '操作确认')
    await adminApi.setUserRole(row.id, role)
    ElMessage.success('角色已更新')
    await loadUsers()
  } catch (error) { if (error !== 'cancel' && error !== 'close') throw error }
}

async function loadCrawlerJobs() {
  loading.value = true
  try {
    const result = await adminApi.crawlerJobs({ pageNum: crawlerPage.value, pageSize: crawlerPageSize.value, source: crawlerSource.value || undefined })
    crawlerJobs.value = result.records
    crawlerTotal.value = result.total
  } finally { loading.value = false }
}

async function runCrawler(source: string) {
  try {
    await ElMessageBox.confirm(`确认立即运行 ${source} 数据采集吗？`, '操作确认')
    await adminApi.runCrawler(source)
    ElMessage.success('采集任务已执行')
    await loadCrawlerJobs()
  } catch (error) { if (error !== 'cancel' && error !== 'close') throw error }
}

async function loadData() {
  dataLoading.value = true
  try {
    if (dataType.value === 'schools') {
      const result = await adminApi.schools({ pageNum: dataPage.value, pageSize: dataPageSize.value, keyword: dataKeyword.value || undefined })
      schools.value = result.records; schoolTotal.value = result.total
    } else if (dataType.value === 'gov-posts') {
      const result = await adminApi.govPosts({ pageNum: dataPage.value, pageSize: dataPageSize.value, keyword: dataKeyword.value || undefined })
      govPosts.value = result.records; govTotal.value = result.total
    } else {
      const result = await adminApi.jobPositions({ pageNum: dataPage.value, pageSize: dataPageSize.value, keyword: dataKeyword.value || undefined })
      jobPositions.value = result.records; jobTotal.value = result.total
    }
  } finally { dataLoading.value = false }
}

function resetForm(type: typeof dataType.value, row?: any) {
  dialogType.value = type
  editingId.value = row?.id
  Object.keys(form).forEach(key => delete form[key])
  Object.assign(form, row ? { ...row } : type === 'schools' ? { name: '', province: '', city: '', level: '', type: '' } : type === 'gov-posts' ? { year: new Date().getFullYear(), deptName: '', postName: '', province: '', examType: '' } : { name: '', category: '', description: '' })
  dialogTitle.value = row ? '编辑数据' : '新增数据'
  dialogVisible.value = true
}

async function saveData() {
  if (dialogType.value === 'schools') await adminApi.saveSchool({ ...form, id: editingId.value } as SchoolRecord)
  else if (dialogType.value === 'gov-posts') await adminApi.saveGovPost({ ...form, id: editingId.value } as GovPostRecord)
  else await adminApi.saveJobPosition({ ...form, id: editingId.value } as JobPositionRecord)
  dialogVisible.value = false
  ElMessage.success('保存成功')
  await loadData()
}

async function removeData(type: typeof dataType.value, id: number) {
  try {
    await ElMessageBox.confirm('删除后不可恢复，确认继续吗？', '操作确认')
    if (type === 'schools') await adminApi.deleteSchool(id)
    else if (type === 'gov-posts') await adminApi.deleteGovPost(id)
    else await adminApi.deleteJobPosition(id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (error) { if (error !== 'cancel' && error !== 'close') throw error }
}

watch(dataType, () => { dataPage.value = 1; loadData() })
watch(activeTab, async (tab) => {
  if (tab === 'overview' && !stats.value) await loadOverview()
  if (tab === 'users' && !users.value.length) await loadUsers()
  if (tab === 'crawler' && !crawlerJobs.value.length) { sources.value = await adminApi.sources(); await loadCrawlerJobs() }
  if (tab === 'data' && !schools.value.length) await loadData()
})

onMounted(loadOverview)
</script>

<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="flex-between flex-wrap gap-2">
          <div>
            <span class="font-semibold text-lg">后台管理</span>
            <span class="text-sm text-gray-500 ml-3">当前账号：{{ userStore.me?.username }}</span>
          </div>
          <el-tag type="danger">管理员</el-tag>
        </div>
      </template>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="系统概览" name="overview">
          <el-alert title="管理员身份已验证" type="success" :closable="false" show-icon class="mb-4" />
          <div class="grid gap-4 grid-cols-2 md:grid-cols-4">
            <el-card v-for="card in statCards" :key="card.label" shadow="never">
              <div class="flex-between"><span class="text-sm text-gray-500">{{ card.label }}</span><i :class="[card.icon, card.color, 'text-xl']" /></div>
              <div class="text-2xl font-semibold mt-3">{{ card.value }}</div>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane label="用户管理" name="users">
          <div class="flex flex-wrap gap-2 mb-4">
            <el-input v-model="userFilters.keyword" placeholder="用户名/昵称" clearable class="w-52" @keyup.enter="userPage = 1; loadUsers()" />
            <el-select v-model="userFilters.status" placeholder="用户状态" clearable class="w-32"><el-option label="正常" :value="0" /><el-option label="禁用" :value="1" /></el-select>
            <el-select v-model="userFilters.role" placeholder="角色" clearable class="w-32"><el-option label="普通用户" value="USER" /><el-option label="管理员" value="ADMIN" /></el-select>
            <el-button type="primary" @click="userPage = 1; loadUsers()">查询</el-button>
          </div>
          <el-table v-loading="loading" :data="users" stripe>
            <el-table-column prop="username" label="用户名" min-width="130" />
            <el-table-column prop="nickname" label="昵称" min-width="110" />
            <el-table-column prop="email" label="邮箱" min-width="170" />
            <el-table-column label="角色" width="100"><template #default="{ row }"><el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">{{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}</el-tag></template></el-table-column>
            <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 0 ? 'success' : 'info'">{{ row.status === 0 ? '正常' : '禁用' }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="190" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="changeUserStatus(row)">{{ row.status === 0 ? '禁用' : '启用' }}</el-button><el-button link type="warning" @click="changeUserRole(row)">{{ row.role === 'ADMIN' ? '降为普通用户' : '设为管理员' }}</el-button></template></el-table-column>
          </el-table>
          <el-pagination v-model:current-page="userPage" v-model:page-size="userPageSize" class="mt-4 justify-end" layout="total, sizes, prev, pager, next" :total="userTotal" @current-change="loadUsers" @size-change="userPage = 1; loadUsers" />
        </el-tab-pane>

        <el-tab-pane label="数据维护" name="data">
          <div class="flex flex-wrap gap-2 mb-4"><el-select v-model="dataType" class="w-36"><el-option label="院校" value="schools" /><el-option label="考公岗位" value="gov-posts" /><el-option label="就业岗位" value="job-positions" /></el-select><el-input v-model="dataKeyword" placeholder="关键词" clearable class="w-52" @keyup.enter="dataPage = 1; loadData()" /><el-button type="primary" @click="dataPage = 1; loadData()">查询</el-button><el-button @click="resetForm(dataType)">新增</el-button></div>
          <el-table v-loading="dataLoading" :data="dataRows" stripe>
            <template v-if="dataType === 'schools'"><el-table-column prop="name" label="院校名称" min-width="180" /><el-table-column prop="province" label="省份" width="100" /><el-table-column prop="city" label="城市" width="100" /><el-table-column prop="level" label="层次" width="100" /><el-table-column label="操作" width="140"><template #default="{ row }"><el-button link type="primary" @click="resetForm('schools', row)">编辑</el-button><el-button link type="danger" @click="removeData('schools', row.id)">删除</el-button></template></el-table-column></template>
            <template v-else-if="dataType === 'gov-posts'"><el-table-column prop="year" label="年份" width="80" /><el-table-column prop="deptName" label="部门" min-width="180" /><el-table-column prop="postName" label="岗位" min-width="180" /><el-table-column prop="province" label="省份" width="100" /><el-table-column label="操作" width="140"><template #default="{ row }"><el-button link type="primary" @click="resetForm('gov-posts', row)">编辑</el-button><el-button link type="danger" @click="removeData('gov-posts', row.id)">删除</el-button></template></el-table-column></template>
            <template v-else><el-table-column prop="name" label="岗位名称" min-width="180" /><el-table-column prop="category" label="分类" width="130" /><el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip /><el-table-column label="操作" width="140"><template #default="{ row }"><el-button link type="primary" @click="resetForm('job-positions', row)">编辑</el-button><el-button link type="danger" @click="removeData('job-positions', row.id)">删除</el-button></template></el-table-column></template>
          </el-table>
          <el-pagination v-model:current-page="dataPage" v-model:page-size="dataPageSize" class="mt-4 justify-end" layout="total, sizes, prev, pager, next" :total="dataTotal" @current-change="loadData" @size-change="dataPage = 1; loadData" />
        </el-tab-pane>

        <el-tab-pane label="采集任务" name="crawler">
          <div class="flex flex-wrap gap-2 mb-4"><el-button v-for="source in sources" :key="source" type="primary" plain @click="runCrawler(source)">运行 {{ source }}</el-button><el-select v-model="crawlerSource" placeholder="筛选数据源" clearable class="w-40" @change="crawlerPage = 1; loadCrawlerJobs()"><el-option v-for="source in sources" :key="source" :label="source" :value="source" /></el-select><el-button @click="loadCrawlerJobs">刷新</el-button></div>
          <el-table v-loading="loading" :data="crawlerJobs" stripe><el-table-column prop="source" label="数据源" width="130" /><el-table-column prop="triggerBy" label="触发方式" width="100" /><el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'FAILED' ? 'danger' : 'warning'">{{ row.status }}</el-tag></template></el-table-column><el-table-column prop="fetched" label="抓取数" width="80" /><el-table-column prop="inserted" label="写入数" width="80" /><el-table-column prop="skipped" label="跳过数" width="80" /><el-table-column prop="message" label="结果" min-width="220" show-overflow-tooltip /><el-table-column prop="startedAt" label="开始时间" min-width="170" /></el-table>
          <el-pagination v-model:current-page="crawlerPage" v-model:page-size="crawlerPageSize" class="mt-4 justify-end" layout="total, sizes, prev, pager, next" :total="crawlerTotal" @current-change="loadCrawlerJobs" @size-change="crawlerPage = 1; loadCrawlerJobs" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form label-width="90px">
        <template v-if="dialogType === 'schools'"><el-form-item label="院校名称"><el-input v-model="form.name" /></el-form-item><el-form-item label="省份"><el-input v-model="form.province" /></el-form-item><el-form-item label="城市"><el-input v-model="form.city" /></el-form-item><el-form-item label="层次"><el-input v-model="form.level" /></el-form-item><el-form-item label="类型"><el-input v-model="form.type" /></el-form-item></template>
        <template v-else-if="dialogType === 'gov-posts'"><el-form-item label="年份"><el-input-number v-model="form.year" :min="2000" :max="2100" /></el-form-item><el-form-item label="部门"><el-input v-model="form.deptName" /></el-form-item><el-form-item label="岗位名称"><el-input v-model="form.postName" /></el-form-item><el-form-item label="省份"><el-input v-model="form.province" /></el-form-item><el-form-item label="考试类型"><el-input v-model="form.examType" /></el-form-item></template>
        <template v-else><el-form-item label="岗位名称"><el-input v-model="form.name" /></el-form-item><el-form-item label="分类"><el-input v-model="form.category" /></el-form-item><el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item></template>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="saveData">保存</el-button></template>
    </el-dialog>
  </div>
</template>
