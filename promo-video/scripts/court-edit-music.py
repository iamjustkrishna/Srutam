"""Cut "The Tiptoe Verdict" (music-src/, made by the user with Suno) to The Brain Court's beats.

    python scripts/court-edit-music.py

Video (s)      Track (s)       What
0    - 5.2     0    - 5.2      dramatic intro hit under the gavel and Aarav
5.2  - 32      6    - 32.8     the tiptoe pizzicato groove under the trial
32   - 36.3    47.7 - 52       the build, for the spiral
36.3 - 38      -               silence: "Objection."
38   - 45.3    54   - 61.3     warm, fuller section for Srutam and the proof
45.3 - 58      105.3 - 118     the ending, so it resolves under the call to action
"""
from pathlib import Path

import numpy as np
import soundfile as sf
import subprocess

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "music-src" / "the-tiptoe-verdict.mp3"
OUT = ROOT / "public" / "court" / "music.wav"
SR = 44100
TOTAL = 1740 / 30
CUTS = [(0.0, 0.0, 5.2), (5.2, 6.0, 26.8), (32.0, 47.7, 4.3), (38.0, 54.0, 7.3), (45.3, 105.3, 12.7)]
XF = 0.12

wav = OUT.with_suffix(".src.wav")
subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", str(SRC), "-ar", str(SR), "-ac", "2", str(wav)], check=True)
x, _ = sf.read(wav)
wav.unlink()
out = np.zeros((int((TOTAL + 1) * SR), 2))
for at, src, dur in CUTS:
    a, n, f = int(src * SR), int((dur + XF) * SR), int(XF * SR)
    seg = x[a:a + n].copy()
    ramp = np.linspace(0, 1, f)[:, None]
    if at > 0:
        seg[:f] *= ramp
    seg[-f:] *= ramp[::-1]
    i = int(at * SR)
    out[i:i + len(seg)] += seg
out[int(TOTAL * SR):] = 0
out *= 0.9 / np.max(np.abs(out))
sf.write(OUT, out.astype(np.float32), SR)
print(OUT.relative_to(ROOT), f"{TOTAL:.1f}s")
