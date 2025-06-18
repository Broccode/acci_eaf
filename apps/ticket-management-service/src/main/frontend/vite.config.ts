import { defineConfig } from 'vite';

export default defineConfig({
  // Vite configuration for Hilla/Vaadin
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: '../resources/static',
    emptyOutDir: true,
  },
});
