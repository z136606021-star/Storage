import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { buildMaterialIoShareUrl } from '@/utils/materialIoShareUrl'

describe('buildMaterialIoShareUrl', () => {
  it('builds url with record id and materialLedgerId query', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/warehouse/material-io', component: { template: '<div />' } }],
    })

    const url = buildMaterialIoShareUrl(router, 42, { materialLedgerId: '7' })

    expect(url).toContain('/warehouse/material-io')
    expect(url).toContain('id=42')
    expect(url).toContain('materialLedgerId=7')
  })
})
