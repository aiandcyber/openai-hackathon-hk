"use client";

import { CopilotKit } from "@copilotkit/react-core";
import { CopilotChat } from "@copilotkit/react-ui";
import "@copilotkit/react-ui/styles.css";
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

  return (
    <CopilotKit runtimeUrl="/api/copilotkit">
      <main className="mx-auto flex min-h-screen max-w-6xl flex-col gap-6 p-6">
        <header className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="text-2xl font-semibold">Caregiver console</h1>
            <p className="text-sm text-zinc-600">
              CopilotKit agent UI + Exa evidence. Auth0 protects this page when
              AUTH0_DOMAIN / CLIENT_ID / CLIENT_SECRET are set.
            </p>
          </div>
          <div className="flex gap-2 text-sm">
            <a className="rounded border px-3 py-1.5" href="/auth/login?returnTo=/caregiver">
              Log in
            </a>
            <a className="rounded border px-3 py-1.5" href="/auth/logout">
              Log out
            </a>
          </div>
        </header>

        <section className="grid gap-6 md:grid-cols-2">
          <div className="rounded-xl border border-zinc-200 p-4">
            <h2 className="mb-2 font-medium">VIP list</h2>
            <ul className="mb-3 space-y-1 text-sm">
              {vips.map((v) => (
                <li key={v.id}>
                  {v.name} — {v.phone}
                </li>
              ))}
            </ul>
            <form onSubmit={addVip} className="flex flex-col gap-2">
              <input
                className="rounded border px-2 py-1"
                placeholder="Name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
              <input
                className="rounded border px-2 py-1"
                placeholder="Phone"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                required
              />
              <button className="rounded bg-zinc-900 px-3 py-1.5 text-white" type="submit">
                Add VIP
              </button>
            </form>
          </div>

          <div className="rounded-xl border border-zinc-200 p-4">
            <h2 className="mb-2 font-medium">Test classify (WhatsApp/email text)</h2>
            <textarea
              className="mb-2 h-28 w-full rounded border p-2 text-sm"
              value={testText}
              onChange={(e) => setTestText(e.target.value)}
            />
            <button
              className="rounded bg-amber-600 px-3 py-1.5 text-white"
              type="button"
              onClick={() => void runClassify()}
            >
              Run /api/classify-scam
            </button>
            {lastClassify && (
              <pre className="mt-3 max-h-48 overflow-auto rounded bg-zinc-50 p-2 text-xs">
                {JSON.stringify(lastClassify, null, 2)}
              </pre>
            )}
          </div>
        </section>

        <section className="rounded-xl border border-zinc-200 p-4">
          <h2 className="mb-2 font-medium">Recent alerts</h2>
          <ul className="space-y-2 text-sm">
            {alerts.length === 0 && <li className="text-zinc-500">No alerts yet.</li>}
            {alerts.map((a, i) => (
              <li key={i} className="rounded border border-red-100 bg-red-50 p-2">
                <div className="font-medium">{a.caregiverMessage}</div>
                <div className="text-zinc-600">{a.reason}</div>
                {a.exaEvidence && a.exaEvidence.length > 0 && (
                  <ul className="mt-1 list-disc pl-5">
                    {a.exaEvidence.map((e) => (
                      <li key={e.url}>
                        <a className="text-blue-700 underline" href={e.url} target="_blank" rel="noreferrer">
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

        <section className="min-h-[420px] rounded-xl border border-zinc-200 p-2">
          <h2 className="mb-2 px-2 font-medium">CopilotKit agent</h2>
          <CopilotChat
            labels={{
              title: "Scam Shield caregiver",
              initial: "Ask about alerts, Exa evidence, or VIP calls.",
            }}
          />
        </section>
      </main>
    </CopilotKit>
  );
}
