import { NextResponse } from "next/server";
import { listAlerts } from "@/lib/store";

export async function GET() {
  return NextResponse.json({ alerts: listAlerts() });
}
