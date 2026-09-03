import assert from 'node:assert/strict'
import { formatRequestError } from '../src/utils/error.ts'

assert.equal(formatRequestError({ msg: '画像不完整' }, '保存失败'), '画像不完整')
assert.equal(formatRequestError({ response: { data: { msg: '接口拒绝' } } }, '请求失败'), '接口拒绝')
assert.equal(formatRequestError(new Error('Network Error'), '请求失败'), '网络连接失败，请检查网络后重试')
assert.equal(formatRequestError({ code: 'ECONNABORTED', message: 'timeout of 15000ms exceeded' }, '请求失败'), '请求超时，请稍后重试')
assert.equal(formatRequestError(null, '保存失败'), '保存失败')

console.log('ERROR_MESSAGE_CHECK_OK')
