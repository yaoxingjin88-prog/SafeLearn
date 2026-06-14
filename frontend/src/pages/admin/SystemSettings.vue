<template>
  <div class="sl-page system-settings">
    <div class="sl-page-header">
      <h2 class="sl-page-title">系统设置</h2>
      <span class="text-gray-400 text-sm">配置修改后用户端实时生效，无需重启</span>
    </div>

    <el-card v-loading="loading">
      <el-tabs v-model="activeTab">
        <el-tab-pane
          v-for="group in groups"
          :key="group.key"
          :label="group.label"
          :name="group.key"
        >
          <el-table :data="configsByCategory[group.key] || []" style="width: 100%">
            <el-table-column prop="label" label="配置项" width="180" />
            <el-table-column prop="configKey" label="键" width="220">
              <template #default="{ row }">
                <code class="text-xs text-gray-500">{{ row.configKey }}</code>
              </template>
            </el-table-column>
            <el-table-column label="当前值" min-width="240">
              <template #default="{ row }">
                <el-tag v-if="row.valueType === 'BOOLEAN'" :type="isTruthy(row.configValue) ? 'success' : 'info'">
                  {{ isTruthy(row.configValue) ? '开启' : '关闭' }}
                </el-tag>
                <span v-else-if="row.isSensitive" class="text-gray-400">{{ row.configValue || '（使用默认）' }}</span>
                <span v-else class="value-preview">{{ preview(row.configValue) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="说明" min-width="200">
              <template #default="{ row }">
                <span class="text-gray-400 text-sm">{{ row.description }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link :disabled="!row.editable" @click="openEdit(row)">
                  编辑
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="`编辑：${current?.label || ''}`" width="600px">
      <el-form label-width="90px" v-if="current">
        <el-form-item label="配置项">
          <span>{{ current.label }}</span>
          <code class="text-xs text-gray-400 ml-2">{{ current.configKey }}</code>
        </el-form-item>
        <el-form-item label="说明">
          <span class="text-gray-500 text-sm">{{ current.description }}</span>
        </el-form-item>
        <el-form-item label="值">
          <!-- BOOLEAN -->
          <el-switch v-if="current.valueType === 'BOOLEAN'" v-model="editBool" />
          <!-- INT -->
          <el-input-number v-else-if="current.valueType === 'INT'" v-model="editInt" :min="0" />
          <!-- STRING -->
          <el-input v-else-if="current.valueType === 'STRING'" v-model="editStr"
            :placeholder="current.isSensitive ? '留空则使用配置文件默认值' : ''" />
          <!-- TEXT -->
          <el-input v-else-if="current.valueType === 'TEXT'" v-model="editStr" type="textarea" :rows="6" />
          <!-- JSON_LIST：每行一条 -->
          <el-input v-else-if="current.valueType === 'JSON_LIST'" v-model="editList" type="textarea" :rows="6"
            placeholder="每行一条" />
          <!-- JSON -->
          <el-input v-else v-model="editJson" type="textarea" :rows="6" placeholder="合法 JSON" />
        </el-form-item>
        <el-form-item v-if="current.valueType === 'JSON_LIST'">
          <span class="text-gray-400 text-xs">每行一条，保存时自动转为列表</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi, type SystemConfigItem } from '@/api/admin'

const groups = [
  { key: 'ai', label: 'AI 问答' },
  { key: 'learning', label: '学习规则' },
  { key: 'scenario', label: '场景开放' },
  { key: 'dashboard', label: '首页展示' },
]

const loading = ref(false)
const saving = ref(false)
const activeTab = ref('ai')
const configsByCategory = reactive<Record<string, SystemConfigItem[]>>({})

const dialogVisible = ref(false)
const current = ref<SystemConfigItem | null>(null)
const editBool = ref(false)
const editInt = ref(0)
const editStr = ref('')
const editList = ref('')
const editJson = ref('')

function isTruthy(v: string) {
  return v === 'true' || v === '1'
}

function preview(v: string) {
  if (!v) return '（空）'
  return v.length > 60 ? v.slice(0, 60) + '…' : v
}

async function load() {
  loading.value = true
  try {
    const res = await adminApi.getSystemConfigs()
    for (const g of groups) configsByCategory[g.key] = []
    for (const item of res.data) {
      ;(configsByCategory[item.category] ||= []).push(item)
    }
  } catch {
    ElMessage.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

function openEdit(row: SystemConfigItem) {
  current.value = row
  switch (row.valueType) {
    case 'BOOLEAN':
      editBool.value = isTruthy(row.configValue)
      break
    case 'INT':
      editInt.value = Number(row.configValue) || 0
      break
    case 'JSON_LIST':
      try {
        editList.value = (JSON.parse(row.configValue || '[]') as string[]).join('\n')
      } catch {
        editList.value = ''
      }
      break
    case 'JSON':
      editJson.value = row.configValue || '{}'
      break
    default:
      // STRING / TEXT；敏感项掩码值不回填，避免把 sk-xxx**** 写回
      editStr.value = row.isSensitive ? '' : row.configValue || ''
  }
  dialogVisible.value = true
}

async function save() {
  if (!current.value) return
  let value: unknown
  switch (current.value.valueType) {
    case 'BOOLEAN':
      value = editBool.value
      break
    case 'INT':
      value = editInt.value
      break
    case 'JSON_LIST':
      value = editList.value.split('\n').map((s) => s.trim()).filter(Boolean)
      break
    case 'JSON':
      try {
        JSON.parse(editJson.value)
      } catch {
        ElMessage.error('JSON 格式不正确')
        return
      }
      value = editJson.value
      break
    default:
      value = editStr.value
  }

  saving.value = true
  try {
    await adminApi.updateSystemConfig(current.value.id, { value })
    ElMessage.success('保存成功，已实时生效')
    dialogVisible.value = false
    await load()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.value-preview {
  word-break: break-all;
}
</style>
