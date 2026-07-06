import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/layouts/AppLayout.vue')
const LoginView = () => import('@/views/auth/LoginView.vue')

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { public: true, title: '登录', skipTab: true },
  },
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/',
    name: 'AppRoot',
    component: AppLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: 'system/roles',
        redirect: '/system/users/roles',
        meta: { requiresAuth: true, permission: 'system:role:read' },
      },
      {
        path: 'system/menus',
        redirect: '/system/users/menus',
        meta: { requiresAuth: true, permission: 'system:menu:read' },
      },
    ],
  },
]
