<template>
  <div class="course-edit-page" v-loading="loading">
    <div class="edit-header">
      <div>
        <h2>{{ isCreate ? '新建课程' : '编辑课程' }}</h2>
        <p>{{ courseForm.title || '请完善课程信息并配置章节内容' }}</p>
      </div>
      <div class="header-actions">
        <el-button @click="saveDraft">保存草稿</el-button>
        <el-button type="primary" @click="publishCourse">发布课程</el-button>
      </div>
    </div>

    <el-steps :active="currentStep" align-center class="edit-steps">
      <el-step title="基本信息" />
      <el-step title="章节内容" />
      <el-step title="课程设置" />
      <el-step title="预览与发布" />
    </el-steps>

    <div v-show="currentStep === 0" class="step-panel">
      <el-form ref="basicFormRef" :model="courseForm" :rules="basicRules" label-width="100px" class="basic-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="课程名称" prop="title">
              <el-input v-model="courseForm.title" placeholder="学员端显示的课程标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="副标题">
              <el-input v-model="courseForm.subtitle" placeholder="课程卡片副标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程分类" prop="category">
              <el-select v-model="courseForm.category" placeholder="请选择分类" class="w-full">
                <el-option v-for="c in enabledCategories" :key="c.code" :label="c.name" :value="c.code" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="适用对象">
              <el-input v-model="courseForm.targetAudience" placeholder="如：全体员工" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="讲师">
              <el-input v-model="courseForm.instructor" placeholder="讲师姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="讲师职称">
              <el-input v-model="courseForm.instructorTitle" placeholder="如：高级工程师" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面图 URL">
              <el-input v-model="courseForm.coverImage" placeholder="学员端课程封面地址" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="课程简介">
              <el-input v-model="courseForm.description" type="textarea" :rows="4" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <div v-show="currentStep === 1" class="step-panel chapter-layout">
      <aside class="chapter-sidebar">
        <div class="sidebar-head">
          <strong>章节目录</strong>
          <el-button type="primary" link @click="addChapter">+ 新建章节</el-button>
        </div>
        <ul class="chapter-menu">
          <li
            v-for="chapter in chapters"
            :key="chapter.id"
            :class="{ active: activeChapterId === chapter.id }"
            @click="selectChapter(chapter.id)"
          >
            <span>{{ String(chapter.order || chapter.orderNum || 0).padStart(2, '0') }}</span>
            <div>
              <strong>{{ chapter.title }}</strong>
              <small>{{ formatDuration(chapter.duration) }}</small>
            </div>
          </li>
        </ul>
      </aside>

      <section v-if="chapterForm.id || chapterForm._tempId" class="chapter-editor">
        <h3>{{ chapterForm.title || '章节编辑' }}</h3>
        <el-form label-width="96px">
          <el-form-item label="课时名称">
            <el-input v-model="chapterForm.title" />
          </el-form-item>
          <el-form-item label="课时时长">
            <el-input-number v-model="chapterForm.duration" :min="1" /> <span class="unit">分钟</span>
          </el-form-item>
          <el-form-item label="内容类型">
            <div class="content-types">
              <button
                v-for="type in contentTypes"
                :key="type.value"
                type="button"
                :class="{ active: chapterForm.contentType === type.value }"
                @click="chapterForm.contentType = type.value"
              >
                {{ type.label }}
              </button>
            </div>
          </el-form-item>
          <el-form-item v-if="chapterForm.contentType === 'video'" label="视频资源">
            <el-input v-model="chapterForm.videoUrl" placeholder="视频 URL" />
          </el-form-item>
          <el-form-item v-else label="章节内容">
            <el-input v-model="chapterForm.content" type="textarea" :rows="8" placeholder="支持 HTML，学员端章节学习页展示" />
          </el-form-item>
          <el-form-item label="课时简介">
            <el-input v-model="chapterForm.summary" type="textarea" :rows="3" maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item label="课时设置">
            <el-checkbox v-model="chapterForm.required">设置为必修课时</el-checkbox>
            <el-checkbox v-model="chapterForm.allowDownload">允许学员下载</el-checkbox>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveChapter">保存章节</el-button>
            <el-button type="danger" plain @click="removeChapter">删除章节</el-button>
          </el-form-item>
        </el-form>
      </section>
      <el-empty v-else description="请先新建章节" />
    </div>

    <div v-show="currentStep === 2" class="step-panel">
      <el-form label-width="110px" class="settings-form">
        <el-form-item label="课程目标">
          <div class="list-editor">
            <div v-for="(item, index) in courseForm.objectives" :key="index" class="list-row">
              <el-input v-model="courseForm.objectives[index]" />
              <el-button link type="danger" @click="courseForm.objectives.splice(index, 1)">删除</el-button>
            </div>
            <el-button link type="primary" @click="courseForm.objectives.push('')">+ 添加目标</el-button>
          </div>
        </el-form-item>
        <el-form-item label="关键知识点">
          <div class="list-editor">
            <div v-for="(item, index) in courseForm.knowledgePoints" :key="index" class="list-row">
              <el-input v-model="courseForm.knowledgePoints[index]" />
              <el-button link type="danger" @click="courseForm.knowledgePoints.splice(index, 1)">删除</el-button>
            </div>
            <el-button link type="primary" @click="courseForm.knowledgePoints.push('')">+ 添加知识点</el-button>
          </div>
        </el-form-item>
        <el-form-item label="课程标签">
          <el-select v-model="courseForm.tags" multiple filterable allow-create default-first-option class="w-full">
            <el-option v-for="tag in courseForm.tags" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用部门">
          <el-select v-model="courseForm.targetDepartments" multiple class="w-full">
            <el-option v-for="dept in DEPARTMENT_OPTIONS.filter(d => d !== '全部部门')" :key="dept" :label="dept" :value="dept" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用岗位">
          <el-select v-model="courseForm.targetRoles" multiple filterable allow-create default-first-option class="w-full">
            <el-option v-for="role in defaultRoles" :key="role" :label="role" :value="role" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <div v-show="currentStep === 3" class="step-panel preview-panel">
      <div class="preview-card">
        <img :src="coverUrl(courseForm.coverImage)" alt="" />
        <div>
          <h3>{{ courseForm.title || '未命名课程' }}</h3>
          <p>{{ courseForm.subtitle || courseForm.description || '暂无简介' }}</p>
          <div class="preview-meta">
            <span>分类：{{ categoryLabel(courseForm.category, categories) }}</span>
            <span>章节：{{ chapters.length }} 章</span>
            <span>时长：{{ formatDuration(totalDuration) }}</span>
            <span>讲师：{{ courseForm.instructor || '-' }}</span>
          </div>
        </div>
      </div>
      <el-alert type="info" show-icon :closable="false" title="确认信息无误后，可点击右上角「发布课程」同步到学员端。" />
    </div>

    <div class="step-footer">
      <el-button v-if="currentStep > 0" @click="currentStep -= 1">上一步</el-button>
      <el-button v-if="currentStep < 3" type="primary" @click="nextStep">下一步</el-button>
      <el-button v-else type="primary" @click="publishCourse">确认发布</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { adminApi, type AdminChapterDetail, type AdminCoursePayload } from '@/api/admin'
import type { CourseCategory } from '@/types'
import { DEPARTMENT_OPTIONS, categoryLabel, coverUrl, formatDuration } from './courseAdminShared'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const currentStep = ref(0)
const categories = ref<CourseCategory[]>([])
const courseId = ref('')
const chapters = ref<AdminChapterDetail[]>([])
const activeChapterId = ref('')
const basicFormRef = ref<FormInstance>()

const isCreate = computed(() => route.path.endsWith('/new'))
const enabledCategories = computed(() => categories.value.filter(c => c.enabled))
const totalDuration = computed(() => chapters.value.reduce((sum, ch) => sum + (ch.duration || 0), 0))
const defaultRoles = ['运维人员', '巡检人员', '应急小组', '全体员工']

const contentTypes = [
  { label: '视频', value: 'video' },
  { label: '图文', value: 'html' },
  { label: 'PPT', value: 'ppt' },
  { label: '试题', value: 'quiz' },
  { label: '实操演练', value: 'practice' },
]

const courseForm = reactive({
  title: '',
  subtitle: '',
  description: '',
  coverImage: '',
  category: '',
  status: 'draft',
  instructor: '',
  instructorTitle: '',
  targetAudience: '全体员工',
  objectives: [''] as string[],
  knowledgePoints: [''] as string[],
  tags: [] as string[],
  targetDepartments: [] as string[],
  targetRoles: [] as string[],
})

const chapterForm = reactive({
  id: '',
  _tempId: '',
  title: '',
  content: '',
  videoUrl: '',
  summary: '',
  contentType: 'html',
  required: true,
  allowDownload: false,
  duration: 30,
  order: 1,
})

const basicRules: FormRules = {
  title: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择课程分类', trigger: 'change' }],
}

function buildCoursePayload(status?: string): AdminCoursePayload {
  return {
    title: courseForm.title,
    subtitle: courseForm.subtitle,
    description: courseForm.description,
    coverImage: courseForm.coverImage || undefined,
    category: courseForm.category,
    status: status || courseForm.status,
    totalDuration: totalDuration.value,
    instructor: courseForm.instructor,
    instructorTitle: courseForm.instructorTitle,
    targetAudience: courseForm.targetAudience,
    objectives: courseForm.objectives.filter(Boolean),
    knowledgePoints: courseForm.knowledgePoints.filter(Boolean),
    tags: courseForm.tags,
    targetDepartments: courseForm.targetDepartments,
    targetRoles: courseForm.targetRoles,
  }
}

async function ensureCourseSaved(status = 'draft') {
  const payload = buildCoursePayload(status)
  if (courseId.value) {
    await adminApi.updateCourse(courseId.value, payload)
  } else {
    const res = await adminApi.createCourse(payload)
    courseId.value = res.data.id
    if (!isCreate.value) return
    await router.replace(`/admin/learning/courses/${courseId.value}/edit`)
  }
}

async function loadData() {
  loading.value = true
  try {
    const catRes = await adminApi.getCourseCategories()
    categories.value = catRes.data || []
    if (!isCreate.value) {
      const res = await adminApi.getCourseById(route.params.id as string)
      courseId.value = res.data.id
      Object.assign(courseForm, {
        title: res.data.title,
        subtitle: res.data.subtitle || '',
        description: res.data.description || '',
        coverImage: res.data.coverImage || '',
        category: res.data.category,
        status: res.data.status || 'draft',
        instructor: res.data.instructor || '',
        instructorTitle: res.data.instructorTitle || '',
        targetAudience: res.data.targetAudience || '全体员工',
        objectives: res.data.objectives?.length ? [...res.data.objectives] : [''],
        knowledgePoints: res.data.knowledgePoints?.length ? [...res.data.knowledgePoints] : [''],
        tags: res.data.tags || [],
        targetDepartments: res.data.targetDepartments || [],
        targetRoles: res.data.targetRoles || [],
      })
      chapters.value = res.data.chapters || []
      if (chapters.value.length) selectChapter(chapters.value[0].id)
    } else {
      courseForm.category = enabledCategories.value[0]?.code || ''
    }
  } finally {
    loading.value = false
  }
}

function selectChapter(id: string) {
  activeChapterId.value = id
  const chapter = chapters.value.find(item => item.id === id)
  if (!chapter) return
  Object.assign(chapterForm, {
    id: chapter.id,
    _tempId: '',
    title: chapter.title,
    content: chapter.content || '',
    videoUrl: chapter.videoUrl || '',
    summary: chapter.summary || '',
    contentType: chapter.contentType || 'html',
    required: chapter.required ?? true,
    allowDownload: chapter.allowDownload ?? false,
    duration: chapter.duration || 30,
    order: chapter.order || chapter.orderNum || 1,
  })
}

function addChapter() {
  const tempId = `temp-${Date.now()}`
  chapters.value.push({
    id: tempId,
    courseId: courseId.value,
    title: `第 ${chapters.value.length + 1} 章`,
    content: '',
    duration: 30,
    order: chapters.value.length + 1,
    contentType: 'html',
    required: true,
    allowDownload: false,
  })
  selectChapter(tempId)
}

async function saveChapter() {
  if (!chapterForm.title.trim()) {
    ElMessage.warning('请输入课时名称')
    return
  }
  await ensureCourseSaved('draft')
  const payload = {
    title: chapterForm.title,
    content: chapterForm.content,
    videoUrl: chapterForm.videoUrl || undefined,
    summary: chapterForm.summary,
    contentType: chapterForm.contentType,
    required: chapterForm.required,
    allowDownload: chapterForm.allowDownload,
    duration: chapterForm.duration,
    orderNum: chapterForm.order,
  }
  if (chapterForm.id.startsWith('temp-')) {
    const res = await adminApi.createChapter(courseId.value, payload)
    chapters.value = chapters.value.filter(ch => ch.id !== chapterForm.id)
    chapters.value.push(res.data)
    selectChapter(res.data.id)
  } else {
    const res = await adminApi.updateChapter(courseId.value, chapterForm.id, payload)
    const index = chapters.value.findIndex(ch => ch.id === chapterForm.id)
    if (index >= 0) chapters.value[index] = res.data
  }
  ElMessage.success('章节已保存')
}

async function removeChapter() {
  if (!chapterForm.id) return
  await ElMessageBox.confirm('确定删除该章节吗？', '提示', { type: 'warning' })
  if (!chapterForm.id.startsWith('temp-')) {
    await adminApi.deleteChapter(courseId.value, chapterForm.id)
  }
  chapters.value = chapters.value.filter(ch => ch.id !== chapterForm.id)
  chapterForm.id = ''
  chapterForm._tempId = ''
  activeChapterId.value = ''
  ElMessage.success('章节已删除')
}

async function saveDraft() {
  const valid = await basicFormRef.value?.validate().catch(() => false)
  if (!valid && currentStep.value === 0) return
  await ensureCourseSaved('draft')
  ElMessage.success('草稿已保存')
}

async function publishCourse() {
  const valid = await basicFormRef.value?.validate().catch(() => false)
  if (!valid) {
    currentStep.value = 0
    return
  }
  await ensureCourseSaved('published')
  ElMessage.success('课程已发布到学员端')
  router.push(`/admin/learning/courses/${courseId.value}`)
}

async function nextStep() {
  if (currentStep.value === 0) {
    const valid = await basicFormRef.value?.validate().catch(() => false)
    if (!valid) return
    await ensureCourseSaved('draft')
  }
  if (currentStep.value === 1) {
    await ensureCourseSaved('draft')
  }
  if (currentStep.value === 2) {
    await ensureCourseSaved('draft')
  }
  currentStep.value += 1
}

onMounted(loadData)
</script>

<style scoped>
.course-edit-page { padding: 20px 24px 24px; min-height: 100%; box-sizing: border-box; }
.edit-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 18px; }
.edit-header h2 { margin: 0; font-size: 18px; color: #1f2937; }
.edit-header p { margin: 6px 0 0; color: #6b7280; font-size: 13px; }
.header-actions { display: flex; gap: 10px; }
.edit-steps { margin-bottom: 24px; }
.step-panel { background: #fff; border: 1px solid #e8edf3; border-radius: 8px; padding: 20px; min-height: 420px; }
.basic-form, .settings-form { max-width: 960px; }
.w-full { width: 100%; }
.unit { margin-left: 8px; color: #9ca3af; }
.chapter-layout { display: grid; grid-template-columns: 280px 1fr; gap: 16px; min-height: 520px; }
.chapter-sidebar { border-right: 1px solid #edf0f4; padding-right: 12px; }
.sidebar-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.chapter-menu { list-style: none; padding: 0; margin: 0; }
.chapter-menu li { display: flex; gap: 10px; padding: 10px; border-radius: 8px; cursor: pointer; margin-bottom: 6px; }
.chapter-menu li.active { background: #eff6ff; }
.chapter-menu strong { display: block; font-size: 13px; color: #1f2937; }
.chapter-menu small { color: #9ca3af; font-size: 12px; }
.chapter-editor h3 { margin: 0 0 16px; font-size: 16px; }
.content-types { display: flex; flex-wrap: wrap; gap: 8px; }
.content-types button { border: 1px solid #dbeafe; background: #fff; color: #374151; border-radius: 8px; padding: 8px 12px; cursor: pointer; font-size: 12px; }
.content-types button.active { border-color: #2563eb; background: #eff6ff; color: #2563eb; }
.list-editor { width: 100%; }
.list-row { display: flex; gap: 8px; margin-bottom: 8px; }
.preview-panel { display: grid; gap: 16px; }
.preview-card { display: flex; gap: 16px; align-items: center; }
.preview-card img { width: 180px; height: 110px; object-fit: cover; border-radius: 8px; }
.preview-card h3 { margin: 0 0 8px; }
.preview-card p { margin: 0 0 12px; color: #6b7280; }
.preview-meta { display: flex; flex-wrap: wrap; gap: 16px; color: #4b5563; font-size: 13px; }
.step-footer { display: flex; justify-content: flex-end; gap: 10px; margin-top: 16px; }
@media (max-width: 960px) {
  .chapter-layout { grid-template-columns: 1fr; }
  .chapter-sidebar { border-right: 0; border-bottom: 1px solid #edf0f4; padding-right: 0; padding-bottom: 12px; }
}
</style>
