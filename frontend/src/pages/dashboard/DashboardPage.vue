<template>
  <div class="sl-page dashboard">
    <div class="sl-page-head">
      <h2 class="sl-page-title">工作台</h2>
    </div>

    <el-row :gutter="20" class="mb-6">
      <el-col :span="6">
        <div class="stat-card bg-blue-500">
          <div class="stat-icon">
            <el-icon :size="40"><Reading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.courseCount }}</div>
            <div class="stat-label">培训课程</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card bg-green-500">
          <div class="stat-icon">
            <el-icon :size="40"><Check /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.completedCount }}</div>
            <div class="stat-label">已完成课程</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card bg-orange-500">
          <div class="stat-icon">
            <el-icon :size="40"><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.simulationCount }}</div>
            <div class="stat-label">推演次数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card bg-purple-500">
          <div class="stat-icon">
            <el-icon :size="40"><Trophy /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.avgScore }}</div>
            <div class="stat-label">平均分数</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 学习人数概览 -->
    <el-row :gutter="20" class="mb-6">
      <el-col :span="6">
        <div class="people-card">
          <div class="people-icon online">
            <el-icon :size="26"><User /></el-icon>
            <span class="online-dot"></span>
          </div>
          <div>
            <div class="people-value">{{ learning.onlineLearners }}</div>
            <div class="people-label">当前在线学习</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="people-card">
          <div class="people-icon today">
            <el-icon :size="26"><Sunny /></el-icon>
          </div>
          <div>
            <div class="people-value">{{ learning.activeToday }}</div>
            <div class="people-label">今日活跃学员</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="people-card">
          <div class="people-icon total">
            <el-icon :size="26"><UserFilled /></el-icon>
          </div>
          <div>
            <div class="people-value">{{ learning.totalLearners }}</div>
            <div class="people-label">总学习人数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="people-card">
          <div class="people-icon dept">
            <el-icon :size="26"><OfficeBuilding /></el-icon>
          </div>
          <div>
            <div class="people-value">{{ learning.departments.length }}</div>
            <div class="people-label">参训部门</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 各部门学习进度 -->
    <h3 class="text-lg font-semibold mb-4">部门学习进度</h3>
    <el-card class="mb-6 dept-card">
      <el-table v-if="learning.departments.length" :data="learning.departments" style="width: 100%">
        <el-table-column prop="name" label="部门" min-width="160">
          <template #default="{ row }">
            <div class="dept-name">
              <el-icon class="dept-name-icon"><OfficeBuilding /></el-icon>
              {{ row.name }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="参训人数" width="140" align="center">
          <template #default="{ row }">
            <span class="dept-members">
              {{ row.learnedMembers }}<span class="dept-members-total"> / {{ row.memberCount }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="completedChapters" label="累计完成章节" width="160" align="center" />
        <el-table-column label="平均进度" min-width="240">
          <template #default="{ row }">
            <el-progress
              :percentage="row.avgProgress"
              :stroke-width="14"
              :color="progressColor(row.avgProgress)"
              :text-inside="true"
            />
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无部门学习数据" />
    </el-card>

    <h3 class="text-lg font-semibold mb-4">快捷入口</h3>
    <el-row :gutter="20" class="mb-6">
      <el-col :span="6" v-for="item in quickLinks" :key="item.path">
        <div class="quick-card" @click="$router.push(item.path)">
          <el-icon :size="48" :color="item.color">
            <component :is="item.icon" />
          </el-icon>
          <div class="quick-title">{{ item.title }}</div>
          <div class="quick-desc">{{ item.desc }}</div>
        </div>
      </el-col>
    </el-row>

    <h3 class="text-lg font-semibold mb-4">最近学习</h3>
    <el-card>
      <el-table :data="recentCourses" style="width: 100%">
        <el-table-column prop="title" label="课程名称" />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="学习进度" width="200">
          <template #default="{ row }">
            <el-progress :percentage="row.progress" :status="row.progress === 100 ? 'success' : ''" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/courses/${row.id}`)">
              继续学习
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  Reading, Check, Warning, Trophy, VideoPlay, Cpu, ChatDotRound,
  User, UserFilled, Sunny, OfficeBuilding,
} from '@element-plus/icons-vue'
import request from '@/api/request'

const stats = reactive({
  courseCount: 0,
  completedCount: 0,
  simulationCount: 0,
  avgScore: 0,
})

interface DeptProgress {
  name: string
  memberCount: number
  learnedMembers: number
  completedChapters: number
  avgProgress: number
}

const learning = reactive({
  onlineLearners: 0,
  activeToday: 0,
  totalLearners: 0,
  departments: [] as DeptProgress[],
})

function progressColor(pct: number): string {
  if (pct >= 80) return '#10b981'
  if (pct >= 50) return '#3b82f6'
  if (pct >= 20) return '#f59e0b'
  return '#ef4444'
}

const quickLinks = [
  { path: '/courses', icon: Reading, color: '#3b82f6', title: '安全培训', desc: '学习储能安全知识' },
  { path: '/simulation', icon: VideoPlay, color: '#f59e0b', title: '事故推演', desc: '模拟热失控过程' },
  { path: '/training', icon: Cpu, color: '#10b981', title: '应急训练', desc: '提升处置能力' },
  { path: '/ai', icon: ChatDotRound, color: '#8b5cf6', title: 'AI问答', desc: '智能安全咨询' },
]

const recentCourses = ref([])

onMounted(async () => {
  const [statsRes, coursesRes] = await Promise.all([
    request.get('/dashboard/stats'),
    request.get('/dashboard/recent-courses'),
  ])
  Object.assign(stats, statsRes.data)
  recentCourses.value = coursesRes.data

  // 管理端学习总览（仅 ADMIN 可访问，失败时静默降级）
  try {
    const overviewRes = await request.get('/admin/learning-overview')
    Object.assign(learning, overviewRes.data)
  } catch (err) {
    console.warn('加载学习总览失败', err)
  }
})
</script>

<style scoped>
.dashboard {
  width: 100%;
  height: calc(100vh - 60px);
  box-sizing: border-box;
  overflow-y: auto;
}

.stat-card {
  padding: 24px;
  border-radius: 12px;
  color: white;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
}

.quick-card {
  padding: 24px;
  background: white;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.quick-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.quick-title {
  margin-top: 12px;
  font-weight: 600;
  color: #1f2937;
}

.quick-desc {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
}

/* 学习人数卡片 */
.people-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.people-icon {
  position: relative;
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.people-icon.online { background: linear-gradient(135deg, #10b981, #059669); }
.people-icon.today { background: linear-gradient(135deg, #f59e0b, #d97706); }
.people-icon.total { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.people-icon.dept { background: linear-gradient(135deg, #8b5cf6, #7c3aed); }

.online-dot {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #34d399;
  border: 2px solid #fff;
  box-shadow: 0 0 0 0 rgba(52, 211, 153, 0.7);
  animation: pulse 1.8s infinite;
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(52, 211, 153, 0.6); }
  70% { box-shadow: 0 0 0 8px rgba(52, 211, 153, 0); }
  100% { box-shadow: 0 0 0 0 rgba(52, 211, 153, 0); }
}

.people-value {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  line-height: 1.1;
}

.people-label {
  font-size: 13px;
  color: #6b7280;
  margin-top: 2px;
}

/* 部门进度 */
.dept-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: #1f2937;
}

.dept-name-icon {
  color: #8b5cf6;
}

.dept-members {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.dept-members-total {
  font-size: 13px;
  font-weight: 400;
  color: #9ca3af;
}
</style>
