<template>
  <div class="course-list">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-2xl font-bold">安全培训课程</h2>
      <el-input
        v-model="searchText"
        placeholder="搜索课程..."
        prefix-icon="Search"
        style="width: 300px"
      />
    </div>

    <!-- 分类筛选 -->
    <div class="mb-6">
      <el-radio-group v-model="activeCategory">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="basic">基础知识</el-radio-button>
        <el-radio-button label="battery">锂电池</el-radio-button>
        <el-radio-button label="thermal">热失控</el-radio-button>
        <el-radio-button label="fire">消防安全</el-radio-button>
        <el-radio-button label="bms">BMS系统</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 课程列表 -->
    <el-skeleton v-if="loading" animated :rows="6" />
    <template v-else>
      <el-empty v-if="!filteredCourses.length" description="暂无课程" />
      <el-row v-else :gutter="20">
        <el-col :span="6" v-for="course in filteredCourses" :key="course.id">
          <el-card class="course-card" shadow="hover" @click="$router.push(`/courses/${course.id}`)">
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
import { Clock, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import type { Course } from '@/types'

const searchText = ref('')
const activeCategory = ref('all')

const courses = ref<Course[]>([])
const loading = ref(true)

onMounted(async () => {
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
  let result = courses.value
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
</style>
