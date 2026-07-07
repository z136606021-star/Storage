import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { buildMaterialIoShareUrl } from '@/utils/materialIoShareUrl'

describe('buildMaterialIoShareUrl', () => {
  it('builds url with record id and materialLedgerId query', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/configured-io-route', component: { template: '<div />' } }],
    })

    const url = buildMaterialIoShareUrl(router, '/configured-io-route', 42, { materialLedgerId: '7' })

    expect(url).toContain('/configured-io-route')
    expect(url).toContain('id=42')
    expect(url).toContain('materialLedgerId=7')
  })
})
