<template>
  <div class="sl-page admin-dashboard">
    <!-- 顶栏 -->
    <div class="dash-toolbar">
      <el-input
        v-model="searchKeyword"
        class="dash-search"
        placeholder="输入搜索关键字，如：消防安全、隐患排查..."
        :prefix-icon="Search"
        clearable
        @keyup.enter="handleSearch"
      />
      <div class="toolbar-actions">
        <button type="button" class="toolbar-btn" title="通知">
          <el-icon :size="20"><Bell /></el-icon>
          <span class="notify-dot" />
        </button>
        <button type="button" class="toolbar-btn" title="切换护眼模式">
          <el-icon :size="20"><Sunny /></el-icon>
        </button>
        <span class="online-pill">
          <span class="online-dot" />
          实时在线
        </span>
      </div>
    </div>

    <!-- 工作台标题区 -->
    <div class="hero-row">
      <div class="hero-text">
        <div class="hero-tags">
          <span class="hero-badge">安全运行中</span>
          <span class="hero-badge hero-badge--course">已上线 {{ platform.totalCourses }} 门课程</span>
          <span class="hero-dot">·</span>
          <span class="hero-sub">控制台已接入 {{ learning.departments.length || 0 }} 组部门数据源</span>
        </div>
        <h1 class="hero-title">工作台</h1>
        <p class="hero-desc">储能电站安全培训 - 运营数据概览与一站式多维可视化监控面板</p>
      </div>
      <div class="hero-actions">
        <button
          type="button"
          class="refresh-btn"
          :class="{ spinning: refreshing }"
          @click="refreshData"
        >
          <el-icon :size="16"><Refresh /></el-icon>
          模拟刷新数据
        </button>
        <div class="clock-box">
          <span class="clock-label">SYSTEM TIME</span>
          <span class="clock-value">{{ dateLabel }} · {{ currentTime }}</span>
        </div>
      </div>
    </div>

    <!-- 管理快捷入口 -->
    <section class="dash-section">
      <h3 class="section-heading">
        管理快捷入口
        <span class="heading-dot tone-blue" />
      </h3>
      <div class="action-grid">
        <AdminQuickActionCard
          v-for="item in quickLinks"
          :key="item.path"
          :title="item.title"
          :desc="item.desc"
          :meta="item.meta"
          :badge="item.badge"
          :icon="item.icon"
          :tone="item.tone"
          @click="$router.push(item.path)"
        />
      </div>
    </section>

    <!-- 学员动态 -->
    <section class="dash-section">
      <div class="section-head-row">
        <h3 class="section-heading">
          学员动态 · 实时监控
          <span class="heading-dot tone-green ping" />
        </h3>
        <span class="section-hint">每 30 秒自动同步数据源</span>
      </div>
      <div class="live-grid">
        <AdminLiveStatCard
          :value="learning.onlineLearners"
          label="当前在线学习"
          :icon="EditPen"
          tone="green"
          :pulse="pulseOnline"
        />
        <AdminLiveStatCard
          :value="learning.activeToday"
          label="今日活跃学员"
          :icon="Sunny"
          tone="orange"
        />
        <AdminLiveStatCard
          :value="learning.totalLearners"
          label="总学习人数"
          :icon="UserFilled"
          tone="blue"
        />
        <AdminLiveStatCard
          :value="learning.departments.length"
          label="参训部门数量"
          unit="个部门"
          :icon="OfficeBuilding"
          tone="purple"
        />
      </div>
    </section>

    <!-- 部门学习进度 -->
    <section class="dash-section">
      <div class="section-head-row">
        <h3 class="section-heading">
          部门学习进度
          <span class="heading-dot tone-blue" />
        </h3>
        <span class="section-hint">共监控 {{ learning.departments.length }} 个一线常备班组</span>
      </div>

      <div class="dept-panel">
          <table v-if="learning.departments.length" class="dept-table">
            <thead>
              <tr>
                <th>部门 / 班组</th>
                <th class="col-center">参训人数</th>
                <th class="col-center">完成章节</th>
                <th>平均进度</th>
                <th class="col-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in learning.departments" :key="row.name">
                <td>
                  <div class="dept-cell">
                    <span class="status-dot" :class="progressLevel(row.avgProgress)" />
                    <div>
                      <span class="dept-name">{{ row.name }}</span>
                      <span class="dept-code">{{ deptCode(row.name) }}</span>
                    </div>
                  </div>
                </td>
                <td class="col-center">
                  <span class="member-pill">{{ row.learnedMembers }}/{{ row.memberCount }}</span>
                </td>
                <td class="col-center">
                  <span class="chapter-num">{{ row.completedChapters }}</span>
                  <span class="chapter-total">/{{ chapterTotal }}</span>
                </td>
                <td>
                  <div class="progress-wrap">
                    <span class="progress-pct" :class="progressLevel(row.avgProgress)">
                      {{ row.avgProgress }}%
                    </span>
                    <div class="progress-track">
                      <div
                        class="progress-fill"
                        :class="progressLevel(row.avgProgress)"
                        :style="{ width: `${row.avgProgress}%` }"
                      />
                    </div>
                  </div>
                </td>
                <td class="col-right">
                  <div class="row-actions">
                    <button type="button" class="row-btn" title="查看明细" @click="openDeptDetail(row)">
                      <el-icon :size="16"><View /></el-icon>
                    </button>
                    <button type="button" class="row-btn" title="导出数据报告">
                      <el-icon :size="16"><Document /></el-icon>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <el-empty v-else description="暂无部门学习数据" :image-size="80" />
      </div>
    </section>

    <!-- 数据分析 -->
    <section class="dash-section">
      <div class="section-head-row">
        <h3 class="section-heading">
          培训数据分析
          <span class="heading-dot tone-blue" />
        </h3>
        <span class="section-hint">基于真实业务数据实时聚合</span>
      </div>
      <AdminAnalyticsPanel />
    </section>

    <!-- 部门详情弹窗 -->
    <el-dialog
      v-model="deptDialogVisible"
      width="480px"
      class="dept-dialog"
      :show-close="false"
      align-center
    >
      <template v-if="activeDept">
        <div class="dialog-head">
          <div class="dialog-avatar">{{ activeDept.name.charAt(0) }}</div>
          <div>
            <h3 class="dialog-title">{{ activeDept.name }}</h3>
            <p class="dialog-sub">{{ deptCode(activeDept.name) }} · 深度学习指标</p>
          </div>
          <button type="button" class="dialog-close" @click="deptDialogVisible = false">
            <el-icon :size="18"><Close /></el-icon>
          </button>
        </div>

        <div class="dialog-kpi">
          <span class="dialog-kpi-label">关键绩效指标 KPI</span>
          <div class="dialog-kpi-grid">
            <div>
              <span class="kpi-name">平均进度</span>
              <span class="kpi-val">{{ activeDept.avgProgress }}%</span>
            </div>
            <div>
              <span class="kpi-name">参训人数</span>
              <span class="kpi-val">{{ activeDept.learnedMembers }}/{{ activeDept.memberCount }}</span>
            </div>
            <div>
              <span class="kpi-name">完成章节</span>
              <span class="kpi-val">{{ activeDept.completedChapters }}</span>
            </div>
          </div>
        </div>
      </template>

      <template #footer>
        <el-button @click="deptDialogVisible = false">关闭面板</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  View, User, UserFilled, Sunny, OfficeBuilding, EditPen, Setting,
  Bell, Search, Refresh, Document, Close,
} from '@element-plus/icons-vue'
import AdminQuickActionCard from '@/components/admin/AdminQuickActionCard.vue'
import AdminLiveStatCard from '@/components/admin/AdminLiveStatCard.vue'
import AdminAnalyticsPanel from '@/components/admin/AdminAnalyticsPanel.vue'
import request from '@/api/request'

const router = useRouter()
const searchKeyword = ref('')
const refreshing = ref(false)
const pulseOnline = ref(false)
const currentTime = ref('')
const deptDialogVisible = ref(false)
const activeDept = ref<DeptProgress | null>(null)
const chapterTotal = 10

let clockTimer: ReturnType<typeof setInterval> | null = null

interface DeptProgress {
  name: string
  memberCount: number
  learnedMembers: number
  completedChapters: number
  avgProgress: number
}

const learning = reactive({
  onlineLearners: 0,
  activeToday: 0,
  totalLearners: 0,
  departments: [] as DeptProgress[],
})

const platform = reactive({
  totalCourses: 0,
})

const dateLabel = computed(() => {
  const now = new Date()
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 · ${weekdays[now.getDay()]}`
})

function updateClock() {
  const now = new Date()
  const h = String(now.getHours()).padStart(2, '0')
  const m = String(now.getMinutes()).padStart(2, '0')
  const s = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${h}:${m}:${s}`
}

function progressLevel(pct: number): 'high' | 'mid' | 'low' {
  if (pct > 80) return 'high'
  if (pct > 40) return 'mid'
  return 'low'
}

function deptCode(name: string): string {
  const map: Record<string, string> = {
    '检修维护部': 'DEPT-MAIN',
    '技术管理部': 'DEPT-TECH',
    '消防与应急管理部': 'DEPT-FIRE',
  }
  return map[name] || `DEPT-${name.slice(0, 2).toUpperCase()}`
}

function openDeptDetail(row: DeptProgress) {
  activeDept.value = row
  deptDialogVisible.value = true
}

function handleSearch() {
  const q = searchKeyword.value.trim()
  if (!q) return
  router.push({ path: '/admin/learning/courses', query: { q } })
}

async function loadData() {
  try {
    const overviewRes = await request.get('/admin/learning-overview')
    Object.assign(learning, overviewRes.data)
  } catch (err) {
    console.warn('加载学习总览失败', err)
  }

  try {
    const statsRes = await request.get('/admin/stats')
    platform.totalCourses = statsRes.data.totalCourses ?? 0
  } catch (err) {
    console.warn('加载平台统计失败', err)
  }
}

async function refreshData() {
  refreshing.value = true
  await loadData()
  setTimeout(() => {
    refreshing.value = false
    pulseOnline.value = true
    setTimeout(() => { pulseOnline.value = false }, 1000)
  }, 800)
}

const quickLinks = computed(() => [
  {
    path: '/admin/learning/courses',
    icon: EditPen,
    tone: 'blue' as const,
    title: '课程管理',
    desc: '配置课程、章节与推演分类树，统筹安防逻辑',
    meta: `${platform.totalCourses || 0} 门课程已发布`,
    badge: '配置中心',
  },
  {
    path: '/admin/users',
    icon: User,
    tone: 'green' as const,
    title: '用户管理',
    desc: '集中管控账号体系、系统角色和对应业务部门权限',
    meta: `${learning.totalLearners || 0} 位在册学员`,
    badge: '组织核心',
  },
  {
    path: '/admin/settings',
    icon: Setting,
    tone: 'orange' as const,
    title: '系统设置',
    desc: '定制全局平台参数、安防硬件联动协议与运行日志',
    meta: '21 项全局安全预设',
    badge: '整站配置',
  },
  {
    path: '/user/dashboard',
    icon: View,
    tone: 'purple' as const,
    title: '学员端预览',
    desc: '无缝免密切换至学员视图，体验完整的安防模拟演练',
    meta: '预览环境就绪',
    badge: '学员环境',
  },
])

onMounted(() => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  loadData()
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<style scoped>
.admin-dashboard {
  width: 100%;
  box-sizing: border-box;
}

/* ── 顶栏 ── */
.dash-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.dash-search {
  width: 320px;
  flex-shrink: 0;
}

.dash-search :deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 2px 14px;
  min-height: 34px;
  box-shadow: none;
  border: 1px solid rgba(226, 232, 240, 0.8);
  background: rgba(248, 250, 252, 0.6);
  font-size: 12px;
}

.dash-search :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
  border-color: #3b82f6;
}

.dash-search :deep(.el-input__inner) {
  font-size: 12px;
  color: #1e293b;
}

.dash-search :deep(.el-input__inner::placeholder) {
  color: #94a3b8;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.toolbar-btn {
  position: relative;
  width: auto;
  height: auto;
  padding: 8px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s, color 0.2s;
}

.toolbar-btn:hover {
  background: #f1f5f9;
  color: #2563eb;
}

.notify-dot {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f43f5e;
  border: 2px solid #fff;
}

.online-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: #059669;
  background: #ecfdf5;
  border: 1px solid #d1fae5;
  border-radius: 999px;
}

.online-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  animation: pulse-dot 1.8s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* ── 工作台标题 ── */
.hero-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.hero-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.hero-badge {
  font-size: 12px;
  font-weight: 600;
  color: #2563eb;
  background: #eff6ff;
  padding: 2px 10px;
  border-radius: 999px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.hero-badge--course {
  color: #059669;
  background: #ecfdf5;
  text-transform: none;
  letter-spacing: 0;
}

.hero-dot {
  color: #94a3b8;
  font-size: 12px;
}

.hero-sub {
  font-size: 12px;
  color: #64748b;
}

.hero-title {
  margin: 0;
  font-size: 24px;
  font-weight: 900;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.hero-desc {
  margin: 4px 0 0;
  font-size: 12px;
  color: #94a3b8;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  font-size: 12px;
  color: #334155;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  cursor: pointer;
  transition: background 0.2s, transform 0.15s;
}

.refresh-btn:hover {
  background: #f8fafc;
}

.refresh-btn:active {
  transform: scale(0.97);
}

.refresh-btn.spinning :deep(.el-icon) {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.clock-box {
  padding: 8px 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  text-align: right;
}

.clock-label {
  display: block;
  font-size: 10px;
  font-weight: 700;
  color: #94a3b8;
  letter-spacing: 0.12em;
  line-height: 1;
}

.clock-value {
  font-size: 12px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 0.02em;
}

/* ── 区块标题 ── */
.dash-section {
  margin-bottom: 24px;
}

.section-heading {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  font-size: 12px;
  font-weight: 900;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.heading-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.heading-dot.tone-blue { background: #3b82f6; }
.heading-dot.tone-green { background: #10b981; }

.heading-dot.ping {
  animation: ping 1.5s cubic-bezier(0, 0, 0.2, 1) infinite;
}

@keyframes ping {
  75%, 100% { transform: scale(1.8); opacity: 0; }
}

.section-head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.section-head-row .section-heading {
  margin-bottom: 0;
}

.section-hint {
  font-size: 12px;
  font-weight: 500;
  color: #94a3b8;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.live-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

/* ── 部门表格 ── */
.dept-panel {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(226, 232, 240, 0.7);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}

.dept-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.dept-table thead tr {
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  background: rgba(248, 250, 252, 0.5);
}

.dept-table th {
  padding: 12px 20px;
  font-size: 12px;
  font-weight: 700;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.dept-table td {
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.dept-table tbody tr {
  background: #fff;
  transition: background 0.15s;
}

.dept-table tbody tr:hover {
  background: rgba(248, 250, 252, 0.6);
}

.dept-table tbody tr:last-child td {
  border-bottom: none;
}

.col-center { text-align: center; }
.col-right { text-align: right; }

.dept-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.high {
  background: #10b981;
  box-shadow: 0 0 0 4px #ecfdf5;
}

.status-dot.mid {
  background: #f59e0b;
  box-shadow: 0 0 0 4px #fffbeb;
}

.status-dot.low {
  background: #cbd5e1;
  box-shadow: 0 0 0 4px #f8fafc;
}

.dept-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.dept-code {
  display: block;
  margin-top: 2px;
  font-size: 10px;
  font-weight: 500;
  color: #94a3b8;
}

.member-pill {
  display: inline-flex;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 700;
  color: #475569;
  background: #f1f5f9;
  border-radius: 999px;
}

.chapter-num {
  font-size: 14px;
  font-weight: 900;
  color: #334155;
}

.chapter-total {
  font-size: 12px;
  color: #94a3b8;
  margin-left: 2px;
}

.progress-wrap {
  max-width: 200px;
}

.progress-pct {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 900;
}

.progress-pct.high { color: #059669; }
.progress-pct.mid { color: #f59e0b; }
.progress-pct.low { color: #94a3b8; }

.progress-track {
  width: 100%;
  height: 6px;
  background: #f1f5f9;
  border-radius: 999px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 1s ease;
}

.progress-fill.high {
  background: linear-gradient(to right, #34d399, #059669);
  box-shadow: 0 1px 4px rgba(16, 185, 129, 0.25);
}

.progress-fill.mid {
  background: linear-gradient(to right, #fbbf24, #f59e0b);
  box-shadow: 0 1px 4px rgba(245, 158, 11, 0.25);
}

.progress-fill.low {
  background: #cbd5e1;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.row-btn {
  padding: 6px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  transition: background 0.2s, color 0.2s;
}

.row-btn:hover {
  background: #f1f5f9;
  color: #2563eb;
}

.row-btn:last-child:hover {
  color: #059669;
}

/* ── 弹窗 ── */
.dialog-head {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
}

.dialog-avatar {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #eff6ff;
  color: #2563eb;
  font-weight: 700;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dialog-title {
  margin: 0;
  font-size: 18px;
  font-weight: 900;
  color: #0f172a;
}

.dialog-sub {
  margin: 2px 0 0;
  font-size: 12px;
  color: #94a3b8;
}

.dialog-close {
  position: absolute;
  top: 0;
  right: 0;
  padding: 6px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
}

.dialog-close:hover {
  background: #f1f5f9;
  color: #475569;
}

.dialog-kpi {
  margin-top: 20px;
  padding: 16px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 12px;
}

.dialog-kpi-label {
  display: block;
  font-size: 10px;
  font-weight: 700;
  color: #94a3b8;
  margin-bottom: 12px;
}

.dialog-kpi-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.kpi-name {
  display: block;
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 2px;
}

.kpi-val {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

@media (max-width: 1200px) {
  .action-grid,
  .live-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .dash-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .dash-search {
    width: 100%;
  }

  .toolbar-actions {
    justify-content: flex-end;
  }

  .hero-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .action-grid,
  .live-grid {
    grid-template-columns: 1fr;
  }

  .admin-dashboard {
    padding: 16px;
  }

  .dept-panel {
    overflow-x: auto;
  }

  .dept-table {
    min-width: 640px;
  }
}
</style>
