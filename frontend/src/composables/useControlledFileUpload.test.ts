import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useControlledFileUpload } from '@/composables/useControlledFileUpload'
import type { FileUploadResult } from '@/types/file'

vi.mock('ant-design-vue', () => ({
  message: {
    success: vi.fn(),
    error: vi.fn(),
  },
}))

function createFile(name: string, size = 1024, type = 'image/png'): File {
  return new File([new Uint8Array(size)], name, { type })
}

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((res) => {
    resolve = res
  })
  return { promise, resolve }
}

describe('useControlledFileUpload', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('enqueues files larger than policy max size and relies on backend validation', async () => {
    const uploadFn = vi.fn().mockRejectedValue(new Error('文件大小超过限制'))
    const { enqueueFile, items } = useControlledFileUpload({
      policy: { maxSizeBytes: 1024 },
      uploadFn,
    })

    const accepted = enqueueFile(createFile('large.png', 2048))
    expect(accepted).toBe(true)
    await Promise.resolve()
    await Promise.resolve()

    expect(uploadFn).toHaveBeenCalledTimes(1)
    expect(items.value[0]?.status).toBe('error')
  })

  it('accepts unknown or empty MIME types for generic attachments', async () => {
    const uploadFn = vi.fn().mockResolvedValue({
      id: 1,
      objectKey: '2026-07-13/demo.bin',
      originalName: 'demo.bin',
      contentType: 'application/octet-stream',
      sizeBytes: 16,
      url: '/api/files/preview?objectKey=demo',
    } satisfies FileUploadResult)

    const { enqueueFile, items } = useControlledFileUpload({ uploadFn })

    expect(enqueueFile(createFile('demo.bin', 16, ''))).toBe(true)
    expect(enqueueFile(createFile('legacy.cad', 16, 'application/x-cad'))).toBe(true)

    await Promise.resolve()
    await Promise.resolve()

    expect(uploadFn).toHaveBeenCalledTimes(2)
    expect(items.value.every((item) => item.status === 'done')).toBe(true)
  })

  it('keeps all completed uploads when completions arrive out of order', async () => {
    const first = deferred<FileUploadResult>()
    const second = deferred<FileUploadResult>()
    const uploadFn = vi
      .fn()
      .mockImplementationOnce(() => first.promise)
      .mockImplementationOnce(() => second.promise)

    const { enqueueFile, items } = useControlledFileUpload({ uploadFn })

    enqueueFile(createFile('a.png'))
    enqueueFile(createFile('b.png'))

    second.resolve({
      id: 2,
      objectKey: '2026-07-10/b.png',
      originalName: 'b.png',
      contentType: 'image/png',
      sizeBytes: 1024,
      url: '/api/files/preview?objectKey=b',
    })
    await Promise.resolve()
    await Promise.resolve()

    first.resolve({
      id: 1,
      objectKey: '2026-07-10/a.png',
      originalName: 'a.png',
      contentType: 'image/png',
      sizeBytes: 1024,
      url: '/api/files/preview?objectKey=a',
    })
    await Promise.resolve()
    await Promise.resolve()

    expect(items.value).toHaveLength(2)
    expect(items.value.every((item) => item.status === 'done')).toBe(true)
    expect(items.value.map((item) => item.objectKey)).toEqual([
      '2026-07-10/a.png',
      '2026-07-10/b.png',
    ])
  })

  it('starts all selected uploads without an application concurrency queue', async () => {
    const gates = Array.from({ length: 5 }, () => deferred<FileUploadResult>())
    const uploadFn = vi.fn().mockImplementation(() => gates[uploadFn.mock.calls.length - 1].promise)

    const { enqueueFile } = useControlledFileUpload({ uploadFn })

    for (let i = 0; i < 5; i += 1) {
      enqueueFile(createFile(`file-${i}.png`))
    }

    await Promise.resolve()
    expect(uploadFn).toHaveBeenCalledTimes(5)
  })

  it('marks failed upload without removing successful ones', async () => {
    const uploadFn = vi
      .fn()
      .mockResolvedValueOnce({
        id: 1,
        objectKey: 'ok',
        originalName: 'ok.png',
        contentType: 'image/png',
        sizeBytes: 1,
        url: '/api/files/preview?objectKey=ok',
      })
      .mockRejectedValueOnce(new Error('network'))

    const { enqueueFile, items, resolveObjectKeys } = useControlledFileUpload({ uploadFn })

    enqueueFile(createFile('ok.png'))
    enqueueFile(createFile('bad.png'))
    await Promise.resolve()
    await Promise.resolve()

    expect(items.value.filter((item) => item.status === 'done')).toHaveLength(1)
    expect(items.value.filter((item) => item.status === 'error')).toHaveLength(1)
    expect(resolveObjectKeys()).toEqual(['ok'])
  })

  it('still validates MIME when allowedTypes is configured', () => {
    const uploadFn = vi.fn()
    const { enqueueFile } = useControlledFileUpload({
      allowedTypes: ['image/png'],
      uploadFn,
    })

    expect(enqueueFile(createFile('demo.jpg', 16, 'image/jpeg'))).toBe(false)
    expect(uploadFn).not.toHaveBeenCalled()
  })
})
