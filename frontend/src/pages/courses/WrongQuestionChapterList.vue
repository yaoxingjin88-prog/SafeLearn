<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, ArrowRight, Reading, RefreshLeft } from '@element-plus/icons-vue'
import type { WrongQuestionsData } from '@/api/quiz'
import type { ChapterSummary, ChapterSortMode } from './wrongQuestionsTypes'
import { useWrongQuestionMastered } from '@/composables/useWrongQuestionMastered'
import { useAppBase } from '@/composables/useAppBase'

const props = defineProps<{ data: WrongQuestionsData }>()

const router = useRouter()
const { p } = useAppBase()
const { activeCount, hasMasteredRecords, resetAllMastered, masteredMap } = useWrongQuestionMastered()

const sortMode = ref<ChapterSortMode>('count')

const summaries = computed<ChapterSummary[]>(() => {
  void masteredMap.value
  const list: ChapterSummary[] = []
  for (const [chapterId, questions] of Object.entries(props.data.chapters)) {
    if (!questions.length) continue
    const dates = questions.map(q => q.attemptDate).filter(Boolean)
    const lastPracticeDate = dates.sort().reverse()[0] || ''
    list.push({
      chapterId,
      chapterTitle: questions[0].chapterTitle || '未知章节',
      wrongCount: activeCount(chapterId, questions.length),
      totalCount: questions.length,
      lastPracticeDate,
      courseId: questions[0].courseId,
    })
  }
  return list.filter(s => s.wrongCount > 0)
})

const sortedSummaries = computed(() => {
  const list = [...summaries.value]
  if (sortMode.value === 'name') {
    return list.sort((a, b) => a.chapterTitle.localeCompare(b.chapterTitle, 'zh-CN'))
  }
  return list.sort((a, b) => {
    if (b.wrongCount !== a.wrongCount) return b.wrongCount - a.wrongCount
    return (b.lastPracticeDate || '').localeCompare(a.lastPracticeDate || '')
  })
})

function formatDate(dateStr: string) {
  if (!dateStr) return '暂无记录'
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric', month: 'short', day: 'numeric',
  })
}

function openChapter(chapterId: string) {
  router.push(p(`/courses/wrong-questions/${chapterId}`))
}

const showResetMastered = computed(() => hasMasteredRecords())

async function handleResetAllMastered() {
  try {
    await ElMessageBox.confirm(
      '确定重置全部「已掌握」标记？重置后所有错题将重新出现在列表中。',
      '重置全部已掌握',
      { confirmButtonText: '确认重置', cancelButtonText: '取消', type: 'warning' },
    )
    resetAllMastered()
    ElMessage.success('已重置全部掌握标记')
  } catch {
    /* cancelled */
  }
}

function goToCourses() {
  router.push(p('/courses/list'))
}
</script>

<template>
  <div class="chapter-list">
    <div class="toolbar">
      <div class="toolbar-left">
        <span class="toolbar-label">排序</span>
        <el-radio-group v-model="sortMode" size="small">
          <el-radio-button value="count">按错题数</el-radio-button>
          <el-radio-button value="name">按章节名称</el-radio-button>
        </el-radio-group>
      </div>
      <el-button
        v-if="showResetMastered"
        type="warning"
        plain
        size="small"
        @click="handleResetAllMastered"
      >
        <el-icon><RefreshLeft /></el-icon>
        重置全部已掌握
      </el-button>
    </div>

    <div v-if="!sortedSummaries.length" class="empty-container">
      <el-empty :description="data.totalWrong > 0 ? '暂无待复习错题，您已全部标记为掌握' : '暂无错题数据'">
        <template #image>
          <div class="empty-icon">
            <el-icon :size="80"><Reading /></el-icon>
          </div>
        </template>
        <el-button v-if="data.totalWrong > 0 && showResetMastered" plain @click="handleResetAllMastered">
          重置全部已掌握
        </el-button>
        <el-button type="primary" @click="goToCourses">去学习课程</el-button>
      </el-empty>
    </div>

    <div v-else class="chapter-cards">
      <div
        v-for="chapter in sortedSummaries"
        :key="chapter.chapterId"
        class="chapter-card"
        @click="openChapter(chapter.chapterId)"
      >
        <div class="chapter-card-icon">
          <el-icon :size="22"><Document /></el-icon>
        </div>
        <div class="chapter-card-main">
          <h3>{{ chapter.chapterTitle }}</h3>
          <p class="chapter-meta">
            <el-tag type="danger" size="small" effect="light">{{ chapter.wrongCount }} 道错题</el-tag>
            <span>上次练习 {{ formatDate(chapter.lastPracticeDate) }}</span>
          </p>
        </div>
        <el-icon class="chapter-arrow"><ArrowRight /></el-icon>
      </div>
    </div>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f2f5;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-label {
  font-size: 14px;
  color: #606266;
}

.empty-container {
  padding: 40px 0;
}

.empty-icon {
  color: #67c23a;
}

.chapter-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.chapter-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.chapter-card:hover {
  background: #fff;
  border-color: #409eff;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.1);
}

.chapter-card-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: #ecf5ff;
  color: #409eff;
  flex-shrink: 0;
}

.chapter-card-main {
  flex: 1;
  min-width: 0;
}

.chapter-card-main h3 {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

.chapter-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.chapter-arrow {
  color: #c0c4cc;
  font-size: 18px;
  flex-shrink: 0;
}

.chapter-card:hover .chapter-arrow {
  color: #409eff;
}
</style>
