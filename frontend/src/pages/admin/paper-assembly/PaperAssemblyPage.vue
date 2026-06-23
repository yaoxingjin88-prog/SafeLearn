<template>
  <div class="paper-assembly-page" v-loading="loading">
    <el-tabs v-model="activeTab" class="assembly-tabs">
      <el-tab-pane label="智能组卷" name="smart" />
      <el-tab-pane label="手动组卷" name="manual" />
      <el-tab-pane label="速导入组卷" name="import" />
    </el-tabs>

    <div v-if="activeTab === 'smart'" class="assembly-layout">
      <div class="config-column">
        <section class="config-card">
          <div class="section-head"><span class="section-no">1</span>选择题库范围</div>
          <div class="form-field">
            <label>选择分类</label>
            <el-select v-model="config.categoryIds" multiple collapse-tags collapse-tags-tooltip placeholder="请选择题库分类" class="w-full">
              <el-option v-for="cat in categoryOptions" :key="cat.id" :label="cat.name" :value="cat.id" />
            </el-select>
          </div>
          <div class="exclude-row">
            <span>排除题目</span>
            <el-switch v-model="config.excludeRecent" />
          </div>
          <el-checkbox v-model="excludeRecentChecked" :disabled="!config.excludeRecent">
            排除已在最近 {{ config.excludeMonths }} 个月考过的题目
          </el-checkbox>
          <div class="section-action">
            <el-button type="primary" @click="generatePaper">一键组卷</el-button>
          </div>
        </section>

        <section class="config-card">
          <div class="section-head"><span class="section-no">2</span>题型与数量设置</div>
          <table class="rule-table">
            <thead>
              <tr>
                <th>题型</th>
                <th>题目数量</th>
                <th>每题分值</th>
                <th>小计分值</th>
                <th>难度分布</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="rule in config.typeRules" :key="rule.type">
                <td>{{ rule.label || typeLabel(rule.type) }}</td>
                <td><el-input-number v-model="rule.count" :min="0" :max="200" size="small" controls-position="right" @change="syncExamScores" /></td>
                <td>{{ rule.scorePerQuestion }} 分</td>
                <td>{{ calcRuleSubtotal(rule) }} 分</td>
                <td class="ratio-cell">{{ formatDifficultyRatio(rule.difficultyRatio) }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td>合计</td>
                <td>{{ previewTotals.totalQuestions }} <span class="muted">(由随机)</span></td>
                <td />
                <td>{{ previewTotals.totalScore }} 分</td>
                <td />
              </tr>
            </tfoot>
          </table>
        </section>

        <section class="config-card">
          <div class="section-head"><span class="section-no">3</span>考试设置</div>
          <div class="form-grid">
            <div class="form-field span-2">
              <label>考试名称</label>
              <el-input v-model="config.examSettings.title" />
            </div>
            <div class="form-field">
              <label>考试类型</label>
              <el-radio-group v-model="config.examSettings.examType">
                <el-radio value="formal">正式考试</el-radio>
                <el-radio value="mock">模拟考试</el-radio>
              </el-radio-group>
            </div>
            <div class="form-field">
              <label>考试时长</label>
              <el-input-number v-model="config.examSettings.timeLimit" :min="1" :max="300" /> <span class="suffix">分钟</span>
            </div>
            <div class="form-field">
              <label>总分</label>
              <el-input-number v-model="config.examSettings.totalScore" :min="1" :max="1000" /> <span class="suffix">分</span>
            </div>
            <div class="form-field">
              <label>及格线</label>
              <el-input-number v-model="config.examSettings.passScore" :min="0" :max="1000" />
              <span class="suffix">分 ({{ config.examSettings.passPercent }}%)</span>
            </div>
            <div class="form-field">
              <label>及格后可查看</label>
              <el-select v-model="config.examSettings.resultView" class="w-full">
                <el-option label="仅分数" value="score_only" />
                <el-option label="分数与解析" value="score_analysis" />
              </el-select>
            </div>
            <div class="form-field span-2">
              <label>考试说明</label>
              <el-input v-model="config.examSettings.description" type="textarea" :rows="2" maxlength="300" show-word-limit />
            </div>
          </div>
        </section>

        <section class="config-card">
          <div class="section-head"><span class="section-no">4</span>防作弊与规则</div>
          <div class="switch-grid">
            <div class="switch-item"><span>乱序展示题目</span><el-switch v-model="config.antiCheat.shuffleQuestions" /></div>
            <div class="switch-item"><span>乱序展示选项</span><el-switch v-model="config.antiCheat.shuffleOptions" /></div>
            <div class="switch-item"><span>禁止切屏</span><el-switch v-model="config.antiCheat.preventSwitchScreen" /></div>
            <div class="switch-item"><span>全屏考试</span><el-switch v-model="config.antiCheat.fullscreen" /></div>
          </div>
          <div class="inline-fields">
            <div class="form-field">
              <label>切屏次数限制</label>
              <el-input-number v-model="config.antiCheat.switchLimit" :min="0" :max="20" /> <span class="suffix">次</span>
            </div>
            <div class="form-field">
              <label>允许考试次数</label>
              <el-input-number v-model="config.antiCheat.attemptLimit" :min="1" :max="10" /> <span class="suffix">次</span>
            </div>
          </div>
          <el-checkbox v-model="config.antiCheat.autoSubmitOnTimeout">时间截止后自动提交</el-checkbox>
        </section>

        <section class="config-card">
          <div class="section-head"><span class="section-no">5</span>发布时间与范围</div>
          <div class="form-field">
            <label>发布时间</label>
            <el-radio-group v-model="config.publish.mode">
              <el-radio value="immediate">立即发布</el-radio>
              <el-radio value="scheduled">定时发布</el-radio>
            </el-radio-group>
            <el-date-picker
              v-if="config.publish.mode === 'scheduled'"
              v-model="config.publish.scheduledAt"
              type="datetime"
              placeholder="选择发布时间"
              value-format="YYYY-MM-DD HH:mm"
              class="schedule-picker"
            />
          </div>
          <div class="form-field">
            <label>适用范围</label>
            <el-radio-group v-model="config.publish.scope">
              <el-radio value="all">全员可见</el-radio>
              <el-radio value="department">指定部门/人员</el-radio>
            </el-radio-group>
          </div>
        </section>
      </div>

      <aside class="preview-column">
        <div class="preview-card stats-card">
          <div class="stat-item"><span>总题数</span><strong>{{ livePreview.totalQuestions }} 题</strong></div>
          <div class="stat-item"><span>总分值</span><strong>{{ livePreview.totalScore }} 分</strong></div>
          <div class="stat-item"><span>考试时长</span><strong>{{ config.examSettings.timeLimit }} 分钟</strong></div>
          <div class="stat-item"><span>及格分数</span><strong>{{ config.examSettings.passScore }} 分</strong></div>
        </div>
        <div class="preview-card chart-card">
          <div class="chart-title">题型分布</div>
          <VChart class="distribution-chart" :option="chartOption" autoresize />
          <div class="chart-legend">
            <div v-for="(item, idx) in chartLegend" :key="item.type" class="legend-item">
              <i :style="{ background: PAPER_CHART_COLORS[idx] }" />
              <span>{{ item.label }} ({{ item.count }})</span>
              <em>{{ item.percent }}%</em>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <div v-else class="placeholder-panel">
      <el-empty :description="activeTab === 'manual' ? '手动组卷功能开发中，请使用智能组卷' : '速导入组卷功能开发中，请使用智能组卷'" />
    </div>

    <div class="footer-actions">
      <el-button @click="goBack">取消</el-button>
      <el-button @click="saveDraft">保存草稿</el-button>
      <el-button @click="previewPaper">预览试卷</el-button>
      <el-button type="primary" @click="publishExam">发布考试</el-button>
    </div>

    <el-dialog v-model="previewVisible" title="试卷预览" width="720px">
      <div v-if="generatedQuestions.length" class="preview-list">
        <div v-for="(q, idx) in generatedQuestions" :key="q.id" class="preview-item">
          <div class="preview-index">{{ idx + 1 }}.</div>
          <div>
            <div class="preview-type">{{ typeLabel(q.type) }} · {{ q.score }} 分</div>
            <div class="preview-content">{{ q.question }}</div>
          </div>
        </div>
      </div>
      <el-empty v-else description="请先点击「一键组卷」生成试卷" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import { LegendComponent, TooltipComponent } from 'echarts/components'
import type { EChartsOption } from 'echarts'
import { adminApi } from '@/api/admin'
import {
  PAPER_CHART_COLORS,
  calcRuleSubtotal,
  calcTotals,
  formatDifficultyRatio,
  typeLabel,
} from './paperAssemblyShared'

use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent])

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const activeTab = ref('smart')
const categoryOptions = ref<Array<{ id: string; name: string }>>([])
const paperId = ref<string | null>(null)
const questionIds = ref<string[]>([])
const generatedQuestions = ref<Array<{ id: string; type: string; question: string; score: number }>>([])
const previewVisible = ref(false)
const generatedPreview = ref<{ totalQuestions: number; totalScore: number; distribution: Array<{ type: string; typeLabel: string; count: number; percent: number }> } | null>(null)

const config = reactive({
  categoryIds: [] as string[],
  excludeRecent: true,
  excludeMonths: 3,
  typeRules: [] as Array<{
    type: string
    label?: string
    count: number
    scorePerQuestion: number
    difficultyRatio: { easy: number; medium: number; hard: number }
  }>,
  examSettings: {
    title: '',
    examType: 'formal',
    timeLimit: 60,
    totalScore: 0,
    passScore: 0,
    passPercent: 60,
    resultView: 'score_only',
    description: '',
  },
  antiCheat: {
    shuffleQuestions: true,
    shuffleOptions: true,
    preventSwitchScreen: true,
    fullscreen: true,
    switchLimit: 3,
    attemptLimit: 2,
    autoSubmitOnTimeout: true,
  },
  publish: {
    mode: 'immediate' as 'immediate' | 'scheduled',
    scheduledAt: null as string | null,
    scope: 'all' as 'all' | 'department',
  },
})

const excludeRecentChecked = computed({
  get: () => config.excludeRecent,
  set: (v: boolean) => { config.excludeRecent = v },
})

const previewTotals = computed(() => {
  if (generatedPreview.value) {
    return {
      totalQuestions: generatedPreview.value.totalQuestions,
      totalScore: generatedPreview.value.totalScore,
    }
  }
  return calcTotals(config.typeRules)
})

const livePreview = computed(() => {
  const totals = previewTotals.value
  return {
    totalQuestions: totals.totalQuestions,
    totalScore: totals.totalScore || config.examSettings.totalScore,
  }
})

const chartLegend = computed(() => {
  const dist = generatedPreview.value?.distribution
  if (dist?.length) {
    return dist.map(d => ({
      type: d.type,
      label: d.typeLabel || typeLabel(d.type),
      count: d.count,
      percent: d.percent,
    }))
  }
  const total = previewTotals.value.totalQuestions || 1
  return config.typeRules
    .filter(r => r.count > 0)
    .map(r => ({
      type: r.type,
      label: r.label || typeLabel(r.type),
      count: r.count,
      percent: Math.round((r.count / total) * 1000) / 10,
    }))
})

const chartOption = computed<EChartsOption>(() => ({
  color: PAPER_CHART_COLORS,
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: ['52%', '72%'],
    center: ['50%', '48%'],
    label: { show: false },
    data: chartLegend.value.map(item => ({
      name: item.label,
      value: item.count,
    })),
  }],
}))

function syncExamScores() {
  const { totalScore } = calcTotals(config.typeRules)
  config.examSettings.totalScore = totalScore
  config.examSettings.passScore = Math.round(totalScore * config.examSettings.passPercent / 100)
}

watch(() => config.examSettings.passPercent, () => syncExamScores())

function buildPayload(status: 'draft' | 'published' = 'draft') {
  return {
    id: paperId.value || undefined,
    title: config.examSettings.title,
    mode: activeTab.value,
    examType: config.examSettings.examType,
    status,
    timeLimit: config.examSettings.timeLimit,
    totalScore: config.examSettings.totalScore || livePreview.value.totalScore,
    passScore: config.examSettings.passScore,
    config: {
      categoryIds: config.categoryIds,
      excludeRecent: config.excludeRecent,
      excludeMonths: config.excludeMonths,
      typeRules: config.typeRules,
      examSettings: config.examSettings,
      antiCheat: config.antiCheat,
      publish: config.publish,
    },
    questionIds: questionIds.value,
    questionsSnapshot: generatedQuestions.value,
  }
}

async function loadMeta() {
  const [cats, defaults] = await Promise.all([
    adminApi.getPaperCategoryOptions(),
    adminApi.getPaperDefaultConfig(),
  ])
  categoryOptions.value = cats.data || []
  if (!paperId.value) {
    applyConfig(defaults.data)
  }
}

function applyConfig(data: Record<string, unknown>) {
  config.categoryIds = (data.categoryIds as string[]) || []
  config.excludeRecent = Boolean(data.excludeRecent ?? true)
  config.excludeMonths = Number(data.excludeMonths ?? 3)
  config.typeRules = ((data.typeRules as typeof config.typeRules) || []).map(r => ({
    ...r,
    label: typeLabel(r.type),
  }))
  const exam = (data.examSettings as typeof config.examSettings) || config.examSettings
  Object.assign(config.examSettings, exam)
  const anti = (data.antiCheat as typeof config.antiCheat) || config.antiCheat
  Object.assign(config.antiCheat, anti)
  const pub = (data.publish as typeof config.publish) || config.publish
  Object.assign(config.publish, pub)
  syncExamScores()
}

async function loadPaper(id: string) {
  const res = await adminApi.getPaperById(id)
  const data = res.data
  paperId.value = data.id
  questionIds.value = data.questionIds || []
  generatedQuestions.value = (data.questions || []) as typeof generatedQuestions.value
  generatedPreview.value = data.preview || null
  if (data.config) applyConfig(data.config as Record<string, unknown>)
  if (data.title) config.examSettings.title = data.title
}

async function generatePaper() {
  if (!config.categoryIds.length) {
    ElMessage.warning('请至少选择一个题库分类')
    return
  }
  loading.value = true
  try {
    const res = await adminApi.generatePaper({ config: buildPayload().config })
    const data = res.data
    questionIds.value = (data.questionIds as string[]) || []
    generatedQuestions.value = (data.questions as typeof generatedQuestions.value) || []
    generatedPreview.value = {
      totalQuestions: Number(data.totalQuestions ?? 0),
      totalScore: Number(data.totalScore ?? 0),
      distribution: data.distribution || [],
    }
    config.examSettings.totalScore = generatedPreview.value.totalScore
    config.examSettings.passScore = Math.round(generatedPreview.value.totalScore * config.examSettings.passPercent / 100)
    ElMessage.success(`已抽取 ${generatedPreview.value.totalQuestions} 道题目`)
  } finally {
    loading.value = false
  }
}

async function saveDraft() {
  if (!config.examSettings.title.trim()) {
    ElMessage.warning('请填写考试名称')
    return
  }
  loading.value = true
  try {
    const res = paperId.value
      ? await adminApi.updatePaper(paperId.value, buildPayload('draft'))
      : await adminApi.savePaper(buildPayload('draft'))
    paperId.value = res.data.id
    ElMessage.success('草稿已保存')
  } finally {
    loading.value = false
  }
}

function previewPaper() {
  if (!generatedQuestions.value.length) {
    ElMessage.warning('请先一键组卷')
    return
  }
  previewVisible.value = true
}

async function publishExam() {
  if (!config.examSettings.title.trim()) {
    ElMessage.warning('请填写考试名称')
    return
  }
  if (!questionIds.value.length) {
    ElMessage.warning('请先一键组卷')
    return
  }
  loading.value = true
  try {
    await adminApi.publishPaperDirect(buildPayload('published'))
    ElMessage.success('考试已发布')
    router.push('/admin/learning/exams')
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/admin/learning/question-bank')
}

onMounted(async () => {
  loading.value = true
  try {
    await loadMeta()
    const editId = route.query.id as string | undefined
    if (editId) await loadPaper(editId)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.paper-assembly-page {
  padding: 16px 24px 96px;
  min-height: 100%;
  box-sizing: border-box;
}

.assembly-tabs {
  margin-bottom: 16px;
}

.assembly-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
  align-items: start;
}

.config-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-card {
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 10px;
  padding: 18px 20px;
}

.section-head {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 16px;
}

.section-no {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #2563eb;
  color: #fff;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.form-field {
  margin-bottom: 14px;
}

.form-field label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  color: #667085;
}

.w-full { width: 100%; }

.exclude-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
  color: #374151;
}

.section-action {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.rule-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.rule-table th,
.rule-table td {
  padding: 10px 8px;
  border-bottom: 1px solid #f1f5f9;
  text-align: left;
}

.rule-table th {
  color: #667085;
  font-weight: 600;
  background: #fafbfc;
}

.ratio-cell {
  color: #667085;
  font-size: 12px;
}

.muted {
  color: #9ca3af;
  font-size: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 16px;
}

.span-2 { grid-column: span 2; }

.suffix {
  margin-left: 6px;
  color: #9ca3af;
  font-size: 12px;
}

.switch-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
}

.switch-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #374151;
}

.inline-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 10px;
}

.schedule-picker {
  margin-top: 10px;
  width: 100%;
}

.preview-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 16px;
}

.preview-card {
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 10px;
  padding: 16px;
}

.stats-card {
  display: grid;
  gap: 12px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #667085;
}

.stat-item strong {
  color: #1f2937;
  font-size: 18px;
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #1f2937;
}

.distribution-chart {
  height: 180px;
}

.chart-legend {
  display: grid;
  gap: 8px;
  margin-top: 8px;
}

.legend-item {
  display: grid;
  grid-template-columns: 10px 1fr auto;
  gap: 8px;
  align-items: center;
  font-size: 12px;
  color: #374151;
}

.legend-item i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.legend-item em {
  font-style: normal;
  color: #9ca3af;
}

.placeholder-panel {
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 10px;
  padding: 48px 20px;
}

.footer-actions {
  position: fixed;
  right: 24px;
  bottom: 24px;
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #e8edf3;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.preview-list {
  max-height: 60vh;
  overflow: auto;
  display: grid;
  gap: 12px;
}

.preview-item {
  display: flex;
  gap: 10px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.preview-index {
  color: #2563eb;
  font-weight: 600;
}

.preview-type {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 4px;
}

.preview-content {
  font-size: 14px;
  color: #1f2937;
  line-height: 1.5;
}

@media (max-width: 1100px) {
  .assembly-layout {
    grid-template-columns: 1fr;
  }
  .preview-column {
    position: static;
  }
}
</style>
