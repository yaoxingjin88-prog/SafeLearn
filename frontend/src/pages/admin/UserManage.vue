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
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role)">{{ getRoleName(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="company" label="所属单位" width="150" />
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="editUser(row)">编辑</el-button>
            <el-button type="danger" link @click="deleteUser(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex justify-end mt-4">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="100"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '添加用户'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
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

const currentPage = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const users = ref<any[]>([])

async function loadUsers() {
  const res = await request.get('/admin/users')
  users.value = res.data.items || res.data
}

onMounted(loadUsers)

const form = reactive({
  id: '',
  username: '',
  email: '',
  role: '',
  company: '',
  department: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

function getRoleType(role: string) {
  const map: Record<string, string> = { admin: 'danger', trainee: '' }
  return map[role] || ''
}

function getRoleName(role: string) {
  const map: Record<string, string> = { admin: '管理员', trainee: '培训人员' }
  return map[role] || role
}

function showAddDialog() {
  isEdit.value = false
  form.id = ''
  form.username = ''
  form.email = ''
  form.role = ''
  form.company = ''
  form.department = ''
  dialogVisible.value = true
}

function editUser(user: any) {
  isEdit.value = true
  Object.assign(form, user)
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

  if (isEdit.value) {
    await request.put(`/admin/users/${form.id}`, form)
  } else {
    await request.post('/admin/users', form)
  }

  await loadUsers()
  dialogVisible.value = false
  ElMessage.success(isEdit.value ? '编辑成功' : '添加成功')
}
</script>

<style scoped>
</style>
