import { NextResponse } from "next/server";
import { listVips, upsertVip } from "@/lib/store";

export async function GET() {
  return NextResponse.json({ vips: listVips() });
}

export async function POST(req: Request) {
  const body = (await req.json()) as { name?: string; phone?: string; id?: string };
  if (!body.name || !body.phone) {
    return NextResponse.json({ error: "name_and_phone_required" }, { status: 400 });
  }
  const vip = upsertVip({ id: body.id, name: body.name, phone: body.phone });
  return NextResponse.json({ vip });
}
