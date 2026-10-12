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
    # Who Said That? (office Hinglish, English captions)
    ("meeting/rao/rao-who", "hm_omega", 1.0, -1.0, False, "चौदह तारीख? किसने हाँ बोला था चौदह तारीख को?!"),
    ("meeting/arjun/arjun-her", "hm_psi", 1.05, 0.0, False, "इसने!"),
    ("meeting/neha/neha-him", "hf_beta", 1.05, 0.0, False, "इसने!"),
    ("meeting/rao/rao-recording", "hm_omega", 0.95, -1.0, True, "ठीक है, चौदह तारीख फाइनल."),
    ("meeting/rao/rao-launch", "hm_omega", 1.0, -1.0, False, "चौदह तारीख को लॉन्च?"),
    ("meeting/rao/rao-final", "hm_omega", 0.95, -1.0, False, "ठीक है, चौदह तारीख फाइनल."),
    ("meeting/rao/rao-deck", "hm_omega", 1.0, -1.0, False, "अर्जुन, गुरुवार तक प्राइसिंग डेक."),
    ("meeting/rao/rao-note", "hm_omega", 1.0, -1.0, False, "बढ़िया. कोई नोट कर लेना!"),
    ("meeting/arjun/arjun-yes", "hm_psi", 1.05, 0.0, False, "हाँ सर!"),
    ("meeting/neha/neha-yes", "hf_beta", 1.05, 0.0, False, "हाँ, हाँ!"),
    # 2 a.m. Before the Exam
    ("exam/prof/prof-chapters", "hm_psi", 0.9, -3.0, False, "चैप्टर चार और सात... और मेट्रो केस स्टडी. बीस मार्क्स."),
    ("exam/prof/prof-recording", "hm_psi", 0.9, -3.0, True, "और मेट्रो केस स्टडी. बीस मार्क्स."),
    ("exam/riya/riya-record", "hf_alpha", 1.05, 0.0, False, "रिकॉर्ड किया था ना?!"),
    ("exam/kabir/kabir-chai", "hm_omega", 1.05, 1.0, False, "मैं चाय लाता हूँ!"),
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
