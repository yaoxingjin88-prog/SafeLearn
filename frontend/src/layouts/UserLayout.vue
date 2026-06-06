<template>
  <el-container class="user-layout">
    <el-aside :width="collapsed ? '72px' : '240px'" class="user-aside">
      <div class="brand" @click="$router.push('/user/dashboard')">
        <img src="@/assets/logo.svg" class="brand-logo" alt="Logo" />
        <span v-if="!collapsed" class="brand-name">储安云</span>
      </div>

      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          router
          class="user-menu"
        >
          <el-menu-item index="/user/dashboard">
            <el-icon><Monitor /></el-icon>
            <template #title>工作台</template>
          </el-menu-item>

          <template v-for="menu in userMenus" :key="menu.path">
            <el-sub-menu v-if="menu.children?.length" :index="menu.path">
              <template #title>
                <el-icon><component :is="menu.icon" /></el-icon>
                <span>{{ menu.title }}</span>
              </template>
              <el-menu-item
                v-for="child in menu.children"
                :key="child.path"
                :index="child.path"
              >
                {{ child.title }}
              </el-menu-item>
            </el-sub-menu>
          </template>

          <el-sub-menu v-if="isAdmin" index="/admin">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/admin/users">用户管理</el-menu-item>
            <el-menu-item index="/admin/courses">课程管理</el-menu-item>
            <el-menu-item index="/admin/data">数据大屏</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="main-wrap">
      <el-header class="user-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <Fold v-if="!collapsed" />
            <Expand v-else />
          </el-icon>
          <el-input
            v-model="searchText"
            class="search-input"
            placeholder="搜索课程、案例或安全规范..."
            :prefix-icon="Search"
            clearable
            @keyup.enter="handleSearch"
          />
        </div>

        <div class="header-right">
          <div v-if="streakDays > 0" class="streak-badge">
            <el-icon><Sunny /></el-icon>
            连续学习 {{ streakDays }} 天
          </div>
          <el-badge :value="3" :max="9" class="notify-badge">
            <el-button circle :icon="Bell" />
          </el-badge>
          <el-dropdown>
            <div class="user-info">
              <el-avatar :size="36">{{ displayInitial }}</el-avatar>
              <span class="user-name">{{ displayName }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>{{ userStore.userInfo?.company || '储能企业' }}</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="user-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Monitor, Reading, Warning, Cpu, Document, ChatDotRound, Setting,
  Fold, Expand, Search, Bell, Sunny,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores'
import request from '@/api/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)
const searchText = ref('')
const streakDays = ref(0)
const displayName = ref('')

const activeMenu = computed(() => route.path)
const isAdmin = computed(() => userStore.role === 'admin')
const displayInitial = computed(() => (displayName.value || '用').charAt(0))

const userMenus = [
  {
    path: '/user/courses',
    title: '安全培训',
    icon: Reading,
    children: [
      { path: '/user/courses/list', title: '课程列表' },
      { path: '/user/courses/skill-tree', title: '安全进阶路径' },
      { path: '/user/courses/my-learning', title: '我的学习' },
    ],
  },
  {
    path: '/user/simulation',
    title: '事故推演',
    icon: Warning,
    children: [
      { path: '/user/simulation/scenarios', title: '推演场景' },
      { path: '/user/simulation/records', title: '推演记录' },
    ],
  },
  {
    path: '/user/training',
    title: '应急训练',
    icon: Cpu,
    children: [
      { path: '/user/training/scenarios', title: '训练场景' },
      { path: '/user/training/records', title: '训练记录' },
    ],
  },
  {
    path: '/user/cases',
    title: '事故案例',
    icon: Document,
    children: [
      { path: '/user/cases/list', title: '案例列表' },
      { path: '/user/cases/review', title: '案例复盘' },
    ],
  },
  {
    path: '/user/ai',
    title: 'AI安全问答',
    icon: ChatDotRound,
    children: [
      { path: '/user/ai/chat', title: '智能问答' },
      { path: '/user/ai/history', title: '问答历史' },
    ],
  },
]

function handleSearch() {
  const q = searchText.value.trim()
  if (!q) return
  router.push({ path: '/user/courses/list', query: { keyword: q } })
}

function handleLogout() {
  userStore.logout()
  router.push('/user/login')
}

onMounted(async () => {
  try {
    const res = await request.get('/dashboard/overview')
    streakDays.value = res.data.streakDays || 0
    displayName.value = res.data.displayName || userStore.username
  } catch {
    displayName.value = userStore.username
  }
})
</script>

<style scoped>
.user-layout {
  height: 100vh;
  background: #f5f7fa;
}

.user-aside {
  background: #fff;
  border-right: 1px solid #eef0f4;
  transition: width 0.25s;
  display: flex;
  flex-direction: column;
}

.brand {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 10px;
  cursor: pointer;
  border-bottom: 1px solid #eef0f4;
}

.brand-logo {
  height: 32px;
}

.brand-name {
  font-size: 18px;
  font-weight: 700;
  color: #2b5aed;
}

.menu-scroll {
  flex: 1;
}

.user-menu {
  border-right: none;
  padding: 12px 8px;
}

.user-menu :deep(.el-menu-item),
.user-menu :deep(.el-sub-menu__title) {
  border-radius: 10px;
  margin-bottom: 4px;
  height: 44px;
}

.user-menu :deep(.el-menu-item.is-active) {
  background: #eef3ff !important;
  color: #2b5aed !important;
  font-weight: 600;
}

.main-wrap {
  min-width: 0;
}

.user-header {
  height: 64px;
  background: #fff;
  border-bottom: 1px solid #eef0f4;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #6b7280;
}

.search-input {
  max-width: 420px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 999px;
  background: #f5f7fa;
  box-shadow: none;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.streak-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  background: #fff7ed;
  color: #ea580c;
  font-size: 13px;
  font-weight: 500;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
}

.user-main {
  padding: 24px;
  background: #f5f7fa;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
