import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  // npm run dev: the API is the backup service on :8090
  server: { proxy: { '/api': 'http://localhost:8090' } },
})
