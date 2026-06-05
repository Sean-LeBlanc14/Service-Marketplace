import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";
import path from "node:path";

const rootDir = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  ".."
);
const backendDir = path.join(
  rootDir,
  "packages",
  "backend",
  "service-marketplace"
);
const command =
  process.platform === "win32" ? "mvnw.cmd" : "sh";
const args =
  process.platform === "win32"
    ? ["clean", "verify"]
    : ["./mvnw", "clean", "verify"];

const child = spawn(command, args, {
  cwd: backendDir,
  stdio: "inherit",
  shell: process.platform === "win32"
});

child.on("exit", (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal);
  }

  process.exit(code ?? 1);
});
