export type ExaEvidence = {
  title: string;
  url: string;
  snippet: string;
};

export type ClassifyResult = {
  isScam: boolean;
  reason: string;
  seniorWarning: string;
  caregiverMessage: string;
  exaEvidence: ExaEvidence[];
  source?: string;
  mode?: "llm+exa" | "rules+exa" | "rules-only";
};

export type Vip = {
  id: string;
  name: string;
  phone: string;
};
