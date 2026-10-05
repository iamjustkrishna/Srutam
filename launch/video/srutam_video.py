#!/usr/bin/env python3
"""Srutam launch-video pipeline: one command per repetitive step.

Every step that used to be done by hand (and cost tokens) lives here:
ElevenLabs generation, music-bed building, Remotion renders, the audio mix,
loudness checks and review contact sheets.

    python3 srutam_video.py credits                      # ElevenLabs credits left
    python3 srutam_video.py voice [--audition]           # narration (+ word timings) for every scene
    python3 srutam_video.py sfx [name ...]               # sound effects from video.config.json
    python3 srutam_video.py music gen <preset> [--seconds 20]   # new seamless loop -> public/music-samples/
    python3 srutam_video.py music use <preset>           # loop it to the video length -> public/music.wav
    python3 srutam_video.py music arrange <preset>       # score it to the edit (drop, breakdown, build) -> public/music.wav
    python3 srutam_video.py music previews               # 32s previews of every sample -> ../music/
    python3 srutam_video.py stills [--fmt 16x9] [--at 10,20,30 | --scenes]   # contact sheet for review
    python3 srutam_video.py render [--fmt 16x9,9x16] [--audio-only] [--suffix -funk]         # final MP4s in launch/
    python3 srutam_video.py social [x ig linkedin] [--audio-only]  # ~20s platform cuts -> launch/social/
    python3 srutam_video.py check                        # duration + loudness of the final files
    python3 srutam_video.py timeline                     # scene start times (seconds)
    python3 srutam_video.py editor                       # visual editor (timeline, cues, music, narration) in your browser

The API key comes from $ELEVENLABS_API_KEY or launch/video/.env (never committed).
"""
from __future__ import annotations

import argparse
import base64
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent
PUB = ROOT / "public"
OUT = ROOT / "out"
CFG = json.loads((ROOT / "video.config.json").read_text())
MANIFEST = PUB / "vo" / "manifest.json"
API = "https://api.elevenlabs.io/v1"


# ---------------------------------------------------------------- helpers
def sh(cmd: list[str], **kw) -> str:
    """Run a command, fail loudly, return stdout."""
    r = subprocess.run(cmd, cwd=kw.pop("cwd", ROOT), capture_output=True, text=True, **kw)
    if r.returncode != 0:
        sys.exit(f"command failed: {' '.join(cmd[:4])}...\n{r.stderr[-1500:]}")
    return r.stdout


def key() -> str:
    k = os.environ.get("ELEVENLABS_API_KEY")
    if not k and (ROOT / ".env").exists():
        m = re.search(r"ELEVENLABS_API_KEY=(\S+)", (ROOT / ".env").read_text())
        k = m.group(1) if m else None
    if not k:
        sys.exit("No ElevenLabs key: set ELEVENLABS_API_KEY or create launch/video/.env")
    return k


def eleven(path: str, body: dict | None = None, want_json: bool = False):
    """POST (or GET when body is None) to ElevenLabs. urllib honours HTTPS_PROXY."""
    req = urllib.request.Request(API + path, data=None if body is None else json.dumps(body).encode(),
                                 headers={"xi-api-key": key(), "Content-Type": "application/json"},
                                 method="GET" if body is None else "POST")
    try:
        with urllib.request.urlopen(req, timeout=300) as r:
            data = r.read()
    except urllib.error.HTTPError as e:
        detail = e.read().decode(errors="replace")[:400]
        hint = " (Music API needs a paid plan; use `music gen`, which uses looping sound effects)" if "paid_plan" in detail else ""
        sys.exit(f"ElevenLabs {path} -> HTTP {e.code}: {detail}{hint}")
    except urllib.error.URLError as e:
        sys.exit(f"Cannot reach ElevenLabs ({e.reason}). Is api.elevenlabs.io allowed in the environment's network settings?")
    return json.loads(data) if want_json else data


def manifest() -> dict:
    return json.loads(MANIFEST.read_text()) if MANIFEST.exists() else {"scenes": {}}


def save_manifest(m: dict) -> None:
    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_text(json.dumps(m, indent=1))


def duration(f: Path) -> float:
    return float(sh(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(f)]).strip())


PLAN_PATH = ROOT / "plan.json"


def load_plan() -> dict:
    return json.loads(PLAN_PATH.read_text())


def save_plan(plan: dict) -> None:
    """Write plan.json (keeps one backup so a bad edit is never fatal)."""
    if PLAN_PATH.exists():
        (ROOT / "plan.backup.json").write_text(PLAN_PATH.read_text())
    PLAN_PATH.write_text(json.dumps(plan, indent=1, ensure_ascii=False) + "\n")


def scenes() -> list[dict]:
    """Scene ids + narration from plan.json (the single source of truth)."""
    return [{"id": s["id"], "vo": s["vo"]} for s in load_plan()["scenes"]]


def timeline() -> list[dict]:
    js = ("import('./src/timeline.ts').then(m=>{const fs=require('fs');let man;try{man=JSON.parse(fs.readFileSync('public/vo/manifest.json'))}catch{}"
          "const tl=m.layoutTimeline(man);console.log(JSON.stringify({tl,total:m.totalDur(man)}))})")
    out = sh(["node", "--no-warnings", "--experimental-strip-types", "-e", js])
    return json.loads(out.strip().splitlines()[-1])


# ---------------------------------------------------------------- commands
def cmd_credits(_):
    d = eleven("/user/subscription", want_json=True)
    print(f"{d['character_limit'] - d['character_count']} credits left ({d['tier']} tier, {d['character_count']}/{d['character_limit']} used)")
    print("rough costs: narration ~1 credit/char · sound effect 40 credits/second · 20s music loop = 800")


def words_from_alignment(al: dict) -> list[dict]:
    out, cur = [], None
    for ch, s, e in zip(al["characters"], al["character_start_times_seconds"], al["character_end_times_seconds"]):
        if ch.isspace():
            if cur:
                out.append(cur)
            cur = None
            continue
        cur = cur or {"w": "", "s": round(s, 3), "e": 0}
        cur["w"] += ch
        cur["e"] = round(e, 3)
    if cur:
        out.append(cur)
    return out


def cmd_voice(a):
    v = CFG["voice"]
    body = lambda text: {"text": text, "model_id": v["model_id"], "voice_settings": v["settings"]}
    if a.audition:
        line = next(s["vo"] for s in scenes() if s["vo"])
        for name, vid in v["auditions"].items():
            (PUB / "vo" / f"audition-{name}.mp3").write_bytes(eleven(f"/text-to-speech/{vid}?output_format=mp3_44100_128", body(line)))
            print("audition", name)
        return
    m = manifest()
    m["voice"] = v["voice_id"]
    todo = [s for s in scenes() if s["vo"] and (not a.only or s["id"] in a.only)]
    print(f"{sum(len(s['vo']) for s in todo)} characters of narration")
    for s in todo:
        r = eleven(f"/text-to-speech/{v['voice_id']}/with-timestamps?output_format=mp3_44100_128", body(s["vo"]), want_json=True)
        f = PUB / "vo" / f"{s['id']}.mp3"
        f.write_bytes(base64.b64decode(r["audio_base64"]))
        m["scenes"][s["id"]] = {"file": f"vo/{s['id']}.mp3", "dur": round(duration(f), 3), "words": words_from_alignment(r["alignment"]), "text": s["vo"]}
        print(f"vo {s['id']:<11} {m['scenes'][s['id']]['dur']:.2f}s")
    save_manifest(m)
    print("narration lengths changed -> run `music use <preset>` again, then `render`")


def cmd_sfx(a):
    m = manifest()
    m.setdefault("sfx", {})
    names = a.names or list(CFG["sfx"])
    for n in names:
        text, secs = CFG["sfx"][n]
        (PUB / "sfx").mkdir(exist_ok=True)
        (PUB / "sfx" / f"{n}.mp3").write_bytes(eleven("/sound-generation?output_format=mp3_44100_128",
                                                      {"text": text, "duration_seconds": secs, "prompt_influence": 0.6, "model_id": "eleven_text_to_sound_v2"}))
        m["sfx"][n] = f"sfx/{n}.mp3"
        print("sfx", n)
    save_manifest(m)


def cmd_music(a):
    samples = PUB / "music-samples"
    samples.mkdir(exist_ok=True)
    if a.action == "gen":
        prompt = a.prompt or CFG["music"]["presets"].get(a.name)
        if not prompt:
            sys.exit(f"no preset '{a.name}' in video.config.json; pass --prompt")
        (samples / f"{a.name}.mp3").write_bytes(eleven("/sound-generation?output_format=mp3_44100_128",
                                                       {"text": prompt, "duration_seconds": a.seconds, "loop": True, "prompt_influence": 0.55, "model_id": "eleven_text_to_sound_v2"}))
        print(f"music loop -> public/music-samples/{a.name}.mp3 (~{int(a.seconds * 40)} credits)")
    elif a.action in ("use", "arrange", "build"):
        plan = load_plan()
        if a.action != "build":
            plan["music"]["track"] = a.name
            plan["music"]["mode"] = "loop" if a.action == "use" else "arrange"
            save_plan(plan)
        build_music()
    elif a.action == "previews":
        dest = ROOT / ".." / "music"
        dest.mkdir(exist_ok=True)
        for i, f in enumerate(sorted(samples.glob("*.mp3")), 1):
            ln = min(32.0, duration(f) * 2)
            sh(["ffmpeg", "-loglevel", "error", "-y", "-stream_loop", "1", "-i", str(f), "-t", f"{ln:.2f}", "-af",
                f"afade=t=in:d=0.5,afade=t=out:st={ln - 2:.2f}:d=2,loudnorm=I=-16:TP=-1.5", "-b:a", "192k", str(dest / f"{i}-{f.stem}.mp3")])
            print("preview", f"{i}-{f.stem}.mp3")


def decode_loop(path: Path):
    """Decode a loop to stereo float PCM at 44.1 kHz. Uses the real audio length (not the mp3's padded
    duration), which is what keeps the beat in time when the loop repeats."""
    import numpy as np
    raw = subprocess.run(["ffmpeg", "-loglevel", "error", "-i", str(path), "-f", "f32le", "-ac", "2", "-ar", "44100", "-"], capture_output=True).stdout
    x = np.frombuffer(raw, dtype=np.float32).reshape(-1, 2).astype(np.float64)
    lead = np.argmax(np.abs(x).max(axis=1) > 1e-3)  # drop any encoder-priming silence
    return x[lead:] if 0 < lead < 4410 else x


def tile_loop(x, n: int, seam: float):
    """Repeat the loop to n samples with a short equal-power overlap at every seam (no gap, no click)."""
    import numpy as np
    sr = 44100
    cf = max(2, int(seam * sr))
    L = len(x)
    fade_in = np.sin(np.linspace(0, np.pi / 2, cf))[:, None]
    fade_out = np.cos(np.linspace(0, np.pi / 2, cf))[:, None]
    out = np.zeros((n + L + cf, 2))
    pos = 0
    while pos < n:
        seg = x.copy()
        if pos > 0:
            seg[:cf] *= fade_in
        seg[-cf:] *= fade_out
        out[pos:pos + L] += seg
        pos += L - cf
    return out[:n]


def build_music() -> None:
    """Build public/music.wav from plan.json: loop the chosen track to the video length, optionally scored
    to the edit (filter + level automation per scene). Costs no credits."""
    import numpy as np
    plan = load_plan()
    mu = plan["music"]
    src = PUB / "music-samples" / f"{mu['track']}.mp3"
    if not src.exists():
        sys.exit(f"{src} missing; run `music gen {mu['track']}` first")
    t = timeline()
    total, starts = t["total"], {s["id"]: s["start"] for s in t["tl"]}
    sr = 44100
    n = int(total * sr)
    x = tile_loop(decode_loop(src), n, mu.get("seam", 0.012))
    tt = np.arange(n) / sr
    if mu["mode"] == "arrange":
        # dark copy: FFT low-pass around 450 Hz, blended in/out per scene ("brightness")
        X = np.fft.rfft(x, axis=0)
        f = np.fft.rfftfreq(n, 1 / sr)
        X[f > 450] *= np.exp(-((f[f > 450] - 450) / 250))[:, None]
        dark = np.fft.irfft(X, n, axis=0)
        bright, level = np.zeros(n), np.ones(n)
        prev_b, prev_l = 0.0, 1.0
        sections = [e for e in plan["arrangement"] if e["scene"] in starts]
        for i, e in enumerate(sections):
            a0 = starts[e["scene"]]
            a1 = starts[sections[i + 1]["scene"]] if i + 1 < len(sections) else total
            seg = (tt >= a0) & (tt < a1)
            k = np.clip((tt[seg] - a0) / max(0.02, e.get("ramp", 0.35)), 0, 1)
            nb = prev_b if e.get("brightness") is None else e["brightness"]
            bright[seg] = prev_b + (nb - prev_b) * k
            level[seg] = prev_l + (e["level"] - prev_l) * k
            prev_b, prev_l = nb, e["level"]
        if sections and sections[0].get("sweep_to") is not None:  # slow filter sweep through the first section
            first = sections[0]["scene"]
            end = starts[sections[1]["scene"]] if len(sections) > 1 else total
            m = (tt >= starts[first]) & (tt < end)
            bright[m] = np.clip((tt[m] - starts[first]) / max(0.1, end - starts[first]), 0, 1) ** 2 * sections[0]["sweep_to"]
        x = (dark * (1 - bright)[:, None] + x * bright[:, None]) * level[:, None]
    x *= np.clip((total - tt) / mu["fade_out"], 0, 1)[:, None] * np.clip(tt / mu["fade_in"], 0, 1)[:, None]
    x = (x / (np.abs(x).max() + 1e-9) * 0.9).astype(np.float32)
    tmp = OUT / "bed.f32"
    OUT.mkdir(exist_ok=True)
    tmp.write_bytes(x.tobytes())
    sh(["ffmpeg", "-loglevel", "error", "-y", "-f", "f32le", "-ac", "2", "-ar", str(sr), "-i", str(tmp),
        "-af", f"loudnorm=I={mu.get('loudness', -16)}:TP=-1.5", "-ar", "44100", str(PUB / "music.wav")])
    tmp.unlink()
    m = manifest()
    m["music"] = "music.wav"
    save_manifest(m)
    print(f"music bed: {mu['track']} ({mu['mode']}) built for {total:.1f}s -> public/music.wav")


def cmd_stills(a):
    tl = timeline()
    times = [float(x) for x in a.at.split(",")] if a.at else [s["start"] + s["dur"] * 0.62 for s in tl["tl"]]
    comp = CFG["render"]["formats"][a.fmt]
    d = OUT / "stills"
    d.mkdir(parents=True, exist_ok=True)
    for old in d.glob(f"{comp}_*.jpeg"):
        old.unlink()
    sh(["node", "stills.mjs", comp, str(d), *[f"{t:.2f}" for t in times]])
    portrait = a.fmt == "9x16"
    cols = min(len(times), 6 if portrait else 4)
    rows = -(-len(times) // cols)
    sheet = OUT / f"sheet-{a.fmt}.jpg"
    sh(["ffmpeg", "-loglevel", "error", "-y", "-pattern_type", "glob", "-i", str(d / f"{comp}_*.jpeg"), "-vf",
        f"scale={'270:480' if portrait else '480:270'},tile={cols}x{rows}", "-frames:v", "1", str(sheet)])
    print(f"contact sheet ({len(times)} frames) -> {sheet}")


def mix_audio() -> Path:
    raw, norm = OUT / "mix.wav", OUT / "mix-norm.wav"
    comp = CFG["render"]["formats"]["16x9"]  # the audio is identical in every format
    sh(["npx", "remotion", "render", comp, str(raw), "--codec=wav", "--log=error"])
    mix = CFG["mix"]
    sh(["ffmpeg", "-loglevel", "error", "-y", "-i", str(raw), "-af",
        f"acompressor=threshold=-20dB:ratio=2.5:attack=5:release=120,loudnorm=I={mix['loudness_lufs']}:TP={mix['true_peak']}:LRA=9",
        "-ar", "48000", str(norm)])
    return norm


def picture_hash() -> str:
    """What the rendered picture depends on: scene list/lengths, voice lengths, captions."""
    import hashlib
    plan, man = load_plan(), manifest()
    key = {"scenes": [(s["id"], s["min"]) for s in plan["scenes"]], "captions": plan["captions"],
           "vo": {k: v.get("dur") for k, v in man.get("scenes", {}).items()}}
    return hashlib.sha1(json.dumps(key, sort_keys=True).encode()).hexdigest()[:12]


def stamp_picture() -> None:
    (OUT / "picture.hash").write_text(picture_hash())


def picture_stale() -> bool:
    f = OUT / "picture.hash"
    return (not f.exists()) or f.read_text().strip() != picture_hash()


def cmd_render(a):
    OUT.mkdir(exist_ok=True)
    fmts = a.fmt.split(",")
    print("building music bed from plan.json...")
    build_music()
    print("mixing audio (narration + sfx + music)...")
    audio = mix_audio()
    for f in fmts:
        comp = CFG["render"]["formats"][f]
        video = OUT / f"{comp}.mp4"
        if a.audio_only and video.exists():
            print(f"{f}: reusing existing picture ({video.name})")
            if picture_stale():
                print("   ! scenes, narration timing or captions changed since the last picture render: run a Full render to update the visuals")
        else:
            print(f"{f}: rendering picture (several minutes)...")
            sh(["npx", "remotion", "render", comp, str(video), f"--concurrency={CFG['render']['concurrency']}", "--muted", "--log=error"])
            stamp_picture()
        final = (ROOT / CFG["render"]["output_dir"] / CFG["render"]["output_name"].format(fmt=f).replace(".mp4", f"{getattr(a, 'suffix', '')}.mp4")).resolve()
        sh(["ffmpeg", "-loglevel", "error", "-y", "-i", str(video), "-i", str(audio), "-map", "0:v", "-map", "1:a", "-c:v", "copy",
            "-c:a", "aac", "-b:a", "192k", "-shortest", "-movflags", "+faststart", str(final)])
        print(f"{f}: -> {final}")
    cmd_check(a)


SOCIAL = {"x": ("SocialX", "srutam-2.5-x-16x9.mp4"), "ig": ("SocialIG", "srutam-2.5-instagram-9x16.mp4"), "linkedin": ("SocialLinkedIn", "srutam-2.5-linkedin-4x5.mp4")}


def cmd_social(a):
    """Render the ~20s social cuts (src/social/*) with audio, normalised to the platform loudness."""
    dest = (ROOT / ".." / "social").resolve()
    dest.mkdir(exist_ok=True)
    OUT.mkdir(exist_ok=True)
    for k in (a.which or list(SOCIAL)):
        comp, name = SOCIAL[k]
        raw = OUT / f"{comp}.mp4"
        mix = CFG["mix"]
        norm = f"loudnorm=I={mix['loudness_lufs']}:TP={mix['true_peak']}:LRA=9"
        if a.audio_only and raw.exists():
            print(f"{k}: remixing audio only (picture reused)...")
            wav = OUT / f"{comp}.wav"
            sh(["npx", "remotion", "render", comp, str(wav), "--codec=wav", "--log=error"])
            sh(["ffmpeg", "-loglevel", "error", "-y", "-i", str(raw), "-i", str(wav), "-map", "0:v", "-map", "1:a", "-c:v", "copy", "-af", norm,
                "-c:a", "aac", "-b:a", "192k", "-ar", "48000", "-shortest", "-movflags", "+faststart", str(dest / name)])
        else:
            print(f"{k}: rendering {comp}...")
            sh(["npx", "remotion", "render", comp, str(raw), f"--concurrency={CFG['render']['concurrency']}", "--log=error"])
            sh(["ffmpeg", "-loglevel", "error", "-y", "-i", str(raw), "-c:v", "copy", "-af", norm,
                "-c:a", "aac", "-b:a", "192k", "-ar", "48000", "-movflags", "+faststart", str(dest / name)])
        print(f"{k}: -> {dest / name} ({duration(dest / name):.1f}s)")


def cmd_check(a):
    for f in CFG["render"]["formats"]:
        final = (ROOT / CFG["render"]["output_dir"] / CFG["render"]["output_name"].format(fmt=f).replace(".mp4", f"{getattr(a, 'suffix', '')}.mp4")).resolve()
        if not final.exists():
            continue
        r = subprocess.run(["ffmpeg", "-hide_banner", "-i", str(final), "-af", "ebur128", "-f", "null", "-"], capture_output=True, text=True)
        lufs = re.findall(r"I:\s+(-?[\d.]+) LUFS", r.stderr)
        print(f"{final.name}: {duration(final):.1f}s, {lufs[-1] if lufs else '?'} LUFS, {final.stat().st_size / 1e6:.1f} MB")


def cmd_editor(a):
    sys.path.insert(0, str(ROOT))
    from editor.server import serve
    serve(a.port)


def cmd_timeline(_):
    t = timeline()
    for s in t["tl"]:
        print(f"{s['id']:<11} start {s['start']:6.2f}s  dur {s['dur']:5.2f}s")
    print(f"total {t['total']:.2f}s")


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)
    sub.add_parser("credits").set_defaults(fn=cmd_credits)
    v = sub.add_parser("voice")
    v.add_argument("--audition", action="store_true", help="only render line 1 in each audition voice")
    v.add_argument("--only", nargs="*", help="scene ids to regenerate")
    v.set_defaults(fn=cmd_voice)
    s = sub.add_parser("sfx")
    s.add_argument("names", nargs="*")
    s.set_defaults(fn=cmd_sfx)
    m = sub.add_parser("music")
    m.add_argument("action", choices=["gen", "use", "arrange", "build", "previews"])
    m.add_argument("name", nargs="?", default=CFG["music"]["current"])
    m.add_argument("--seconds", type=float, default=20)
    m.add_argument("--prompt")
    m.set_defaults(fn=cmd_music)
    st = sub.add_parser("stills")
    st.add_argument("--fmt", default="16x9", choices=list(CFG["render"]["formats"]))
    st.add_argument("--at", help="comma-separated seconds; default = one frame per scene")
    st.set_defaults(fn=cmd_stills)
    r = sub.add_parser("render")
    r.add_argument("--fmt", default=",".join(CFG["render"]["formats"]))
    r.add_argument("--audio-only", action="store_true", help="keep the last picture render, only remix/remux audio")
    r.add_argument("--suffix", default="", help="variation tag for the output file, e.g. -funk")
    r.set_defaults(fn=cmd_render)
    so = sub.add_parser("social")
    so.add_argument("which", nargs="*", help="x, ig, linkedin (default: all)")
    so.add_argument("--audio-only", action="store_true", help="reuse the last picture render, only remix audio")
    so.set_defaults(fn=cmd_social)
    c = sub.add_parser("check")
    c.add_argument("--suffix", default="")
    c.set_defaults(fn=cmd_check)
    sub.add_parser("timeline").set_defaults(fn=cmd_timeline)
    ed = sub.add_parser("editor", help="open the visual editor at http://127.0.0.1:8765")
    ed.add_argument("--port", type=int, default=8765)
    ed.set_defaults(fn=cmd_editor)
    a = p.parse_args()
    a.fn(a)


if __name__ == "__main__":
    main()
