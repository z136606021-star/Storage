<script setup lang="ts">
import { h, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  AppstoreOutlined,
  BookOutlined,
  BulbOutlined,
  DatabaseOutlined,
  HomeOutlined,
  ProjectOutlined,
  SettingOutlined,
  ShoppingOutlined,
  ToolOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

const route = useRoute()
const router = useRouter()

const openKeys = ref(['resource', 'procurement', 'warehouse', 'warehouse-config', 'skill'])
const selectedKeys = ref<string[]>(['material-ledger'])

function syncSelectedKeys() {
  if (route.name === 'MaterialLedger') {
    selectedKeys.value = ['material-ledger']
  }
}

watch(() => route.name, syncSelectedKeys, { immediate: true })

function handleMenuClick({ key }: { key: string }) {
  if (key === 'material-ledger') {
    router.push({ name: 'MaterialLedger' })
    return
  }
  message.info('功能开发中，敬请期待')
}

const menuItems = [
  {
    key: 'personal',
    icon: () => h(HomeOutlined),
    label: '个人中心',
  },
  {
    key: 'project',
    icon: () => h(ProjectOutlined),
    label: '项目管理中心',
    children: [
      { key: 'new-project', label: '新建项目' },
      { key: 'project-set', label: '项目集' },
    ],
  },
  {
    key: 'resource',
    icon: () => h(DatabaseOutlined),
    label: '资源管理',
    children: [
      {
        key: 'procurement',
        label: '采购管理',
        children: [
          { key: 'procurement-list', label: '采购管理' },
          { key: 'procurement-add', label: '新增采购需求' },
          { key: 'procurement-mine', label: '我的采购需求' },
        ],
      },
      {
        key: 'warehouse',
        label: '仓库管理',
        children: [
          { key: 'material-ledger', label: '物料台账' },
          { key: 'material-io', label: '物料出入库' },
          { key: 'safety-stock', label: '安全库存管理' },
        ],
      },
      {
        key: 'warehouse-config',
        label: '配置管理',
        children: [
          { key: 'bin-mgmt', label: 'Bin位管理' },
          { key: 'bom-mgmt', label: '物料清单管理' },
        ],
      },
    ],
  },
  {
    key: 'design',
    icon: () => h(BulbOutlined),
    label: '设计指引',
  },
  {
    key: 'skill',
    icon: () => h(ToolOutlined),
    label: '技能中心',
    children: [
      { key: 'skill-matrix', label: '技能矩阵' },
      { key: 'talent-profile', label: '人才画像' },
      { key: 'training-plan', label: '人才培训计划' },
    ],
  },
  {
    key: 'experience',
    icon: () => h(BookOutlined),
    label: '经验库',
  },
  {
    key: 'finance',
    icon: () => h(ShoppingOutlined),
    label: '财务中心',
    children: [
      { key: 'finance-dashboard', label: '业务分析看板' },
      { key: 'finance-settlement', label: '财务结算中心' },
      { key: 'finance-cost', label: '成本分析中心' },
    ],
  },
  {
    key: 'system',
    icon: () => h(SettingOutlined),
    label: '系统管理',
    children: [
      { key: 'user-mgmt', label: '用户管理' },
      { key: 'customer-mgmt', label: '客户管理' },
    ],
  },
]
</script>

<template>
  <div class="side-menu">
    <div class="logo">
      <AppstoreOutlined class="logo-icon" />
      <span class="logo-text">项目管理平台</span>
    </div>
    <a-menu
      v-model:open-keys="openKeys"
      v-model:selected-keys="selectedKeys"
      mode="inline"
      :items="menuItems"
      class="menu"
      @click="handleMenuClick"
    />
  </div>
</template>

<style scoped>
.side-menu {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #f0f0f0;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.logo-icon {
  font-size: 22px;
  color: #1677ff;
}

.logo-text {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  white-space: nowrap;
}

.menu {
  flex: 1;
  overflow: auto;
  border-inline-end: none !important;
}

.menu :deep(.ant-menu-item-selected) {
  background-color: #1677ff !important;
  color: #fff !important;
}

.menu :deep(.ant-menu-item-selected .ant-menu-title-content) {
  color: #fff !important;
}
</style>
