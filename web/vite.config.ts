import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  base: '/podpirate/',
  server: {
    port: 3000,
    proxy: {
      '/podpirate/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/podpirate/, ''),
      },
    },
  },
})
