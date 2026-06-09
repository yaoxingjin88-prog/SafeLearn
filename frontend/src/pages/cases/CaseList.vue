<template>
  <div class="sl-page case-list">
    <div class="sl-page-header">
      <h2 class="sl-page-title">事故案例库</h2>
      <el-input
        v-model="searchText"
        placeholder="搜索案例..."
        prefix-icon="Search"
        style="width: 300px"
      />
    </div>

    <div class="filter-toolbar sl-page-section">
      <div class="category-filters">
        <button
          v-for="type in caseTypes"
          :key="type.value"
          type="button"
          class="category-pill"
          :class="{ active: activeType === type.value }"
          @click="activeType = type.value"
        >
          {{ type.label }}
        </button>
      </div>
      <div class="sort-control">
        <span class="sort-label">排序：</span>
        <el-select v-model="sortBy" class="sort-select" size="default">
          <el-option label="最新发布" value="latest" />
          <el-option label="未复盘优先" value="popular" />
        </el-select>
      </div>
    </div>

    <!-- 案例列表 -->
    <el-skeleton v-if="loading" animated :rows="6" />
    <template v-else>
      <el-empty v-if="!filteredCases.length" description="暂无案例" />
      <el-row v-else :gutter="20">
        <el-col :span="8" v-for="caseItem in filteredCases" :key="caseItem.id">
          <el-card class="case-card" shadow="hover" @click="router.push(p(`/cases/${caseItem.id}`))">
            <div class="case-header">
              <div class="case-tags">
                <el-tag :type="getSeverityType(caseItem.severity)">
                  {{ getSeverityName(caseItem.severity) }}
                </el-tag>
                <el-tag v-if="progressMap[caseItem.id]?.completed" type="success" size="small" effect="plain">
                  已复盘
                </el-tag>
              </div>
              <span class="case-date">{{ caseItem.date }}</span>
            </div>
            <h3 class="case-title">{{ caseItem.title }}</h3>
            <p class="case-location">
              <el-icon><Location /></el-icon>
              {{ caseItem.location }}
            </p>
            <p class="case-desc">{{ caseItem.description }}</p>
            <div v-if="caseItem.timeline?.length" class="timeline-preview">
              <div v-for="(evt, idx) in caseItem.timeline.slice(0, 3)" :key="idx" class="timeline-preview-item">
                <span class="tp-time">{{ evt.time }}</span>
                <span class="tp-title">{{ evt.title }}</span>
              </div>
            </div>
            <div class="case-meta">
              <el-tag :type="getTypeType(caseItem.type)" size="small">
                {{ getTypeName(caseItem.type) }}
              </el-tag>
              <el-button
                type="primary"
                link
                size="small"
                @click.stop="startReview(caseItem.id)"
              >
                {{ progressMap[caseItem.id]?.completed ? '再次复盘' : '复盘引导' }}
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Location } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useAppBase } from '@/composables/useAppBase'
import type { AccidentCase } from '@/types'

const router = useRouter()
const { p } = useAppBase()

const searchText = ref('')
const activeType = ref('all')
const sortBy = ref<'latest' | 'popular'>('latest')

const caseTypes = [
  { label: '全部', value: 'all' },
  { label: '火灾', value: 'fire' },
  { label: '爆炸', value: 'explosion' },
  { label: '热失控', value: 'thermal_runaway' },
  { label: '气体泄漏', value: 'gas_leak' },
]

const severityWeight: Record<string, number> = {
  critical: 4,
  severe: 4,
  major: 3,
  moderate: 2,
  minor: 1,
}

const cases = ref<AccidentCase[]>([])
const loading = ref(true)
const progressMap = ref<Record<string, { completed: boolean }>>({})

function startReview(id: string) {
  router.push({ path: p(`/cases/${id}`), query: { review: '1' } })
}

onMounted(async () => {
  loading.value = true
  try {
    const [casesRes, progressRes] = await Promise.all([
      request.get('/cases'),
      request.get('/cases/progress/summary').catch(() => ({ data: [] })),
    ])
    cases.value = casesRes.data
    const map: Record<string, { completed: boolean }> = {}
    for (const item of progressRes.data || []) {
      map[item.caseId] = { completed: !!item.completed }
    }
    progressMap.value = map
  } catch (error) {
    console.error('加载案例失败', error)
    ElMessage.error('加载案例失败，请稍后重试')
  } finally {
    loading.value = false
  }
})

const filteredCases = computed(() => {
  let result = [...cases.value]
  if (activeType.value !== 'all') {
    result = result.filter(c => c.type === activeType.value)
  }
  if (searchText.value) {
    const search = searchText.value.toLowerCase()
    result = result.filter(c =>
      c.title.toLowerCase().includes(search) ||
      c.description.toLowerCase().includes(search)
    )
  }
  if (sortBy.value === 'popular') {
    result.sort((a, b) => {
      const aDone = progressMap.value[a.id]?.completed ? 1 : 0
      const bDone = progressMap.value[b.id]?.completed ? 1 : 0
      if (aDone !== bDone) return aDone - bDone
      return (severityWeight[b.severity] || 0) - (severityWeight[a.severity] || 0)
    })
  } else {
    result.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
  }
  return result
})

function getSeverityType(severity: string) {
  const map: Record<string, string> = {
    minor: 'info',
    moderate: 'warning',
    major: 'danger',
    critical: 'danger',
  }
  return map[severity] || ''
}

function getSeverityName(severity: string) {
  const map: Record<string, string> = {
    minor: '轻微',
    moderate: '中等',
    major: '严重',
    critical: '特别严重',
    severe: '特别严重',
  }
  return map[severity] || severity
}

function getTypeType(type: string) {
  const map: Record<string, string> = {
    fire: 'danger',
    explosion: 'warning',
    thermal_runaway: 'warning',
    gas_leak: 'info',
  }
  return map[type] || ''
}

function getTypeName(type: string) {
  const map: Record<string, string> = {
    fire: '火灾',
    explosion: '爆炸',
    thermal_runaway: '热失控',
    gas_leak: '气体泄漏',
  }
  return map[type] || type
}

</script>

<style scoped>
.filter-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 16px 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.06);
}

.category-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.category-pill {
  border: none;
  border-radius: 8px;
  padding: 8px 18px;
  font-size: 14px;
  font-weight: 500;
  color: #4b5563;
  background: #f3f4f6;
  cursor: pointer;
  transition: all 0.2s ease;
  line-height: 1.4;
}

.category-pill:hover {
  background: #e5e7eb;
  color: #1f2937;
}

.category-pill.active {
  background: #2b5aed;
  color: #fff;
  box-shadow: 0 2px 8px rgba(43, 90, 237, 0.25);
}

.sort-control {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.sort-label {
  font-size: 14px;
  color: #6b7280;
  white-space: nowrap;
}

.sort-select {
  width: 130px;
}

@media (max-width: 768px) {
  .filter-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .sort-control {
    justify-content: flex-end;
  }
}

.case-card {
  margin-bottom: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.case-card:hover {
  transform: translateY(-4px);
}

.case-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.case-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.timeline-preview {
  border-left: 2px solid #e5e7eb;
  padding-left: 10px;
  margin-bottom: 12px;
}

.timeline-preview-item {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
  display: flex;
  gap: 8px;
}

.tp-time {
  color: #2b5aed;
  font-weight: 500;
  flex-shrink: 0;
}

.tp-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.case-date {
  font-size: 13px;
  color: #9ca3af;
}

.case-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 8px;
  color: #1f2937;
}

.case-location {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 12px;
}

.case-desc {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.case-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
</style>
