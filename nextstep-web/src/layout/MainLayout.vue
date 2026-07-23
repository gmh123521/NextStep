<script setup lang="ts">
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menuItems = [
  { path: '/dashboard', label: '首页',     icon: 'i-ep-house' },
  { path: '/profile',   label: '我的画像', icon: 'i-ep-user' },
  { path: '/plan',      label: '我的规划', icon: 'i-ep-calendar' },
  { path: '/school',    label: '考研院校', icon: 'i-ep-school' },
  { path: '/gov',       label: '考公岗位', icon: 'i-ep-office-building' },
  { path: '/job',       label: '就业行情', icon: 'i-ep-briefcase' }
]

const drawerOpen = ref(false)
const isMobile = ref(window.innerWidth < 768)
window.addEventListener('resize', () => { isMobile.value = window.innerWidth < 768 })

onMounted(() => {
  if (!userStore.me) userStore.refreshMe().catch(() => {})
})

function navigate(p: string) {
  router.push(p)
  drawerOpen.value = false
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="layout-root">
    <header class="layout-header">
      <div class="header-left">
        <el-button v-if="isMobile" link @click="drawerOpen = true">
          <i class="i-ep-menu text-xl" />
        </el-button>
        <span class="brand">NextStep</span>
      </div>
      <el-dropdown trigger="click">
        <span class="user-trigger">
          <el-avatar :size="28">{{ userStore.me?.username?.[0]?.toUpperCase() || '?' }}</el-avatar>
          <span class="username">{{ userStore.me?.username || '未登录' }}</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="router.push('/profile')">我的画像</el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </header>

    <div class="layout-body">
      <aside v-if="!isMobile" class="layout-aside">
        <div
          v-for="m in menuItems"
          :key="m.path"
          class="menu-item"
          :class="{ 'menu-item-active': route.path === m.path }"
          @click="navigate(m.path)"
        >
          <i :class="m.icon" class="menu-icon" />
          <span>{{ m.label }}</span>
        </div>
      </aside>

      <el-drawer v-model="drawerOpen" direction="ltr" size="220px" :with-header="false">
        <div class="drawer-menu">
          <div
            v-for="m in menuItems"
            :key="m.path"
            class="menu-item"
            :class="{ 'menu-item-active': route.path === m.path }"
            @click="navigate(m.path)"
          >
            <i :class="m.icon" class="menu-icon" />
            <span>{{ m.label }}</span>
          </div>
        </div>
      </el-drawer>

      <main class="layout-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout-root {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.layout-header {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 10;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.brand { font-size: 18px; font-weight: bold; color: #409EFF; }
.user-trigger {
  display: flex; align-items: center; gap: 8px; cursor: pointer;
}
.username { display: none; }
@media (min-width: 768px) {
  .username { display: inline; }
}
.layout-body {
  display: flex;
  flex: 1;
  min-height: 0;
}
.layout-aside {
  width: 220px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  padding: 16px 0;
}
.layout-main {
  flex: 1;
  overflow: auto;
  min-width: 0;
}
.drawer-menu { padding: 16px 0; }
.menu-item {
  padding: 12px 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: background 0.15s;
}
.menu-item:hover { background: #f9fafb; }
.menu-item-active {
  background: #eff6ff;
  color: #409EFF;
  font-weight: 500;
  border-right: 2px solid #409EFF;
}
.menu-icon { font-size: 18px; }
</style>
