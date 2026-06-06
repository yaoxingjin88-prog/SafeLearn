<template>
  <div class="user-login-page">
    <div class="login-left">
      <div class="brand-block">
        <img src="@/assets/logo.svg" class="logo" alt="Logo" />
        <h1>储安云</h1>
        <p>储能电站安全培训与事故推演系统</p>
        <ul class="features">
          <li>安全培训课程 · 阶梯式学习路径</li>
          <li>热失控事故 3D 推演演示</li>
          <li>应急决策训练 · AI 安全问答</li>
        </ul>
      </div>
    </div>

    <div class="login-right">
      <div class="login-card">
        <h2>学员登录</h2>
        <p class="subtitle">欢迎回来，请登录您的学习账号</p>

        <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              size="large"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" :loading="loading" class="w-full" @click="handleLogin">
              进入学习平台
            </el-button>
          </el-form-item>
        </el-form>

        <div class="demo-tip">
          <p>演示账号：<code>zhanggong</code> / <code>admin123</code></p>
        </div>

        <div class="login-footer">
          <span>还没有账号？</span>
          <el-link type="primary" @click="$router.push('/register')">立即注册</el-link>
        </div>
        <div class="admin-link">
          <el-link type="info" @click="$router.push('/login')">管理员入口 →</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/user/dashboard')
  } catch {
    ElMessage.error('用户名或密码错误')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.user-login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.login-left {
  background: linear-gradient(135deg, #2b5aed 0%, #1e40af 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.brand-block {
  max-width: 420px;
}

.logo {
  height: 48px;
  margin-bottom: 24px;
  filter: brightness(0) invert(1);
}

.brand-block h1 {
  font-size: 36px;
  font-weight: 800;
  margin-bottom: 12px;
}

.brand-block > p {
  font-size: 16px;
  opacity: 0.9;
  margin-bottom: 32px;
}

.features {
  list-style: none;
  padding: 0;
  margin: 0;
}

.features li {
  padding: 10px 0;
  font-size: 15px;
  opacity: 0.95;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
}

.login-right {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.08);
}

.login-card h2 {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 8px;
}

.subtitle {
  color: #6b7280;
  font-size: 14px;
  margin-bottom: 28px;
}

.login-form :deep(.el-button--primary) {
  background: #2b5aed;
  border-color: #2b5aed;
}

.demo-tip {
  margin-top: 16px;
  padding: 12px;
  background: #f0f5ff;
  border-radius: 8px;
  font-size: 13px;
  color: #4b5563;
}

.demo-tip code {
  background: #e0e7ff;
  padding: 2px 6px;
  border-radius: 4px;
  color: #2b5aed;
}

.login-footer {
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
  color: #6b7280;
}

.admin-link {
  margin-top: 12px;
  text-align: center;
}

@media (max-width: 900px) {
  .user-login-page {
    grid-template-columns: 1fr;
  }
  .login-left {
    display: none;
  }
}
</style>
