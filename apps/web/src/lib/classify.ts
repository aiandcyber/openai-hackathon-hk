import OpenAI from "openai";
import { ruleBasedScamScore } from "./scam-rules";
import { searchScamEvidence } from "./exa";
import type { ClassifyResult } from "./types";

function openRouterClient() {
  const apiKey = process.env.OPENROUTER_API_KEY;
  if (!apiKey) return null;
  return new OpenAI({
    apiKey,
    baseURL: process.env.OPENROUTER_BASE_URL || "https://openrouter.ai/api/v1",
  });
}

async function llmClassify(text: string, source: string): Promise<Omit<ClassifyResult, "exaEvidence" | "source" | "mode"> | null> {
  const client = openRouterClient();
  if (!client) return null;

  const primary = process.env.OPENROUTER_MODEL || "openai/gpt-4o-mini";
  const fallback = process.env.OPENROUTER_FALLBACK_MODEL || "openai/gpt-4.1-mini";
  const models = [primary, fallback];

  const system = `You are Scam Shield for Hong Kong seniors.
Decide if a WhatsApp/email/SMS message is a scam.
Respond ONLY with JSON:
{"isScam":boolean,"reason":string,"seniorWarning":string,"caregiverMessage":string}
seniorWarning: short calm spoken warning for the senior.
caregiverMessage: one SMS-style alert for family.`;

  let lastError: unknown;
  for (const model of models) {
    try {
      const completion = await client.chat.completions.create({
        model,
        temperature: 0,
        response_format: { type: "json_object" },
        messages: [
          { role: "system", content: system },
          { role: "user", content: `source=${source}\n\n${text}` },
        ],
      });
      const raw = completion.choices[0]?.message?.content ?? "{}";
      const parsed = JSON.parse(raw) as {
        isScam?: boolean;
        reason?: string;
        seniorWarning?: string;
        caregiverMessage?: string;
      };
      return {
        isScam: Boolean(parsed.isScam),
        reason: parsed.reason ?? "No reason provided",
        seniorWarning:
          parsed.seniorWarning ??
          (parsed.isScam
            ? "This message looks unsafe. Do not share codes or tap links. Call your family."
            : "This message looks okay, but stay careful."),
        caregiverMessage:
          parsed.caregiverMessage ??
          (parsed.isScam ? `Possible scam (${source}): ${text.slice(0, 120)}` : "Message reviewed — not flagged."),
      };
    } catch (err) {
      lastError = err;
    }
  }
  console.error("OpenRouter classify failed", lastError);
  return null;
}

export async function classifyScam(text: string, source = "unknown"): Promise<ClassifyResult> {
  const trimmed = text.trim();
  if (!trimmed) {
    return {
      isScam: false,
      reason: "Empty message",
      seniorWarning: "",
      caregiverMessage: "",
      exaEvidence: [],
      source,
      mode: "rules-only",
    };
  }

  const llm = await llmClassify(trimmed, source);
  const rules = ruleBasedScamScore(trimmed);
  const base = llm ?? {
    isScam: rules.isScam,
    reason: rules.reason,
    seniorWarning: rules.isScam
      ? "This message looks unsafe. Do not share codes or tap links. Call your family."
      : "This message looks okay, but stay careful.",
    caregiverMessage: rules.isScam
      ? `Possible scam (${source}): ${trimmed.slice(0, 120)}`
      : "Message reviewed — not flagged.",
  };

  let exaEvidence: ClassifyResult["exaEvidence"] = [];
  try {
    if (base.isScam || rules.isScam) {
      exaEvidence = await searchScamEvidence(trimmed);
    }
  } catch (err) {
    console.error("Exa search failed", err);
  }

  return {
    ...base,
    exaEvidence,
    source,
    mode: llm ? (exaEvidence.length ? "llm+exa" : "rules+exa") : exaEvidence.length ? "rules+exa" : "rules-only",
  };
}
