<template>
  <div class="sl-page simulation-records">
    <div class="sl-page-head">
      <h2 class="sl-page-title">推演记录</h2>
    </div>
    <el-card v-loading="loading">
      <el-table :data="records" style="width: 100%">
        <el-table-column prop="scenarioName" label="推演场景" min-width="200" />
        <el-table-column prop="outcome" label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.outcome === 'success' ? 'success' : 'danger'">
              {{ row.outcome === 'success' ? '受控' : '扩大' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="得分" width="80" />
        <el-table-column prop="rating" label="评级" width="100">
          <template #default="{ row }">
            <el-tag>{{ ratingLabel(row.rating) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="finishedAt" label="完成时间" width="180" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button type="primary" link @click="router.push(p(`/simulation/${row.scenarioId}`))">
              再次推演
            </el-button>
            <el-button type="primary" link @click="viewReplay(row.sessionId)">
              回放
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !records.length" description="暂无推演记录，完成一次单电池推演后将显示在此" />
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

function ratingLabel(r: string) {
  const map: Record<string, string> = {
    excellent: '优秀',
    good: '良好',
    average: '及格',
    poor: '不及格',
  }
  return map[r] || r || '-'
}

function viewReplay(sessionId: string) {
  router.push(p(`/simulation/replay/${sessionId}`))
}

onMounted(async () => {
  try {
    const res = await request.get('/deduction/sessions')
    records.value = (res.data || []).filter((r: any) => r.status === 'completed')
  } finally {
    loading.value = false
  }
})
</script>
