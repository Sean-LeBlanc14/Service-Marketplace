import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  resolve: {
    dedupe: ["@react-aria/ssr"]
  },
  optimizeDeps: {
    include: []
  },
  build: {
    commonjsOptions: {
      include: [/node_modules/]
    }
  }
});
