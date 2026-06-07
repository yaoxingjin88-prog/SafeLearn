<template>
  <div class="sl-page course-manage">
    <div class="sl-page-header">
      <h2 class="sl-page-title">课程管理</h2>
      <el-button type="primary" @click="showAddCourse">
        <el-icon><Plus /></el-icon>
        添加课程
      </el-button>
    </div>

    <el-card v-loading="loading">
      <el-table :data="courses" style="width: 100%">
        <el-table-column prop="title" label="课程名称" min-width="200" />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag>{{ categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chapterCount" label="章节数" width="90" />
        <el-table-column prop="totalDuration" label="时长(分钟)" width="110" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'published' ? 'success' : 'info'">
              {{ row.status === 'published' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openChapters(row)">章节</el-button>
            <el-button type="primary" link @click="editCourse(row)">编辑</el-button>
            <el-button type="danger" link @click="deleteCourse(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 课程表单 -->
    <el-dialog v-model="courseDialogVisible" :title="isEdit ? '编辑课程' : '添加课程'" width="560px">
      <el-form ref="courseFormRef" :model="courseForm" :rules="courseRules" label-width="90px">
        <el-form-item label="课程名称" prop="title">
          <el-input v-model="courseForm.title" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="courseForm.category" placeholder="请选择分类" class="w-full">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="courseForm.status" class="w-full">
            <el-option label="已发布" value="published" />
            <el-option label="草稿" value="draft" />
          </el-select>
        </el-form-item>
        <el-form-item label="时长(分钟)">
          <el-input-number v-model="courseForm.totalDuration" :min="0" :step="5" />
        </el-form-item>
        <el-form-item label="课程简介">
          <el-input v-model="courseForm.description" type="textarea" :rows="4" placeholder="请输入课程简介" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="courseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCourse">确定</el-button>
      </template>
    </el-dialog>

    <!-- 章节管理 -->
    <el-drawer v-model="chapterDrawerVisible" :title="`章节管理 — ${activeCourse?.title || ''}`" size="640px">
      <div class="chapter-toolbar">
        <el-button type="primary" size="small" @click="showAddChapter">
          <el-icon><Plus /></el-icon>
          添加章节
        </el-button>
      </div>
      <el-table :data="chapters" v-loading="chapterLoading" size="small">
        <el-table-column prop="order" label="序号" width="60" />
        <el-table-column prop="title" label="章节标题" min-width="160" />
        <el-table-column prop="duration" label="时长(分)" width="80" />
        <el-table-column prop="difficultyLevel" label="难度" width="80">
          <template #default="{ row }">
            {{ difficultyLabel(row.difficultyLevel) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="editChapter(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="deleteChapter(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- 章节表单 -->
    <el-dialog v-model="chapterDialogVisible" :title="chapterIsEdit ? '编辑章节' : '添加章节'" width="560px" append-to-body>
      <el-form ref="chapterFormRef" :model="chapterForm" :rules="chapterRules" label-width="90px">
        <el-form-item label="章节标题" prop="title">
          <el-input v-model="chapterForm.title" placeholder="请输入章节标题" />
        </el-form-item>
        <el-form-item label="排序" prop="order">
          <el-input-number v-model="chapterForm.order" :min="1" />
        </el-form-item>
        <el-form-item label="时长(分钟)">
          <el-input-number v-model="chapterForm.duration" :min="1" :step="5" />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="chapterForm.difficultyLevel" class="w-full">
            <el-option label="基础理论" :value="1" />
            <el-option label="案例分析" :value="2" />
            <el-option label="高级实操" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="章节内容">
          <el-input v-model="chapterForm.content" type="textarea" :rows="6" placeholder="支持 HTML 内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="chapterDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitChapter">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { adminApi } from '@/api/admin'
import type { Course, Chapter } from '@/types'

const loading = ref(false)
const chapterLoading = ref(false)
const courses = ref<Course[]>([])
const chapters = ref<Chapter[]>([])
const activeCourse = ref<Course | null>(null)

const courseDialogVisible = ref(false)
const chapterDrawerVisible = ref(false)
const chapterDialogVisible = ref(false)
const isEdit = ref(false)
const chapterIsEdit = ref(false)

const courseFormRef = ref<FormInstance>()
const chapterFormRef = ref<FormInstance>()

const categories = [
  { label: '基础知识', value: 'basic' },
  { label: '电池安全', value: 'battery' },
  { label: '热失控', value: 'thermal' },
  { label: '消防安全', value: 'fire' },
  { label: 'BMS管理', value: 'bms' },
  { label: '事故案例', value: 'case' },
]

const courseForm = reactive({
  id: '',
  title: '',
  description: '',
  category: 'basic',
  totalDuration: 60,
  status: 'published',
})

const chapterForm = reactive({
  id: '',
  title: '',
  content: '',
  duration: 30,
  order: 1,
  difficultyLevel: 1,
})

const courseRules: FormRules = {
  title: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

const chapterRules: FormRules = {
  title: [{ required: true, message: '请输入章节标题', trigger: 'blur' }],
  order: [{ required: true, message: '请输入排序', trigger: 'blur' }],
}

function categoryLabel(c: string) {
  return categories.find(x => x.value === c)?.label || c
}

function difficultyLabel(level?: number) {
  const map: Record<number, string> = { 1: '基础', 2: '案例', 3: '实操' }
  return map[level ?? 1] || '-'
}

async function loadCourses() {
  loading.value = true
  try {
    const res = await adminApi.getCourses()
    courses.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function loadChapters(courseId: string) {
  chapterLoading.value = true
  try {
    const res = await adminApi.getChapters(courseId)
    chapters.value = res.data || []
  } finally {
    chapterLoading.value = false
  }
}

function showAddCourse() {
  isEdit.value = false
  courseForm.id = ''
  courseForm.title = ''
  courseForm.description = ''
  courseForm.category = 'basic'
  courseForm.totalDuration = 60
  courseForm.status = 'published'
  courseDialogVisible.value = true
}

function editCourse(row: Course) {
  isEdit.value = true
  courseForm.id = row.id
  courseForm.title = row.title
  courseForm.description = row.description || ''
  courseForm.category = row.category
  courseForm.totalDuration = row.totalDuration || 0
  courseForm.status = row.status || 'published'
  courseDialogVisible.value = true
}

async function submitCourse() {
  const valid = await courseFormRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload = {
    title: courseForm.title,
    description: courseForm.description,
    category: courseForm.category,
    totalDuration: courseForm.totalDuration,
    status: courseForm.status,
  }

  if (isEdit.value) {
    await adminApi.updateCourse(courseForm.id, payload)
  } else {
    await adminApi.createCourse(payload)
  }

  courseDialogVisible.value = false
  await loadCourses()
  ElMessage.success(isEdit.value ? '编辑成功' : '添加成功')
}

async function deleteCourse(row: Course) {
  await ElMessageBox.confirm(`确定删除课程「${row.title}」吗？关联章节将一并删除。`, '提示', { type: 'warning' })
  await adminApi.deleteCourse(row.id)
  await loadCourses()
  ElMessage.success('删除成功')
}

async function openChapters(row: Course) {
  activeCourse.value = row
  chapterDrawerVisible.value = true
  await loadChapters(row.id)
}

function showAddChapter() {
  if (!activeCourse.value) return
  chapterIsEdit.value = false
  chapterForm.id = ''
  chapterForm.title = ''
  chapterForm.content = ''
  chapterForm.duration = 30
  chapterForm.order = chapters.value.length + 1
  chapterForm.difficultyLevel = 1
  chapterDialogVisible.value = true
}

function editChapter(row: Chapter) {
  chapterIsEdit.value = true
  chapterForm.id = row.id
  chapterForm.title = row.title
  chapterForm.content = row.content || ''
  chapterForm.duration = row.duration
  chapterForm.order = row.order
  chapterForm.difficultyLevel = row.difficultyLevel ?? 1
  chapterDialogVisible.value = true
}

async function submitChapter() {
  if (!activeCourse.value) return
  const valid = await chapterFormRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload = {
    title: chapterForm.title,
    content: chapterForm.content,
    duration: chapterForm.duration,
    orderNum: chapterForm.order,
    difficultyLevel: chapterForm.difficultyLevel,
  }

  if (chapterIsEdit.value) {
    await adminApi.updateChapter(activeCourse.value.id, chapterForm.id, payload)
  } else {
    await adminApi.createChapter(activeCourse.value.id, payload)
  }

  chapterDialogVisible.value = false
  await loadChapters(activeCourse.value.id)
  await loadCourses()
  ElMessage.success(chapterIsEdit.value ? '编辑成功' : '添加成功')
}

async function deleteChapter(row: Chapter) {
  if (!activeCourse.value) return
  await ElMessageBox.confirm(`确定删除章节「${row.title}」吗？`, '提示', { type: 'warning' })
  await adminApi.deleteChapter(activeCourse.value.id, row.id)
  await loadChapters(activeCourse.value.id)
  await loadCourses()
  ElMessage.success('删除成功')
}

onMounted(loadCourses)
</script>

<style scoped>
.chapter-toolbar {
  margin-bottom: 12px;
}

.w-full {
  width: 100%;
}
</style>
