"""Hindi voices for the story videos with Supertonic 3 (Supertone, 2026; MIT code,
OpenRAIL-M model), run through sherpa-onnx. A flow-matching model with 10 voices
(F1-F5, M1-M5) that reads Hindi with natural, sentence-level prosody.

Model: https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/sherpa-onnx-supertonic-3-tts-int8-2026-05-11.tar.bz2

    python scripts/generate-voices-supertonic.py path/to/sherpa-onnx-supertonic-3-tts-int8-2026-05-11 [out_dir] [ids...]

out_dir defaults to public/voiceover; English words are written in Devanagari so the
Hindi reader pronounces them the way Hindi speakers do.

The model samples noise, so every take differs. Like a director, the script records
TAKES takes of each line and keeps the liveliest believable one: the widest pitch range
(capped, so a pitch-tracker glitch never wins), and for lines marked "up" (shouts,
calls) a take pitched above the others.
"""

import sys
from pathlib import Path

import numpy as np
import pyworld
import sherpa_onnx
import soxr

from voice_fx import TREAT, finish, trim

ROOT = Path(__file__).resolve().parent.parent
SR = 44100
TAKES = 6

# sid in voice.bin (sorted: F1..F5 = 0..4, M1..M5 = 5..9), speed (>1 faster)
VOICES = {
    "rao": (6, 0.98),  # M2: deep, settled
    "arjun": (5, 1.05),  # M1: light, young
    "neha": (2, 1.02),  # F3
    "prof": (9, 0.92),  # M5: deep, deliberate
    "kabir": (8, 1.08),  # M4
    "riya": (3, 1.08),  # F4: bright
}

# id, character, text, treatment, gain dB, "up" for shouts and calls
LINES = [
    ("meeting/rao/rao-who", "rao", "फ़ोर्टीन? किसने हाँ बोला था फ़ोर्टीन को?!", "room", 2.0, True),
    ("meeting/rao/rao-launch", "rao", "तो... लॉन्च, फ़ोर्टीन को करें?", "room", 0.0, False),
    ("meeting/arjun/arjun-21", "arjun", "सर, ट्वेंटी-फ़र्स्ट ज़्यादा सेफ़ रहेगा...", "room", 0.0, False),
    ("meeting/rao/rao-final", "rao", "नहीं, नहीं... फ़ोर्टीन. फ़ाइनल.", "room", 0.5, False),
    ("meeting/rao/rao-deck", "rao", "अर्जुन, प्राइसिंग डेक, थर्सडे तक.", "room", 0.0, False),
    ("meeting/rao/rao-note", "rao", "कोई नोट कर लो!", "room", 1.0, True),
    ("meeting/arjun/arjun-her", "arjun", "नेहा ने!", "room", 1.0, True),
    ("meeting/neha/neha-him", "neha", "अर्जुन ने!", "room", 1.0, True),
    ("meeting/rao/rao-sheepish", "rao", "अच्छा... तो फ़ोर्टीन ही फ़ाइनल है.", "room", -1.5, False),
    ("exam/prof/prof-lecture", "prof", "तो, थर्सडे के एग्ज़ाम में...", "muffled", -2.0, False),
    ("exam/prof/prof-recording", "prof", "थर्सडे के एग्ज़ाम में, चैप्टर चार और सात, और मेट्रो केस स्टडी. बीस मार्क्स का.", "phone", 0.0, False),
    ("exam/kabir/kabir-chai", "kabir", "चाय आ गई!", "room", 1.0, True),
    ("exam/riya/riya-study", "riya", "चल, शुरू करते हैं!", "room", 1.0, True),
]

# Lines that must fit a tighter slot than the character's usual pace.
SPEED = {"meeting/arjun/arjun-21": 1.16}

# The playback in "Who Said That?" is the very same take as Tuesday's line.
COPIES = [("meeting/rao/rao-final", "meeting/rao/rao-recording", "phone")]


def load(model: Path):
    cfg = sherpa_onnx.OfflineTtsConfig(
        model=sherpa_onnx.OfflineTtsModelConfig(
            supertonic=sherpa_onnx.OfflineTtsSupertonicModelConfig(
                duration_predictor=str(model / "duration_predictor.int8.onnx"),
                text_encoder=str(model / "text_encoder.int8.onnx"),
                vector_estimator=str(model / "vector_estimator.int8.onnx"),
                vocoder=str(model / "vocoder.int8.onnx"),
                tts_json=str(model / "tts.json"),
                unicode_indexer=str(model / "unicode_indexer.bin"),
                voice_style=str(model / "voice.bin"),
            ),
            num_threads=4,
            provider="cpu",
        )
    )
    return sherpa_onnx.OfflineTts(cfg)


def pitch(x: np.ndarray):
    """Median F0 (Hz), 10-90% pitch range (semitones) and voiced share of a take."""
    f0, _ = pyworld.harvest(x.astype(np.float64), SR, frame_period=10)
    v = f0[f0 > 0]
    if len(v) < 10:
        return 0.0, 0.0, 0.0
    st = 12 * np.log2(v / np.median(v))
    return float(np.median(v)), float(np.percentile(st, 90) - np.percentile(st, 10)), len(v) / len(f0)


def best_take(takes: list, up: bool) -> int:
    stats = [pitch(x) for x in takes]
    mid = np.median([m for m, _, _ in stats if m > 0] or [1.0])

    def score(st):
        med, rng, voiced = st
        if med <= 0 or voiced < 0.45:
            return -99.0
        lift = 12 * np.log2(med / mid) if up else -abs(12 * np.log2(med / mid)) * 0.3
        return min(rng, 12.0) + 0.8 * lift

    return int(np.argmax([score(st) for st in stats]))


def main():
    model = Path(sys.argv[1])
    out_root = Path(sys.argv[2]) if len(sys.argv) > 2 else ROOT / "public" / "voiceover"
    only = set(sys.argv[3:])
    tts = load(model)
    import soundfile as sf

    dry = {}
    for lid, who, text, treat, gain, up in LINES:
        if only and lid not in only and Path(lid).name not in only:
            continue
        sid, speed = VOICES[who]
        speed = SPEED.get(lid, speed)
        gc = sherpa_onnx.GenerationConfig()
        gc.sid = sid
        gc.num_steps = 24
        gc.speed = speed
        gc.extra = {"lang": "hi"}
        takes = []
        for _ in range(TAKES):
            audio = tts.generate(text, gc)
            x = soxr.resample(np.array(audio.samples, dtype=np.float32), audio.sample_rate, SR)
            takes.append(trim(x, SR).astype(np.float64))
        x = takes[best_take(takes, up)]
        dry[lid] = (x, gain)
        y = TREAT[treat](x, SR)
        out = out_root / f"{lid}.wav"
        out.parent.mkdir(parents=True, exist_ok=True)
        sf.write(out, finish(y, SR, gain), SR)
        print(f"{out}  {len(y) / SR:.2f}s")
    for src, dst, treat in COPIES:
        if src in dry:
            x, g = dry[src]
            out = out_root / f"{dst}.wav"
            sf.write(out, finish(TREAT[treat](x, SR), SR, g), SR)
            print(f"{out}  (same take as {src})")


if __name__ == "__main__":
    main()
