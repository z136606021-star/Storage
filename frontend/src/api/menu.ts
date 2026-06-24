import { http } from '@/api/http'
import type { NavMenuNode } from '@/types/system'

export function fetchNavTree() {
  return http.get<NavMenuNode[]>('/menus/nav-tree')
}
