<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2 class="auth-title">重置密码</h2>
      <p class="auth-desc">请设置您的新密码</p>

      <el-alert v-if="!token" type="error" title="重置链接无效" description="请从邮件中的链接进入，或重新申请找回密码" show-icon :closable="false" class="mb-4" />

      <el-form v-else ref="formRef" :model="form" :rules="rules" @submit.prevent="handleSubmit">
        <el-form-item prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password placeholder="新密码（6-64 位）" size="large" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="确认新密码" size="large" />
        </el-form-item>
        <el-button type="primary" size="large" class="submit-btn" :loading="loading" native-type="submit">
          确认重置
        </el-button>
      </el-form>

      <router-link to="/login" class="back-link">返回登录</router-link>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { authApi } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const token = computed(() => (route.query.token as string) || '')

const form = reactive({
  newPassword: '',
  confirmPassword: '',
})

const rules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度 6-64 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_r, v, cb) => {
        if (v !== form.newPassword) cb(new Error('两次输入的密码不一致'))
        else cb()
      },
      trigger: 'blur',
    },
  ],
}

async function handleSubmit() {
  if (!token.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await authApi.resetPassword(token.value, form.newPassword)
    ElMessage.success(res.data.message)
    router.push('/login')
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
}

.submit-btn {
  width: 100%;
}

.mb-4 {
  margin-bottom: 16px;
}

.back-link {
  display: inline-block;
  margin-top: 20px;
  font-size: 14px;
  color: #3b82f6;
  text-decoration: none;
}
</style>
