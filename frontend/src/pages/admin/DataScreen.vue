<template>
  <div class="data-screen">
    <h2 class="text-2xl font-bold mb-6">数据大屏</h2>

    <el-row :gutter="20">
      <!-- 统计卡片 -->
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-blue-500">
            <el-icon :size="32"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalUsers }}</div>
            <div class="stat-label">总用户数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-green-500">
            <el-icon :size="32"><Reading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalCourses }}</div>
            <div class="stat-label">培训课程</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-orange-500">
            <el-icon :size="32"><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalSimulations }}</div>
            <div class="stat-label">推演次数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-purple-500">
            <el-icon :size="32"><Trophy /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.avgScore }}</div>
            <div class="stat-label">平均分数</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-6">
      <!-- 培训完成率 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span class="font-bold">培训完成率</span>
          </template>
          <div ref="completionChart" style="height: 300px"></div>
        </el-card>
      </el-col>

      <!-- 事故类型分布 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span class="font-bold">事故类型分布</span>
          </template>
          <div ref="accidentChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-6">
      <!-- 月度趋势 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <span class="font-bold">月度培训趋势</span>
          </template>
          <div ref="trendChart" style="height: 300px"></div>
        </el-card>
      </el-col>

      <!-- 风险等级分布 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span class="font-bold">风险等级分布</span>
          </template>
          <div ref="riskChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { User, Reading, Warning, Trophy } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '@/api/request'

const completionChart = ref<HTMLElement>()
const accidentChart = ref<HTMLElement>()
const trendChart = ref<HTMLElement>()
const riskChart = ref<HTMLElement>()

let charts: echarts.ECharts[] = []

const stats = reactive({
  totalUsers: 0,
  totalCourses: 0,
  totalSimulations: 0,
  avgScore: 0,
})

onMounted(async () => {
  const res = await request.get('/admin/stats')
  Object.assign(stats, res.data)
  initCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  charts.forEach(chart => chart.dispose())
  window.removeEventListener('resize', handleResize)
})

function handleResize() {
  charts.forEach(chart => chart.resize())
}

function initCharts() {
  // 培训完成率
  if (completionChart.value) {
    const chart = echarts.init(completionChart.value)
    charts.push(chart)
    chart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 20, fontWeight: 'bold' } },
        data: [
          { value: 735, name: '已完成', itemStyle: { color: '#10b981' } },
          { value: 580, name: '进行中', itemStyle: { color: '#3b82f6' } },
          { value: 484, name: '未开始', itemStyle: { color: '#d1d5db' } },
        ],
      }],
    })
  }

  // 事故类型分布
  if (accidentChart.value) {
    const chart = echarts.init(accidentChart.value)
    charts.push(chart)
    chart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: '50%',
        data: [
          { value: 580, name: '火灾', itemStyle: { color: '#ef4444' } },
          { value: 320, name: '热失控', itemStyle: { color: '#f59e0b' } },
          { value: 150, name: '爆炸', itemStyle: { color: '#8b5cf6' } },
          { value: 80, name: '气体泄漏', itemStyle: { color: '#06b6d4' } },
        ],
      }],
    })
  }

  // 月度趋势
  if (trendChart.value) {
    const chart = echarts.init(trendChart.value)
    charts.push(chart)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['培训人数', '推演次数'] },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: ['1月', '2月', '3月', '4月', '5月', '6月'],
      },
      yAxis: { type: 'value' },
      series: [
        {
          name: '培训人数',
          type: 'line',
          smooth: true,
          data: [120, 200, 150, 320, 280, 350],
          itemStyle: { color: '#3b82f6' },
        },
        {
          name: '推演次数',
          type: 'line',
          smooth: true,
          data: [80, 150, 120, 250, 200, 280],
          itemStyle: { color: '#10b981' },
        },
      ],
    })
  }

  // 风险等级分布
  if (riskChart.value) {
    const chart = echarts.init(riskChart.value)
    charts.push(chart)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: ['低风险', '中风险', '高风险', '极高风险'],
      },
      yAxis: { type: 'value' },
      series: [{
        type: 'bar',
        data: [
          { value: 450, itemStyle: { color: '#10b981' } },
          { value: 320, itemStyle: { color: '#f59e0b' } },
          { value: 180, itemStyle: { color: '#f97316' } },
          { value: 80, itemStyle: { color: '#ef4444' } },
        ],
      }],
    })
  }
}
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #1f2937;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
}
</style>
