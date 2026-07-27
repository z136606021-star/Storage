import { describe, expect, it, vi } from 'vitest'
import { usePaginatedCrudList } from '@/composables/usePaginatedCrudList'

vi.mock('ant-design-vue', () => ({ message: { error: vi.fn() } }))

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((done) => { resolve = done })
  return { promise, resolve }
}

describe('usePaginatedCrudList', () => {
  it('keeps the latest response and loading state', async () => {
    const first = deferred<any>()
    const second = deferred<any>()
    let call = 0
    const list = usePaginatedCrudList<{ id: number }, { keyword?: string }>({
      fetchPage: () => (++call === 1 ? first.promise : second.promise),
      buildQueryParams: () => ({}),
    })

    const oldRequest = list.loadData()
    const latestRequest = list.handleSearch()
    first.resolve({ records: [{ id: 1 }], total: 1, current: 1, size: 10 })
    await oldRequest
    expect(list.loading.value).toBe(true)
    second.resolve({ records: [{ id: 2 }], total: 1, current: 1, size: 10 })
    await latestRequest

    expect(list.dataSource.value).toEqual([{ id: 2 }])
    expect(list.loading.value).toBe(false)
  })
})
