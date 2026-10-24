const state = {
  sequence: 1,
  photos: 0,
  voiceNotes: 0,
};

const el = {
  pin: document.querySelector("#pin"),
  group: document.querySelector("#group"),
  hiveLabel: document.querySelector("#hiveLabel"),
  seqValue: document.querySelector("#seqValue"),
  prevSeq: document.querySelector("#prevSeq"),
  nextSeq: document.querySelector("#nextSeq"),
  photoChip: document.querySelector("#photoChip"),
  voiceChip: document.querySelector("#voiceChip"),
  nextFile: document.querySelector("#nextFile"),
  photoButton: document.querySelector("#photoButton"),
  voiceButton: document.querySelector("#voiceButton"),
  saveNext: document.querySelector("#saveNext"),
};

function clean(value) {
  const cleaned = value.trim().replace(/[^A-Za-z0-9_-]+/g, "_").replace(/^_+|_+$/g, "");
  return cleaned || "UNKNOWN";
}

function padded(value, width) {
  return String(value).padStart(width, "0");
}

function render() {
  const seq = padded(state.sequence, 4);
  const nextPhoto = padded(state.photos + 1, 2);
  const pin = clean(el.pin.value);
  const group = clean(el.group.value);

  el.hiveLabel.textContent = `Hive ${seq}`;
  el.seqValue.textContent = seq;
  el.photoChip.textContent = `${state.photos} photos`;
  el.voiceChip.textContent = `${state.voiceNotes} voice notes`;
  el.nextFile.textContent = `${pin}_${group}_${seq}_${nextPhoto}.jpg`;
}

el.prevSeq.addEventListener("click", () => {
  state.sequence = Math.max(1, state.sequence - 1);
  render();
});

el.nextSeq.addEventListener("click", () => {
  state.sequence += 1;
  render();
});

el.photoButton.addEventListener("click", () => {
  state.photos += 1;
  render();
});

el.voiceButton.addEventListener("click", () => {
  state.voiceNotes += 1;
  render();
});

el.saveNext.addEventListener("click", () => {
  state.sequence += 1;
  state.photos = 0;
  state.voiceNotes = 0;
  render();
  window.scrollTo({ top: 0, behavior: "smooth" });
});

document.querySelectorAll(".options").forEach((group) => {
  group.addEventListener("click", (event) => {
    if (!(event.target instanceof HTMLButtonElement)) return;
    group.querySelectorAll("button").forEach((button) => button.classList.remove("selected"));
    event.target.classList.add("selected");
  });
});

el.pin.addEventListener("input", render);
el.group.addEventListener("input", render);
render();
