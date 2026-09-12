import { NextResponse } from "next/server";
import { getAuth0, isAuth0Configured } from "./auth0";

/** When Auth0 env is set, require a caregiver session. Otherwise allow (local demo). */
export async function requireCaregiverSession() {
  if (!isAuth0Configured()) {
    return { session: null as null, error: null as NextResponse | null };
  }
  const session = await getAuth0().getSession();
  if (!session) {
    return {
      session: null,
      error: NextResponse.json({ error: "unauthorized" }, { status: 401 }),
    };
  }
  return { session, error: null };
}
