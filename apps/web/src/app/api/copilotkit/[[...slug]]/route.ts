import {
  BuiltInAgent,
  CopilotRuntime,
  createCopilotRuntimeHandler,
} from "@copilotkit/runtime/v2";

/**
 * CopilotKit Runtime — required agent endpoint.
 * Prefer OpenRouter by setting OPENROUTER_API_KEY (mapped to OpenAI-compatible env for BuiltInAgent).
 */
function resolveOpenAICompatEnv() {
  if (process.env.OPENROUTER_API_KEY) {
    process.env.OPENAI_API_KEY = process.env.OPENROUTER_API_KEY;
    process.env.OPENAI_BASE_URL =
      process.env.OPENROUTER_BASE_URL || "https://openrouter.ai/api/v1";
  }
}

resolveOpenAICompatEnv();

const model =
  process.env.OPENROUTER_MODEL ||
  process.env.COPILOTKIT_MODEL ||
  "openai/gpt-4.1-mini";

const runtime = new CopilotRuntime({
  agents: {
    default: new BuiltInAgent({
      model,
      prompt: `You are the caregiver assistant for AI for Seniors / Scam Shield.
Help family review scam alerts from WhatsApp/email, explain Exa evidence, and manage VIP call list.
Be concise and practical.`,
    }),
  },
});

const handler = createCopilotRuntimeHandler({
  runtime,
  basePath: "/api/copilotkit",
});

export const GET = handler;
export const POST = handler;
export const OPTIONS = handler;
