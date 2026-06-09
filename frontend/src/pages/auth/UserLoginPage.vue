<template>
  <CyberLoginShell
    :loading="loading"
    @submit="handleLogin"
    @toggle-portal="router.push('/login')"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores'
import CyberLoginShell from '@/components/auth/CyberLoginShell.vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

async function handleLogin(payload: { username: string; password: string; remember: boolean }) {
  loading.value = true
  try {
    await userStore.login(payload.username, payload.password)
    ElMessage.success('登录成功')
    router.push('/user/dashboard')
  } catch {
    ElMessage.error('用户名或密码错误')
  } finally {
    loading.value = false
  }
}
</script>
