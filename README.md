# AI for Seniors — Scam Shield (hackathon)

Monorepo starter for AI Tinkerers: Android capture/dial + Next.js caregiver with **CopilotKit** + **Exa** + OpenRouter.

## Layout

- `apps/web` — Next.js API + caregiver CopilotKit UI
- `apps/android` — Kotlin app (create with Android Studio; JDK required)

## Quick start (web)

```bash
cd apps/web
cp .env.example .env.local
# fill OPENROUTER_API_KEY and EXA_API_KEY (and Auth0/Gmail when ready)
npm install
npm run dev
```

Open:

- http://localhost:3000/
- http://localhost:3000/caregiver

Smoke test classify (works with rule fallback even without API keys):

```bash
curl -s http://localhost:3000/api/classify-scam \
  -H 'Content-Type: application/json' \
  -d '{"source":"whatsapp","text":"Your HSBC account will be closed. Send OTP to verify now."}'
```

With `EXA_API_KEY`, response includes `exaEvidence`.

## Evaluator notes (expand before submit)

- Publish debug APK on GitHub Releases
- Host caregiver on Vercel; put URL here
- Demo video link here

## Team

- A: Android
- B: API / CopilotKit runtime / Exa / Gmail
- C: Caregiver UI / Auth0 / README / video
