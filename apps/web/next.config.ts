import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Allow the WSL IP (and Windows localhost) to load Next.js dev assets.
  allowedDevOrigins: ["127.0.0.1", "localhost", "172.17.100.251", "10.151.42.71"],
};

export default nextConfig;
