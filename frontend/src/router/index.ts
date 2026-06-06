import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores'

/** 管理端业务路由（原系统，保持不变） */
const adminChildren: RouteRecordRaw[] = [
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
      { path: 'list', name: 'CourseList', component: () => import('@/pages/courses/CourseList.vue'), meta: { title: '课程列表' } },
      { path: 'skill-tree', name: 'SkillTree', component: () => import('@/pages/courses/SkillTree.vue'), meta: { title: '安全进阶路径' } },
      { path: ':id', name: 'CourseDetail', component: () => import('@/pages/courses/CourseDetail.vue'), meta: { title: '课程详情', hidden: true } },
      { path: ':courseId/chapters/:chapterId', name: 'ChapterView', component: () => import('@/pages/courses/ChapterView.vue'), meta: { title: '章节学习', hidden: true } },
    ],
  },
  {
    path: 'simulation',
    name: 'Simulation',
    redirect: '/simulation/scenarios',
    meta: { title: '事故推演', icon: 'Warning' },
    children: [
      { path: 'scenarios', name: 'ScenarioList', component: () => import('@/pages/simulation/ScenarioList.vue'), meta: { title: '推演场景' } },
      { path: ':id', name: 'SimulationView', component: () => import('@/pages/simulation/SimulationView.vue'), meta: { title: '推演演示', hidden: true } },
    ],
  },
  {
    path: 'training',
    name: 'Training',
    redirect: '/training/scenarios',
    meta: { title: '应急训练', icon: 'Cpu' },
    children: [
      { path: 'scenarios', name: 'TrainingScenarioList', component: () => import('@/pages/training/TrainingScenarioList.vue'), meta: { title: '训练场景' } },
      { path: 'records/:id', name: 'TrainingReport', component: () => import('@/pages/training/TrainingReport.vue'), meta: { title: '训练报告', hidden: true } },
      { path: ':id', name: 'TrainingView', component: () => import('@/pages/training/TrainingView.vue'), meta: { title: '训练进行中', hidden: true } },
    ],
  },
  {
    path: 'cases',
    name: 'Cases',
    redirect: '/cases/list',
    meta: { title: '事故案例', icon: 'Document' },
    children: [
      { path: 'list', name: 'CaseList', component: () => import('@/pages/cases/CaseList.vue'), meta: { title: '案例列表' } },
      { path: ':id', name: 'CaseDetail', component: () => import('@/pages/cases/CaseDetail.vue'), meta: { title: '案例详情', hidden: true } },
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
      { path: 'users', name: 'UserManage', component: () => import('@/pages/admin/UserManage.vue'), meta: { title: '用户管理' } },
      { path: 'courses', name: 'CourseManage', component: () => import('@/pages/admin/CourseManage.vue'), meta: { title: '课程管理' } },
      { path: 'data', name: 'DataScreen', component: () => import('@/pages/admin/DataScreen.vue'), meta: { title: '数据大屏' } },
    ],
  },
]

/** 用户端业务路由（新学员平台） */
const userChildren: RouteRecordRaw[] = [
  {
    path: 'dashboard',
    name: 'UserDashboard',
    component: () => import('@/pages/user/UserDashboard.vue'),
    meta: { title: '工作台' },
  },
  {
    path: 'courses',
    redirect: '/user/courses/list',
    meta: { title: '安全培训' },
    children: [
      { path: 'list', name: 'UserCourseList', component: () => import('@/pages/courses/CourseList.vue'), meta: { title: '课程列表' } },
      { path: 'skill-tree', name: 'UserSkillTree', component: () => import('@/pages/courses/SkillTree.vue'), meta: { title: '安全进阶路径' } },
      { path: 'my-learning', name: 'UserMyLearning', component: () => import('@/pages/courses/MyLearning.vue'), meta: { title: '我的学习' } },
      { path: ':id', name: 'UserCourseDetail', component: () => import('@/pages/courses/CourseDetail.vue'), meta: { title: '课程详情', hidden: true } },
      { path: ':courseId/chapters/:chapterId', name: 'UserChapterView', component: () => import('@/pages/courses/ChapterView.vue'), meta: { title: '章节学习', hidden: true } },
    ],
  },
  {
    path: 'simulation',
    redirect: '/user/simulation/scenarios',
    meta: { title: '事故推演' },
    children: [
      { path: 'scenarios', name: 'UserScenarioList', component: () => import('@/pages/simulation/ScenarioList.vue'), meta: { title: '推演场景' } },
      { path: 'records', name: 'UserSimulationRecords', component: () => import('@/pages/simulation/SimulationRecords.vue'), meta: { title: '推演记录' } },
      { path: ':id', name: 'UserSimulationView', component: () => import('@/pages/simulation/SimulationView.vue'), meta: { title: '推演演示', hidden: true } },
    ],
  },
  {
    path: 'training',
    redirect: '/user/training/scenarios',
    meta: { title: '应急训练' },
    children: [
      { path: 'scenarios', name: 'UserTrainingScenarioList', component: () => import('@/pages/training/TrainingScenarioList.vue'), meta: { title: '训练场景' } },
      { path: 'records', name: 'UserTrainingRecords', component: () => import('@/pages/training/TrainingRecords.vue'), meta: { title: '训练记录' } },
      { path: 'records/:id', name: 'UserTrainingReport', component: () => import('@/pages/training/TrainingReport.vue'), meta: { title: '训练报告', hidden: true } },
      { path: ':id', name: 'UserTrainingView', component: () => import('@/pages/training/TrainingView.vue'), meta: { title: '训练进行中', hidden: true } },
    ],
  },
  {
    path: 'cases',
    redirect: '/user/cases/list',
    meta: { title: '事故案例' },
    children: [
      { path: 'list', name: 'UserCaseList', component: () => import('@/pages/cases/CaseList.vue'), meta: { title: '案例列表' } },
      { path: 'review', name: 'UserCaseReview', component: () => import('@/pages/cases/CaseReview.vue'), meta: { title: '案例复盘' } },
      { path: ':id', name: 'UserCaseDetail', component: () => import('@/pages/cases/CaseDetail.vue'), meta: { title: '案例详情', hidden: true } },
    ],
  },
  {
    path: 'ai',
    redirect: '/user/ai/chat',
    meta: { title: 'AI安全问答' },
    children: [
      { path: 'chat', name: 'UserAIChat', component: () => import('@/pages/ai/AIChat.vue'), meta: { title: '智能问答' } },
      { path: 'history', name: 'UserAIHistory', component: () => import('@/pages/ai/AIHistory.vue'), meta: { title: '问答历史' } },
    ],
  },
]

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/auth/LoginPage.vue'),
    meta: { requiresAuth: false, portal: 'admin' },
  },
  {
    path: '/user/login',
    name: 'UserLogin',
    component: () => import('@/pages/auth/UserLoginPage.vue'),
    meta: { requiresAuth: false, portal: 'user' },
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
    meta: { portal: 'admin' },
    children: adminChildren,
  },
  {
    path: '/user',
    component: () => import('@/layouts/UserLayout.vue'),
    redirect: '/user/dashboard',
    meta: { portal: 'user' },
    children: userChildren,
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

router.beforeEach(async (to, _from, next) => {
  const token = localStorage.getItem('token')
  const userStore = useUserStore()
  const isUserPortal = to.path.startsWith('/user')
  const loginPath = isUserPortal ? '/user/login' : '/login'
  const homePath = isUserPortal ? '/user/dashboard' : '/dashboard'

  if (to.meta.requiresAuth !== false && !token) {
    return next(loginPath)
  }

  if ((to.path === '/login' || to.path === '/user/login') && token) {
    return next(to.path === '/user/login' ? '/user/dashboard' : '/dashboard')
  }

  if (token && !userStore.userInfo) {
    try {
      await userStore.getUserInfo()
    } catch {
      localStorage.removeItem('token')
      return next(loginPath)
    }
  }

  if (to.fullPath.startsWith('/admin') && userStore.role !== 'admin') {
    return next('/dashboard')
  }

  return next()
})

export default router
