<template>
  <div class="sl-page case-review">
    <div class="sl-page-head">
      <h2 class="sl-page-title">案例复盘</h2>
    </div>
    <el-row :gutter="20" v-loading="loading">
      <el-col :span="12" v-for="item in cases" :key="item.id">
        <el-card class="review-card" shadow="hover" @click="router.push(p(`/cases/${item.id}`))">
          <div class="card-header">
            <el-tag :type="severityType(item.severity)">{{ severityLabel(item.severity) }}</el-tag>
            <span class="date">{{ item.date }}</span>
          </div>
          <h3>{{ item.title }}</h3>
          <p class="location">{{ item.location }}</p>
          <div class="timeline-preview" v-if="item.timeline?.length">
            <div v-for="(evt, idx) in item.timeline.slice(0, 3)" :key="idx" class="timeline-item">
              <span class="time">{{ evt.time }}</span>
              <span class="event">{{ evt.title }}</span>
            </div>
          </div>
          <el-button type="primary" link class="mt-2">查看完整复盘 →</el-button>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!loading && !cases.length" description="暂无案例" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/api/request'
import { useAppBase } from '@/composables/useAppBase'

const router = useRouter()
const { p } = useAppBase()
const loading = ref(true)
const cases = ref<any[]>([])

function severityType(s: string) {
  const map: Record<string, string> = { minor: 'info', moderate: 'warning', major: 'danger', critical: 'danger', severe: 'danger' }
  return map[s] || ''
}

function severityLabel(s: string) {
  const map: Record<string, string> = { minor: '轻微', moderate: '一般', major: '重大', critical: '特别重大', severe: '特别重大' }
  return map[s] || s
}

onMounted(async () => {
  try {
    const res = await request.get('/cases')
    cases.value = res.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.review-card {
  margin-bottom: 20px;
  cursor: pointer;
}

.card-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.date {
  font-size: 13px;
  color: #9ca3af;
}

.review-card h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
}

.location {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 12px;
}

.timeline-preview {
  border-left: 2px solid #e5e7eb;
  padding-left: 12px;
}

.timeline-item {
  font-size: 13px;
  margin-bottom: 6px;
}

.timeline-item .time {
  color: #2b5aed;
  margin-right: 8px;
  font-weight: 500;
}
</style>
