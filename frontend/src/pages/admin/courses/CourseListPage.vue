<template>
  <div class="course-list-page" v-loading="loading">
    <div class="page-toolbar">
      <div class="category-tabs">
        <button
          v-for="tab in categoryTabs"
          :key="tab.value"
          type="button"
          class="category-tab"
          :class="{ active: filters.category === tab.value }"
          @click="switchCategory(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="filter-bar">
        <el-select v-model="filters.status" placeholder="课程状态" clearable class="filter-item">
          <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.department" placeholder="适用部门" clearable class="filter-item">
          <el-option v-for="dept in DEPARTMENT_OPTIONS" :key="dept" :label="dept" :value="dept === '全部部门' ? 'all' : dept" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          class="filter-date"
        />
        <el-input v-model="filters.keyword" placeholder="搜索课程名称、关键词" clearable class="filter-search" @keyup.enter="search" />
        <el-button @click="resetFilters">重置</el-button>
        <el-button type="primary" @click="search">搜索</el-button>
      </div>

      <div class="action-bar">
        <span class="total-text">共 {{ total }} 门课程</span>
        <div class="action-right">
          <el-button @click="$router.push('/admin/learning/categories')">分类配置</el-button>
          <el-button type="primary" @click="$router.push('/admin/learning/courses/new')">
            <el-icon><Plus /></el-icon> 新建课程
          </el-button>
        </div>
      </div>
    </div>

    <div class="table-card">
      <table class="course-table">
        <thead>
          <tr>
            <th>课程信息</th>
            <th>课程类型</th>
            <th>适用对象</th>
            <th>总时长</th>
            <th>讲师</th>
            <th>状态</th>
            <th>完成率</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="course in courses" :key="course.id">
            <td class="course-info-cell">
              <img :src="coverUrl(course.coverImage)" alt="" class="course-cover" />
              <div>
                <strong>{{ course.title }}</strong>
                <p>{{ course.subtitle || course.description || '暂无简介' }}</p>
              </div>
            </td>
            <td>
              <el-tag :type="categoryTagType(course.category, categories) || undefined" size="small">
                {{ course.categoryName || categoryLabel(course.category, categories) }}
              </el-tag>
            </td>
            <td>{{ course.targetAudience || '全体员工' }}</td>
            <td>{{ formatDuration(course.totalDuration) }}</td>
            <td>{{ course.instructor || '-' }}</td>
            <td><span class="status-pill" :class="statusClass(course.status)">{{ statusLabel(course.status) }}</span></td>
            <td class="rate-cell">
              <template v-if="course.status === 'published'">
                <span>{{ course.completionRate ?? 0 }}%</span>
                <div class="rate-bar"><i :style="{ width: `${Math.min(100, course.completionRate ?? 0)}%` }" /></div>
              </template>
              <span v-else class="muted">—</span>
            </td>
            <td class="action-cell">
              <button type="button" class="link-btn" @click="viewCourse(course.id)">查看</button>
              <button type="button" class="link-btn" @click="editCourse(course.id)">编辑</button>
              <el-dropdown trigger="click" @command="(cmd: string) => handleMore(cmd, course)">
                <button type="button" class="link-btn icon-btn">···</button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="monitoring">学习监控</el-dropdown-item>
                    <el-dropdown-item command="preview">预览学员端</el-dropdown-item>
                    <el-dropdown-item v-if="course.status === 'published'" command="unpublish">下架课程</el-dropdown-item>
                    <el-dropdown-item v-else command="publish">发布课程</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </td>
          </tr>
        </tbody>
      </table>
      <el-empty v-if="!loading && !courses.length" description="暂无课程" />
    </div>

    <div class="pagination-bar">
      <span>共 {{ total }} 条</span>
      <el-pagination
        v-model:current-page="filters.page"
        v-model:page-size="filters.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="sizes, prev, pager, next, jumper"
        @current-change="loadCourses"
        @size-change="onPageSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, type AdminCourseListItem } from '@/api/admin'
import type { CourseCategory } from '@/types'
import {
  DEPARTMENT_OPTIONS, STATUS_OPTIONS, categoryLabel, categoryTagType,
  coverUrl, formatDuration, statusClass, statusLabel,
} from './courseAdminShared'

const router = useRouter()
const loading = ref(false)
const courses = ref<AdminCourseListItem[]>([])
const categories = ref<CourseCategory[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)

const filters = reactive({
  category: 'all',
  status: 'all',
  department: 'all',
  keyword: '',
  page: 1,
  pageSize: 10,
})

const categoryTabs = computed(() => [
  { label: '全部课程', value: 'all' },
  ...categories.value.filter(c => c.enabled).map(c => ({ label: c.name, value: c.code })),
])

async function loadCategories() {
  const res = await adminApi.getCourseCategories()
  categories.value = res.data || []
}

async function loadCourses() {
  loading.value = true
  try {
    const res = await adminApi.getCourses({
      category: filters.category === 'all' ? undefined : filters.category,
      status: filters.status === 'all' ? undefined : filters.status,
      department: filters.department === 'all' ? undefined : filters.department,
      keyword: filters.keyword || undefined,
      createdFrom: dateRange.value?.[0],
      createdTo: dateRange.value?.[1],
      page: filters.page,
      pageSize: filters.pageSize,
    })
    courses.value = res.data.items || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function switchCategory(value: string) {
  filters.category = value
  filters.page = 1
  loadCourses()
}

function search() {
  filters.page = 1
  loadCourses()
}

function resetFilters() {
  filters.category = 'all'
  filters.status = 'all'
  filters.department = 'all'
  filters.keyword = ''
  filters.page = 1
  dateRange.value = null
  loadCourses()
}

function onPageSizeChange() {
  filters.page = 1
  loadCourses()
}

function viewCourse(id: string) {
  router.push(`/admin/learning/courses/${id}`)
}

function editCourse(id: string) {
  router.push(`/admin/learning/courses/${id}/edit`)
}

async function handleMore(command: string, course: AdminCourseListItem) {
  if (command === 'monitoring') {
    router.push({ path: '/admin/learning/monitoring', query: { courseId: course.id } })
    return
  }
  if (command === 'preview') {
    window.open(`/user/courses/${course.id}`, '_blank')
    return
  }
  if (command === 'publish' || command === 'unpublish') {
    const status = command === 'publish' ? 'published' : 'draft'
    await adminApi.updateCourse(course.id, { status })
    ElMessage.success(command === 'publish' ? '课程已发布' : '课程已下架')
    await loadCourses()
    return
  }
  if (command === 'delete') {
    await ElMessageBox.confirm(`确定删除课程「${course.title}」吗？`, '提示', { type: 'warning' })
    await adminApi.deleteCourse(course.id)
    ElMessage.success('删除成功')
    await loadCourses()
  }
}

onMounted(async () => {
  await loadCategories()
  await loadCourses()
})
</script>

<style scoped>
.course-list-page { padding: 20px 24px 24px; min-height: 100%; box-sizing: border-box; }
.page-toolbar { margin-bottom: 16px; }
.category-tabs { display: flex; flex-wrap: wrap; gap: 20px; border-bottom: 1px solid #e8edf3; margin-bottom: 16px; }
.category-tab { padding: 0 0 12px; border: 0; background: none; color: #667085; font-size: 14px; cursor: pointer; position: relative; }
.category-tab.active { color: #2563eb; font-weight: 600; }
.category-tab.active::after { content: ''; position: absolute; left: 0; right: 0; bottom: -1px; height: 2px; background: #2563eb; border-radius: 2px; }
.filter-bar { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 14px; }
.filter-item { width: 140px; }
.filter-date { width: 260px; }
.filter-search { width: 220px; }
.action-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.total-text { color: #667085; font-size: 13px; }
.action-right { display: flex; gap: 10px; }
.table-card { background: #fff; border: 1px solid #e8edf3; border-radius: 8px; overflow: hidden; }
.course-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.course-table th { padding: 12px 14px; background: #fafbfc; color: #8b95a4; text-align: left; font-weight: 500; white-space: nowrap; }
.course-table td { padding: 14px; border-top: 1px solid #edf0f4; vertical-align: middle; color: #4b5563; }
.course-info-cell { display: flex; align-items: center; gap: 12px; min-width: 280px; }
.course-cover { width: 88px; height: 56px; border-radius: 6px; object-fit: cover; background: #f3f4f6; flex-shrink: 0; }
.course-info-cell strong { display: block; color: #1f2937; font-size: 14px; margin-bottom: 4px; }
.course-info-cell p { margin: 0; color: #9ca3af; font-size: 12px; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.status-pill { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.status-pill.is-published { color: #16a34a; background: #ecfdf3; }
.status-pill.is-draft { color: #9ca3af; background: #f3f4f6; }
.rate-cell span { display: block; margin-bottom: 4px; font-size: 12px; color: #374151; }
.rate-bar { width: 72px; height: 5px; background: #edf1f6; border-radius: 99px; overflow: hidden; }
.rate-bar i { display: block; height: 100%; background: #3b82f6; border-radius: inherit; }
.action-cell { white-space: nowrap; }
.link-btn { border: 0; background: none; color: #2563eb; font-size: 13px; cursor: pointer; padding: 0 4px; }
.icon-btn { letter-spacing: 1px; }
.muted { color: #cbd5e1; }
.pagination-bar { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; color: #667085; font-size: 13px; }
</style>
