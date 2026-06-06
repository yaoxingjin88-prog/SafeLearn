import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/auth/LoginPage.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/pages/auth/RegisterPage.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/pages/dashboard/DashboardPage.vue'),
        meta: { title: '工作台', icon: 'Monitor' },
      },
      {
        path: 'courses',
        name: 'Courses',
        redirect: '/courses/list',
        meta: { title: '安全培训', icon: 'Reading' },
        children: [
          {
            path: 'list',
            name: 'CourseList',
            component: () => import('@/pages/courses/CourseList.vue'),
            meta: { title: '课程列表' },
          },
          {
            path: ':id',
            name: 'CourseDetail',
            component: () => import('@/pages/courses/CourseDetail.vue'),
            meta: { title: '课程详情', hidden: true },
          },
          {
            path: ':courseId/chapters/:chapterId',
            name: 'ChapterView',
            component: () => import('@/pages/courses/ChapterView.vue'),
            meta: { title: '章节学习', hidden: true },
          },
        ],
      },
      {
        path: 'simulation',
        name: 'Simulation',
        redirect: '/simulation/scenarios',
        meta: { title: '事故推演', icon: 'Warning' },
        children: [
          {
            path: 'scenarios',
            name: 'ScenarioList',
            component: () => import('@/pages/simulation/ScenarioList.vue'),
            meta: { title: '推演场景' },
          },
          {
            path: ':id',
            name: 'SimulationView',
            component: () => import('@/pages/simulation/SimulationView.vue'),
            meta: { title: '推演演示', hidden: true },
          },
        ],
      },
      {
        path: 'training',
        name: 'Training',
        redirect: '/training/scenarios',
        meta: { title: '应急训练', icon: 'Cpu' },
        children: [
          {
            path: 'scenarios',
            name: 'TrainingScenarioList',
            component: () => import('@/pages/training/TrainingScenarioList.vue'),
            meta: { title: '训练场景' },
          },
          {
            path: ':id',
            name: 'TrainingView',
            component: () => import('@/pages/training/TrainingView.vue'),
            meta: { title: '训练进行中', hidden: true },
          },
          {
            path: 'records/:id',
            name: 'TrainingReport',
            component: () => import('@/pages/training/TrainingReport.vue'),
            meta: { title: '训练报告', hidden: true },
          },
        ],
      },
      {
        path: 'cases',
        name: 'Cases',
        redirect: '/cases/list',
        meta: { title: '事故案例', icon: 'Document' },
        children: [
          {
            path: 'list',
            name: 'CaseList',
            component: () => import('@/pages/cases/CaseList.vue'),
            meta: { title: '案例列表' },
          },
          {
            path: ':id',
            name: 'CaseDetail',
            component: () => import('@/pages/cases/CaseDetail.vue'),
            meta: { title: '案例详情', hidden: true },
          },
        ],
      },
      {
        path: 'ai',
        name: 'AI',
        component: () => import('@/pages/ai/AIChat.vue'),
        meta: { title: 'AI安全问答', icon: 'ChatDotRound' },
      },
      {
        path: 'admin',
        name: 'Admin',
        redirect: '/admin/users',
        meta: { title: '系统管理', icon: 'Setting', roles: ['admin'] },
        children: [
          {
            path: 'users',
            name: 'UserManage',
            component: () => import('@/pages/admin/UserManage.vue'),
            meta: { title: '用户管理' },
          },
          {
            path: 'data',
            name: 'DataScreen',
            component: () => import('@/pages/admin/DataScreen.vue'),
            meta: { title: '数据大屏' },
          },
        ],
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/pages/NotFound.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach(async (to, _from, next) => {
  const token = localStorage.getItem('token')
  const userStore = useUserStore()

  // 未登录且需要鉴权
  if (to.meta.requiresAuth !== false && !token) {
    return next('/login')
  }

  // 已登录访问登录页，跳转到首页
  if (to.path === '/login' && token) {
    return next('/dashboard')
  }

  // 如果有 token 但还没有用户信息，尝试拉取
  if (token && !userStore.userInfo) {
    try {
      await userStore.getUserInfo()
    } catch (e) {
      // 获取失败视为未登录
      localStorage.removeItem('token')
      return next('/login')
    }
  }

  // 角色校验：仅 admin 可访问 /admin 下路由
  if (to.fullPath.startsWith('/admin')) {
    if (userStore.role !== 'admin') {
      return next('/dashboard')
    }
  }

  return next()
})

export default router
