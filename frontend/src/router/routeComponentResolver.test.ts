import { describe, expect, it } from 'vitest'
import { normalizeRouteComponentKey, resolveRouteComponent } from '@/router/routeComponentResolver'

describe('route component resolver', () => {
  it('normalizes menu-managed component module paths', () => {
    expect(normalizeRouteComponentKey('views/warehouse/MaterialLedgerView.vue')).toBe(
      '../views/warehouse/MaterialLedgerView.vue',
    )
    expect(normalizeRouteComponentKey('@/components/system/RoleManagePanel')).toBe(
      '../components/system/RoleManagePanel.vue',
    )
    expect(normalizeRouteComponentKey('src/views/system/SystemManageLayout.vue')).toBe(
      '../views/system/SystemManageLayout.vue',
    )
  })

  it('resolves existing views and rejects non-menu module keys', () => {
    expect(resolveRouteComponent('views/warehouse/MaterialLedgerView.vue')).toEqual(expect.any(Function))
    expect(resolveRouteComponent('MaterialLedger')).toBeNull()
    expect(resolveRouteComponent('views/warehouse/UnknownView.vue')).toBeNull()
  })
})
