<template>
  <div class="sl-page dashboard-page" v-loading="loading">
    <section class="welcome-section">
      <h1 class="welcome-title">{{ greeting }}，{{ overview.displayName }}！</h1>
      <p class="welcome-sub">
        今天是您加入 SafeLearn 平台的第 {{ overview.joinDays }} 天，继续保持安全学习习惯。
      </p>
    </section>

    <section class="stats-row">
      <div class="stat-card" v-for="item in statCards" :key="item.label">
        <div class="stat-icon" :style="{ background: item.bg }">
          <el-icon :size="22"><component :is="item.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value">
            {{ item.value }}<span class="stat-unit">{{ item.unit }}</span>
          </div>
        </div>
        <div v-if="item.accent" class="stat-accent" :style="{ background: item.accent }" />
      </div>
    </section>

    <section class="middle-row">
      <div class="continue-card" v-if="overview.continueLearning">
        <div class="continue-player-deco" aria-hidden="true">
          <svg viewBox="0 0 120 120" xmlns="http://www.w3.org/2000/svg">
            <circle cx="60" cy="60" r="52" fill="none" stroke="currentColor" stroke-width="5" />
            <path d="M48 38 L48 82 L84 60 Z" fill="currentColor" />
          </svg>
        </div>
        <div class="continue-content">
          <el-tag effect="dark" type="info" class="continue-tag">上次学到</el-tag>
          <h2 class="continue-title">《{{ overview.continueLearning.courseTitle }}》</h2>
          <p class="continue-chapter">{{ overview.continueLearning.chapterTitle }}</p>
          <el-button type="primary" class="continue-btn" @click="goContinue">
            <el-icon class="mr-1"><VideoPlay /></el-icon>
            继续学习
          </el-button>
          <div class="continue-progress">
            <div class="progress-meta">
              <span>进度 {{ overview.continueLearning.progress }}%</span>
              <span>剩余 {{ overview.continueLearning.remainingMinutes }} 分钟</span>
            </div>
            <el-progress
              :percentage="overview.continueLearning.progress"
              :show-text="false"
              :stroke-width="8"
              color="#fff"
            />
          </div>
        </div>
      </div>

      <div class="tasks-card">
        <div class="tasks-header">
          <h3>近期必修任务</h3>
          <el-button link type="primary" @click="router.push(p('/courses/my-learning'))">查看全部</el-button>
        </div>
        <div v-if="overview.mandatoryTasks?.length" class="task-list">
          <div v-for="task in overview.mandatoryTasks" :key="task.id" class="task-item">
            <div class="task-icon" :class="task.type">
              <el-icon><Warning v-if="task.type === 'training'" /><Reading v-else /></el-icon>
            </div>
            <div class="task-body">
              <div class="task-title">{{ task.title }}</div>
              <div class="task-deadline">{{ task.deadline }}</div>
            </div>
            <el-button size="small" type="primary" plain @click="router.push(p(task.link))">
              {{ task.actionText }}
            </el-button>
          </div>
        </div>
        <el-empty v-else description="暂无必修任务" :image-size="80" />
      </div>
    </section>

    <section v-if="overview.deductionCount > 0" class="deduction-section">
      <div class="recommend-header">
        <h3>推演训练成绩</h3>
        <el-button link type="primary" @click="router.push(p('/simulation/records'))">查看全部</el-button>
      </div>
      <div class="deduction-stats">
        <div class="ded-stat">
          <div class="ded-value">{{ overview.deductionCount }}</div>
          <div class="ded-label">完成推演</div>
        </div>
        <div class="ded-stat">
          <div class="ded-value">{{ overview.deductionAvgScore || '--' }}</div>
          <div class="ded-label">平均得分</div>
        </div>
        <div class="ded-stat">
          <div class="ded-value">{{ overview.deductionSuccessRate || 0 }}%</div>
          <div class="ded-label">受控率</div>
        </div>
      </div>
      <el-table v-if="overview.recentDeductions?.length" :data="overview.recentDeductions" size="small" class="mt-4">
        <el-table-column prop="scenarioName" label="场景" min-width="160" />
        <el-table-column prop="totalScore" label="得分" width="70" />
        <el-table-column prop="outcome" label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.outcome === 'success' ? 'success' : 'danger'" size="small">
              {{ row.outcome === 'success' ? '受控' : '扩大' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="router.push(p(`/simulation/replay/${row.sessionId}`))">
              回放
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="recommend-section">
      <div class="recommend-header">
        <h3>岗位推荐课程</h3>
        <div class="recommend-nav">
          <el-button circle :icon="ArrowLeft" size="small" />
          <el-button circle :icon="ArrowRight" size="small" />
        </div>
      </div>
      <el-row :gutter="20">
        <el-col :span="12" v-for="course in overview.recommendedCourses" :key="course.id">
          <div class="course-card" @click="router.push(p(`/courses/${course.id}`))">
            <div class="course-thumb">
              <div class="thumb-placeholder">
                <el-icon :size="40"><VideoPlay /></el-icon>
              </div>
              <el-tag v-if="course.tag" class="course-tag" effect="dark" size="small">{{ course.tag }}</el-tag>
            </div>
            <div class="course-info">
              <h4>{{ course.title }}</h4>
              <p>{{ course.description || course.meta }}</p>
            </div>
          </div>
        </el-col>
      </el-row>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Clock, CircleCheck, Medal, WarningFilled, VideoPlay, Warning, Reading,
  ArrowLeft, ArrowRight,
} from '@element-plus/icons-vue'
import request from '@/api/request'
import { useAppBase } from '@/composables/useAppBase'

const router = useRouter()
const { p } = useAppBase()
const loading = ref(true)

const overview = reactive<any>({
  displayName: '',
  joinDays: 1,
  streakDays: 0,
  studyHours: 0,
  completedCourses: 0,
  avgScore: 0,
  pendingMandatory: 0,
  continueLearning: null,
  mandatoryTasks: [],
  recommendedCourses: [],
  deductionCount: 0,
  deductionAvgScore: 0,
  deductionSuccessRate: 0,
  recentDeductions: [],
})

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 12) return '早上好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const statCards = computed(() => [
  { label: '累计学习时长', value: overview.studyHours, unit: '小时', icon: Clock, bg: '#eef3ff', accent: '' },
  { label: '已完成课程', value: overview.completedCourses, unit: '门', icon: CircleCheck, bg: '#ecfdf5', accent: '' },
  { label: '考试平均分', value: overview.avgScore || '--', unit: overview.avgScore ? '分' : '', icon: Medal, bg: '#f5f3ff', accent: '' },
  { label: '待完成必修', value: overview.pendingMandatory, unit: '项', icon: WarningFilled, bg: '#fef2f2', accent: '#ef4444' },
])

function goContinue() {
  const c = overview.continueLearning
  if (!c) return
  if (c.chapterId) {
    router.push(p(`/courses/${c.courseId}/chapters/${c.chapterId}`))
  } else {
    router.push(p(`/courses/${c.courseId}`))
  }
}

onMounted(async () => {
  try {
    const res = await request.get('/dashboard/overview')
    Object.assign(overview, res.data)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.welcome-section { margin-bottom: 24px; }
.welcome-title { font-size: 28px; font-weight: 700; color: #111827; margin-bottom: 8px; }
.welcome-sub { color: #6b7280; font-size: 14px; }
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card { background: #fff; border-radius: 14px; padding: 20px; display: flex; align-items: center; gap: 14px; box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04); position: relative; overflow: hidden; }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: #2b5aed; }
.stat-label { font-size: 13px; color: #6b7280; margin-bottom: 4px; }
.stat-value { font-size: 26px; font-weight: 700; color: #111827; }
.stat-unit { font-size: 14px; font-weight: 500; margin-left: 4px; color: #6b7280; }
.stat-accent { position: absolute; right: 0; top: 0; bottom: 0; width: 4px; }
.middle-row { display: grid; grid-template-columns: 1.6fr 1fr; gap: 20px; margin-bottom: 24px; }
.continue-card { position: relative; border-radius: 16px; overflow: hidden; min-height: 280px; background: linear-gradient(135deg, #2b5aed 0%, #4f7df3 50%, #6b93f7 100%); color: #fff; }
.continue-player-deco {
  position: absolute;
  top: -28px;
  right: -32px;
  width: 180px;
  height: 180px;
  color: rgba(255, 255, 255, 0.22);
  pointer-events: none;
  z-index: 0;
}
.continue-player-deco svg {
  width: 100%;
  height: 100%;
}
.continue-content { position: relative; padding: 28px; z-index: 1; }
.continue-tag {
  background: rgba(255, 255, 255, 0.25) !important;
  border: none !important;
  color: #fff !important;
  font-size: 15px !important;
  font-weight: 500;
  height: auto !important;
  padding: 6px 14px !important;
  margin-bottom: 18px;
}
.continue-title {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 10px;
  line-height: 1.45;
}
.continue-chapter {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.95);
  margin-bottom: 22px;
  line-height: 1.5;
}
.continue-btn { background: #fff !important; color: #2b5aed !important; border: none !important; font-weight: 600; font-size: 15px; margin-bottom: 28px; }
.continue-progress { max-width: 420px; }
.progress-meta {
  display: flex;
  justify-content: space-between;
  font-size: 15px;
  font-weight: 500;
  color: #fff;
  margin-bottom: 10px;
}
.continue-progress :deep(.el-progress-bar__outer) { background: rgba(255, 255, 255, 0.25); }
.tasks-card { background: #fff; border-radius: 16px; padding: 20px; box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04); }
.tasks-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.tasks-header h3 { font-size: 16px; font-weight: 700; color: #111827; }
.task-list { display: flex; flex-direction: column; gap: 14px; }
.task-item { display: flex; align-items: center; gap: 12px; padding: 12px; border-radius: 12px; background: #f9fafb; }
.task-icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.task-icon.training { background: #fef2f2; color: #ef4444; }
.task-icon.course { background: #eef3ff; color: #2b5aed; }
.task-body { flex: 1; min-width: 0; }
.task-title { font-size: 14px; font-weight: 600; color: #1f2937; margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.task-deadline { font-size: 12px; color: #9ca3af; }
.deduction-section { background: #fff; border-radius: 16px; padding: 20px; box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04); margin-bottom: 20px; }
.deduction-stats { display: flex; gap: 24px; margin-bottom: 8px; }
.ded-stat { text-align: center; min-width: 80px; }
.ded-value { font-size: 24px; font-weight: 700; color: #2b5aed; }
.ded-label { font-size: 12px; color: #9ca3af; margin-top: 4px; }
.mt-4 { margin-top: 16px; }
.recommend-section { background: #fff; border-radius: 16px; padding: 20px; box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04); }
.recommend-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.recommend-header h3 { font-size: 16px; font-weight: 700; }
.course-card { display: flex; gap: 16px; padding: 12px; border-radius: 12px; cursor: pointer; transition: background 0.2s; margin-bottom: 12px; }
.course-card:hover { background: #f9fafb; }
.course-thumb { position: relative; width: 140px; height: 90px; border-radius: 10px; overflow: hidden; flex-shrink: 0; }
.thumb-placeholder { width: 100%; height: 100%; background: linear-gradient(135deg, #1e293b, #334155); display: flex; align-items: center; justify-content: center; color: rgba(255, 255, 255, 0.8); }
.course-tag { position: absolute; top: 8px; left: 8px; }
.course-info h4 { font-size: 15px; font-weight: 600; color: #1f2937; margin-bottom: 6px; }
.course-info p { font-size: 13px; color: #6b7280; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
@media (max-width: 1100px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .middle-row { grid-template-columns: 1fr; }
  .continue-player-deco { width: 120px; height: 120px; top: -16px; right: -20px; opacity: 0.5; }
}
</style>
