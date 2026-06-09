<template>
  <div class="category-panel">
    <div class="panel-toolbar">
      <p class="panel-desc">管理学员端课程列表顶部的分类筛选标签，课程需选择对应分类编码。</p>
      <el-button type="primary" @click="showAdd">
        <el-icon><Plus /></el-icon>
        添加分类
      </el-button>
    </div>

    <el-table :data="categories" v-loading="loading" size="small">
      <el-table-column prop="sortOrder" label="排序" width="70" />
      <el-table-column prop="name" label="显示名称" min-width="140" />
      <el-table-column prop="code" label="分类编码" width="120">
        <template #default="{ row }">
          <code class="code-tag">{{ row.code }}</code>
        </template>
      </el-table-column>
      <el-table-column label="标签样式" width="110">
        <template #default="{ row }">
          <el-tag :type="row.tagType || undefined" size="small">{{ row.name }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="courseCount" label="课程数" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
            {{ row.enabled ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="editRow(row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="removeRow(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '添加分类'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="显示名称" prop="name">
          <el-input v-model="form.name" placeholder="如：基础知识" />
        </el-form-item>
        <el-form-item label="分类编码" prop="code">
          <el-input
            v-model="form.code"
            placeholder="如：basic（英文小写，用于课程关联）"
            :disabled="isEdit && (form.courseCount ?? 0) > 0"
          />
          <div v-if="isEdit && (form.courseCount ?? 0) > 0" class="field-tip">已有课程使用此编码，不可修改</div>
        </el-form-item>
        <el-form-item label="标签样式">
          <el-select v-model="form.tagType" placeholder="默认" clearable class="w-full">
            <el-option label="默认" value="" />
            <el-option label="成功(绿)" value="success" />
            <el-option label="警告(橙)" value="warning" />
            <el-option label="危险(红)" value="danger" />
            <el-option label="信息(蓝)" value="info" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
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
import type { CourseCategory } from '@/types'

const emit = defineEmits<{ changed: [] }>()

const loading = ref(false)
const categories = ref<CourseCategory[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: '',
  name: '',
  code: '',
  tagType: '',
  sortOrder: 0,
  enabled: true,
  courseCount: 0,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入分类编码', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_-]*$/, message: '仅支持小写字母、数字、下划线', trigger: 'blur' },
  ],
}

async function load() {
  loading.value = true
  try {
    const res = await adminApi.getCourseCategories()
    categories.value = res.data || []
  } finally {
    loading.value = false
  }
}

function showAdd() {
  isEdit.value = false
  form.id = ''
  form.name = ''
  form.code = ''
  form.tagType = ''
  form.sortOrder = categories.value.length + 1
  form.enabled = true
  form.courseCount = 0
  dialogVisible.value = true
}

function editRow(row: CourseCategory) {
  isEdit.value = true
  form.id = row.id
  form.name = row.name
  form.code = row.code
  form.tagType = row.tagType || ''
  form.sortOrder = row.sortOrder
  form.enabled = row.enabled
  form.courseCount = row.courseCount ?? 0
  dialogVisible.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload = {
    name: form.name,
    code: form.code,
    tagType: form.tagType,
    sortOrder: form.sortOrder,
    enabled: form.enabled,
  }

  try {
    if (isEdit.value) {
      await adminApi.updateCourseCategory(form.id, payload)
    } else {
      await adminApi.createCourseCategory(payload)
    }
    dialogVisible.value = false
    await load()
    emit('changed')
    ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '操作失败'
    ElMessage.error(msg)
  }
}

async function removeRow(row: CourseCategory) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '提示', { type: 'warning' })
    await adminApi.deleteCourseCategory(row.id)
    await load()
    emit('changed')
    ElMessage.success('删除成功')
  } catch (e: unknown) {
    if (e === 'cancel') return
    const msg = e instanceof Error ? e.message : '删除失败'
    ElMessage.error(msg)
  }
}

onMounted(load)

defineExpose({ reload: load })
</script>

<style scoped>
.panel-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.panel-desc {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
  max-width: 520px;
}

.code-tag {
  font-size: 12px;
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
  color: #374151;
}

.field-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
}

.w-full {
  width: 100%;
}
</style>
