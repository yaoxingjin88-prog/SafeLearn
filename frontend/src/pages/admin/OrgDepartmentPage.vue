<template>
  <div class="org-dept-page" v-loading="loading">
    <div class="page-layout">
      <aside class="org-sidebar">
        <el-input
          v-model="treeKeyword"
          placeholder="搜索部门名称"
          clearable
          class="tree-search"
          :prefix-icon="Search"
          @input="loadTree"
        />

        <el-tree
          v-if="treeData.length"
          :key="treeKey"
          class="org-tree"
          :data="treeData"
          :props="treeProps"
          node-key="id"
          highlight-current
          default-expand-all
          :expand-on-click-node="false"
          :current-node-key="selectedDeptId"
          @node-click="onSelectDept"
        >
          <template #default="{ node, data }">
            <div class="tree-node" :class="{ active: selectedDeptId === data.id }">
              <el-icon class="tree-icon">
                <FolderOpened v-if="!node.isLeaf" />
                <Document v-else />
              </el-icon>
              <span class="tree-label">{{ data.name }}</span>
              <span class="tree-count">({{ data.memberCount }})</span>
            </div>
          </template>
        </el-tree>
        <el-empty v-else description="暂无组织数据" :image-size="64" />

        <div v-if="deptDetail" class="sidebar-stats">
          <div class="sidebar-stats-title">部门统计</div>
          <div class="sidebar-stats-grid">
            <div class="sidebar-stat">
              <span>总人数</span>
              <strong>{{ deptDetail.memberCount }}</strong>
            </div>
            <div class="sidebar-stat">
              <span>培训完成率</span>
              <strong>{{ deptDetail.trainingCompletionRate }}%</strong>
            </div>
            <div class="sidebar-stat">
              <span>高风险岗位数</span>
              <strong>{{ deptDetail.highRiskPositionCount }}</strong>
            </div>
            <div class="sidebar-stat">
              <span>证书到期人数</span>
              <strong>{{ deptDetail.certExpiringCount }}</strong>
            </div>
          </div>
        </div>
      </aside>

      <section class="org-main" v-if="deptDetail">
        <div class="main-header">
          <h1>{{ deptDetail.name }}</h1>
          <div class="header-actions">
            <el-button type="primary" @click="openCreateDept">
              <el-icon><Plus /></el-icon>
              新增部门
            </el-button>
            <el-button @click="openEditDept">
              <el-icon><EditPen /></el-icon>
              调整组织
            </el-button>
            <el-button @click="handleBatchAssign">批量分配</el-button>
          </div>
        </div>

        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">部门负责人</span>
            <span>{{ deptDetail.leaderName || '—' }}</span>
            <span v-if="deptDetail.leaderTitle" class="info-sub">{{ deptDetail.leaderTitle }}</span>
          </div>
          <div class="info-item"><span class="info-label">人员数量</span><span>{{ deptDetail.memberCount }}人</span></div>
          <div class="info-item"><span class="info-label">培训完成率</span><span>{{ deptDetail.trainingCompletionRate }}%</span></div>
          <div class="info-item"><span class="info-label">高风险岗位数</span><span>{{ deptDetail.highRiskPositionCount }}个</span></div>
          <div class="info-item"><span class="info-label">证书到期人数</span><span>{{ deptDetail.certExpiringCount }}人</span></div>
          <div class="info-item"><span class="info-label">本月新增人员</span><span>{{ deptDetail.newMembersThisMonth }}人</span></div>
        </div>

        <div class="tabs-bar">
          <button
            v-for="tab in DEPT_TABS"
            :key="tab.key"
            type="button"
            class="tab-btn"
            :class="{ active: activeTab === tab.key }"
            @click="switchTab(tab.key)"
          >
            {{ tab.label }}<template v-if="tab.key === 'members'"> ({{ memberTotal }})</template>
            <template v-else-if="tab.key === 'positions'"> ({{ positions.length }})</template>
          </button>
        </div>

        <div v-show="activeTab === 'members'" class="tab-panel">
          <el-table :data="members" style="width: 100%">
            <el-table-column label="头像" width="72" align="center">
              <template #default="{ row }">
                <el-avatar :size="36" :src="row.avatarUrl || undefined">{{ row.username?.charAt(0) }}</el-avatar>
              </template>
            </el-table-column>
            <el-table-column prop="username" label="姓名" min-width="100" />
            <el-table-column prop="employeeNo" label="工号" width="110" />
            <el-table-column prop="position" label="岗位" width="120">
              <template #default="{ row }">{{ row.position || '—' }}</template>
            </el-table-column>
            <el-table-column label="角色" width="120">
              <template #default="{ row }">{{ deptRoleLabel(row.role) }}</template>
            </el-table-column>
            <el-table-column label="培训完成率" min-width="160">
              <template #default="{ row }">
                <div class="progress-cell">
                  <span class="progress-text">{{ row.trainingCompletionRate }}%</span>
                  <el-progress :percentage="row.trainingCompletionRate" :stroke-width="8" :show-text="false" />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="证书状态" width="110">
              <template #default="{ row }">
                <span class="cert-tag" :class="certStatusClass(row.certStatus)">{{ row.certStatusLabel }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <button type="button" class="link-btn" @click="editMember(row)">编辑</button>
                <button type="button" class="link-btn" @click="removeMember(row)">移除</button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-bar">
            <span>共 {{ memberTotal }} 条</span>
            <el-pagination
              v-model:current-page="memberPage"
              v-model:page-size="memberPageSize"
              :total="memberTotal"
              :page-sizes="[10, 20, 50]"
              layout="sizes, prev, pager, next, jumper"
              @current-change="loadMembers"
              @size-change="onMemberPageSizeChange"
            />
          </div>
        </div>

        <div v-show="activeTab === 'positions'" class="tab-panel">
          <div class="position-toolbar">
            <el-button type="primary" size="small" @click="openCreatePosition">+ 新增岗位</el-button>
          </div>
          <el-table :data="positions" style="width: 100%">
            <el-table-column prop="name" label="岗位名称" min-width="160" />
            <el-table-column label="高风险岗位" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.highRisk ? 'danger' : 'info'" size="small">{{ row.highRisk ? '是' : '否' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <button type="button" class="link-btn" @click="openEditPosition(row)">编辑</button>
                <button type="button" class="link-btn" @click="deletePosition(row)">删除</button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-show="activeTab === 'stats'" class="tab-panel">
          <div v-if="statsDetail" class="stats-panel">
            <div class="stats-cards">
              <div class="stats-card">
                <span>已完成培训</span>
                <strong>{{ statsDetail.trainingBreakdown.completed }} 人</strong>
              </div>
              <div class="stats-card">
                <span>培训进行中</span>
                <strong>{{ statsDetail.trainingBreakdown.inProgress }} 人</strong>
              </div>
              <div class="stats-card">
                <span>未开始培训</span>
                <strong>{{ statsDetail.trainingBreakdown.notStarted }} 人</strong>
              </div>
            </div>
            <div class="stats-section">
              <h3>角色分布</h3>
              <ul class="dist-list">
                <li v-for="(count, role) in statsDetail.roleDistribution" :key="role">
                  <span>{{ deptRoleLabel(String(role)) }}</span>
                  <strong>{{ count }} 人</strong>
                </li>
              </ul>
            </div>
            <div class="stats-section">
              <h3>证书状态分布</h3>
              <ul class="dist-list">
                <li v-for="(count, label) in statsDetail.certStatusDistribution" :key="label">
                  <span>{{ label }}</span>
                  <strong>{{ count }} 人</strong>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      <section v-else class="org-main org-main-empty">
        <el-empty description="请选择左侧部门查看详情" />
      </section>
    </div>

    <el-dialog v-model="deptDialogVisible" :title="deptDialogMode === 'create' ? '新增部门' : '调整组织'" width="480px">
      <el-form ref="deptFormRef" :model="deptForm" label-width="96px">
        <el-form-item label="部门名称" required>
          <el-input v-model="deptForm.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="deptForm.leaderName" placeholder="负责人姓名" />
        </el-form-item>
        <el-form-item label="负责人职务">
          <el-input v-model="deptForm.leaderTitle" placeholder="如：安全部门经理" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deptDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDept">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="positionDialogVisible" :title="positionDialogMode === 'create' ? '新增岗位' : '编辑岗位'" width="420px">
      <el-form :model="positionForm" label-width="96px">
        <el-form-item label="岗位名称" required>
          <el-input v-model="positionForm.name" placeholder="请输入岗位名称" />
        </el-form-item>
        <el-form-item label="高风险岗位">
          <el-switch v-model="positionForm.highRisk" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="positionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPosition">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="memberDrawerVisible" :title="`编辑成员 · ${memberForm.username}`" size="420px" destroy-on-close>
      <div class="member-drawer-body">
        <div class="member-drawer-header">
          <el-avatar :size="56" :src="memberForm.avatarUrl || undefined">{{ memberForm.username?.charAt(0) }}</el-avatar>
          <div>
            <strong>{{ memberForm.username }}</strong>
            <p>{{ deptDetail?.name || memberForm.department || '—' }}</p>
          </div>
        </div>

        <el-form ref="memberFormRef" :model="memberForm" :rules="memberRules" label-width="88px">
          <el-form-item label="工号">
            <el-input v-model="memberForm.employeeNo" placeholder="请输入工号" />
          </el-form-item>
          <el-form-item label="岗位" prop="position">
            <el-select
              v-model="memberForm.position"
              filterable
              allow-create
              default-first-option
              placeholder="请选择或输入岗位"
              style="width: 100%"
            >
              <el-option v-for="pos in positions" :key="pos.id" :label="pos.name" :value="pos.name" />
            </el-select>
          </el-form-item>
          <el-form-item label="角色" prop="role">
            <el-select v-model="memberForm.role" placeholder="请选择角色" style="width: 100%">
              <el-option label="普通学员" value="trainee" />
              <el-option label="安全管理员" value="admin" />
            </el-select>
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="memberForm.phone" placeholder="请输入手机号" maxlength="20" />
          </el-form-item>
          <el-form-item label="账号状态">
            <el-switch v-model="memberForm.enabled" active-text="正常" inactive-text="停用" />
          </el-form-item>
        </el-form>

        <div class="readonly-stats">
          <div><span>培训完成率</span><strong>{{ memberForm.trainingCompletionRate ?? 0 }}%</strong></div>
          <div><span>证书状态</span><strong>{{ memberForm.certStatusLabel || '—' }}</strong></div>
        </div>
      </div>

      <template #footer>
        <div class="member-drawer-footer">
          <button type="button" class="profile-link" @click="viewMemberProfile">查看完整档案</button>
          <div class="footer-actions">
            <el-button @click="memberDrawerVisible = false">取消</el-button>
            <el-button type="primary" :loading="submitting" @click="submitMember">保存</el-button>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus, EditPen, FolderOpened, Document } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  adminApi,
  type AdminOrgTreeNode,
  type AdminDepartmentDetail,
  type AdminDepartmentMember,
  type AdminDepartmentPosition,
  type AdminDepartmentStatsDetail,
} from '@/api/admin'
import { DEPT_TABS, deptRoleLabel, certStatusClass, type DeptTabKey } from './orgDepartmentShared'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const treeKeyword = ref('')
const treeKey = ref(0)
const treeData = ref<AdminOrgTreeNode[]>([])
const selectedDeptId = ref('')
const deptDetail = ref<AdminDepartmentDetail | null>(null)
const members = ref<AdminDepartmentMember[]>([])
const memberTotal = ref(0)
const memberPage = ref(1)
const memberPageSize = ref(10)
const positions = ref<AdminDepartmentPosition[]>([])
const statsDetail = ref<AdminDepartmentStatsDetail | null>(null)
const activeTab = ref<DeptTabKey>('members')

const treeProps = { children: 'children', label: 'name' }

const deptDialogVisible = ref(false)
const deptDialogMode = ref<'create' | 'edit'>('create')
const deptForm = reactive({ name: '', leaderName: '', leaderTitle: '' })

const positionDialogVisible = ref(false)
const positionDialogMode = ref<'create' | 'edit'>('create')
const editingPositionId = ref('')
const positionForm = reactive({ name: '', highRisk: false })

const memberDrawerVisible = ref(false)
const memberFormRef = ref<FormInstance>()
const memberForm = reactive({
  id: '',
  username: '',
  employeeNo: '',
  position: '',
  role: 'trainee',
  phone: '',
  enabled: true,
  department: '',
  avatarUrl: '',
  trainingCompletionRate: 0,
  certStatusLabel: '',
})

const memberRules: FormRules = {
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

async function loadTree() {
  try {
    const res = await adminApi.getOrgTree(treeKeyword.value.trim() || undefined)
    treeData.value = res.data || []
    treeKey.value += 1
    if (!selectedDeptId.value && treeData.value.length) {
      const target = findDefaultDept(treeData.value)
      if (target) await selectDept(target.id)
    }
  } catch {
    ElMessage.error('加载组织树失败')
  }
}

function findDefaultDept(nodes: AdminOrgTreeNode[]): AdminOrgTreeNode | null {
  for (const node of nodes) {
    if (node.name === '安全部') return node
    if (node.children?.length) {
      const found = findDefaultDept(node.children)
      if (found) return found
    }
  }
  const root = nodes[0]
  if (root?.children?.length) return root.children[0]
  return root || null
}

async function onSelectDept(data: AdminOrgTreeNode) {
  await selectDept(data.id)
}

async function selectDept(id: string) {
  selectedDeptId.value = id
  memberPage.value = 1
  await Promise.all([loadDeptDetail(), loadMembers(), loadPositions()])
  if (activeTab.value === 'stats') await loadStats()
}

async function loadDeptDetail() {
  if (!selectedDeptId.value) return
  const res = await adminApi.getDepartmentDetail(selectedDeptId.value)
  deptDetail.value = res.data
}

async function loadMembers() {
  if (!selectedDeptId.value) return
  const res = await adminApi.getDepartmentMembers(selectedDeptId.value, {
    page: memberPage.value,
    pageSize: memberPageSize.value,
  })
  members.value = res.data?.items || []
  memberTotal.value = res.data?.total ?? 0
}

async function loadPositions() {
  if (!selectedDeptId.value) return
  const res = await adminApi.getDepartmentPositions(selectedDeptId.value)
  positions.value = res.data || []
}

async function loadStats() {
  if (!selectedDeptId.value) return
  const res = await adminApi.getDepartmentStats(selectedDeptId.value)
  statsDetail.value = res.data
}

function onMemberPageSizeChange() {
  memberPage.value = 1
  loadMembers()
}

async function switchTab(key: DeptTabKey) {
  activeTab.value = key
  if (key === 'stats' && !statsDetail.value) await loadStats()
}

function openCreateDept() {
  deptDialogMode.value = 'create'
  Object.assign(deptForm, { name: '', leaderName: '', leaderTitle: '' })
  deptDialogVisible.value = true
}

function openEditDept() {
  if (!deptDetail.value) return
  deptDialogMode.value = 'edit'
  Object.assign(deptForm, {
    name: deptDetail.value.name,
    leaderName: deptDetail.value.leaderName || '',
    leaderTitle: deptDetail.value.leaderTitle || '',
  })
  deptDialogVisible.value = true
}

async function submitDept() {
  if (!deptForm.name.trim()) {
    ElMessage.warning('请输入部门名称')
    return
  }
  submitting.value = true
  try {
    const payload = {
      name: deptForm.name.trim(),
      leaderName: deptForm.leaderName.trim(),
      leaderTitle: deptForm.leaderTitle.trim(),
    }
    if (deptDialogMode.value === 'create') {
      await adminApi.createDepartment({ ...payload, parentId: selectedDeptId.value })
      ElMessage.success('部门已创建')
    } else {
      await adminApi.updateDepartment(selectedDeptId.value, payload)
      ElMessage.success('部门已更新')
    }
    deptDialogVisible.value = false
    await loadTree()
    await loadDeptDetail()
  } finally {
    submitting.value = false
  }
}

function handleBatchAssign() {
  ElMessage.info('请前往用户管理进行批量分配')
  router.push('/admin/users')
}

function editMember(row: AdminDepartmentMember) {
  Object.assign(memberForm, {
    id: row.id,
    username: row.username,
    employeeNo: row.employeeNo || '',
    position: row.position || '',
    role: row.role || 'trainee',
    phone: row.phone || '',
    enabled: row.enabled !== false,
    department: row.department || deptDetail.value?.name || '',
    avatarUrl: row.avatarUrl || '',
    trainingCompletionRate: row.trainingCompletionRate ?? 0,
    certStatusLabel: row.certStatusLabel || '未持证',
  })
  memberDrawerVisible.value = true
}

function viewMemberProfile() {
  if (!memberForm.id) return
  router.push(`/admin/users/${memberForm.id}`)
}

async function submitMember() {
  const valid = await memberFormRef.value?.validate().catch(() => false)
  if (!valid || !selectedDeptId.value || !memberForm.id) return

  submitting.value = true
  try {
    await adminApi.updateDepartmentMember(selectedDeptId.value, memberForm.id, {
      employeeNo: memberForm.employeeNo.trim(),
      position: memberForm.position.trim(),
      role: memberForm.role,
      phone: memberForm.phone.trim(),
      enabled: memberForm.enabled,
    })
    ElMessage.success('成员信息已更新')
    memberDrawerVisible.value = false
    await Promise.all([loadTree(), loadDeptDetail(), loadMembers()])
    if (activeTab.value === 'stats') {
      statsDetail.value = null
      await loadStats()
    }
  } finally {
    submitting.value = false
  }
}

async function removeMember(row: AdminDepartmentMember) {
  await ElMessageBox.confirm(`确定将「${row.username}」从当前部门移除吗？`, '提示', { type: 'warning' })
  await adminApi.removeDepartmentMember(selectedDeptId.value, row.id)
  ElMessage.success('已移除')
  await Promise.all([loadTree(), loadDeptDetail(), loadMembers()])
}

function openCreatePosition() {
  positionDialogMode.value = 'create'
  editingPositionId.value = ''
  Object.assign(positionForm, { name: '', highRisk: false })
  positionDialogVisible.value = true
}

function openEditPosition(row: AdminDepartmentPosition) {
  positionDialogMode.value = 'edit'
  editingPositionId.value = row.id
  Object.assign(positionForm, { name: row.name, highRisk: row.highRisk })
  positionDialogVisible.value = true
}

async function submitPosition() {
  if (!positionForm.name.trim()) {
    ElMessage.warning('请输入岗位名称')
    return
  }
  submitting.value = true
  try {
    const payload = { name: positionForm.name.trim(), highRisk: positionForm.highRisk }
    if (positionDialogMode.value === 'create') {
      await adminApi.createDepartmentPosition(selectedDeptId.value, payload)
      ElMessage.success('岗位已创建')
    } else {
      await adminApi.updateDepartmentPosition(editingPositionId.value, payload)
      ElMessage.success('岗位已更新')
    }
    positionDialogVisible.value = false
    await loadPositions()
    await loadDeptDetail()
  } finally {
    submitting.value = false
  }
}

async function deletePosition(row: AdminDepartmentPosition) {
  await ElMessageBox.confirm(`确定删除岗位「${row.name}」吗？`, '提示', { type: 'warning' })
  await adminApi.deleteDepartmentPosition(row.id)
  ElMessage.success('已删除')
  await loadPositions()
}

onMounted(async () => {
  loading.value = true
  try {
    await loadTree()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.org-dept-page {
  min-height: 100%;
}

.page-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 16px;
  align-items: start;
}

.org-sidebar {
  background: #fff;
  border-radius: 10px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  min-height: 640px;
  display: flex;
  flex-direction: column;
}

.tree-search {
  margin-bottom: 12px;
}

.org-tree {
  flex: 1;
  background: transparent;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
  padding: 2px 0;
  font-size: 13px;
}

.tree-node.active {
  color: #409eff;
}

.tree-icon {
  color: #409eff;
  flex-shrink: 0;
}

.tree-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-count {
  color: #909399;
  flex-shrink: 0;
}

.sidebar-stats {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f2f3f5;
}

.sidebar-stats-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.sidebar-stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.sidebar-stat {
  background: #f8fafc;
  border-radius: 8px;
  padding: 10px;
  font-size: 12px;
  color: #909399;
}

.sidebar-stat strong {
  display: block;
  margin-top: 4px;
  font-size: 18px;
  color: #303133;
}

.org-main {
  background: #fff;
  border-radius: 10px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  min-height: 640px;
}

.org-main-empty {
  display: flex;
  align-items: center;
  justify-content: center;
}

.main-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.main-header h1 {
  margin: 0;
  font-size: 22px;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px 24px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 20px;
}

.info-item {
  font-size: 14px;
  color: #303133;
}

.info-label {
  color: #909399;
  margin-right: 8px;
}

.info-sub {
  display: block;
  margin-top: 2px;
  color: #606266;
  font-size: 13px;
}

.tabs-bar {
  display: flex;
  gap: 24px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 16px;
}

.tab-btn {
  border: none;
  background: none;
  padding: 0 0 12px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  position: relative;
}

.tab-btn.active {
  color: #409eff;
  font-weight: 600;
}

.tab-btn.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  background: #409eff;
}

.progress-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.progress-text {
  font-size: 12px;
  color: #606266;
}

.cert-tag {
  font-size: 12px;
}

.cert-valid { color: #67c23a; }
.cert-expiring { color: #e6a23c; }
.cert-expired { color: #f56c6c; }
.cert-none { color: #909399; }

.link-btn {
  border: none;
  background: none;
  color: #409eff;
  cursor: pointer;
  font-size: 14px;
  padding: 0 4px;
}

.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  color: #606266;
  font-size: 14px;
}

.position-toolbar {
  margin-bottom: 12px;
}

.stats-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stats-card {
  background: #f8fafc;
  border-radius: 8px;
  padding: 16px;
  font-size: 13px;
  color: #909399;
}

.stats-card strong {
  display: block;
  margin-top: 6px;
  font-size: 20px;
  color: #303133;
}

.stats-section h3 {
  margin: 0 0 10px;
  font-size: 15px;
  color: #303133;
}

.dist-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.dist-list li {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f2f3f5;
  font-size: 14px;
}

@media (max-width: 1200px) {
  .page-layout {
    grid-template-columns: 1fr;
  }

  .info-grid {
    grid-template-columns: 1fr 1fr;
  }
}

.member-drawer-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.member-drawer-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f2f3f5;
}

.member-drawer-header strong {
  display: block;
  font-size: 16px;
  color: #303133;
}

.member-drawer-header p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #909399;
}

.readonly-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 14px;
  background: #f8fafc;
  border-radius: 8px;
}

.readonly-stats div {
  font-size: 13px;
  color: #909399;
}

.readonly-stats strong {
  display: block;
  margin-top: 4px;
  color: #303133;
  font-size: 15px;
}

.member-drawer-footer {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.profile-link {
  align-self: flex-start;
  border: none;
  background: none;
  color: #409eff;
  cursor: pointer;
  font-size: 13px;
  padding: 0;
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
