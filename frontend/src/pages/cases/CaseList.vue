<template>
  <div class="case-list">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-2xl font-bold">事故案例库</h2>
      <el-input
        v-model="searchText"
        placeholder="搜索案例..."
        prefix-icon="Search"
        style="width: 300px"
      />
    </div>

    <!-- 分类筛选 -->
    <div class="mb-6">
      <el-radio-group v-model="activeType">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="fire">火灾</el-radio-button>
        <el-radio-button label="explosion">爆炸</el-radio-button>
        <el-radio-button label="thermal_runaway">热失控</el-radio-button>
        <el-radio-button label="gas_leak">气体泄漏</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 案例列表 -->
    <el-skeleton v-if="loading" animated :rows="6" />
    <template v-else>
      <el-empty v-if="!filteredCases.length" description="暂无案例" />
      <el-row v-else :gutter="20">
        <el-col :span="8" v-for="caseItem in filteredCases" :key="caseItem.id">
          <el-card class="case-card" shadow="hover" @click="$router.push(`/cases/${caseItem.id}`)">
            <div class="case-header">
              <el-tag :type="getSeverityType(caseItem.severity)">
                {{ getSeverityName(caseItem.severity) }}
              </el-tag>
              <span class="case-date">{{ caseItem.date }}</span>
            </div>
            <h3 class="case-title">{{ caseItem.title }}</h3>
            <p class="case-location">
              <el-icon><Location /></el-icon>
              {{ caseItem.location }}
            </p>
            <p class="case-desc">{{ caseItem.description }}</p>
            <div class="case-meta">
              <el-tag :type="getTypeType(caseItem.type)" size="small">
                {{ getTypeName(caseItem.type) }}
              </el-tag>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Location } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import type { AccidentCase } from '@/types'

const searchText = ref('')
const activeType = ref('all')

const cases = ref<AccidentCase[]>([])
const loading = ref(true)

onMounted(async () => {
  loading.value = true
  try {
    const res = await request.get('/cases')
    cases.value = res.data
  } catch (error) {
    console.error('加载案例失败', error)
    ElMessage.error('加载案例失败，请稍后重试')
  } finally {
    loading.value = false
  }
})

const filteredCases = computed(() => {
  let result = cases.value
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
  gap: 8px;
}
</style>
