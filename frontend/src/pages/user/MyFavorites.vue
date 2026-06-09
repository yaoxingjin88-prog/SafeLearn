<template>
  <div class="sl-page my-favorites" v-loading="loading">
    <h2 class="sl-page-title">我的收藏</h2>
    <el-tabs v-model="tab">
      <el-tab-pane label="课程" name="course">
        <div v-if="courses.length" class="fav-grid">
          <div v-for="item in courses" :key="item.id" class="fav-card" @click="goCourse(item.targetId)">
            <div class="fav-thumb">
              <img :src="item.coverImage || '/images/default-course.svg'" alt="" />
            </div>
            <div class="fav-info">
              <h4>{{ item.title }}</h4>
              <el-tag size="small">{{ item.category }}</el-tag>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无收藏课程" />
      </el-tab-pane>
      <el-tab-pane label="案例" name="case">
        <div v-if="cases.length" class="fav-list">
          <div v-for="item in cases" :key="item.id" class="fav-row" @click="goCase(item.targetId)">
            <span class="fav-title">{{ item.title }}</span>
            <el-tag size="small" type="danger">{{ item.severity }}</el-tag>
          </div>
        </div>
        <el-empty v-else description="暂无收藏案例" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { favoriteApi, type FavoriteItem } from '@/api/favorite'
import { useAppBase } from '@/composables/useAppBase'

const router = useRouter()
const { p } = useAppBase()
const loading = ref(true)
const tab = ref('course')
const courses = ref<FavoriteItem[]>([])
const cases = ref<FavoriteItem[]>([])

function goCourse(id: string) {
  router.push(p(`/courses/${id}`))
}

function goCase(id: string) {
  router.push(p(`/cases/${id}`))
}

onMounted(async () => {
  try {
    const [cRes, aRes] = await Promise.all([
      favoriteApi.list('course'),
      favoriteApi.list('case'),
    ])
    courses.value = cRes.data || []
    cases.value = aRes.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.fav-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.fav-card { display: flex; gap: 12px; padding: 12px; border-radius: 12px; background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.04); cursor: pointer; }
.fav-card:hover { background: #f9fafb; }
.fav-thumb { width: 80px; height: 56px; border-radius: 8px; overflow: hidden; flex-shrink: 0; }
.fav-thumb img { width: 100%; height: 100%; object-fit: cover; }
.fav-info h4 { font-size: 14px; font-weight: 600; margin-bottom: 6px; }
.fav-list { display: flex; flex-direction: column; gap: 8px; }
.fav-row { display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; background: #fff; border-radius: 10px; cursor: pointer; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.fav-row:hover { background: #f9fafb; }
.fav-title { font-weight: 500; color: #1f2937; }
</style>
