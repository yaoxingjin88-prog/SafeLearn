<template>
  <div class="sl-page exam-center">
    <div class="sl-page-head">
      <h2 class="sl-page-title">考试中心</h2>
      <p class="sl-page-desc">查看已发布的正式考试、章节考试与综合考试</p>
    </div>

    <el-card v-loading="loading">
      <el-table :data="exams" style="width: 100%">
        <el-table-column label="考试名称" min-width="240">
          <template #default="{ row }">
            <div class="exam-title-cell">
              <span>{{ row.title }}</span>
              <el-tag size="small" :type="sourceTagType(row.sourceType)">{{ sourceLabel(row.sourceType) }}</el-tag>
            </div>
            <div v-if="row.courseTitle" class="exam-sub">{{ row.courseTitle }}</div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="88">
          <template #default="{ row }">
            {{ row.examType === 'formal' ? '正式' : '模拟' }}
          </template>
        </el-table-column>
        <el-table-column label="时长" width="88">
          <template #default="{ row }">{{ row.timeLimit || '—' }} 分钟</template>
        </el-table-column>
        <el-table-column label="及格线" width="88">
          <template #default="{ row }">{{ row.passScore ?? '—' }} 分</template>
        </el-table-column>
        <el-table-column label="已考次数" width="96" prop="attemptCount" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.examPassed"
              type="success"
              link
              disabled
            >
              已通过
            </el-button>
            <el-button v-else type="primary" link @click="goExam(row)">进入考试</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !exams.length" description="暂无可参加的考试" />
      <div v-if="total > 0" class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadExams"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { examApi, type UserExamItem } from '@/api/exam'
import { useAppBase } from '@/composables/useAppBase'

const router = useRouter()
const { p } = useAppBase()

const loading = ref(true)
const exams = ref<UserExamItem[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

function sourceLabel(type: string) {
  const map: Record<string, string> = {
    paper: '独立试卷',
    chapter: '章节考试',
    comprehensive: '综合考试',
  }
  return map[type] || type
}

function sourceTagType(type: string) {
  if (type === 'comprehensive') return 'warning'
  if (type === 'paper') return 'success'
  return ''
}

function goExam(row: UserExamItem) {
  if (!row.link) {
    ElMessage.info('暂不支持此类型考试')
    return
  }
  router.push(row.link.startsWith('/user') ? row.link : p(row.link.replace(/^\//, '')))
}

async function loadExams() {
  loading.value = true
  try {
    const res = await examApi.getList({ page: page.value, pageSize: pageSize.value })
    exams.value = res.data.items || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(loadExams)
</script>

<style scoped>
.exam-title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.exam-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
