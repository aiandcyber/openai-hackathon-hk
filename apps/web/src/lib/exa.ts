import Exa from "exa-js";
import type { ExaEvidence } from "./types";

export async function searchScamEvidence(text: string): Promise<ExaEvidence[]> {
  const apiKey = process.env.EXA_API_KEY;
  if (!apiKey) {
    return [];
  }

  const query = `Hong Kong senior phishing scam OTP bank fraud similar to: ${text.slice(0, 240)}`;
  const exa = new Exa(apiKey);
  const result = await exa.searchAndContents(query, {
    type: "auto",
    numResults: 3,
    text: { maxCharacters: 400 },
  });

  return (result.results ?? []).map((r) => ({
    title: r.title ?? "Untitled",
    url: r.url,
    snippet: (r.text ?? "").slice(0, 280),
  }));
}
