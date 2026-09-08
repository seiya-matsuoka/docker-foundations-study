import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// React / Vite 自体の設定学習が目的ではないため、React Plugin のみを有効にする。
export default defineConfig({
  plugins: [react()],
});
