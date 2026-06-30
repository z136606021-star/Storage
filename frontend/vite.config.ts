import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'

function parsePort(value: string | undefined, fallback: number, name: string) {
  if (!value) {
    return fallback
  }
  const port = Number(value)
  if (!Number.isInteger(port) || port <= 0) {
    throw new Error(`${name} must be a positive integer, got "${value}"`)
  }
  return port
}

export default defineConfig(({ mode }) => {
  const repoRoot = fileURLToPath(new URL('..', import.meta.url))
  const env = loadEnv(mode, repoRoot, '')
  const backendPort = env.BACKEND_PORT || '8080'
  const frontendPort = parsePort(env.FRONTEND_PORT, 5173, 'FRONTEND_PORT')
  const apiProxy = env.VITE_API_PROXY || `http://localhost:${backendPort}`

  return {
    envDir: repoRoot,
    plugins: [
      vue(),
      Components({
        dts: false,
        resolvers: [
          AntDesignVueResolver({
            importStyle: false,
          }),
        ],
      }),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: frontendPort,
      strictPort: true,
      open: '/login',
      proxy: {
        '/api': {
          target: apiProxy,
          changeOrigin: true,
        },
      },
    },
  }
})
