import { createIcons, icons } from "lucide";
import "./style.css";
import {
  demoVisits,
  totals,
  issues,
  reportText,
  escapeHtml as esc,
} from "./data.js";
import { parseVisitZip } from "./importer.js";
import { saveVisit, loadVisits } from "./store.js";
import { stages, newRun, transition, analytics } from "./workflow.js";

const app = document.querySelector("#app"),
  modal = document.querySelector("#modal");
const routes = [
  ["dashboard", "Overview", "layout-dashboard"],
  ["visits", "Visits", "clipboard-list"],
  ["agents", "Agent runs", "workflow"],
  ["reports", "Reports", "file-text"],
  ["analytics", "Analytics", "chart-no-axes-combined"],
  ["imports", "Imports", "folder-input"],
  ["team", "Team", "users"],
  ["activity", "Activity", "history"],
  ["settings", "Settings", "settings-2"],
];
const subtitles = {
  dashboard: "Field evidence. Clear decisions.",
  visits: "Visit records and source evidence.",
  agents: "Evidence to report, with human review.",
  reports: "Review and export inspection drafts.",
  analytics: "Coverage, completeness and visit comparisons.",
  imports: "Android visit archives.",
  team: "Local assignment directory.",
  activity: "Local workspace activity.",
  settings: "Workflow preferences and connection status.",
};
function read(key, fallback) {
  try {
    return JSON.parse(localStorage.getItem(key)) ?? fallback;
  } catch {
    return fallback;
  }
}
let visits = demoVisits(),
  selected = visits[0].id,
  section = routes.some((r) => r[0] === location.hash.slice(1))
    ? location.hash.slice(1)
    : "dashboard",
  tab = "hives",
  query = "",
  filter = "all",
  hiveIndex = 0,
  loading = false;
let state = read("bee-workspace-v2", {
  runs: [],
  records: {},
  team: [{ id: "local", name: "Local reviewer", email: "", role: "Reviewer" }],
  events: [],
  config: { structure: "DeepSeek", analysis: "OpenAI", fail: false },
});
let activeRun = state.runs[0]?.id,
  scope = "all";
const urls = new Map(),
  current = () => visits.find((v) => v.id === selected) || visits[0];
const icon = (name) => `<i data-lucide="${name}" aria-hidden="true"></i>`;
const button = (action, label, name, cls = "", extra = "") =>
  `<button data-action="${action}" class="${cls}" title="${esc(label)}" aria-label="${esc(label)}" ${extra}>${name ? icon(name) : ""}${cls.includes("icon-btn") ? "" : esc(label)}</button>`;
const badge = (text, tone = "neutral") =>
  `<span class="badge ${tone}">${esc(text)}</span>`;
const countIssues = (v) => v.hives.filter((h) => issues(h).length).length;
const meta = (v) =>
  state.records[v.id] || { status: "Unreviewed", owner: "local" };
const notesFor = (v) => {
  try {
    return localStorage.getItem(`bee-notes-${v.id}`) || "";
  } catch {
    return "";
  }
};
const fileUrl = (f) => {
  if (f.url) return f.url;
  if (!urls.has(f.blob)) urls.set(f.blob, URL.createObjectURL(f.blob));
  return urls.get(f.blob);
};
const visitSelect = (id = "report-visit", value = selected) =>
  `<select id="${id}" aria-label="Visit">${visits.map((v) => `<option value="${esc(v.id)}" ${v.id === value ? "selected" : ""}>${esc(v.pin)} / ${v.date}${v.demo ? " / Demo" : ""}</option>`).join("")}</select>`;
function toast(message) {
  const el = document.querySelector("#toast");
  el.textContent = message;
  el.classList.add("visible");
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => el.classList.remove("visible"), 4000);
}
function persist() {
  try {
    localStorage.setItem("bee-workspace-v2", JSON.stringify(state));
    return true;
  } catch {
    toast("Storage unavailable. Changes will not survive a reload.");
    return false;
  }
}
function log(message) {
  state.events.unshift({ time: new Date().toISOString(), message });
  state.events = state.events.slice(0, 300);
  persist();
}
function navigate(next) {
  section = next;
  location.hash = next;
  render();
}
function stats(items) {
  return `<section class="stats">${items.map(([label, value, detail, i]) => `<div class="stat"><div>${esc(label)}${icon(i || "boxes")}</div><strong>${esc(value)}</strong><small>${esc(detail)}</small></div>`).join("")}</section>`;
}
function empty(text) {
  return `<div class="empty">${icon("inbox")}<p>${esc(text)}</p></div>`;
}
function visitTable(items) {
  return `<div class="table-wrap"><table><thead><tr><th>Visit</th><th>Date</th><th>Hives</th><th>Review status</th><th>Assigned to</th><th></th></tr></thead><tbody>${items.map((v) => `<tr><td><strong>${esc(v.pin)}</strong><small class="block">${esc(v.title)} ${v.demo ? " / Demo" : ""}</small></td><td>${v.date}</td><td>${v.hives.length}</td><td>${badge(meta(v).status, meta(v).status === "Reviewed" ? "green" : "amber")}</td><td>${esc(state.team.find((t) => t.id === meta(v).owner)?.name || "Unassigned")}</td><td>${button("select", "Open visit", "arrow-right", "icon-btn", `data-id="${esc(v.id)}"`)}</td></tr>`).join("")}</tbody></table></div>`;
}
function coverage(a) {
  return [
    ["GPS captured", a.gps],
    ["Form saved", a.forms],
    ["2+ photos", a.photos],
    ["Voice note present", a.audio],
  ]
    .map(
      ([label, n], i) =>
        `<div class="bar-row"><div><span>${label}</span><strong>${n} / ${a.hives}</strong></div><meter class="meter-${i}" min="0" max="${a.hives || 1}" value="${n}" aria-label="${label}">${n}</meter></div>`,
    )
    .join("");
}
function dashboard() {
  const a = analytics(visits),
    pending = state.runs.filter((r) =>
      ["queued", "running", "paused"].includes(r.status),
    ).length;
  return `${stats([
    ["Visits", visits.length, "Includes labelled demo visits", "folder-open"],
    ["Hives", a.hives, "Recorded across visits"],
    [
      "Needs review",
      visits.filter((v) => meta(v).status !== "Reviewed").length,
      "Human review outstanding",
      "flag",
    ],
    ["Active runs", pending, "Local simulation only", "workflow"],
  ])}<div class="section-heading"><h2>Recent visits</h2>${button("nav-visits", "All visits", "arrow-right")}</div>${visitTable(visits)}<div class="operations-grid"><section class="work-section"><div class="section-heading"><h2>Agent workspace</h2>${badge("Simulator", "amber")}</div><p class="section-copy">${state.runs.length} runs / ${state.runs.filter((r) => r.status === "review").length} drafts ready for review</p><div class="pipeline">${stages.map((s, i) => `<div><span>${i + 1}</span>${s}</div>`).join("")}</div>${button("nav-agents", "Open Agent workspace", "workflow", "primary")}</section><section class="work-section"><h2>Evidence coverage</h2>${coverage(a)}<p class="muted">Coverage measures documentation, not colony health.</p></section></div>`;
}
function visitsView(v) {
  const list = visits.filter(
    (v) =>
      `${v.pin} ${v.title} ${v.date}`
        .toLowerCase()
        .includes(query.toLowerCase()) &&
      (filter === "all" ||
        (filter === "local" && !v.demo) ||
        (filter === "review" && meta(v).status !== "Reviewed")),
  );
  return `<div class="visit-layout spaced"><section class="visit-list"><div class="section-heading"><h2>Visits <span>${visits.length}</span></h2>${icon("list-filter")}</div><label class="search">${icon("search")}<input id="search" placeholder="Search PIN, orchard, date" aria-label="Search visits" value="${esc(query)}"></label><div class="filter-row">${[
    ["all", "All"],
    ["review", "To review"],
    ["local", "Imported"],
  ]
    .map(([f, l]) =>
      button("filter-" + f, l, "", filter === f ? "filter selected" : "filter"),
    )
    .join(
      "",
    )}</div><div class="visit-items">${list.map((item) => `<button class="visit-item ${item.id === v.id ? "selected" : ""}" data-action="select" data-id="${esc(item.id)}"><div class="visit-item-top"><span class="pin">${esc(item.pin)}</span>${badge(item.demo ? "Demo" : "Imported")}</div><strong>${esc(item.title)}</strong><div class="visit-meta">${icon("calendar-days")}${item.date} / ${item.hives.length} hives</div><div class="visit-item-bottom">${badge(meta(item).status, meta(item).status === "Reviewed" ? "green" : "amber")}${icon("arrow-up-right")}</div></button>`).join("") || empty("No matching visits") + button("reset-filter", "Clear filters", "rotate-ccw")}</div></section><section class="visit-detail"><div class="detail-heading"><div class="detail-id">${badge(v.demo ? "Demo visit" : "Imported visit")}<span>${v.date}</span></div><div class="detail-title"><div><h2>${esc(v.title)}</h2><span class="muted">${esc(v.pin)} / ${v.hives.length} hives</span></div><div class="actions">${button("run-visit", "Run agent", "workflow", "primary")}${button("report", "Report draft", "file-text")}</div></div><div class="record-controls"><label>Review status<select id="visit-status">${["Unreviewed", "In review", "Follow-up required", "Reviewed"].map((s) => `<option ${s === meta(v).status ? "selected" : ""}>${s}</option>`).join("")}</select></label><label>Assigned to<select id="visit-owner">${state.team.map((t) => `<option value="${esc(t.id)}" ${t.id === meta(v).owner ? "selected" : ""}>${esc(t.name)}</option>`).join("")}</select></label></div></div><div class="tabs" role="tablist">${[
    ["hives", "Hives", "boxes"],
    ["media", "Photos & audio", "images"],
    ["overview", "Visit details", "map-pin"],
  ]
    .map(
      ([t, l, i]) =>
        `<button role="tab" aria-selected="${tab === t}" class="${tab === t ? "active" : ""}" data-action="tab-${t}">${icon(i)}${l}</button>`,
    )
    .join(
      "",
    )}</div>${tab === "hives" ? hivesView(v) : tab === "media" ? mediaView(v) : overview(v)}</section></div>`;
}
function hivesView(v) {
  const h = v.hives[Math.min(hiveIndex, v.hives.length - 1)];
  if (!h) return empty("No hive records");
  return `<div class="table-wrap"><table><thead><tr><th>Hive</th><th>Condition</th><th>Evidence</th><th>Checks</th></tr></thead><tbody>${v.hives.map((h, i) => `<tr class="${i === hiveIndex ? "selected" : ""}"><td>${button("hive", `${h.group} ${String(h.sequence).padStart(4, "0")}`, "box", "hive-link", `data-index="${i}"`)}</td><td>${badge(h.form?.condition || "Not recorded", h.form?.condition === "Good" ? "green" : "neutral")}</td><td><span class="evidence">${icon("image")}${h.photos.length} ${icon("mic")}${h.audio.length}</span></td><td>${badge(issues(h).length ? `${issues(h).length} flags` : "Recorded", issues(h).length ? "amber" : "green")}</td></tr>`).join("")}</tbody></table></div><div class="hive-inspector"><div class="section-heading"><h3>${esc(h.group)} ${h.sequence}</h3>${badge(v.demo ? "Demo evidence" : "Source evidence")}</div><div class="inspector-grid"><div class="photo-preview">${h.photos[0] ? `<button data-action="photo" data-hive="${hiveIndex}" data-index="0"><img src="${esc(fileUrl(h.photos[0]))}" alt="${v.demo ? "Demo orchard hives" : "Hive photograph"}"><span>${icon("maximize-2")}View photo</span></button>` : empty("No photographs")}</div><div class="inspector-notes"><p class="small-label">FIELD NOTES</p><p>${esc(h.form?.notes || "No written notes")}</p><div class="gps-line">${icon("map-pin")}${h.gps ? `${h.gps.lat.toFixed(5)}, ${h.gps.lon.toFixed(5)}` : "GPS not recorded"}</div><div class="inline-alert">${icon("flag")}<p>${issues(h).map(esc).join(" / ") || "No missing evidence detected by local rules."}</p></div></div></div></div>`;
}
function mediaView(v) {
  return `<div class="media-body"><h3>Photographs (${totals(v).photos})</h3><div class="photo-grid">${v.hives.flatMap((h, hi) => h.photos.map((p, i) => `<button class="photo-tile" data-action="photo" data-hive="${hi}" data-index="${i}"><img src="${esc(fileUrl(p))}" alt="${esc(p.name)}" loading="lazy"><span>${esc(h.group)} ${h.sequence}<small>${i + 1} / ${v.demo ? "Demo" : "Photo"}</small></span></button>`)).join("") || empty("No photographs")}</div><h3 class="audio-heading">Voice notes (${totals(v).audio})</h3>${v.hives.flatMap((h) => h.audio.map((a) => `<div class="audio-row">${icon("mic")}<div><strong>${esc(a.name)}</strong><audio controls preload="metadata" src="${esc(fileUrl(a))}"></audio></div></div>`)).join("") || empty("No voice notes in this visit")}</div>`;
}
const mapLink = (h) =>
  `<a target="_blank" rel="noopener noreferrer" href="https://www.openstreetmap.org/?mlat=${encodeURIComponent(h.gps.lat)}&mlon=${encodeURIComponent(h.gps.lon)}#map=17/${encodeURIComponent(h.gps.lat)}/${encodeURIComponent(h.gps.lon)}">Open map</a>`;
function overview(v) {
  return `<div class="overview"><dl><div><dt>PIN / KPIN</dt><dd>${esc(v.pin)}</dd></div><div><dt>Visit date</dt><dd>${v.date}</dd></div><div><dt>Source</dt><dd>${v.demo ? "Demo fixture" : "Android ZIP"}</dd></div><div><dt>Cloud backup</dt><dd>Not connected</dd></div></dl><h3>GPS locations</h3>${v.hives.map((h) => `<p>${esc(h.group)} ${h.sequence}: ${h.gps ? `${h.gps.lat}, ${h.gps.lon} / ${mapLink(h)}` : "Not recorded"}</p>`).join("")}<h3>Evidence checks</h3><p>${countIssues(v)} hives flagged for missing forms, GPS, fewer than two photos or recorded damage.</p>${v.demo ? '<p class="muted">Repeated demo photo: <a href="https://grocycle.com/beekeeping-for-profit/" target="_blank" rel="noopener noreferrer">GroCycle</a>. Not inspection evidence.</p>' : ""}</div>`;
}
function reportsView(v) {
  return `<div class="report-toolbar"><label>Visit ${visitSelect()}</label><div>${button("download", "Markdown", "download")}${button("print", "Print / PDF", "printer", "primary")}</div></div><div class="report-layout"><article class="report-sheet"><div class="report-top"><strong>Bee Audit</strong>${badge("Unsigned draft", "amber")}</div><h2>Pollination inspection record</h2><p>${esc(v.pin)} / ${v.date}</p><div class="report-disclaimer">${v.demo ? "DEMO DATA. " : ""}Template report from recorded fields. No AI analysis, transcription or auditor signature.</div><div class="report-counts">${Object.entries(
    totals(v),
  )
    .map(([k, n]) => `<span><b>${n}</b>${k}</span>`)
    .join(
      "",
    )}</div><h3>01 / Visit summary</h3><p>${countIssues(v)} hives flagged for review. Review status: ${esc(meta(v).status)}. This status is a local workflow label, not certification.</p><h3>02 / Evidence by hive</h3>${v.hives.map((h) => `<section class="report-hive"><strong>${esc(h.group)} ${h.sequence}</strong><p>${esc(h.form?.notes || "No written notes.")}</p><small>Condition: ${esc(h.form?.condition || "Not recorded")} / Activity: ${esc(h.form?.activity || "Not recorded")}</small><small>GPS: ${h.gps ? `${h.gps.lat}, ${h.gps.lon}` : "Not recorded"}</small><p class="report-issue">${issues(h).map(esc).join(" / ")}</p><div class="report-evidence">${h.photos.map((p) => `<figure><img src="${esc(fileUrl(p))}" alt="${esc(p.name)}"><figcaption>${esc(p.name)}</figcaption></figure>`).join("")}</div>${h.audio.map((a) => `<small>Audio: ${esc(a.name)} (not transcribed)</small>`).join("")}</section>`).join("")}<h3>03 / Reviewer notes</h3><p class="printed-notes">${esc(notesFor(v) || "Pending review")}</p><div class="signature">Reviewer: __________________ Date: ______________</div></article><aside class="report-side"><h3>Human review</h3><p class="section-copy">${badge(meta(v).status, meta(v).status === "Reviewed" ? "green" : "amber")}</p><label for="report-notes">Reviewer notes</label><textarea id="report-notes" rows="6" placeholder="Record follow-up actions and evidence references">${esc(notesFor(v))}</textarea><small id="note-status">Saved in this browser only</small><div class="stack-actions">${button("review", "Mark reviewed", "check", "primary")}${button("followup", "Request follow-up", "flag")}${button("run-visit", "Run agent simulation", "workflow")}</div><div class="inline-alert">${icon("info")}<p>Audio is not transcribed. Source files and notes remain unchanged.</p></div></aside></div>`;
}
function agentsView() {
  const r = state.runs.find((r) => r.id === activeRun) || state.runs[0],
    v = r && visits.find((v) => v.id === r.visitId);
  return `<div class="report-toolbar"><label>Visit ${visitSelect("agent-visit")}</label>${button("run-visit", "Start simulated run", "play", "primary")}</div><div class="notice">${icon("flask-conical")}Local simulator / No model calls / API spend: $0 / Closing this tab pauses execution</div><div class="agent-layout spaced"><section><div class="section-heading"><h2>Run history</h2><span>${state.runs.length} runs</span></div>${
    state.runs
      .map((x) => {
        const v = visits.find((v) => v.id === x.visitId);
        return `<button class="run-item ${r?.id === x.id ? "selected" : ""}" data-action="run-select" data-id="${esc(x.id)}"><div><strong>${esc(v?.pin || "Unavailable visit")}</strong><small>${new Date(x.createdAt).toLocaleString("en-NZ")} / Attempt ${x.attempts}</small></div>${badge(x.status, x.status === "failed" ? "red" : x.status === "review" ? "green" : "amber")}</button>`;
      })
      .join("") || empty("No runs yet")
  }</section><section class="work-section run-detail">${r ? `<div class="section-heading"><div><h2>${esc(v?.pin || "Unavailable visit")}</h2><small>Run ${esc(r.id.slice(0, 8))} / Simulation</small></div>${badge(r.status, r.status === "failed" ? "red" : "amber")}</div><progress value="${r.step}" max="${stages.length}" aria-label="Run progress"></progress><ol class="steps agent-steps">${stages.map((s, i) => `<li class="${i < r.step ? "done" : ""}">${icon(i < r.step ? "check-circle-2" : i === r.step ? "circle-dot" : "circle")}<div>${s}<small>${i === 0 ? "Local rule checks" : i === 4 ? "Local template" : "Simulated; no AI output"} / ${i < r.step ? "Complete" : i === r.step ? r.status : "Waiting"}</small></div></li>`).join("")}</ol><div class="actions">${["queued", "running"].includes(r.status) ? button("run-pause", "Pause", "pause") : ""}${r.status === "paused" ? button("run-resume", "Resume", "play") : ""}${["queued", "running", "paused"].includes(r.status) ? button("run-cancel", "Cancel", "square") : ""}${["failed", "cancelled"].includes(r.status) ? button("run-retry", "Retry", "rotate-cw", "primary") : ""}${r.status === "review" && v ? button("run-report", "Review draft", "file-text", "primary") : ""}</div><h3 class="spaced">Execution log</h3><ol class="run-log">${r.logs.map((l) => `<li>${esc(l)}</li>`).join("")}</ol><small>Routing: ${esc(r.config.structure)} / ${esc(r.config.analysis)}. No tokens used.</small>` : empty("Choose a visit and start a simulated run.")}</section></div>`;
}
function analyticsView() {
  const list = visits.filter(
      (v) =>
        scope === "all" ||
        (scope === "demo" && v.demo) ||
        (scope === "imported" && !v.demo),
    ),
    a = analytics(list),
    max = Math.max(1, ...list.map((v) => v.hives.length));
  return `<div class="report-toolbar"><label>Data scope<select id="analytics-scope"><option value="all" ${scope === "all" ? "selected" : ""}>All records (includes demo)</option><option value="imported" ${scope === "imported" ? "selected" : ""}>Imported only</option><option value="demo" ${scope === "demo" ? "selected" : ""}>Demo only</option></select></label>${button("export-csv", "Export summary CSV", "download")}</div>${stats(
    [
      ["Visits", list.length, "Selected scope", "folder-open"],
      ["Hives", a.hives, "Recorded"],
      [
        "GPS coverage",
        a.hives ? `${Math.round((a.gps / a.hives) * 100)}%` : "N/A",
        "Hives with coordinates",
        "map-pin",
      ],
      [
        "Flagged hives",
        list.reduce((n, v) => n + countIssues(v), 0),
        "Evidence checks, not diagnosis",
        "flag",
      ],
    ],
  )}<div class="operations-grid"><section class="work-section"><h2>Evidence coverage</h2>${coverage(a)}</section><section class="work-section"><h2>Hives by visit date</h2><p class="muted">Separate visits; not a colony-health trend.</p>${
    [...list]
      .sort((a, b) => a.date.localeCompare(b.date))
      .map(
        (v) =>
          `<div class="bar-row"><div><span>${v.date} / ${esc(v.pin)} ${v.demo ? "(Demo)" : ""}</span><strong>${v.hives.length}</strong></div><meter min="0" max="${max}" value="${v.hives.length}" aria-label="${esc(v.pin)} hive count">${v.hives.length}</meter></div>`,
      )
      .join("") || empty("No visits in this scope")
  }</section></div><section class="work-section"><h2>Visit comparison</h2>${visitTable(list)}</section><section class="work-section"><h2>Location coverage</h2><p class="muted">Coordinates are source records. External map links require a map service.</p><div class="table-wrap"><table><thead><tr><th>Visit</th><th>Hive</th><th>Latitude</th><th>Longitude</th><th>Map</th></tr></thead><tbody>${list.flatMap((v) => v.hives.filter((h) => h.gps).map((h) => `<tr><td>${esc(v.pin)}${v.demo ? " (Demo)" : ""}</td><td>${esc(h.group)} ${h.sequence}</td><td>${h.gps.lat}</td><td>${h.gps.lon}</td><td>${mapLink(h)}</td></tr>`)).join("")}</tbody></table></div></section>`;
}
function importsView() {
  const locals = visits.filter((v) => !v.demo);
  return `<section class="import-zone"><h2>Import a visit archive</h2><p>Bee Audit Android ZIP / Up to 100 MB</p>${button("import", "Choose ZIP", "upload", "primary", loading ? "disabled" : "")}<small>Local storage only. Keep your original ZIP as a separate backup.</small></section><h2>Import history</h2>${locals.length ? visitTable(locals) : empty("No imported visits")}`;
}
function teamView() {
  return `<section class="work-section"><div class="section-heading"><h2>Assignment directory</h2>${badge("Local preview")}</div><p class="section-copy">No invitations, login accounts or access permissions are created.</p><form id="team-form" class="record-controls"><label>Name<input name="name" required maxlength="80"></label><label>Email (optional)<input name="email" type="email" maxlength="150"></label><label>Role label<select name="role"><option>Reviewer</option><option>Auditor</option><option>Coordinator</option></select></label><button class="primary" type="submit">${icon("user-plus")}Add person</button></form><div class="table-wrap"><table><thead><tr><th>Name</th><th>Email</th><th>Role label</th><th></th></tr></thead><tbody>${state.team.map((t) => `<tr><td>${esc(t.name)}</td><td>${esc(t.email || "Not provided")}</td><td>${esc(t.role)}</td><td>${t.id === "local" ? "Default reviewer" : button("remove-person", "Remove person", "trash-2", "icon-btn", `data-id="${esc(t.id)}"`)}</td></tr>`).join("")}</tbody></table></div></section>`;
}
function activityView() {
  return `<section class="work-section"><div class="section-heading"><h2>Activity log</h2>${button("export-log", "Export JSON", "download")}</div><p class="section-copy">Local browser log, up to 300 events. Not a tamper-proof audit trail.</p>${state.events.length ? `<ol class="activity-log">${state.events.map((e) => `<li><time>${new Date(e.time).toLocaleString("en-NZ")}</time><span>${esc(e.message)}</span></li>`).join("")}</ol>` : empty("No activity recorded yet")}</section>`;
}
function settingsView() {
  return `<section class="settings-section"><h2>Agent routing preview</h2><p>No API keys are collected. These choices affect simulation labels only.</p><form id="settings-form" class="settings-form"><label>Observation structuring<select name="structure">${["DeepSeek", "OpenAI"].map((s) => `<option ${state.config.structure === s ? "selected" : ""}>${s}</option>`).join("")}</select></label><label>Complex review<select name="analysis">${["OpenAI", "DeepSeek", "Human only"].map((s) => `<option ${state.config.analysis === s ? "selected" : ""}>${s}</option>`).join("")}</select></label><label class="checkbox"><input type="checkbox" name="fail" ${state.config.fail ? "checked" : ""}>Simulate provider failure on new runs</label><button class="primary" type="submit">${icon("save")}Save preferences</button></form></section><section class="settings-section"><h2>Connections</h2><dl><div><dt>Cloud storage</dt><dd>Not connected</dd></div><div><dt>Model APIs</dt><dd>Not connected</dd></div><div><dt>Speech transcription</dt><dd>Not connected</dd></div><div><dt>Authentication</dt><dd>Not configured</dd></div></dl></section><section class="settings-section"><h2>Data retention</h2><p>Imported evidence is stored in this browser using IndexedDB. Assignments, simulation runs and review notes are local. Clearing website data removes them; this is not cloud backup.</p></section>`;
}
function render() {
  const v = current(),
    title = routes.find((r) => r[0] === section)?.[1] || "Overview";
  app.innerHTML = `<aside class="sidebar"><a href="#dashboard" class="brand" data-action="nav-dashboard"><span class="brand-mark">${icon("hexagon")}</span><span>Bee Audit<small>OPERATIONS WORKSPACE</small></span></a><div class="workspace-label">WORKSPACE</div><nav aria-label="Main navigation">${routes.map(([id, l, i]) => button("nav-" + id, l, i, section === id ? "nav-item active" : "nav-item", section === id ? 'aria-current="page"' : "")).join("")}</nav><div class="sidebar-bottom"><div class="connection"><span class="dot"></span><div>Local preview<small>No cloud connection</small></div></div></div></aside><div class="workspace"><header class="topbar"><div class="breadcrumb">Workspace ${icon("chevron-right")}<strong>${title}</strong></div><div class="top-actions">${badge("Simulation mode", "amber")}${button("help", "Help", "circle-help", "icon-btn")}</div></header><main><div class="page-heading"><div><p class="eyebrow">BEE AUDIT / OPERATIONS</p><h1>${title}</h1><p class="subtitle">${subtitles[section]}</p></div>${button("import", "Import ZIP", "plus", "primary", loading ? "disabled" : "")}</div><div class="notice">${icon("hard-drive")}<span>Local browser only. No uploads or AI calls.</span><span class="demo-label">${visits.some((v) => !v.demo) ? "Contains imported and demo records" : "Demo records"}</span></div>${({ dashboard, visits: () => visitsView(v), agents: agentsView, reports: () => reportsView(v), analytics: analyticsView, imports: importsView, team: teamView, activity: activityView, settings: settingsView }[section] || dashboard)()}<footer>Bee Audit<span>Field evidence / Human oversight</span><span>Local preview 0.2</span></footer></main></div>`;
  createIcons({ icons });
  document.querySelector("#search")?.addEventListener("input", (e) => {
    const p = e.target.selectionStart;
    query = e.target.value;
    render();
    const i = document.querySelector("#search");
    i.focus();
    i.setSelectionRange(p, p);
  });
  document.querySelector("#report-notes")?.addEventListener("input", (e) => {
    try {
      localStorage.setItem(`bee-notes-${v.id}`, e.target.value);
      document.querySelector("#note-status").textContent =
        "Saved in this browser";
    } catch {
      document.querySelector("#note-status").textContent =
        "Save failed. Export your notes before leaving.";
    }
  });
}
function showModal(html) {
  modal.innerHTML = html;
  if (!modal.open) modal.showModal();
  createIcons({ icons });
}
function showPhoto(hi, index) {
  const v = current(),
    h = v.hives[hi],
    f = h?.photos[index];
  if (!f) return;
  showModal(
    `<div class="modal-head"><div><strong>${esc(f.name)}</strong><small>${v.demo ? "Demo image, not inspection evidence" : esc(v.pin)}</small></div>${button("close", "Close photo", "x", "icon-btn")}</div><img class="lightbox" src="${esc(fileUrl(f))}" alt="${esc(f.name)}"><div class="modal-foot">${button("photo", "Previous photo", "arrow-left", "icon-btn", `data-hive="${hi}" data-index="${index - 1}" ${index === 0 ? "disabled" : ""}`)}<span>${index + 1} / ${h.photos.length}</span>${button("photo", "Next photo", "arrow-right", "icon-btn", `data-hive="${hi}" data-index="${index + 1}" ${index === h.photos.length - 1 ? "disabled" : ""}`)}</div>`,
  );
}
async function importFile(file) {
  if (!file || loading) return;
  loading = true;
  showModal(
    '<div class="modal-content"><h2>Reading visit archive</h2><p id="import-progress" role="status">Parsing ZIP...</p></div>',
  );
  try {
    const v = await parseVisitZip(
      file,
      (n, t) =>
        (document.querySelector("#import-progress").textContent =
          `Reading media ${n} / ${t}`),
    );
    if (visits.some((x) => x.id === v.id))
      throw Error(
        "Visit already imported. Existing evidence has not been overwritten.",
      );
    await saveVisit(v);
    visits.unshift(v);
    selected = v.id;
    hiveIndex = 0;
    tab = "hives";
    query = "";
    filter = "all";
    modal.close();
    log(`Imported ${v.pin} / ${v.date}.`);
    navigate("visits");
    toast("Visit saved locally. Nothing uploaded.");
  } catch (e) {
    showModal(
      `<div class="modal-content"><h2>Import not completed</h2><p>${esc(e.message)}</p>${button("close", "Close", "x")}</div>`,
    );
  } finally {
    loading = false;
    document.querySelector("#zip-input").value = "";
    render();
  }
}
function download(text, name, type = "text/plain") {
  const url = URL.createObjectURL(new Blob([text], { type })),
    a = document.createElement("a");
  a.href = url;
  a.download = name;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
function startRun() {
  const v = current();
  const existing = state.runs.find(
    (r) =>
      r.visitId === v.id && ["queued", "running", "paused"].includes(r.status),
  );
  if (existing) {
    activeRun = existing.id;
    toast("This visit already has an active run.");
  } else {
    const r = newRun(v.id, state.config);
    state.runs.unshift(r);
    activeRun = r.id;
    log(`Started simulated run for ${v.pin}.`);
  }
  navigate("agents");
}
function markStatus(status) {
  const v = current();
  state.records[v.id] = { ...meta(v), status };
  log(`${v.pin}: review status changed to ${status}.`);
  render();
}
document.addEventListener("click", (e) => {
  const el = e.target.closest("[data-action]");
  if (!el || el.disabled) return;
  e.preventDefault();
  const a = el.dataset.action;
  if (a.startsWith("nav-")) navigate(a.slice(4));
  else if (a === "select") {
    selected = el.dataset.id;
    hiveIndex = 0;
    tab = "hives";
    navigate("visits");
  } else if (a.startsWith("tab-")) {
    tab = a.slice(4);
    render();
  } else if (a.startsWith("filter-")) {
    filter = a.slice(7);
    render();
  } else if (a === "reset-filter") {
    query = "";
    filter = "all";
    render();
  } else if (a === "hive") {
    hiveIndex = Number(el.dataset.index);
    render();
  } else if (a === "photo")
    showPhoto(Number(el.dataset.hive), Number(el.dataset.index));
  else if (a === "close") modal.close();
  else if (a === "import") document.querySelector("#zip-input").click();
  else if (a === "report") navigate("reports");
  else if (a === "run-visit") startRun();
  else if (a === "run-select") {
    activeRun = el.dataset.id;
    render();
  } else if (
    ["run-pause", "run-resume", "run-cancel", "run-retry"].includes(a)
  ) {
    const r = state.runs.find((r) => r.id === activeRun) || state.runs[0];
    if (r) {
      state.runs = state.runs.map((x) =>
        x.id === r.id ? transition(x, a.slice(4)) : x,
      );
      log(`${a.slice(4)}: run ${r.id.slice(0, 8)}.`);
      render();
    }
  } else if (a === "run-report") {
    const r = state.runs.find((r) => r.id === activeRun) || state.runs[0];
    selected = r.visitId;
    navigate("reports");
  } else if (a === "download") {
    download(
      reportText(current(), notesFor(current())),
      `${current().pin}_${current().date}_draft.md`,
      "text/markdown",
    );
    log(`Exported draft for ${current().pin}.`);
  } else if (a === "print") {
    render();
    Promise.all(
      [...document.querySelectorAll(".report-sheet img")].map((i) =>
        i.decode().catch(() => {}),
      ),
    ).then(() => window.print());
  } else if (a === "review") {
    showModal(
      `<div class="modal-content"><h2>Mark this visit reviewed?</h2><p>${countIssues(current())} hives have evidence flags. Audio has not been transcribed. This local status does not certify hive health or sign the report.</p><label class="checkbox"><input id="review-confirm" type="checkbox">I have checked the source evidence and reviewer notes.</label><div class="actions spaced">${button("confirm-review", "Mark reviewed", "check", "primary")}${button("close", "Cancel", "x")}</div></div>`,
    );
  } else if (a === "confirm-review") {
    if (!document.querySelector("#review-confirm").checked) {
      toast("Confirm the evidence review first.");
      return;
    }
    modal.close();
    markStatus("Reviewed");
  } else if (a === "followup") markStatus("Follow-up required");
  else if (a === "remove-person") {
    const id = el.dataset.id;
    if (Object.values(state.records).some((r) => r.owner === id)) {
      toast("Reassign this person’s visits before removing them.");
      return;
    }
    state.team = state.team.filter((t) => t.id !== id);
    log("Removed a local directory entry.");
    render();
  } else if (a === "export-log")
    download(
      JSON.stringify(state.events, null, 2),
      "bee-local-activity.json",
      "application/json",
    );
  else if (a === "export-csv") {
    const cell = (x) =>
      '"' +
      String(x)
        .replace(/^[=+@-]/, "'$&")
        .replaceAll('"', '""') +
      '"';
    const list = visits.filter(
      (v) =>
        scope === "all" ||
        (scope === "demo" && v.demo) ||
        (scope === "imported" && !v.demo),
    );
    download(
      [
        [
          "PIN",
          "Date",
          "Demo",
          "Hives",
          "Photos",
          "Audio",
          "Flagged hives",
          "Review status",
        ],
        ...list.map((v) => [
          v.pin,
          v.date,
          v.demo,
          ...Object.values(totals(v)),
          countIssues(v),
          meta(v).status,
        ]),
      ]
        .map((row) => row.map(cell).join(","))
        .join("\r\n"),
      "bee-visit-summary.csv",
      "text/csv",
    );
  } else if (a === "help")
    showModal(
      `<div class="modal-head"><h2>Workspace help</h2>${button("close", "Close help", "x", "icon-btn")}</div><div class="modal-content"><ol><li>Export a visit ZIP from Bee Audit Android and keep a backup.</li><li>Import the ZIP, then review the visit’s photos, audio and forms.</li><li>Assign a reviewer and update review status.</li><li>Start an Agent simulation to preview the workflow. No models are called.</li><li>Review the template draft, add notes and export Markdown or PDF.</li></ol><p>All evidence and workflow state stay in this browser. Agent execution stops when the tab is closed. Team roles are labels, not permissions. For a simulated error, enable provider failure in Settings, then start a new run and use Retry.</p></div>`,
    );
});
document.addEventListener("change", (e) => {
  const id = e.target.id;
  if (id === "report-visit" || id === "agent-visit") {
    selected = e.target.value;
    render();
  } else if (id === "visit-status") {
    if (e.target.value === "Reviewed") {
      e.target.value = meta(current()).status;
      navigate("reports");
      toast("Use Mark reviewed after checking the report and evidence.");
    } else markStatus(e.target.value);
  } else if (id === "visit-owner") {
    state.records[current().id] = { ...meta(current()), owner: e.target.value };
    log(`Changed reviewer for ${current().pin}.`);
  } else if (id === "analytics-scope") {
    scope = e.target.value;
    render();
  }
});
document.addEventListener("submit", (e) => {
  if (!["settings-form", "team-form"].includes(e.target.id)) return;
  e.preventDefault();
  const d = new FormData(e.target);
  if (e.target.id === "settings-form") {
    state.config = {
      structure: d.get("structure"),
      analysis: d.get("analysis"),
      fail: d.has("fail"),
    };
    log("Updated simulation routing preferences.");
    toast("Preferences saved. No services connected.");
  } else {
    const name = d.get("name").trim();
    if (!name) {
      toast("Enter a name.");
      return;
    }
    state.team.push({
      id: crypto.randomUUID(),
      name,
      email: d.get("email").trim(),
      role: d.get("role"),
    });
    log(`Added local directory entry: ${name}.`);
  }
  render();
});
document
  .querySelector("#zip-input")
  .addEventListener("change", (e) => importFile(e.target.files[0]));
document.addEventListener(
  "play",
  (e) => {
    if (e.target.tagName === "AUDIO")
      document.querySelectorAll("audio").forEach((a) => {
        if (a !== e.target) a.pause();
      });
  },
  true,
);
modal.addEventListener("click", (e) => {
  if (e.target === modal && !loading) modal.close();
});
modal.addEventListener("cancel", (e) => {
  if (loading) e.preventDefault();
});
window.addEventListener("hashchange", () => {
  const next = location.hash.slice(1);
  if (routes.some((r) => r[0] === next) && section !== next) {
    section = next;
    render();
  }
});
// Background simulation must not interrupt media playback or note editing.
setInterval(() => {
  let changed = false;
  state.runs = state.runs.map((r) => {
    const next = transition(r, "tick");
    if (next !== r) {
      changed = true;
      if (next.status === "review" || next.status === "failed")
        state.events.unshift({
          time: new Date().toISOString(),
          message: `Run ${next.id.slice(0, 8)}: ${next.status} (simulation).`,
        });
    }
    return next;
  });
  if (changed) {
    state.events = state.events.slice(0, 300);
    persist();
    if (section === "agents" && !modal.open) render();
  }
}, 1800);
render();
loadVisits()
  .then((stored) => {
    visits = [...stored, ...visits];
    if (stored.length) selected = stored[0].id;
    render();
  })
  .catch(() =>
    toast("Local evidence storage unavailable. Imports may not persist."),
  );
