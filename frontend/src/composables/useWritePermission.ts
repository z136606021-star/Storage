import { computed } from 'vue'
import { useAuth } from '@/composables/useAuth'

export function useWritePermission(permission: string) {
  const auth = useAuth()
  const canWrite = computed(() => auth.hasPermission(permission))
  return { canWrite }
}
