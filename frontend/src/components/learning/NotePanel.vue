<template>
  <el-card class="note-panel">
    <template #header>
      <div class="note-header">
        <span class="font-bold">学习笔记</span>
        <el-tag size="small" type="info">{{ notes.length }} 条</el-tag>
      </div>
    </template>
    <el-input
      v-model="draft"
      type="textarea"
      :rows="4"
      placeholder="记录本章要点、疑问或心得..."
      maxlength="2000"
      show-word-limit
    />
    <div class="note-actions">
      <el-button type="primary" size="small" :loading="saving" @click="saveNote">保存笔记</el-button>
    </div>
    <div v-if="notes.length" class="note-list">
      <div v-for="note in notes" :key="note.id" class="note-item">
        <p class="note-content">{{ note.content }}</p>
        <div class="note-meta">
          <span>{{ formatTime(note.updatedAt || note.createdAt) }}</span>
          <el-button type="danger" link size="small" @click="removeNote(note.id)">删除</el-button>
        </div>
      </div>
    </div>
    <el-empty v-else description="暂无笔记，开始记录吧" :image-size="60" />
  </el-card>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { noteApi, type UserNote } from '@/api/note'

const props = defineProps<{
  targetType: 'chapter' | 'case'
  targetId: string
  courseId?: string
}>()

const notes = ref<UserNote[]>([])
const draft = ref('')
const saving = ref(false)

function formatTime(t?: string) {
  if (!t) return ''
  return t.replace('T', ' ').slice(0, 16)
}

async function load() {
  if (!props.targetId) return
  try {
    const res = await noteApi.list(props.targetType, props.targetId)
    notes.value = res.data || []
  } catch { notes.value = [] }
}

async function saveNote() {
  const content = draft.value.trim()
  if (!content) {
    ElMessage.warning('请输入笔记内容')
    return
  }
  saving.value = true
  try {
    await noteApi.save({
      targetType: props.targetType,
      targetId: props.targetId,
      courseId: props.courseId,
      content,
    })
    draft.value = ''
    await load()
    ElMessage.success('笔记已保存')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function removeNote(id: string) {
  await ElMessageBox.confirm('确定删除这条笔记？', '提示', { type: 'warning' })
  await noteApi.remove(id)
  await load()
  ElMessage.success('已删除')
}

onMounted(load)
watch(() => props.targetId, load)
</script>

<style scoped>
.note-header { display: flex; justify-content: space-between; align-items: center; }
.note-actions { margin-top: 10px; text-align: right; }
.note-list { margin-top: 16px; max-height: 280px; overflow-y: auto; }
.note-item { padding: 10px 0; border-bottom: 1px solid #f3f4f6; }
.note-content { font-size: 13px; color: #374151; line-height: 1.6; white-space: pre-wrap; }
.note-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 6px; font-size: 12px; color: #9ca3af; }
</style>
