import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";
import { getAuth0, isAuth0Configured } from "./lib/auth0";

export async function proxy(request: NextRequest) {
  // Without Auth0 env, pass through so local demo keeps working.
  if (!isAuth0Configured()) {
    return NextResponse.next();
  }

  const auth0 = getAuth0();
  // Handle /auth/login, /auth/logout, /auth/callback.
  // Do not hard-redirect /caregiver: the page has its own Log in button,
  // and a forced login redirect made localhost look like it "failed to load".
  return auth0.middleware(request);
}

export const config = {
  matcher: [
    "/((?!_next/static|_next/image|favicon.ico|sitemap.xml|robots.txt).*)",
  ],
};
