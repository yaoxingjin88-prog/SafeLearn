<template>
  <div class="sl-page my-certificates" v-loading="loading">
    <h2 class="sl-page-title">我的证书</h2>
    <p class="page-sub">完成含高级实操章节的课程后，系统自动颁发结业证书</p>

    <el-row v-if="certificates.length" :gutter="20">
      <el-col :span="8" v-for="cert in certificates" :key="cert.id">
        <div class="cert-card" @click="openCert(cert)">
          <div class="cert-badge">高级结业</div>
          <h3>{{ cert.courseTitle }}</h3>
          <p class="cert-no">{{ cert.certificateNo }}</p>
          <p class="cert-date">{{ formatDate(cert.issuedAt) }}</p>
          <el-button type="primary" plain size="small" class="mt-3">查看证书</el-button>
        </div>
      </el-col>
    </el-row>
    <el-empty v-else description="暂无证书，完成高级课程后可获得" :image-size="100" />

    <el-dialog v-model="visible" width="640px" class="cert-dialog" :show-close="true">
      <div v-if="current" class="cert-preview" id="cert-print">
        <div class="cert-border">
          <div class="cert-logo">SafeLearn · 储安云</div>
          <h1>结业证书</h1>
          <p class="cert-holder">{{ current.userName }}</p>
          <p class="cert-body">
            已完成 <strong>{{ current.courseTitle }}</strong> 全部高级课程学习，
            经考核达到结业标准，特发此证。
          </p>
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
              <div class="cert-label">颁发单位</div>
              <div>{{ current.company || '储能安全培训平台' }}</div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="visible = false">关闭</el-button>
        <el-button type="primary" @click="printCert">打印 / 保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { certificateApi, type UserCertificate } from '@/api/certificate'

const loading = ref(true)
const certificates = ref<UserCertificate[]>([])
const visible = ref(false)
const current = ref<UserCertificate | null>(null)

function formatDate(t?: string) {
  if (!t) return ''
  return t.replace('T', ' ').slice(0, 10)
}

function openCert(cert: UserCertificate) {
  current.value = cert
  visible.value = true
}

function printCert() {
  window.print()
}

onMounted(async () => {
  try {
    const res = await certificateApi.mine()
    certificates.value = res.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-sub { color: #6b7280; font-size: 14px; margin-bottom: 24px; }
.cert-card {
  background: linear-gradient(135deg, #1e3a8a, #2b5aed);
  color: #fff;
  border-radius: 16px;
  padding: 24px;
  cursor: pointer;
  margin-bottom: 20px;
  transition: transform 0.2s;
}
.cert-card:hover { transform: translateY(-4px); }
.cert-badge { display: inline-block; background: rgba(255,255,255,0.2); padding: 4px 10px; border-radius: 20px; font-size: 12px; margin-bottom: 12px; }
.cert-card h3 { font-size: 18px; font-weight: 700; margin-bottom: 8px; }
.cert-no { font-size: 12px; opacity: 0.85; font-family: monospace; }
.cert-date { font-size: 12px; opacity: 0.7; margin-top: 4px; }
.mt-3 { margin-top: 12px; }
.cert-preview { padding: 8px; }
.cert-border {
  border: 3px double #c9a227;
  padding: 40px 32px;
  text-align: center;
  background: linear-gradient(180deg, #fffef8, #fff);
}
.cert-logo { font-size: 14px; color: #6b7280; letter-spacing: 2px; }
.cert-border h1 { font-size: 32px; color: #1e3a8a; margin: 16px 0 24px; letter-spacing: 8px; }
.cert-holder { font-size: 28px; font-weight: 700; color: #111827; margin-bottom: 20px; }
.cert-body { font-size: 16px; line-height: 1.8; color: #374151; max-width: 480px; margin: 0 auto 32px; }
.cert-footer { display: flex; justify-content: space-around; text-align: center; font-size: 13px; color: #374151; }
.cert-label { font-size: 12px; color: #9ca3af; margin-bottom: 4px; }
@media print {
  body * { visibility: hidden; }
  #cert-print, #cert-print * { visibility: visible; }
  #cert-print { position: absolute; left: 0; top: 0; width: 100%; }
}
</style>
