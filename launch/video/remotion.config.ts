/**
 * Note: When using the Node.JS APIs, the config file
 * doesn't apply. Instead, pass options directly to the APIs.
 *
 * All configuration options: https://remotion.dev/docs/config
 */

import { Config } from "@remotion/cli/config";

Config.setRspack(true);
Config.setVideoImageFormat("jpeg");
Config.setOverwriteOutput(true);
import { existsSync } from "node:fs";
// Only use the sandbox browser when it exists; on your own machine Remotion downloads/uses its own.
const SANDBOX_BROWSER = "/opt/pw-browsers/chromium_headless_shell-1194/chrome-linux/headless_shell";
if (existsSync(SANDBOX_BROWSER)) Config.setBrowserExecutable(SANDBOX_BROWSER);
