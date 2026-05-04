import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  optimizeDeps: {
    include: ['@azure/msal-react', '@azure/msal-browser'],
  },
  // Ensure the commonjs plugin handles the msal library
  build: {
    commonjsOptions: {
      include: [/node_modules/],
    },
  },
});
