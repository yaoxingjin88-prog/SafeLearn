<template>
  <el-container class="admin-layout">
    <el-aside :width="collapsed ? '72px' : '240px'" class="admin-aside" :class="{ 'is-collapsed': collapsed }">
      <div class="brand" @click="$router.push('/dashboard')">
        <img src="@/assets/logo.png" class="brand-logo" alt="Logo" />
        <span v-if="!collapsed" class="brand-name">储能安全培训管理平台</span>
      </div>

      <el-scrollbar class="menu-scroll">
        <el-menu ref="menuRef" :default-active="activeMenu" :default-openeds="defaultOpeneds" :collapse="collapsed"
          :collapse-transition="false" unique-opened router class="admin-menu">
          <el-menu-item index="/dashboard" class="nav-top-item" v-if="canAccessModule('dashboard')">
            <el-icon class="nav-top-icon">
              <Monitor />
            </el-icon>
            <template #title>首页</template>
          </el-menu-item>

          <template v-for="menu in visibleMenus" :key="menu.path">
            <el-sub-menu v-if="menu.children?.length" :index="menu.path" class="nav-group">
              <template #title>
                <el-icon class="nav-top-icon">
                  <component :is="menu.icon" />
                </el-icon>
                <span class="nav-top-label">{{ menu.title }}</span>
              </template>
              <el-menu-item v-for="child in menu.children" :key="child.path" :index="child.path" class="nav-sub-item">
                <el-icon v-if="child.icon" class="nav-sub-icon">
                  <component :is="child.icon" />
                </el-icon>
                <template #title>{{ child.title }}</template>
              </el-menu-item>
            </el-sub-menu>

            <el-menu-item v-else :index="menu.path" class="nav-top-item">
              <el-icon class="nav-top-icon">
                <component :is="menu.icon" />
              </el-icon>
              <template #title>{{ menu.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>

      <div class="sidebar-user">
        <el-dropdown trigger="click" placement="top-start">
          <div class="sidebar-user-inner" :class="{ collapsed }">
            <el-avatar :size="40" class="sidebar-avatar">{{ displayInitial }}</el-avatar>
            <div v-if="!collapsed" class="sidebar-user-text">
              <div class="sidebar-user-name">{{ displayName }}</div>
              <div class="sidebar-user-dept">{{ displayDepartment }}</div>
            </div>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>{{ userStore.userInfo?.company || '储能企业' }}</el-dropdown-item>
              <el-dropdown-item @click="$router.push('/admin/account')">账号设置</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-aside>

    <el-container class="main-wrap">
      <el-header class="admin-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <Fold v-if="!collapsed" />
            <Expand v-else />
          </el-icon>
          <div v-if="route.path === '/dashboard'" class="header-crumbs">
            <span>首页</span><i>/</i><strong>概览</strong>
          </div>
        </div>
        <div class="header-right">
          <AdminNotificationPopover v-if="showInboxTools" />
          <AdminMessagePopover v-if="showInboxTools" />
          <AdminHelpDrawer v-if="isAdmin" />
          <el-dropdown trigger="click">
            <div class="header-user">
              <el-avatar :size="32" class="header-avatar">{{ displayInitial }}</el-avatar>
              <div class="header-user-text">
                <span class="header-username">{{ displayName }}</span>
                <span v-if="isAdmin" class="header-role-tag">管理员</span>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/admin/account')">账号设置</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <div v-if="breadcrumbs.length && route.path !== '/dashboard' && !route.meta.hideLayoutBreadcrumb" class="breadcrumb-bar">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="`${item.path ?? item.title}-${index}`"
            :to="item.path ? { path: item.path } : undefined">
            {{ item.title }}
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <el-main class="admin-main" :class="{ 'admin-main--dashboard': route.path === '/dashboard' }">
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
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Monitor, Setting, Fold, Expand, User, EditPen, Reading, UserFilled, DataLine, Document, Collection, Files,
  OfficeBuilding, DataAnalysis, BellFilled, Key,
} from '@element-plus/icons-vue'
import type { Component } from 'vue'
import { ElMessageBox, type MenuInstance } from 'element-plus'
import { useUserStore } from '@/stores'
import { useBreadcrumbs } from '@/composables/useBreadcrumbs'
import AdminNotificationPopover from '@/components/admin/AdminNotificationPopover.vue'
import AdminMessagePopover from '@/components/admin/AdminMessagePopover.vue'
import AdminHelpDrawer from '@/components/admin/AdminHelpDrawer.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)
const displayName = ref('')
const menuRef = ref<MenuInstance>()

interface NavChild {
  path: string
  title: string
  icon: Component
  permissionModule?: string
}

interface NavMenu {
  path: string
  title: string
  icon: Component
  children?: NavChild[]
  adminOnly?: boolean
  permissionModule?: string
}

/**
 * 管理端菜单：首页 → 培训课程 / 考试题库 → 组织与账号 → 系统设置
 */
const adminMenus: NavMenu[] = [
  {
    path: '/admin/training',
    title: '培训课程',
    icon: Reading,
    adminOnly: true,
    children: [
      { path: '/admin/learning/courses', title: '课程管理', icon: EditPen, permissionModule: 'training' },
      { path: '/admin/learning/monitoring', title: '学习监控', icon: DataLine, permissionModule: 'training' },
      { path: '/dashboard/reports', title: '学习报表', icon: DataAnalysis, permissionModule: 'report' },
      { path: '/dashboard/alerts', title: '预警管理', icon: BellFilled, permissionModule: 'hazard' },
    ],
  },
  {
    path: '/admin/exams',
    title: '考试题库',
    icon: Document,
    adminOnly: true,
    children: [
      { path: '/admin/learning/exams', title: '考试管理', icon: EditPen, permissionModule: 'exam' },
      { path: '/admin/learning/question-bank', title: '题库管理', icon: Collection, permissionModule: 'exam' },
      { path: '/admin/learning/paper-assembly', title: '组卷管理', icon: Files, permissionModule: 'exam' },
    ],
  },
  {
    path: '/admin/org',
    title: '组织与账号',
    icon: UserFilled,
    adminOnly: true,
    children: [
      { path: '/admin/org', title: '组织与部门', icon: OfficeBuilding, permissionModule: 'organization' },
      { path: '/admin/users', title: '用户管理', icon: User, permissionModule: 'organization' },
      { path: '/admin/roles', title: '角色与权限', icon: Key, permissionModule: 'permission' },
    ],
  },
  {
    path: '/admin/settings',
    title: '系统设置',
    icon: Setting,
    adminOnly: true,
    permissionModule: 'permission',
  },
]

const isAdmin = computed(() => userStore.role === 'admin')
const displayInitial = computed(() => (displayName.value || '管').charAt(0))
const displayDepartment = computed(() => userStore.userInfo?.department || '安全管理部')

function canAccessModule(module?: string) {
  if (!module) return true
  return userStore.hasPermission(module, 'view')
}

const visibleMenus = computed(() =>
  adminMenus
    .filter(m => !m.adminOnly || isAdmin.value)
    .map(menu => {
      if (!menu.children?.length) {
        return canAccessModule(menu.permissionModule) ? menu : null
      }
      const children = menu.children.filter(child => canAccessModule(child.permissionModule))
      if (!children.length) return null
      return { ...menu, children }
    })
    .filter((menu): menu is NavMenu => menu !== null),
)

const showInboxTools = computed(() => isAdmin.value && canAccessModule('dashboard'))

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/dashboard/reports')) {
    return '/dashboard/reports'
  }
  if (path.startsWith('/dashboard/alerts')) {
    return '/dashboard/alerts'
  }
  if (path.startsWith('/admin/learning/exams')) {
    return '/admin/learning/exams'
  }
  if (path.startsWith('/admin/learning/paper-assembly')) {
    return '/admin/learning/paper-assembly'
  }
  if (path.startsWith('/admin/learning/question-bank')) {
    if (path.includes('/edit') || path.endsWith('/new')) {
      return path
    }
    return '/admin/learning/question-bank'
  }
  if (path.startsWith('/admin/learning/monitoring')) {
    return '/admin/learning/monitoring'
  }
  if (path.startsWith('/admin/learning')) {
    return '/admin/learning/courses'
  }
  if (path.startsWith('/admin/settings')) {
    return '/admin/settings'
  }
  if (path.startsWith('/admin/users')) {
    return '/admin/users'
  }
  if (path.startsWith('/admin/roles')) {
    return '/admin/roles'
  }
  if (path.startsWith('/admin/org')) {
    return '/admin/org'
  }
  return path
})

const breadcrumbMenus = computed(() =>
  visibleMenus.value.map(m => ({
    path: m.path,
    title: m.title,
    children: m.children?.map(c => ({ path: c.path, title: c.title })),
  })),
)

const { breadcrumbs } = useBreadcrumbs(breadcrumbMenus, [
  { path: '/dashboard', title: '首页' },
])

function resolveOpenIndex(): string | null {
  const active = activeMenu.value
  for (const menu of visibleMenus.value) {
    if (menu.children?.some(c => c.path === active)) {
      return menu.path
    }
  }
  return null
}

const defaultOpeneds = computed(() => {
  const index = resolveOpenIndex()
  return index ? [index] : []
})

watch(
  () => [route.path, collapsed.value] as const,
  async () => {
    if (collapsed.value) return
    await nextTick()
    const index = resolveOpenIndex()
    if (index) menuRef.value?.open(index)
  },
  { immediate: true },
)

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

onMounted(async () => {
  if (!userStore.userInfo) {
    try { await userStore.getUserInfo() } catch { /* ignore */ }
  }
  displayName.value = userStore.userInfo?.username || userStore.username || '管理员'
})
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  background: #f5f7fa;
}

.admin-aside {
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
  justify-content: flex-start;
  padding: 0 20px;
  gap: 10px;
  cursor: pointer;
  border-bottom: 1px solid #eef0f4;
}

.admin-aside.is-collapsed .brand {
  justify-content: center;
  padding: 0;
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
  min-height: 0;
}

.sidebar-user {
  flex-shrink: 0;
  border-top: 1px solid #eef0f4;
  padding: 16px;
}

.admin-aside.is-collapsed .sidebar-user {
  padding: 16px 0;
  display: flex;
  justify-content: center;
}

.sidebar-user-inner {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  border-radius: 10px;
  padding: 4px;
  transition: background 0.2s;
}

.sidebar-user-inner:hover {
  background: #f5f7fa;
}

.sidebar-user-inner.collapsed {
  justify-content: center;
  padding: 4px 0;
}

.sidebar-avatar {
  flex-shrink: 0;
  background: #2b5aed;
  color: #fff;
  font-weight: 600;
}

.sidebar-user-text {
  min-width: 0;
  flex: 1;
}

.sidebar-user-name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-user-dept {
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.admin-menu {
  --nav-active-bg: #e8efff;
  --nav-active-color: #1d4ed8;
  --nav-indicator: #2b5aed;
  --nav-sub-indent: 12px;
  --nav-expand-duration: 0.32s;
  --nav-expand-ease: cubic-bezier(0.4, 0, 0.2, 1);
  --el-menu-base-level-padding: 8px;
  --el-menu-level-padding: 0px;
  border-right: none;
  padding: 12px 8px;
}

.admin-menu :deep(.el-collapse-transition-enter-active),
.admin-menu :deep(.el-collapse-transition-leave-active) {
  transition:
    max-height var(--nav-expand-duration) var(--nav-expand-ease),
    padding-top var(--nav-expand-duration) var(--nav-expand-ease),
    padding-bottom var(--nav-expand-duration) var(--nav-expand-ease) !important;
}

.admin-menu :deep(.el-menu--inline .el-menu-item) {
  transition: background 0.18s ease, color 0.18s ease;
}

.admin-menu :deep(.nav-top-item),
.admin-menu :deep(.el-sub-menu__title) {
  border-radius: 10px;
  margin-bottom: 4px;
  height: 48px;
  line-height: 48px;
  font-size: 14px;
  color: #1f2937;
  font-weight: 600;
  transition: background 0.28s var(--nav-expand-ease), color 0.22s ease;
}

.admin-menu :deep(.nav-top-icon) {
  font-size: 18px;
  color: #4b5563;
}

.admin-menu :deep(.nav-top-label) {
  font-weight: 600;
  letter-spacing: 0.01em;
}

.admin-menu :deep(.el-sub-menu__title:hover),
.admin-menu :deep(.nav-top-item:hover) {
  background: #f3f4f6 !important;
  color: #111827 !important;
}

.admin-menu :deep(.el-sub-menu__icon-arrow) {
  right: 10px;
  margin-top: -6px;
  font-size: 11px;
  color: #9ca3af;
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1), color 0.2s;
}

.admin-menu :deep(.el-sub-menu.is-opened > .el-sub-menu__title .el-sub-menu__icon-arrow) {
  transform: rotate(180deg);
  color: var(--nav-active-color);
}

.admin-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: var(--nav-active-color);
}

.admin-menu :deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
  background: #f1f5f9 !important;
  color: #1e40af;
  transition: background 0.28s var(--nav-expand-ease), color 0.22s ease;
}

.admin-menu :deep(.el-sub-menu.is-opened > .el-sub-menu__title .nav-top-icon) {
  color: var(--nav-active-color);
}

.admin-menu :deep(.el-sub-menu) {
  margin-bottom: 6px;
}

.admin-menu :deep(.nav-group > .el-menu) {
  margin: 2px 4px 8px var(--nav-sub-indent);
  padding: 6px 8px 8px 10px;
  border-radius: 8px;
  border-left: 2px solid transparent;
  background: transparent !important;
  box-shadow: none;
  overflow: hidden;
  transition:
    background-color 0.28s var(--nav-expand-ease),
    border-color 0.28s var(--nav-expand-ease),
    box-shadow 0.28s var(--nav-expand-ease);
}

.admin-menu :deep(.nav-group.is-opened > .el-menu) {
  border-left-color: #dbeafe;
  background: #f8fafc !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.admin-menu :deep(.nav-group.is-opened > .el-menu .nav-sub-item) {
  animation: nav-sub-reveal 0.36s var(--nav-expand-ease) both;
}

.admin-menu :deep(.nav-group.is-opened > .el-menu .nav-sub-item:nth-child(1)) {
  animation-delay: 0.04s;
}

.admin-menu :deep(.nav-group.is-opened > .el-menu .nav-sub-item:nth-child(2)) {
  animation-delay: 0.07s;
}

.admin-menu :deep(.nav-group.is-opened > .el-menu .nav-sub-item:nth-child(3)) {
  animation-delay: 0.10s;
}

@keyframes nav-sub-reveal {
  from {
    opacity: 0;
    transform: translateY(-6px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.admin-menu :deep(.el-sub-menu .el-menu) {
  background: transparent !important;
}

.admin-menu :deep(.nav-sub-item) {
  height: 38px !important;
  line-height: 38px !important;
  margin-bottom: 2px;
  padding-left: 8px !important;
  padding-right: 8px !important;
  font-size: 13px;
  font-weight: 400;
  color: #64748b;
  border-radius: 6px;
  transition: background 0.15s ease, color 0.15s ease;
}

.admin-menu :deep(.nav-sub-item:hover) {
  background: #eef2ff !important;
  color: #334155 !important;
}

.admin-menu :deep(.nav-sub-icon) {
  font-size: 14px;
  color: #94a3b8;
  margin-right: 8px;
  vertical-align: middle;
  transition: color 0.18s;
}

.admin-menu :deep(.nav-sub-item:hover .nav-sub-icon) {
  color: #64748b;
}

.admin-menu :deep(.nav-top-item.is-active) {
  position: relative;
  background: var(--nav-active-bg) !important;
  color: var(--nav-active-color) !important;
}

.admin-menu :deep(.nav-sub-item.is-active) {
  position: relative;
  background: #fff !important;
  color: var(--nav-active-color) !important;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(43, 90, 237, 0.08);
}

.admin-menu :deep(.nav-top-item.is-active)::before,
.admin-menu :deep(.nav-sub-item.is-active)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  background: var(--nav-indicator);
  border-radius: 0 3px 3px 0;
}

.admin-menu :deep(.nav-top-item.is-active)::before {
  height: 22px;
}

.admin-menu :deep(.nav-sub-item.is-active)::before {
  left: -2px;
  height: 16px;
}

.admin-menu :deep(.nav-sub-item.is-active .nav-sub-icon) {
  color: var(--nav-active-color) !important;
}

.admin-menu :deep(.nav-top-item.is-active .nav-top-icon) {
  color: var(--nav-active-color) !important;
}

.admin-menu.el-menu--collapse {
  width: 100%;
  padding: 12px 0;
}

.admin-menu.el-menu--collapse :deep(.el-menu-item),
.admin-menu.el-menu--collapse :deep(.el-sub-menu__title) {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 44px;
  height: 44px !important;
  margin: 0 auto 4px;
  padding: 0 !important;
}

.admin-menu.el-menu--collapse :deep(.el-menu-item.is-active)::before {
  left: 0;
  height: 28px;
}

.admin-menu.el-menu--collapse :deep(.el-menu-tooltip__trigger) {
  display: flex !important;
  justify-content: center;
  align-items: center;
  width: 44px;
  height: 44px;
  padding: 0 !important;
  left: auto !important;
  top: auto !important;
  position: relative !important;
}

.admin-menu.el-menu--collapse :deep(.el-menu-item .el-icon),
.admin-menu.el-menu--collapse :deep(.el-sub-menu__title .el-icon) {
  margin: 0 !important;
}

.admin-menu.el-menu--collapse :deep(.el-sub-menu__icon-arrow) {
  display: none;
}

.main-wrap {
  min-width: 0;
}

.admin-header {
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
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #6b7280;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.header-user-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  line-height: 1.2;
}

.header-role-tag {
  font-size: 10px;
  color: #2b5aed;
  background: #edf3ff;
  border-radius: 4px;
  padding: 1px 6px;
}

.header-avatar {
  background: #2b5aed;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.header-username {
  font-size: 14px;
  color: #374151;
}

.breadcrumb-bar {
  background: #fff;
  padding: 10px 24px;
  border-bottom: 1px solid #eef0f4;
}

.breadcrumb-bar :deep(.el-breadcrumb__inner) {
  font-size: 13px;
  color: #6b7280;
  font-weight: 400;
}

.breadcrumb-bar :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: #111827;
  font-weight: 500;
}

.breadcrumb-bar :deep(.el-breadcrumb__inner.is-link:hover) {
  color: #2b5aed;
}

.admin-main {
  padding: 0;
  background: #f5f7fc;
  height: calc(100vh - 64px - 41px);
  overflow: auto;
}

.admin-main--dashboard {
  height: calc(100vh - 64px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 深蓝运营驾驶舱主题 */
.admin-layout {
  background: #f2f5fa;
}

.admin-aside {
  border-right: 0;
  background: linear-gradient(180deg, #071936 0%, #092653 100%);
  box-shadow: 4px 0 18px rgba(7, 24, 54, 0.12);
}

.brand {
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

.brand-name {
  color: #fff;
  font-size: 15px;
  white-space: nowrap;
}

.brand-logo {
  border-radius: 7px;
  background: #fff;
  padding: 2px;
}

.admin-menu {
  --nav-active-bg: linear-gradient(90deg, #0877f9, #1468df);
  --nav-active-color: #fff;
  --nav-indicator: #47b4ff;
  background: transparent;
}

.admin-menu :deep(.nav-top-item),
.admin-menu :deep(.el-sub-menu__title) {
  color: #bdcbe0;
}

.admin-menu :deep(.nav-top-icon),
.admin-menu :deep(.nav-sub-icon) {
  color: #91a8c7;
}

.admin-menu :deep(.el-sub-menu__title:hover),
.admin-menu :deep(.nav-top-item:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
  color: #fff !important;
}

.admin-menu :deep(.el-sub-menu.is-opened > .el-sub-menu__title),
.admin-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  background: rgba(255, 255, 255, 0.06) !important;
  color: #fff;
}

.admin-menu :deep(.nav-group.is-opened > .el-menu) {
  border-left-color: rgba(74, 163, 255, 0.45);
  background: rgba(0, 0, 0, 0.1) !important;
}

.admin-menu :deep(.nav-sub-item) {
  color: #aebed4;
}

.admin-menu :deep(.nav-sub-item:hover) {
  background: rgba(255, 255, 255, 0.07) !important;
  color: #fff !important;
}

.admin-menu :deep(.nav-top-item.is-active),
.admin-menu :deep(.nav-sub-item.is-active) {
  background: linear-gradient(90deg, #0877f9, #1468df) !important;
  color: #fff !important;
  box-shadow: 0 8px 18px rgba(3, 111, 246, 0.28);
}

.sidebar-user {
  border-top-color: rgba(255, 255, 255, 0.08);
}

.sidebar-user-inner:hover {
  background: rgba(255, 255, 255, 0.07);
}

.sidebar-user-name {
  color: #fff;
}

.sidebar-user-dept {
  color: #8fa5c1;
}

.sidebar-avatar,
.header-avatar {
  background: #1382f4;
}

.admin-header {
  border-bottom: 0;
  background: linear-gradient(90deg, #071936, #0a2148);
  box-shadow: 0 2px 12px rgba(7, 24, 54, 0.14);
}

.collapse-btn,
.header-username {
  color: #eaf2ff;
}

.admin-main {
  background: #f2f5fa;
}

/* 参考管理驾驶舱：白色工具栏 + 深蓝导航 */
.admin-header {
  border-bottom: 1px solid #e9edf3;
  background: #fff;
  box-shadow: 0 2px 9px rgba(31, 49, 78, 0.035);
}

.collapse-btn {
  color: #263246;
}

.header-crumbs {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #8a94a2;
  font-size: 13px;
}

.header-crumbs i {
  color: #c1c7d0;
  font-style: normal;
}

.header-crumbs strong {
  color: #3d4858;
  font-weight: 600;
}

:deep(.header-icon-btn) {
  position: relative;
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #526174;
  font-size: 18px;
  cursor: pointer;
}

:deep(.header-icon-btn:hover) {
  background: #f2f5fa;
  color: #2f76e8;
}

:deep(.header-icon-btn em) {
  position: absolute;
  top: -2px;
  right: -2px;
  min-width: 15px;
  height: 15px;
  padding: 0 3px;
  box-sizing: border-box;
  border: 2px solid #fff;
  border-radius: 999px;
  background: #ef3340;
  color: #fff;
  font-size: 8px;
  font-style: normal;
  line-height: 11px;
  text-align: center;
}

.header-username {
  color: #3c4859;
}
</style>
