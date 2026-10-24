function database() {
  return new Promise((resolve, reject) => {
    const r = indexedDB.open("bee-workbench", 1);
    r.onupgradeneeded = () =>
      r.result.createObjectStore("visits", { keyPath: "id" });
    r.onsuccess = () => resolve(r.result);
    r.onerror = () => reject(r.error);
  });
}
export async function saveVisit(visit) {
  const db = await database();
  return new Promise((resolve, reject) => {
    const tx = db.transaction("visits", "readwrite");
    tx.objectStore("visits").put(visit);
    tx.oncomplete = () => {
      db.close();
      resolve();
    };
    tx.onerror = () => {
      db.close();
      reject(tx.error);
    };
  });
}
export async function loadVisits() {
  const db = await database();
  return new Promise((resolve, reject) => {
    const tx = db.transaction("visits", "readonly");
    const req = tx.objectStore("visits").getAll();
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
    tx.oncomplete = () => db.close();
  });
}
