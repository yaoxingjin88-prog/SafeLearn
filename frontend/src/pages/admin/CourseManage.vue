<template>
  <div class="course-manage">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-2xl font-bold">课程管理</h2>
      <el-button type="primary" @click="showAddCourse">
        <el-icon><Plus /></el-icon>
        添加课程
      </el-button>
    </div>

    <el-card>
      <el-table :data="courses" v-loading="loading" style="width: 100%">
        <el-table-column prop="title" label="课程名称" min-width="180" />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag>{{ getCategoryName(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chapterCount" label="章节数" width="90" />
        <el-table-column prop="totalDuration" label="时长(分钟)" width="110" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'published' ? 'success' : 'info'">
              {{ row.status === 'published' ? '已发布' : row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openChapters(row)">章节</el-button>
            <el-button type="primary" link @click="editCourse(row)">编辑</el-button>
            <el-button type="danger" link @click="removeCourse(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 课程对话框 -->
    <el-dialog v-model="courseDialogVisible" :title="isEditCourse ? '编辑课程' : '添加课程'" width="560px">
      <el-form ref="courseFormRef" :model="courseForm" :rules="courseRules" label-width="90px">
        <el-form-item label="课程名称" prop="title">
          <el-input v-model="courseForm.title" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="courseForm.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="基础知识" value="basic" />
            <el-option label="锂电池" value="battery" />
            <el-option label="热失控" value="thermal" />
            <el-option label="消防安全" value="fire" />
            <el-option label="BMS系统" value="bms" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="courseForm.status" style="width: 100%">
            <el-option label="已发布" value="published" />
            <el-option label="草稿" value="draft" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面地址">
          <el-input v-model="courseForm.coverImage" placeholder="可选，图片 URL" />
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

    <!-- 章节抽屉 -->
    <el-drawer v-model="chapterDrawerVisible" :title="`${currentCourse?.title || ''} - 章节管理`" size="720px">
      <div class="flex justify-end mb-4">
        <el-button type="primary" @click="showAddChapter">
          <el-icon><Plus /></el-icon>
          添加章节
        </el-button>
      </div>
      <el-table :data="chapters" v-loading="chapterLoading">
        <el-table-column prop="orderNum" label="序号" width="70" />
        <el-table-column prop="title" label="章节标题" min-width="160" />
        <el-table-column prop="duration" label="时长" width="80" />
        <el-table-column prop="difficultyLevel" label="难度" width="80" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button type="primary" link @click="editChapter(row)">编辑</el-button>
            <el-button type="danger" link @click="removeChapter(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- 章节对话框 -->
    <el-dialog v-model="chapterDialogVisible" :title="isEditChapter ? '编辑章节' : '添加章节'" width="640px">
      <el-form ref="chapterFormRef" :model="chapterForm" :rules="chapterRules" label-width="90px">
        <el-form-item label="章节标题" prop="title">
          <el-input v-model="chapterForm.title" placeholder="请输入章节标题" />
        </el-form-item>
        <el-form-item label="序号">
          <el-input-number v-model="chapterForm.orderNum" :min="1" />
        </el-form-item>
        <el-form-item label="时长">
          <el-input-number v-model="chapterForm.duration" :min="1" />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="chapterForm.difficultyLevel" style="width: 100%">
            <el-option label="初级" :value="1" />
            <el-option label="中级" :value="2" />
            <el-option label="高级" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="章节内容">
          <el-input v-model="chapterForm.content" type="textarea" :rows="8" placeholder="支持 HTML 内容" />
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
const currentCourse = ref<Course | null>(null)

const courseDialogVisible = ref(false)
const chapterDrawerVisible = ref(false)
const chapterDialogVisible = ref(false)
const isEditCourse = ref(false)
const isEditChapter = ref(false)

const courseFormRef = ref<FormInstance>()
const chapterFormRef = ref<FormInstance>()

const courseForm = reactive<{
  id: string
  title: string
  description: string
  coverImage: string
  category: Course['category']
  status: string
}>({
  id: '',
  title: '',
  description: '',
  coverImage: '',
  category: 'basic',
  status: 'published',
})

const chapterForm = reactive({
  id: '',
  title: '',
  content: '',
  duration: 30,
  orderNum: 1,
  difficultyLevel: 1,
})

const courseRules: FormRules = {
  title: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

const chapterRules: FormRules = {
  title: [{ required: true, message: '请输入章节标题', trigger: 'blur' }],
}

function getCategoryName(category: string) {
  const map: Record<string, string> = {
    basic: '基础知识',
    battery: '锂电池',
    thermal: '热失控',
    fire: '消防安全',
    bms: 'BMS系统',
    case: '事故案例',
  }
  return map[category] || category
}

async function loadCourses() {
  loading.value = true
  try {
    const res = await adminApi.getCourses()
    courses.value = res.data as Course[]
  } finally {
    loading.value = false
  }
}

async function loadChapters() {
  if (!currentCourse.value) return
  chapterLoading.value = true
  try {
    const res = await adminApi.getChapters(currentCourse.value.id)
    chapters.value = res.data
  } finally {
    chapterLoading.value = false
  }
}

function showAddCourse() {
  isEditCourse.value = false
  Object.assign(courseForm, {
    id: '', title: '', description: '', coverImage: '', category: 'basic', status: 'published',
  })
  courseDialogVisible.value = true
}

function editCourse(row: Course) {
  isEditCourse.value = true
  Object.assign(courseForm, {
    id: row.id,
    title: row.title,
    description: row.description,
    coverImage: row.coverImage,
    category: row.category,
    status: (row as any).status || 'published',
  })
  courseDialogVisible.value = true
}

async function submitCourse() {
  await courseFormRef.value?.validate()
  const payload = {
    title: courseForm.title,
    description: courseForm.description,
    coverImage: courseForm.coverImage,
    category: courseForm.category,
    status: courseForm.status,
  }
  if (isEditCourse.value) {
    await adminApi.updateCourse(courseForm.id, payload)
    ElMessage.success('课程已更新')
  } else {
    await adminApi.createCourse(payload)
    ElMessage.success('课程已创建')
  }
  courseDialogVisible.value = false
  loadCourses()
}

async function removeCourse(row: Course) {
  await ElMessageBox.confirm(`确定删除课程「${row.title}」吗？`, '提示', { type: 'warning' })
  await adminApi.deleteCourse(row.id)
  ElMessage.success('删除成功')
  loadCourses()
}

function openChapters(row: Course) {
  currentCourse.value = row
  chapterDrawerVisible.value = true
  loadChapters()
}

function showAddChapter() {
  isEditChapter.value = false
  Object.assign(chapterForm, {
    id: '',
    title: '',
    content: '',
    duration: 30,
    orderNum: chapters.value.length + 1,
    difficultyLevel: 1,
  })
  chapterDialogVisible.value = true
}

function editChapter(row: Chapter) {
  isEditChapter.value = true
  Object.assign(chapterForm, {
    id: row.id,
    title: row.title,
    content: row.content,
    duration: row.duration,
    orderNum: (row as any).orderNum || row.order,
    difficultyLevel: row.difficultyLevel || 1,
  })
  chapterDialogVisible.value = true
}

async function submitChapter() {
  if (!currentCourse.value) return
  await chapterFormRef.value?.validate()
  const payload = {
    title: chapterForm.title,
    content: chapterForm.content,
    duration: chapterForm.duration,
    orderNum: chapterForm.orderNum,
    difficultyLevel: chapterForm.difficultyLevel,
  }
  if (isEditChapter.value) {
    await adminApi.updateChapter(currentCourse.value.id, chapterForm.id, payload)
    ElMessage.success('章节已更新')
  } else {
    await adminApi.createChapter(currentCourse.value.id, payload)
    ElMessage.success('章节已创建')
  }
  chapterDialogVisible.value = false
  loadChapters()
  loadCourses()
}

async function removeChapter(row: Chapter) {
  if (!currentCourse.value) return
  await ElMessageBox.confirm(`确定删除章节「${row.title}」吗？`, '提示', { type: 'warning' })
  await adminApi.deleteChapter(currentCourse.value.id, row.id)
  ElMessage.success('删除成功')
  loadChapters()
  loadCourses()
}

onMounted(loadCourses)
</script>

<style scoped>
.course-manage {
  width: 100%;
  min-height: 100%;
  padding: 20px;
  box-sizing: border-box;
}
</style>
