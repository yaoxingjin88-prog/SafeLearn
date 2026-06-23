<template>
  <div class="question-edit-page" v-loading="loading">
    <div class="page-head">
      <button type="button" class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </button>
      <h2>{{ isCreate ? '新建试题' : '编辑试题' }}</h2>
    </div>

    <div class="edit-layout">
      <section class="main-panel">
        <div class="form-row two-col">
          <div class="form-field required">
            <label>题目类型</label>
            <el-select v-model="form.type" class="w-full" @change="onTypeChange">
              <el-option v-for="item in QUESTION_TYPE_EDIT_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
          <div class="form-field">
            <label>难度</label>
            <el-select v-model="form.difficulty" class="w-full">
              <el-option v-for="item in DIFFICULTY_EDIT_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
        </div>

        <div class="form-field required">
          <label>所属分类</label>
          <el-select v-model="form.categoryId" placeholder="请选择分类" class="w-full">
            <el-option v-for="cat in categoryOptions" :key="cat.id" :label="cat.label" :value="cat.id" />
          </el-select>
        </div>

        <div class="form-field required">
          <label>知识标签</label>
          <div class="tag-editor">
            <el-tag
              v-for="tag in form.tags"
              :key="tag"
              closable
              class="tag-item"
              @close="removeTag(tag)"
            >{{ tag }}</el-tag>
            <el-input
              v-if="tagInputVisible"
              ref="tagInputRef"
              v-model="tagInput"
              size="small"
              class="tag-input"
              @keyup.enter="confirmTag"
              @blur="confirmTag"
            />
            <button v-else type="button" class="add-tag-btn" @click="showTagInput">+ 添加标签</button>
          </div>
        </div>

        <div class="form-field required">
          <div class="field-head">
            <label>题干</label>
            <span class="char-count">{{ form.content.length }}/500</span>
          </div>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入题干内容"
          />
        </div>

        <div v-if="showOptions" class="option-section">
          <div class="section-title">选项设置</div>
          <div v-for="(opt, index) in form.options" :key="opt.id" class="option-row">
            <span class="option-label">{{ optionLabel(index) }}</span>
            <el-radio
              v-if="form.type === 'single' || form.type === 'truefalse'"
              :model-value="singleCorrectId"
              :value="opt.id"
              class="option-radio"
              @change="setSingleCorrect(opt.id)"
            />
            <el-checkbox
              v-else-if="form.type === 'multiple'"
              v-model="opt.correct"
              class="option-radio"
            />
            <el-input v-model="opt.text" :placeholder="`请输入选项 ${optionLabel(index)}`" class="option-input" />
            <button
              v-if="form.type !== 'truefalse' && form.options.length > 2"
              type="button"
              class="icon-btn danger"
              title="删除选项"
              @click="removeOption(index)"
            >
              <el-icon><Delete /></el-icon>
            </button>
          </div>
          <button
            v-if="form.type !== 'truefalse' && form.options.length < 8"
            type="button"
            class="add-option-btn"
            @click="addOption"
          >+ 添加选项</button>
        </div>

        <div v-if="showOptions && form.type === 'single'" class="form-field">
          <label>正确答案</label>
          <el-radio-group v-model="singleCorrectId" class="answer-group" @change="setSingleCorrect">
            <el-radio v-for="(opt, index) in form.options" :key="opt.id" :value="opt.id">{{ optionLabel(index) }}</el-radio>
          </el-radio-group>
        </div>

        <div class="form-field">
          <div class="field-head">
            <label>答案解析</label>
            <span class="char-count">{{ form.explanation.length }}/1000</span>
          </div>
          <div class="rich-toolbar">
            <button type="button" @click="wrapExplanation('**')">B</button>
            <button type="button" @click="wrapExplanation('*')">I</button>
            <button type="button" @click="wrapExplanation('__')">U</button>
            <button type="button" @click="insertExplanationPrefix('- ')">•</button>
          </div>
          <el-input
            ref="explanationRef"
            v-model="form.explanation"
            type="textarea"
            :rows="5"
            maxlength="1000"
            placeholder="请输入答案解析"
          />
        </div>
      </section>

      <aside class="side-panel">
        <div class="side-card">
          <div class="side-title">附件与材料</div>
          <el-upload
            drag
            :auto-upload="false"
            :show-file-list="false"
            accept=".pdf,.doc,.docx,.ppt,.pptx,.jpg,.jpeg,.png"
            @change="onFileChange"
          >
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
            <div class="upload-text">点击或拖拽上传附件</div>
            <div class="upload-hint">支持 pdf / doc / ppt / jpg / png，单个不超过 10MB</div>
          </el-upload>
          <div v-if="form.attachments.files.length" class="file-list">
            <div v-for="(file, index) in form.attachments.files" :key="`${file.name}-${index}`" class="file-item">
              <el-icon><Document /></el-icon>
              <span>{{ file.name }}</span>
              <button type="button" class="icon-btn danger" @click="removeFile(index)"><el-icon><Close /></el-icon></button>
            </div>
          </div>
        </div>

        <div class="side-card">
          <div class="side-title">添加图片（可选）</div>
          <div class="image-grid">
            <div v-for="(img, index) in form.attachments.images" :key="`${img.name}-${index}`" class="image-item">
              <img v-if="img.url" :src="img.url" :alt="img.name" />
              <button type="button" class="image-remove" @click="removeImage(index)"><el-icon><Close /></el-icon></button>
            </div>
            <el-upload
              :auto-upload="false"
              :show-file-list="false"
              accept="image/*"
              class="image-upload"
              @change="onImageChange"
            >
              <button type="button" class="image-add">+</button>
            </el-upload>
          </div>
        </div>

        <div class="side-card">
          <div class="side-title">关联规范/文件（可选）</div>
          <div v-if="form.attachments.norms.length" class="file-list">
            <div v-for="(norm, index) in form.attachments.norms" :key="`${norm.name}-${index}`" class="file-item">
              <el-icon><Document /></el-icon>
              <span>{{ norm.name }}</span>
              <button type="button" class="icon-btn danger" @click="removeNorm(index)"><el-icon><Close /></el-icon></button>
            </div>
          </div>
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept=".pdf,.doc,.docx"
            @change="onNormChange"
          >
            <el-button class="w-full">+ 添加规范文件</el-button>
          </el-upload>
        </div>

        <div class="side-card">
          <div class="side-title">设置项</div>
          <el-checkbox v-model="form.settings.allowComments">允许评论</el-checkbox>
          <el-checkbox v-model="form.settings.allowReport">允许举报</el-checkbox>
          <el-checkbox v-model="form.settings.showAnalysisInExam">在考试中显示解析</el-checkbox>
        </div>
      </aside>
    </div>

    <div class="footer-actions">
      <el-button @click="goBack">取消</el-button>
      <el-button @click="saveDraft">保存草稿</el-button>
      <el-button type="primary" @click="saveAndPublish">保存并发布</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { InputInstance, UploadFile } from 'element-plus'
import { ArrowLeft, Close, Delete, Document, UploadFilled } from '@element-plus/icons-vue'
import { adminApi, type AdminQuestionCategoryNode } from '@/api/admin'
import {
  DIFFICULTY_EDIT_OPTIONS,
  QUESTION_TYPE_EDIT_OPTIONS,
  createDefaultOptions,
  defaultAttachments,
  defaultSettings,
  getSingleCorrectId,
  needsOptions,
  optionLabel,
  parseAttachments,
  parseOptions,
  parseSettings,
  syncSingleCorrect,
  type QuestionOptionItem,
} from './questionEditShared'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const categories = ref<AdminQuestionCategoryNode[]>([])
const tagInputVisible = ref(false)
const tagInput = ref('')
const tagInputRef = ref<InputInstance>()
const explanationRef = ref<InputInstance>()

const isCreate = computed(() => route.name === 'LearningQuestionCreate' || !route.params.id)

const form = reactive({
  categoryId: '',
  type: 'single',
  difficulty: 'medium',
  status: 'draft' as 'draft' | 'published' | 'disabled',
  content: '',
  tags: [] as string[],
  options: createDefaultOptions('single') as QuestionOptionItem[],
  explanation: '',
  attachments: defaultAttachments(),
  settings: defaultSettings(),
})

const showOptions = computed(() => needsOptions(form.type))

const singleCorrectId = computed({
  get: () => getSingleCorrectId(form.options),
  set: (id: string) => syncSingleCorrect(form.options, id),
})

const categoryOptions = computed(() => {
  const result: Array<{ id: string; label: string }> = []
  for (const node of categories.value) {
    if (node.id === 'all') continue
    if (node.children?.length) {
      for (const child of node.children) {
        result.push({ id: child.id, label: `${node.name} / ${child.name}` })
      }
    } else {
      result.push({ id: node.id, label: node.name })
    }
  }
  return result
})

function onTypeChange(type: string) {
  form.options = createDefaultOptions(type)
}

function setSingleCorrect(id: string) {
  syncSingleCorrect(form.options, id)
}

function addOption() {
  const nextIndex = form.options.length
  form.options.push({
    id: optionLabel(nextIndex).toLowerCase(),
    text: '',
    correct: false,
  })
}

function removeOption(index: number) {
  form.options.splice(index, 1)
  if (form.type === 'single') {
    const current = getSingleCorrectId(form.options)
    if (!form.options.some(opt => opt.id === current)) {
      syncSingleCorrect(form.options, form.options[0]?.id || '')
    }
  }
}

function removeTag(tag: string) {
  form.tags = form.tags.filter(t => t !== tag)
}

function showTagInput() {
  tagInputVisible.value = true
  nextTick(() => tagInputRef.value?.focus())
}

function confirmTag() {
  const value = tagInput.value.trim()
  if (value && !form.tags.includes(value)) {
    form.tags.push(value)
  }
  tagInputVisible.value = false
  tagInput.value = ''
}

function wrapExplanation(wrapper: string) {
  const el = explanationRef.value?.textarea
  if (!el) return
  const { selectionStart, selectionEnd, value } = el
  const selected = value.slice(selectionStart, selectionEnd) || '文本'
  const next = `${value.slice(0, selectionStart)}${wrapper}${selected}${wrapper}${value.slice(selectionEnd)}`
  form.explanation = next.slice(0, 1000)
}

function insertExplanationPrefix(prefix: string) {
  form.explanation = `${form.explanation}${form.explanation ? '\n' : ''}${prefix}`
}

function readUploadFile(file: UploadFile['raw'], maxMb = 10) {
  if (!file) return null
  if (file.size > maxMb * 1024 * 1024) {
    ElMessage.warning(`文件不能超过 ${maxMb}MB`)
    return null
  }
  return file
}

function onFileChange(uploadFile: UploadFile) {
  const file = readUploadFile(uploadFile.raw)
  if (!file) return
  form.attachments.files.push({ name: file.name, size: file.size, type: file.type })
}

function onNormChange(uploadFile: UploadFile) {
  const file = readUploadFile(uploadFile.raw)
  if (!file) return
  form.attachments.norms.push({ name: file.name, size: file.size, type: file.type })
}

function onImageChange(uploadFile: UploadFile) {
  const file = readUploadFile(uploadFile.raw)
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    form.attachments.images.push({
      name: file.name,
      size: file.size,
      type: file.type,
      url: String(reader.result || ''),
    })
  }
  reader.readAsDataURL(file)
}

function removeFile(index: number) {
  form.attachments.files.splice(index, 1)
}

function removeNorm(index: number) {
  form.attachments.norms.splice(index, 1)
}

function removeImage(index: number) {
  form.attachments.images.splice(index, 1)
}

function validateForm() {
  if (!form.categoryId) {
    ElMessage.warning('请选择所属分类')
    return false
  }
  if (!form.content.trim()) {
    ElMessage.warning('请输入题干')
    return false
  }
  if (!form.tags.length) {
    ElMessage.warning('请至少添加一个知识标签')
    return false
  }
  if (showOptions.value) {
    if (form.options.some(opt => !opt.text.trim())) {
      ElMessage.warning('请完善所有选项内容')
      return false
    }
    if (!form.options.some(opt => opt.correct)) {
      ElMessage.warning('请设置正确答案')
      return false
    }
  }
  return true
}

function buildPayload(status: 'draft' | 'published') {
  return {
    categoryId: form.categoryId,
    type: form.type,
    difficulty: form.difficulty,
    status,
    content: form.content.trim(),
    tags: form.tags,
    options: showOptions.value ? form.options : [],
    explanation: form.explanation.trim(),
    attachments: form.attachments,
    settings: form.settings,
  }
}

async function saveDraft() {
  if (!validateForm()) return
  loading.value = true
  try {
    if (isCreate.value) {
      const res = await adminApi.createQuestion(buildPayload('draft'))
      ElMessage.success('草稿已保存')
      router.replace(`/admin/learning/question-bank/${res.data.id}/edit`)
    } else {
      await adminApi.updateQuestion(String(route.params.id), buildPayload('draft'))
      ElMessage.success('草稿已保存')
    }
  } finally {
    loading.value = false
  }
}

async function saveAndPublish() {
  if (!validateForm()) return
  loading.value = true
  try {
    if (isCreate.value) {
      await adminApi.createQuestion(buildPayload('published'))
      ElMessage.success('试题已发布')
    } else {
      await adminApi.updateQuestion(String(route.params.id), buildPayload('published'))
      ElMessage.success('试题已发布')
    }
    router.push('/admin/learning/question-bank')
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/admin/learning/question-bank')
}

async function loadCategories() {
  const res = await adminApi.getQuestionCategories()
  categories.value = res.data || []
  if (!form.categoryId) {
    form.categoryId = categoryOptions.value[0]?.id || ''
  }
}

async function loadQuestion() {
  if (isCreate.value) return
  const res = await adminApi.getQuestionById(String(route.params.id))
  const data = res.data
  form.categoryId = data.categoryId
  form.type = data.type
  form.difficulty = data.difficulty
  form.status = data.status as typeof form.status
  form.content = data.content
  form.tags = [...(data.tags || [])]
  form.options = parseOptions(data.options)
  if (!form.options.length && needsOptions(form.type)) {
    form.options = createDefaultOptions(form.type)
  }
  form.explanation = data.explanation || ''
  form.attachments = parseAttachments(data.attachments)
  form.settings = parseSettings(data.settings)
}

onMounted(async () => {
  loading.value = true
  try {
    await loadCategories()
    await loadQuestion()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.question-edit-page {
  padding: 20px 24px 88px;
  min-height: 100%;
  box-sizing: border-box;
}

.page-head {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 0;
  background: none;
  color: #667085;
  cursor: pointer;
  font-size: 14px;
}

.page-head h2 {
  margin: 0;
  font-size: 20px;
  color: #1f2937;
}

.edit-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
  align-items: start;
}

.main-panel,
.side-card {
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 10px;
}

.main-panel {
  padding: 20px 24px;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 16px;
}

.side-card {
  padding: 16px;
}

.side-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
}

.form-row.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-field {
  margin-bottom: 18px;
}

.form-field label,
.field-head label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  color: #374151;
}

.form-field.required > label::before {
  content: '*';
  color: #ef4444;
  margin-right: 4px;
}

.field-head label::before {
  content: none;
}

.field-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.field-head label {
  margin-bottom: 0;
}

.char-count {
  font-size: 12px;
  color: #9ca3af;
}

.w-full {
  width: 100%;
}

.tag-editor {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  min-height: 36px;
  padding: 8px 10px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
}

.tag-item {
  margin: 0;
}

.tag-input {
  width: 120px;
}

.add-tag-btn,
.add-option-btn {
  border: 0;
  background: none;
  color: #2563eb;
  cursor: pointer;
  font-size: 13px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.option-label {
  width: 18px;
  font-weight: 600;
  color: #374151;
}

.option-radio {
  margin-right: 0;
}

.option-input {
  flex: 1;
}

.answer-group {
  display: flex;
  gap: 18px;
}

.rich-toolbar {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}

.rich-toolbar button {
  width: 28px;
  height: 28px;
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  color: #374151;
}

.upload-icon {
  font-size: 28px;
  color: #94a3b8;
  margin-bottom: 8px;
}

.upload-text {
  color: #374151;
  font-size: 13px;
}

.upload-hint {
  margin-top: 4px;
  color: #9ca3af;
  font-size: 12px;
}

.file-list {
  margin-top: 12px;
  display: grid;
  gap: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 13px;
  color: #374151;
}

.file-item span {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.icon-btn {
  border: 0;
  background: none;
  cursor: pointer;
  color: #94a3b8;
  display: inline-flex;
  align-items: center;
}

.icon-btn.danger:hover {
  color: #ef4444;
}

.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.image-item,
.image-upload {
  width: 88px;
  height: 88px;
}

.image-item {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.image-add {
  width: 88px;
  height: 88px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  color: #94a3b8;
  font-size: 28px;
  cursor: pointer;
}

.side-card :deep(.el-checkbox) {
  display: flex;
  margin-bottom: 10px;
}

.footer-actions {
  position: fixed;
  right: 24px;
  bottom: 24px;
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #e8edf3;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

@media (max-width: 1100px) {
  .edit-layout {
    grid-template-columns: 1fr;
  }

  .side-panel {
    position: static;
  }
}
</style>
