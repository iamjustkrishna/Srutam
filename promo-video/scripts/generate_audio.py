"""Synthesize the animatic's temp music bed and sound effects (no downloads).

The bed follows the color script: a muted, tense minor pad while the idea is
dying, a hard drop to silence right before the tap, then warm major chords
once the thought is caught, swelling at the payoff.

Times are in seconds and mirror src/timeline.ts (frame / 30).

Usage: python generate_audio.py
"""
from pathlib import Path

import numpy as np
import soundfile as sf

SR = 44100
OUT = Path(__file__).resolve().parent.parent / "public"
rng = np.random.default_rng(7)

# Mirrors src/timeline.ts
DURATION = 30.0
DROP_START = 228 / 30
CAUGHT = 251 / 30
NIGHT = 640 / 30
END = 790 / 30


def t_axis(seconds):
    return np.arange(int(seconds * SR)) / SR


def note(name):
    names = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}
    semis = names[name[0]] + (1 if "#" in name else 0)
    octave = int(name[-1])
    return 440.0 * 2 ** ((semis - 9) / 12 + (octave - 4))


def lowpass(x, cutoff):
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc = (1 - a) * v + a * acc
        y[i] = acc
    return y


def pad(chord, seconds, bright=0.5):
    t = t_axis(seconds)
    out = np.zeros_like(t)
    for n in chord:
        f = note(n)
        for detune in (-0.12, 0.0, 0.12):
            ff = f * 2 ** (detune / 12)
            for h, amp in ((1, 1.0), (2, 0.35 * bright), (3, 0.15 * bright)):
                out += amp * np.sin(2 * np.pi * ff * h * t + rng.uniform(0, 6.28))
    out /= len(chord) * 3
    attack = np.clip(t / 0.6, 0, 1)
    release = np.clip((seconds - t) / 0.6, 0, 1)
    return out * attack * release


def pluck(freq, seconds=0.9):
    t = t_axis(seconds)
    tone = np.sin(2 * np.pi * freq * t) + 0.3 * np.sin(2 * np.pi * freq * 2 * t)
    return tone * np.exp(-t * 6) * np.clip(t / 0.004, 0, 1)


def place(buf, clip, at, gain=1.0):
    i = int(at * SR)
    j = min(len(buf), i + len(clip))
    if j > i:
        buf[i:j] += clip[: j - i] * gain


def tick():
    t = t_axis(0.06)
    return (np.sin(2 * np.pi * 2200 * t) + 0.5 * rng.standard_normal(len(t))) * np.exp(-t * 90)


def music():
    buf = np.zeros(int(DURATION * SR))
    # Act 1: grey, tense. A minor, dark and filtered.
    place(buf, lowpass(pad(["A2", "E3", "A3", "C4"], DROP_START + 0.3, 0.2), 900), 0.0, 0.5)
    # Act 2: caught. Warm major progression.
    prog = [
        (["F2", "C3", "E3", "A3"], 2.4),
        (["C3", "G3", "C4", "E4"], 2.4),
        (["G2", "D3", "G3", "B3"], 2.4),
        (["A2", "E3", "A3", "C4"], 2.4),
        (["F2", "C3", "F3", "A3"], NIGHT - CAUGHT - 9.6 + 0.4),
    ]
    at = CAUGHT
    for chord, length in prog:
        place(buf, lowpass(pad(chord, length + 0.4, 0.5), 2200), at, 0.55)
        at += length
    # Act 3: night / payoff. Wider, brighter.
    place(buf, lowpass(pad(["F2", "C3", "E3", "A3", "C5"], END - NIGHT + 0.5, 0.9), 3200), NIGHT, 0.6)
    # Resolution: C major, fading to near-silence so the loop back to frame 0 is seamless.
    resolve = pad(["C3", "G3", "C4", "E4", "G4"], DURATION - END, 0.7)
    resolve *= np.linspace(1, 0, len(resolve)) ** 1.5
    place(buf, lowpass(resolve, 2600), END, 0.6)
    # Plucked pentatonic arpeggio once the idea is alive.
    scale = ["C5", "D5", "E5", "G5", "A5", "C6"]
    step = 60 / 104 / 2
    t = CAUGHT + 0.6
    while t < DURATION - 1.5:
        if rng.random() < 0.8:
            place(buf, pluck(note(scale[rng.integers(len(scale))])), t, 0.09)
        t += step
    return buf


def whoosh():
    t = t_axis(0.7)
    noise = rng.standard_normal(len(t))
    sweep = np.empty_like(noise)
    acc = 0.0
    for i, v in enumerate(noise):
        cutoff = 300 + 5000 * (i / len(noise))
        a = np.exp(-2 * np.pi * cutoff / SR)
        acc = (1 - a) * v + a * acc
        sweep[i] = acc
    return sweep * np.sin(np.pi * t / t[-1]) ** 2 * 1.6


def pop():
    t = t_axis(0.25)
    f = 500 + 900 * np.exp(-t * 30)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 18)


def tap():
    t = t_axis(0.08)
    return (np.sin(2 * np.pi * 180 * t) + 0.4 * rng.standard_normal(len(t))) * np.exp(-t * 60)


def ash():
    t = t_axis(1.0)
    grains = np.zeros_like(t)
    for _ in range(40):
        place(grains, rng.standard_normal(int(0.01 * SR)) * np.hanning(int(0.01 * SR)), rng.uniform(0, 0.9), 0.4)
    f = 900 * np.exp(-t * 2.5) + 120
    sigh = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 3) * 0.5
    return lowpass(grains, 3000) * 2 + sigh


def shimmer():
    t = t_axis(1.6)
    out = np.zeros_like(t)
    for i, n in enumerate(["C6", "E6", "G6", "C7", "E7"]):
        place(out, pluck(note(n), 1.2), i * 0.07, 0.5)
    return out * np.clip((1.6 - t) / 0.4, 0, 1)


def write(path, data, peak=0.9):
    data = data / max(1e-9, np.max(np.abs(data))) * peak
    path.parent.mkdir(parents=True, exist_ok=True)
    sf.write(path, data.astype(np.float32), SR)
    print(path.relative_to(OUT), f"{len(data) / SR:.2f}s")


write(OUT / "music" / "bed.wav", music(), 0.5)
write(OUT / "sfx" / "tick.wav", tick(), 0.5)
write(OUT / "sfx" / "whoosh.wav", whoosh(), 0.6)
write(OUT / "sfx" / "pop.wav", pop(), 0.6)
write(OUT / "sfx" / "tap.wav", tap(), 0.7)
write(OUT / "sfx" / "ash.wav", ash(), 0.5)
write(OUT / "sfx" / "shimmer.wav", shimmer(), 0.5)
