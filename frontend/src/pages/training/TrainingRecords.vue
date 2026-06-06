<template>
  <div class="training-records">
    <h2 class="page-title">训练记录</h2>
    <el-card v-loading="loading">
      <el-table :data="records" style="width: 100%">
        <el-table-column prop="scenarioName" label="训练场景" min-width="200" />
        <el-table-column prop="totalScore" label="得分" width="100" />
        <el-table-column prop="rating" label="评级" width="100">
          <template #default="{ row }">
            <el-tag :type="ratingType(row.rating)">{{ ratingLabel(row.rating) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="completedAt" label="完成时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="router.push(p(`/training/records/${row.id}`))">
              查看报告
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !records.length" description="暂无训练记录" />
    </el-card>
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
const records = ref<any[]>([])

function ratingType(r: string) {
  const map: Record<string, string> = { excellent: 'success', good: '', average: 'warning', poor: 'danger' }
  return map[r] || ''
}

function ratingLabel(r: string) {
  const map: Record<string, string> = { excellent: '优秀', good: '良好', average: '及格', poor: '不及格' }
  return map[r] || r || '-'
}

onMounted(async () => {
  try {
    const res = await request.get('/training/records')
    records.value = res.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.training-records {
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
</style>
