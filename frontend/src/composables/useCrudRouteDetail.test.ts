// @vitest-environment happy-dom

import { createApp, defineComponent, h, KeepAlive, nextTick, reactive, ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'
import { useCrudRouteDetail } from '@/composables/useCrudRouteDetail'

function createRoute(query: Record<string, unknown> = {}) {
  return {
    path: '/configured-detail-route',
    query,
  }
}

function createRouter() {
  const replace = vi.fn().mockResolvedValue(undefined)
  return { replace }
}

describe('useCrudRouteDetail', () => {
  it('omitKey mode removes only the configured query key', async () => {
    const route = createRoute({ id: '9', materialLedgerId: '3' })
    const router = createRouter()
    const onRouteIdChange = vi.fn().mockResolvedValue(undefined)

    const { clearRouteDetailQuery } = useCrudRouteDetail({
      route: route as never,
      router: router as never,
      openDetail: vi.fn(),
      queryKey: 'id',
      parseId: (raw) => (typeof raw === 'string' && /^\d+$/.test(raw) ? Number(raw) : null),
      rowHighlightClass: 'io-row-highlight',
      clearQueryMode: 'omitKey',
      onRouteIdChange,
    })

    await clearRouteDetailQuery()

    expect(router.replace).toHaveBeenCalledWith({
      path: '/configured-detail-route',
      query: { materialLedgerId: '3' },
    })
    expect(onRouteIdChange).toHaveBeenCalledWith(null)
  })

  it('clearAll mode replaces route with path only', async () => {
    const route = createRoute({ materialLedgerId: '3', foo: 'bar' })
    const router = createRouter()

    const { clearRouteDetailQuery } = useCrudRouteDetail({
      route: route as never,
      router: router as never,
      openDetail: vi.fn(),
      queryKey: 'materialLedgerId',
      parseId: (raw) => (typeof raw === 'string' && /^\d+$/.test(raw) ? Number(raw) : null),
      rowHighlightClass: 'ledger-row-highlight',
      clearQueryMode: 'clearAll',
    })

    await clearRouteDetailQuery()

    expect(router.replace).toHaveBeenCalledWith({
      path: '/configured-detail-route',
    })
  })

  it('calls onRouteIdChange before openDetail', async () => {
    const route = createRoute({ id: '5' })
    const router = createRouter()
    const openDetail = vi.fn().mockResolvedValue(undefined)
    const onRouteIdChange = vi.fn().mockResolvedValue(undefined)
    const callOrder: string[] = []
    onRouteIdChange.mockImplementation(async () => {
      callOrder.push('onRouteIdChange')
    })
    openDetail.mockImplementation(async () => {
      callOrder.push('openDetail')
    })

    const { initFromRoute } = useCrudRouteDetail({
      route: route as never,
      router: router as never,
      openDetail,
      queryKey: 'id',
      parseId: (raw) => (typeof raw === 'string' && /^\d+$/.test(raw) ? Number(raw) : null),
      rowHighlightClass: 'io-row-highlight',
      onRouteIdChange,
    })

    await initFromRoute()

    expect(callOrder).toEqual(['onRouteIdChange', 'openDetail'])
  })

  it('ignores route query changes while the owning page is deactivated', async () => {
    const route = reactive({
      path: '/warehouse/material-io',
      query: { id: '1' } as Record<string, string | undefined>,
    })
    const onRouteIdChange = vi.fn().mockResolvedValue(undefined)
    const openDetail = vi.fn().mockResolvedValue(undefined)

    const ActivePage = defineComponent({
      setup() {
        const { setupRouteWatch } = useCrudRouteDetail({
          route: route as never,
          router: { replace: vi.fn().mockResolvedValue(undefined) } as never,
          openDetail,
          queryKey: 'id',
          parseId: (raw) => (typeof raw === 'string' && /^\d+$/.test(raw) ? Number(raw) : null),
          rowHighlightClass: 'io-row-highlight',
          onRouteIdChange,
        })
        setupRouteWatch()
        return () => h('div', 'active')
      },
    })

    const InactivePage = defineComponent({
      setup() {
        return () => h('div', 'inactive')
      },
    })

    const current = ref<'active' | 'inactive'>('active')
    const Shell = defineComponent({
      setup() {
        return () =>
          h(
            KeepAlive,
            null,
            {
              default: () => (current.value === 'active' ? h(ActivePage) : h(InactivePage)),
            },
          )
      },
    })

    const container = document.createElement('div')
    const app = createApp(Shell)
    app.mount(container)
    await nextTick()

    onRouteIdChange.mockClear()
    openDetail.mockClear()

    current.value = 'inactive'
    await nextTick()

    route.query.id = '2'
    await nextTick()

    expect(onRouteIdChange).not.toHaveBeenCalled()
    expect(openDetail).not.toHaveBeenCalled()

    app.unmount()
  })
})
