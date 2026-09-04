import assert from 'node:assert/strict'
import { validateAdminDataForm } from '../src/utils/adminValidation.ts'

assert.equal(validateAdminDataForm('schools', { name: '  ' }), '院校名称不能为空')
assert.equal(
  validateAdminDataForm('gov-posts', { year: 2026, deptName: '', postName: '技术岗' }),
  '考公岗位年份、部门和岗位名称不能为空'
)
assert.equal(validateAdminDataForm('job-positions', { name: '' }), '就业岗位名称不能为空')
assert.equal(
  validateAdminDataForm('salary-stats', {
    positionId: 1, city: '上海', experience: 'FRESH', degree: 'BACHELOR', statYear: 2026,
    minSalary: 20000, maxSalary: 10000
  }),
  '最低薪资不能高于最高薪资'
)
assert.equal(
  validateAdminDataForm('salary-stats', {
    positionId: 1, city: '上海', experience: 'FRESH', degree: 'BACHELOR', statYear: 2026,
    minSalary: 10000, maxSalary: 20000, medianSalary: 25000
  }),
  '中位薪资必须处于最低薪资和最高薪资之间'
)
assert.equal(
  validateAdminDataForm('salary-stats', {
    positionId: 1, city: '上海', experience: 'FRESH', degree: 'BACHELOR', statYear: 2026,
    minSalary: 10000, maxSalary: 20000, medianSalary: 15000, sampleSize: 100
  }),
  null
)

console.log('ADMIN_VALIDATION_CHECK_OK')
