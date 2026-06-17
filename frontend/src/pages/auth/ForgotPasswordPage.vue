<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2 class="auth-title">找回密码</h2>
      <p class="auth-desc">输入注册邮箱，我们将发送密码重置链接（有效期 30 分钟）</p>

      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleSubmit">
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="请输入注册邮箱" prefix-icon="Message" size="large" />
        </el-form-item>
        <el-button type="primary" size="large" class="submit-btn" :loading="loading" native-type="submit">
          发送重置链接
        </el-button>
      </el-form>

      <p v-if="devResetUrl" class="dev-hint">
        开发环境重置链接：
        <router-link :to="devResetPath">{{ devResetUrl }}</router-link>
      </p>

      <router-link to="/login" class="back-link">返回登录</router-link>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { authApi } from '@/api/auth'

const formRef = ref<FormInstance>()
const loading = ref(false)
const devResetUrl = ref('')

const form = reactive({ email: '' })

const rules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
}

const devResetPath = computed(() => {
  if (!devResetUrl.value) return '/reset-password'
  try {
    const url = new URL(devResetUrl.value)
    return url.pathname + url.search
  } catch {
    return devResetUrl.value
  }
})

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  devResetUrl.value = ''
  try {
    const res = await authApi.forgotPassword(form.email.trim())
    ElMessage.success(res.data.message)
    if (res.data.devResetUrl) devResetUrl.value = res.data.devResetUrl
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fc;
  padding: 24px;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  border-radius: 16px;
}

.auth-title {
  margin: 0 0 8px;
  font-size: 22px;
  color: #0f172a;
}

.auth-desc {
  margin: 0 0 24px;
  font-size: 14px;
  color: #64748b;
  line-height: 1.5;
}

.submit-btn {
  width: 100%;
}

.dev-hint {
  margin-top: 16px;
  padding: 12px;
  font-size: 12px;
  color: #64748b;
  background: #f8fafc;
  border-radius: 8px;
  word-break: break-all;
}

.back-link {
  display: inline-block;
  margin-top: 20px;
  font-size: 14px;
  color: #3b82f6;
  text-decoration: none;
}
</style>
