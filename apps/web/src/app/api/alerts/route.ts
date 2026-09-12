import { NextResponse } from "next/server";
import { listAlerts } from "@/lib/store";

export async function GET() {
  // Readable without login so the demo console shows phone-detected alerts.
  return NextResponse.json({ alerts: listAlerts() });
}
