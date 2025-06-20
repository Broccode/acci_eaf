import { defineConfig } from 'vite';
import devtoolsJson from 'vite-plugin-devtools-json';

export default defineConfig({
  plugins: [
    devtoolsJson({
      uuid: 'ticket-management-service-dev-workspace',
    }),
  ],
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
