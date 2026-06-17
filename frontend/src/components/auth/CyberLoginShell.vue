<template>
  <div class="cyber-login" :class="{ 'is-admin': isAdmin }">
    <div class="cyber-grid" />
    <div class="glow glow-tl" />
    <div class="glow glow-br" />
    <div class="scan-bar" :class="{ 'is-admin': isAdmin }" />

    <div class="login-shell">
      <!-- 左侧品牌区 -->
      <div class="panel-left">
        <div class="panel-top">
          <div class="status-group">
            <div class="status-dot-wrap">
              <span class="status-ping" />
              <span class="status-dot" />
            </div>
            <span class="status-text">System Status: Ready</span>
          </div>
          <span class="version-badge">V3.4.2-Secure</span>
        </div>

        <div class="hero-block">
          <div class="battery-wrap">
            <svg class="orbit orbit-cw" viewBox="0 0 100 100">
              <circle cx="50" cy="50" r="46" fill="none" stroke-width="1.5" stroke-dasharray="10 15 30 10" opacity="0.6" />
            </svg>
            <svg class="orbit orbit-ccw" viewBox="0 0 100 100">
              <circle cx="50" cy="50" r="44" fill="none" stroke-dasharray="40 10 5 15" stroke-width="1" opacity="0.4" />
            </svg>
            <div class="battery-core">
              <div class="battery-fill" :style="{ height: currentCharge + '%' }" />
              <svg class="bolt-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
              <span class="charge-label">{{ currentCharge }}% CAPACITY</span>
            </div>
          </div>

          <div class="brand-title-wrap">
            <h2 class="hero-slogan">
              <span class="slogan-a">储能安全</span>
              <span class="slogan-b">智领未来</span>
            </h2>
            <p class="hero-tagline">智能安全培训与事故推演平台</p>
            <div class="brand-mark">
              <span class="brand-mark-line" />
              <span class="brand-product">储安云</span>
              <span class="brand-mark-line" />
            </div>
          </div>
        </div>

        <div class="feature-grid">
          <div v-for="mod in featureModules" :key="mod.title" class="feature-card">
            <div class="feature-icon" v-html="mod.svg" />
            <div>
              <h4 class="feature-name">{{ mod.title }}</h4>
              <p class="feature-desc">{{ mod.desc }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录区 -->
      <div class="panel-right">
        <div class="role-switch-wrap">
          <button type="button" class="role-switch" @click="$emit('toggle-portal')">
            <svg class="switch-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
            </svg>
            切换至：{{ isAdmin ? '学员登录' : '管理员终端' }}
          </button>
        </div>

        <div class="form-block">
          <div class="form-header">
            <h2>{{ isAdmin ? '管理员应急终端' : '学员安全入口' }}</h2>
            <p>{{ isAdmin ? '请使用高级管理密钥或凭证进行鉴权' : '欢迎回来，请输入您的学习及实操考试账号' }}</p>
          </div>

          <form class="login-form" @submit.prevent="handleSubmit">
            <div class="field">
              <label>USERNAME / 用户名</label>
              <div class="input-wrap">
                <svg class="input-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                <input v-model="form.username" type="text" required placeholder="请输入用户名/邮箱" />
              </div>
            </div>

            <div class="field">
              <div class="field-label-row">
                <label>PASSWORD / 密码</label>
                <a href="#" class="forgot-link" @click.prevent="goForgot">忘记密码?</a>
              </div>
              <div class="input-wrap">
                <svg class="input-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                </svg>
                <input
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  required
                  placeholder="请输入账户安全密码"
                />
                <button type="button" class="pwd-toggle" @click="showPassword = !showPassword">
                  <svg v-if="showPassword" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                  </svg>
                  <svg v-else fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                  </svg>
                </button>
              </div>
            </div>

            <div class="form-options">
              <label class="remember-check">
                <input v-model="form.remember" type="checkbox" />
                <span>安全保持登录</span>
              </label>
              <span class="secure-link">SECURE LINK ✔</span>
            </div>

            <button type="submit" class="submit-btn" :disabled="loading">
              <span v-if="loading" class="spinner" />
              <span v-else>{{ isAdmin ? '验证高级授权凭证 →' : '进入云平台 →' }}</span>
            </button>
          </form>
        </div>

        <div class="panel-footer">
          <div class="demo-box">
            <div>
              <p class="demo-label">Demo Account / 快捷填入</p>
              <p class="demo-cred">{{ demoAccount }}</p>
            </div>
            <button type="button" class="demo-fill" @click="fillDemo">自动填充</button>
          </div>
          <div class="footer-links">
            <span v-if="!isAdmin">
              没有实操证书？
              <router-link to="/register" class="link-accent">申请系统备案</router-link>
            </span>
            <span v-else class="footer-placeholder" />
            <a href="#" class="footer-service" @click.prevent>技术服务中心 →</a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps<{
  isAdmin?: boolean
  loading?: boolean
}>()

const emit = defineEmits<{
  submit: [payload: { username: string; password: string; remember: boolean }]
  'toggle-portal': []
}>()

const isAdmin = computed(() => props.isAdmin ?? false)
const router = useRouter()
const showPassword = ref(false)
const currentCharge = ref(78)

const form = reactive({
  username: '',
  password: '',
  remember: true,
})

const demoAccount = computed(() =>
  isAdmin.value ? 'admin / admin123' : 'zhanggong / admin123',
)

const featureModules = [
  {
    title: '阶段式安全培训',
    desc: '精细化微课学习路径',
    svg: '<svg width="20" height="20" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/></svg>',
  },
  {
    title: '3D 事故热失控演练',
    desc: '高还原实操模拟考评',
    svg: '<svg width="20" height="20" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z"/></svg>',
  },
  {
    title: 'AI 应急安全问答',
    desc: '全天候智能事故排险辅助',
    svg: '<svg width="20" height="20" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/></svg>',
  },
]

let chargeTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  chargeTimer = setInterval(() => {
    currentCharge.value = currentCharge.value >= 99 ? 75 : currentCharge.value + 1
  }, 5000)
})

onUnmounted(() => {
  if (chargeTimer) clearInterval(chargeTimer)
})

function fillDemo() {
  if (isAdmin.value) {
    form.username = 'admin'
    form.password = 'admin123'
  } else {
    form.username = 'zhanggong'
    form.password = 'admin123'
  }
}

function goForgot() {
  router.push('/forgot-password')
}

function handleSubmit() {
  emit('submit', { ...form })
}
</script>

<style scoped>
.cyber-login {
  --accent: #00f2fe;
  --accent-2: #4facfe;
  --accent-glow: rgba(0, 242, 254, 0.4);
  --grid-color: rgba(0, 242, 254, 0.04);
  --border: rgba(0, 242, 254, 0.2);
  --title-grad: linear-gradient(to right, #22d3ee, #3b82f6);
  --btn-grad: linear-gradient(to right, #06b6d4, #2563eb);
  --btn-text: #0f172a;
  --glow-tl: rgba(6, 182, 212, 0.3);
  --glow-br: rgba(37, 99, 235, 0.2);

  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 32px;
  overflow: hidden;
  background: #080c14;
  color: #f1f5f9;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  user-select: none;
}

.cyber-login::before {
  content: '';
  position: absolute;
  inset: 0;
  background: url('@/assets/login-bg.png') center center / cover no-repeat;
  opacity: 0.35;
  z-index: 0;
}

.cyber-login.is-admin {
  --accent: #ff4b5c;
  --accent-2: #f97316;
  --accent-glow: rgba(255, 75, 92, 0.4);
  --grid-color: rgba(255, 75, 92, 0.04);
  --border: rgba(255, 75, 92, 0.2);
  --title-grad: linear-gradient(to right, #ef4444, #fb923c);
  --btn-grad: linear-gradient(to right, #dc2626, #f97316);
  --btn-text: #fff;
  --glow-tl: rgba(220, 38, 38, 0.3);
  --glow-br: rgba(249, 115, 22, 0.2);
}

.cyber-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(var(--grid-color) 1px, transparent 1px),
    linear-gradient(90deg, var(--grid-color) 1px, transparent 1px);
  background-size: 40px 40px;
  animation: grid-drift 20s linear infinite;
  z-index: 0;
  transition: all 0.7s;
}

.glow {
  position: absolute;
  width: 384px;
  height: 384px;
  border-radius: 50%;
  filter: blur(120px);
  opacity: 0.3;
  transition: all 0.7s;
  z-index: 0;
  pointer-events: none;
}

.glow-tl { top: -160px; left: -160px; background: var(--glow-tl); }
.glow-br { bottom: -160px; right: -160px; background: var(--glow-br); opacity: 0.2; }

.scan-bar {
  position: absolute;
  left: 0;
  right: 0;
  height: 120px;
  background: linear-gradient(180deg, transparent, rgba(0, 242, 254, 0.4), transparent);
  animation: scan-line 6s infinite linear;
  z-index: 50;
  pointer-events: none;
}

.is-admin .scan-bar {
  background: linear-gradient(180deg, transparent, rgba(255, 75, 92, 0.35), transparent);
}

.login-shell {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 1152px;
  display: flex;
  flex-direction: column;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(24px);
  border: 1px solid var(--border);
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  transition: all 0.7s;
}

@media (min-width: 1024px) {
  .login-shell { flex-direction: row; }
}

.panel-left {
  flex: 7;
  padding: 32px 48px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border-bottom: 1px solid var(--border);
}

@media (min-width: 1024px) {
  .panel-left {
    border-bottom: none;
    border-right: 1px solid var(--border);
  }
}

.panel-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.status-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot-wrap {
  position: relative;
  width: 12px;
  height: 12px;
  flex-shrink: 0;
}

.status-ping {
  position: absolute;
  left: 0;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--accent);
  opacity: 0.75;
  animation: ping 1.5s cubic-bezier(0, 0, 0.2, 1) infinite;
}

.status-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--accent);
  position: relative;
}

.status-text {
  font-size: 12px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #94a3b8;
  font-family: ui-monospace, monospace;
}

.version-badge {
  font-size: 12px;
  font-family: ui-monospace, monospace;
  color: #94a3b8;
  background: rgba(30, 41, 59, 0.5);
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid rgba(51, 65, 85, 0.5);
}

.hero-block {
  margin: 40px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.battery-wrap {
  position: relative;
  width: 192px;
  height: 192px;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: float-slow 6s infinite ease-in-out;
}

.orbit {
  position: absolute;
  width: 100%;
  height: 100%;
}

.orbit circle { stroke: var(--accent); }
.orbit-ccw circle { stroke: var(--accent-2); }

.orbit-cw { animation: rotate-cw 20s linear infinite; }
.orbit-ccw { width: 83%; height: 83%; animation: rotate-ccw 15s linear infinite; }

.battery-core {
  width: 128px;
  height: 128px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(2, 6, 23, 0.8);
  border: 1px solid color-mix(in srgb, var(--accent) 30%, transparent);
  box-shadow: 0 0 15px var(--accent-glow), 0 0 30px color-mix(in srgb, var(--accent-glow) 50%, transparent);
  position: relative;
  overflow: hidden;
}

.battery-fill {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  border-radius: 8px 8px 0 0;
  background: linear-gradient(to top, color-mix(in srgb, var(--accent) 30%, transparent), color-mix(in srgb, var(--accent) 10%, transparent));
  transition: height 1s ease-in-out;
}

.bolt-icon {
  width: 64px;
  height: 64px;
  color: var(--accent);
  position: relative;
  z-index: 1;
  filter: drop-shadow(0 0 10px var(--accent-glow));
}

.charge-label {
  font-size: 12px;
  font-family: ui-monospace, monospace;
  font-weight: 700;
  color: color-mix(in srgb, var(--accent) 80%, white);
  margin-top: 4px;
  position: relative;
  z-index: 1;
}

.brand-title-wrap {
  margin-top: 28px;
  padding: 4px 0;
  overflow: visible;
  width: 100%;
  max-width: 420px;
  text-align: center;
}

.hero-slogan {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: center;
  gap: 0.25em 0.45em;
  margin: 0;
  line-height: 1.3;
  font-size: clamp(1.5rem, 3.2vw, 2.35rem);
  font-weight: 800;
  letter-spacing: 0.06em;
}

.slogan-a,
.slogan-b {
  display: inline-block;
  line-height: 1.35;
  padding: 0.06em 0;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: transparent;
}

.slogan-a {
  background-image: linear-gradient(100deg, #22d3ee 0%, #38bdf8 35%, #60a5fa 100%);
}

.slogan-b {
  background-image: linear-gradient(100deg, #3b82f6 0%, #8b5cf6 48%, #ec4899 100%);
}

.is-admin .slogan-a {
  background-image: linear-gradient(100deg, #fdba74 0%, #fb923c 40%, #ef4444 100%);
}

.is-admin .slogan-b {
  background-image: linear-gradient(100deg, #ef4444 0%, #f43f5e 50%, #fb7185 100%);
}

.hero-tagline {
  margin: 14px 0 0;
  color: #9ca3af;
  font-size: clamp(12px, 1.6vw, 14px);
  letter-spacing: 0.14em;
  font-weight: 400;
  line-height: 1.6;
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 18px;
}

.brand-mark-line {
  flex: 1;
  max-width: 48px;
  height: 1px;
  background: linear-gradient(90deg, transparent, color-mix(in srgb, var(--accent) 45%, transparent));
}

.brand-mark-line:last-child {
  background: linear-gradient(90deg, color-mix(in srgb, var(--accent) 45%, transparent), transparent);
}

.brand-product {
  font-size: clamp(1.125rem, 2vw, 1.375rem);
  font-weight: 800;
  letter-spacing: 0.2em;
  color: color-mix(in srgb, var(--accent) 90%, white);
  white-space: nowrap;
  text-shadow: 0 0 24px color-mix(in srgb, var(--accent) 35%, transparent);
}

.feature-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

@media (min-width: 768px) {
  .feature-grid { grid-template-columns: repeat(3, 1fr); }
}

.feature-card {
  padding: 16px;
  border-radius: 12px;
  background: rgba(2, 6, 23, 0.6);
  border: 1px solid rgba(30, 41, 59, 0.8);
  transition: border-color 0.3s;
  cursor: default;
}

.feature-card:hover { border-color: #334155; }
.feature-card:hover .feature-icon { color: var(--accent); background: color-mix(in srgb, var(--accent) 10%, transparent); }
.feature-card:hover .feature-name { color: color-mix(in srgb, var(--accent) 80%, white); }

.feature-card {
  display: flex;
  align-items: center;
  gap: 12px;
}

.feature-icon {
  padding: 8px;
  border-radius: 8px;
  background: #0f172a;
  color: #94a3b8;
  transition: all 0.3s;
  flex-shrink: 0;
}

.feature-name {
  font-size: 12px;
  font-weight: 700;
  color: #e2e8f0;
  transition: color 0.3s;
  white-space: nowrap;
}

.feature-desc {
  font-size: 10px;
  color: #64748b;
  margin-top: 2px;
}

.panel-right {
  flex: 5;
  padding: 32px 48px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background: rgba(2, 6, 23, 0.6);
}

.role-switch-wrap {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 32px;
}

.role-switch {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border-radius: 999px;
  font-size: 12px;
  font-family: ui-monospace, monospace;
  cursor: pointer;
  transition: all 0.3s;
  background: color-mix(in srgb, var(--accent) 10%, transparent);
  border: 1px solid color-mix(in srgb, var(--accent) 30%, transparent);
  color: var(--accent);
}

.role-switch:hover {
  background: color-mix(in srgb, var(--accent) 20%, transparent);
}

.switch-icon { width: 14px; height: 14px; }

.form-block { margin: auto 0; }

.form-header { margin-bottom: 24px; }

.form-header h2 {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--accent);
}

.form-header p {
  font-size: 12px;
  color: #64748b;
  margin-top: 8px;
}

.login-form { display: flex; flex-direction: column; gap: 16px; }

.field label {
  display: block;
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-family: ui-monospace, monospace;
  color: #64748b;
  margin-bottom: 4px;
}

.field-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.field-label-row label { margin-bottom: 0; }

.forgot-link {
  font-size: 10px;
  color: #64748b;
  text-decoration: none;
  transition: color 0.2s;
}

.forgot-link:hover { color: var(--accent); }

.input-wrap {
  position: relative;
}

.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  width: 16px;
  height: 16px;
  color: #64748b;
  pointer-events: none;
}

.input-wrap input {
  width: 100%;
  background: #0f172a;
  border: 1px solid #1e293b;
  border-radius: 12px;
  padding: 12px 40px 12px 40px;
  font-size: 14px;
  color: #f1f5f9;
  outline: none;
  transition: all 0.3s;
}

.input-wrap input::placeholder { color: #475569; }

.input-wrap input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--accent) 50%, transparent);
}

.pwd-toggle {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  padding: 0 12px;
  background: none;
  border: none;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.pwd-toggle:hover { color: #cbd5e1; }
.pwd-toggle svg { width: 16px; height: 16px; }

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  padding: 4px 0;
}

.remember-check {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #94a3b8;
  cursor: pointer;
}

.remember-check input {
  width: 14px;
  height: 14px;
  accent-color: var(--accent);
  cursor: pointer;
}

.secure-link {
  color: #64748b;
  font-family: ui-monospace, monospace;
}

.submit-btn {
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.05em;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: var(--btn-grad);
  color: var(--btn-text);
  transition: all 0.3s;
}

.submit-btn:hover:not(:disabled) {
  box-shadow: 0 0 15px var(--accent-glow), 0 0 30px color-mix(in srgb, var(--accent-glow) 50%, transparent);
  filter: brightness(1.08);
}

.submit-btn:active:not(:disabled) { transform: scale(0.98); }
.submit-btn:disabled { opacity: 0.7; cursor: not-allowed; }

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.panel-footer {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid rgba(30, 41, 59, 0.6);
}

.demo-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  background: rgba(15, 23, 42, 0.8);
  border-radius: 12px;
  border: 1px solid rgba(30, 41, 59, 0.8);
  margin-bottom: 12px;
}

.demo-label {
  font-size: 10px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-family: ui-monospace, monospace;
}

.demo-cred {
  font-size: 13px;
  color: #cbd5e1;
  font-family: ui-monospace, monospace;
  margin-top: 2px;
}

.demo-fill {
  padding: 4px 10px;
  border-radius: 6px;
  background: #1e293b;
  border: 1px solid rgba(51, 65, 85, 0.5);
  color: #cbd5e1;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s;
  flex-shrink: 0;
}

.demo-fill:hover { background: #334155; }

.footer-links {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #64748b;
  gap: 12px;
  flex-wrap: wrap;
}

.footer-placeholder { flex: 1; }

.link-accent {
  color: var(--accent);
  text-decoration: none;
}

.link-accent:hover { text-decoration: underline; }

.footer-service {
  color: #64748b;
  text-decoration: none;
  transition: color 0.2s;
}

.footer-service:hover { color: #cbd5e1; }

@keyframes grid-drift {
  from { background-position: 0 0; }
  to { background-position: 40px 40px; }
}

@keyframes float-slow {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-10px) rotate(2deg); }
}

@keyframes rotate-cw {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes rotate-ccw {
  from { transform: rotate(360deg); }
  to { transform: rotate(0deg); }
}

@keyframes scan-line {
  0% { top: 0; opacity: 0; }
  5% { opacity: 1; }
  95% { opacity: 1; }
  100% { top: 100%; opacity: 0; }
}

@keyframes ping {
  75%, 100% { transform: scale(2); opacity: 0; }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 900px) {
  .cyber-login { padding: 12px; }
  .panel-left, .panel-right { padding: 24px; }
  .feature-grid { grid-template-columns: 1fr; }
}
</style>
