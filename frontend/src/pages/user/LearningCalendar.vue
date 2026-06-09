<template>
  <div class="sl-page learning-calendar" v-loading="loading">
    <div class="page-head">
      <div>
        <h2 class="sl-page-title">学习日历</h2>
        <p class="page-sub">查看每日学习记录，支持断点续学</p>
      </div>
      <div class="head-stats">
        <div class="stat"><span class="val">{{ summary.activeDays }}</span><span class="lbl">活跃天数</span></div>
        <div class="stat"><span class="val">{{ summary.totalHours }}</span><span class="lbl">本月学时</span></div>
      </div>
    </div>

    <el-row :gutter="20" class="calendar-row">
      <el-col :span="16">
        <el-card class="panel-card calendar-card">
          <div class="calendar-toolbar">
            <el-button :icon="ArrowLeft" circle @click="prevMonth" />
            <span class="month-label">{{ currentYear }} 年 {{ currentMonth }} 月</span>
            <el-button :icon="ArrowRight" circle @click="nextMonth" />
            <el-button size="small" class="ml-4" @click="goToday">今天</el-button>
          </div>
          <el-calendar v-model="calendarDate">
            <template #date-cell="{ data }">
              <div class="cal-cell" :class="{ active: isActiveDay(data.day), selected: selectedDate === data.day }" @click="selectDay(data.day)">
                <span class="day-num">{{ data.day.split('-')[2] }}</span>
                <span v-if="dayMinutes(data.day)" class="day-dot" :title="`${dayMinutes(data.day)} 分钟`" />
              </div>
            </template>
          </el-calendar>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="panel-card record-card">
          <template #header><span class="font-bold">{{ selectedDate }} 学习记录</span></template>
          <div class="record-scroll">
            <div class="day-section">
              <div v-if="selectedEvents.length">
                <div class="day-summary">学习 {{ selectedMinutes }} 分钟</div>
                <div v-for="(ev, i) in selectedEvents" :key="i" class="event-item" @click="goEvent(ev)">
                  <el-tag :type="eventTag(ev.type)" size="small">{{ eventLabel(ev.type) }}</el-tag>
                  <span class="event-title">{{ ev.title }}</span>
                  <span class="event-min">{{ ev.minutes }}分</span>
                </div>
              </div>
              <el-empty v-else description="当天暂无学习记录" :image-size="48" />
            </div>
            <div class="history-section">
              <div class="history-header">
                <span class="font-bold">最近学习</span>
                <el-button link type="primary" @click="router.push(p('/courses/my-learning'))">我的学习</el-button>
              </div>
              <div v-for="(item, i) in history" :key="i" class="history-item" @click="goHistory(item)">
                <el-tag :type="eventTag(item.type)" size="small">{{ eventLabel(item.type) }}</el-tag>
                <div class="history-body">
                  <div class="history-title">{{ item.title }}</div>
                  <div class="history-sub">{{ item.subtitle }}</div>
                </div>
              </div>
              <el-empty v-if="!history.length" description="暂无记录" :image-size="40" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import request from '@/api/request'
import { useAppBase } from '@/composables/useAppBase'

interface DayData {
  date: string
  studyMinutes: number
  events: { type: string; title: string; minutes: number; link?: string }[]
}

const router = useRouter()
const { p } = useAppBase()
const loading = ref(true)
const calendarDate = ref(new Date())
const selectedDate = ref('')
const dayDataMap = ref<Record<string, DayData>>({})
const history = ref<any[]>([])
const summary = reactive({ activeDays: 0, totalHours: 0 })

const currentYear = computed(() => calendarDate.value.getFullYear())
const currentMonth = computed(() => calendarDate.value.getMonth() + 1)

const selectedEvents = computed(() => dayDataMap.value[selectedDate.value]?.events || [])
const selectedMinutes = computed(() => dayDataMap.value[selectedDate.value]?.studyMinutes || 0)

function isActiveDay(day: string) {
  return (dayDataMap.value[day]?.studyMinutes || 0) > 0
}

function dayMinutes(day: string) {
  return dayDataMap.value[day]?.studyMinutes || 0
}

function selectDay(day: string) {
  selectedDate.value = day
}

function eventTag(type: string) {
  const map: Record<string, string> = { course: '', training: 'warning', simulation: 'danger' }
  return (map[type] || 'info') as any
}

function eventLabel(type: string) {
  const map: Record<string, string> = { course: '课程', training: '训练', simulation: '推演' }
  return map[type] || type
}

function goEvent(ev: { link?: string }) {
  if (ev.link) router.push(ev.link)
}

function goHistory(item: { link?: string }) {
  if (item.link) router.push(item.link)
}

function prevMonth() {
  const d = new Date(calendarDate.value)
  d.setMonth(d.getMonth() - 1)
  calendarDate.value = d
}

function nextMonth() {
  const d = new Date(calendarDate.value)
  d.setMonth(d.getMonth() + 1)
  calendarDate.value = d
}

function goToday() {
  calendarDate.value = new Date()
  selectedDate.value = new Date().toISOString().slice(0, 10)
}

async function loadCalendar() {
  loading.value = true
  try {
    const res = await request.get('/dashboard/learning-calendar', {
      params: { year: currentYear.value, month: currentMonth.value },
    })
    const map: Record<string, DayData> = {}
    for (const d of res.data.days || []) map[d.date] = d
    dayDataMap.value = map
    summary.activeDays = res.data.activeDays || 0
    summary.totalHours = Math.round((res.data.totalMinutes || 0) / 60 * 10) / 10
    if (!selectedDate.value) selectedDate.value = new Date().toISOString().slice(0, 10)
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  const res = await request.get('/dashboard/learning-history')
  history.value = res.data || []
}

onMounted(async () => {
  selectedDate.value = new Date().toISOString().slice(0, 10)
  await Promise.all([loadCalendar(), loadHistory()])
})

watch(calendarDate, loadCalendar)
</script>

<style scoped>
.page-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.page-sub { color: #6b7280; font-size: 14px; margin-top: 4px; }
.head-stats { display: flex; gap: 24px; }
.stat { text-align: center; }
.stat .val { display: block; font-size: 24px; font-weight: 700; color: #2b5aed; }
.stat .lbl { font-size: 12px; color: #9ca3af; }

.calendar-row { align-items: flex-start; }
.calendar-row > :deep(.el-col) { display: flex; }

.panel-card {
  width: 100%;
  height: var(--panel-h);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.panel-card :deep(.el-card__body) {
  flex: 1;
  overflow: hidden;
  padding: 12px 16px;
  min-height: 0;
}
.panel-card :deep(.el-card__header) {
  padding: 12px 16px;
}

.calendar-toolbar { display: flex; align-items: center; margin-bottom: 8px; }
.month-label { font-weight: 600; margin: 0 12px; min-width: 120px; text-align: center; font-size: 14px; }

/* 隐藏 el-calendar 自带头部，避免重复 */
.calendar-card :deep(.el-calendar__header) { display: none; }
.calendar-card :deep(.el-calendar__body) { padding: 0; }
.calendar-card :deep(.el-calendar-table thead th) {
  padding: 6px 0;
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}
.calendar-card :deep(.el-calendar-table .el-calendar-day) {
  height: 52px;
  padding: 0;
}
.calendar-card :deep(.el-calendar-table td) {
  border: none;
}
.calendar-card :deep(.el-calendar-table tr td:first-child) {
  border-left: none;
}

.cal-cell {
  height: 100%;
  min-height: 44px;
  padding: 2px;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.cal-cell:hover { background: #f3f4f6; }
.cal-cell.selected { background: #eef3ff; }
.cal-cell.active .day-num { color: #2b5aed; font-weight: 600; }
.day-num { font-size: 13px; line-height: 1.2; }
.day-dot { display: block; width: 5px; height: 5px; border-radius: 50%; background: #2b5aed; margin-top: 2px; }

.record-card :deep(.el-card__body) {
  padding: 0 16px 12px;
  display: flex;
  flex-direction: column;
}
.record-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}
.record-scroll::-webkit-scrollbar { width: 6px; }
.record-scroll::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 3px; }
.record-scroll::-webkit-scrollbar-thumb:hover { background: #9ca3af; }

.day-section {
  padding-bottom: 12px;
  margin-bottom: 12px;
  border-bottom: 1px solid #f3f4f6;
}
.day-summary { font-size: 13px; color: #6b7280; margin-bottom: 8px; }
.event-item { display: flex; align-items: center; gap: 8px; padding: 8px 0; border-bottom: 1px solid #f3f4f6; cursor: pointer; }
.event-item:hover { background: #f9fafb; }
.event-title { flex: 1; font-size: 13px; color: #374151; }
.event-min { font-size: 12px; color: #9ca3af; flex-shrink: 0; }

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  position: sticky;
  top: 0;
  background: #fff;
  padding: 4px 0;
  z-index: 1;
}
.history-item { display: flex; gap: 10px; padding: 8px 0; border-bottom: 1px solid #f3f4f6; cursor: pointer; }
.history-item:hover { background: #f9fafb; }
.history-title { font-size: 13px; font-weight: 600; color: #1f2937; }
.history-sub { font-size: 12px; color: #9ca3af; }
.ml-4 { margin-left: 16px; }

.learning-calendar {
  --panel-h: 520px;
}
</style>
