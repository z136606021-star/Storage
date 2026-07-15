import { ref } from 'vue'
import { message } from 'ant-design-vue'

export function useCrudDetailDrawer<T>(
  fetchById: (id: number) => Promise<T>,
  loadErrorMessage = '加载详情失败',
) {
  const drawerOpen = ref(false)
  const detailLoading = ref(false)
  const detailRecord = ref<T | null>(null)

  async function openDetail(recordOrId: { id: number } | number) {
    const id = typeof recordOrId === 'number' ? recordOrId : recordOrId.id
    drawerOpen.value = true
    detailLoading.value = true
    detailRecord.value = null
    try {
      detailRecord.value = await fetchById(id)
    } catch {
      message.error(loadErrorMessage)
      drawerOpen.value = false
    } finally {
      detailLoading.value = false
    }
  }

  function closeDetail() {
    drawerOpen.value = false
    detailRecord.value = null
  }

  return {
    drawerOpen,
    detailLoading,
    detailRecord,
    openDetail,
    closeDetail,
  }
}
