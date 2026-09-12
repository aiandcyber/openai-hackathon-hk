# Android — Scam Shield (Person A / Windows Android Studio)

Package: `com.aiforseniors.scamshield` · Min SDK 26

## Open in Android Studio (Windows)

1. Clone or pull [openai-hackathon-hk](https://github.com/aiandcyber/openai-hackathon-hk)
2. **File → Open** → `...\openai-hackathon\apps\android` (this folder, not the monorepo root)
3. Copy `local.properties.example` → `local.properties`
4. Set `sdk.dir` to your Android SDK path
5. Set `API_BASE_URL`:
   - Emulator: `http://10.0.2.2:3000`
   - USB phone (same Wi‑Fi as PC): `http://YOUR_PC_LAN_IP:3000` (WSL often needs `netsh interface portproxy` or run Next on Windows)
   - After deploy: `https://YOUR_VERCEL_OR_WORKER_URL`
6. Sync Gradle → **Run ▶** on the Samsung phone (`R5CX52J1K6T`)

## Features

| Feature | How |
|---|---|
| WhatsApp → classify → warn | `WhatsAppNotificationListener` → `POST /api/classify-scam` → `WarnActivity` (+ TTS) |
| Call VIP | Load `GET /api/vips` → speech “Call [name]” → `ACTION_CALL` |
| Manual test | Home screen “Test classify” without WhatsApp |

## On-device checklist

1. Settings → **Notification access** → enable **Scam Shield**
2. Grant **Microphone** + **Phone**
3. Add VIPs on caregiver web, then **Load VIP list**
4. Send yourself a scam-like WhatsApp message, or use Test classify
5. Say **Call [VIP name]**

## Build APK

```bash
cd apps/android
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

WSL note: JDK/Android SDK are expected on **Windows** Android Studio for this hackathon; this tree is source-complete for Studio to build.
