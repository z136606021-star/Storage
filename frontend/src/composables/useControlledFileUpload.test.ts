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
      allowedTypes: ['image/png'],
      uploadFn,
    })

    const accepted = enqueueFile(createFile('large.png', 2048))
    expect(accepted).toBe(true)
    await Promise.resolve()
    await Promise.resolve()

    expect(uploadFn).toHaveBeenCalledTimes(1)
    expect(items.value[0]?.status).toBe('error')
  })

  it('keeps all completed uploads when completions arrive out of order', async () => {
    const first = deferred<FileUploadResult>()
    const second = deferred<FileUploadResult>()
    const uploadFn = vi
      .fn()
      .mockImplementationOnce(() => first.promise)
      .mockImplementationOnce(() => second.promise)

    const { enqueueFile, items } = useControlledFileUpload({
      policy: { uploadConcurrency: 2, maxFilesPerRecord: 20 },
      allowedTypes: ['image/png'],
      uploadFn,
    })

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

  it('limits concurrent uploads to configured concurrency', async () => {
    const gates = Array.from({ length: 5 }, () => deferred<FileUploadResult>())
    const uploadFn = vi.fn().mockImplementation(() => gates[uploadFn.mock.calls.length - 1].promise)

    const { enqueueFile, items } = useControlledFileUpload({
      policy: { uploadConcurrency: 3, maxFilesPerRecord: 20 },
      allowedTypes: ['image/png'],
      uploadFn,
    })

    for (let i = 0; i < 5; i += 1) {
      enqueueFile(createFile(`file-${i}.png`))
    }

    await Promise.resolve()
    expect(uploadFn).toHaveBeenCalledTimes(3)
    expect(items.value.filter((item) => item.status === 'uploading').length).toBeGreaterThanOrEqual(3)

    for (const gate of gates.slice(0, 3)) {
      gate.resolve({
        id: 1,
        objectKey: 'done',
        originalName: 'done.png',
        contentType: 'image/png',
        sizeBytes: 1,
        url: '/api/files/preview?objectKey=done',
      })
    }
    await Promise.resolve()
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

    const { enqueueFile, items, resolveObjectKeys } = useControlledFileUpload({
      policy: { uploadConcurrency: 2, maxFilesPerRecord: 20 },
      allowedTypes: ['image/png'],
      uploadFn,
    })

    enqueueFile(createFile('ok.png'))
    enqueueFile(createFile('bad.png'))
    await Promise.resolve()
    await Promise.resolve()

    expect(items.value.filter((item) => item.status === 'done')).toHaveLength(1)
    expect(items.value.filter((item) => item.status === 'error')).toHaveLength(1)
    expect(resolveObjectKeys()).toEqual(['ok'])
  })

  it('rejects enqueue when max count reached', () => {
    const { setItems, enqueueFile, items } = useControlledFileUpload({
      policy: { maxFilesPerRecord: 2 },
      allowedTypes: ['image/png'],
    })

    setItems([
      { uid: '1', name: 'a.png', status: 'done', objectKey: 'a' },
      { uid: '2', name: 'b.png', status: 'done', objectKey: 'b' },
    ])

    const accepted = enqueueFile(createFile('c.png'))
    expect(accepted).toBe(false)
    expect(items.value).toHaveLength(2)
  })
})
