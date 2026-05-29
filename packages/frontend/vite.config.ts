import { dirname, resolve } from "path";
import { fileURLToPath } from "url";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const __dirname = dirname(fileURLToPath(import.meta.url));
const shimPath = resolve(__dirname, "src/shims/react-aria-ssr.js");

export default defineConfig({
  plugins: [react()],
  resolve: {
    dedupe: ["@react-aria/ssr"],
    alias: {
      "react-aria/SSRProvider": shimPath,
      "react-aria/private/ssr/SSRProvider": shimPath,
    },
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