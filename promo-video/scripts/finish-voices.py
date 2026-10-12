"""Finish the ElevenLabs Hindi takes for the story videos.

generate-voiceover-elevenlabs.mjs writes one mp3 per line (scripts/vo/hindi/*.json);
this turns each into the .wav the timelines play: trimmed, levelled, with a little
room air for dialogue, a phone-speaker treatment for recordings played back from a
phone, and a muffled memory for the flashback. The playback in "Who Said That?" is
made from the very same take as Tuesday's line.

    python scripts/finish-voices.py
"""

import subprocess
import tempfile
from pathlib import Path

import numpy as np
import soundfile as sf

from voice_fx import TREAT, finish, trim

ROOT = Path(__file__).resolve().parent.parent
VO = ROOT / "public" / "voiceover"
SR = 44100

# source mp3 (relative to public/voiceover), output wav, treatment, gain dB
JOBS = [
    ("meeting/rao/rao-who", "meeting/rao/rao-who", "room", 1.5),
    ("meeting/rao/rao-launch", "meeting/rao/rao-launch", "room", 0.0),
    ("meeting/rao/rao-final", "meeting/rao/rao-final", "room", 0.0),
    ("meeting/rao/rao-final", "meeting/rao/rao-recording", "phone", 0.0),
    ("meeting/rao/rao-deck", "meeting/rao/rao-deck", "room", 0.0),
    ("meeting/rao/rao-note", "meeting/rao/rao-note", "room", 0.0),
    ("meeting/rao/rao-sheepish", "meeting/rao/rao-sheepish", "room", -1.5),
    ("meeting/arjun/arjun-21", "meeting/arjun/arjun-21", "room", 0.0),
    ("meeting/arjun/arjun-her", "meeting/arjun/arjun-her", "room", 0.0),
    ("meeting/neha/neha-him", "meeting/neha/neha-him", "room", 0.0),
    ("exam/prof/prof-recording", "exam/prof/prof-recording", "phone", 0.0),
    ("exam/prof/prof-lecture", "exam/prof/prof-lecture", "muffled", -2.0),
    ("exam/kabir/kabir-chai", "exam/kabir/kabir-chai", "room", 0.0),
    ("exam/riya/riya-study", "exam/riya/riya-study", "room", 0.0),
]


def load_mp3(path: Path):
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / "x.wav"
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(path), "-ac", "1", "-ar", str(SR), str(wav)], check=True)
        x, _ = sf.read(wav, dtype="float32")
    return x


def main():
    for src, dst, treat, gain in JOBS:
        mp3 = VO / f"{src}.mp3"
        if not mp3.exists():
            print(f"skip {src} (no take yet)")
            continue
        x = trim(load_mp3(mp3), SR)
        y = TREAT[treat](x.astype(np.float64), SR)
        out = VO / f"{dst}.wav"
        sf.write(out, finish(y, SR, gain), SR)
        print(f"{out.relative_to(ROOT)}  {len(y) / SR:.2f}s  ({treat})")


if __name__ == "__main__":
    main()
