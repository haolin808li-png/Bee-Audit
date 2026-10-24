const { chromium, webkit } = require("../../preview/node_modules/playwright");
const assert = require("node:assert/strict");
const fs = require("node:fs/promises");
const path = require("node:path");
const out = path.resolve(__dirname, "../test-results");
async function run() {
  await fs.mkdir(out, { recursive: true });
  for (const engine of [chromium, webkit]) {
    const browser = await engine.launch({ headless: true });
    try {
      const page = await browser.newPage({
        viewport: { width: 1440, height: 1050 },
      });
      const errors = [],
        external = [];
      page.on("pageerror", (e) => errors.push(e.message));
      page.on("request", (r) => {
        if (
          !r.url().startsWith("http://127.0.0.1:4173") &&
          !r.url().startsWith("blob:") &&
          !r.url().startsWith("data:")
        )
          external.push(r.url());
      });
      await page.goto("http://127.0.0.1:4173/");
      await page.locator("h1").filter({ hasText: "Overview" }).waitFor();
      await page.screenshot({
        path: path.join(out, `${engine.name()}-overview.png`),
        fullPage: true,
      });
      const nav = async (x) =>
        page.locator(`[data-action="nav-${x}"]`).first().click();
      await nav("team");
      await page.locator('[name="name"]').fill("Dave");
      await page.locator('[name="email"]').fill("dave@example.test");
      await page.getByRole("button", { name: "Add person" }).click();
      await page.getByRole("cell", { name: "Dave", exact: true }).waitFor();
      await nav("visits");
      await page.locator("#visit-owner").selectOption({ label: "Dave" });
      await page.locator("#visit-status").selectOption("In review");
      await page.reload();
      assert.equal(
        await page.locator("#visit-status").inputValue(),
        "In review",
      );
      assert.equal(
        await page.locator("#visit-owner option:checked").textContent(),
        "Dave",
      );
      await nav("settings");
      await page.locator('[name="fail"]').check();
      await page.getByRole("button", { name: "Save preferences" }).click();
      await nav("agents");
      await page.locator('[data-action="run-visit"]').click();
      await page.locator('[data-action="run-pause"]').click();
      const progress = await page.locator("progress").getAttribute("value");
      await page.waitForTimeout(2100);
      assert.equal(
        await page.locator("progress").getAttribute("value"),
        progress,
      );
      await page.reload();
      await page.locator('[data-action="run-resume"]').waitFor();
      await page.locator('[data-action="run-resume"]').click();
      await page
        .locator('[data-action="run-retry"]')
        .waitFor({ timeout: 12000 });
      await page
        .getByText("Simulated provider timeout.", { exact: false })
        .waitFor();
      await page.screenshot({
        path: path.join(out, `${engine.name()}-agent-failed.png`),
        fullPage: true,
      });
      await page.locator('[data-action="run-retry"]').click();
      await page
        .locator('[data-action="run-report"]')
        .waitFor({ timeout: 16000 });
      await page.screenshot({
        path: path.join(out, `${engine.name()}-agent.png`),
        fullPage: true,
      });
      await page.locator('[data-action="run-report"]').click();
      await page
        .locator("#report-notes")
        .fill("Reviewed against original images. Arrange lid inspection.");
      await page.locator('[data-action="review"]').click();
      await page.locator('[data-action="confirm-review"]').click();
      assert.equal(
        await page.locator("dialog[open]").count(),
        1,
        "must require confirmation",
      );
      await page.locator("#review-confirm").check();
      await page.locator('[data-action="confirm-review"]').click();
      await page
        .locator(".report-side .badge")
        .filter({ hasText: "Reviewed" })
        .waitFor();
      await nav("visits");
      assert.equal(
        await page.locator("#visit-status").inputValue(),
        "Reviewed",
      );
      await nav("analytics");
      await page.screenshot({
        path: path.join(out, `${engine.name()}-analytics.png`),
        fullPage: true,
      });
      await page.locator("#analytics-scope").selectOption("imported");
      assert.equal(
        await page.locator(".stats .stat strong").first().textContent(),
        "0",
      );
      const dl = page.waitForEvent("download");
      await page.locator('[data-action="export-csv"]').click();
      assert.match((await dl).suggestedFilename(), /\.csv$/);
      await nav("activity");
      await page
        .getByText("Started simulated run for KPIN1024.", { exact: true })
        .waitFor();
      const logs = page.waitForEvent("download");
      await page.locator('[data-action="export-log"]').click();
      assert.match((await logs).suggestedFilename(), /\.json$/);
      await nav("agents");
      await page.locator('[data-action="run-visit"]').click();
      await page.locator('[data-action="run-cancel"]').click();
      await page.locator('[data-action="run-retry"]').waitFor();
      for (const width of [1440, 768, 390, 360]) {
        await page.setViewportSize({ width, height: 900 });
        for (const route of [
          "dashboard",
          "visits",
          "agents",
          "reports",
          "analytics",
          "team",
          "activity",
          "imports",
          "settings",
        ]) {
          await nav(route);
          assert.ok(
            await page.evaluate(
              () => document.documentElement.scrollWidth <= innerWidth,
            ),
            `${engine.name()} ${route} overflows at ${width}`,
          );
          assert.doesNotMatch(
            await page.locator("body").innerText(),
            /[\u3400-\u9fff]/,
            `${route} is not English`,
          );
        }
        if (width === 390) {
          await nav("agents");
          await page.screenshot({
            path: path.join(out, `${engine.name()}-mobile-agent.png`),
            fullPage: true,
          });
        }
      }
      assert.deepEqual(errors, []);
      assert.deepEqual(external, [], "No external service calls expected");
      console.log(
        `PASS ${engine.name()}: assignments, review status, simulation pause/reload/fail/retry/cancel, human confirmation, CSV/log exports, English-only UI, 4 viewports, zero external requests.`,
      );
    } finally {
      await browser.close();
    }
  }
}
run().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
