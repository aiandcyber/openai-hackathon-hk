# AI For Seniors — Scam Shield

OpenAI Global Hackathon Hong Kong · AI Tinkerers **Agents Everywhere**

Android capture/dial + **CopilotKit** caregiver agent + **Auth0** + **Exa** web evidence.

WhatsApp notification text is classified (OpenRouter / OpenAI, with a rule fallback). **Exa** searches the public web for similar scam/phishing patterns. Sources are shown in the caregiver CopilotKit UI. The senior phone warns on scam; the senior can say **“Call Martin”** (or Amir / Tim) to place a cellular call.

**Repo:** [github.com/aiandcyber/openai-hackathon-hk](https://github.com/aiandcyber/openai-hackathon-hk)

| | |
|---|---|
| **Caregiver (hosted)** | _Add Vercel/Cloudflare URL before submit_ — local: http://localhost:3000/caregiver |
| **Debug APK** | _Publish `app-debug.apk` on [GitHub Releases](https://github.com/aiandcyber/openai-hackathon-hk/releases)_ |
| **Demo video** | _Add 2-minute link here_ |
| **Android package** | `com.aiforseniors.scamshield` |
| **Min Android** | 8.0 (API 26) |
| **App name on device** | AI For Seniors |

Judges do not need Auth0 / OpenRouter / Exa accounts if the hosted caregiver URL and sideload APK are provided. Building from source is backup only.

---

## Architecture

```
Senior Android phone
  WhatsApp notification  →  NotificationListener  →  POST /api/classify-scam
  Voice “Call [name]”    →  SpeechRecognizer      →  ACTION_CALL
                            GET /api/vips

Next.js (apps/web)
  OpenRouter (gpt-4o-mini, fallback gpt-4.1-mini) + rule fallback
  Exa search  →  exaEvidence[{ title, url, snippet }]
  CopilotKit Runtime  /api/copilotkit
  Auth0 login on /caregiver
  GET/POST /api/vips · GET /api/alerts
```

**Exa does not read WhatsApp.** The phone supplies the message text. Exa searches the web for similar scam patterns so the caregiver agent can show evidence.

Email ingest (Gmail) and Trigger.dev push alerts are in the plan, not in this build. The live phone path is **WhatsApp → classify → warn + caregiver alert list**.

---

## For evaluators — easiest path

1. Open this GitHub repo → **Releases** → download `app-debug.apk`.
2. On an Android phone: open the APK → allow **Install unknown apps** → Install.
3. Open **AI For Seniors** → Settings (gear) → **Notification access** → turn on **AI For Seniors — WhatsApp protection** (required for WhatsApp detection).
4. Grant **Microphone** and **Phone** when asked (for “Call VIP”).
5. In WhatsApp, turn **message preview** on so notification text is visible.
6. Caregiver web: open the hosted URL in the table above → **Log in** (Auth0) → CopilotKit chat + alerts.

### What to try

| Demo | How |
|---|---|
| WhatsApp scam | Send the demo phone the sample text below → phone warning screen; caregiver **Recent alerts** + Exa links |
| Voice call | Say **“Call Martin”** (or Amir / Tim) → dialer/call starts |
| Caregiver agent | Open `/caregiver` → Auth0 → ask CopilotKit about alerts or trusted people |

**Sample WhatsApp text**

```
Your HSBC account will be closed. Send OTP to verify now.
```

**VIP names on the phone (built-in):** Martin · Amir · Tim

---

## Layout

```
openai-hackathon-hk/
  apps/web/       Next.js API + CopilotKit caregiver UI
  apps/android/   Kotlin senior app (Android Studio / JDK 17)
  README.md
```

Sponsors used: **OpenAI** (via OpenRouter), **OpenRouter**, **Auth0**, **CopilotKit**, **Exa**.

---

## Optional — run caregiver web locally

Needs Node 20+. Do not commit secrets; copy from `apps/web/.env.example`.

```bash
cd apps/web
cp .env.example .env.local
# set OPENROUTER_API_KEY, EXA_API_KEY
# Auth0 (v4): AUTH0_DOMAIN, AUTH0_CLIENT_ID, AUTH0_CLIENT_SECRET, AUTH0_SECRET, APP_BASE_URL
# Auth0 callback: http://localhost:3000/auth/callback
# Auth0 logout:   http://localhost:3000
npm install
npm run dev -- -H 0.0.0.0 -p 3000
```

- http://localhost:3000/
- http://localhost:3000/caregiver

Classify smoke test (rule fallback works without API keys; Exa fills `exaEvidence` when `EXA_API_KEY` is set):

```bash
curl -s http://localhost:3000/api/classify-scam \
  -H 'Content-Type: application/json' \
  -d '{"source":"whatsapp","text":"Your HSBC account will be closed. Send OTP to verify now."}'
```

Expect JSON: `isScam`, `reason`, `seniorWarning`, `caregiverMessage`, `exaEvidence`, `mode` (`llm+exa`, `rules+exa`, or `rules-only`).

---

## Optional — build the APK from source

Android Studio on Windows: **File → Open** → `apps/android` (not the repo root). Copy `local.properties.example` → `local.properties`. Set `sdk.dir` and `API_BASE_URL` to the **live HTTPS API** (not localhost) for a phone that is not USB-reversed.

```bash
cd apps/android
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

USB + local Next.js: `adb reverse tcp:3000 tcp:3000` and `API_BASE_URL=http://127.0.0.1:3000`. Emulator: `http://10.0.2.2:3000`.

On-device: Notification access + WhatsApp message preview, as above.

---

## Team

| | Owns |
|---|---|
| A | Android: NotificationListener, warn UI, dial, speech |
| B | API: CopilotKit runtime, classify, Exa, OpenRouter |
| C | Caregiver UI, Auth0, README, demo video |
