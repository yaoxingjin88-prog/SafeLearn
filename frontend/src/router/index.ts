import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores'
import { resolveRoutePermission } from '@/utils/permissions'

/** 管理端路由：工作台 + 培训课程 + 考试题库 + 组织与账号 + 系统设置 */
const adminChildren: RouteRecordRaw[] = [
  {
    path: 'dashboard',
    name: 'Dashboard',
    component: () => import('@/pages/dashboard/DashboardPage.vue'),
    meta: { title: '首页', icon: 'Monitor' },
  },
  {
    path: 'dashboard/alerts',
    name: 'AlertCenter',
    component: () => import('@/pages/dashboard/AlertCenterPage.vue'),
    meta: { title: '预警管理', hideLayoutBreadcrumb: true },
  },
  {
    path: 'dashboard/notifications',
    name: 'NotificationCenter',
    component: () => import('@/pages/dashboard/NotificationCenterPage.vue'),
    meta: { title: '通知中心', hidden: true },
  },
  {
    path: 'dashboard/messages',
    name: 'MessageCenter',
    component: () => import('@/pages/dashboard/MessageCenterPage.vue'),
    meta: { title: '消息中心', hidden: true },
  },
  {
    path: 'dashboard/reports',
    name: 'LearningReports',
    component: () => import('@/pages/dashboard/LearningReportPage.vue'),
    meta: { title: '学习报表', hideLayoutBreadcrumb: true },
  },
  // 旧版业务路径重定向至学员端
  { path: 'courses/:pathMatch(.*)*', redirect: to => `/user/courses/${to.params.pathMatch}` },
  { path: 'courses', redirect: '/user/courses/list' },
  { path: 'simulation/:pathMatch(.*)*', redirect: to => `/user/simulation/${to.params.pathMatch}` },
  { path: 'simulation', redirect: '/user/simulation/scenarios' },
  { path: 'training/:pathMatch(.*)*', redirect: to => `/user/training/${to.params.pathMatch}` },
  { path: 'training', redirect: '/user/training/scenarios' },
  { path: 'cases/:pathMatch(.*)*', redirect: to => `/user/cases/${to.params.pathMatch}` },
  { path: 'cases', redirect: '/user/cases/list' },
  { path: 'ai', redirect: '/user/ai/chat' },
  {
    path: 'admin/learning',
    name: 'AdminLearning',
    redirect: '/admin/learning/courses',
    meta: { title: '培训课程', roles: ['admin'] },
    children: [
      {
        path: 'exams',
        name: 'LearningExamList',
        component: () => import('@/pages/admin/exams/ExamListPage.vue'),
        meta: { title: '考试管理' },
      },
      {
        path: 'question-bank',
        name: 'LearningQuestionBank',
        component: () => import('@/pages/admin/question-bank/QuestionBankPage.vue'),
        meta: { title: '题库管理' },
      },
      {
        path: 'question-bank/new',
        name: 'LearningQuestionCreate',
        component: () => import('@/pages/admin/question-bank/QuestionEditPage.vue'),
        meta: { title: '新建试题', hidden: true },
      },
      {
        path: 'question-bank/:id/edit',
        name: 'LearningQuestionEdit',
        component: () => import('@/pages/admin/question-bank/QuestionEditPage.vue'),
        meta: { title: '编辑试题', hidden: true },
      },
      {
        path: 'paper-assembly',
        name: 'LearningPaperAssembly',
        component: () => import('@/pages/admin/paper-assembly/PaperAssemblyPage.vue'),
        meta: { title: '组卷管理' },
      },
      {
        path: 'monitoring',
        name: 'LearningCourseMonitoring',
        component: () => import('@/pages/admin/courses/CourseMonitoringPage.vue'),
        meta: { title: '学习监控' },
      },
      {
        path: 'courses',
        name: 'LearningCourseList',
        component: () => import('@/pages/admin/courses/CourseListPage.vue'),
        meta: { title: '课程管理' },
      },
      {
        path: 'courses/new',
        name: 'LearningCourseCreate',
        component: () => import('@/pages/admin/courses/CourseEditPage.vue'),
        meta: { title: '新建课程', hidden: true },
      },
      {
        path: 'courses/:id/edit',
        name: 'LearningCourseEdit',
        component: () => import('@/pages/admin/courses/CourseEditPage.vue'),
        meta: { title: '编辑课程', hidden: true },
      },
      {
        path: 'courses/:id',
        name: 'LearningCourseDetail',
        component: () => import('@/pages/admin/courses/CourseDetailPage.vue'),
        meta: { title: '课程详情', hidden: true },
      },
      {
        path: 'categories',
        name: 'LearningCourseCategories',
        component: () => import('@/pages/admin/courses/CourseCategoryPage.vue'),
        meta: { title: '分类配置', hidden: true },
      },
    ],
  },
  {
    path: 'admin',
    name: 'Admin',
    redirect: '/admin/users',
    meta: { title: '组织与账号', icon: 'UserFilled', roles: ['admin'] },
    children: [
      { path: 'users', name: 'UserManage', component: () => import('@/pages/admin/UserManage.vue'), meta: { title: '用户管理' } },
      { path: 'users/:id', name: 'UserDetail', component: () => import('@/pages/admin/UserDetailPage.vue'), meta: { title: '用户详情' } },
      { path: 'org', name: 'OrgDepartment', component: () => import('@/pages/admin/OrgDepartmentPage.vue'), meta: { title: '组织与部门' } },
      { path: 'roles', name: 'RolePermission', component: () => import('@/pages/admin/RolePermissionPage.vue'), meta: { title: '角色与权限' } },
      { path: 'settings', name: 'SystemSettings', component: () => import('@/pages/admin/SystemSettings.vue'), meta: { title: '系统设置' } },
      { path: 'account', name: 'AdminAccount', component: () => import('@/pages/auth/AccountSettingsPage.vue'), meta: { title: '账号设置' } },
      { path: 'courses', redirect: '/admin/learning/courses' },
      { path: 'data', redirect: '/dashboard' },
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
    path: 'search',
    name: 'UserSearch',
    component: () => import('@/pages/user/UserSearchPage.vue'),
    meta: { title: '搜索' },
  },
  {
    path: 'learning',
    redirect: '/user/learning/calendar',
    meta: { title: '学习中心' },
    children: [
      { path: 'calendar', name: 'UserLearningCalendar', component: () => import('@/pages/user/LearningCalendar.vue'), meta: { title: '学习日历' } },
      { path: 'favorites', name: 'UserMyFavorites', component: () => import('@/pages/user/MyFavorites.vue'), meta: { title: '我的收藏' } },
      { path: 'certificates', name: 'UserMyCertificates', component: () => import('@/pages/user/MyCertificates.vue'), meta: { title: '我的证书' } },
    ],
  },
  {
    path: 'courses',
    redirect: '/user/courses/list',
    meta: { title: '安全培训' },
    children: [
      { path: 'list', name: 'UserCourseList', component: () => import('@/pages/courses/CourseList.vue'), meta: { title: '课程列表' } },
      { path: 'skill-tree', name: 'UserSkillTree', component: () => import('@/pages/courses/SkillTree.vue'), meta: { title: '安全进阶路径' } },
      { path: 'my-learning', name: 'UserMyLearning', component: () => import('@/pages/courses/MyLearning.vue'), meta: { title: '我的学习' } },
      { path: 'exams', name: 'UserExamCenter', component: () => import('@/pages/user/ExamCenterPage.vue'), meta: { title: '考试中心' } },
      { path: 'learning-report', name: 'UserLearningReport', component: () => import('@/pages/user/UserLearningReportPage.vue'), meta: { title: '学习报告' } },
      { path: 'wrong-questions', name: 'UserWrongQuestions', component: () => import('@/pages/courses/WrongQuestions.vue'), meta: { title: '错题本' } },
      { path: 'wrong-questions/:chapterId', name: 'UserWrongQuestionChapter', component: () => import('@/pages/courses/WrongQuestions.vue'), meta: { title: '章节错题', hidden: true } },
      { path: ':id', name: 'UserCourseDetail', component: () => import('@/pages/courses/CourseDetail.vue'), meta: { title: '课程详情', hidden: true } },
      { path: ':courseId/chapters/:chapterId', name: 'UserChapterView', component: () => import('@/pages/courses/ChapterView.vue'), meta: { title: '章节学习', hidden: true } },
      { path: ':courseId/chapters/:chapterId/quiz', name: 'UserQuizView', component: () => import('@/pages/courses/QuizView.vue'), meta: { title: '章节测验', hidden: true } },
      { path: ':courseId/comprehensive-exam', name: 'UserComprehensiveExam', component: () => import('@/pages/courses/ComprehensiveExamView.vue'), meta: { title: '综合考试', hidden: true } },
      { path: 'quiz/result/:attemptId', name: 'UserQuizResult', component: () => import('@/pages/courses/QuizResult.vue'), meta: { title: '测验结果', hidden: true } },
    ],
  },
  {
    path: 'simulation',
    redirect: '/user/simulation/scenarios',
    meta: { title: '事故推演' },
    children: [
      { path: 'scenarios', name: 'UserScenarioList', component: () => import('@/pages/simulation/ScenarioList.vue'), meta: { title: '推演场景' } },
      { path: 'records', name: 'UserSimulationRecords', component: () => import('@/pages/simulation/SimulationRecords.vue'), meta: { title: '推演记录' } },
      { path: 'timeline/:code', name: 'TimelineDeductionRun', component: () => import('@/pages/timeline-deduction/TimelineRun.vue'), meta: { title: '时间轴推演', hidden: true, immersive: true } },
      { path: 'test/:code', name: 'TestSimulationRun', component: () => import('@/pages/test-simulation/TestSimulationRun.vue'), meta: { title: '测试作业推演', hidden: true, immersive: true } },
      { path: 'replay/:sessionId', name: 'UserSimulationReplay', component: () => import('@/pages/simulation/SimulationReplay.vue'), meta: { title: '推演回放', hidden: true, immersive: true } },
      { path: ':id', name: 'UserSimulationView', component: () => import('@/pages/simulation/SimulationView.vue'), meta: { title: '推演演示', hidden: true, immersive: true } },
    ],
  },
  { path: 'timeline-deduction', redirect: '/user/simulation/scenarios' },
  { path: 'timeline-deduction/hub', redirect: '/user/simulation/scenarios' },
  { path: 'timeline-deduction/run/:code', redirect: to => `/user/simulation/timeline/${to.params.code}` },
  {
    path: 'training',
    redirect: '/user/training/scenarios',
    meta: { title: '应急训练' },
    children: [
      { path: 'scenarios', name: 'UserTrainingScenarioList', component: () => import('@/pages/training/TrainingScenarioList.vue'), meta: { title: '训练场景' } },
      { path: 'records', name: 'UserTrainingRecords', component: () => import('@/pages/training/TrainingRecords.vue'), meta: { title: '训练记录' } },
      { path: 'records/:id', name: 'UserTrainingReport', component: () => import('@/pages/training/TrainingReport.vue'), meta: { title: '训练报告', hidden: true } },
      { path: ':id', name: 'UserTrainingView', component: () => import('@/pages/training/TrainingView.vue'), meta: { title: '训练进行中', hidden: true, immersive: true } },
    ],
  },
  {
    path: 'cases',
    redirect: '/user/cases/list',
    meta: { title: '事故案例' },
    children: [
      { path: 'list', name: 'UserCaseList', component: () => import('@/pages/cases/CaseList.vue'), meta: { title: '案例库' } },
      { path: 'review', redirect: '/user/cases/list' },
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
  {
    path: 'exams/:id',
    name: 'UserPaperExam',
    component: () => import('@/pages/user/PaperExamView.vue'),
    meta: { title: '独立试卷', hidden: true, immersive: true },
  },
  {
    path: 'account',
    name: 'UserAccount',
    component: () => import('@/pages/auth/AccountSettingsPage.vue'),
    meta: { title: '账号设置' },
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
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/pages/auth/ForgotPasswordPage.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('@/pages/auth/ResetPasswordPage.vue'),
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

  if (userStore.role === 'admin') {
    const permission = resolveRoutePermission(to.path)
    if (permission && !userStore.hasPermission(permission.module, permission.action || 'view')) {
      return next('/dashboard')
    }
  }

  return next()
})

export default router
