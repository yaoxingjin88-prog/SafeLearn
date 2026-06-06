<template>
  <div class="simulation-records">
    <h2 class="page-title">推演记录</h2>
    <el-card v-loading="loading">
      <el-table :data="records" style="width: 100%">
        <el-table-column prop="name" label="推演场景" min-width="200" />
        <el-table-column prop="difficulty" label="难度" width="100">
          <template #default="{ row }">
            <el-tag>{{ difficultyLabel(row.difficulty) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长(秒)" width="100" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="router.push(p(`/simulation/${row.id}`))">
              再次推演
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !records.length" description="暂无推演记录" />
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

function difficultyLabel(d: number) {
  const map: Record<number, string> = { 1: '基础', 2: '中级', 3: '高级' }
  return map[d] || '基础'
}

onMounted(async () => {
  try {
    const res = await request.get('/simulation/scenarios')
    records.value = res.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.simulation-records {
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
