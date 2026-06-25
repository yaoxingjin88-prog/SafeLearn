<template>
  <div class="user-manage-page" v-loading="loading">
    <div class="page-toolbar">
      <div class="filter-row">
        <div class="filter-field">
          <span class="filter-label">姓名/工号</span>
          <el-input v-model="filters.keyword" placeholder="请输入姓名或工号" clearable class="filter-input" @keyup.enter="search" />
        </div>
        <div class="filter-field">
          <span class="filter-label">所属部门</span>
          <el-select v-model="filters.department" class="filter-item">
            <el-option label="全部部门" value="all" />
            <el-option v-for="dept in departmentOptions" :key="dept" :label="dept" :value="dept" />
          </el-select>
        </div>
        <div class="filter-field">
          <span class="filter-label">角色</span>
          <el-select v-model="filters.role" class="filter-item">
            <el-option v-for="item in USER_ROLE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div class="filter-field">
          <span class="filter-label">状态</span>
          <el-select v-model="filters.status" class="filter-item">
            <el-option v-for="item in USER_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
      </div>

      <div v-show="filtersExpanded" class="filter-row filter-row-extra">
        <div class="filter-field">
          <span class="filter-label">持证情况</span>
          <el-select v-model="filters.certStatus" class="filter-item">
            <el-option v-for="item in USER_CERT_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div class="filter-field filter-field-range">
          <span class="filter-label">培训完成率</span>
          <div class="range-inputs">
            <el-input-number v-model="filters.progressMin" :min="0" :max="100" :controls="false" placeholder="最小" class="range-num" />
            <span class="range-sep">~</span>
            <el-input-number v-model="filters.progressMax" :min="0" :max="100" :controls="false" placeholder="最大" class="range-num" />
            <span class="range-unit">%</span>
          </div>
        </div>
      </div>

      <div class="filter-row filter-row-bottom">
        <div class="filter-actions">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="search">查询</el-button>
          <button type="button" class="expand-link" @click="filtersExpanded = !filtersExpanded">
            {{ filtersExpanded ? '收起' : '展开' }}
            <el-icon :class="{ rotated: filtersExpanded }"><ArrowDown /></el-icon>
          </button>
        </div>
      </div>
    </div>

    <div class="action-toolbar">
      <div class="action-left">
        <el-button type="primary" @click="showAddDialog">
          <el-icon><Plus /></el-icon>
          新增用户
        </el-button>
        <el-button @click="handleImport">
          <el-icon><Upload /></el-icon>
          导入用户
        </el-button>
        <el-dropdown trigger="click" :disabled="!selectedIds.length" @command="handleBatch">
          <el-button :disabled="!selectedIds.length">
            批量操作
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="enable">批量启用</el-dropdown-item>
              <el-dropdown-item command="disable">批量停用</el-dropdown-item>
              <el-dropdown-item command="delete" divided>批量删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <div class="action-right">
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table
        :data="users"
        style="width: 100%"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column label="头像" width="72" align="center">
          <template #default="{ row }">
            <el-avatar :size="36" :src="row.avatarUrl || undefined">{{ (row.username || '?').charAt(0) }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="姓名" min-width="100">
          <template #default="{ row }">
            <button type="button" class="name-link" @click="goDetail(row)">{{ row.username }}</button>
          </template>
        </el-table-column>
        <el-table-column prop="employeeNo" label="工号" width="110" />
        <el-table-column prop="department" label="部门" width="110">
          <template #default="{ row }">{{ row.department || '—' }}</template>
        </el-table-column>
        <el-table-column prop="position" label="岗位" width="120">
          <template #default="{ row }">{{ row.position || '—' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="120">
          <template #default="{ row }">{{ roleLabel(row.role) }}</template>
        </el-table-column>
        <el-table-column label="联系方式" width="130">
          <template #default="{ row }">{{ row.phoneMasked || '—' }}</template>
        </el-table-column>
        <el-table-column label="培训完成率" min-width="160">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress :percentage="row.trainingCompletionRate ?? 0" :stroke-width="8" :show-text="false" />
              <span class="progress-text">{{ row.trainingCompletionRate ?? 0 }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="160">
          <template #default="{ row }">{{ formatLoginTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="账号状态" width="100" align="center">
          <template #default="{ row }">
            <span class="status-pill" :class="row.enabled ? 'status-active' : 'status-disabled'">
              {{ row.enabled ? '正常' : '停用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-links">
              <button type="button" class="link-btn" @click="editUser(row)">编辑</button>
              <button type="button" class="link-btn" @click="toggleStatus(row)">
                {{ row.enabled ? '停用' : '启用' }}
              </button>
              <el-dropdown trigger="click" @command="(cmd: string) => handleMore(cmd, row)">
                <button type="button" class="link-btn">更多</button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="detail">查看详情</el-dropdown-item>
                    <el-dropdown-item command="resetPwd">重置密码</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>删除用户</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !users.length" description="暂无用户数据" />
    </div>

    <div class="pagination-bar">
      <span>共 {{ total }} 条</span>
      <el-pagination
        v-model:current-page="filters.page"
        v-model:page-size="filters.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="sizes, prev, pager, next, jumper"
        @current-change="loadUsers"
        @size-change="onPageSizeChange"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="姓名" prop="username">
          <el-input v-model="form.username" placeholder="请输入姓名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="工号">
          <el-input v-model="form.employeeNo" placeholder="留空则自动生成" />
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
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%" @change="onRoleChange">
            <el-option label="普通学员" value="trainee" />
            <el-option label="安全管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.role === 'admin'" label="权限角色">
          <el-select v-model="form.permissionRoleId" clearable placeholder="默认系统管理员（全部权限）" style="width: 100%">
            <el-option
              v-for="role in permissionRoleOptions"
              :key="role.id"
              :label="`${role.name} (${role.code})`"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属部门">
          <el-select v-model="form.department" filterable allow-create placeholder="请选择或输入部门" style="width: 100%">
            <el-option v-for="dept in departmentOptions" :key="dept" :label="dept" :value="dept" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位">
          <el-input v-model="form.position" placeholder="请输入岗位" />
        </el-form-item>
        <el-form-item label="所属单位">
          <el-input v-model="form.company" placeholder="请输入所属单位" />
        </el-form-item>
        <el-form-item label="头像地址">
          <el-input v-model="form.avatarUrl" placeholder="图片 URL（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Upload, Download, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { adminApi, type AdminRoleSummary, type AdminUserListItem, type AdminUserPayload } from '@/api/admin'
import {
  USER_ROLE_OPTIONS,
  USER_STATUS_OPTIONS,
  USER_CERT_OPTIONS,
  DEFAULT_DEPARTMENTS,
  roleLabel,
  formatLoginTime,
} from './userManageShared'

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const filtersExpanded = ref(true)
const formRef = ref<FormInstance>()
const users = ref<AdminUserListItem[]>([])
const total = ref(0)
const selectedIds = ref<string[]>([])
const departmentOptions = ref<string[]>([...DEFAULT_DEPARTMENTS])
const permissionRoleOptions = ref<AdminRoleSummary[]>([])

const filters = reactive({
  keyword: '',
  department: 'all',
  role: 'all',
  status: 'all',
  certStatus: 'all',
  progressMin: undefined as number | undefined,
  progressMax: undefined as number | undefined,
  page: 1,
  pageSize: 10,
})

const form = reactive({
  id: '',
  username: '',
  employeeNo: '',
  email: '',
  phone: '',
  password: '',
  role: 'trainee',
  permissionRoleId: '' as string | null,
  company: '',
  department: '',
  position: '',
  avatarUrl: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

async function loadFilterOptions() {
  try {
    const [optionsRes, rolesRes] = await Promise.all([
      adminApi.getUserFilterOptions(),
      adminApi.listRoles(),
    ])
    const depts = optionsRes.data?.departments || []
    departmentOptions.value = [...new Set([...DEFAULT_DEPARTMENTS, ...depts])]
    permissionRoleOptions.value = rolesRes.data || []
  } catch {
    departmentOptions.value = [...DEFAULT_DEPARTMENTS]
    permissionRoleOptions.value = []
  }
}

function onRoleChange(role: string) {
  if (role !== 'admin') {
    form.permissionRoleId = null
  }
}

async function loadUsers() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: filters.page,
      pageSize: filters.pageSize,
    }
    if (filters.keyword.trim()) params.keyword = filters.keyword.trim()
    if (filters.department !== 'all') params.department = filters.department
    if (filters.role !== 'all') params.role = filters.role
    if (filters.status !== 'all') params.status = filters.status
    if (filters.certStatus !== 'all') params.certStatus = filters.certStatus
    if (filters.progressMin != null) params.progressMin = filters.progressMin
    if (filters.progressMax != null) params.progressMax = filters.progressMax

    const res = await adminApi.getUsers(params)
    users.value = res.data?.items || []
    total.value = res.data?.total ?? 0
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadFilterOptions()
  await loadUsers()
})

function search() {
  filters.page = 1
  loadUsers()
}

function resetFilters() {
  Object.assign(filters, {
    keyword: '',
    department: 'all',
    role: 'all',
    status: 'all',
    certStatus: 'all',
    progressMin: undefined,
    progressMax: undefined,
    page: 1,
  })
  loadUsers()
}

function onPageSizeChange() {
  filters.page = 1
  loadUsers()
}

function onSelectionChange(rows: AdminUserListItem[]) {
  selectedIds.value = rows.map(r => r.id)
}

function showAddDialog() {
  isEdit.value = false
  Object.assign(form, {
    id: '', username: '', employeeNo: '', email: '', phone: '', password: '',
    role: 'trainee', permissionRoleId: null, company: '', department: '', position: '', avatarUrl: '',
  })
  dialogVisible.value = true
}

function editUser(user: AdminUserListItem) {
  isEdit.value = true
  Object.assign(form, { ...user, password: '' })
  dialogVisible.value = true
}

async function toggleStatus(user: AdminUserListItem) {
  const next = !user.enabled
  const action = next ? '启用' : '停用'
  await ElMessageBox.confirm(`确定要${action}用户「${user.username}」吗？`, '提示', { type: 'warning' })
  await adminApi.updateUserStatus(user.id, next)
  ElMessage.success(`${action}成功`)
  await loadUsers()
}

async function handleMore(cmd: string, user: AdminUserListItem) {
  if (cmd === 'detail') {
    goDetail(user)
    return
  }
  if (cmd === 'delete') {
    await ElMessageBox.confirm(`确定要删除用户「${user.username}」吗？`, '提示', { type: 'warning' })
    await adminApi.deleteUser(user.id)
    ElMessage.success('删除成功')
    await loadUsers()
    return
  }
  if (cmd === 'resetPwd') {
    await ElMessageBox.confirm(`确定将「${user.username}」的密码重置为 123456 吗？`, '重置密码', { type: 'warning' })
    await adminApi.updateUser(user.id, { password: '123456' })
    ElMessage.success('密码已重置')
  }
}

function goDetail(user: AdminUserListItem) {
  router.push(`/admin/users/${user.id}`)
}

async function handleBatch(action: 'enable' | 'disable' | 'delete') {
  const labels = { enable: '启用', disable: '停用', delete: '删除' }
  await ElMessageBox.confirm(`确定要批量${labels[action]}选中的 ${selectedIds.value.length} 个用户吗？`, '批量操作', { type: 'warning' })
  const res = await adminApi.batchOperateUsers({ ids: selectedIds.value, action })
  ElMessage.success(`已${labels[action]} ${res.data?.affected ?? 0} 个用户`)
  selectedIds.value = []
  await loadUsers()
}

function handleImport() {
  ElMessage.info('导入功能开发中，敬请期待')
}

function handleExport() {
  if (!users.value.length) {
    ElMessage.warning('当前没有可导出的数据')
    return
  }
  const headers = ['姓名', '工号', '部门', '岗位', '角色', '联系方式', '培训完成率', '最近登录', '账号状态']
  const rows = users.value.map(u => [
    u.username,
    u.employeeNo || '',
    u.department || '',
    u.position || '',
    roleLabel(u.role),
    u.phoneMasked || '',
    `${u.trainingCompletionRate ?? 0}%`,
    formatLoginTime(u.lastLoginAt),
    u.enabled ? '正常' : '停用',
  ])
  const csv = [headers, ...rows].map(r => r.map(c => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `用户列表_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload: AdminUserPayload = {
      username: form.username,
      email: form.email,
      role: form.role,
      company: form.company,
      department: form.department,
      position: form.position,
      employeeNo: form.employeeNo,
      phone: form.phone,
      avatarUrl: form.avatarUrl,
    }
    if (form.role === 'admin') {
      payload.permissionRoleId = form.permissionRoleId || null
    }
    if (form.password) payload.password = form.password

    if (isEdit.value) {
      await adminApi.updateUser(form.id, payload)
    } else {
      await adminApi.createUser({ ...payload, password: form.password || '123456' })
    }

    await loadUsers()
    await loadFilterOptions()
    dialogVisible.value = false
    ElMessage.success(isEdit.value ? '编辑成功' : '添加成功')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.user-manage-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
}

.page-toolbar {
  background: #fff;
  border-radius: 8px;
  padding: 20px 20px 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
  align-items: center;
  margin-bottom: 12px;
}

.filter-row-bottom {
  justify-content: flex-end;
  margin-bottom: 0;
}

.filter-field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  flex-shrink: 0;
  font-size: 14px;
  color: #606266;
  min-width: 72px;
  text-align: right;
}

.filter-input,
.filter-item {
  width: 180px;
}

.filter-field-range .range-inputs {
  display: flex;
  align-items: center;
  gap: 6px;
}

.range-num {
  width: 72px;
}

.range-sep {
  color: #909399;
}

.range-unit {
  color: #909399;
  font-size: 13px;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.expand-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: none;
  color: #409eff;
  cursor: pointer;
  font-size: 14px;
  padding: 0 8px;
}

.expand-link .el-icon {
  transition: transform 0.2s;
}

.expand-link .el-icon.rotated {
  transform: rotate(180deg);
}

.action-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-left,
.action-right {
  display: flex;
  gap: 8px;
}

.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 0 0 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-cell :deep(.el-progress) {
  flex: 1;
}

.progress-text {
  flex-shrink: 0;
  font-size: 13px;
  color: #606266;
  min-width: 36px;
}

.status-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 20px;
}

.status-active {
  background: #f0f9eb;
  color: #67c23a;
}

.status-disabled {
  background: #fef0f0;
  color: #f56c6c;
}

.action-links {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: nowrap;
}

.link-btn {
  border: none;
  background: none;
  color: #409eff;
  cursor: pointer;
  font-size: 14px;
  padding: 0 4px;
}

.link-btn:hover {
  color: #66b1ff;
}

.name-link {
  border: none;
  background: none;
  color: #409eff;
  cursor: pointer;
  font-size: 14px;
  padding: 0;
}

.name-link:hover {
  color: #66b1ff;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 4px;
  color: #606266;
  font-size: 14px;
}
</style>
