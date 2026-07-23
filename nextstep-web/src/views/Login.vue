<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const tab = ref<'login' | 'register'>('login')
const form = reactive({ username: '', password: '', nickname: '', email: '' })
const submitting = ref(false)

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号密码')
    return
  }
  submitting.value = true
  try {
    await userStore.login({ username: form.username, password: form.password })
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

async function handleRegister() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号密码')
    return
  }
  submitting.value = true
  try {
    await userStore.register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
      email: form.email || undefined
    })
    ElMessage.success('注册成功，已自动登录')
    await userStore.login({ username: form.username, password: form.password })
    router.push('/dashboard')
  } catch {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card shadow-lg">
      <div class="text-center mb-6">
        <h1 class="text-2xl font-bold text-brand mb-2">NextStep</h1>
        <p class="text-sm text-gray-500">考研 / 考公 / 就业，你的下一步</p>
      </div>

      <el-tabs v-model="tab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form @submit.prevent="handleLogin" label-position="top">
            <el-form-item label="账号">
              <el-input v-model="form.username" placeholder="用户名" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="form.password" type="password" show-password placeholder="密码" />
            </el-form-item>
            <el-button type="primary" class="w-full" :loading="submitting" @click="handleLogin">登录</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form @submit.prevent="handleRegister" label-position="top">
            <el-form-item label="账号">
              <el-input v-model="form.username" placeholder="4-32 字符" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
            </el-form-item>
            <el-form-item label="昵称（可选）">
              <el-input v-model="form.nickname" />
            </el-form-item>
            <el-form-item label="邮箱（可选）">
              <el-input v-model="form.email" />
            </el-form-item>
            <el-button type="primary" class="w-full" :loading="submitting" @click="handleRegister">注册并登录</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: linear-gradient(135deg, #eff6ff 0%, #e0e7ff 100%);
}
.login-card {
  width: 100%;
  max-width: 420px;
}
</style>
