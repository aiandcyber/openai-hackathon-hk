import { Auth0Client } from "@auth0/nextjs-auth0/server";

export function isAuth0Configured(): boolean {
  return Boolean(
    process.env.AUTH0_DOMAIN &&
      process.env.AUTH0_CLIENT_ID &&
      process.env.AUTH0_CLIENT_SECRET &&
      process.env.AUTH0_SECRET,
  );
}

let client: Auth0Client | null = null;

export function getAuth0(): Auth0Client {
  if (!isAuth0Configured()) {
    throw new Error("Auth0 is not configured");
  }
  if (!client) {
    client = new Auth0Client({
      appBaseUrl:
        process.env.APP_BASE_URL || process.env.AUTH0_BASE_URL || "http://localhost:3000",
    });
  }
  return client;
}
