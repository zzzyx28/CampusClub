import { fileURLToPath, URL } from 'node:url'
import process from 'node:process'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())

  const plugins = [vue()]
  if (mode === 'development') {
    plugins.push(vueDevTools())
  }

  return {
    plugins,
    build: {
      // Element Plus is intentionally registered globally; keep the warning
      // threshold aligned with the measured vendor chunk while route views stay lazy.
      chunkSizeWarningLimit: 1200
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
    server: {
      port: parseInt(env.PORT || '5175'),
      open: true,
      proxy: {
        '/api': {
          target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    }
  }
})
