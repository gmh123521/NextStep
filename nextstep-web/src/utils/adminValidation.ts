export type AdminDataType = 'schools' | 'gov-posts' | 'job-positions' | 'salary-stats'

type AdminForm = Record<string, unknown>

function hasText(value: unknown): boolean {
  return typeof value === 'string' && value.trim().length > 0
}

function numberValue(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null
}

/** 与后台服务保持一致的快速校验，避免提交明显无效的数据。 */
export function validateAdminDataForm(type: AdminDataType, form: AdminForm): string | null {
  if (type === 'schools') return hasText(form.name) ? null : '院校名称不能为空'
  if (type === 'gov-posts') {
    return numberValue(form.year) != null && hasText(form.deptName) && hasText(form.postName)
      ? null
      : '考公岗位年份、部门和岗位名称不能为空'
  }
  if (type === 'job-positions') return hasText(form.name) ? null : '就业岗位名称不能为空'

  const positionId = numberValue(form.positionId)
  const statYear = numberValue(form.statYear)
  if (positionId == null || positionId < 1 || !hasText(form.city)
      || !hasText(form.experience) || !hasText(form.degree) || statYear == null) {
    return '薪资统计必须填写岗位、城市、经验、学历和统计年份'
  }
  if (statYear < 2000 || statYear > 2100) return '统计年份必须处于 2000-2100 之间'

  const numericFields = ['minSalary', 'maxSalary', 'medianSalary', 'sampleSize'] as const
  if (numericFields.some(key => {
    const value = numberValue(form[key])
    return value != null && value < 0
  })) return '薪资和样本量不能为负数'

  const min = numberValue(form.minSalary)
  const max = numberValue(form.maxSalary)
  const median = numberValue(form.medianSalary)
  if (min != null && max != null && min > max) return '最低薪资不能高于最高薪资'
  if (median != null && ((min != null && median < min) || (max != null && median > max))) {
    return '中位薪资必须处于最低薪资和最高薪资之间'
  }
  return null
}
