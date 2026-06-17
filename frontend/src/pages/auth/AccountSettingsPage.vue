<template>
  <div class="sl-page account-page">
    <div class="sl-page-header">
      <h2 class="sl-page-title">账号设置</h2>
    </div>

    <div class="account-grid">
      <el-card class="account-card">
        <template #header>
          <span class="card-title">基本信息</span>
        </template>
        <div class="profile-head">
          <el-avatar :size="72" :src="profileForm.avatarUrl || undefined" class="profile-avatar">
            {{ displayInitial }}
          </el-avatar>
          <div class="profile-meta">
            <h3>{{ userStore.username }}</h3>
            <p>{{ roleLabel }}</p>
            <p v-if="userStore.userInfo?.lastLoginAt" class="meta-sub">
              上次登录：{{ formatTime(userStore.userInfo.lastLoginAt) }}
            </p>
          </div>
        </div>

        <el-form ref="profileRef" :model="profileForm" :rules="profileRules" label-width="88px" class="profile-form">
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="profileForm.phone" placeholder="请输入手机号" maxlength="20" />
          </el-form-item>
          <el-form-item label="头像地址">
            <el-input v-model="profileForm.avatarUrl" placeholder="图片 URL（可选）" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="account-card">
        <template #header>
          <span class="card-title">修改密码</span>
        </template>
        <el-form ref="pwdRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
          <el-form-item label="当前密码" prop="oldPassword">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-64 位" />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingPwd" @click="savePassword">更新密码</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores'
import { authApi } from '@/api/auth'

const userStore = useUserStore()
const profileRef = ref<FormInstance>()
const pwdRef = ref<FormInstance>()
const savingProfile = ref(false)
const savingPwd = ref(false)

const profileForm = reactive({
  email: '',
  phone: '',
  avatarUrl: '',
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const displayInitial = computed(() => (userStore.username || '用').charAt(0).toUpperCase())
const roleLabel = computed(() => userStore.role === 'admin' ? '系统管理员' : '培训学员')

const profileRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  phone: [{ pattern: /^$|^1\d{10}$/, message: '请输入有效手机号', trigger: 'blur' }],
}

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度 6-64 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_r, v, cb) => {
        if (v !== pwdForm.newPassword) cb(new Error('两次输入的密码不一致'))
        else cb()
      },
      trigger: 'blur',
    },
  ],
}

function formatTime(iso: string) {
  try {
    return new Date(iso).toLocaleString('zh-CN')
  } catch {
    return iso
  }
}

function loadForm() {
  const u = userStore.userInfo
  if (!u) return
  profileForm.email = u.email || ''
  profileForm.phone = u.phone || ''
  profileForm.avatarUrl = u.avatarUrl || u.avatar || ''
}

async function saveProfile() {
  const valid = await profileRef.value?.validate().catch(() => false)
  if (!valid) return
  savingProfile.value = true
  try {
    await authApi.updateProfile({ ...profileForm })
    await userStore.getUserInfo()
    ElMessage.success('资料已更新')
  } finally {
    savingProfile.value = false
  }
}

async function savePassword() {
  const valid = await pwdRef.value?.validate().catch(() => false)
  if (!valid) return
  savingPwd.value = true
  try {
    await authApi.changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    ElMessage.success('密码修改成功')
  } finally {
    savingPwd.value = false
  }
}

onMounted(async () => {
  if (!userStore.userInfo) await userStore.getUserInfo()
  loadForm()
})
</script>

<style scoped>
.account-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 16px;
}

.card-title {
  font-weight: 600;
  color: #1e293b;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.profile-avatar {
  background: linear-gradient(135deg, #3b82f6, #10b981);
  color: #fff;
  font-size: 28px;
  font-weight: 700;
}

.profile-meta h3 {
  margin: 0 0 4px;
  font-size: 18px;
  color: #0f172a;
}

.profile-meta p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.meta-sub {
  margin-top: 4px !important;
  font-size: 12px !important;
  color: #94a3b8 !important;
}

.profile-form {
  margin-top: 8px;
}

@media (max-width: 900px) {
  .account-grid {
    grid-template-columns: 1fr;
  }
}
</style>
