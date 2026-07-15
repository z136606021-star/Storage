import { describe, expect, it, vi } from 'vitest'
import { useCrudDetailDrawer } from '@/composables/useCrudDetailDrawer'

vi.mock('ant-design-vue', () => ({
  message: {
    error: vi.fn(),
  },
}))

describe('useCrudDetailDrawer', () => {
  it('loads detail when passed a record', async () => {
    const fetchById = vi.fn().mockResolvedValue({ id: 7, summary: 'record detail' })
    const { detailRecord, drawerOpen, openDetail } = useCrudDetailDrawer(fetchById)

    await openDetail({ id: 7 })

    expect(fetchById).toHaveBeenCalledWith(7)
    expect(detailRecord.value).toEqual({ id: 7, summary: 'record detail' })
    expect(drawerOpen.value).toBe(true)
  })

  it('loads detail when passed an id directly', async () => {
    const fetchById = vi.fn().mockResolvedValue({ id: 9, summary: 'id detail' })
    const { detailRecord, openDetail } = useCrudDetailDrawer(fetchById)

    await openDetail(9)

    expect(fetchById).toHaveBeenCalledWith(9)
    expect(detailRecord.value).toEqual({ id: 9, summary: 'id detail' })
  })
})
