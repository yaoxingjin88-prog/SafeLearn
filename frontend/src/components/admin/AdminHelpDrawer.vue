<template>
  <button type="button" class="header-icon-btn" title="帮助" @click="visible = true">
    <el-icon><QuestionFilled /></el-icon>
  </button>

  <el-drawer v-model="visible" title="帮助中心" size="420px" append-to-body>
    <section class="help-section">
      <h3>{{ currentGuide.title }}</h3>
      <p>{{ currentGuide.summary }}</p>
      <ul class="help-list">
        <li v-for="(tip, index) in currentGuide.tips" :key="index">{{ tip }}</li>
      </ul>
    </section>

    <section class="help-section">
      <h3>快捷入口</h3>
      <div class="help-links">
        <button v-for="link in quickLinks" :key="link.path" type="button" @click="go(link.path)">
          {{ link.label }}
        </button>
      </div>
    </section>

    <section class="help-section">
      <h3>常见问题</h3>
      <el-collapse>
        <el-collapse-item v-for="faq in faqs" :key="faq.q" :title="faq.q" :name="faq.q">
          <p>{{ faq.a }}</p>
        </el-collapse-item>
      </el-collapse>
    </section>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { QuestionFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const visible = ref(false)

const moduleGuides: Record<string, { title: string; summary: string; tips: string[] }> = {
  '/dashboard': {
    title: '首页驾驶舱',
    summary: '查看培训完成率、安全预警、培训计划与公告通知。',
    tips: ['顶部统计卡可快速了解全局指标', '「最新安全预警」点击更多进入预警中心', '快捷入口可跳转常用管理功能'],
  },
  '/admin/learning/courses': {
    title: '课程管理',
    summary: '维护培训课程、章节内容与发布状态。',
    tips: ['新建课程后需发布才会对学员可见', '可在课程详情中管理章节与测验', '支持按分类、部门筛选课程'],
  },
  '/admin/learning/monitoring': {
    title: '学习监控',
    summary: '按课程查看学员学习进度、考试得分与预警状态。',
    tips: ['选择课程后可筛选低进度学员', '预警状态可用于发现未完成培训人员', '支持导出学员学习数据'],
  },
  '/admin/learning/exams': {
    title: '考试管理',
    summary: '管理章节考试与组卷发布的考试。',
    tips: ['可预览学员端考试页面', '查看成绩了解通过率', '组卷发布的考试会出现在此列表'],
  },
  '/admin/learning/question-bank': {
    title: '题库管理',
    summary: '维护试题分类、题型与难度，支持批量操作。',
    tips: ['左侧分类树按业务领域组织题目', '支持单选、多选、判断等题型', '已发布题目可用于组卷'],
  },
  '/admin/learning/paper-assembly': {
    title: '组卷管理',
    summary: '按规则自动抽题组卷，发布为正式考试。',
    tips: ['配置题型比例与难度分布', '预览试卷结构后再发布', '发布后考试会同步到考试管理'],
  },
  '/admin/org': {
    title: '组织与部门',
    summary: '维护组织架构、部门成员与岗位设置。',
    tips: ['左侧组织树切换部门', '成员编辑可在当前页抽屉完成', '岗位可标记是否为高风险岗位'],
  },
  '/admin/users': {
    title: '用户管理',
    summary: '管理平台账号、角色与部门归属。',
    tips: ['支持按部门、角色、证书状态筛选', '点击姓名可查看用户详情档案', '批量操作支持启用、停用与删除'],
  },
  '/admin/settings': {
    title: '系统设置',
    summary: '配置平台参数与首页公告等内容。',
    tips: ['修改配置后可能需要刷新页面', '首页公告会同步到消息中心'],
  },
}

const defaultGuide = {
  title: '管理平台帮助',
  summary: '储能安全培训管理平台管理端，覆盖课程、考试、组织与用户管理。',
  tips: ['左侧菜单按业务模块分组', '顶部通知为需处理的预警，消息为平台告知', '遇到问题请联系系统管理员'],
}

const currentGuide = computed(() => {
  const path = route.path
  const matched = Object.keys(moduleGuides)
    .sort((a, b) => b.length - a.length)
    .find(key => path === key || path.startsWith(`${key}/`))
  return matched ? moduleGuides[matched] : defaultGuide
})

const quickLinks = [
  { label: '首页', path: '/dashboard' },
  { label: '通知中心', path: '/dashboard/notifications' },
  { label: '消息中心', path: '/dashboard/messages' },
  { label: '预警中心', path: '/dashboard/alerts' },
  { label: '课程管理', path: '/admin/learning/courses' },
  { label: '用户管理', path: '/admin/users' },
]

const faqs = [
  { q: '通知和消息有什么区别？', a: '通知是需要管理员处理的预警（如考试未通过、培训进度低）；消息是平台告知类信息（如公告、课程上线）。' },
  { q: '为什么角标数字与列表不一致？', a: '角标表示未读数量。持久化通知/消息在服务端记录已读；动态预警类通知在本地浏览器记录已读。连接 SSE 后新消息会实时推送并自动刷新。' },
  { q: '如何发布新课程？', a: '进入课程管理 → 新建课程 → 编辑章节 → 将状态设为已发布。' },
  { q: '管理员无法访问某些页面？', a: '请确认账号角色为 admin，并重新登录刷新权限。' },
]

function go(path: string) {
  visible.value = false
  router.push(path)
}
</script>

<style scoped>
.help-section {
  margin-bottom: 24px;
}

.help-section h3 {
  margin: 0 0 8px;
  font-size: 15px;
  color: #1c2738;
}

.help-section > p {
  margin: 0 0 10px;
  font-size: 13px;
  color: #667085;
  line-height: 1.6;
}

.help-list {
  margin: 0;
  padding-left: 18px;
  color: #485568;
  font-size: 13px;
  line-height: 1.7;
}

.help-links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.help-links button {
  border: 1px solid #dbe3f0;
  background: #f8fafc;
  color: #3478ef;
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
}

.help-links button:hover {
  border-color: #3478ef;
  background: #edf3ff;
}

.help-section :deep(.el-collapse-item__header) {
  font-size: 13px;
}

.help-section :deep(.el-collapse-item__content p) {
  margin: 0;
  font-size: 13px;
  color: #667085;
  line-height: 1.6;
}
</style>
