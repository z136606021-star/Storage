import { describe, expect, it } from 'vitest'
import { useLinkedFilterOptions } from '@/composables/useLinkedFilterOptions'

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((done) => { resolve = done })
  return { promise, resolve }
}

describe('useLinkedFilterOptions', () => {
  it('ignores an older response including its ensure side effects', async () => {
    const query = { category: undefined, genericName: '新值' as string | undefined }
    const first = deferred<{ genericNames: string[] }>()
    const second = deferred<{ genericNames: string[] }>()
    let call = 0
    const fetchOptions = () => (++call === 1 ? first.promise : second.promise)
    const { filterOptionsRaw, loadFilterOptions } = useLinkedFilterOptions({ queryForm: query })
    const ensure = [{ field: 'genericName' as const, optionsKey: 'genericNames' as const }]
    const pick = (raw: { genericNames: string[] }) => raw.genericNames

    const oldRequest = loadFilterOptions(fetchOptions, { category: '旧' }, ensure, pick)
    const newRequest = loadFilterOptions(fetchOptions, {}, ensure, pick)
    second.resolve({ genericNames: ['新值'] })
    await newRequest
    first.resolve({ genericNames: ['旧值'] })
    await oldRequest

    expect(filterOptionsRaw.value.genericNames).toEqual(['新值'])
    expect(query.genericName).toBe('新值')
  })
})
