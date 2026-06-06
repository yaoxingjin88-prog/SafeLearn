<template>
  <div class="ai-history">
    <h2 class="page-title">问答历史</h2>
    <el-card v-loading="loading">
      <div v-for="item in records" :key="item.id" class="history-item">
        <div class="question">
          <el-icon><ChatDotRound /></el-icon>
          {{ item.question }}
        </div>
        <div class="answer">{{ item.answer }}</div>
        <div class="meta">
          <span>{{ item.createdAt }}</span>
          <el-rate v-if="item.rating" :model-value="item.rating" disabled />
        </div>
      </div>
      <el-empty v-if="!loading && !records.length" description="暂无问答记录" />
      <div class="flex justify-center mt-4" v-if="total > records.length">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ChatDotRound } from '@element-plus/icons-vue'
import request from '@/api/request'

const loading = ref(true)
const records = ref<any[]>([])
const page = ref(1)
const pageSize = 10
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const res = await request.get('/ai/history', { params: { page: page.value, pageSize } })
    records.value = res.data.items || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.ai-history {
  width: 100%;
  min-height: 100%;
  padding: 20px;
  box-sizing: border-box;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 20px;
}

.history-item {
  padding: 16px 0;
  border-bottom: 1px solid #f3f4f6;
}

.question {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.answer {
  font-size: 14px;
  color: #4b5563;
  line-height: 1.7;
  margin-bottom: 8px;
  white-space: pre-wrap;
}

.meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #9ca3af;
}
</style>
