export const PERMISSION_ACTIONS = [
  { code: 'view', label: '查看' },
  { code: 'create', label: '新增' },
  { code: 'edit', label: '编辑' },
  { code: 'delete', label: '删除' },
  { code: 'export', label: '导出' },
  { code: 'approve', label: '审批' },
] as const

export type PermissionActionCode = typeof PERMISSION_ACTIONS[number]['code']

export type PermissionMatrix = Record<string, Record<PermissionActionCode, boolean>>

export const DATA_SCOPE_OPTIONS = [
  { code: 'all', label: '全部数据' },
  { code: 'department', label: '本部门数据' },
  { code: 'dept_tree', label: '本部门及下级部门数据' },
  { code: 'self', label: '仅本人数据' },
  { code: 'custom', label: '自定义数据范围' },
] as const

export function createEmptyMatrix(moduleCodes: string[]): PermissionMatrix {
  const matrix: PermissionMatrix = {}
  for (const code of moduleCodes) {
    matrix[code] = {
      view: false,
      create: false,
      edit: false,
      delete: false,
      export: false,
      approve: false,
    }
  }
  return matrix
}

export function mergeMatrix(base: PermissionMatrix, incoming?: PermissionMatrix): PermissionMatrix {
  const merged = createEmptyMatrix(Object.keys(base))
  for (const moduleCode of Object.keys(merged)) {
    for (const action of PERMISSION_ACTIONS) {
      merged[moduleCode][action.code] = Boolean(incoming?.[moduleCode]?.[action.code])
    }
  }
  return merged
}
