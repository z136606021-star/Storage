import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import { useMaterialIoRouteDetail } from '@/composables/useMaterialIoRouteDetail'

function createRoute(query: Record<string, unknown> = {}) {
  return ref({
    path: '/configured-io-route',
    query,
  })
}

function createRouter() {
  const replace = vi.fn().mockResolvedValue(undefined)
  return { replace }
}

describe('useMaterialIoRouteDetail', () => {
  it('initFromRoute opens detail when id query is valid', async () => {
    const route = createRoute({ id: '7' })
    const router = createRouter()
    const openDetail = vi.fn().mockResolvedValue(undefined)
    const onRouteIdChange = vi.fn().mockResolvedValue(undefined)

    const { initFromRoute, highlightRecordId } = useMaterialIoRouteDetail({
      route: route.value as never,
      router: router as never,
      openDetail,
      onRouteIdChange,
    })

    const applied = await initFromRoute()

    expect(applied).toBe(true)
    expect(highlightRecordId.value).toBe(7)
    expect(onRouteIdChange).toHaveBeenCalledWith(7)
    expect(openDetail).toHaveBeenCalledWith({ id: 7 })
  })

  it('initFromRoute returns false when id query is missing', async () => {
    const route = createRoute()
    const router = createRouter()
    const openDetail = vi.fn()

    const { initFromRoute } = useMaterialIoRouteDetail({
      route: route.value as never,
      router: router as never,
      openDetail,
    })

    expect(await initFromRoute()).toBe(false)
    expect(openDetail).not.toHaveBeenCalled()
  })

  it('clearRouteDetailQuery removes id and preserves other query params', async () => {
    const route = createRoute({ id: '9', materialLedgerId: '3' })
    const router = createRouter()
    const onRouteIdChange = vi.fn().mockResolvedValue(undefined)

    const { clearRouteDetailQuery, highlightRecordId } = useMaterialIoRouteDetail({
      route: route.value as never,
      router: router as never,
      openDetail: vi.fn(),
      onRouteIdChange,
    })

    await clearRouteDetailQuery()

    expect(highlightRecordId.value).toBeNull()
    expect(router.replace).toHaveBeenCalledWith({
      path: '/configured-io-route',
      query: { materialLedgerId: '3' },
    })
    expect(onRouteIdChange).toHaveBeenCalledWith(null)
  })

  it('customRow applies highlight class for active record', async () => {
    const route = createRoute({ id: '5' })
    const router = createRouter()

    const { initFromRoute, customRow } = useMaterialIoRouteDetail({
      route: route.value as never,
      router: router as never,
      openDetail: vi.fn().mockResolvedValue(undefined),
    })

    await initFromRoute()

    expect(customRow({ id: 5 }).class).toBe('io-row-highlight')
    expect(customRow({ id: 4 }).class).toBe('')
  })
})
