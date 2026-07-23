<script setup lang="ts">
import { schoolApi, type School, type SchoolMajor } from '@/api/school'

const filter = reactive({ keyword: '', level: '', pageNum: 1, pageSize: 12 })
const data = ref<School[]>([])
const total = ref(0)
const loading = ref(false)

const drawerOpen = ref(false)
const selected = ref<School | null>(null)
const majors = ref<SchoolMajor[]>([])
const enrolls = ref<any[]>([])

const isMobile = ref(window.innerWidth < 768)
window.addEventListener('resize', () => { isMobile.value = window.innerWidth < 768 })

async function load() {
  loading.value = true
  try {
    const p = await schoolApi.page(filter)
    data.value = p.records
    total.value = p.total
  } finally { loading.value = false }
}

async function viewSchool(s: School) {
  selected.value = s
  drawerOpen.value = true
  enrolls.value = []
  majors.value = await schoolApi.majors(s.id)
}

async function viewMajor(m: SchoolMajor) {
  enrolls.value = await schoolApi.admitStats(m.id)
}

onMounted(load)
</script>

<template>
  <div class="page space-y-4">
    <el-card>
      <div class="flex flex-wrap gap-3">
        <el-input v-model="filter.keyword" placeholder="搜索院校" clearable class="!w-60" @change="filter.pageNum = 1; load()" />
        <el-select v-model="filter.level" placeholder="层次" clearable class="!w-40" @change="filter.pageNum = 1; load()">
          <el-option label="C9 联盟" value="C9" />
          <el-option label="985 工程" value="985" />
          <el-option label="211 工程" value="211" />
          <el-option label="双一流" value="DOUBLE_FIRST" />
          <el-option label="普通本科" value="REGULAR" />
          <el-option label="专科" value="COLLEGE" />
        </el-select>
      </div>
    </el-card>

    <div v-loading="loading" class="grid gap-3 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      <el-card
        v-for="s in data"
        :key="s.id"
        class="card-hover"
        shadow="never"
        @click="viewSchool(s)"
      >
        <div class="font-semibold mb-2">{{ s.name }}</div>
        <div class="text-xs text-gray-500 flex gap-3">
          <span>{{ s.province }} · {{ s.city }}</span>
          <el-tag size="small" type="primary">{{ s.level }}</el-tag>
          <el-tag v-if="s.isSelfMarking" size="small" type="warning">自划线</el-tag>
        </div>
      </el-card>
    </div>

    <el-pagination
      v-if="total > 0"
      v-model:current-page="filter.pageNum"
      :page-size="filter.pageSize"
      :total="total"
      layout="prev, pager, next"
      class="justify-end flex"
      @current-change="load"
    />

    <el-drawer v-model="drawerOpen" :title="selected?.name" :size="isMobile ? '95%' : '50%'" direction="rtl">
      <div class="space-y-4">
        <div>
          <h4 class="text-sm font-semibold text-gray-700 mb-2">招生专业</h4>
          <div class="space-y-2">
            <div
              v-for="m in majors"
              :key="m.id"
              class="p-3 border border-gray-200 rounded card-hover"
              @click="viewMajor(m)"
            >
              <div class="font-medium">{{ m.majorName }}</div>
              <div class="text-xs text-gray-500">{{ m.majorCode }} · {{ m.degreeType === 'ACADEMIC' ? '学硕' : '专硕' }}</div>
            </div>
            <div v-if="!majors.length" class="text-gray-400 text-sm">暂无专业数据</div>
          </div>
        </div>

        <div v-if="enrolls.length">
          <h4 class="text-sm font-semibold text-gray-700 mb-2">历年上岸率</h4>
          <el-table :data="enrolls" stripe>
            <el-table-column prop="year" label="年份" width="80" />
            <el-table-column prop="enroll_actual" label="实录" />
            <el-table-column prop="apply_count" label="报考" />
            <el-table-column prop="cutoff_score" label="复试线" />
            <el-table-column prop="admit_rate_pct" label="上岸率">
              <template #default="{ row }">{{ row.admit_rate_pct }}%</template>
            </el-table-column>
            <el-table-column prop="apply_per_seat" label="报录比">
              <template #default="{ row }">{{ row.apply_per_seat }}:1</template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-drawer>
  </div>
</template>
