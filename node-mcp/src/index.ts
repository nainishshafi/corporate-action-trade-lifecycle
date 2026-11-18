import { fileURLToPath } from "node:url";
import { startServer } from "./bootstrap/createServer.js";

/**
 * Start Server
 */
if (process.argv[1] === fileURLToPath(import.meta.url)) {
  startServer().catch((error) => {
    console.error("Server error:", error);
    process.exit(1);
  });
}
