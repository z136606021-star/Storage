declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    requiresAuth?: boolean
    permission?: string
    title?: string
    tabClosable?: boolean
    skipTab?: boolean
  }
}

export {}
