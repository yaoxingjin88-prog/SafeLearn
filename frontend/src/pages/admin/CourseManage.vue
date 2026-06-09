<template>
  <div class="sl-page course-manage">
    <div class="sl-page-header">
      <div>
        <h2 class="sl-page-title">学员课程配置</h2>
        <p class="page-desc">配置学员端「安全培训」模块的课程、章节与学习路径，发布后学员即可在课程列表中学习。</p>
      </div>
      <div class="header-actions">
        <el-button @click="openUserPreview('/user/courses/list')">
          <el-icon class="mr-1"><View /></el-icon>
          预览学员端
        </el-button>
        <el-button v-if="activeTab === 'courses'" type="primary" @click="showAddCourse">
          <el-icon><Plus /></el-icon>
          添加课程
        </el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="config-tabs">
      <el-tab-pane label="课程配置" name="courses" />
      <el-tab-pane label="分类配置" name="categories" />
    </el-tabs>

    <CategoryConfigPanel
      v-if="activeTab === 'categories'"
      @changed="loadCategories"
    />

    <el-card v-else v-loading="loading">
      <el-table :data="courses" style="width: 100%">
        <el-table-column prop="title" label="课程名称" min-width="200" />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag :type="categoryTagType(row.category)">{{ categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chapterCount" label="章节数" width="90" />
        <el-table-column prop="totalDuration" label="时长(分钟)" width="110" />
        <el-table-column prop="status" label="发布状态" width="120">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 'published'"
              active-text="已发布"
              inactive-text="草稿"
              inline-prompt
              @change="(val: boolean) => togglePublish(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openChapters(row)">章节</el-button>
            <el-button type="primary" link @click="openUserPreview(`/user/courses/${row.id}`)">预览</el-button>
            <el-button type="primary" link @click="editCourse(row)">编辑</el-button>
            <el-button type="danger" link @click="deleteCourse(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 课程表单 -->
    <el-dialog v-model="courseDialogVisible" :title="isEdit ? '编辑课程' : '添加课程'" width="600px">
      <el-form ref="courseFormRef" :model="courseForm" :rules="courseRules" label-width="100px">
        <el-form-item label="课程名称" prop="title">
          <el-input v-model="courseForm.title" placeholder="学员端显示的课程标题" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="courseForm.category" placeholder="请选择分类" class="w-full">
            <el-option
              v-for="c in enabledCategories"
              :key="c.code"
              :label="c.name"
              :value="c.code"
            />
          </el-select>
          <div v-if="!enabledCategories.length" class="field-tip">
            请先在「分类配置」中添加并启用分类
          </div>
        </el-form-item>
        <el-form-item label="发布状态" prop="status">
          <el-radio-group v-model="courseForm.status">
            <el-radio value="published">已发布（学员可见）</el-radio>
            <el-radio value="draft">草稿（仅管理员可见）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="封面图 URL">
          <el-input v-model="courseForm.coverImage" placeholder="可选，学员端课程卡片封面" />
        </el-form-item>
        <el-form-item label="时长(分钟)">
          <el-input-number v-model="courseForm.totalDuration" :min="0" :step="5" />
          <span class="field-tip">保存章节后会自动汇总，也可手动填写</span>
        </el-form-item>
        <el-form-item label="课程简介">
          <el-input v-model="courseForm.description" type="textarea" :rows="4" placeholder="学员端课程详情页展示" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="courseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCourse">确定</el-button>
      </template>
    </el-dialog>

    <!-- 章节管理 -->
    <el-drawer v-model="chapterDrawerVisible" :title="`章节配置 — ${activeCourse?.title || ''}`" size="720px">
      <div class="chapter-toolbar">
        <el-button type="primary" size="small" @click="showAddChapter">
          <el-icon><Plus /></el-icon>
          添加章节
        </el-button>
        <el-button size="small" @click="openUserPreview(`/user/courses/${activeCourse?.id}`)">
          预览课程
        </el-button>
      </div>
      <el-table :data="chapters" v-loading="chapterLoading" size="small">
        <el-table-column prop="order" label="序号" width="60" />
        <el-table-column prop="title" label="章节标题" min-width="160" />
        <el-table-column prop="duration" label="时长(分)" width="80" />
        <el-table-column prop="difficultyLevel" label="难度" width="90">
          <template #default="{ row }">
            {{ difficultyLabel(row.difficultyLevel) }}
          </template>
        </el-table-column>
        <el-table-column label="前置章节" min-width="120">
          <template #default="{ row }">
            <span v-if="!row.prerequisiteIds?.length" class="text-muted">无</span>
            <el-tag v-else size="small" type="info">{{ row.prerequisiteIds.length }} 个前置</el-tag>
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
    <el-dialog v-model="chapterDialogVisible" :title="chapterIsEdit ? '编辑章节' : '添加章节'" width="640px" append-to-body>
      <el-form ref="chapterFormRef" :model="chapterForm" :rules="chapterRules" label-width="100px">
        <el-form-item label="章节标题" prop="title">
          <el-input v-model="chapterForm.title" placeholder="学员端章节列表显示名称" />
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
        <el-form-item label="前置章节">
          <el-select
            v-model="chapterForm.prerequisiteIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="学完前置章节后解锁本章节"
            class="w-full"
          >
            <el-option
              v-for="ch in prerequisiteOptions"
              :key="ch.id"
              :label="`${ch.order}. ${ch.title}`"
              :value="ch.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="视频地址">
          <el-input v-model="chapterForm.videoUrl" placeholder="可选，章节视频 URL" />
        </el-form-item>
        <el-form-item label="章节内容">
          <el-input v-model="chapterForm.content" type="textarea" :rows="8" placeholder="支持 HTML，学员端章节学习页展示" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { adminApi } from '@/api/admin'
import CategoryConfigPanel from '@/components/admin/CategoryConfigPanel.vue'
import type { Course, Chapter, CourseCategory } from '@/types'

const activeTab = ref('courses')
const categoryList = ref<CourseCategory[]>([])
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

const enabledCategories = computed(() => categoryList.value.filter(c => c.enabled))

const courseForm = reactive({
  id: '',
  title: '',
  description: '',
  coverImage: '',
  category: '',
  totalDuration: 60,
  status: 'published',
})

const chapterForm = reactive({
  id: '',
  title: '',
  content: '',
  videoUrl: '',
  duration: 30,
  order: 1,
  difficultyLevel: 1,
  prerequisiteIds: [] as string[],
})

const courseRules: FormRules = {
  title: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

const chapterRules: FormRules = {
  title: [{ required: true, message: '请输入章节标题', trigger: 'blur' }],
  order: [{ required: true, message: '请输入排序', trigger: 'blur' }],
}

const prerequisiteOptions = computed(() =>
  chapters.value.filter(ch => ch.id !== chapterForm.id),
)

function categoryLabel(code: string) {
  return categoryList.value.find(x => x.code === code)?.name || code
}

function categoryTagType(code: string) {
  const tag = categoryList.value.find(x => x.code === code)?.tagType
  return tag || undefined
}

async function loadCategories() {
  const res = await adminApi.getCourseCategories()
  categoryList.value = res.data || []
}

function difficultyLabel(level?: number) {
  const map: Record<number, string> = { 1: '基础', 2: '案例', 3: '实操' }
  return map[level ?? 1] || '-'
}

function openUserPreview(path: string) {
  window.open(path, '_blank')
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
  courseForm.coverImage = ''
  courseForm.category = enabledCategories.value[0]?.code || ''
  courseForm.totalDuration = 60
  courseForm.status = 'published'
  courseDialogVisible.value = true
}

function editCourse(row: Course) {
  isEdit.value = true
  courseForm.id = row.id
  courseForm.title = row.title
  courseForm.description = row.description || ''
  courseForm.coverImage = row.coverImage || ''
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
    coverImage: courseForm.coverImage || undefined,
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

async function togglePublish(row: Course, published: boolean) {
  const status = published ? 'published' : 'draft'
  await adminApi.updateCourse(row.id, { status })
  row.status = status
  ElMessage.success(published ? '已发布到学员端' : '已转为草稿')
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
  chapterForm.videoUrl = ''
  chapterForm.duration = 30
  chapterForm.order = chapters.value.length + 1
  chapterForm.difficultyLevel = 1
  chapterForm.prerequisiteIds = []
  chapterDialogVisible.value = true
}

function editChapter(row: Chapter) {
  chapterIsEdit.value = true
  chapterForm.id = row.id
  chapterForm.title = row.title
  chapterForm.content = row.content || ''
  chapterForm.videoUrl = row.videoUrl || ''
  chapterForm.duration = row.duration
  chapterForm.order = row.order
  chapterForm.difficultyLevel = row.difficultyLevel ?? 1
  chapterForm.prerequisiteIds = [...(row.prerequisiteIds || [])]
  chapterDialogVisible.value = true
}

async function submitChapter() {
  if (!activeCourse.value) return
  const valid = await chapterFormRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload = {
    title: chapterForm.title,
    content: chapterForm.content,
    videoUrl: chapterForm.videoUrl || undefined,
    duration: chapterForm.duration,
    orderNum: chapterForm.order,
    difficultyLevel: chapterForm.difficultyLevel,
    prerequisiteIds: chapterForm.prerequisiteIds,
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

onMounted(async () => {
  await loadCategories()
  await loadCourses()
})
</script>

<style scoped>
.page-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.chapter-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.field-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #9ca3af;
}

.text-muted {
  color: #9ca3af;
  font-size: 12px;
}

.w-full {
  width: 100%;
}

.mr-1 {
  margin-right: 4px;
}

.config-tabs {
  margin-bottom: 16px;
}

.config-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
</style>
