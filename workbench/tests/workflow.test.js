import { test } from "node:test";
import assert from "node:assert/strict";
import { newRun, transition, stages, analytics } from "../src/workflow.js";
import { demoVisits, reportText } from "../src/data.js";
const config = { structure: "DeepSeek", analysis: "OpenAI", fail: false };
test("run progresses to human review and never auto approves", () => {
  let r = newRun("visit", config, "test");
  for (let i = 0; i < stages.length; i++) r = transition(r, "tick");
  assert.equal(r.status, "review");
  assert.equal(r.step, 5);
  assert.equal(transition(r, "tick"), r);
  assert.match(r.logs.join(" "), /not transcribed|untranscribed/);
});
test("pause survives serialization and stops ticks; resume continues", () => {
  let r = transition(newRun("visit", config, "test"), "tick");
  r = JSON.parse(JSON.stringify(transition(r, "pause")));
  assert.equal(transition(r, "tick"), r);
  assert.equal(transition(transition(r, "resume"), "tick").step, 2);
});
test("failure is explicit and retry resumes without repeating completed stages", () => {
  let r = newRun("visit", { ...config, fail: true }, "test");
  r = transition(transition(r, "tick"), "tick");
  assert.equal(r.status, "failed");
  assert.equal(r.step, 1);
  r = transition(r, "retry");
  assert.equal(r.attempts, 2);
  assert.equal(transition(r, "tick").step, 2);
});
test("cancel halts progress and invalid actions do not mutate runs", () => {
  const r = transition(newRun("visit", config, "test"), "cancel");
  assert.equal(transition(r, "tick"), r);
  assert.equal(transition(r, "resume"), r);
  assert.equal(transition(r, "retry").status, "queued");
});
test("routing is snapshotted and source data remains unchanged", () => {
  const c = { ...config };
  const r = newRun("visit", c, "test");
  c.structure = "Other";
  assert.equal(r.config.structure, "DeepSeek");
  const before = JSON.stringify(r);
  transition(r, "tick");
  assert.equal(JSON.stringify(r), before);
});
test("analytics derives counts from evidence, including empty scope", () => {
  assert.deepEqual(analytics([]), {
    hives: 0,
    gps: 0,
    forms: 0,
    photos: 0,
    audio: 0,
  });
  assert.deepEqual(analytics(demoVisits()), {
    hives: 13,
    gps: 11,
    forms: 13,
    photos: 11,
    audio: 0,
  });
});
test("English demo reports disclose limits and retain evidence references", () => {
  const text = reportText(demoVisits()[0]);
  assert.doesNotMatch(text, /[\u3400-\u9fff]/);
  assert.match(text, /DEMO DATA/);
  assert.match(text, /not transcribed/);
  assert.match(text, /KPIN1024_Hive_0001_01.jpg/);
});
