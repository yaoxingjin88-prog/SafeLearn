<template>
  <div class="sl-page wrong-questions">
    <div class="sl-page-header">
      <div class="sl-page-head">
        <el-button
          v-if="chapterId"
          class="back-link"
          text
          @click="goToList"
        >
          <el-icon><ArrowLeft /></el-icon>
          返回章节列表
        </el-button>
        <h2 class="sl-page-title">{{ pageTitle }}</h2>
        <p class="sl-page-desc">{{ pageDesc }}</p>
      </div>
      <el-tag v-if="data && !loading && data.totalWrong > 0" type="danger" size="large" class="count-tag">
        {{ chapterId ? `${activeChapterCount} 道错题` : `共 ${activeTotal} 道错题` }}
      </el-tag>
    </div>

    <el-card v-loading="loading" shadow="never" class="content-card">
      <div v-if="!loading && (!data || data.totalWrong === 0)" class="empty-container">
        <el-empty description="暂无错题数据">
          <template #image>
            <div class="empty-icon">
              <el-icon :size="80"><CircleCheck /></el-icon>
            </div>
          </template>
          <el-button type="primary" @click="goToCourses">去学习课程</el-button>
        </el-empty>
      </div>

      <WrongQuestionChapterDetail
        v-else-if="chapterId && chapterQuestions.length"
        :chapter-id="chapterId"
        :questions="chapterQuestions"
      />

      <div v-else-if="chapterId && !chapterQuestions.length" class="empty-container">
        <el-empty description="该章节暂无错题">
          <el-button type="primary" @click="goToList">返回章节列表</el-button>
        </el-empty>
      </div>

      <WrongQuestionChapterList v-else-if="data" :data="data" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, CircleCheck } from '@element-plus/icons-vue'
import { getWrongQuestions, type WrongQuestionsData } from '@/api/quiz'
import WrongQuestionChapterList from './WrongQuestionChapterList.vue'
import WrongQuestionChapterDetail from './WrongQuestionChapterDetail.vue'
import { useWrongQuestionMastered } from '@/composables/useWrongQuestionMastered'
import { useAppBase } from '@/composables/useAppBase'

const route = useRoute()
const router = useRouter()
const { p } = useAppBase()
const { activeCount, masteredMap } = useWrongQuestionMastered()

const loading = ref(true)
const data = ref<WrongQuestionsData | null>(null)

const chapterId = computed(() => route.params.chapterId as string | undefined)

const chapterQuestions = computed(() => {
  if (!data.value || !chapterId.value) return []
  return data.value.chapters[chapterId.value] || []
})

const chapterTitle = computed(() => chapterQuestions.value[0]?.chapterTitle || '章节错题')

const pageTitle = computed(() => (chapterId.value ? chapterTitle.value : '错题本'))

const pageDesc = computed(() =>
  chapterId.value
    ? '复习本章测验错题，标记掌握后可隐藏'
    : '按章节分类管理测验错题，点击章节进入复习',
)

const activeChapterCount = computed(() => {
  void masteredMap.value
  return activeCount(chapterId.value || '', chapterQuestions.value.length)
})

const activeTotal = computed(() => {
  void masteredMap.value
  if (!data.value) return 0
  return Object.entries(data.value.chapters).reduce(
    (sum, [id, qs]) => sum + activeCount(id, qs.length),
    0,
  )
})

onMounted(async () => {
  try {
    const res = await getWrongQuestions()
    if (res.code === 200 && res.data) {
      data.value = res.data as WrongQuestionsData
    } else {
      ElMessage.error(res.message || '加载错题本失败')
    }
  } catch {
    ElMessage.error('加载错题本失败')
  } finally {
    loading.value = false
  }
})

function goToList() {
  router.push(p('/courses/wrong-questions'))
}

function goToCourses() {
  router.push(p('/courses/list'))
}
</script>

<style scoped>
.wrong-questions {
  min-height: 100%;
}

.sl-page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.sl-page-head {
  flex: 1;
  min-width: 0;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 0 0 8px;
  padding: 0;
  font-size: 14px;
  color: #606266;
}

.back-link:hover {
  color: #409eff;
}

.sl-page-desc {
  margin: 6px 0 0;
  font-size: 14px;
  color: #64748b;
  line-height: 1.5;
}

.count-tag {
  flex-shrink: 0;
}

.content-card {
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.content-card :deep(.el-card__body) {
  padding: 20px 24px;
}

.empty-container {
  padding: 48px 0;
}

.empty-icon {
  color: #67c23a;
}
</style>
