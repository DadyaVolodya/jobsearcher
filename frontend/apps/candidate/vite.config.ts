import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'node:url';

export default defineConfig(({ command }) => ({
  plugins: [react()],
  base: process.env.VITE_BASE || (command === 'build' ? '/candidate/' : '/'),
  envDir: fileURLToPath(new URL('../..', import.meta.url)),
  server: {
    headers: { 'Cache-Control': 'no-store, max-age=0' },
    proxy: { '/api': 'http://localhost:8080' },
  },
}));
