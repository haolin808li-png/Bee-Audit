import JSZip from "jszip";
// A bounded, local-only reader for Android's exportVisitZip format.
export async function parseVisitZip(file, onProgress = () => {}) {
  if (file.size > 100 * 1024 * 1024)
    throw Error("ZIP exceeds 100 MB. Export one visit at a time.");
  const bytes = file instanceof Blob ? await file.arrayBuffer() : file;
  const zip = await JSZip.loadAsync(bytes);
  const entries = Object.values(zip.files).filter(
    (e) =>
      !e.dir &&
      !e.name.includes("__MACOSX") &&
      !e.name.split("/").pop().startsWith("."),
  );
  if (entries.length > 1500)
    throw Error("ZIP exceeds 1500 files. Split the export.");
  let total = 0;
  for (const e of entries) {
    const path = e.unsafeOriginalName || e.name;
    if (
      path.split("/").includes("..") ||
      path.startsWith("/") ||
      path.includes("\\")
    )
      throw Error("ZIP contains an unsafe path.");
    // JSZip exposes the declared size before inflation; also check actual bytes below.
    const size = e._data?.uncompressedSize;
    if (typeof size !== "number" || size > 40 * 1024 * 1024)
      throw Error("A file exceeds 40 MB or the ZIP format is unsupported.");
    total += size;
    if (total > 200 * 1024 * 1024)
      throw Error("Expanded ZIP exceeds 200 MB. Split the export.");
  }
  const json = new Map();
  for (const e of entries.filter((e) => e.name.endsWith(".json"))) {
    if (e._data.uncompressedSize > 1024 * 1024)
      throw Error("A form exceeds 1 MB.");
    try {
      json.set(e.name, JSON.parse(await e.async("string")));
    } catch {
      throw Error(`Cannot read form: ${e.name}`);
    }
  }
  const records = [...json.entries()].filter(
    ([name, r]) =>
      !name.endsWith("session.json") &&
      r &&
      typeof r === "object" &&
      Number.isInteger(r.sequence_number),
  );
  const session = [...json.entries()].find(([name]) =>
    name.endsWith("session.json"),
  )?.[1];
  const meta = session || records[0]?.[1];
  if (
    !meta ||
    typeof meta.pin !== "string" ||
    !meta.pin.trim() ||
    !/^\d{4}-\d{2}-\d{2}$/.test(meta.visit_date)
  )
    throw Error(
      "Missing valid PIN or visit date. Export a visit from the current Bee Audit app.",
    );
  const { pin, visit_date: date } = meta;
  const dateObj = new Date(`${date}T12:00:00Z`);
  if (
    Number.isNaN(dateObj.getTime()) ||
    dateObj.toISOString().slice(0, 10) !== date
  )
    throw Error("Invalid visit date.");
  const hives = new Map();
  for (const [path, r] of records) {
    if (r.pin !== pin || r.visit_date !== date)
      throw Error("A ZIP must contain the same PIN and visit date.");
    if (r.sequence_number < 1 || typeof r.group !== "string")
      throw Error("Invalid hive number or group.");
    const folder = path.slice(0, path.lastIndexOf("/"));
    if (hives.has(folder))
      throw Error("Duplicate forms in the same hive folder.");
    hives.set(folder, {
      id: folder,
      sequence: r.sequence_number,
      group: r.group,
      record: true,
      gps:
        r.gps && Number.isFinite(r.gps.lat) && Number.isFinite(r.gps.lon)
          ? r.gps
          : null,
      form: r.form || {},
      photos: [],
      audio: [],
    });
  }
  let completed = 0;
  const media = entries.filter((e) =>
    /\.(jpg|jpeg|png|m4a|mp3|wav)$/i.test(e.name),
  );
  for (const e of media) {
    const folder = e.name.slice(0, e.name.lastIndexOf("/"));
    if (!hives.has(folder)) {
      const match = folder
        .split("/")
        .pop()
        .match(/_(\d+)$/);
      if (!match) throw Error(`Cannot associate media with a hive: ${e.name}`);
      hives.set(folder, {
        id: folder,
        sequence: Number(match[1]),
        group: "Hive",
        record: false,
        gps: null,
        form: {},
        photos: [],
        audio: [],
      });
    }
    const photo = /\.(jpg|jpeg|png)$/i.test(e.name);
    const type = photo
      ? e.name.toLowerCase().endsWith(".png")
        ? "image/png"
        : "image/jpeg"
      : e.name.toLowerCase().endsWith(".mp3")
        ? "audio/mpeg"
        : e.name.toLowerCase().endsWith(".wav")
          ? "audio/wav"
          : "audio/mp4";
    const blob = new Blob([await e.async("uint8array")], { type });
    hives
      .get(folder)
      [
        photo ? "photos" : "audio"
      ].push({ name: e.name.split("/").pop(), blob });
    onProgress(++completed, media.length);
  }
  if (!hives.size) throw Error("No hive forms or media found in ZIP.");
  return {
    id: `local-${pin}-${date}`,
    pin,
    date,
    title: pin,
    demo: false,
    importedAt: new Date().toISOString(),
    hives: [...hives.values()].sort(
      (a, b) => a.group.localeCompare(b.group) || a.sequence - b.sequence,
    ),
  };
}
