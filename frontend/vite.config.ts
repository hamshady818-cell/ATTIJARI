import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    // Découpage des vendors pour éviter le warning "chunk > 500 kB"
    // Vite 8 utilise Rolldown : manualChunks doit être une fonction
    chunkSizeWarningLimit: 600,
    rolldownOptions: {
      output: {
        manualChunks(id: string) {
          if (id.includes('node_modules/react') || id.includes('node_modules/react-dom')) {
            return 'vendor-react'
          }
          if (id.includes('node_modules/react-router-dom')) {
            return 'vendor-router'
          }
          if (id.includes('node_modules/@tanstack/react-query')) {
            return 'vendor-query'
          }
          if (
            id.includes('node_modules/react-hook-form') ||
            id.includes('node_modules/@hookform') ||
            id.includes('node_modules/zod')
          ) {
            return 'vendor-form'
          }
          if (id.includes('node_modules/docx-preview')) {
            return 'vendor-docx'
          }
          if (id.includes('node_modules/xlsx')) {
            return 'vendor-xlsx'
          }
          if (id.includes('node_modules/keycloak-js')) {
            return 'vendor-keycloak'
          }
        },
      },
    },
  },
})


