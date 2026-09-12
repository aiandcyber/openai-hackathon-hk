import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";
import { getAuth0, isAuth0Configured } from "./lib/auth0";

export async function proxy(request: NextRequest) {
  // Without Auth0 env, pass through so local demo keeps working.
  if (!isAuth0Configured()) {
    return NextResponse.next();
  }

  const auth0 = getAuth0();
  const authRes = await auth0.middleware(request);

  const { pathname } = request.nextUrl;
  if (pathname.startsWith("/caregiver")) {
    const session = await auth0.getSession(request);
    if (!session) {
      const login = new URL("/auth/login", request.nextUrl.origin);
      login.searchParams.set("returnTo", "/caregiver");
      return NextResponse.redirect(login);
    }
  }

  return authRes;
}

export const config = {
  matcher: [
    "/((?!_next/static|_next/image|favicon.ico|sitemap.xml|robots.txt).*)",
  ],
};
