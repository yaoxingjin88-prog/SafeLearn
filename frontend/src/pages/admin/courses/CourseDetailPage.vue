<template>
  <div class="course-detail-page" v-loading="loading">
    <template v-if="course">
      <div class="detail-top">
        <div class="hero-card">
          <img :src="coverUrl(course.coverImage)" alt="" class="hero-image" />
          <div class="hero-overlay">
            <div class="hero-badge">
              <el-tag size="small" effect="dark">{{ course.categoryName || categoryLabel(course.category, categories) }}</el-tag>
            </div>
            <div class="hero-text">
              <h1>{{ course.title }}</h1>
              <p>{{ course.subtitle || course.description }}</p>
            </div>
          </div>
        </div>

        <div class="info-card">
          <div class="info-head">
            <h2>基本信息</h2>
            <div class="info-actions">
              <el-button size="small" type="primary" plain @click="editCourse">编辑课程</el-button>
              <el-button size="small" @click="togglePublish">{{ course.status === 'published' ? '下架课程' : '发布课程' }}</el-button>
            </div>
          </div>
          <dl class="info-list">
            <div class="info-row"><dt>课程名称</dt><dd>{{ course.title }}</dd></div>
            <div class="info-row"><dt>课程类型</dt><dd>{{ course.categoryName || categoryLabel(course.category, categories) }}</dd></div>
            <div class="info-row"><dt>适用对象</dt><dd>{{ course.targetAudience || '全体员工' }}</dd></div>
            <div class="info-row"><dt>总时长</dt><dd>{{ formatDuration(course.totalDuration) }}</dd></div>
            <div class="info-row"><dt>讲师</dt><dd>{{ instructorText }}</dd></div>
            <div class="info-row"><dt>创建时间</dt><dd>{{ course.createdAt || '-' }}</dd></div>
            <div class="info-row"><dt>更新时间</dt><dd>{{ course.updatedAt || '-' }}</dd></div>
            <div class="info-row"><dt>课程状态</dt><dd><span class="status-pill" :class="statusClass(course.status)">{{ statusLabel(course.status) }}</span></dd></div>
          </dl>
        </div>
      </div>

      <div class="detail-body">
        <article class="panel panel-left">
          <section class="block">
            <h3>课程目标</h3>
            <ol>
              <li v-for="(item, index) in objectives" :key="index">{{ item }}</li>
            </ol>
          </section>

          <section class="block block-roles">
            <h3>适用岗位/部门</h3>
            <div class="role-grid">
              <div v-for="role in targetRoles" :key="role" class="role-item">
                <span class="role-icon"><el-icon><User /></el-icon></span>
                <span>{{ role }}</span>
              </div>
            </div>
          </section>

          <section class="block block-tags">
            <h3>课程标签</h3>
            <div class="tag-list">
              <el-tag v-for="(tag, index) in tags" :key="tag" size="small" effect="plain" :type="tagTypes[index % tagTypes.length]">{{ tag }}</el-tag>
            </div>
          </section>
        </article>

        <div class="detail-right">
          <div class="detail-right-top">
            <article class="panel panel-knowledge">
              <h3>关键知识点</h3>
              <ul class="knowledge-list">
                <li v-for="(point, index) in knowledgePoints" :key="index">
                  <span class="check-icon"><el-icon><CircleCheckFilled /></el-icon></span>
                  <span>{{ point }}</span>
                </li>
              </ul>
            </article>

            <article class="panel panel-chapters">
              <div class="panel-head">
                <h3>章节大纲<span class="chapter-count">（共 {{ chapters.length }} 章）</span></h3>
              </div>
              <ul class="chapter-list">
                <li v-for="chapter in chapters" :key="chapter.id">
                  <span class="chapter-no">{{ String(chapter.order || chapter.orderNum || 0).padStart(2, '0') }}</span>
                  <span class="chapter-title">{{ chapter.title }}</span>
                  <span class="chapter-duration">{{ formatChapterDuration(chapter.duration) }}</span>
                </li>
              </ul>
            </article>
          </div>

          <div class="stats-bar panel">
            <div v-for="item in statCards" :key="item.label" class="stat-item">
              <span class="stat-icon" :class="item.tone"><el-icon><component :is="item.icon" /></el-icon></span>
              <div class="stat-text">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CircleCheckFilled, DataAnalysis, DataLine, Medal, Reading, User,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { adminApi, type AdminCourseDetail } from '@/api/admin'
import type { CourseCategory } from '@/types'
import {
  categoryLabel, coverUrl, formatDuration, statusClass, statusLabel,
} from './courseAdminShared'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const course = ref<AdminCourseDetail | null>(null)
const categories = ref<CourseCategory[]>([])
const tagTypes = ['', 'warning', 'success', 'info'] as const

const chapters = computed(() => course.value?.chapters || [])
const objectives = computed(() => {
  const list = course.value?.objectives || []
  if (list.length) return list.slice(0, 3)
  return course.value?.description ? [course.value.description] : ['暂无课程目标']
})
const tags = computed(() => course.value?.tags?.length
  ? course.value.tags.slice(0, 4)
  : [course.value?.categoryName || '安全培训'].filter(Boolean) as string[])
const knowledgePoints = computed(() => {
  const list = course.value?.knowledgePoints?.length
    ? course.value.knowledgePoints
    : chapters.value.map(ch => ch.title)
  return list.slice(0, 6)
})
const targetRoles = computed(() => {
  const roles = course.value?.targetRoles || []
  const depts = course.value?.targetDepartments || []
  const merged = [...roles, ...depts]
  if (merged.length) return merged.slice(0, 4)
  return [course.value?.targetAudience || '全体员工']
})
const instructorText = computed(() => {
  if (!course.value?.instructor) return '-'
  return course.value.instructorTitle
    ? `${course.value.instructor}（${course.value.instructorTitle}）`
    : course.value.instructor
})

const statCards = computed(() => {
  const stats = course.value?.stats
  return [
    { label: '学习人数', value: stats?.learnerCount ?? 0, icon: User, tone: 'blue' },
    { label: '完成人数', value: stats?.completedCount ?? 0, icon: Reading, tone: 'green' },
    { label: '完成率', value: `${stats?.completionRate ?? 0}%`, icon: DataLine, tone: 'orange' },
    { label: '平均得分', value: stats?.avgScore ?? 0, icon: Medal, tone: 'purple' },
    { label: '好评率', value: `${stats?.positiveRate ?? 0}%`, icon: DataAnalysis, tone: 'cyan' },
  ]
})

function formatChapterDuration(minutes?: number) {
  if (!minutes) return '00:00'
  const mm = String(minutes).padStart(2, '0')
  return `${mm}:00`
}

async function loadCourse() {
  loading.value = true
  try {
    const [courseRes, catRes] = await Promise.all([
      adminApi.getCourseById(route.params.id as string),
      adminApi.getCourseCategories(),
    ])
    course.value = courseRes.data
    categories.value = catRes.data || []
  } finally {
    loading.value = false
  }
}

function editCourse() {
  router.push(`/admin/learning/courses/${route.params.id}/edit`)
}

async function togglePublish() {
  if (!course.value) return
  const status = course.value.status === 'published' ? 'draft' : 'published'
  await adminApi.updateCourse(course.value.id, { status })
  course.value.status = status
  ElMessage.success(status === 'published' ? '课程已发布' : '课程已下架')
}

onMounted(loadCourse)
</script>

<style scoped>
.course-detail-page {
  height: calc(100vh - 64px - 41px);
  padding: 8px 12px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
}

.detail-top {
  flex: 0 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 480px) minmax(0, 1fr);
  gap: 10px;
  align-items: stretch;
}

.hero-card,
.info-card,
.panel {
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(31, 49, 78, 0.04);
}

.hero-card {
  position: relative;
  overflow: hidden;
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 10px;
}

.hero-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.15) 0%, rgba(15, 23, 42, 0.78) 100%);
  color: #fff;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.hero-badge {
  align-self: flex-start;
}

.hero-text {
  margin-top: auto;
}

.hero-overlay h1 {
  margin: 0 0 4px;
  font-size: 18px;
  line-height: 1.3;
  font-weight: 700;
}

.hero-overlay p {
  margin: 0;
  font-size: 12px;
  opacity: 0.92;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.info-card {
  padding: 12px 16px;
  height: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.info-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  flex-shrink: 0;
}

.info-head h2 {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
}

.info-actions {
  display: flex;
  gap: 6px;
}

.info-actions :deep(.el-button) {
  padding: 4px 10px;
  height: 26px;
}

.info-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 0;
  overflow: hidden;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 12px;
  line-height: 1.4;
}

.info-list dt {
  flex: 0 0 60px;
  margin: 0;
  color: #9ca3af;
  font-size: 12px;
  white-space: nowrap;
}

.info-list dd {
  flex: 1;
  margin: 0;
  color: #374151;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.detail-body {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 3fr 7fr;
  gap: 10px;
  align-items: stretch;
}

.detail-right {
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-right-top {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.panel {
  padding: 12px 14px;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-left {
  gap: 12px;
  justify-content: flex-start;
}

.panel-left .block:first-child {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.block-tags {
  flex-shrink: 0;
}

.block h3,
.panel-knowledge h3,
.panel-head h3 {
  margin: 0 0 6px;
  font-size: 13px;
  font-weight: 700;
  color: #1f2937;
}

.block-roles {
  flex-shrink: 0;
}

.block ol {
  margin: 0;
  padding-left: 16px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}

.block ol li + li {
  margin-top: 2px;
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 4px;
}

.role-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  text-align: center;
  color: #4b5563;
  font-size: 10px;
}

.role-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #eff6ff;
  color: #2563eb;
  display: grid;
  place-items: center;
  font-size: 13px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.panel-knowledge h3 {
  flex-shrink: 0;
}

.knowledge-list {
  list-style: none;
  padding: 0;
  margin: 0;
  flex: 1;
  overflow: auto;
}

.knowledge-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 5px 0;
  color: #374151;
  font-size: 12px;
  line-height: 1.4;
  border-bottom: 1px solid #f3f4f6;
}

.knowledge-list li:last-child {
  border-bottom: 0;
}

.check-icon {
  flex: 0 0 16px;
  color: #3b82f6;
  font-size: 15px;
  line-height: 1;
  margin-top: 1px;
}

.panel-head {
  margin-bottom: 6px;
  flex-shrink: 0;
}

.chapter-count {
  margin-left: 4px;
  color: #9ca3af;
  font-size: 12px;
  font-weight: 400;
}

.chapter-list {
  list-style: none;
  padding: 0;
  margin: 0;
  flex: 1;
  overflow: auto;
}

.chapter-list li {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  gap: 6px;
  align-items: center;
  padding: 6px 0;
  border-top: 1px solid #f1f5f9;
  font-size: 12px;
}

.chapter-list li:first-child {
  border-top: 0;
  padding-top: 0;
}

.chapter-no {
  color: #9ca3af;
  font-weight: 600;
}

.chapter-title {
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chapter-duration {
  color: #6b7280;
  font-size: 11px;
}

.stats-bar {
  flex: 0 0 72px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0;
  padding: 10px 8px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  border-right: 1px solid #f1f5f9;
}

.stat-item:last-child {
  border-right: 0;
}

.stat-text {
  min-width: 0;
}

.stat-text span {
  display: block;
  color: #9ca3af;
  font-size: 11px;
  line-height: 1.2;
  margin-bottom: 2px;
}

.stat-icon {
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 16px;
}

.stat-icon.blue { background: #eff6ff; color: #2563eb; }
.stat-icon.green { background: #ecfdf5; color: #16a34a; }
.stat-icon.orange { background: #fff7ed; color: #ea580c; }
.stat-icon.purple { background: #f5f3ff; color: #7c3aed; }
.stat-icon.cyan { background: #ecfeff; color: #0891b2; }

.stat-item strong {
  display: block;
  font-size: 18px;
  line-height: 1.1;
  color: #1f2937;
  font-weight: 700;
}

.status-pill {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
}

.status-pill.is-published { color: #16a34a; background: #ecfdf3; }
.status-pill.is-draft { color: #9ca3af; background: #f3f4f6; }

@media (max-width: 1200px) {
  .course-detail-page {
    height: auto;
    overflow: auto;
  }

  .detail-top {
    grid-template-columns: 1fr;
    flex: none;
  }

  .detail-body {
    grid-template-columns: 1fr;
  }

  .detail-right-top {
    grid-template-columns: 1fr;
  }

  .stats-bar {
    flex: none;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
    height: auto;
  }

  .stat-item {
    border-right: 0;
    padding: 8px;
  }

}
</style>
