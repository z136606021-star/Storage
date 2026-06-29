import type { Router } from 'vue-router'

export function buildMaterialIoShareUrl(
  router: Router,
  recordId: number,
  extraQuery: Record<string, string | string[] | undefined> = {},
): string {
  const query: Record<string, string> = {}
  for (const [key, value] of Object.entries(extraQuery)) {
    if (value == null || value === '') {
      continue
    }
    query[key] = Array.isArray(value) ? value[0] : value
  }
  query.id = String(recordId)

  const resolved = router.resolve({
    path: '/warehouse/material-io',
    query,
  })

  if (typeof window !== 'undefined' && window.location?.origin) {
    return new URL(resolved.href, window.location.origin).href
  }
  return resolved.href
}
