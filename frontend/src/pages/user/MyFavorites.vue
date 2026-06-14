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
              <el-tag v-if="item.category" size="small">{{ getCategoryName(item.category) }}</el-tag>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无收藏课程" />
      </el-tab-pane>
      <el-tab-pane label="案例" name="case">
        <div v-if="cases.length" class="fav-list">
          <div v-for="item in cases" :key="item.id" class="fav-case-card" @click="goCase(item.targetId)">
            <div class="fav-case-main">
              <h4 class="fav-case-title">{{ item.title }}</h4>
              <div class="fav-case-meta">
                <span v-if="item.date" class="meta-item">
                  <el-icon><Calendar /></el-icon>{{ item.date }}
                </span>
                <span v-if="item.location" class="meta-item">
                  <el-icon><Location /></el-icon>{{ item.location }}
                </span>
                <span v-if="item.type" class="type-chip" :class="`type-${item.type}`">
                  {{ getTypeName(item.type) }}
                </span>
              </div>
            </div>
            <el-tag v-if="item.severity" size="small" :type="getSeverityTagType(item.severity)" class="fav-sev-tag">
              {{ getSeverityName(item.severity) }}
            </el-tag>
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
import { Calendar, Location } from '@element-plus/icons-vue'
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

function getCategoryName(category?: string) {
  if (!category) return ''
  const map: Record<string, string> = {
    basic: '基础',
    intermediate: '进阶',
    advanced: '高级',
    battery: '锂电池',
    thermal: '热失控',
    fire: '消防',
    bms: 'BMS',
  }
  return map[category] || category
}

function getSeverityName(severity?: string) {
  if (!severity) return ''
  const map: Record<string, string> = {
    minor: '轻微',
    moderate: '中等',
    major: '严重',
    critical: '特别严重',
    severe: '特别严重',
  }
  return map[severity] || severity
}

function getSeverityTagType(severity?: string): 'success' | 'warning' | 'danger' | 'info' {
  if (!severity) return 'info'
  if (severity === 'critical' || severity === 'severe') return 'danger'
  if (severity === 'major') return 'warning'
  if (severity === 'moderate') return 'warning'
  return 'info'
}

function getTypeName(type?: string) {
  if (!type) return ''
  const map: Record<string, string> = {
    fire: '火灾',
    explosion: '爆炸',
    thermal_runaway: '热失控',
    gas_leak: '气体泄漏',
  }
  return map[type] || type
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
.fav-list { display: flex; flex-direction: column; gap: 10px; }

.fav-case-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border-radius: 10px;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  border: 1px solid #f1f5f9;
}
.fav-case-card:hover { background: #f9fafb; }

.fav-case-main {
  flex: 1;
  min-width: 0;
}

.fav-case-title {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.45;
}

.fav-case-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 14px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #64748b;
}

.meta-item .el-icon {
  font-size: 13px;
  color: #94a3b8;
}

.type-chip {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}
.type-chip.type-fire { background: #fef2f2; color: #dc2626; }
.type-chip.type-explosion { background: #fff7ed; color: #ea580c; }
.type-chip.type-thermal_runaway { background: #fffbeb; color: #d97706; }
.type-chip.type-gas_leak { background: #eff6ff; color: #2563eb; }

.fav-sev-tag { flex-shrink: 0; margin-top: 2px; }
</style>
