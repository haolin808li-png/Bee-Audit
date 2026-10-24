export const escapeHtml = (value) =>
  String(value ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
export const totals = (visit) => ({
  hives: visit.hives.length,
  photos: visit.hives.reduce((n, h) => n + h.photos.length, 0),
  audio: visit.hives.reduce((n, h) => n + h.audio.length, 0),
});
export const issues = (h) =>
  [
    !h.record && "Missing inspection form",
    !h.gps && "Missing GPS",
    h.photos.length < 2 && "Fewer than 2 photographs",
    h.form?.damage_visible && "Visible damage recorded",
  ].filter(Boolean);
export function demoVisits() {
  return [
    ["KPIN1024", "Te Puke / North orchard", "2026-10-24", 6],
    ["KPIN2086", "Katikati / West orchard", "2026-10-23", 4],
    ["KPIN3102", "Tauranga / East orchard", "2026-10-22", 3],
  ].map(([pin, title, date, count], v) => ({
    id: `demo-${pin}`,
    pin,
    title,
    date,
    demo: true,
    importedAt: "2026-10-25T08:00:00Z",
    hives: Array.from({ length: count }, (_, i) => ({
      id: `${pin}_Hive_${String(i + 1).padStart(4, "0")}`,
      sequence: i + 1,
      group: "Hive",
      record: true,
      gps:
        i === 3
          ? null
          : {
              lat: -37.7841 + i * 0.0001,
              lon: 176.3238 + i * 0.0001,
              accuracy_m: 8,
            },
      form: {
        condition: i === 2 ? "Fair" : "Good",
        activity: i === 2 ? "Low" : "High",
        hive_present: true,
        damage_visible: i === 2,
        notes:
          i === 2
            ? "Low entrance activity. Damage visible at lid edge. Field follow-up suggested."
            : "Normal entrance activity. Hive exterior intact.",
      },
      photos: Array.from({ length: i === 3 ? 1 : 2 }, (_, p) => ({
        name: `${pin}_Hive_${String(i + 1).padStart(4, "0")}_${String(p + 1).padStart(2, "0")}.jpg`,
        url: "/assets/orchard.jpg",
        demo: true,
      })),
      audio: [],
    })),
  }));
}
export function reportText(visit, notes = "") {
  const t = totals(visit);
  return `# Bee Audit inspection draft\n\n${visit.demo ? "DEMO DATA. Not a real inspection." : "Source: locally imported Bee Audit evidence."}\nTemplate only. No AI analysis or auditor signature. Audio is not transcribed.\n\nPIN: ${visit.pin}\nVisit date: ${visit.date}\nHives: ${t.hives} / Photos: ${t.photos} / Audio: ${t.audio}\n\n${visit.hives.map((h) => `## ${h.group} ${String(h.sequence).padStart(4, "0")}\n\nCondition: ${h.form?.condition ?? "Not recorded"}\nActivity: ${h.form?.activity ?? "Not recorded"}\nGPS: ${h.gps ? `${h.gps.lat}, ${h.gps.lon}` : "Not recorded"}\nNotes: ${h.form?.notes || "None"}\nReview flags: ${issues(h).join("; ") || "No rule flags"}\nPhotos: ${h.photos.map((p) => p.name).join(", ") || "None"}\nAudio: ${h.audio.map((p) => p.name).join(", ") || "None"}\n`).join("\n")}\n## Reviewer notes\n\n${notes || "Pending"}\n`;
}
