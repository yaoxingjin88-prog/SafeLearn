<template>
  <div class="sl-page my-learning">
    <div class="sl-page-head">
      <h2 class="sl-page-title">我的学习</h2>
    </div>
    <el-card v-loading="loading">
      <el-table :data="records" style="width: 100%">
        <el-table-column prop="courseTitle" label="课程" min-width="200" />
        <el-table-column prop="chapterTitle" label="章节" min-width="180" />
        <el-table-column prop="progress" label="进度" width="180">
          <template #default="{ row }">
            <el-progress :percentage="row.progress" :status="row.completed ? 'success' : ''" />
          </template>
        </el-table-column>
        <el-table-column prop="masteryLevel" label="掌握度" width="100">
          <template #default="{ row }">{{ row.masteryLevel || 0 }}%</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="goChapter(row)">继续</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !records.length" description="暂无学习记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { courseApi } from '@/api/course'
import { useAppBase } from '@/composables/useAppBase'

const router = useRouter()
const { p } = useAppBase()
const loading = ref(true)
const records = ref<any[]>([])

function resolveChapterTitle(
  chapterId: string,
  chapterTitle: string | undefined,
  chapterMap: Map<string, string>,
) {
  return chapterTitle || chapterMap.get(chapterId) || '未知章节'
}

async function load() {
  loading.value = true
  try {
    const coursesRes = await courseApi.getList()
    const courses = coursesRes.data.items || coursesRes.data
    const all: any[] = []
    for (const course of courses) {
      const [progressRes, detailRes] = await Promise.all([
        courseApi.getProgress(course.id),
        courseApi.getById(course.id),
      ])
      const chapterMap = new Map(
        (detailRes.data.chapters || []).map(ch => [ch.id, ch.title]),
      )
      for (const p of progressRes.data || []) {
        all.push({
          courseId: course.id,
          courseTitle: course.title,
          chapterId: p.chapterId,
          chapterTitle: resolveChapterTitle(p.chapterId, p.chapterTitle, chapterMap),
          progress: p.progress,
          completed: p.completed,
          masteryLevel: p.masteryLevel,
        })
      }
    }
    records.value = all
  } finally {
    loading.value = false
  }
}

function goChapter(row: any) {
  router.push(p(`/courses/${row.courseId}/chapters/${row.chapterId}`))
}

onMounted(load)
</script>
