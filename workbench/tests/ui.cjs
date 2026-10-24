const { chromium, webkit } = require("../../preview/node_modules/playwright");
const JSZip = require("../node_modules/jszip");
const fs = require("node:fs/promises");
const assert = require("node:assert/strict");
const path = require("node:path");

async function run() {
  const out = path.resolve(__dirname, "../test-results");
  await fs.mkdir(out, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  try {
    const context = await browser.newContext({
      viewport: { width: 1440, height: 1100 },
    });
    const page = await context.newPage();
    const errors = [];
    page.on("pageerror", (e) => errors.push(e.message));
    await page.goto("http://127.0.0.1:4173/#visits");
    await page.locator("h1").filter({ hasText: "Visits" }).waitFor();
    await page.waitForFunction(() =>
      [...document.images].every((i) => i.complete && i.naturalWidth > 0),
    );
    await page.screenshot({
      path: path.join(out, "desktop.png"),
      fullPage: true,
    });
    await page.locator("#search").fill("not-a-pin");
    await page.getByText("No matching visits").waitFor();
    await page.getByRole("button", { name: "Clear filters" }).click();
    await page.locator('[data-action="photo"]').first().click();
    await page.locator("dialog[open] .lightbox").waitFor();
    await page.getByRole("button", { name: "Next photo" }).click();
    await page.getByRole("button", { name: "Close photo" }).click();
    await page.locator('[data-action="nav-reports"]').click();
    await page.locator("#report-notes").fill("Please inspect the lid.");
    await page.reload();
    await page.locator('[data-action="nav-reports"]').click();
    assert.equal(
      await page.locator("#report-notes").inputValue(),
      "Please inspect the lid.",
    );
    const download = page.waitForEvent("download");
    await page.locator('[data-action="download"]').click();
    assert.match((await download).suggestedFilename(), /_draft.md$/);
    await page.screenshot({
      path: path.join(out, "report.png"),
      fullPage: true,
    });
    await page.locator('[data-action="nav-visits"]').first().click();
    const zip = new JSZip();
    const record = {
      pin: "UI_TEST",
      visit_date: "2026-10-25",
      group: "Hive",
      sequence_number: 1,
      gps: { lat: -37.78, lon: 176.32 },
      form: {
        condition: "Good",
        activity: "High",
        notes: '<img src=x onerror="alert(1)"> Real imported notes',
      },
    };
    zip.file(
      "session.json",
      JSON.stringify({
        pin: record.pin,
        visit_date: record.visit_date,
        group: "Hive",
      }),
    );
    zip.file(
      "UI_TEST_Hive_0001/UI_TEST_Hive_0001.json",
      JSON.stringify(record),
    );
    zip.file(
      "UI_TEST_Hive_0001/UI_TEST_Hive_0001_01.jpg",
      await fs.readFile(
        path.resolve(__dirname, "../public/assets/orchard.jpg"),
      ),
    );
    const wav = Buffer.alloc(44 + 16000);
    wav.write("RIFF");
    wav.writeUInt32LE(wav.length - 8, 4);
    wav.write("WAVEfmt ", 8);
    wav.writeUInt32LE(16, 16);
    wav.writeUInt16LE(1, 20);
    wav.writeUInt16LE(1, 22);
    wav.writeUInt32LE(8000, 24);
    wav.writeUInt32LE(16000, 28);
    wav.writeUInt16LE(2, 32);
    wav.writeUInt16LE(16, 34);
    wav.write("data", 36);
    wav.writeUInt32LE(16000, 40);
    zip.file("UI_TEST_Hive_0001/UI_TEST_Hive_0001_01.wav", wav);
    const payload = {
      name: "UI_TEST_2026-10-25.zip",
      mimeType: "application/zip",
      buffer: await zip.generateAsync({ type: "nodebuffer" }),
    };
    await page.locator("#zip-input").setInputFiles(payload);
    await page
      .locator(".detail-title h2")
      .filter({ hasText: "UI_TEST" })
      .waitFor();
    assert.equal(await page.locator('img[src="x"]').count(), 0);
    await page.reload();
    await page
      .locator(".detail-title h2")
      .filter({ hasText: "UI_TEST" })
      .waitFor();
    await page.locator('[data-action="tab-media"]').click();
    await page.locator("audio").waitFor();
    await page.locator("audio").evaluate(async (a) => {
      await a.play();
    });
    await page.waitForFunction(
      () => document.querySelector("audio").currentTime > 0,
    );
    await page.locator('[data-action="photo"]').first().click();
    await page.waitForFunction(
      () => document.querySelector(".lightbox")?.naturalWidth > 0,
    );
    await page.getByRole("button", { name: "Close photo" }).click();
    await page.locator("#zip-input").setInputFiles(payload);
    await page.getByText("Visit already imported.", { exact: false }).waitFor();
    await page.locator('dialog [data-action="close"]').click();
    await page.locator("#zip-input").setInputFiles({
      name: "bad.zip",
      mimeType: "application/zip",
      buffer: Buffer.from("not a zip"),
    });
    await page.getByText("Import not completed").waitFor();
    await page.locator('dialog [data-action="close"]').click();
    await page.setViewportSize({ width: 390, height: 844 });
    await page.locator('[data-action="tab-hives"]').click();
    await page.screenshot({
      path: path.join(out, "mobile.png"),
      fullPage: true,
    });
    assert.ok(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
      "mobile body overflows",
    );
    for (const route of [
      "dashboard",
      "reports",
      "imports",
      "settings",
      "agents",
      "analytics",
      "team",
      "activity",
    ]) {
      await page.locator(`[data-action="nav-${route}"]`).first().click();
      assert.ok(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
        `${route} overflows`,
      );
    }
    assert.deepEqual(errors, []);
    console.log(
      "PASS Chromium: import, persistence, photo navigation, audio playback, escaping, duplicate/invalid ZIP, report notes/download, mobile layouts.",
    );
    await context.close();
  } finally {
    await browser.close();
  }
  // Safari's engine, where installed. Missing binaries are reported, never counted as a pass.
  let safari;
  try {
    safari = await webkit.launch({ headless: true });
  } catch (e) {
    console.log("WebKit not available:", e.message.split("\n")[0]);
    return;
  }
  try {
    const p = await safari.newPage();
    await p.goto("http://127.0.0.1:4173/#visits");
    await p.locator("h1").filter({ hasText: "Visits" }).waitFor();
    await p.locator('[data-action="nav-reports"]').click();
    await p.locator(".report-sheet").waitFor();
    console.log("PASS WebKit: visits and report rendering.");
  } finally {
    await safari.close();
  }
}
run().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
