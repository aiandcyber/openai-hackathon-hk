const SCAM_PATTERNS: RegExp[] = [
  /\botp\b/i,
  /\bone[-\s]?time\s+password\b/i,
  /\bverify\s+(your\s+)?account\b/i,
  /\baccount\s+(will\s+be\s+)?(closed|locked|suspended)\b/i,
  /\bsend\s+money\b/i,
  /\btransfer\b.*\bbank\b/i,
  /\bpolice\b.*\bfine\b/i,
  /\birs\b|\btax\s+office\b/i,
  /\bclick\s+(this|the)\s+link\b/i,
  /\burgent\b.*\b(bank|account|payment)\b/i,
];

export function ruleBasedScamScore(text: string): { isScam: boolean; reason: string } {
  const hits = SCAM_PATTERNS.filter((re) => re.test(text)).map((re) => re.source);
  if (hits.length === 0) {
    return { isScam: false, reason: "No common scam keywords detected." };
  }
  return {
    isScam: true,
    reason: `Matched scam-like patterns: ${hits.slice(0, 4).join(", ")}`,
  };
}
