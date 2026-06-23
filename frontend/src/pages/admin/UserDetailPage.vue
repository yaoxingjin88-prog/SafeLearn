<template>
  <div class="user-detail-page" v-loading="loading">
    <button type="button" class="back-link" @click="goBack">
      <el-icon><ArrowLeft /></el-icon>
      返回 用户详情
    </button>

    <div v-if="detail" class="detail-layout">
      <aside class="profile-card">
        <div class="profile-header">
          <el-avatar :size="88" :src="detail.profile.avatarUrl || undefined" class="profile-avatar">
            {{ detail.profile.username?.charAt(0) }}
          </el-avatar>
          <h2>{{ detail.profile.username }}</h2>
          <div class="profile-badges">
            <span class="role-tag">{{ roleLabel(detail.profile.role) }}</span>
            <span class="status-pill" :class="detail.profile.enabled ? 'status-active' : 'status-disabled'">
              {{ detail.profile.enabled ? '正常' : '停用' }}
            </span>
          </div>
        </div>

        <dl class="info-list">
          <div v-for="item in profileItems" :key="item.label" class="info-row">
            <dt>{{ item.label }}</dt>
            <dd :class="item.class">{{ item.value }}</dd>
          </div>
        </dl>

        <div class="tags-section">
          <div class="section-title">标签</div>
          <div class="tag-list">
            <span v-for="tag in detail.profile.tags" :key="tag" class="skill-tag">{{ tag }}</span>
            <button type="button" class="add-tag-btn" @click="openAddTag">+ 添加标签</button>
          </div>
        </div>
      </aside>

      <section class="detail-main">
        <div class="stat-row">
          <div v-for="item in statCards" :key="item.label" class="stat-card" :class="`tone-${item.tone}`">
            <div class="stat-icon"><el-icon :size="22"><component :is="item.icon" /></el-icon></div>
            <div class="stat-body">
              <span class="stat-label">{{ item.label }}</span>
              <strong>{{ item.value }}<small>{{ item.unit }}</small></strong>
            </div>
          </div>
        </div>

        <div class="widget-row widget-row-3">
          <div class="widget-card">
            <div class="widget-title">培训学习档案</div>
            <div class="training-widget">
              <div class="donut-wrap">
                <VChart class="donut-chart" :option="trainingChartOption" autoresize />
                <div class="donut-center">
                  <span class="donut-rate">{{ detail.trainingArchive.progressRate }}%</span>
                  <span class="donut-label">进阶比率</span>
                </div>
              </div>
              <ul class="archive-legend">
                <li><i class="dot dot-completed" />已完成 <strong>{{ detail.trainingArchive.completed }}门</strong></li>
                <li><i class="dot dot-progress" />进行中 <strong>{{ detail.trainingArchive.inProgress }}门</strong></li>
                <li><i class="dot dot-not-started" />未开始 <strong>{{ detail.trainingArchive.notStarted }}门</strong></li>
                <li><i class="dot dot-expired" />过期未学 <strong>{{ detail.trainingArchive.expired }}门</strong></li>
              </ul>
            </div>
          </div>

          <div class="widget-card">
            <div class="widget-title">考试成绩</div>
            <div class="exam-stats">
              <div class="exam-stat"><span>平均分</span><strong>{{ detail.examSummary.avgScore }}分</strong></div>
              <div class="exam-stat"><span>考试次数</span><strong>{{ detail.examSummary.attemptCount }}次</strong></div>
              <div class="exam-stat"><span>通过率</span><strong>{{ detail.examSummary.passRate }}%</strong></div>
            </div>
            <div v-if="detail.examSummary.latestExam" class="latest-exam">
              <div class="latest-label">最近一次考试</div>
              <div class="latest-body">
                <span class="latest-title">{{ detail.examSummary.latestExam.title }}</span>
                <strong class="latest-score">{{ detail.examSummary.latestExam.score }} 分</strong>
                <span class="latest-date">{{ detail.examSummary.latestExam.date }}</span>
              </div>
            </div>
            <el-empty v-else description="暂无考试记录" :image-size="56" />
          </div>

          <div class="widget-card">
            <div class="widget-title">证书与到期提醒</div>
            <ul v-if="detail.certificateReminders.length" class="cert-list">
              <li v-for="cert in detail.certificateReminders" :key="cert.id">
                <span class="cert-title">{{ cert.title }}</span>
                <span class="cert-date">{{ cert.expiresAt }}</span>
                <span class="cert-days" :class="`urgency-${cert.urgency}`">{{ cert.daysLeft }}天</span>
              </li>
            </ul>
            <el-empty v-else description="暂无证书" :image-size="56" />
            <button type="button" class="more-link">更多证书 ></button>
          </div>
        </div>

        <div class="widget-row widget-row-2">
          <div class="widget-card">
            <div class="widget-title">最近学习课程</div>
            <ul v-if="detail.recentCourses.length" class="course-list">
              <li v-for="course in detail.recentCourses" :key="course.id">
                <img :src="coverUrl(course.coverImage)" alt="" class="course-thumb" />
                <div class="course-info">
                  <div class="course-title">{{ course.title }}</div>
                  <div class="course-progress">
                    <el-progress :percentage="course.progress" :stroke-width="6" :show-text="false" />
                    <span>{{ course.progress }}%</span>
                  </div>
                  <div v-if="course.completedAt" class="course-date">完成时间 {{ course.completedAt }}</div>
                </div>
              </li>
            </ul>
            <el-empty v-else description="暂无学习记录" :image-size="56" />
            <button type="button" class="more-link">查看更多 ></button>
          </div>

          <div class="widget-card">
            <div class="widget-title">风险预警记录</div>
            <table v-if="detail.warnings.length" class="warning-table">
              <thead>
                <tr>
                  <th>预警类型</th>
                  <th>预警内容</th>
                  <th>预警时间</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, idx) in detail.warnings" :key="idx">
                  <td>{{ row.type }}</td>
                  <td>{{ row.content }}</td>
                  <td>{{ row.time || '—' }}</td>
                  <td>
                    <span class="warning-status" :class="row.status === 'processed' ? 'is-done' : 'is-pending'">
                      {{ row.status === 'processed' ? '已处理' : '待处理' }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
            <el-empty v-else description="暂无预警" :image-size="56" />
            <button type="button" class="more-link">查看更多 ></button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import { TooltipComponent } from 'echarts/components'
import type { EChartsOption } from 'echarts'
import { ArrowLeft, Document, Medal, Reading, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, type AdminUserDetail } from '@/api/admin'
import { roleLabel } from './userManageShared'
import { coverUrl } from './courses/courseAdminShared'

use([CanvasRenderer, PieChart, TooltipComponent])

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref<AdminUserDetail | null>(null)

const profileItems = computed(() => {
  if (!detail.value) return []
  const p = detail.value.profile
  return [
    { label: '工号', value: p.employeeNo || '—' },
    { label: '部门', value: p.department || '—' },
    { label: '岗位', value: p.position || '—' },
    { label: '手机', value: p.phoneMasked || '—' },
    { label: '邮箱', value: p.email || '—' },
    { label: '入职日期', value: p.entryDate || '—' },
    { label: '账号状态', value: p.enabled ? '正常' : '停用', class: p.enabled ? 'text-success' : 'text-danger' },
    { label: '账号来源', value: p.accountSource || '—' },
  ]
})

const statCards = computed(() => {
  if (!detail.value) return []
  const s = detail.value.stats
  return [
    { label: '完成课程数', value: s.completedCourses, unit: '个', icon: Reading, tone: 'blue' },
    { label: '平均成绩', value: s.avgScore, unit: '分', icon: Document, tone: 'cyan' },
    { label: '证书数量', value: s.certificateCount, unit: '个', icon: Medal, tone: 'orange' },
    { label: '预警次数', value: s.warningCount, unit: '次', icon: WarningFilled, tone: 'red' },
  ]
})

const trainingChartOption = computed<EChartsOption>(() => {
  const archive = detail.value?.trainingArchive
  if (!archive) return {}
  const data = [
    { value: archive.completed, name: '已完成', itemStyle: { color: '#1d4ed8' } },
    { value: archive.inProgress, name: '进行中', itemStyle: { color: '#60a5fa' } },
    { value: archive.notStarted, name: '未开始', itemStyle: { color: '#f59e0b' } },
    { value: archive.expired, name: '过期未学', itemStyle: { color: '#ef4444' } },
  ].filter(d => d.value > 0)
  if (!data.length) {
    data.push({ value: 1, name: '暂无数据', itemStyle: { color: '#e5e7eb' } })
  }
  return {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['62%', '82%'],
      center: ['50%', '50%'],
      label: { show: false },
      data,
    }],
  }
})

async function loadDetail() {
  const id = route.params.id as string
  if (!id) return
  loading.value = true
  try {
    const res = await adminApi.getUserDetail(id)
    detail.value = res.data
  } catch {
    ElMessage.error('加载用户详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)

function goBack() {
  router.push('/admin/users')
}

async function openAddTag() {
  if (!detail.value) return
  try {
    const { value } = await ElMessageBox.prompt('请输入标签名称', '添加标签', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '标签不能为空',
    })
    const tag = value.trim()
    if (detail.value.profile.tags.includes(tag)) {
      ElMessage.warning('标签已存在')
      return
    }
    const tags = [...detail.value.profile.tags, tag]
    await adminApi.updateUserTags(detail.value.profile.id, tags)
    detail.value.profile.tags = tags
    ElMessage.success('标签已添加')
  } catch {
    // cancelled
  }
}
</script>

<style scoped>
.user-detail-page {
  min-height: 100%;
  padding-bottom: 24px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  background: none;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  margin-bottom: 16px;
  padding: 0;
}

.back-link:hover {
  color: #409eff;
}

.detail-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 16px;
  align-items: start;
}

.profile-card {
  background: #fff;
  border-radius: 10px;
  padding: 24px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.profile-header {
  text-align: center;
  margin-bottom: 20px;
}

.profile-avatar {
  margin-bottom: 12px;
  font-size: 32px;
}

.profile-header h2 {
  margin: 0 0 10px;
  font-size: 20px;
  color: #303133;
}

.profile-badges {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.role-tag {
  color: #409eff;
  font-size: 13px;
}

.status-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.status-active {
  background: #f0f9eb;
  color: #67c23a;
}

.status-disabled {
  background: #fef0f0;
  color: #f56c6c;
}

.info-list {
  margin: 0;
  padding: 0;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f2f3f5;
  font-size: 13px;
}

.info-row:last-child {
  border-bottom: none;
}

.info-row dt {
  color: #909399;
  flex-shrink: 0;
}

.info-row dd {
  margin: 0;
  color: #303133;
  text-align: right;
  word-break: break-all;
}

.text-success {
  color: #67c23a !important;
}

.text-danger {
  color: #f56c6c !important;
}

.tags-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f2f3f5;
}

.section-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 10px;
  font-weight: 600;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skill-tag {
  padding: 4px 10px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 4px;
  font-size: 12px;
}

.add-tag-btn {
  border: 1px dashed #dcdfe6;
  background: #fff;
  color: #909399;
  border-radius: 4px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
}

.add-tag-btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.detail-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.tone-blue .stat-icon { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.tone-cyan .stat-icon { background: linear-gradient(135deg, #06b6d4, #0891b2); }
.tone-orange .stat-icon { background: linear-gradient(135deg, #f59e0b, #d97706); }
.tone-red .stat-icon { background: linear-gradient(135deg, #ef4444, #dc2626); }

.stat-label {
  display: block;
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-body strong {
  font-size: 22px;
  color: #303133;
}

.stat-body small {
  font-size: 13px;
  font-weight: normal;
  color: #909399;
  margin-left: 2px;
}

.widget-row {
  display: grid;
  gap: 16px;
}

.widget-row-3 {
  grid-template-columns: repeat(3, 1fr);
}

.widget-row-2 {
  grid-template-columns: 1fr 1fr;
}

.widget-card {
  background: #fff;
  border-radius: 10px;
  padding: 18px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  min-height: 240px;
  display: flex;
  flex-direction: column;
}

.widget-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 14px;
}

.training-widget {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.donut-wrap {
  position: relative;
  width: 140px;
  height: 140px;
  flex-shrink: 0;
}

.donut-chart {
  width: 140px;
  height: 140px;
}

.donut-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.donut-rate {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  line-height: 1.1;
}

.donut-label {
  font-size: 12px;
  color: #909399;
}

.archive-legend {
  list-style: none;
  margin: 0;
  padding: 0;
  flex: 1;
  font-size: 13px;
  color: #606266;
}

.archive-legend li {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.archive-legend strong {
  margin-left: auto;
  color: #303133;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot-completed { background: #1d4ed8; }
.dot-progress { background: #60a5fa; }
.dot-not-started { background: #f59e0b; }
.dot-expired { background: #ef4444; }

.exam-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 16px;
}

.exam-stat {
  text-align: center;
  padding: 10px 6px;
  background: #f8fafc;
  border-radius: 8px;
}

.exam-stat span {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.exam-stat strong {
  font-size: 16px;
  color: #303133;
}

.latest-exam {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px 14px;
}

.latest-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.latest-body {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
}

.latest-title {
  flex: 1;
  font-size: 14px;
  color: #303133;
  min-width: 120px;
}

.latest-score {
  color: #409eff;
  font-size: 18px;
}

.latest-date {
  font-size: 12px;
  color: #909399;
  width: 100%;
}

.cert-list {
  list-style: none;
  margin: 0;
  padding: 0;
  flex: 1;
}

.cert-list li {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 8px;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f2f3f5;
  font-size: 13px;
}

.cert-title {
  color: #303133;
}

.cert-date {
  color: #909399;
}

.cert-days {
  font-weight: 600;
  min-width: 40px;
  text-align: right;
}

.urgency-danger { color: #f56c6c; }
.urgency-warning { color: #e6a23c; }
.urgency-normal { color: #67c23a; }

.course-list {
  list-style: none;
  margin: 0;
  padding: 0;
  flex: 1;
}

.course-list li {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f2f3f5;
}

.course-thumb {
  width: 72px;
  height: 48px;
  border-radius: 6px;
  object-fit: cover;
  background: #f2f3f5;
  flex-shrink: 0;
}

.course-info {
  flex: 1;
  min-width: 0;
}

.course-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.course-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #606266;
}

.course-progress :deep(.el-progress) {
  flex: 1;
}

.course-date {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.warning-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  flex: 1;
}

.warning-table th,
.warning-table td {
  padding: 10px 8px;
  text-align: left;
  border-bottom: 1px solid #f2f3f5;
}

.warning-table th {
  color: #909399;
  font-weight: 500;
}

.warning-status {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.is-done {
  background: #f0f9eb;
  color: #67c23a;
}

.is-pending {
  background: #fdf6ec;
  color: #e6a23c;
}

.more-link {
  align-self: flex-start;
  margin-top: 12px;
  border: none;
  background: none;
  color: #409eff;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}

@media (max-width: 1280px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .widget-row-3,
  .widget-row-2 {
    grid-template-columns: 1fr;
  }
}
</style>
