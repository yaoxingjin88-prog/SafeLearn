<template>
  <div class="sl-page course-detail">
    <!-- 优化后的头部区域 -->
    <header class="course-header">
      <div class="header-left">
        <button class="back-btn" @click="backToCourseList">
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <div class="header-title-area">
          <h1 class="course-title">{{ course.title || '课程详情' }}</h1>
          <div class="header-meta">
            <FavoriteButton v-if="course.id" target-type="course" :target-id="course.id" />
            <span class="meta-divider" v-if="course.id"></span>
            <el-tag v-if="course.category" :type="getCategoryTagType(course.category)">
              {{ getCategoryName(course.category) }}
            </el-tag>
          </div>
        </div>
      </div>
      <div class="header-right">
        <el-button-group>
          <el-button @click="shareCourse">
            <el-icon><Share /></el-icon>
            分享
          </el-button>
          <el-button @click="printCourse">
            <el-icon><Printer /></el-icon>
            打印
          </el-button>
        </el-button-group>
      </div>
    </header>

    <el-skeleton v-if="loading" animated :rows="6" class="mt-4" />

    <template v-else>
      <el-empty v-if="!course.id" description="未找到课程" class="mt-8" />

      <div v-else class="course-layout">
        <!-- 左侧主内容 -->
        <main class="main-content">
          <!-- 课程封面和信息 -->
          <el-card class="course-info-card">
            <div class="course-hero">
              <div class="course-cover-wrapper">
                <img :src="course.coverImage || '/images/default-course.svg'" class="course-cover" alt="cover" />
                <div class="cover-overlay">
                  <el-button circle size="large" @click="startLearning">
                    <el-icon :size="32"><VideoPlay /></el-icon>
                  </el-button>
                </div>
              </div>
              <div class="course-details">
                <h2 class="course-name">{{ course.title }}</h2>
                <p class="course-desc">{{ course.description }}</p>
                <div class="course-stats">
                  <div class="stat-item">
                    <el-icon><Clock /></el-icon>
                    <span>{{ course.totalDuration }}分钟</span>
                  </div>
                  <div class="stat-item">
                    <el-icon><Document /></el-icon>
                    <span>{{ course.chapters?.length || 0 }}章节</span>
                  </div>
                  <div class="stat-item">
                    <el-icon><User /></el-icon>
                    <span>{{ course.learnerCount || 0 }}人学习</span>
                  </div>
                </div>
                <div class="course-actions">
                  <el-button type="primary" size="large" @click="startLearning">
                    {{ overallProgress > 0 ? '继续学习' : '开始学习' }}
                  </el-button>
                  <el-button size="large" @click="showShareDialog = true">
                    <el-icon><Share /></el-icon>
                    分享课程
                  </el-button>
                </div>
              </div>
            </div>
          </el-card>

          <!-- 课程章节 - 阶梯式学习路径 -->
          <el-card class="chapters-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">课程章节</span>
                <div class="chapter-filters">
                  <el-radio-group v-model="chapterFilter" size="small">
                    <el-radio-button value="all">全部</el-radio-button>
                    <el-radio-button value="completed">已完成</el-radio-button>
                    <el-radio-button value="unlocked">可学习</el-radio-button>
                  </el-radio-group>
                </div>
              </div>
            </template>

            <div v-if="filteredChapters.length" class="chapters-list">
              <template v-for="(tier, tierIdx) in difficultyTiers" :key="tier.level">
                <div v-if="tier.chapters.length" class="difficulty-tier">
                  <div class="tier-header">
                    <div class="tier-badge" :style="{ background: tier.color + '20', color: tier.color }">
                      <el-tag :type="tier.tagType" effect="dark" size="small">{{ tier.label }}</el-tag>
                      <span class="tier-count">{{ tier.chapters.length }} 章节</span>
                    </div>
                    <div class="tier-progress">
                      <span class="progress-text">{{ tier.completedCount }}/{{ tier.chapters.length }} 已完成</span>
                      <el-progress
                        :percentage="Math.round(tier.completedCount / tier.chapters.length * 100)"
                        :show-text="false"
                        :stroke-width="4"
                        :color="tier.color"
                      />
                    </div>
                  </div>
                  <div v-if="tierIdx > 0" class="tier-connector"></div>
                  <div
                    v-for="(chapter, index) in tier.chapters"
                    :key="chapter.id"
                    class="chapter-item"
                    :class="{
                      completed: isChapterCompleted(chapter.id),
                      locked: chapter.unlocked === false,
                      active: isCurrentChapter(chapter.id)
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
                      <div class="chapter-meta">
                        <span class="chapter-duration">
                          <el-icon><Clock /></el-icon>
                          {{ chapter.duration }}分钟
                        </span>
                        <el-tag
                          v-if="chapter.difficultyLevel"
                          :type="getDifficultyTagType(chapter.difficultyLevel)"
                          size="small"
                          effect="plain"
                        >
                          {{ getDifficultyLabel(chapter.difficultyLevel) }}
                        </el-tag>
                      </div>
                    </div>
                    <div class="chapter-status">
                      <el-tag v-if="isChapterCompleted(chapter.id)" type="success" size="small" effect="plain">
                        已完成
                      </el-tag>
                      <el-tag v-else-if="chapter.unlocked === false" type="info" size="small" effect="plain">
                        未解锁
                      </el-tag>
                      <el-button
                        v-else
                        type="primary"
                        size="small"
                        @click.stop="startChapter(chapter)"
                      >
                        开始学习
                      </el-button>
                    </div>
                  </div>
                </div>
              </template>
            </div>
            <el-empty v-else description="暂无章节" />
          </el-card>

          <!-- 学员评价 -->
          <el-card class="reviews-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">学员评价</span>
                <span v-if="reviewSummary.total" class="review-avg">
                  {{ reviewSummary.averageRating.toFixed(1) }} 分 · {{ reviewSummary.total }} 条
                </span>
              </div>
            </template>
            <div class="review-form">
              <el-rate v-model="reviewForm.rating" />
              <el-input
                v-model="reviewForm.content"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="分享你的学习体验..."
              />
              <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">
                提交评价
              </el-button>
            </div>
            <div v-if="reviewSummary.items.length" class="reviews-list mt-4">
              <div v-for="review in reviewSummary.items" :key="review.id" class="review-item">
                <div class="review-header">
                  <div class="reviewer-info">
                    <div class="reviewer-details">
                      <span class="reviewer-name">{{ review.displayName || review.username || '学员' }}</span>
                      <span class="review-date">{{ formatReviewDate(review.createdAt) }}</span>
                    </div>
                  </div>
                  <el-rate :model-value="review.rating" disabled />
                </div>
                <p class="review-content">{{ review.content }}</p>
              </div>
            </div>
            <el-empty v-else description="暂无评价，来做第一个评价的人吧" :image-size="60" />
          </el-card>
        </main>

        <!-- 右侧信息栏 -->
        <aside class="sidebar">
          <!-- 学习进度卡片 -->
          <el-card class="progress-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">学习进度</span>
                <el-button link type="primary" @click="viewProgressDetail">
                  详情
                </el-button>
              </div>
            </template>
            <div class="progress-container">
              <el-progress
                type="circle"
                :percentage="overallProgress"
                :status="overallProgress === 100 ? 'success' : ''"
                :width="120"
                :stroke-width="8"
              />
              <div class="progress-stats">
                <div class="stat-row">
                  <span class="stat-label">已完成章节</span>
                  <span class="stat-value">{{ completedChapters }}/{{ course.chapters?.length || 0 }}</span>
                </div>
                <div class="stat-row">
                  <span class="stat-label">学习时长</span>
                  <span class="stat-value">{{ studyHours }}小时</span>
                </div>
                <div class="stat-row">
                  <span class="stat-label">掌握度</span>
                  <span class="stat-value">{{ masteryLevel }}%</span>
                </div>
              </div>
            </div>
            <el-button type="primary" class="w-full mt-4" size="large" @click="startLearning">
              {{ overallProgress > 0 ? '继续学习' : '开始学习' }}
            </el-button>
            <el-alert
              v-if="overallProgress === 100"
              class="completion-alert"
              type="success"
              show-icon
              :closable="false"
            >
              <template #title>
                <div class="alert-content">
                  <span class="alert-title">🎉 恭喜完成课程！</span>
                  <span class="alert-desc">可以申请证书或复习内容</span>
                </div>
              </template>
            </el-alert>
          </el-card>

          <!-- 综合考试 -->
          <el-card v-if="examStatus.available" class="comprehensive-exam-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">综合考试</span>
                <el-tag v-if="examStatus.examPassed" type="success" size="small">已通过</el-tag>
              </div>
            </template>
            <div class="exam-info">
              <p class="exam-desc">从各章节题库随机抽题，检验跨章节综合能力</p>
              <div class="exam-meta">
                <span>{{ examStatus.drawCount }} 题 / {{ examStatus.timeLimit }} 分钟</span>
                <span>及格 {{ examStatus.passScore }} 分</span>
              </div>
              <div v-if="examStatus.bestScore != null" class="exam-best">
                历史最高：{{ examStatus.bestScore }} 分
              </div>
              <el-alert
                v-if="!examStatus.eligible && examStatus.eligibilityHint"
                :title="examStatus.eligibilityHint"
                type="info"
                :closable="false"
                show-icon
                class="exam-hint"
              />
              <el-button
                type="warning"
                class="w-full"
                size="large"
                :disabled="!examStatus.eligible || examStatus.examPassed"
                @click="goToComprehensiveExam"
              >
                {{ examStatus.examPassed ? '已通过综合考试' : '参加综合考试' }}
              </el-button>
            </div>
          </el-card>

          <!-- 课程简介卡片 -->
          <el-card class="description-card">
            <template #header>
              <span class="card-title">课程简介</span>
            </template>
            <div class="description-content">
              <p class="description-text">{{ course.description }}</p>
              <div class="description-tags">
                <el-tag v-for="tag in courseTags" :key="tag" size="small" effect="plain">
                  {{ tag }}
                </el-tag>
              </div>
            </div>
          </el-card>

          <!-- 相关课程推荐 -->
          <el-card class="related-card">
            <template #header>
              <span class="card-title">相关课程</span>
            </template>
            <div v-if="relatedCourses.length" class="related-list">
              <div
                v-for="related in relatedCourses"
                :key="related.id"
                class="related-item"
                @click="switchCourse(related.id)"
              >
                <div class="related-cover">
                  <img :src="related.coverImage || '/images/default-course.svg'" alt="cover" />
                </div>
                <div class="related-info">
                  <div class="related-title">{{ related.title }}</div>
                  <div class="related-meta">
                    <span>{{ related.chapters?.length || 0 }}章节</span>
                    <span>{{ related.learnerCount || 0 }}人学习</span>
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无推荐" :image-size="60" />
          </el-card>
        </aside>
      </div>
    </template>

    <!-- 分享对话框 -->
    <el-dialog v-model="showShareDialog" title="分享课程" width="400px" @opened="renderShareQr">
      <div class="share-content">
        <div class="share-qr-wrap">
          <canvas ref="shareQrCanvas" class="share-qr" />
          <p class="share-qr-tip">扫码打开课程</p>
        </div>
        <p>或复制链接分享给同事：</p>
        <div class="share-link">
          <el-input v-model="shareLink" readonly>
            <template #append>
              <el-button @click="copyShareLink">复制链接</el-button>
            </template>
          </el-input>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import QRCode from 'qrcode'
import {
  Clock, Document, User, Check, Lock, VideoPlay, Share, Printer, ArrowLeft
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { courseApi } from '@/api/course'
import { dashboardApi } from '@/api/dashboard'
import { getComprehensiveExamStatus, type ComprehensiveExamStatus } from '@/api/quiz'
import { useAppBase } from '@/composables/useAppBase'
import { useCourseNavigation } from '@/composables/useCourseNavigation'
import FavoriteButton from '@/components/learning/FavoriteButton.vue'
import type { Course, Chapter, CourseReviewSummary } from '@/types'

const route = useRoute()
const router = useRouter()
const { p } = useAppBase()
const { backToCourseList, switchCourse, goToChapter } = useCourseNavigation()

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
const chapterFilter = ref('all')
const showShareDialog = ref(false)
const shareLink = ref('')
const shareQrCanvas = ref<HTMLCanvasElement>()
const studyHours = ref(0)
const masteryLevel = ref(0)
const relatedCourses = ref<any[]>([])
const courseTags = ref<string[]>([])
const reviewSummary = ref<CourseReviewSummary>({ items: [], total: 0, averageRating: 0 })
const reviewForm = ref({ rating: 5, content: '' })
const reviewSubmitting = ref(false)
const examStatus = ref<ComprehensiveExamStatus>({
  available: false,
  eligible: false,
  questionPoolSize: 0,
  drawCount: 0,
  passScore: 70,
  timeLimit: 45,
  completedChapterCount: 0,
  totalChapterCount: 0,
  requiredCompletionRatio: 0.6,
})

const overallProgress = computed(() => {
  if (!course.value.chapters?.length) return 0
  return Math.round((completedChapters.value / course.value.chapters.length) * 100)
})

const filteredChapters = computed(() => {
  if (!course.value.chapters) return []
  switch (chapterFilter.value) {
    case 'completed':
      return course.value.chapters.filter(ch => isChapterCompleted(ch.id))
    case 'unlocked':
      return course.value.chapters.filter(ch => ch.unlocked !== false)
    default:
      return course.value.chapters
  }
})

function isChapterCompleted(chapterId: string) {
  return !!progressMap.value[chapterId]
}

function isCurrentChapter(_chapterId: string) {
  // 这里可以添加逻辑判断当前正在学习的章节
  return false
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

function getCategoryTagType(category: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    basic: '',
    battery: 'success',
    thermal: 'danger',
    fire: 'warning',
    bms: 'info',
  }
  return map[category] || ''
}

const difficultyConfig: Record<number, { label: string; color: string; tagType: '' | 'success' | 'warning' | 'danger' }> = {
  1: { label: '基础理论', color: '#10b981', tagType: 'success' },
  2: { label: '案例分析', color: '#f59e0b', tagType: 'warning' },
  3: { label: '高级实操', color: '#ef4444', tagType: 'danger' },
}

const difficultyTiers = computed(() => {
  const groups: Record<number, Chapter[]> = { 1: [], 2: [], 3: [] }
  filteredChapters.value.forEach(ch => {
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

function getDifficultyLabel(level?: number) {
  const map: Record<number, string> = { 1: '基础', 2: '案例', 3: '实操' }
  return map[level || 1] || '基础'
}

function getDifficultyTagType(level?: number): '' | 'success' | 'warning' | 'danger' {
  const map: Record<number, '' | 'success' | 'warning' | 'danger'> = { 1: 'success', 2: 'warning', 3: 'danger' }
  return map[level || 1] || ''
}

function handleChapterClick(chapter: Chapter) {
  if (chapter.unlocked === false) {
    ElMessage.warning('请先完成前置章节')
    return
  }
  goToChapter(course.value.id, chapter.id)
}

function startLearning() {
  const nextChapter = course.value.chapters?.find(c => !isChapterCompleted(c.id) && c.unlocked !== false)
  if (nextChapter) {
    goToChapter(course.value.id, nextChapter.id)
  } else {
    ElMessage.info('所有可用章节已完成')
  }
}

function startChapter(chapter: Chapter) {
  goToChapter(course.value.id, chapter.id)
}

function shareCourse() {
  showShareDialog.value = true
  shareLink.value = `${window.location.origin}/user/courses/${course.value.id}`
}

async function renderShareQr() {
  await nextTick()
  if (!shareQrCanvas.value || !shareLink.value) return
  try {
    await QRCode.toCanvas(shareQrCanvas.value, shareLink.value, { width: 180, margin: 1 })
  } catch {
    // 二维码生成失败时不阻断分享
  }
}

function copyShareLink() {
  navigator.clipboard.writeText(shareLink.value)
  ElMessage.success('链接已复制')
}

function formatReviewDate(value?: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

async function submitReview() {
  if (!course.value.id) return
  if (!reviewForm.value.content.trim()) {
    ElMessage.warning('请填写评价内容')
    return
  }
  reviewSubmitting.value = true
  try {
    await courseApi.submitReview(course.value.id, {
      rating: reviewForm.value.rating,
      content: reviewForm.value.content.trim(),
    })
    ElMessage.success('评价已提交')
    reviewForm.value.content = ''
    const res = await courseApi.getReviews(course.value.id)
    reviewSummary.value = res.data
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '提交失败')
  } finally {
    reviewSubmitting.value = false
  }
}

function printCourse() {
  window.print()
}

function viewProgressDetail() {
  router.push(p('/courses/my-learning'))
}

function goToComprehensiveExam() {
  if (!examStatus.value.eligible || examStatus.value.examPassed) return
  router.push(p(`/courses/${course.value.id}/comprehensive-exam`))
}

onMounted(async () => {
  const id = route.params.id as string
  loading.value = true
  try {
    const [detailRes, progressRes, examRes, relatedRes, overviewRes, reviewsRes] = await Promise.all([
      courseApi.getById(id),
      courseApi.getProgress(id).catch(() => ({ data: [] } as any)),
      getComprehensiveExamStatus(id).catch(() => ({ data: null } as any)),
      courseApi.getRelated(id).catch(() => ({ data: [] } as any)),
      dashboardApi.getOverview().catch(() => ({ data: {} } as any)),
      courseApi.getReviews(id).catch(() => ({ data: { items: [], total: 0, averageRating: 0 } } as any)),
    ])
    course.value = detailRes.data
    reviewSummary.value = reviewsRes.data
    if (examRes.data) {
      examStatus.value = examRes.data
    }
    progressMap.value = {}
    completedChapters.value = 0
    let totalMastery = 0
    let masteryCount = 0
    ;(progressRes.data || []).forEach((p: any) => {
      if (p.chapterId && p.completed) {
        progressMap.value[p.chapterId] = true
      }
      if (p.masteryLevel) {
        totalMastery += p.masteryLevel
        masteryCount += 1
      }
    })
    completedChapters.value = Object.keys(progressMap.value).length
    studyHours.value = overviewRes.data?.studyHours || Math.round(completedChapters.value * 0.5 * 10) / 10
    masteryLevel.value = masteryCount > 0 ? Math.round(totalMastery / masteryCount) : overallProgress.value
    courseTags.value = course.value.category ? [getCategoryName(course.value.category)] : []
    relatedCourses.value = relatedRes.data || []
  } catch (error) {
    console.error('加载课程失败', error)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
/* 头部区域优化 */
.course-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.header-title-area {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.course-title {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.meta-divider {
  width: 1px;
  height: 16px;
  background: #e5e7eb;
}

/* 主布局优化 */
.course-layout {
  display: flex;
  gap: 24px;
  min-height: calc(100vh - 200px);
}

/* 主内容区优化 */
.main-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 课程信息卡片优化 */
.course-info-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
}

.course-hero {
  display: flex;
  gap: 24px;
}

.course-cover-wrapper {
  position: relative;
  width: 320px;
  height: 200px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
}

.course-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.course-cover-wrapper:hover .cover-overlay {
  opacity: 1;
}

.course-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.course-name {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.course-desc {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.6;
  margin: 0;
}

.course-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #9ca3af;
}

.course-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

/* 章节卡片优化 */
.chapters-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.chapter-filters {
  display: flex;
  gap: 8px;
}

.chapters-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.difficulty-tier {
  margin-bottom: 16px;
}

.tier-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
  margin-bottom: 8px;
}

.tier-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  border-radius: 20px;
}

.tier-count {
  font-size: 12px;
  color: #6b7280;
}

.tier-progress {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-text {
  font-size: 12px;
  color: #9ca3af;
}

.tier-connector {
  width: 2px;
  height: 16px;
  background: #e5e7eb;
  margin: 0 0 0 15px;
}

.chapter-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
  border: 1px solid #f3f4f6;
}

.chapter-item:hover {
  background: #f9fafb;
  border-color: #e5e7eb;
}

.chapter-item.completed {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.chapter-item.active {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.chapter-item.locked {
  opacity: 0.45;
  cursor: not-allowed;
}

.chapter-item.locked:hover {
  background: #fff;
  border-color: #f3f4f6;
}

.chapter-index {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: #374151;
  margin-right: 16px;
  flex-shrink: 0;
}

.chapter-info {
  flex: 1;
  min-width: 0;
}

.chapter-title {
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 4px;
}

.chapter-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chapter-duration {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #9ca3af;
}

.chapter-status {
  flex-shrink: 0;
}

/* 评价卡片优化 */
.reviews-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  padding: 16px;
  background: #f9fafb;
  border-radius: 10px;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.reviewer-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.reviewer-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.reviewer-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
}

.review-date {
  font-size: 12px;
  color: #9ca3af;
}

.review-content {
  font-size: 14px;
  color: #374151;
  line-height: 1.6;
  margin: 0;
}

/* 侧边栏优化 */
.sidebar {
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex-shrink: 0;
}

.progress-card,
.comprehensive-exam-card,
.description-card,
.related-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
}

.exam-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.exam-desc {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}

.exam-meta {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #909399;
}

.exam-best {
  font-size: 13px;
  color: #e6a23c;
}

.exam-hint {
  margin-bottom: 4px;
}

.progress-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.progress-stats {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f3f4f6;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
}

.stat-value {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
}

.completion-alert {
  margin-top: 12px;
  border-radius: 8px;
}

.alert-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.alert-title {
  font-weight: 600;
  color: #111827;
}

.alert-desc {
  font-size: 12px;
  color: #6b7280;
}

.description-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.description-text {
  font-size: 14px;
  color: #374151;
  line-height: 1.6;
  margin: 0;
}

.description-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.related-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.related-item {
  display: flex;
  gap: 12px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.related-item:hover {
  background: #f9fafb;
}

.related-cover {
  width: 60px;
  height: 40px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}

.related-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.related-info {
  flex: 1;
  min-width: 0;
}

.related-title {
  font-size: 13px;
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.related-meta {
  display: flex;
  gap: 8px;
  font-size: 11px;
  color: #9ca3af;
}

/* 分享对话框优化 */
.share-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  text-align: center;
}

.share-qr-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.share-qr {
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.share-qr-tip {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
}

.review-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-avg {
  font-size: 13px;
  color: #6b7280;
}

.share-link {
  width: 100%;
  margin-top: 4px;
}

/* 响应式优化 */
@media (max-width: 1200px) {
  .course-layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .progress-card,
  .description-card,
  .related-card {
    flex: 1;
    min-width: 300px;
  }

  .course-hero {
    flex-direction: column;
  }

  .course-cover-wrapper {
    width: 100%;
    height: 200px;
  }
}

@media (max-width: 768px) {
  .course-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .header-right {
    width: 100%;
  }

  .sidebar {
    flex-direction: column;
  }

  .progress-card,
  .description-card,
  .related-card {
    min-width: 100%;
  }
}
</style>
