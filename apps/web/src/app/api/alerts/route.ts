import { NextResponse } from "next/server";
import { listAlerts } from "@/lib/store";
import { requireCaregiverSession } from "@/lib/require-auth";

export async function GET() {
  const { error } = await requireCaregiverSession();
  if (error) return error;
  return NextResponse.json({ alerts: listAlerts() });
}
