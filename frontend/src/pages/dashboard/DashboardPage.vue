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
import { Reading, Check, Warning, Trophy, VideoPlay, Cpu, ChatDotRound } from '@element-plus/icons-vue'
import request from '@/api/request'

const stats = reactive({
  courseCount: 0,
  completedCount: 0,
  simulationCount: 0,
  avgScore: 0,
})

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
</style>
