import Link from "next/link";

export default function Home() {
  return (
    <main className="mx-auto flex min-h-screen max-w-2xl flex-col justify-center gap-4 p-8">
      <h1 className="text-3xl font-semibold">AI for Seniors — Scam Shield</h1>
      <p className="text-zinc-600">
        Hackathon build: WhatsApp/email scam classify + Exa evidence + CopilotKit caregiver
        + VIP voice call (Android).
      </p>
      <Link className="w-fit rounded bg-zinc-900 px-4 py-2 text-white" href="/caregiver">
        Open caregiver console
      </Link>
      <p className="text-sm text-zinc-500">
        Android APK lives under apps/android (needs Android Studio / JDK on the build machine).
      </p>
    </main>
  );
}
