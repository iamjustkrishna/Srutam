"""Hindi lines for Ma and Nani with Kokoro TTS (local, Hindi voices).

Kokoro's young Hindi voices are slowed and lowered a little so they read as
older women. The voice-note playback line also gets a small-speaker EQ.

Usage: python scripts/generate-voices-kokoro.py <kokoro.onnx> <voices.bin>
"""
import sys
from pathlib import Path

import numpy as np
import soundfile as sf
from kokoro_onnx import Kokoro

OUT = Path(__file__).resolve().parent.parent / "public" / "voiceover"
SR_OUT = 44100

# (file, voice, speed, semitones, phone EQ, text)
LINES = [
    ("knock/ma/ma-remembered", "hf_beta", 0.92, -1.5, False, "अरे! तुझे याद रहा!"),
    ("nani/nani/nani-smell", "hf_alpha", 0.86, -2.5, False, "नापना? अरे बेटा, खुशबू से पता चलता है!"),
    ("nani/nani/nani-recipe", "hf_alpha", 0.82, -2.5, True, "एक मुट्ठी गुड़, हर कटोरी बेसन के लिए... और थोड़ा सा प्यार. बस."),
    ("nani/nani/nani-mine", "hf_alpha", 0.88, -2.5, False, "अरे वाह! बिल्कुल मेरे जैसे!"),
]


def lower(x, sr, semis):
    """Lower pitch and formants together by resampling (an older, warmer voice)."""
    k = 2 ** (semis / 12)
    n = int(len(x) / k)
    return np.interp(np.arange(n) * k, np.arange(len(x)), x), sr


def phone_eq(x, sr):
    """A phone speaker: band-limit to roughly 300 Hz - 4 kHz."""
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / sr)
    spec *= (f > 280) * (f < 4200) + 0.08
    return np.fft.irfft(spec, len(x))


def to_out(x, sr):
    n = int(len(x) * SR_OUT / sr)
    return np.interp(np.arange(n) * sr / SR_OUT, np.arange(len(x)), x)


def main(model, voices):
    k = Kokoro(model, voices)
    for name, voice, speed, semis, phone, text in LINES:
        audio, sr = k.create(text, voice=voice, speed=speed, lang="hi")
        audio, sr = lower(np.asarray(audio, dtype=np.float64), sr, semis)
        if phone:
            audio = phone_eq(audio, sr)
        audio = to_out(audio, sr)
        nz = np.flatnonzero(np.abs(audio) > 0.01)
        audio = audio[max(0, nz[0] - 2000): nz[-1] + 6000]
        fade = int(0.05 * SR_OUT)
        audio[-fade:] *= np.linspace(1, 0, fade)
        audio = audio / np.max(np.abs(audio)) * 0.9
        path = OUT / f"{name}.wav"
        path.parent.mkdir(parents=True, exist_ok=True)
        sf.write(path, audio.astype(np.float32), SR_OUT)
        print(f"{name}: {len(audio) / SR_OUT:.2f}s")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
