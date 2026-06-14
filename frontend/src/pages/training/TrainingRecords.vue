<template>
  <div class="sl-page training-records">
    <div class="sl-page-head">
      <h2 class="sl-page-title">训练记录</h2>
    </div>
    <el-card v-loading="loading">
      <el-table :data="records" style="width: 100%">
        <el-table-column prop="scenarioName" label="训练场景" min-width="200" />
        <el-table-column prop="totalScore" label="得分" width="100" />
        <el-table-column prop="rating" label="评级" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.rating" :type="ratingType(row.rating)">{{ ratingLabel(row.rating) }}</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="完成时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.completedAt || row.endTime || row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="router.push(p(`/training/records/${row.id}`))">
              查看报告
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !records.length" description="暂无训练记录" />
      <div v-if="total > 0" class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadRecords"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { trainingApi } from '@/api/training'
import { useAppBase } from '@/composables/useAppBase'

const router = useRouter()
const { p } = useAppBase()

const loading = ref(true)
const records = ref<any[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

function ratingType(r: string) {
  const map: Record<string, string> = { excellent: 'success', good: '', average: 'warning', poor: 'danger' }
  return map[r] || ''
}

function ratingLabel(r: string) {
  const map: Record<string, string> = { excellent: '优秀', good: '良好', average: '及格', poor: '不及格' }
  return map[r] || r || '—'
}

function formatDateTime(t?: string) {
  if (!t) return '—'
  const normalized = t.replace('T', ' ').replace(/\.\d+$/, '')
  return normalized.length >= 19 ? normalized.slice(0, 19) : normalized
}

async function loadRecords() {
  loading.value = true
  try {
    const res = await trainingApi.getRecords({ page: page.value, pageSize: pageSize.value })
    records.value = res.data.items || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSizeChange(size: number) {
  pageSize.value = size
  page.value = 1
  loadRecords()
}

onMounted(loadRecords)
</script>

<style scoped>
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
