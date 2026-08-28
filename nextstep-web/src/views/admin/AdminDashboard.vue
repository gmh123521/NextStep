<script setup lang="ts">
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="flex-between">
          <span class="font-semibold">后台管理</span>
          <el-tag type="danger">管理员</el-tag>
        </div>
      </template>
      <el-alert
        title="管理员身份已验证"
        type="success"
        :closable="false"
        show-icon
      >
        当前登录账号：{{ userStore.me?.username }}
      </el-alert>
      <p class="text-sm text-gray-500 mt-4">
        用户管理、数据维护和采集任务页面将在后台功能模块中提供。
      </p>
    </el-card>
  </div>
</template>
