import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  const baseApi = env.VITE_APP_BASE_API || 'http://localhost:8080/api'
  const apiPath = baseApi.substring(baseApi.lastIndexOf('/'))
  const targetServer = baseApi.substring(0, baseApi.lastIndexOf('/'))

  const plugins = [vue()]
  if (mode === 'development') {
    plugins.push(vueDevTools())
  }

  return {
    plugins,
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
    server: {
      port: parseInt(env.PORT || '5173'),
      open: true,
      proxy: {
        [apiPath]: {
          target: targetServer,
          changeOrigin: true,
          rewrite: (path) => path.replace(new RegExp(`^${apiPath}`), '')
        }
      }
    }
  }
})
