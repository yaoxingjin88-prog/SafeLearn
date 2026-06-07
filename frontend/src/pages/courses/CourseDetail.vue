<template>
  <div class="sl-page course-detail">
    <el-page-header @back="$router.back()">
      <template #content>
        <span class="text-lg font-bold">{{ course.title || '课程详情' }}</span>
      </template>
    </el-page-header>

    <el-skeleton v-if="loading" animated :rows="6" class="mt-4" />

    <template v-else>
      <el-empty v-if="!course.id" description="未找到课程" class="mt-8" />

      <el-row v-else :gutter="24" class="mt-6">
        <!-- 左侧内容 -->
        <el-col :span="16">
          <el-card>
            <div class="course-header">
              <img :src="course.coverImage || '/images/default-course.svg'" class="course-cover" alt="cover" />
              <div class="course-info">
                <h1>{{ course.title }}</h1>
                <p>{{ course.description }}</p>
                <div class="course-meta">
                  <el-tag>{{ getCategoryName(course.category) }}</el-tag>
                  <span><el-icon><Clock /></el-icon> {{ course.totalDuration }}分钟</span>
                  <span><el-icon><Document /></el-icon> {{ course.chapters?.length || 0 }}章节</span>
                </div>
              </div>
            </div>
          </el-card>

          <!-- 阶梯式学习路径 -->
          <el-card class="mt-4">
            <template #header>
              <span class="font-bold">课程章节</span>
            </template>
            <div v-if="course.chapters?.length">
              <template v-for="(tier, tierIdx) in difficultyTiers" :key="tier.level">
                <div v-if="tier.chapters.length" class="difficulty-tier">
                  <div class="tier-header">
                    <el-tag :type="tier.tagType" effect="dark" size="small">{{ tier.label }}</el-tag>
                    <span class="tier-progress">{{ tier.completedCount }}/{{ tier.chapters.length }} 已完成</span>
                  </div>
                  <div v-if="tierIdx > 0" class="tier-connector"></div>
                  <div
                    v-for="(chapter, index) in tier.chapters"
                    :key="chapter.id"
                    class="chapter-item"
                    :class="{
                      completed: isChapterCompleted(chapter.id),
                      locked: chapter.unlocked === false
                    }"
                    @click="handleChapterClick(chapter)"
                  >
                    <div
                      class="chapter-index"
                      :style="chapter.unlocked === false ? {} : { background: tier.color + '20', color: tier.color }"
                    >
                      <el-icon v-if="chapter.unlocked === false"><Lock /></el-icon>
                      <template v-else-if="isChapterCompleted(chapter.id)">
                        <el-icon><Check /></el-icon>
                      </template>
                      <template v-else>{{ index + 1 }}</template>
                    </div>
                    <div class="chapter-info">
                      <div class="chapter-title">{{ chapter.title }}</div>
                      <div class="chapter-duration">{{ chapter.duration }}分钟</div>
                    </div>
                    <el-tag v-if="isChapterCompleted(chapter.id)" type="success" size="small" effect="plain">已完成</el-tag>
                  </div>
                </div>
              </template>
            </div>
            <el-empty v-else description="暂无章节" />
          </el-card>
        </el-col>

        <!-- 右侧信息 -->
        <el-col :span="8">
          <el-card>
            <template #header>
              <span class="font-bold">学习进度</span>
            </template>
            <div class="progress-container">
              <el-progress type="circle" :percentage="overallProgress" :status="overallProgress === 100 ? 'success' : ''" />
              <p class="mt-4 text-gray-600">
                已完成 {{ completedChapters }}/{{ course.chapters?.length || 0 }} 章节
              </p>
            </div>
            <el-button type="primary" class="w-full mt-4" @click="handleStartLearning">
              {{ overallProgress > 0 ? '继续学习' : '开始学习' }}
            </el-button>
            <el-alert
              v-if="overallProgress === 100"
              class="mt-3"
              type="success"
              show-icon
              title="已完成全部章节"
              description="可以返回课程列表或复习内容"
            />
          </el-card>

          <el-card class="mt-4">
            <template #header>
              <span class="font-bold">课程简介</span>
            </template>
            <p class="text-gray-600 text-sm leading-relaxed">
              {{ course.description }}
            </p>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Clock, Document, Check, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { courseApi } from '@/api/course'
import { useAppBase } from '@/composables/useAppBase'
import type { Course, Chapter } from '@/types'

const route = useRoute()
const router = useRouter()
const { p } = useAppBase()

const course = ref<Course>({
  id: '',
  title: '',
  description: '',
  coverImage: '',
  category: 'basic',
  chapters: [],
  totalDuration: 0,
  createdAt: '',
  updatedAt: '',
})

const completedChapters = ref(0)
const progressMap = ref<Record<string, boolean>>({})
const loading = ref(true)
const overallProgress = computed(() => {
  if (!course.value.chapters?.length) return 0
  return Math.round((completedChapters.value / course.value.chapters.length) * 100)
})

function isChapterCompleted(chapterId: string) {
  return !!progressMap.value[chapterId]
}

function getCategoryName(category: string) {
  const map: Record<string, string> = {
    basic: '基础',
    battery: '锂电池',
    thermal: '热失控',
    fire: '消防',
    bms: 'BMS',
  }
  return map[category] || category
}

const difficultyConfig: Record<number, { label: string; color: string; tagType: '' | 'success' | 'warning' | 'danger' }> = {
  1: { label: '基础理论', color: '#10b981', tagType: 'success' },
  2: { label: '案例分析', color: '#f59e0b', tagType: 'warning' },
  3: { label: '高级实操', color: '#ef4444', tagType: 'danger' },
}

const difficultyTiers = computed(() => {
  const groups: Record<number, Chapter[]> = { 1: [], 2: [], 3: [] }
  course.value.chapters?.forEach(ch => {
    const level = ch.difficultyLevel || 1
    if (!groups[level]) groups[level] = []
    groups[level].push(ch)
  })
  return [1, 2, 3].map(level => ({
    level,
    label: difficultyConfig[level]?.label || '未知',
    color: difficultyConfig[level]?.color || '#6b7280',
    tagType: difficultyConfig[level]?.tagType || '' as const,
    chapters: groups[level] || [],
    completedCount: (groups[level] || []).filter(ch => isChapterCompleted(ch.id)).length,
  })).filter(t => t.chapters.length > 0)
})

function handleChapterClick(chapter: Chapter) {
  if (chapter.unlocked === false) {
    ElMessage.warning('请先完成前置章节')
    return
  }
  router.push(p(`/courses/${course.value.id}/chapters/${chapter.id}`))
}

function handleStartLearning() {
  const nextChapter = course.value.chapters?.find(c => !isChapterCompleted(c.id) && c.unlocked !== false)
  if (nextChapter) {
    router.push(p(`/courses/${course.value.id}/chapters/${nextChapter.id}`))
  } else {
    ElMessage.info('所有可用章节已完成')
  }
}

onMounted(async () => {
  const id = route.params.id as string
  loading.value = true
  try {
    const [detailRes, progressRes] = await Promise.all([
      request.get(`/courses/${id}`),
      courseApi.getProgress(id).catch(() => ({ data: [] } as any)),
    ])
    course.value = detailRes.data
    progressMap.value = {}
    completedChapters.value = 0
    ;(progressRes.data || []).forEach((p: any) => {
      if (p.chapterId && p.completed) {
        progressMap.value[p.chapterId] = true
      }
    })
    completedChapters.value = Object.keys(progressMap.value).length
  } catch (error) {
    console.error('加载课程失败', error)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.course-header {
  display: flex;
  gap: 24px;
}

.course-cover {
  width: 300px;
  height: 180px;
  object-fit: cover;
  border-radius: 8px;
}

.course-info h1 {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 12px;
}

.course-info p {
  color: #6b7280;
  margin-bottom: 16px;
}

.course-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  color: #9ca3af;
}

.course-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.chapter-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: background 0.2s;
}

.chapter-item:hover {
  background: #f9fafb;
}

.chapter-item:last-child {
  border-bottom: none;
}

.chapter-index {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: #374151;
  margin-right: 16px;
}

.chapter-info {
  flex: 1;
}

.chapter-title {
  font-weight: 500;
  color: #1f2937;
}

.chapter-duration {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

.progress-container {
  text-align: center;
}

.difficulty-tier {
  margin-bottom: 8px;
}

.tier-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.tier-progress {
  font-size: 12px;
  color: #9ca3af;
}

.tier-connector {
  width: 2px;
  height: 16px;
  background: #e5e7eb;
  margin: 0 0 0 15px;
}

.chapter-item.locked {
  opacity: 0.45;
  cursor: not-allowed;
}

.chapter-item.locked:hover {
  background: transparent;
}
</style>
