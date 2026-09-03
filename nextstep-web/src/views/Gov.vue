<script setup lang="ts">
import { govApi } from '@/api/gov'
import { ElMessage } from 'element-plus'
import { formatRequestError } from '@/utils/error'

const filter = reactive<{ year?: number; examType?: string; province?: string; keyword?: string }>({
  year: 2025, examType: ''
})
const list = ref<any[]>([])
const loading = ref(false)
const drawerOpen = ref(false)
const detail = ref<any | null>(null)

async function load() {
  loading.value = true
  try {
    list.value = await govApi.posts(filter)
  } catch (e) {
    list.value = []
    ElMessage.error('读取岗位数据失败：' + formatRequestError(e, '请稍后重试'))
  } finally { loading.value = false }
}

async function viewDetail(p: any) {
  try {
    detail.value = await govApi.detail(p.id)
    drawerOpen.value = true
  } catch (e) {
    ElMessage.error('读取岗位详情失败：' + formatRequestError(e, '请稍后重试'))
  }
}

onMounted(load)
</script>

<template>
  <div class="page space-y-4">
    <el-card>
      <div class="flex flex-wrap gap-3">
        <el-input v-model="filter.keyword" placeholder="搜索单位/岗位" clearable class="!w-60" @change="load" />
        <el-select v-model="filter.examType" placeholder="考试类型" clearable class="!w-32" @change="load">
          <el-option label="国考" value="NATIONAL" />
          <el-option label="省考" value="PROVINCIAL" />
        </el-select>
        <el-input-number v-model="filter.year" :min="2020" :max="2030" class="!w-32" @change="load" />
      </div>
    </el-card>

    <el-card v-loading="loading">
      <el-table :data="list" stripe @row-click="viewDetail">
        <el-table-column prop="dept_name" label="单位" min-width="200" show-overflow-tooltip />
        <el-table-column prop="post_name" label="岗位" min-width="180" show-overflow-tooltip />
        <el-table-column prop="region" label="地点" width="100" />
        <el-table-column prop="degree_required" label="学历" width="100" />
        <el-table-column prop="enroll_count" label="招" width="80" />
        <el-table-column prop="apply_pass" label="过审" width="80" />
        <el-table-column label="上岸率" width="100">
          <template #default="{ row }">
            <el-tag :type="row.admit_rate_pct < 1 ? 'danger' : row.admit_rate_pct < 3 ? 'warning' : 'success'">
              {{ row.admit_rate_pct }}%
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="drawerOpen" :title="detail?.post_name" size="50%">
      <div v-if="detail" class="space-y-4">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="招录单位">{{ detail.dept_name }}</el-descriptions-item>
          <el-descriptions-item label="岗位">{{ detail.post_name }}</el-descriptions-item>
          <el-descriptions-item label="地点">{{ detail.region }}</el-descriptions-item>
          <el-descriptions-item label="学历要求">{{ detail.degree_required }}</el-descriptions-item>
          <el-descriptions-item label="专业要求">{{ detail.major_required }}</el-descriptions-item>
          <el-descriptions-item label="政治面貌">{{ detail.political }}</el-descriptions-item>
          <el-descriptions-item label="其他要求">{{ detail.extra_required }}</el-descriptions-item>
          <el-descriptions-item label="招录人数">{{ detail.enroll_count }}</el-descriptions-item>
          <el-descriptions-item label="过审人数">{{ detail.apply_pass }}</el-descriptions-item>
          <el-descriptions-item label="上岸率">{{ detail.admit_rate_pct }}%</el-descriptions-item>
          <el-descriptions-item label="报录比">{{ detail.apply_per_seat }}:1</el-descriptions-item>
          <el-descriptions-item label="进面线">[{{ detail.interview_min }}, {{ detail.interview_max }}]</el-descriptions-item>
          <el-descriptions-item label="录用最低综合">{{ detail.final_min }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>
  </div>
</template>
