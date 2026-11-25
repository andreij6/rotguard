#!/usr/bin/env node
// Renders the permission tutorial videos in scene.html to app/src/main/res/raw[-<lang>]/<variant>.mp4 (540x960, narrated).
//   node tools/tutorials/render.mjs                           all variants, English (res/raw)
//   node tools/tutorials/render.mjs tutorial_overlay           just these
//   node tools/tutorials/render.mjs --lang es,ar               these languages (or --lang all); es -> res/raw-es, zh-CN -> res/raw-zh-rCN
//   node tools/tutorials/render.mjs --preview --lang ar        no TTS: one PNG per step into $TMPDIR/rotguard-preview/ to check layout
// Narration: Gemini TTS (Interactions API, same setup as leet-clone/tools/video/tts/say.py), cached in
// tools/tutorials/tts-cache by text hash, so re-renders after visual tweaks cost no API calls.
// Needs Google Chrome, ffmpeg/ffprobe, Node 22+, GEMINI_API_KEY in the env or ~/.env.
import { spawn, execFileSync } from "node:child_process";
import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(HERE, "../..");
const RES = path.join(ROOT, "app/src/main/res");
const CACHE = path.join(HERE, "tts-cache");
const CHROME = process.env.CHROME || "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
const FPS = 24, W = 360, H = 640, SCALE = 1.5, LEAD_MS = 400;
// "lateinit Story Narrator": warm, unhurried educator voice (leet-clone make_voice.py). Created as an en-US persona, but a
// TTS -> Gemini transcription check (Nov 2026) rated its es/zh/hi/ar native with no English accent, so all languages share it.
// Swap a language to a prebuilt voice (e.g. "Kore") here if a native speaker hears otherwise.
const V = process.env.TUTORIAL_VOICE || "voice_ucl45l2nnhlu";
const VOICES = { en: V, es: V, "zh-CN": V, hi: V, ar: V };
const LANGS = Object.keys(VOICES);
const STYLE = "friendly, calm, reassuring help-guide narrator walking someone through phone settings, clear and unhurried";
const MODELS = ["gemini-3.8-flash-tts", "gemini-3.8-flash-lite-tts"]; // lite has its own daily quota
const sleep = ms => new Promise(r => setTimeout(r, ms));
const secs = f => Number(execFileSync("ffprobe", ["-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", f]).toString());
const exited = p => new Promise((res, rej) => p.on("exit", c => (c === 0 ? res() : rej(new Error(`exit ${c}`)))));

function geminiKey() {
  if (process.env.GEMINI_API_KEY) return process.env.GEMINI_API_KEY;
  const line = fs.readFileSync(path.join(os.homedir(), ".env"), "utf8").split("\n").find(l => l.startsWith("GEMINI_API_KEY="));
  if (!line) throw new Error("GEMINI_API_KEY not in env or ~/.env");
  return line.split("=", 2)[1].trim().replace(/^["']|["']$/g, "");
}

let lastCall = 0, model = 0, calls = 0;
async function tts(text, VOICE) {
  const file = path.join(CACHE, `${crypto.createHash("sha256").update([VOICE, STYLE, text].join("\n")).digest("hex").slice(0, 16)}.wav`);
  if (fs.existsSync(file)) return file;
  for (let attempt = 1; ; attempt++) {
    await sleep(Math.max(0, lastCall + 6500 - Date.now())); // 10 requests/min tier limit
    lastCall = Date.now();
    console.log(`  tts call ${++calls} (${MODELS[model]}, ${VOICE}): ${text.slice(0, 50)}`);
    const body = { model: MODELS[model], input: [{ type: "user_input", content: [{ type: "text", text,
      annotations: [{ type: "speech_metadata", style: STYLE }] }] }], response_format: { type: "audio" },
      generation_config: { speech_config: [{ voice: VOICE }] } };
    const r = await fetch("https://generativelanguage.googleapis.com/v1beta/interactions", { method: "POST",
      headers: { "Content-Type": "application/json", "x-goog-api-key": geminiKey() }, body: JSON.stringify(body) });
    const d = await r.json().catch(() => ({}));
    const audio = d.steps?.flatMap(s => s.content ?? []).find(c => c.type === "audio")?.data;
    if (r.ok && audio) {
      fs.mkdirSync(CACHE, { recursive: true });
      fs.writeFileSync(file, Buffer.from(audio, "base64"));
      return file;
    }
    const msg = JSON.stringify(d.error ?? d).slice(0, 300);
    if (/per day/i.test(msg) && model < MODELS.length - 1) { model++; console.warn(`  daily quota hit, falling back to ${MODELS[model]}`); continue; }
    if (attempt >= 4) throw new Error(`tts ${r.status}: ${msg}`);
    console.warn(`  tts ${r.status}, retry ${attempt}: ${msg}`);
    await sleep(attempt * 10000);
  }
}

// Minimal DevTools protocol client over the global WebSocket.
function cdp(url) {
  const ws = new WebSocket(url), calls = new Map(), waits = [];
  let n = 0;
  ws.onmessage = ({ data }) => {
    const m = JSON.parse(data);
    if (m.id) { const c = calls.get(m.id); calls.delete(m.id); m.error ? c.rej(new Error(m.error.message)) : c.res(m.result); }
    else {
      if (m.method === "Runtime.exceptionThrown") console.error("page error:", m.params.exceptionDetails.exception?.description);
      for (const w of waits.splice(0)) w.method === m.method ? w.res(m.params) : waits.push(w);
    }
  };
  const send = (method, params = {}) => new Promise((res, rej) => { calls.set(++n, { res, rej }); ws.send(JSON.stringify({ id: n, method, params })); });
  const once = method => new Promise(res => waits.push({ method, res }));
  return new Promise((res, rej) => { ws.onopen = () => res({ send, once, close: () => ws.close() }); ws.onerror = rej; });
}

const argv = process.argv.slice(2), flag = n => { const i = argv.indexOf(n); return i < 0 ? null : argv.splice(i, n === "--lang" ? 2 : 1)[1] ?? true; };
const PREVIEW = flag("--preview"), langArg = flag("--lang") || "en";
const langs = langArg === "all" ? LANGS : langArg.split(",");
for (const l of langs) if (!VOICES[l]) throw new Error(`unknown language ${l} (have ${LANGS.join(", ")})`);
const outDir = lang => path.join(RES, lang === "en" ? "raw" : `raw-${lang.replace("-", "-r")}`);

const tmp = fs.mkdtempSync(path.join(os.tmpdir(), "rotguard-tutorial-"));
const chrome = spawn(CHROME, ["--headless=new", "--remote-debugging-port=0", `--user-data-dir=${tmp}`, "--force-color-profile=srgb",
  "--hide-scrollbars", "--no-first-run", "--no-default-browser-check", "about:blank"], { stdio: "ignore" });
try {
  let port;
  for (let i = 0; i < 150 && !port; i++) {
    try { port = Number(fs.readFileSync(path.join(tmp, "DevToolsActivePort"), "utf8").split("\n")[0]) || undefined; } catch {}
    if (!port) await sleep(100);
  }
  if (!port) throw new Error("Chrome did not write DevToolsActivePort");
  const page = (await (await fetch(`http://127.0.0.1:${port}/json/list`)).json()).find(t => t.type === "page");
  const { send, once, close } = await cdp(page.webSocketDebuggerUrl);
  const ev = async expression => {
    const r = await send("Runtime.evaluate", { expression, returnByValue: true, awaitPromise: true });
    if (r.exceptionDetails) throw new Error(r.exceptionDetails.exception?.description || r.exceptionDetails.text);
    return r.result.value;
  };
  await send("Page.enable");
  await send("Runtime.enable");
  await send("Emulation.setDeviceMetricsOverride", { width: W, height: H, deviceScaleFactor: SCALE, mobile: false });
  const loaded = once("Page.loadEventFired");
  await send("Page.navigate", { url: pathToFileURL(path.join(HERE, "scene.html")).href });
  await loaded;
  await ev("document.fonts.ready.then(() => true)");

  for (const lang of langs) {
  const all = await ev(`setLang(${JSON.stringify(lang)})`);
  const ids = argv.length ? argv : Object.keys(all);
  const OUT = PREVIEW ? path.join(os.tmpdir(), "rotguard-preview") : outDir(lang);
  fs.mkdirSync(OUT, { recursive: true });
  for (const id of ids) {
    if (!all[id]) throw new Error(`unknown variant ${id}`);
    if (PREVIEW) {
      const T = await ev(`setup(${JSON.stringify(id)}, null)`);
      for (const [i, s] of T.steps.entries()) {
        await ev(`seek(${(s.start + s.dur - 0.3) * 1000})`);
        const { data } = await send("Page.captureScreenshot", { format: "png" });
        fs.writeFileSync(path.join(OUT, `${lang}_${id}_${i + 1}.png`), Buffer.from(data, "base64"));
      }
      continue;
    }
    console.log(`${lang} ${id}: narrating ${all[id].length} steps`);
    const clips = [];
    for (const say of all[id]) clips.push(await tts(say, VOICES[lang]));
    const speech = clips.map(secs);
    const T = await ev(`setup(${JSON.stringify(id)}, ${JSON.stringify(speech)})`);

    // Audio: each step = 400 ms lead + its clip, padded to the step length; then concat and normalise.
    const wav = path.join(tmp, `${lang}_${id}.wav`);
    const filters = T.steps.map((s, i) =>
      `[${i}:a]aresample=44100,aformat=channel_layouts=mono,adelay=${LEAD_MS},apad=whole_dur=${s.dur.toFixed(4)}[s${i}]`);
    filters.push(`${T.steps.map((_, i) => `[s${i}]`).join("")}concat=n=${T.steps.length}:v=0:a=1,loudnorm=I=-16:TP=-1.5:LRA=11[out]`);
    execFileSync("ffmpeg", ["-y", "-loglevel", "error", ...clips.flatMap(c => ["-i", c]), "-filter_complex", filters.join(";"),
      "-map", "[out]", "-ar", "44100", wav]);

    const frames = Math.ceil(T.duration * FPS);
    const mp4 = path.join(OUT, `${id}.mp4`);
    const ff = spawn("ffmpeg", ["-y", "-loglevel", "error", "-f", "image2pipe", "-framerate", `${FPS}`, "-c:v", "png", "-i", "-",
      "-i", wav, "-map", "0:v", "-map", "1:a", "-c:v", "libx264", "-profile:v", "main", "-preset", "slow", "-tune", "animation",
      "-crf", "27", "-pix_fmt", "yuv420p", "-r", `${FPS}`, "-c:a", "aac", "-b:a", "64k", "-ac", "1", "-shortest",
      "-movflags", "+faststart", mp4], { stdio: ["pipe", "inherit", "inherit"] });
    const done = exited(ff);
    for (let f = 0; f < frames; f++) {
      await ev(`seek(${(f * 1000) / FPS})`);
      const { data } = await send("Page.captureScreenshot", { format: "png", optimizeForSpeed: true });
      if (!ff.stdin.write(Buffer.from(data, "base64"))) await new Promise(r => ff.stdin.once("drain", r));
    }
    ff.stdin.end();
    await done;
    console.log(`  -> ${path.relative(ROOT, mp4)}: ${T.duration.toFixed(1)} s, ${(fs.statSync(mp4).size / 1024).toFixed(0)} KB`);
  }
  if (PREVIEW) console.log(`previews in ${OUT}`);
  }
  console.log(`${calls} TTS API calls this run`);
  close();
} finally {
  chrome.kill();
  await new Promise(r => (chrome.exitCode !== null ? r() : chrome.once("exit", r)));
  fs.rmSync(tmp, { recursive: true, force: true });
}
