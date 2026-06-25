<template>
  <div class="role-permission-page" v-loading="loading">
    <header class="page-toolbar">
      <div class="toolbar-left">
        <h1>角色与权限</h1>
        <p>配置各角色的功能权限、数据范围与审批权限</p>
      </div>
      <div class="toolbar-right">
        <span class="toolbar-label">切换角色</span>
        <el-select v-model="selectedRoleId" class="role-select" @change="onRoleChange">
          <el-option v-for="role in roles" :key="role.id" :label="role.name" :value="role.id">
            <span>{{ role.name }}</span>
            <em>{{ role.code }}</em>
          </el-option>
        </el-select>
        <el-button type="primary" plain @click="openCreateDialog">+ 新建角色</el-button>
      </div>
    </header>

    <section v-if="role" class="panel">
      <div class="panel-top">
        <div class="current-role">
          当前角色：<strong>{{ role.name }}</strong>
          <el-tag size="small" :type="role.status === 'published' ? 'success' : 'warning'">
            {{ role.statusLabel || (role.status === 'published' ? '已发布' : '草稿') }}
          </el-tag>
        </div>
        <el-tabs v-model="activeTab" class="config-tabs">
          <el-tab-pane label="权限配置" name="permissions" />
          <el-tab-pane label="数据范围" name="scope" />
          <el-tab-pane label="审批权限" name="approve" />
        </el-tabs>
      </div>

      <div class="panel-body">
        <div class="main-column">
          <div v-if="activeTab === 'permissions'" class="matrix-wrap">
            <table class="perm-table">
              <thead>
                <tr>
                  <th class="col-module">功能模块</th>
                  <th v-for="action in actions" :key="action.code">{{ action.label }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="module in modules" :key="module.code">
                  <td class="col-module"><strong>{{ module.name }}</strong></td>
                  <td v-for="action in actions" :key="action.code">
                    <el-checkbox
                      :model-value="permissionValue(module.code, action.code)"
                      @change="(val: boolean) => setPermission(module.code, action.code, val)"
                    />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-else-if="activeTab === 'scope'" class="scope-panel">
            <h3>数据范围设置</h3>
            <p>控制该角色在系统中可访问的数据边界</p>
            <el-radio-group v-model="form.dataScope" class="scope-group">
              <el-radio v-for="item in dataScopes" :key="item.code" :value="item.code">
                {{ item.label }}
              </el-radio>
            </el-radio-group>
            <div v-if="form.dataScope === 'custom'" class="custom-scope">
              <button type="button" class="plain-link" @click="deptDialogVisible = true">设置部门范围</button>
              <div v-if="form.customDeptIds.length" class="selected-depts">
                已选 {{ form.customDeptIds.length }} 个部门
              </div>
            </div>
          </div>

          <div v-else class="matrix-wrap">
            <h3 class="approve-title">审批权限配置</h3>
            <p class="approve-desc">为支持审批流程的功能模块单独配置审批权限</p>
            <table class="perm-table">
              <thead>
                <tr>
                  <th class="col-module">功能模块</th>
                  <th>审批</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="module in approveModules" :key="module.code">
                  <td class="col-module"><strong>{{ module.name }}</strong></td>
                  <td>
                    <el-checkbox
                      :model-value="permissionValue(module.code, 'approve')"
                      @change="(val: boolean) => setPermission(module.code, 'approve', val)"
                    />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <aside class="side-column">
          <article class="info-card">
            <h3>角色信息</h3>
            <dl>
              <div><dt>角色名称</dt><dd>{{ role.name }}</dd></div>
              <div><dt>角色编码</dt><dd>{{ role.code }}</dd></div>
              <div><dt>角色类型</dt><dd>{{ role.roleTypeLabel || (role.roleType === 'system' ? '系统角色' : '自定义角色') }}</dd></div>
              <div><dt>角色描述</dt><dd>{{ role.description || '—' }}</dd></div>
              <div><dt>创建时间</dt><dd>{{ role.createdAt || '—' }}</dd></div>
              <div><dt>更新时间</dt><dd>{{ role.updatedAt || '—' }}</dd></div>
            </dl>
          </article>

          <article class="info-card">
            <h3>数据范围设置</h3>
            <el-radio-group v-model="form.dataScope" class="scope-group scope-group--compact">
              <el-radio v-for="item in dataScopes" :key="item.code" :value="item.code">
                {{ item.label }}
              </el-radio>
            </el-radio-group>
            <button
              v-if="form.dataScope === 'custom'"
              type="button"
              class="plain-link"
              @click="deptDialogVisible = true"
            >
              设置部门范围
            </button>
          </article>
        </aside>
      </div>

      <footer class="panel-footer">
        <el-button @click="resetForm">取消</el-button>
        <el-button :loading="saving" @click="saveRole">保存</el-button>
        <el-button type="primary" :loading="publishing" @click="publishRole">发布权限</el-button>
      </footer>
    </section>

    <el-dialog v-model="createDialogVisible" title="新建角色" width="480px">
      <el-form label-width="88px">
        <el-form-item label="角色名称" required>
          <el-input v-model="createForm.name" placeholder="如：区域培训专员" />
        </el-form-item>
        <el-form-item label="角色编码" required>
          <el-input v-model="createForm.code" placeholder="如：ROLE_AREA_TRAIN" />
        </el-form-item>
        <el-form-item label="角色描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createRole">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deptDialogVisible" title="设置部门范围" width="520px">
      <el-tree
        v-if="deptTree.length"
        ref="deptTreeRef"
        :data="deptTree"
        node-key="id"
        show-checkbox
        default-expand-all
        :props="{ label: 'name', children: 'children' }"
      />
      <el-empty v-else description="暂无部门数据" :image-size="64" />
      <template #footer>
        <el-button @click="deptDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="applyDeptScope">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type ElTree } from 'element-plus'
import {
  adminApi,
  type AdminRoleDetail,
  type AdminRoleModule,
  type AdminRoleSummary,
  type AdminOrgTreeNode,
} from '@/api/admin'
import {
  createEmptyMatrix,
  mergeMatrix,
  type PermissionActionCode,
  type PermissionMatrix,
} from './rolePermissionShared'

const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const creating = ref(false)
const roles = ref<AdminRoleSummary[]>([])
const selectedRoleId = ref('')
const role = ref<AdminRoleDetail | null>(null)
const modules = ref<AdminRoleModule[]>([])
const actions = ref<Array<{ code: string; label: string }>>([])
const dataScopes = ref<Array<{ code: string; label: string }>>([])
const activeTab = ref<'permissions' | 'scope' | 'approve'>('permissions')
const deptTree = ref<AdminOrgTreeNode[]>([])
const deptDialogVisible = ref(false)
const deptTreeRef = ref<InstanceType<typeof ElTree>>()
const createDialogVisible = ref(false)

const form = reactive({
  dataScope: 'all',
  customDeptIds: [] as string[],
  permissions: {} as PermissionMatrix,
})

const createForm = reactive({
  name: '',
  code: '',
  description: '',
})

const approveModules = computed(() =>
  modules.value.filter(item => item.supportsApprove),
)

function permissionValue(moduleCode: string, action: string) {
  return Boolean(form.permissions[moduleCode]?.[action as PermissionActionCode])
}

function setPermission(moduleCode: string, action: string, value: boolean) {
  if (!form.permissions[moduleCode]) {
    form.permissions[moduleCode] = createEmptyMatrix([moduleCode])[moduleCode]
  }
  form.permissions[moduleCode][action as PermissionActionCode] = value
}

function applyForm(detail: AdminRoleDetail) {
  role.value = detail
  form.dataScope = detail.dataScope || 'all'
  form.customDeptIds = [...(detail.customDeptIds || [])]
  form.permissions = mergeMatrix(
    createEmptyMatrix(modules.value.map(item => item.code)),
    detail.permissions as PermissionMatrix,
  )
}

async function loadMeta() {
  const res = await adminApi.getRoleModules()
  modules.value = res.data.modules
  actions.value = res.data.actions
  dataScopes.value = res.data.dataScopes
}

async function loadRoles(preferredId?: string) {
  const res = await adminApi.listRoles()
  roles.value = res.data
  if (!roles.value.length) return
  const nextId = preferredId && roles.value.some(item => item.id === preferredId)
    ? preferredId
    : roles.value.find(item => item.code === 'ROLE_TRAIN_ADMIN')?.id || roles.value[0].id
  selectedRoleId.value = nextId
  await loadRoleDetail(nextId)
}

async function loadRoleDetail(id: string) {
  loading.value = true
  try {
    const res = await adminApi.getRole(id)
    applyForm(res.data)
  } catch {
    ElMessage.error('加载角色详情失败')
  } finally {
    loading.value = false
  }
}

async function loadDeptTree() {
  try {
    const res = await adminApi.getOrgTree()
    deptTree.value = res.data
  } catch {
    deptTree.value = []
  }
}

function onRoleChange(id: string) {
  if (id) loadRoleDetail(id)
}

function resetForm() {
  if (role.value) applyForm(role.value)
}

async function saveRole() {
  if (!selectedRoleId.value) return
  saving.value = true
  try {
    const res = await adminApi.updateRole(selectedRoleId.value, {
      dataScope: form.dataScope,
      customDeptIds: form.customDeptIds,
      permissions: form.permissions,
    })
    applyForm(res.data)
    ElMessage.success('已保存草稿')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function publishRole() {
  if (!selectedRoleId.value) return
  try {
    await ElMessageBox.confirm('确认发布当前角色的权限配置？发布后将应用于已分配该角色的账号。', '发布权限', {
      confirmButtonText: '发布',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  publishing.value = true
  try {
    await adminApi.updateRole(selectedRoleId.value, {
      dataScope: form.dataScope,
      customDeptIds: form.customDeptIds,
      permissions: form.permissions,
    })
    const res = await adminApi.publishRole(selectedRoleId.value)
    applyForm(res.data)
    await loadRoles(selectedRoleId.value)
    ElMessage.success(res.data.message || '权限已发布')
  } catch {
    ElMessage.error('发布失败')
  } finally {
    publishing.value = false
  }
}

function openCreateDialog() {
  createForm.name = ''
  createForm.code = ''
  createForm.description = ''
  createDialogVisible.value = true
}

async function createRole() {
  if (!createForm.name.trim() || !createForm.code.trim()) {
    ElMessage.warning('请填写角色名称与编码')
    return
  }
  creating.value = true
  try {
    const res = await adminApi.createRole({
      name: createForm.name.trim(),
      code: createForm.code.trim(),
      description: createForm.description.trim(),
      dataScope: 'department',
      permissions: createEmptyMatrix(modules.value.map(item => item.code)),
    })
    createDialogVisible.value = false
    await loadRoles(res.data.id)
    ElMessage.success('角色创建成功')
  } catch {
    ElMessage.error('创建角色失败')
  } finally {
    creating.value = false
  }
}

function applyDeptScope() {
  const checked = deptTreeRef.value?.getCheckedKeys(false) as string[] | undefined
  form.customDeptIds = checked ?? []
  deptDialogVisible.value = false
}

onMounted(async () => {
  loading.value = true
  try {
    await loadMeta()
    await loadDeptTree()
    await loadRoles()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.role-permission-page {
  min-height: 100%;
  padding: 20px 24px 16px;
  box-sizing: border-box;
  color: #243044;
  background: #f5f7fc;
}

.page-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
}

.page-toolbar h1 {
  margin: 0;
  font-size: 22px;
}

.page-toolbar p {
  margin: 6px 0 0;
  color: #7c8695;
  font-size: 13px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-label {
  color: #8b95a4;
  font-size: 12px;
}

.role-select {
  width: 220px;
}

.role-select :deep(.el-select-dropdown__item) {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.role-select em {
  font-style: normal;
  color: #98a1ae;
  font-size: 11px;
}

.panel {
  background: #fff;
  border: 1px solid #e7ebf1;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(34, 54, 86, 0.04);
  overflow: hidden;
}

.panel-top {
  padding: 16px 20px 0;
  border-bottom: 1px solid #eef2f7;
}

.current-role {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  font-size: 14px;
  color: #586578;
}

.current-role strong {
  color: #3478ef;
  font-size: 16px;
}

.config-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.panel-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 0;
  min-height: 520px;
}

.main-column {
  padding: 18px 20px;
  border-right: 1px solid #eef2f7;
}

.side-column {
  padding: 18px 16px;
  background: #fafbfc;
}

.matrix-wrap {
  overflow-x: auto;
}

.perm-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.perm-table th,
.perm-table td {
  padding: 12px 10px;
  border-bottom: 1px solid #f0f3f8;
  text-align: center;
}

.perm-table th.col-module,
.perm-table td.col-module {
  text-align: left;
  min-width: 140px;
}

.perm-table th {
  color: #8b95a4;
  font-weight: 500;
  background: #fafbfc;
}

.scope-panel h3,
.approve-title {
  margin: 0 0 6px;
  font-size: 15px;
}

.scope-panel p,
.approve-desc {
  margin: 0 0 16px;
  color: #8b95a4;
  font-size: 12px;
}

.scope-group {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
}

.scope-group--compact :deep(.el-radio) {
  margin-right: 0;
}

.custom-scope {
  margin-top: 16px;
}

.selected-depts {
  margin-top: 8px;
  color: #586578;
  font-size: 12px;
}

.info-card + .info-card {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #eef2f7;
}

.info-card h3 {
  margin: 0 0 12px;
  font-size: 14px;
  color: #263246;
}

.info-card dl {
  margin: 0;
}

.info-card dl > div {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 8px;
  margin-bottom: 10px;
  font-size: 12px;
}

.info-card dt {
  color: #8b95a4;
}

.info-card dd {
  margin: 0;
  color: #364153;
  line-height: 1.5;
  word-break: break-word;
}

.plain-link {
  border: none;
  background: none;
  color: #3478ef;
  font-size: 12px;
  cursor: pointer;
  padding: 0;
}

.panel-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid #eef2f7;
  background: #fff;
}

@media (max-width: 1100px) {
  .panel-body {
    grid-template-columns: 1fr;
  }

  .main-column {
    border-right: none;
    border-bottom: 1px solid #eef2f7;
  }
}

@media (max-width: 720px) {
  .page-toolbar {
    flex-direction: column;
  }

  .toolbar-right {
    width: 100%;
    flex-wrap: wrap;
  }

  .role-select {
    flex: 1;
    min-width: 180px;
  }
}
</style>
