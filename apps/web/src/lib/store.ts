import type { Vip } from "./types";

const g = globalThis as unknown as { __vips?: Vip[]; __alerts?: unknown[] };

function store() {
  if (!g.__vips) {
    g.__vips = [
      { id: "1", name: "Amma", phone: "+85290000001" },
      { id: "2", name: "Daughter", phone: "+85290000002" },
    ];
  }
  if (!g.__alerts) g.__alerts = [];
  return g;
}

export function listVips(): Vip[] {
  return store().__vips!;
}

export function upsertVip(vip: Omit<Vip, "id"> & { id?: string }): Vip {
  const vips = store().__vips!;
  if (vip.id) {
    const idx = vips.findIndex((v) => v.id === vip.id);
    if (idx >= 0) {
      vips[idx] = { id: vip.id, name: vip.name, phone: vip.phone };
      return vips[idx];
    }
  }
  const created = { id: String(Date.now()), name: vip.name, phone: vip.phone };
  vips.push(created);
  return created;
}

export function addAlert(alert: unknown) {
  store().__alerts!.unshift(alert);
  store().__alerts = store().__alerts!.slice(0, 50);
}

export function listAlerts() {
  return store().__alerts!;
}
