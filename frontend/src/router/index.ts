import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'
import MaterialLedgerView from '@/views/material-ledger/MaterialLedgerView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: AppLayout,
      redirect: '/warehouse/material-ledger',
      children: [
        {
          path: 'warehouse/material-ledger',
          name: 'MaterialLedger',
          component: MaterialLedgerView,
          meta: { title: '物料台账' },
        },
      ],
    },
  ],
})

export default router
