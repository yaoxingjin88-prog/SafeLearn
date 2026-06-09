<template>
  <div class="sl-page chapter-view">
    <!-- 优化后的头部区域 -->
    <header class="chapter-header">
      <div class="header-left">
        <button class="back-btn" @click="backToCourseDetail(route.params.courseId as string)">
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <div class="header-title-area">
          <h1 class="chapter-title">{{ chapter.title || '章节学习' }}</h1>
          <div class="header-meta">
            <FavoriteButton
              v-if="route.params.courseId"
              target-type="course"
              :target-id="route.params.courseId as string"
            />
            <span class="meta-divider" v-if="route.params.courseId"></span>
            <span class="learning-streak" v-if="learningStreak > 0">
              <el-icon><Sunny /></el-icon>
              连续学习 {{ learningStreak }} 天
            </span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <el-input
          v-model="searchText"
          placeholder="搜索课程内容..."
          prefix-icon="Search"
          class="search-input"
          clearable
        />
        <el-badge :value="notifications" :hidden="notifications === 0" class="notification-badge">
          <el-button circle>
            <el-icon><Bell /></el-icon>
          </el-button>
        </el-badge>
      </div>
      <!-- 阅读进度条 -->
      <div class="reading-progress" :style="{ width: readProgress + '%' }"></div>
    </header>

    <el-skeleton v-if="loading" animated :rows="6" class="mt-4" />

    <template v-else>
      <el-empty v-if="!chapter.id" description="未找到章节" class="mt-8" />

      <div v-else class="chapter-layout">
        <!-- 左侧边栏 - 课程目录 -->
        <aside class="sidebar">
          <el-card class="sidebar-card toc-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">课程目录</span>
                <span class="chapter-count">{{ chapters.length }} 章节</span>
              </div>
            </template>
            <div v-if="chapters.length" class="toc-list">
              <div
                v-for="(item, index) in chapters"
                :key="item.id"
                class="toc-item"
                :class="{
                  active: item.id === chapter.id,
                  completed: completed[item.id],
                  locked: item.unlocked === false
                }"
                @click="handleTocClick(item)"
              >
                <div class="toc-indicator">
                  <div class="indicator-line" :class="{ completed: completed[item.id] }"></div>
                  <div class="indicator-dot" :class="{
                    active: item.id === chapter.id,
                    completed: completed[item.id],
                    locked: item.unlocked === false
                  }">
                    <el-icon v-if="item.unlocked === false"><Lock /></el-icon>
                    <el-icon v-else-if="completed[item.id]"><Check /></el-icon>
                    <span v-else>{{ index + 1 }}</span>
                  </div>
                </div>
                <div class="toc-content">
                  <div class="toc-title">{{ item.title }}</div>
                  <div class="toc-meta">
                    <el-tag
                      v-if="item.difficultyLevel"
                      :type="getDifficultyTagType(item.difficultyLevel)"
                      size="small"
                      effect="plain"
                    >
                      {{ getDifficultyLabel(item.difficultyLevel) }}
                    </el-tag>
                    <span v-if="completed[item.id]" class="completion-badge">
                      <el-icon><CircleCheck /></el-icon>
                      已完成
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无章节" />
          </el-card>

          <!-- 学习笔记卡片 -->
          <NotePanel
            class="sidebar-card note-card"
            target-type="chapter"
            :target-id="route.params.chapterId as string"
            :course-id="route.params.courseId as string"
            :show-all="showAllNotes"
          />

          <!-- 操作按钮卡片 -->
          <el-card class="sidebar-card action-card">
            <div v-if="isCurrentChapterCompleted" class="completed-status">
              <div class="completed-banner">
                <el-icon class="completed-icon"><CircleCheck /></el-icon>
                <div class="completed-text">
                  <span class="completed-title">已完成本章学习</span>
                  <span v-if="masteryLevel > 0" class="completed-meta">掌握度 {{ masteryLevel }}%</span>
                  <span v-else-if="quizBestScore > 0" class="completed-meta">测验得分 {{ quizBestScore }} 分</span>
                </div>
              </div>
              <el-button
                class="action-btn secondary-btn"
                @click="handleNext"
                :disabled="!hasNext"
              >
                <el-icon><ArrowRight /></el-icon>
                下一章
              </el-button>
            </div>
            <div v-else class="action-buttons">
              <el-button
                v-if="hasQuiz"
                type="primary"
                class="action-btn primary-cta"
                @click="goToQuiz"
              >
                <el-icon><EditPen /></el-icon>
                参加测验
              </el-button>
              <el-button
                v-if="!hasQuiz"
                type="primary"
                class="action-btn primary-cta"
                @click="handleComplete"
              >
                <el-icon><CircleCheck /></el-icon>
                完成本章
              </el-button>
              <el-button
                v-else
                class="action-btn secondary-btn"
                @click="handleComplete"
              >
                <el-icon><CircleCheck /></el-icon>
                直接完成
              </el-button>
              <el-button
                class="action-btn secondary-btn"
                @click="handleNext"
                :disabled="!hasNext"
              >
                <el-icon><ArrowRight /></el-icon>
                下一章
              </el-button>
            </div>
            <el-alert
              v-if="!hasNext && completed[chapter.id]"
              class="completion-alert"
              type="success"
              show-icon
              :closable="false"
            >
              <template #title>
                <div class="alert-content">
                  <span class="alert-title">🎉 恭喜完成所有章节！</span>
                  <span class="alert-desc">可以返回课程详情查看整体进度</span>
                </div>
              </template>
            </el-alert>
          </el-card>
        </aside>

        <!-- 主内容区 -->
        <main class="main-content">
          <el-card class="content-card">
            <!-- 视频播放器 -->
            <div v-if="chapter.videoUrl" class="video-container">
              <video
                ref="videoRef"
                :src="chapter.videoUrl"
                controls
                class="w-full"
              />
              <div class="video-overlay" v-if="!videoPlaying">
                <el-button circle size="large" @click="playVideo">
                  <el-icon :size="32"><VideoPlay /></el-icon>
                </el-button>
              </div>
            </div>

            <!-- 本章关键词 -->
            <div v-if="keywords.length" class="keyword-bar">
              <span class="keyword-label">
                <el-icon><PriceTag /></el-icon>
                本章关键词
              </span>
              <div class="keyword-chips">
                <span v-for="kw in keywords" :key="kw" class="keyword-chip">{{ kw }}</span>
              </div>
            </div>

            <!-- 图文内容 - 优化排版 -->
            <div ref="contentRef" class="content-area" v-html="chapter.content" />

            <!-- 章节导航 -->
            <div class="chapter-navigation">
              <el-button
                v-if="currentIndex > 0"
                class="nav-btn prev-btn"
                @click="switchChapter(chapters[currentIndex - 1].id)"
              >
                <el-icon><ArrowLeft /></el-icon>
                上一章
              </el-button>
              <div class="nav-spacer"></div>
              <el-button
                v-if="hasNext"
                type="primary"
                class="nav-btn next-btn"
                @click="handleNext"
              >
                下一章
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </el-card>

          <!-- 学习进度提示 -->
          <div class="progress-hint" v-if="chapterProgress > 0 && chapterProgress < 100">
            <div class="hint-icon">
              <el-icon><InfoFilled /></el-icon>
            </div>
            <div class="hint-content">
              <div class="hint-title">学习进度 {{ chapterProgress }}%</div>
              <div class="hint-desc">继续学习以完成本章节</div>
            </div>
            <el-progress
              :percentage="chapterProgress"
              :show-text="false"
              :stroke-width="6"
              class="hint-progress"
            />
          </div>
        </main>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Lock, EditPen, ArrowLeft, ArrowRight, Check, CircleCheck,
  VideoPlay, InfoFilled, Bell, Sunny, PriceTag
} from '@element-plus/icons-vue'
import request from '@/api/request'
import { courseApi } from '@/api/course'
import { checkQuizExists } from '@/api/quiz'
import { useAppBase } from '@/composables/useAppBase'
import { useCourseNavigation } from '@/composables/useCourseNavigation'
import NotePanel from '@/components/learning/NotePanel.vue'
import FavoriteButton from '@/components/learning/FavoriteButton.vue'

const route = useRoute()
const router = useRouter()
const { p } = useAppBase()
const { backToCourseDetail, switchChapter: navSwitchChapter } = useCourseNavigation()

const loading = ref(true)
const chapter = ref({ id: '', title: '', content: '', videoUrl: '' })
const chapters = ref<{ id: string; title: string; difficultyLevel?: number; unlocked?: boolean }[]>([])
const completed = ref<Record<string, boolean>>({})
const hasQuiz = ref(false)
const quizPassed = ref(false)
const quizBestScore = ref(0)
const masteryLevel = ref(0)
const searchText = ref('')
const notifications = ref(3)
const learningStreak = ref(5)
const showAllNotes = ref(false)
const videoPlaying = ref(false)
const chapterProgress = ref(0)
const readProgress = ref(0)
const contentRef = ref<HTMLElement | null>(null)
const keywords = ref<string[]>([])

// 从内容中的 <strong> 标签自动提取关键词
function extractKeywords(html: string) {
  const matches = html.match(/<strong>(.*?)<\/strong>/g) || []
  const terms = matches
    .map(m => m.replace(/<\/?strong>/g, '').replace(/[：:，,。]/g, '').trim())
    .filter(t => t.length >= 2 && t.length <= 12 && !/^[0-9.\s]+$/.test(t))
  return Array.from(new Set(terms)).slice(0, 8)
}

// 基于页面滚动计算阅读进度
function handleScroll() {
  const el = contentRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const total = el.offsetHeight - window.innerHeight
  if (total <= 0) {
    readProgress.value = 100
    return
  }
  const scrolled = Math.min(Math.max(-rect.top, 0), total)
  readProgress.value = Math.round((scrolled / total) * 100)
}

const currentIndex = computed(() => chapters.value.findIndex(c => c.id === chapter.value.id))
const hasNext = computed(() => currentIndex.value >= 0 && currentIndex.value < chapters.value.length - 1)
const isCurrentChapterCompleted = computed(() => !!completed.value[chapter.value.id])

function switchChapter(id: string) {
  navSwitchChapter(route.params.courseId as string, id)
}

function handleTocClick(item: { id: string; unlocked?: boolean }) {
  if (item.unlocked === false) {
    ElMessage.warning('请先完成前置章节')
    return
  }
  switchChapter(item.id)
}

function getDifficultyLabel(level?: number) {
  const map: Record<number, string> = { 1: '基础', 2: '案例', 3: '实操' }
  return map[level || 1] || '基础'
}

function getDifficultyTagType(level?: number): '' | 'success' | 'warning' | 'danger' {
  const map: Record<number, '' | 'success' | 'warning' | 'danger'> = { 1: 'success', 2: 'warning', 3: 'danger' }
  return map[level || 1] || ''
}

function playVideo() {
  const video = document.querySelector('video')
  if (video) {
    video.play()
    videoPlaying.value = true
  }
}

async function handleComplete() {
  const courseId = route.params.courseId as string
  const chapterId = route.params.chapterId as string

  if (isCurrentChapterCompleted.value) {
    return
  }

  // 检查是否有测验
  if (hasQuiz.value) {
    try {
      await ElMessageBox.confirm(
        '本章包含测验，完成测验可获得掌握度评分。是否现在参加测验？',
        '章节测验',
        {
          confirmButtonText: '参加测验',
          cancelButtonText: '直接完成',
          distinguishCancelAndClose: true,
          type: 'info',
        }
      )
      router.push(p(`/courses/${courseId}/chapters/${chapterId}/quiz`))
      return
    } catch (action) {
      // 仅「直接完成」按钮继续完成；点 X / Esc 关闭弹窗则取消操作
      if (action !== 'cancel') return
    }
  }

  try {
    const res = await courseApi.updateProgress({ courseId, chapterId, progress: 100, completed: true })
    if (res.data?.alreadyCompleted) {
      completed.value[chapterId] = true
      chapterProgress.value = 100
      return
    }
    completed.value[chapterId] = true
    chapterProgress.value = 100
    ElMessage.success('已完成本章学习')
    if (res.data?.newCertificate) {
      ElMessageBox.alert(
        `恭喜获得「${res.data.newCertificate.courseTitle}」高级结业证书！`,
        '证书已颁发',
        { confirmButtonText: '查看证书', type: 'success' },
      ).then(() => router.push(p('/learning/certificates')))
    }
  } catch (error) {
    console.error('updateProgress failed', error)
    ElMessage.error('保存进度失败，请检查网络或后端服务')
  }
}

function goToQuiz() {
  if (isCurrentChapterCompleted.value || quizPassed.value) {
    return
  }
  const courseId = route.params.courseId as string
  const chapterId = route.params.chapterId as string
  router.push(p(`/courses/${courseId}/chapters/${chapterId}/quiz`))
}

function handleNext() {
  if (!chapters.value.length) {
    ElMessage.warning('暂无更多章节')
    return
  }

  const idx = currentIndex.value >= 0 ? currentIndex.value : 0
  if (idx < chapters.value.length - 1) {
    const nextChapter = chapters.value[idx + 1]
    switchChapter(nextChapter.id)
  } else {
    ElMessage.success('已是最后一章')
  }
}

async function loadChapter() {
  const courseId = route.params.courseId as string
  const chapterId = route.params.chapterId as string
  loading.value = true
  try {
    const [chapterRes, progressRes, quizRes] = await Promise.all([
      request.get(`/courses/${courseId}/chapters/${chapterId}`),
      courseApi.getProgress(courseId).catch(() => ({ data: [] })),
      checkQuizExists(chapterId).catch(() => ({ data: { exists: false } })),
    ])
    chapter.value = chapterRes.data.chapter
    chapters.value = chapterRes.data.chapters
    keywords.value = extractKeywords(chapter.value.content || '')
    hasQuiz.value = quizRes.data?.exists || false
    quizPassed.value = quizRes.data?.quizPassed || false
    quizBestScore.value = quizRes.data?.bestScore || 0
    masteryLevel.value = quizRes.data?.masteryLevel || 0
    const progressMap: Record<string, boolean> = {}
    for (const item of progressRes.data || []) {
      if (item.completed) progressMap[item.chapterId] = true
      if (item.chapterId === chapterId) {
        chapterProgress.value = item.progress || 0
        if (item.masteryLevel) masteryLevel.value = item.masteryLevel
      }
    }
    if (quizRes.data?.chapterCompleted) {
      progressMap[chapterId] = true
      chapterProgress.value = 100
    }
    completed.value = progressMap
    await nextTick()
    handleScroll()
  } catch (error) {
    console.error('加载章节失败', error)
    ElMessage.error('加载章节失败，请检查网络或后端服务')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadChapter()
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

watch(() => [route.params.courseId, route.params.chapterId], () => {
  window.scrollTo({ top: 0 })
  readProgress.value = 0
  loadChapter()
})
</script>

<style scoped>
/* 头部区域优化 */
.chapter-header {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
  margin-bottom: 24px;
  overflow: hidden;
}

/* 阅读进度条 */
.reading-progress {
  position: absolute;
  left: 0;
  bottom: 0;
  height: 3px;
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
  transition: width 0.15s ease-out;
  border-radius: 0 3px 3px 0;
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

.chapter-title {
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

.learning-streak {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #f59e0b;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input {
  width: 240px;
}

.notification-badge :deep(.el-badge__content) {
  background: #ef4444;
}

/* 主布局优化 */
.chapter-layout {
  display: flex;
  gap: 24px;
  min-height: calc(100vh - 200px);
}

/* 侧边栏优化 */
.sidebar {
  width: 340px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex-shrink: 0;
}

.sidebar-card {
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

.chapter-count {
  font-size: 12px;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 10px;
}

/* 课程目录优化 */
.toc-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.toc-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 8px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s;
}

.toc-item:hover {
  background: #f9fafb;
}

.toc-item.active {
  background: #eff6ff;
}

.toc-item.locked {
  opacity: 0.45;
  cursor: not-allowed;
}

.toc-item.locked:hover {
  background: transparent;
}

.toc-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding-top: 2px;
}

.indicator-line {
  width: 2px;
  height: 20px;
  background: #e5e7eb;
  transition: background 0.2s;
}

.indicator-line.completed {
  background: #10b981;
}

.indicator-dot {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
  transition: all 0.2s;
}

.indicator-dot.active {
  background: #3b82f6;
  color: white;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.indicator-dot.completed {
  background: #10b981;
  color: white;
}

.indicator-dot.locked {
  background: #f3f4f6;
  color: #9ca3af;
}

.toc-content {
  flex: 1;
  min-width: 0;
}

.toc-title {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 4px;
  line-height: 1.4;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.toc-item.active .toc-title {
  color: #3b82f6;
  font-weight: 600;
}

.toc-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.completion-badge {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  color: #10b981;
  font-weight: 500;
}

/* 笔记卡片优化 */
.note-card {
  flex: 1;
  min-height: 200px;
}

/* 操作按钮优化 */
.action-card {
  background: #f9fafb;
}

.completed-status {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.completed-banner {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 10px;
}

.completed-icon {
  font-size: 22px;
  color: #10b981;
  flex-shrink: 0;
  margin-top: 2px;
}

.completed-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.completed-title {
  font-size: 14px;
  font-weight: 600;
  color: #065f46;
}

.completed-meta {
  font-size: 12px;
  color: #047857;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.action-btn {
  width: 100%;
  height: 44px;
  font-size: 14px;
  font-weight: 500;
  margin-left: 0 !important;
}

.primary-cta {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border: none;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.primary-cta:hover {
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.4);
}

.secondary-btn {
  background: #fff;
  border: 1px solid #e5e7eb;
  color: #4b5563;
}

.secondary-btn:hover {
  background: #f5f3ff;
  border-color: #c7d2fe;
  color: #4f46e5;
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

/* 主内容区优化 */
.main-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.content-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
}

.video-container {
  position: relative;
  margin-bottom: 24px;
  border-radius: 10px;
  overflow: hidden;
  background: #000;
}

.video-container video {
  display: block;
  width: 100%;
  height: auto;
}

.video-overlay {
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

.video-container:hover .video-overlay {
  opacity: 1;
}

/* 本章关键词 */
.keyword-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 14px 18px;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #f5f3ff, #eef2ff);
  border: 1px solid #e0e7ff;
  border-radius: 10px;
}

.keyword-label {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  font-weight: 600;
  color: #6366f1;
  flex-shrink: 0;
}

.keyword-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.keyword-chip {
  font-size: 12px;
  color: #4f46e5;
  background: #fff;
  border: 1px solid #c7d2fe;
  padding: 3px 12px;
  border-radius: 20px;
  font-weight: 500;
  transition: all 0.2s;
}

.keyword-chip:hover {
  background: #6366f1;
  color: #fff;
  border-color: #6366f1;
}

/* 内容排版优化 */
.content-area {
  line-height: 1.85;
  color: #374151;
  font-size: 15px;
}

.content-area :deep(h2) {
  font-size: 21px;
  font-weight: 700;
  margin: 36px 0 18px;
  color: #111827;
  padding: 0 0 0 14px;
  border-left: 5px solid #6366f1;
  line-height: 1.3;
}

.content-area :deep(h2:first-child) {
  margin-top: 0;
}

.content-area :deep(h3) {
  font-size: 17px;
  font-weight: 600;
  margin: 28px 0 12px;
  color: #1f2937;
  display: flex;
  align-items: center;
}

.content-area :deep(h3)::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #8b5cf6;
  margin-right: 8px;
  flex-shrink: 0;
}

.content-area :deep(p) {
  margin-bottom: 16px;
  line-height: 1.85;
}

.content-area :deep(ul),
.content-area :deep(ol) {
  padding-left: 0;
  margin-bottom: 20px;
  list-style: none;
}

.content-area :deep(ul) > :deep(li) {
  position: relative;
  padding: 10px 16px 10px 36px;
  margin-bottom: 8px;
  background: #f9fafb;
  border-radius: 8px;
  line-height: 1.6;
}

.content-area :deep(ul) > :deep(li)::before {
  content: '';
  position: absolute;
  left: 16px;
  top: 18px;
  width: 7px;
  height: 7px;
  border-radius: 2px;
  background: #8b5cf6;
}

.content-area :deep(ol) {
  counter-reset: ol-counter;
}

.content-area :deep(ol) > :deep(li) {
  position: relative;
  counter-increment: ol-counter;
  padding: 10px 16px 10px 44px;
  margin-bottom: 8px;
  background: #f9fafb;
  border-radius: 8px;
  line-height: 1.6;
}

.content-area :deep(ol) > :deep(li)::before {
  content: counter(ol-counter);
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #6366f1;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 表格卡片化 —— 覆盖内容内联样式 */
.content-area :deep(table) {
  width: 100% !important;
  border-collapse: separate !important;
  border-spacing: 0;
  margin: 20px 0 !important;
  font-size: 14px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}

.content-area :deep(th),
.content-area :deep(td) {
  border: none !important;
  border-bottom: 1px solid #f1f3f5 !important;
  padding: 12px 16px !important;
  text-align: left;
}

.content-area :deep(tr:last-child) :deep(td) {
  border-bottom: none !important;
}

.content-area :deep(th) {
  background: linear-gradient(135deg, #6366f1, #8b5cf6) !important;
  font-weight: 600;
  color: #fff !important;
}

.content-area :deep(tr):nth-child(even) :deep(td) {
  background: #fafafe;
}

.content-area :deep(tbody tr):hover :deep(td),
.content-area :deep(table tr):hover :deep(td) {
  background: #f5f3ff;
}

/* 引用 / 参考规范 卡片化 —— 覆盖内联样式 */
.content-area :deep(blockquote) {
  border-left: 4px solid #6366f1 !important;
  padding: 16px 20px !important;
  background: #eef2ff !important;
  margin: 24px 0 !important;
  border-radius: 0 10px 10px 0;
  color: #3730a3 !important;
  font-style: normal;
  line-height: 1.7;
}

.content-area :deep(strong) {
  color: #4f46e5;
  font-weight: 600;
}

.content-area :deep(em) {
  color: #6d28d9;
  font-style: normal;
  font-weight: 500;
}

/* 章节导航优化 */
.chapter-navigation {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e5e7eb;
}

.nav-btn {
  height: 44px;
  padding: 0 24px;
  font-size: 14px;
  font-weight: 500;
}

.prev-btn {
  background: #f9fafb;
  border-color: #e5e7eb;
}

.prev-btn:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.next-btn {
  background: #3b82f6;
  border-color: #3b82f6;
}

.next-btn:hover {
  background: #2563eb;
  border-color: #2563eb;
}

.nav-spacer {
  flex: 1;
}

/* 学习进度提示优化 */
.progress-hint {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}

.hint-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #eff6ff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3b82f6;
  flex-shrink: 0;
}

.hint-content {
  flex: 1;
  min-width: 0;
}

.hint-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 2px;
}

.hint-desc {
  font-size: 12px;
  color: #6b7280;
}

.hint-progress {
  width: 120px;
  flex-shrink: 0;
}

/* 响应式优化 */
@media (max-width: 1200px) {
  .chapter-layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .toc-card {
    flex: 1;
    min-width: 300px;
  }

  .note-card {
    flex: 1;
    min-width: 300px;
  }

  .action-card {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .chapter-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .header-right {
    width: 100%;
  }

  .search-input {
    width: 100%;
  }

  .sidebar {
    flex-direction: column;
  }

  .toc-card,
  .note-card {
    min-width: 100%;
  }
}
</style>
