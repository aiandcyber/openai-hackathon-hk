"use client";

import { useUser } from "@auth0/nextjs-auth0/client";
import { CopilotKit } from "@copilotkit/react-core";
import { CopilotChat } from "@copilotkit/react-ui";
import "@copilotkit/react-ui/styles.css";
import Image from "next/image";
import { useEffect, useState } from "react";

type Vip = { id: string; name: string; phone: string };
type Alert = {
  at?: string;
  isScam?: boolean;
  reason?: string;
  caregiverMessage?: string;
  exaEvidence?: { title: string; url: string; snippet: string }[];
};

export default function CaregiverPage() {
  const { user, isLoading, error } = useUser();
  const [vips, setVips] = useState<Vip[]>([]);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [testText, setTestText] = useState(
    "Your HSBC account will be closed. Send OTP to verify now.",
  );
  const [lastClassify, setLastClassify] = useState<Alert | null>(null);

  async function refresh() {
    const [v, a] = await Promise.all([
      fetch("/api/vips").then((r) => r.json()),
      fetch("/api/alerts").then((r) => r.json()),
    ]);
    setVips(v.vips ?? []);
    setAlerts(a.alerts ?? []);
  }

  useEffect(() => {
    void refresh();
  }, []);

  async function addVip(e: React.FormEvent) {
    e.preventDefault();
    await fetch("/api/vips", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, phone }),
    });
    setName("");
    setPhone("");
    await refresh();
  }

  async function runClassify() {
    const res = await fetch("/api/classify-scam", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ source: "whatsapp", text: testText }),
    });
    const data = await res.json();
    setLastClassify(data);
    await refresh();
  }

  const loggedIn = Boolean(user) && !error;

  return (
    <CopilotKit runtimeUrl="/api/copilotkit">
      <div className="caregiver-shell min-h-screen">
        <header className="caregiver-header">
          <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-6 py-5">
            <div className="flex items-center gap-4">
              <Image
                src="/ai-for-seniors-logo.png"
                alt="AI For Seniors — elder and caregiver"
                width={72}
                height={72}
                className="rounded-2xl shadow-sm ring-1 ring-black/5"
                priority
              />
              <div>
                <p className="text-sm font-semibold tracking-wide text-[#0E7490]">
                  AI For Seniors
                </p>
                <h1 className="font-[family-name:var(--font-display)] text-3xl font-semibold text-[#0F2C4C]">
                  Caregiver Console
                </h1>
                <p className="mt-0.5 text-sm text-slate-600">
                  Protect messages · Manage family contacts · Review alerts
                </p>
              </div>
            </div>

            <div className="flex items-center gap-3">
              {isLoading ? (
                <span className="text-sm text-slate-500">Checking sign-in…</span>
              ) : loggedIn ? (
                <>
                  <div className="hidden text-right sm:block">
                    <p className="text-sm font-medium text-[#0F2C4C]">
                      {user?.name || user?.email || "Caregiver"}
                    </p>
                    <p className="text-xs text-slate-500">Signed in</p>
                  </div>
                  <a className="btn-secondary" href="/auth/logout">
                    Log out
                  </a>
                </>
              ) : (
                <a className="btn-primary" href="/auth/login?returnTo=/caregiver">
                  Log in
                </a>
              )}
            </div>
          </div>
        </header>

        <main className="mx-auto flex max-w-6xl flex-col gap-6 px-6 py-8">
          <section className="grid gap-6 lg:grid-cols-2">
            <article className="panel">
              <div className="mb-4 flex items-end justify-between gap-2">
                <div>
                  <h2 className="panel-title">Family contacts</h2>
                  <p className="panel-sub">Shown on the senior’s phone for one-tap calling</p>
                </div>
                <span className="badge">{vips.length} saved</span>
              </div>
              <ul className="mb-4 space-y-2">
                {vips.length === 0 && (
                  <li className="rounded-xl bg-slate-50 px-4 py-3 text-sm text-slate-500">
                    No contacts yet — add someone below.
                  </li>
                )}
                {vips.map((v) => (
                  <li
                    key={v.id}
                    className="flex items-center justify-between rounded-xl border border-slate-100 bg-white px-4 py-3"
                  >
                    <span className="font-medium text-[#0F2C4C]">{v.name}</span>
                    <span className="font-mono text-sm text-slate-600">{v.phone}</span>
                  </li>
                ))}
              </ul>
              <form onSubmit={addVip} className="grid gap-3 sm:grid-cols-[1fr_1fr_auto]">
                <input
                  className="field"
                  placeholder="Name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                />
                <input
                  className="field"
                  placeholder="Phone"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  required
                />
                <button className="btn-primary" type="submit">
                  Add
                </button>
              </form>
            </article>

            <article className="panel">
              <div className="mb-4">
                <h2 className="panel-title">Message safety check</h2>
                <p className="panel-sub">Paste a WhatsApp or email message to test</p>
              </div>
              <textarea
                className="field mb-3 min-h-28"
                value={testText}
                onChange={(e) => setTestText(e.target.value)}
              />
              <button className="btn-accent" type="button" onClick={() => void runClassify()}>
                Check message
              </button>
              {lastClassify && (
                <div
                  className={`mt-4 rounded-xl border px-4 py-3 text-sm ${
                    lastClassify.isScam
                      ? "border-rose-200 bg-rose-50 text-rose-900"
                      : "border-emerald-200 bg-emerald-50 text-emerald-900"
                  }`}
                >
                  <p className="font-semibold">
                    {lastClassify.isScam ? "Flagged as unsafe" : "Looks okay"}
                  </p>
                  <p className="mt-1 opacity-90">{lastClassify.reason}</p>
                  {lastClassify.caregiverMessage && (
                    <p className="mt-2 text-xs opacity-80">{lastClassify.caregiverMessage}</p>
                  )}
                </div>
              )}
            </article>
          </section>

          <section className="panel">
            <div className="mb-4 flex items-end justify-between">
              <div>
                <h2 className="panel-title">Recent alerts</h2>
                <p className="panel-sub">Flags from the senior’s phone and message checks</p>
              </div>
            </div>
            <ul className="space-y-3">
              {alerts.length === 0 && (
                <li className="rounded-xl bg-slate-50 px-4 py-6 text-center text-sm text-slate-500">
                  No alerts yet. When a risky message is detected, it will appear here.
                </li>
              )}
              {alerts.map((a, i) => (
                <li
                  key={i}
                  className="rounded-2xl border border-rose-100 bg-gradient-to-br from-rose-50 to-white p-4"
                >
                  <div className="font-semibold text-rose-950">{a.caregiverMessage}</div>
                  <div className="mt-1 text-sm text-slate-600">{a.reason}</div>
                  {a.exaEvidence && a.exaEvidence.length > 0 && (
                    <ul className="mt-3 space-y-1 border-t border-rose-100 pt-3 text-sm">
                      {a.exaEvidence.map((e) => (
                        <li key={e.url}>
                          <a
                            className="font-medium text-[#0E7490] underline-offset-2 hover:underline"
                            href={e.url}
                            target="_blank"
                            rel="noreferrer"
                          >
                            {e.title}
                          </a>
                        </li>
                      ))}
                    </ul>
                  )}
                </li>
              ))}
            </ul>
          </section>

          <section className="panel overflow-hidden p-0">
            <div className="border-b border-slate-100 px-5 py-4">
              <h2 className="panel-title">Caregiver assistant</h2>
              <p className="panel-sub">Ask about alerts, evidence, or family contacts</p>
            </div>
            <div className="min-h-[420px] p-2">
              <CopilotChat
                labels={{
                  title: "AI For Seniors",
                  initial: "Ask about alerts, scam evidence, or family contacts.",
                }}
              />
            </div>
          </section>
        </main>
      </div>
    </CopilotKit>
  );
}
