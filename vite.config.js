import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    // Proxy API calls to the Spring Boot backend (application.properties: server.port=8050)
    // so the browser can use relative "/api/..." URLs with no CORS concerns in dev.
    proxy: {
      "/api": {
        target: "http://localhost:8050",
        changeOrigin: true,
      },
    },
  },
});
