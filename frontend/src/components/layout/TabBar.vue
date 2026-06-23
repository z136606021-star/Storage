<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { CloseOutlined } from '@ant-design/icons-vue'

const route = useRoute()

const tabs = [
  { key: 'personal', title: '个人中心', closable: true },
  { key: 'project', title: '项目中心', closable: true },
  { key: 'material-ledger', title: '物料台账', closable: true },
]

const activeKey = computed(() => {
  if (route.name === 'MaterialLedger') {
    return 'material-ledger'
  }
  return 'material-ledger'
})
</script>

<template>
  <div class="tab-bar">
    <a-tabs
      :active-key="activeKey"
      type="card"
      size="small"
      :tab-bar-gutter="4"
      class="tabs"
    >
      <a-tab-pane v-for="tab in tabs" :key="tab.key">
        <template #tab>
          <span class="tab-label">
            {{ tab.title }}
            <CloseOutlined v-if="tab.closable" class="tab-close" />
          </span>
        </template>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<style scoped>
.tab-bar {
  background: #fff;
  padding: 8px 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.tabs :deep(.ant-tabs-nav) {
  margin-bottom: 0;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tab-close {
  font-size: 10px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
