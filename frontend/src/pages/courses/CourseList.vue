<template>
  <div class="sl-page course-list">
    <div class="sl-page-header">
      <h2 class="sl-page-title">安全培训课程</h2>
      <div class="header-actions">
        <el-button type="primary" plain @click="router.push(p('/courses/skill-tree'))">
          安全进阶路径
        </el-button>
        <el-input
          v-model="searchText"
          placeholder="搜索课程..."
          prefix-icon="Search"
          style="width: 300px"
        />
      </div>
    </div>

    <div class="filter-toolbar sl-page-section">
      <div class="category-filters">
        <button
          v-for="cat in categories"
          :key="cat.value"
          type="button"
          class="category-pill"
          :class="{ active: activeCategory === cat.value }"
          @click="activeCategory = cat.value"
        >
          {{ cat.label }}
        </button>
      </div>
      <div class="sort-control">
        <span class="sort-label">排序：</span>
        <el-select v-model="sortBy" class="sort-select" size="default">
          <el-option label="最新发布" value="latest" />
          <el-option label="最热学习" value="popular" />
        </el-select>
      </div>
    </div>

    <el-skeleton v-if="loading" animated :rows="6" />
    <template v-else>
      <el-empty v-if="!filteredCourses.length" description="暂无课程" />
      <el-row v-else :gutter="20">
        <el-col :span="6" v-for="course in filteredCourses" :key="course.id">
          <el-card class="course-card" shadow="hover" @click="router.push(p(`/courses/${course.id}`))">
            <div class="course-cover">
              <img :src="course.coverImage || '/images/default-course.svg'" alt="cover" />
              <el-tag class="course-tag" :type="getCategoryType(course.category)">
                {{ getCategoryName(course.category) }}
              </el-tag>
            </div>
            <div class="course-info">
              <h3 class="course-title">{{ course.title }}</h3>
              <p class="course-desc">{{ course.description }}</p>
              <div class="course-meta">
                <span><el-icon><Clock /></el-icon> {{ course.totalDuration }}分钟</span>
                <span><el-icon><Document /></el-icon> {{ course.chapters?.length || 0 }}章节</span>
              </div>
              <el-progress
                :percentage="course.progress || 0"
                :status="course.progress === 100 ? 'success' : ''"
                class="mt-3"
              />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Clock, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useAppBase } from '@/composables/useAppBase'
import type { Course } from '@/types'

const route = useRoute()
const router = useRouter()
const { p } = useAppBase()
const searchText = ref('')
const activeCategory = ref('all')
const sortBy = ref<'latest' | 'popular'>('latest')

const categories = [
  { label: '全部', value: 'all' },
  { label: '基础知识', value: 'basic' },
  { label: '锂电池', value: 'battery' },
  { label: '热失控', value: 'thermal' },
  { label: '消防安全', value: 'fire' },
  { label: 'BMS系统', value: 'bms' },
]

const courses = ref<Course[]>([])
const loading = ref(true)

onMounted(async () => {
  if (route.query.keyword) {
    searchText.value = String(route.query.keyword)
  }
  loading.value = true
  try {
    const res = await request.get('/courses')
    courses.value = res.data.items
  } catch (error) {
    console.error('加载课程列表失败', error)
    ElMessage.error('加载课程列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
})

const filteredCourses = computed(() => {
  let result = [...courses.value]
  if (activeCategory.value !== 'all') {
    result = result.filter(c => c.category === activeCategory.value)
  }
  if (searchText.value) {
    const search = searchText.value.toLowerCase()
    result = result.filter(c =>
      c.title.toLowerCase().includes(search) ||
      c.description.toLowerCase().includes(search)
    )
  }
  if (sortBy.value === 'popular') {
    result.sort((a, b) => (b.learnerCount || 0) - (a.learnerCount || 0))
  } else {
    result.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  }
  return result
})

function getCategoryType(category: string) {
  const map: Record<string, string> = {
    basic: '',
    battery: 'success',
    thermal: 'danger',
    fire: 'warning',
    bms: 'info',
  }
  return map[category] || ''
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
</script>

<style scoped>
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 16px 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.06);
}

.category-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.category-pill {
  border: none;
  border-radius: 8px;
  padding: 8px 18px;
  font-size: 14px;
  font-weight: 500;
  color: #4b5563;
  background: #f3f4f6;
  cursor: pointer;
  transition: all 0.2s ease;
  line-height: 1.4;
}

.category-pill:hover {
  background: #e5e7eb;
  color: #1f2937;
}

.category-pill.active {
  background: #2b5aed;
  color: #fff;
  box-shadow: 0 2px 8px rgba(43, 90, 237, 0.25);
}

.sort-control {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.sort-label {
  font-size: 14px;
  color: #6b7280;
  white-space: nowrap;
}

.sort-select {
  width: 130px;
}

.course-card {
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 20px;
}

.course-card:hover {
  transform: translateY(-4px);
}

.course-cover {
  position: relative;
  height: 160px;
  overflow: hidden;
  border-radius: 8px;
  margin: -20px -20px 16px;
}

.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.course-tag {
  position: absolute;
  top: 8px;
  right: 8px;
}

.course-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #1f2937;
}

.course-desc {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.course-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #9ca3af;
}

.course-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

@media (max-width: 768px) {
  .filter-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .sort-control {
    justify-content: flex-end;
  }
}
</style>
