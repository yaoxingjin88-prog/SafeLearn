<template>
  <div class="sl-page user-search-page" v-loading="loading">
    <h2 class="sl-page-title">搜索结果</h2>
    <p v-if="keyword" class="search-hint">关键词：{{ keyword }}</p>

    <section class="result-section">
      <h3>课程（{{ courses.length }}）</h3>
      <el-empty v-if="!courses.length" description="未找到相关课程" :image-size="64" />
      <div v-else class="result-list">
        <div v-for="item in courses" :key="item.id" class="result-card" @click="goCourse(item.id)">
          <img :src="item.coverImage || '/images/default-course.svg'" alt="" />
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.description }}</p>
          </div>
        </div>
      </div>
    </section>

    <section class="result-section">
      <h3>案例（{{ cases.length }}）</h3>
      <el-empty v-if="!cases.length" description="未找到相关案例" :image-size="64" />
      <div v-else class="result-list">
        <div v-for="item in cases" :key="item.id" class="result-card" @click="goCase(item.id)">
          <div class="case-icon"><el-icon><Files /></el-icon></div>
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.location }} · {{ item.date }}</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Files } from '@element-plus/icons-vue'
import { caseApi } from '@/api/case'
import type { AccidentCase } from '@/types'
import { courseApi } from '@/api/course'
import { useAppBase } from '@/composables/useAppBase'
import type { Course } from '@/types'

const route = useRoute()
const router = useRouter()
const { p } = useAppBase()
const loading = ref(false)
const courses = ref<Course[]>([])
const cases = ref<AccidentCase[]>([])

const keyword = computed(() => String(route.query.q || '').trim())

async function search() {
  const q = keyword.value
  if (!q) {
    courses.value = []
    cases.value = []
    return
  }
  loading.value = true
  try {
    const [courseRes, caseRes] = await Promise.all([
      courseApi.getList({ keyword: q, page: 1, pageSize: 50 }),
      caseApi.search({ keyword: q }),
    ])
    courses.value = courseRes.data?.items || []
    cases.value = caseRes.data || []
  } finally {
    loading.value = false
  }
}

function goCourse(id: string) {
  router.push(p(`/courses/${id}`))
}

function goCase(id: string) {
  router.push(p(`/cases/${id}`))
}

onMounted(search)
watch(() => route.query.q, search)
</script>

<style scoped>
.search-hint { color: #6b7280; margin-bottom: 20px; }
.result-section { margin-bottom: 28px; }
.result-section h3 { font-size: 16px; font-weight: 700; margin-bottom: 12px; }
.result-list { display: flex; flex-direction: column; gap: 10px; }
.result-card {
  display: flex;
  gap: 14px;
  padding: 12px;
  background: #fff;
  border-radius: 12px;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.06);
}
.result-card:hover { background: #f9fafb; }
.result-card img { width: 96px; height: 64px; object-fit: cover; border-radius: 8px; }
.result-card strong { display: block; margin-bottom: 4px; }
.result-card p { margin: 0; font-size: 13px; color: #6b7280; }
.case-icon {
  width: 96px;
  height: 64px;
  border-radius: 8px;
  background: #eef3ff;
  color: #2b5aed;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}
</style>
