import { test } from "node:test";
import assert from "node:assert/strict";
import JSZip from "jszip";
import { parseVisitZip } from "../src/importer.js";
import { reportText, escapeHtml, issues } from "../src/data.js";

const record = {
  pin: "TEST001",
  visit_date: "2026-10-25",
  group: "Hive",
  sequence_number: 1,
  gps: { lat: -37.78, lon: 176.32 },
  form: { notes: "Inspect <roof> & entrance", condition: "Good" },
};
async function fixture(mutate) {
  const zip = new JSZip();
  zip.file("session.json", JSON.stringify(record));
  zip.file("TEST001_Hive_0001/TEST001_Hive_0001.json", JSON.stringify(record));
  zip.file(
    "TEST001_Hive_0001/TEST001_Hive_0001_01.jpg",
    new Uint8Array([1, 2, 3]),
  );
  zip.file(
    "TEST001_Hive_0001/TEST001_Hive_0001_01.m4a",
    new Uint8Array([1, 2, 3]),
  );
  mutate?.(zip);
  return new Blob([await zip.generateAsync({ type: "uint8array" })]);
}
test("imports Android export, counts actual media and builds evidence references", async () => {
  const v = await parseVisitZip(await fixture());
  assert.equal(v.pin, "TEST001");
  assert.equal(v.hives[0].photos.length, 1);
  assert.equal(v.hives[0].audio[0].blob.type, "audio/mp4");
  assert.match(reportText(v), /TEST001_Hive_0001_01.m4a/);
  assert.equal(v.demo, false);
});
test("retains media without a saved hive form and flags it", async () => {
  const v = await parseVisitZip(
    await fixture((z) =>
      z.file("TEST001_Hive_0002/TEST001_Hive_0002_01.jpg", "x"),
    ),
  );
  assert.equal(v.hives.length, 2);
  assert.ok(issues(v.hives[1]).includes("Missing inspection form"));
});
test("rejects mixed visits without partially importing", async () => {
  await assert.rejects(
    () =>
      fixture((z) =>
        z.file(
          "other/form.json",
          JSON.stringify({ ...record, pin: "OTHER", sequence_number: 2 }),
        ),
      ).then(parseVisitZip),
    /same PIN/,
  );
});
test("rejects malformed JSON", async () => {
  await assert.rejects(
    () => fixture((z) => z.file("session.json", "{")).then(parseVisitZip),
    /Cannot read form/,
  );
});
test("rejects impossible date", async () => {
  await assert.rejects(
    () =>
      fixture((z) =>
        z.file(
          "session.json",
          JSON.stringify({ ...record, visit_date: "2026-02-31" }),
        ),
      ).then(parseVisitZip),
    /Invalid visit date/,
  );
});
test("rejects traversal paths before decoding", async () => {
  await assert.rejects(
    () => fixture((z) => z.file("../unsafe.jpg", "x")).then(parseVisitZip),
    /unsafe path/,
  );
});
test("rejects unrecognized exports", async () => {
  const z = new JSZip();
  z.file("notes.txt", "hi");
  const b = new Blob([await z.generateAsync({ type: "uint8array" })]);
  await assert.rejects(() => parseVisitZip(b), /Missing valid PIN/);
});
test("escapes untrusted record text before HTML rendering", () =>
  assert.equal(
    escapeHtml('<img src=x onerror="alert(1)">'),
    "&lt;img src=x onerror=&quot;alert(1)&quot;&gt;",
  ));
