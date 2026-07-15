import { describe, expect, it } from 'vitest'
import axios, { type InternalAxiosRequestConfig } from 'axios'
import { getErrorMessage } from '@/api/http'

function mockResponse(status: number, data: unknown) {
  return {
    status,
    data,
    statusText: String(status),
    headers: {},
    config: { headers: {} } as InternalAxiosRequestConfig,
  }
}

describe('getErrorMessage', () => {
  it('returns timeout message for aborted uploads', () => {
    const error = new axios.AxiosError('timeout', 'ECONNABORTED')
    expect(getErrorMessage(error, '上传失败')).toBe('请求超时，请检查网络或稍后重试')
  })

  it('returns network message when response is missing', () => {
    const error = new axios.AxiosError('network')
    expect(getErrorMessage(error, '上传失败')).toBe('网络连接失败，请检查网络或代理配置')
  })

  it('returns explicit 413 message', () => {
    const error = new axios.AxiosError('payload too large', 'ERR_BAD_REQUEST', undefined, undefined, mockResponse(413, '<html>Request Entity Too Large</html>'))
    expect(getErrorMessage(error, '上传失败')).toBe('文件超过服务器允许大小（HTTP 413）')
  })

  it('extracts backend JSON message', () => {
    const error = new axios.AxiosError('bad request', 'ERR_BAD_REQUEST', undefined, undefined, mockResponse(400, { message: '文件大小超过限制' }))
    expect(getErrorMessage(error, '上传失败')).toBe('文件大小超过限制')
  })

  it('preserves plain Error message', () => {
    expect(getErrorMessage(new Error('NTID 不能包含空格或空白字符'), '保存失败')).toBe(
      'NTID 不能包含空格或空白字符',
    )
  })

  it('appends request id from response header when body omits it', () => {
    const response = mockResponse(400, { message: 'NTID 已存在' })
    response.headers = { 'x-request-id': 'req-abc-123' }
    const error = new axios.AxiosError('bad request', 'ERR_BAD_REQUEST', undefined, undefined, response)
    expect(getErrorMessage(error, '保存失败')).toBe('NTID 已存在（请求ID: req-abc-123）')
  })
})
