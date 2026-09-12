import { NextResponse } from "next/server";
import { classifyScam } from "@/lib/classify";
import { addAlert } from "@/lib/store";

export async function POST(req: Request) {
  try {
    const body = (await req.json()) as { text?: string; source?: string };
    const text = body.text ?? "";
    const source = body.source ?? "unknown";
    const result = await classifyScam(text, source);
    if (result.isScam) {
      addAlert({
        at: new Date().toISOString(),
        ...result,
      });
    }
    return NextResponse.json(result);
  } catch (err) {
    console.error(err);
    return NextResponse.json({ error: "classify_failed" }, { status: 500 });
  }
}
