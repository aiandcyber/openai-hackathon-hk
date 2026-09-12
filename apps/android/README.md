# Android app (Person A)

JDK 17+ and Android Studio are required on the machine that builds the APK.

This environment check (2026-09-12): `java` was not installed in the WSL/dev sandbox, so the Kotlin project was not generated here.

## Create project

1. Android Studio → New Project → Empty Activity (Kotlin), Min SDK 26
2. Save/open this folder as the Android module root (or create under here)
3. Implement:
   - `NotificationListenerService` for `com.whatsapp`
   - POST message text to `https://YOUR_VERCEL_URL/api/classify-scam`
   - Warn UI when `isScam`
   - Speech → match VIP from `GET /api/vips` → `ACTION_CALL`

## Build APK

```bash
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```
