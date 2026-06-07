<template>
  <el-container class="h-screen">
    <!-- 侧边栏 -->
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="transition-all duration-300">
      <div class="h-full bg-gray-900 flex flex-col">
        <!-- Logo -->
        <div class="h-16 flex items-center justify-center border-b border-gray-700">
          <img v-if="!appStore.sidebarCollapsed" src="@/assets/logo.svg" class="h-8" alt="Logo" />
          <span v-if="!appStore.sidebarCollapsed" class="ml-2 text-white font-bold text-lg">储安云</span>
          <img v-else src="@/assets/logo.svg" class="h-8" alt="Logo" />
        </div>

        <!-- 菜单 -->
        <el-scrollbar class="flex-1">
          <el-menu
            :default-active="activeMenu"
            :collapse="appStore.sidebarCollapsed"
            background-color="#1f2937"
            text-color="#9ca3af"
            active-text-color="#3b82f6"
            router
          >
            <template v-for="route in menuRoutes" :key="route.path">
              <!-- 单级菜单 -->
              <el-menu-item
                v-if="!route.children || route.children.length === 1"
                :index="route.children ? `/${route.path}/${route.children[0].path}` : `/${route.path}`"
              >
                <el-icon><component :is="route.meta?.icon" /></el-icon>
                <template #title>{{ route.meta?.title }}</template>
              </el-menu-item>

              <!-- 多级菜单 -->
              <el-sub-menu v-else :index="`/${route.path}`">
                <template #title>
                  <el-icon><component :is="route.meta?.icon" /></el-icon>
                  <span>{{ route.meta?.title }}</span>
                </template>
                <el-menu-item
                  v-for="child in route.children.filter(c => !c.meta?.hidden)"
                  :key="child.path"
                  :index="`/${route.path}/${child.path}`"
                >
                  {{ child.meta?.title }}
                </el-menu-item>
              </el-sub-menu>
            </template>
          </el-menu>
        </el-scrollbar>
      </div>
    </el-aside>

    <!-- 主内容区 -->
    <el-container class="flex-1 overflow-hidden">
      <!-- 顶部栏 -->
      <el-header class="flex items-center justify-between bg-white shadow-sm border-b">
        <div class="flex items-center">
          <el-icon
            class="cursor-pointer text-gray-500 hover:text-gray-700"
            @click="appStore.toggleSidebar"
          >
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/" class="ml-4">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.meta?.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="flex items-center gap-4">
          <el-dropdown>
            <div class="flex items-center cursor-pointer">
              <el-avatar :size="32" src="" />
              <span class="ml-2 text-sm">{{ userStore.username || '用户' }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="bg-gray-50">
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
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore, useUserStore } from '@/stores'
import { Fold, Expand } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const breadcrumbs = computed(() => {
  return route.matched.filter(item => item.meta?.title && item.path !== '/')
})

const menuRoutes = computed(() => {
  const mainRoute = router.options.routes.find(r => r.path === '/')
  return mainRoute?.children || []
})

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出当前账号吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
    userStore.logout()
    router.push('/login')
  } catch {
    // 用户取消
  }
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

:deep(.el-menu) {
  border-right: none;
}

:deep(.el-sub-menu .el-menu) {
  background-color: #111827 !important;
}

:deep(.el-menu-item.is-active) {
  background-color: rgba(59, 130, 246, 0.1) !important;
}

:deep(.el-main) {
  padding: 0 !important;
  height: calc(100vh - 60px) !important;
  overflow: hidden !important;
  display: flex !important;
  flex-direction: column !important;
}

:deep(.el-header) {
  height: 60px;
  line-height: 60px;
}

:deep(.el-aside) {
  overflow: hidden;
}
</style>
