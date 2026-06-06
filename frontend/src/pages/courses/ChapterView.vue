<template>
  <div class="chapter-view">
    <el-page-header @back="$router.back()">
      <template #content>
        <span class="text-lg font-bold">{{ chapter.title || '章节学习' }}</span>
      </template>
    </el-page-header>

    <el-skeleton v-if="loading" animated :rows="6" class="mt-4" />

    <template v-else>
      <el-empty v-if="!chapter.id" description="未找到章节" class="mt-8" />

      <el-row v-else :gutter="24" class="mt-6">
        <!-- 主内容区 -->
        <el-col :span="18">
          <el-card>
            <!-- 视频播放器 -->
            <div v-if="chapter.videoUrl" class="video-container">
              <video
                ref="videoRef"
                :src="chapter.videoUrl"
                controls
                class="w-full"
              />
            </div>

            <!-- 图文内容 -->
            <div class="content-area" v-html="chapter.content" />
          </el-card>
        </el-col>

        <!-- 右侧目录 -->
        <el-col :span="6">
          <el-card>
            <template #header>
              <span class="font-bold">课程目录</span>
            </template>
            <div v-if="chapters.length">
              <div
                v-for="(item, index) in chapters"
                :key="item.id"
                class="toc-item"
                :class="{ active: item.id === chapter.id, completed: completed[item.id], locked: item.unlocked === false }"
                @click="handleTocClick(item)"
              >
                <span class="toc-index">
                  <el-icon v-if="item.unlocked === false"><Lock /></el-icon>
                  <template v-else>{{ index + 1 }}</template>
                </span>
                <span class="toc-title">{{ item.title }}</span>
                <el-tag v-if="item.difficultyLevel" :type="getDifficultyTagType(item.difficultyLevel)" size="small" effect="plain">
                  {{ getDifficultyLabel(item.difficultyLevel) }}
                </el-tag>
                <el-tag v-if="completed[item.id]" size="small" type="success">已完成</el-tag>
              </div>
            </div>
            <el-empty v-else description="暂无章节" />
          </el-card>

          <el-card class="mt-4">
            <el-button type="primary" class="w-full" @click="handleComplete">
              完成本章
            </el-button>
            <el-button class="w-full mt-2" @click="handleNext" :disabled="!hasNext">
              下一章
            </el-button>
            <el-alert
              v-if="!hasNext && completed[chapter.id]"
              class="mt-3"
              type="success"
              show-icon
              title="已是最后一章"
              description="可返回课程详情查看整体进度"
            />
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import request from '@/api/request'
import { courseApi } from '@/api/course'
import { useAppBase } from '@/composables/useAppBase'

const route = useRoute()
const router = useRouter()
const { p } = useAppBase()

const loading = ref(true)
const chapter = ref({ id: '', title: '', content: '', videoUrl: '' })
const chapters = ref<{ id: string; title: string; difficultyLevel?: number; unlocked?: boolean }[]>([])
const completed = ref<Record<string, boolean>>({})

const currentIndex = computed(() => chapters.value.findIndex(c => c.id === chapter.value.id))
const hasNext = computed(() => currentIndex.value >= 0 && currentIndex.value < chapters.value.length - 1)

function switchChapter(id: string) {
  const courseId = route.params.courseId
  router.push(p(`/courses/${courseId}/chapters/${id}`))
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

async function handleComplete() {
  const courseId = route.params.courseId as string
  const chapterId = route.params.chapterId as string
  try {
    await courseApi.updateProgress({ courseId, chapterId, progress: 100, completed: true })
  } catch (error) {
    console.error('updateProgress failed', error)
    ElMessage.error('保存进度失败，请检查网络或后端服务')
    return
  }
  completed.value[chapterId] = true
  ElMessage.success('已完成本章学习')
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
    const [chapterRes, progressRes] = await Promise.all([
      request.get(`/courses/${courseId}/chapters/${chapterId}`),
      courseApi.getProgress(courseId).catch(() => ({ data: [] })),
    ])
    chapter.value = chapterRes.data.chapter
    chapters.value = chapterRes.data.chapters
    const progressMap: Record<string, boolean> = {}
    for (const item of progressRes.data || []) {
      if (item.completed) progressMap[item.chapterId] = true
    }
    completed.value = progressMap
  } catch (error) {
    console.error('加载章节失败', error)
    ElMessage.error('加载章节失败，请检查网络或后端服务')
  } finally {
    loading.value = false
  }
}

onMounted(loadChapter)

watch(() => [route.params.courseId, route.params.chapterId], () => {
  loadChapter()
})
</script>

<style scoped>
.chapter-view {
  width: 100%;
  min-height: 100%;
  padding: 20px;
  box-sizing: border-box;
}

.video-container {
  margin-bottom: 24px;
  border-radius: 8px;
  overflow: hidden;
}

.content-area {
  line-height: 1.8;
  color: #374151;
}

.content-area h2 {
  font-size: 20px;
  font-weight: bold;
  margin: 24px 0 12px;
  color: #1f2937;
}

.content-area p {
  margin-bottom: 16px;
}

.content-area ul, .content-area ol {
  padding-left: 24px;
  margin-bottom: 16px;
}

.content-area li {
  margin-bottom: 8px;
}

.toc-item {
  display: flex;
  align-items: center;
  padding: 12px;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.2s;
}

.toc-item:hover {
  background: #f3f4f6;
}

.toc-item.active {
  background: #eff6ff;
  color: #3b82f6;
}

.toc-index {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  margin-right: 12px;
}

.toc-item.active .toc-index {
  background: #3b82f6;
  color: white;
}

.toc-item.locked {
  opacity: 0.45;
  cursor: not-allowed;
}

.toc-item.locked:hover {
  background: transparent;
}
</style>
