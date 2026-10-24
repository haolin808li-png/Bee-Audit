export const stages = [
  "Validate evidence",
  "Transcribe voice notes",
  "Structure observations",
  "Review findings",
  "Prepare report",
];
export function newRun(visitId, config, id = crypto.randomUUID()) {
  return {
    id,
    visitId,
    status: "queued",
    step: 0,
    config: { ...config },
    createdAt: new Date().toISOString(),
    logs: ["Queued in local simulator."],
    attempts: 1,
  };
}
export function transition(run, action) {
  const r = { ...run, logs: [...run.logs] };
  if (action === "pause" && ["queued", "running"].includes(r.status))
    r.status = "paused";
  else if (action === "resume" && r.status === "paused") r.status = "queued";
  else if (
    action === "cancel" &&
    ["queued", "running", "paused"].includes(r.status)
  )
    r.status = "cancelled";
  else if (action === "retry" && ["failed", "cancelled"].includes(r.status)) {
    r.status = "queued";
    r.attempts++;
    r.config.fail = false;
  } else if (action === "tick" && ["queued", "running"].includes(r.status)) {
    if (r.config.fail && r.step === 1) {
      r.status = "failed";
      r.logs.push("Simulated provider timeout. No API request was made.");
    } else {
      r.logs.push(
        [
          "Local evidence inventory checked. Missing evidence remains flagged.",
          "Transcription simulated only. Audio remains untranscribed.",
          `Routing preview: ${r.config.structure}. No model called.`,
          `Review preview: ${r.config.analysis}. No visual or diagnostic inference performed.`,
          "Template draft prepared from recorded fields. Human review required.",
        ][r.step],
      );
      r.step++;
      r.status = r.step === stages.length ? "review" : "running";
    }
  } else return run;
  if (action !== "tick")
    r.logs.push(
      `${action[0].toUpperCase() + action.slice(1)} requested locally.`,
    );
  return r;
}
export function analytics(visits) {
  const hives = visits.flatMap((v) => v.hives);
  return {
    hives: hives.length,
    gps: hives.filter((h) => h.gps).length,
    forms: hives.filter((h) => h.record).length,
    photos: hives.filter((h) => h.photos.length >= 2).length,
    audio: hives.filter((h) => h.audio.length).length,
  };
}
