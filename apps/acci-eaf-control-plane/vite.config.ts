import { resolve } from 'path';
import { defineConfig } from 'vite';

export default defineConfig({
  // Development server configuration
  server: {
    port: 3000,
    proxy: {
      // Proxy API calls to Spring Boot backend
      '/connect': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
    open: false, // Don't auto-open browser (Spring Boot handles this)
  },

  // Build configuration
  build: {
    outDir: 'target/frontend',
    emptyOutDir: true,
    sourcemap: true,
    rollupOptions: {
      input: {
        main: resolve(__dirname, 'src/main/frontend/index.html'),
      },
    },
  },

  // Development configuration
  define: {
    'process.env.NODE_ENV': JSON.stringify(
      process.env.NODE_ENV || 'development'
    ),
  },

  // Plugin configuration (Hilla will add its own plugins)
  plugins: [],

  // CSS configuration
  css: {
    devSourcemap: true,
  },

  // Resolve configuration
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src/main/frontend'),
    },
  },

  // Optimize dependencies
  optimizeDeps: {
    include: ['lit', '@vaadin/router', '@vaadin/flow-frontend'],
  },
});
