<template>
  <div class="sl-page my-certificates" v-loading="loading">
    <h2 class="sl-page-title">我的证书</h2>
    <p class="page-sub">
      支持基础结业、高级结业、专业认证三类模板，含有效期管理与续证机制
    </p>

    <!-- 模板说明 -->
    <div v-if="templates.length" class="template-legend">
      <div v-for="tpl in templates" :key="tpl.code" class="legend-item">
        <span class="legend-dot" :style="{ background: tpl.accentColor }"></span>
        <span class="legend-name">{{ tpl.name }}</span>
        <span class="legend-meta">有效期 {{ tpl.validityMonths }} 个月</span>
      </div>
    </div>

    <el-row v-if="certificates.length" :gutter="20">
      <el-col :xs="24" :sm="12" :lg="8" v-for="cert in certificates" :key="cert.id">
        <div
          class="cert-card"
          :style="cardStyle(cert)"
          @click="openCert(cert)"
        >
          <div class="cert-top">
            <span class="cert-badge">{{ cert.template?.badgeLabel || '结业' }}</span>
            <el-tag
              :type="statusTagType(cert.status)"
              size="small"
              effect="dark"
              class="status-tag"
            >
              {{ statusLabel(cert) }}
            </el-tag>
          </div>
          <h3>{{ cert.courseTitle }}</h3>
          <p class="cert-no">{{ cert.certificateNo }}</p>
          <p class="cert-date">颁发：{{ formatDate(cert.issuedAt) }}</p>
          <p class="cert-expire">有效期至：{{ formatDate(cert.expiresAt) }}</p>
          <p v-if="cert.renewalCount > 0" class="cert-renewal">已续证 {{ cert.renewalCount }} 次</p>
          <el-button type="primary" plain size="small" class="mt-3">查看证书</el-button>
        </div>
      </el-col>
    </el-row>
    <el-empty v-else description="暂无证书，完成课程或综合考试后可获得" :image-size="100" />

    <el-dialog v-model="visible" width="680px" class="cert-dialog" :show-close="true">
      <div v-if="current" class="cert-preview" id="cert-print">
        <div
          class="cert-border"
          :style="{
            borderColor: current.template?.borderColor || '#c9a227',
            '--cert-accent': current.template?.accentColor || '#1e3a8a',
          }"
        >
          <div class="cert-logo">SafeLearn · 储安云</div>
          <h1>{{ current.template?.headerTitle || '结业证书' }}</h1>
          <p class="cert-holder">{{ current.userName }}</p>
          <p class="cert-body">{{ current.bodyText || `已完成「${current.courseTitle}」学习，特发此证。` }}</p>
          <div class="cert-footer">
            <div>
              <div class="cert-label">证书编号</div>
              <div>{{ current.certificateNo }}</div>
            </div>
            <div>
              <div class="cert-label">颁发日期</div>
              <div>{{ formatDate(current.issuedAt) }}</div>
            </div>
            <div>
              <div class="cert-label">有效期至</div>
              <div>{{ formatDate(current.expiresAt) }}</div>
            </div>
            <div>
              <div class="cert-label">颁发单位</div>
              <div>{{ current.company || '储能安全培训平台' }}</div>
            </div>
          </div>
        </div>

        <el-alert
          v-if="current.renewal?.eligible"
          type="success"
          :closable="false"
          show-icon
          class="renew-alert"
          title="已满足续证条件，可延长有效期"
        />
        <el-alert
          v-else-if="current.status === 'expired'"
          type="warning"
          :closable="false"
          show-icon
          class="renew-alert"
          :title="current.renewal?.reason || '证书已过期'"
        />
        <el-alert
          v-else-if="current.renewal?.expiringSoon"
          type="info"
          :closable="false"
          show-icon
          class="renew-alert"
          :title="`证书即将到期（剩余 ${current.renewal?.daysUntilExpiry} 天）`"
        />
      </div>
      <template #footer>
        <el-button @click="visible = false">关闭</el-button>
        <el-button
          v-if="current?.renewal?.eligible"
          type="warning"
          :loading="renewing"
          @click="handleRenew"
        >
          立即续证
        </el-button>
        <el-button
          v-else-if="needsComprehensiveExam(current)"
          type="primary"
          plain
          @click="goComprehensiveExam"
        >
          去参加综合考试
        </el-button>
        <el-button type="primary" @click="printCert">打印 / 保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  certificateApi,
  type UserCertificate,
  type CertificateTemplate,
} from '@/api/certificate'
import { useAppBase } from '@/composables/useAppBase'

const router = useRouter()
const { p } = useAppBase()

const loading = ref(true)
const renewing = ref(false)
const certificates = ref<UserCertificate[]>([])
const templates = ref<CertificateTemplate[]>([])
const visible = ref(false)
const current = ref<UserCertificate | null>(null)

function formatDate(t?: string) {
  if (!t) return '—'
  return t.replace('T', ' ').slice(0, 10)
}

function cardStyle(cert: UserCertificate) {
  const accent = cert.template?.accentColor || '#1e3a8a'
  const secondary = cert.template?.borderColor || '#2b5aed'
  return {
    background: `linear-gradient(135deg, ${accent}, ${secondary})`,
  }
}

function statusTagType(status?: string) {
  if (status === 'active') return 'success'
  if (status === 'expired') return 'danger'
  return 'info'
}

function statusLabel(cert: UserCertificate) {
  if (cert.status === 'expired') return '已过期'
  if (cert.renewal?.expiringSoon) return '即将到期'
  if (cert.status === 'active') return '有效'
  return cert.status || '未知'
}

function needsComprehensiveExam(cert: UserCertificate | null) {
  if (!cert) return false
  return cert.renewal?.renewalRequirement === 'comprehensive_exam'
    && !cert.renewal?.requirementMet
    && (cert.status === 'expired' || cert.renewal?.expiringSoon)
}

async function openCert(cert: UserCertificate) {
  try {
    const res = await certificateApi.detail(cert.id)
    current.value = res.data || cert
  } catch {
    current.value = cert
  }
  visible.value = true
}

async function handleRenew() {
  if (!current.value) return
  renewing.value = true
  try {
    const res = await certificateApi.renew(current.value.id)
    if (res.data) {
      current.value = res.data
      const idx = certificates.value.findIndex(c => c.id === res.data!.id)
      if (idx >= 0) certificates.value[idx] = res.data
      ElMessage.success(res.data.message || '续证成功')
    }
  } catch {
    ElMessage.error('续证失败，请确认是否已满足续证条件')
  } finally {
    renewing.value = false
  }
}

function goComprehensiveExam() {
  if (!current.value?.courseId) return
  visible.value = false
  router.push(p(`/courses/${current.value.courseId}/comprehensive-exam`))
}

function printCert() {
  window.print()
}

onMounted(async () => {
  try {
    const [mineRes, tplRes] = await Promise.all([
      certificateApi.mine(),
      certificateApi.templates().catch(() => ({ data: [] })),
    ])
    certificates.value = mineRes.data || []
    templates.value = tplRes.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-sub {
  color: #6b7280;
  font-size: 14px;
  margin-bottom: 20px;
}

.template-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
  margin-bottom: 24px;
  padding: 14px 18px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #475569;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-name {
  font-weight: 600;
  color: #1e293b;
}

.legend-meta {
  color: #94a3b8;
}

.cert-card {
  color: #fff;
  border-radius: 16px;
  padding: 24px;
  cursor: pointer;
  margin-bottom: 20px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.cert-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.15);
}

.cert-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.cert-badge {
  display: inline-block;
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
}

.status-tag {
  border: none;
}

.cert-card h3 {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 8px;
}

.cert-no {
  font-size: 12px;
  opacity: 0.85;
  font-family: monospace;
}

.cert-date,
.cert-expire,
.cert-renewal {
  font-size: 12px;
  opacity: 0.8;
  margin-top: 4px;
}

.mt-3 {
  margin-top: 12px;
}

.cert-preview {
  padding: 8px;
}

.cert-border {
  border: 3px double var(--cert-accent, #c9a227);
  padding: 40px 32px;
  text-align: center;
  background: linear-gradient(180deg, #fffef8, #fff);
}

.cert-logo {
  font-size: 14px;
  color: #6b7280;
  letter-spacing: 2px;
}

.cert-border h1 {
  font-size: 32px;
  color: var(--cert-accent, #1e3a8a);
  margin: 16px 0 24px;
  letter-spacing: 8px;
}

.cert-holder {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 20px;
}

.cert-body {
  font-size: 16px;
  line-height: 1.8;
  color: #374151;
  max-width: 520px;
  margin: 0 auto 32px;
}

.cert-footer {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  text-align: center;
  font-size: 13px;
  color: #374151;
}

.cert-label {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 4px;
}

.renew-alert {
  margin-top: 16px;
}

@media print {
  body * {
    visibility: hidden;
  }
  #cert-print,
  #cert-print * {
    visibility: visible;
  }
  #cert-print {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
  }
}
</style>
