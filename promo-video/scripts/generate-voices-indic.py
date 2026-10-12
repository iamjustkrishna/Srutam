"""Hindi voices for the story videos with AI4Bharat Indic-TTS (FastPitch + HiFi-GAN,
native Hindi studio voices, MIT): https://github.com/AI4Bharat/Indic-TTS

The model has one male and one female voice. Each character gets its own voice by
steering the model's own pitch prediction (level and range, so intonation stays
natural), its pace, and a light vocal-tract (formant) shift with WORLD. Lines are
written the way people actually talk: Hinglish, with English words in Devanagari.

    python scripts/generate-voices-indic.py path/to/Indic-TTS/hi [only-these-ids...]

Needs: pip install coqui-tts torch pyworld soundfile soxr scipy
"""

import sys
from pathlib import Path

import numpy as np
import soundfile as sf
import soxr
import torch
from voice_fx import TREAT, finish, trim

ROOT = Path(__file__).resolve().parent.parent
PUB = ROOT / "public" / "voiceover"
SR = 44100

# speaker, pitch level (model's normalised units), pitch range, pace (>1 slower), formant ratio, gain dB
VOICES = {
    "rao": ("male", -0.35, 1.15, 1.0, 0.95, 0.0),
    "arjun": ("male", 0.45, 1.2, 0.93, 1.05, 0.0),
    "neha": ("female", -0.15, 1.1, 0.95, 0.98, 0.0),
    "prof": ("male", -0.15, 0.95, 1.1, 0.97, 0.0),
    "riya": ("female", 0.35, 1.3, 0.9, 1.05, 0.0),
    "kabir": ("male", 0.2, 1.35, 0.9, 1.02, 0.0),
}

# id, character, text, mood, treatment
#   mood: calm | firm | angry | excited | sheepish | lecture (adjusts pitch, range, pace, level)
#   treatment: room (a little office/hostel air) | phone (played back from a phone) | muffled (memory)
LINES = [
    # Who Said That?
    ("meeting/rao/rao-who", "rao", "फ़ोर्टीन? किसने हाँ बोला था फ़ोर्टीन को?!", "angry", "room"),
    ("meeting/rao/rao-launch", "rao", "तो... लॉन्च, फ़ोर्टीन को करें?", "calm", "room"),
    ("meeting/arjun/arjun-21", "arjun", "सर, ट्वेंटी-फ़र्स्ट ज़्यादा सेफ़ रहेगा...", "calm", "room"),
    ("meeting/rao/rao-final", "rao", "नहीं नहीं, फ़ोर्टीन. फ़ाइनल.", "firm", "room"),
    ("meeting/rao/rao-deck", "rao", "अर्जुन, प्राइसिंग डेक, थर्सडे तक.", "firm", "room"),
    ("meeting/rao/rao-note", "rao", "कोई नोट कर लो!", "excited", "room"),
    ("meeting/arjun/arjun-her", "arjun", "नेहा ने!", "excited", "room"),
    ("meeting/neha/neha-him", "neha", "अर्जुन ने!", "excited", "room"),
    ("meeting/rao/rao-sheepish", "rao", "अच्छा... तो फ़ोर्टीन ही फ़ाइनल है.", "sheepish", "room"),
    # 2 a.m. Before the Exam
    ("exam/prof/prof-lecture", "prof", "तो थर्सडे के एग्ज़ाम में...", "lecture", "muffled"),
    ("exam/prof/prof-recording", "prof", "थर्सडे के एग्ज़ाम में, चैप्टर चार और सात, और मेट्रो केस स्टडी. बीस मार्क्स का.", "lecture", "phone"),
    ("exam/kabir/kabir-chai", "kabir", "चाय आ गई!", "excited", "room"),
    ("exam/riya/riya-study", "riya", "चल, शुरू करते हैं!", "excited", "room"),
]

# The playback in "Who Said That?" must be the very same take as Tuesday's line.
COPIES = [("meeting/rao/rao-final", "meeting/rao/rao-recording", "phone")]

MOODS = {
    # pitch level offset, range multiplier, pace multiplier, gain dB
    "calm": (0.0, 1.0, 1.02, 0.0),
    "firm": (-0.1, 1.15, 0.92, 1.5),
    "angry": (0.7, 1.6, 0.9, 3.0),
    "excited": (0.5, 1.4, 0.92, 2.0),
    "sheepish": (-0.2, 0.8, 1.0, -2.0),
    "lecture": (0.0, 1.1, 0.95, 0.0),
}


def load(model_dir: Path):
    import json
    import tempfile

    from TTS.utils.synthesizer import Synthesizer

    # The released config points at the training machine's paths; aim it at this copy.
    speakers = str(model_dir / "fastpitch" / "speakers.pth")
    raw = (model_dir / "fastpitch" / "config.json").read_text().replace("models/v1/hi/fastpitch/speakers.pth", speakers)
    cfg = Path(tempfile.mkdtemp()) / "config.json"
    cfg.write_text(raw)
    json.loads(raw)
    syn = Synthesizer(
        tts_checkpoint=str(model_dir / "fastpitch" / "best_model.pth"),
        tts_config_path=str(cfg),
        tts_speakers_file=str(model_dir / "fastpitch" / "speakers.pth"),
        vocoder_checkpoint=str(model_dir / "hifigan" / "best_model.pth"),
        vocoder_config=str(model_dir / "hifigan" / "config.json"),
        use_cuda=False,
    )
    return syn


class PitchSteer:
    """Wraps the FastPitch pitch predictor: o' = level + range * o (voiced frames)."""

    def __init__(self, model):
        self.model = model
        self.orig = model.pitch_predictor.forward
        self.level = 0.0
        self.range = 1.0
        model.pitch_predictor.forward = self.forward

    def forward(self, *a, **k):
        o = self.orig(*a, **k)
        mean = o.mean()
        return (o - mean) * self.range + mean + self.level


def formant(x, sr, ratio):
    """Scale the vocal tract (spectral envelope) by `ratio`, keeping pitch and timing."""
    if abs(ratio - 1) < 0.005:
        return x
    import pyworld as pw

    x64 = x.astype(np.float64)
    f0, t = pw.harvest(x64, sr, frame_period=5.0)
    sp = pw.cheaptrick(x64, f0, t, sr)
    ap = pw.d4c(x64, f0, t, sr)
    bins = sp.shape[1]
    src = np.clip(np.arange(bins) / ratio, 0, bins - 1)
    warp = lambda m: np.stack([np.interp(src, np.arange(bins), row) for row in m])
    y = pw.synthesize(f0, warp(sp), warp(ap), sr, frame_period=5.0)
    return y.astype(np.float32)



def main():
    model_dir = Path(sys.argv[1])
    only = set(sys.argv[2:])
    torch.manual_seed(0)
    syn = load(model_dir)
    model = syn.tts_model
    steer = PitchSteer(model)
    base_scale = model.length_scale
    vsr = syn.output_sample_rate
    dry = {}
    for lid, who, text, mood, treat in LINES:
        if only and lid not in only and Path(lid).name not in only:
            continue
        spk, level, rng, pace, ratio, gain = VOICES[who]
        dl, dr, dp, dg = MOODS[mood]
        steer.level = level + dl
        steer.range = rng * dr
        model.length_scale = base_scale * pace * dp
        wav = np.array(syn.tts(text, speaker_name=spk), dtype=np.float32)
        x = soxr.resample(wav, vsr, SR)
        x = formant(x, SR, ratio)
        x = trim(x, SR)
        dry[lid] = (x, gain + dg)
        y = TREAT[treat](x, SR)
        out = PUB / f"{lid}.wav"
        out.parent.mkdir(parents=True, exist_ok=True)
        sf.write(out, finish(y, SR, gain + dg), SR)
        print(f"{out.relative_to(ROOT)}  {len(y) / SR:.2f}s")
    for src, dst, treat in COPIES:
        if src not in dry:
            continue
        x, g = dry[src]
        y = TREAT[treat](x, SR)
        out = PUB / f"{dst}.wav"
        sf.write(out, finish(y, SR, g), SR)
        print(f"{out.relative_to(ROOT)}  {len(y) / SR:.2f}s (same take as {src})")


if __name__ == "__main__":
    main()
