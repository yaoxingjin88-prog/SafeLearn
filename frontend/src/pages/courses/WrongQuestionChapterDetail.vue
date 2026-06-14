<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight } from '@element-plus/icons-vue'
import WrongQuestionCard from '@/components/courses/WrongQuestionCard.vue'
import type { WrongQuestionItem, DetailViewMode } from './wrongQuestionsTypes'
import { useWrongQuestionMastered } from '@/composables/useWrongQuestionMastered'
import { useAppBase } from '@/composables/useAppBase'

const props = defineProps<{
  chapterId: string
  questions: WrongQuestionItem[]
}>()

const router = useRouter()
const { p } = useAppBase()
const { hideMastered, isMastered, markMastered, clearChapter } = useWrongQuestionMastered()

const viewMode = ref<DetailViewMode>('time')

const chapterTitle = computed(() => props.questions[0]?.chapterTitle || '未知章节')
const courseId = computed(() => props.questions[0]?.courseId)

const visibleQuestions = computed(() => {
  let list = [...props.questions]
  if (hideMastered.value) {
    list = list.filter(q => !isMastered(props.chapterId, q.questionId))
  }
  if (viewMode.value === 'time') {
    return list.sort((a, b) => (b.attemptDate || '').localeCompare(a.attemptDate || ''))
  }
  return list
})

const groupedByType = computed(() => {
  const order = ['single', 'multiple', 'truefalse'] as const
  const labels: Record<string, string> = {
    single: '单选题', multiple: '多选题', truefalse: '判断题',
  }
  const groups: Array<{ type: string; label: string; items: WrongQuestionItem[] }> = []
  for (const type of order) {
    const items = visibleQuestions.value
      .filter(q => q.type === type)
      .sort((a, b) => (b.attemptDate || '').localeCompare(a.attemptDate || ''))
    if (items.length) groups.push({ type, label: labels[type], items })
  }
  const other = visibleQuestions.value.filter(q => !order.includes(q.type as typeof order[number]))
  if (other.length) groups.push({ type: 'other', label: '其他', items: other })
  return groups
})

const activeCount = computed(() =>
  props.questions.filter(q => !isMastered(props.chapterId, q.questionId)).length,
)

function handleMastered(questionId: string) {
  markMastered(props.chapterId, questionId)
  ElMessage.success('已标记为掌握')
}

async function handleClearChapter() {
  try {
    await ElMessageBox.confirm(
      '确定清空本章全部错题？清空后可在重新测验时再次产生。',
      '清空本章错题',
      { confirmButtonText: '确认清空', cancelButtonText: '取消', type: 'warning' },
    )
    clearChapter(props.chapterId, props.questions.map(q => q.questionId))
    ElMessage.success('本章错题已清空')
  } catch {
    /* cancelled */
  }
}

function retryQuiz() {
  if (courseId.value) {
    router.push(p(`/courses/${courseId.value}/chapters/${props.chapterId}/quiz`))
  } else {
    router.push(p('/courses/list'))
    ElMessage.info('请从课程列表进入对应章节重新测验')
  }
}

function goToCourses() {
  router.push(p('/courses/list'))
}
</script>

<template>
  <div class="chapter-detail">
    <div class="detail-toolbar">
      <div class="toolbar-left">
        <el-switch v-model="hideMastered" active-text="隐藏已掌握" />
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="time">按时间</el-radio-button>
          <el-radio-button value="type">按题型</el-radio-button>
        </el-radio-group>
      </div>
      <el-button type="danger" plain size="small" @click="handleClearChapter">
        清空本章
      </el-button>
    </div>

    <div v-if="activeCount === 0" class="empty-container">
      <el-empty description="暂无错题，快去练习吧~">
        <el-button type="primary" @click="retryQuiz">重新测验本章</el-button>
      </el-empty>
    </div>

    <div v-else-if="!visibleQuestions.length" class="empty-container">
      <el-empty description="已掌握全部错题，可关闭「隐藏已掌握」查看">
        <el-button @click="hideMastered = false">显示全部</el-button>
      </el-empty>
    </div>

    <template v-else-if="viewMode === 'time'">
      <div class="questions-list">
        <WrongQuestionCard
          v-for="q in visibleQuestions"
          :key="q.questionId"
          :question="q"
          show-master-action
          @mastered="handleMastered(q.questionId)"
        />
      </div>
    </template>

    <template v-else>
      <div v-for="group in groupedByType" :key="group.type" class="type-group">
        <div class="type-group-header">
          <span>{{ group.label }}</span>
          <el-tag size="small" type="info">{{ group.items.length }} 道</el-tag>
        </div>
        <div class="questions-list">
          <WrongQuestionCard
            v-for="q in group.items"
            :key="q.questionId"
            :question="q"
            show-master-action
            @mastered="handleMastered(q.questionId)"
          />
        </div>
      </div>
    </template>

    <div v-if="activeCount > 0" class="bottom-actions">
      <el-button type="primary" @click="retryQuiz">
        <el-icon><RefreshRight /></el-icon>
        重新测验本章
      </el-button>
      <el-button plain @click="goToCourses">继续学习</el-button>
    </div>
  </div>
</template>

<style scoped>
.detail-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 20px;
  padding: 14px 16px;
  background: #f8fafc;
  border: 1px solid #eef2f6;
  border-radius: 8px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.empty-container {
  padding: 40px 0;
}

.type-group {
  margin-bottom: 28px;
}

.type-group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.bottom-actions {
  display: flex;
  justify-content: flex-start;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f2f5;
}
</style>
