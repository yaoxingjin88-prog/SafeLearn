<template>
  <div class="sl-page case-detail">
    <el-page-header @back="$router.back()">
      <template #content>
        <span class="text-lg font-bold">{{ caseData.title }}</span>
      </template>
    </el-page-header>

    <el-row :gutter="24" class="mt-6">
      <!-- 左侧内容 -->
      <el-col :span="16">
        <!-- 基本信息 -->
        <el-card>
          <div class="case-header">
            <el-tag :type="getSeverityType(caseData.severity)" size="large">
              {{ getSeverityName(caseData.severity) }}
            </el-tag>
            <el-tag :type="getTypeType(caseData.type)" size="large">
              {{ getTypeName(caseData.type) }}
            </el-tag>
          </div>
          <h1 class="case-title">{{ caseData.title }}</h1>
          <div class="case-meta">
            <span><el-icon><Location /></el-icon> {{ caseData.location }}</span>
            <span><el-icon><Calendar /></el-icon> {{ caseData.date }}</span>
          </div>
          <p class="case-desc">{{ caseData.description }}</p>
        </el-card>

        <!-- 时间线 -->
        <el-card class="mt-4">
          <template #header>
            <span class="font-bold">事故时间线</span>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="event in caseData.timeline"
              :key="event.id"
              :timestamp="event.time"
              :type="getTimelineType(event.type)"
              placement="top"
            >
              <h4>{{ event.title }}</h4>
              <p class="text-gray-600">{{ event.description }}</p>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <!-- 原因分析 -->
        <el-card class="mt-4">
          <template #header>
            <span class="font-bold">原因分析</span>
          </template>
          <p class="text-gray-700 leading-relaxed">{{ caseData.causeAnalysis }}</p>
        </el-card>

        <!-- 经验教训 -->
        <el-card class="mt-4">
          <template #header>
            <span class="font-bold">经验教训</span>
          </template>
          <p class="text-gray-700 leading-relaxed">{{ caseData.lessonsLearned }}</p>
        </el-card>
      </el-col>

      <!-- 右侧信息 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span class="font-bold">事故概况</span>
          </template>
          <div class="info-list">
            <div class="info-item">
              <span class="label">事故类型</span>
              <span class="value">{{ getTypeName(caseData.type) }}</span>
            </div>
            <div class="info-item">
              <span class="label">严重程度</span>
              <span class="value">{{ getSeverityName(caseData.severity) }}</span>
            </div>
            <div class="info-item">
              <span class="label">发生地点</span>
              <span class="value">{{ caseData.location }}</span>
            </div>
            <div class="info-item">
              <span class="label">发生日期</span>
              <span class="value">{{ caseData.date }}</span>
            </div>
            <div class="info-item">
              <span class="label">损失估计</span>
              <span class="value">{{ caseData.lossEstimate }}</span>
            </div>
          </div>
        </el-card>

        <el-card class="mt-4">
          <template #header>
            <span class="font-bold">相关资料</span>
          </template>
          <div class="reference-list">
            <div v-for="(ref, index) in caseData.references" :key="index" class="reference-item">
              <el-icon><Link /></el-icon>
              <a :href="ref" target="_blank">{{ ref }}</a>
            </div>
            <div v-if="caseData.references.length === 0" class="text-gray-400 text-sm">
              暂无相关资料
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Location, Calendar, Link } from '@element-plus/icons-vue'
import request from '@/api/request'

const route = useRoute()

const caseData = ref({
  id: '', title: '', location: '', date: '', type: 'fire', severity: 'major',
  description: '', timeline: [] as any[], causeAnalysis: '', lossEstimate: '',
  lessonsLearned: '', references: [] as string[],
})

onMounted(async () => {
  const caseId = route.params.id as string
  const res = await request.get(`/cases/${caseId}`)
  caseData.value = res.data
})

function getSeverityType(severity: string) {
  const map: Record<string, string> = { minor: 'info', moderate: 'warning', major: 'danger', critical: 'danger' }
  return map[severity] || ''
}

function getSeverityName(severity: string) {
  const map: Record<string, string> = { minor: '轻微', moderate: '中等', major: '严重', critical: '特别严重' }
  return map[severity] || severity
}

function getTypeType(type: string) {
  const map: Record<string, string> = { fire: 'danger', explosion: 'warning', thermal_runaway: 'warning', gas_leak: 'info' }
  return map[type] || ''
}

function getTypeName(type: string) {
  const map: Record<string, string> = { fire: '火灾', explosion: '爆炸', thermal_runaway: '热失控', gas_leak: '气体泄漏' }
  return map[type] || type
}

function getTimelineType(type: string) {
  const map: Record<string, string> = { detection: 'primary', alarm: 'warning', response: 'success', escalation: 'danger', resolution: 'success' }
  return map[type] || ''
}

</script>

<style scoped>
.case-header {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.case-title {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 16px;
  color: #1f2937;
}

.case-meta {
  display: flex;
  gap: 24px;
  color: #6b7280;
  margin-bottom: 16px;
}

.case-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.case-desc {
  font-size: 15px;
  line-height: 1.8;
  color: #374151;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
}

.info-item .label {
  color: #6b7280;
}

.info-item .value {
  font-weight: 500;
  color: #1f2937;
}

.reference-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reference-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reference-item a {
  color: #3b82f6;
  text-decoration: none;
  font-size: 14px;
}

.reference-item a:hover {
  text-decoration: underline;
}
</style>
