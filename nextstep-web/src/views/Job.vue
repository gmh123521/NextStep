<script setup lang="ts">
import { jobApi, type Industry, type JobPosition } from '@/api/job'
import { ElMessage } from 'element-plus'
import { formatRequestError } from '@/utils/error'

const industries = ref<Industry[]>([])
const positions = ref<JobPosition[]>([])
const salary = ref<any[]>([])
const selectedIndustry = ref<number | undefined>(undefined)
const selectedPosition = ref<JobPosition | null>(null)
const drawerOpen = ref(false)
const loading = ref(false)

async function loadPositions() {
  loading.value = true
  try {
    positions.value = await jobApi.positions({ industryId: selectedIndustry.value })
  } catch (e) {
    positions.value = []
    ElMessage.error('读取岗位列表失败：' + formatRequestError(e, '请稍后重试'))
  } finally { loading.value = false }
}

async function viewSalary(p: JobPosition) {
  selectedPosition.value = p
  drawerOpen.value = true
  try {
    salary.value = await jobApi.salary(p.id)
  } catch (e) {
    salary.value = []
    ElMessage.error('读取薪资行情失败：' + formatRequestError(e, '请稍后重试'))
  }
}

onMounted(async () => {
  try {
    industries.value = await jobApi.industries()
    await loadPositions()
  } catch (e) {
    ElMessage.error('读取就业数据失败：' + formatRequestError(e, '请稍后重试'))
  }
})
</script>

<template>
  <div class="page space-y-4">
    <el-card>
      <div class="flex flex-wrap gap-2">
        <el-tag
          :type="!selectedIndustry ? 'primary' : 'info'"
          class="cursor-pointer"
          effect="dark"
          @click="selectedIndustry = undefined; loadPositions()"
        >全部</el-tag>
        <el-tag
          v-for="i in industries"
          :key="i.id"
          :type="selectedIndustry === i.id ? 'primary' : 'info'"
          class="cursor-pointer"
          :effect="selectedIndustry === i.id ? 'dark' : 'plain'"
          @click="selectedIndustry = i.id; loadPositions()"
        >{{ i.name }}</el-tag>
      </div>
    </el-card>

    <div v-loading="loading" class="grid gap-3 grid-cols-1 sm:grid-cols-2 md:grid-cols-3">
      <el-card
        v-for="p in positions"
        :key="p.id"
        class="card-hover"
        shadow="never"
        @click="viewSalary(p)"
      >
        <div class="font-semibold mb-2">{{ p.name }}</div>
        <el-tag size="small">{{ p.category }}</el-tag>
        <div class="text-xs text-gray-500 mt-2">{{ p.description }}</div>
      </el-card>
    </div>

    <el-drawer v-model="drawerOpen" :title="(selectedPosition?.name ?? '') + ' 薪资行情'" size="50%">
      <el-table :data="salary" stripe>
        <el-table-column prop="city" label="城市" width="80" />
        <el-table-column prop="experience" label="经验" width="100" />
        <el-table-column prop="degree" label="学历" width="100" />
        <el-table-column label="月薪范围">
          <template #default="{ row }">{{ row.min_salary }} - {{ row.max_salary }}</template>
        </el-table-column>
        <el-table-column label="中位">
          <template #default="{ row }">
            <span class="text-brand font-semibold">{{ row.median_salary }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sample_size" label="样本" width="80" />
      </el-table>
    </el-drawer>
  </div>
</template>
