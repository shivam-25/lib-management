import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

// https://vitejs.dev/config/
export default ({ mode }: { mode: string }) => {
  process.env = Object.assign(process.env, loadEnv(mode, process.cwd(), ''));

  return defineConfig({
    plugins: [react()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src')
      }
    },
    server: {
      host: true,
      port: 5173,
      open: '/app',
      // Allow the *.app.github.dev (Codespaces) and other proxy hosts to reach the dev server.
      allowedHosts: true,
      // Single-origin proxy: when VITE_API_URL is empty the app calls relative
      // /api/v1, and these requests are forwarded to the backend on the same
      // origin — so auth cookies (incl. the JS-readable csrfToken) just work.
      proxy: {
        // The PDF report endpoint is served by the Java service (:5008), not the
        // Node backend — route it there before the catch-all /api rule below.
        '^/api/v1/students/[^/]+/report': {
          target: process.env.REPORT_PROXY_TARGET || 'http://localhost:5008',
          changeOrigin: true,
          secure: false
        },
        '/api': {
          target: process.env.BACKEND_PROXY_TARGET || 'http://localhost:5007',
          changeOrigin: true,
          secure: false
        }
      }
    }
  });
};
