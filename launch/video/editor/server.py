"""Local editor server for the Srutam launch video. Standard library only.

    python3 srutam_video.py editor        # then open http://127.0.0.1:8765

Listens on 127.0.0.1 only. The ElevenLabs key stays on the server and is never sent to the browser.
Every POST needs the X-Srutam header, so other websites open in your browser cannot drive it.
"""
from __future__ import annotations

import json
import mimetypes
import os
import subprocess
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, unquote, urlparse

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
LAUNCH = ROOT.parent
sys.path.insert(0, str(ROOT))
import srutam_video as sv  # noqa: E402

JOBS: dict[str, dict] = {}
JOB_LOCK = threading.Lock()
KNOWN_SCENES = {  # components that exist in src/scenes, with defaults for re-adding a removed scene
    "hook": (3.6, "Your best ideas never wait for a good moment."),
    "title": (3.2, "Meet Srutam two point five."),
    "dock": (7.4, "A floating dock lives on the edge of your screen. One tap, and you're recording, from any app."),
    "transcribe": (4.8, "Your words are transcribed right on your phone."),
    "organize": (6.2, "Then Srutam sorts the ramble into ideas, next steps, decisions and reminders."),
    "newin": (2.0, ""),
    "cloud": (5.0, "New in two point five: Cloud Sync keeps every note with you."),
    "connect": (6.6, "And with Srutam MCP, your voice notes live inside your AI coding agent. Scan once. No keys to paste."),
    "agent": (7.6, "Ask what's on your plate. Your agent fixes it, and checks it off on your phone."),
    "trust": (4.6, "Private notes stay private. And agents can't delete a thing."),
    "end": (6.4, "Srutam two point five. Say it once. Use it everywhere. Get it on Google Play."),
}


def list_names(folder: Path, pattern: str) -> list[str]:
    return sorted(p.stem for p in folder.glob(pattern)) if folder.exists() else []


def state() -> dict:
    plan, man = sv.load_plan(), sv.manifest()
    outputs = []
    for p in sorted(list(LAUNCH.glob("*.mp4")) + list((LAUNCH / "social").glob("*.mp4"))):
        outputs.append({"name": p.name, "path": str(p.relative_to(LAUNCH)), "mb": round(p.stat().st_size / 1e6, 1), "mtime": int(p.stat().st_mtime)})
    pics = {k: (sv.OUT / f"{c}.mp4").exists() for k, c in sv.CFG["render"]["formats"].items()}
    return {
        "plan": plan,
        "vo": {k: {"dur": v["dur"], "text": v.get("text")} for k, v in man.get("scenes", {}).items()},
        "sfx": list_names(sv.PUB / "sfx", "*.mp3"),
        "sfx_config": sv.CFG["sfx"],
        "music": list_names(sv.PUB / "music-samples", "*.mp3"),
        "known_scenes": {k: {"min": v[0], "vo": v[1]} for k, v in KNOWN_SCENES.items()},
        "outputs": outputs,
        "pictures": pics,
        "stale": sv.picture_stale(),
        "busy": any(j["status"] == "running" for j in JOBS.values()),
    }


def start_job(argv: list[str], label: str) -> str:
    with JOB_LOCK:
        if any(j["status"] == "running" for j in JOBS.values()):
            raise RuntimeError("another job is still running")
        jid = str(int(time.time() * 1000))
        JOBS[jid] = {"label": label, "status": "running", "log": [f"$ {' '.join(argv)}"], "started": time.time()}

    def run():
        try:
            proc = subprocess.Popen([sys.executable, str(ROOT / "srutam_video.py"), *argv], cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
            for line in proc.stdout:  # type: ignore[union-attr]
                JOBS[jid]["log"].append(line.rstrip())
            code = proc.wait()
            JOBS[jid]["status"] = "done" if code == 0 else "error"
        except Exception as e:  # noqa: BLE001
            JOBS[jid]["log"].append(f"failed to start: {e}")
            JOBS[jid]["status"] = "error"
        JOBS[jid]["log"].append("[finished]" if JOBS[jid]["status"] == "done" else "[failed]")

    threading.Thread(target=run, daemon=True).start()
    return jid


def validate_plan(p: dict) -> None:
    assert isinstance(p.get("scenes"), list) and p["scenes"], "plan needs at least one scene"
    ids = [s["id"] for s in p["scenes"]]
    assert len(ids) == len(set(ids)), "duplicate scene ids"
    for s in p["scenes"]:
        assert s["id"] in KNOWN_SCENES, f"unknown scene '{s['id']}' (the editor can reorder and edit known scenes)"
        assert float(s["min"]) >= 1.0, "a scene needs at least 1s"
        assert isinstance(s.get("vo", ""), str)
    for c in p.get("cues", []):
        assert c["sfx"] and isinstance(c["at"], (int, float)) and 0 <= float(c["vol"]) <= 1.5, f"bad cue {c}"
    m = p["music"]
    assert m["mode"] in ("loop", "arrange", "track"), "music.mode must be loop, arrange or track"
    assert isinstance(m["track"], str) and m["track"].replace("-", "").replace("_", "").isalnum(), "bad track name"


class H(BaseHTTPRequestHandler):
    server_version = "SrutamEditor/1"

    def log_message(self, *a):  # keep the console quiet
        pass

    # ---- helpers
    def send_json(self, obj, code=200):
        b = json.dumps(obj).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(b)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(b)

    def send_file(self, path: Path):
        if not path.is_file():
            return self.send_json({"error": "not found"}, 404)
        size = path.stat().st_size
        ctype = mimetypes.guess_type(str(path))[0] or "application/octet-stream"
        start, end, code = 0, size - 1, 200
        rng = self.headers.get("Range")
        if rng and rng.startswith("bytes="):
            a, _, b = rng[6:].partition("-")
            start = int(a) if a else 0
            end = int(b) if b else size - 1
            end = min(end, size - 1)
            code = 206
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Content-Length", str(end - start + 1))
        self.send_header("Cache-Control", "no-cache")
        if code == 206:
            self.send_header("Content-Range", f"bytes {start}-{end}/{size}")
        self.end_headers()
        try:
            with open(path, "rb") as f:
                f.seek(start)
                left = end - start + 1
                while left > 0:
                    chunk = f.read(min(1 << 16, left))
                    if not chunk:
                        break
                    self.wfile.write(chunk)
                    left -= len(chunk)
        except (BrokenPipeError, ConnectionResetError):
            pass

    def media_path(self, rel: str) -> Path | None:
        """Serve only media files: launch/*.mp4, launch/{social,music}/*, and launch/video/{public,out}/** media.
        Never dotfiles, never plan/config/source files."""
        p = (LAUNCH / unquote(rel)).resolve()
        if p.suffix.lower() not in {".mp4", ".mp3", ".wav"} or any(part.startswith(".") for part in p.relative_to(LAUNCH.resolve()).parts if True):
            return None
        L, R = LAUNCH.resolve(), ROOT.resolve()
        if R in p.parents:
            return p if any(a in p.parents for a in ((R / "public"), (R / "out"))) else None
        return p if L in p.parents or p.parent == L else None

    # ---- routes
    def do_GET(self):
        u = urlparse(self.path)
        if u.path in ("/", "/index.html"):
            return self.send_file(HERE / "index.html")
        if u.path == "/api/state":
            return self.send_json(state())
        if u.path == "/api/credits":
            try:
                d = sv.eleven("/user/subscription", want_json=True)
                return self.send_json({"left": d["character_limit"] - d["character_count"], "tier": d["tier"]})
            except SystemExit as e:
                return self.send_json({"error": str(e)}, 502)
        if u.path == "/api/timeline":
            return self.send_json(sv.timeline())
        if u.path == "/api/job":
            q = parse_qs(u.query)
            j = JOBS.get(q.get("id", [""])[0])
            if not j:
                return self.send_json({"error": "no such job"}, 404)
            since = int(q.get("since", ["0"])[0])
            return self.send_json({"status": j["status"], "label": j["label"], "log": j["log"][since:], "next": len(j["log"])})
        if u.path.startswith("/media/"):
            p = self.media_path(u.path[len("/media/"):])
            return self.send_file(p) if p else self.send_json({"error": "forbidden"}, 403)
        return self.send_json({"error": "not found"}, 404)

    def do_POST(self):
        if self.headers.get("X-Srutam") != "1":
            return self.send_json({"error": "missing X-Srutam header"}, 403)
        n = int(self.headers.get("Content-Length") or 0)
        try:
            body = json.loads(self.rfile.read(n) or b"{}")
        except json.JSONDecodeError:
            return self.send_json({"error": "bad json"}, 400)
        u = urlparse(self.path)
        try:
            if u.path == "/api/plan":
                validate_plan(body)
                sv.save_plan(body)
                return self.send_json({"ok": True})
            if u.path == "/api/run":
                return self.send_json({"job": run_action(body)})
        except (AssertionError, KeyError, ValueError, RuntimeError) as e:
            return self.send_json({"error": str(e)}, 400)
        return self.send_json({"error": "not found"}, 404)


def run_action(b: dict) -> str:
    act = b.get("action")
    fmts = ",".join(f for f in b.get("fmts", ["16x9", "9x16"]) if f in sv.CFG["render"]["formats"]) or "16x9"
    if act == "build_music":
        return start_job(["music", "build"], "Rebuild music bed")
    if act == "remix":
        return start_job(["render", "--audio-only", "--fmt", fmts], "Remix audio")
    if act == "render":
        return start_job(["render", "--fmt", fmts], "Full render")
    if act == "voice":
        ids = [i for i in b.get("scenes", []) if i in KNOWN_SCENES]
        return start_job(["voice", "--only", *ids] if ids else ["voice"], "Generate narration")
    if act == "music_gen":
        name = "".join(ch for ch in str(b.get("name", "")) if ch.isalnum() or ch in "-_")[:40]
        assert name and b.get("prompt"), "need a name and a prompt"
        return start_job(["music", "gen", name, "--prompt", str(b["prompt"])[:600], "--seconds", str(min(30, float(b.get("seconds", 20))))], "Generate music loop")
    raise ValueError(f"unknown action '{act}'")


def serve(port: int = 8765) -> None:
    srv = ThreadingHTTPServer(("127.0.0.1", port), H)
    print(f"Srutam video editor: http://127.0.0.1:{port}   (Ctrl+C to stop)")
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    serve(int(sys.argv[1]) if len(sys.argv) > 1 else 8765)
