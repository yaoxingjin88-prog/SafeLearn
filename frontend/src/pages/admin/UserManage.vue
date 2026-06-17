<template>
  <div class="sl-page user-manage">
    <div class="sl-page-header">
      <h2 class="sl-page-title">用户管理</h2>
      <el-button type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon>
        添加用户
      </el-button>
    </div>

    <el-card>
      <el-table :data="users" style="width: 100%">
        <el-table-column label="用户" width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.avatarUrl || undefined">{{ (row.username || '?').charAt(0) }}</el-avatar>
              <span>{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role)">{{ getRoleName(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="部门" width="130" />
        <el-table-column label="上次登录" width="170">
          <template #default="{ row }">{{ formatTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="editUser(row)">编辑</el-button>
            <el-button type="danger" link @click="deleteUser(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '添加用户'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="20" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="默认 123456" />
        </el-form-item>
        <el-form-item v-else label="重置密码">
          <el-input v-model="form.password" type="password" show-password placeholder="留空则不修改" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色">
            <el-option label="管理员" value="admin" />
            <el-option label="培训人员" value="trainee" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属单位">
          <el-input v-model="form.company" placeholder="请输入所属单位" />
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="form.department" placeholder="请输入部门" />
        </el-form-item>
        <el-form-item label="头像地址">
          <el-input v-model="form.avatarUrl" placeholder="图片 URL（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/api/request'

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const users = ref<any[]>([])

const form = reactive({
  id: '',
  username: '',
  email: '',
  phone: '',
  password: '',
  role: '',
  company: '',
  department: '',
  avatarUrl: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

function formatTime(v?: string) {
  if (!v) return '—'
  try {
    return new Date(v).toLocaleString('zh-CN')
  } catch {
    return v
  }
}

async function loadUsers() {
  const res = await request.get('/admin/users')
  users.value = res.data.items || res.data
}

onMounted(loadUsers)

function getRoleType(role: string) {
  return role === 'admin' ? 'danger' : ''
}

function getRoleName(role: string) {
  return role === 'admin' ? '管理员' : '培训人员'
}

function showAddDialog() {
  isEdit.value = false
  Object.assign(form, { id: '', username: '', email: '', phone: '', password: '', role: '', company: '', department: '', avatarUrl: '' })
  dialogVisible.value = true
}

function editUser(user: any) {
  isEdit.value = true
  Object.assign(form, { ...user, password: '' })
  dialogVisible.value = true
}

async function deleteUser(user: any) {
  await ElMessageBox.confirm('确定要删除该用户吗？', '提示', { type: 'warning' })
  await request.delete(`/admin/users/${user.id}`)
  await loadUsers()
  ElMessage.success('删除成功')
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload: Record<string, string> = {
    username: form.username,
    email: form.email,
    role: form.role,
    company: form.company,
    department: form.department,
    phone: form.phone,
    avatarUrl: form.avatarUrl,
  }
  if (form.password) payload.password = form.password

  if (isEdit.value) {
    await request.put(`/admin/users/${form.id}`, payload)
  } else {
    await request.post('/admin/users', { ...payload, password: form.password || '123456' })
  }

  await loadUsers()
  dialogVisible.value = false
  ElMessage.success(isEdit.value ? '编辑成功' : '添加成功')
}
</script>

<style scoped>
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
