"""Synthesize the score and sound effects for "The 2 a.m. Idea" (no downloads).

Night: a hushed music-box lullaby. The forgotten morning: the alarm, then a
tape-stop into comic silence. The rewind: a tape whir. The payoff: warm,
building major chords. Times mirror src/bedtime/timeline.ts (frame / 30).

Usage: python generate_audio_bedtime.py
"""
from pathlib import Path

import numpy as np
import soundfile as sf

SR = 44100
OUT = Path(__file__).resolve().parent.parent / "public" / "bedtime"
rng = np.random.default_rng(11)

F = 1 / 30
DURATION = 1200 * F
NIGHT1_END = 255 * F
ALARM = 258 * F
REWIND = 345 * F
NIGHT2 = 420 * F
MORNING2 = 820 * F
LATER = 1000 * F
END = 1085 * F


def t_axis(seconds):
    return np.arange(int(seconds * SR)) / SR


def note(name):
    names = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}
    semis = names[name[0]] + (1 if "#" in name else 0) - (1 if "b" in name[1:-1] else 0)
    return 440.0 * 2 ** ((semis - 9) / 12 + (int(name[-1]) - 4))


def lowpass(x, cutoff):
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc = (1 - a) * v + a * acc
        y[i] = acc
    return y


def place(buf, clip, at, gain=1.0):
    i = int(at * SR)
    j = min(len(buf), i + len(clip))
    if j > i:
        buf[i:j] += clip[: j - i] * gain


def pad(chord, seconds, bright=0.4):
    t = t_axis(seconds)
    out = np.zeros_like(t)
    for n in chord:
        f = note(n)
        for det in (-0.08, 0.0, 0.08):
            ff = f * 2 ** (det / 12)
            for h, amp in ((1, 1.0), (2, 0.3 * bright), (3, 0.12 * bright)):
                out += amp * np.sin(2 * np.pi * ff * h * t + rng.uniform(0, 6.28))
    out /= len(chord) * 3
    return out * np.clip(t / 0.8, 0, 1) * np.clip((seconds - t) / 0.8, 0, 1)


def music_box(freq, seconds=1.6):
    t = t_axis(seconds)
    tone = np.sin(2 * np.pi * freq * t) + 0.35 * np.sin(2 * np.pi * freq * 3.01 * t) + 0.15 * np.sin(2 * np.pi * freq * 5.2 * t)
    return tone * np.exp(-t * 3.2) * np.clip(t / 0.003, 0, 1)


def pluck(freq, seconds=1.0):
    t = t_axis(seconds)
    tone = np.sin(2 * np.pi * freq * t) + 0.3 * np.sin(2 * np.pi * freq * 2 * t)
    return tone * np.exp(-t * 5) * np.clip(t / 0.004, 0, 1)


def melody(buf, start, end, notes, step, gain, fn=music_box, seed=0):
    r = np.random.default_rng(seed)
    t = start
    while t < end - 0.3:
        if r.random() < 0.75:
            place(buf, fn(note(notes[r.integers(len(notes))])), t, gain)
        t += step


def music():
    buf = np.zeros(int(DURATION * SR))
    # Night 1: hushed A minor lullaby
    place(buf, lowpass(pad(["A2", "E3", "C4", "G4"], NIGHT1_END + 0.6, 0.2), 1200), 0, 0.45)
    melody(buf, 0.6, NIGHT1_END, ["A4", "C5", "E5", "G5", "E5"], 0.6, 0.12, seed=1)
    # Tape stop right after the alarm: pitch dives to silence
    stop = pad(["A2", "E3", "C4"], 0.9, 0.2)
    t = t_axis(0.9)
    warp = np.cumsum(np.clip(1 - t / 0.9, 0, 1)) / SR
    stop = np.interp(warp * SR, np.arange(len(stop)), stop) * np.clip(1 - t / 0.9, 0, 1)
    place(buf, stop, NIGHT1_END, 0.45)
    # Night 2 + save: warmer F major lullaby
    place(buf, lowpass(pad(["F2", "C3", "A3", "E4"], 6.7, 0.25), 1400), NIGHT2, 0.45)
    place(buf, lowpass(pad(["D3", "A3", "C4", "F4"], MORNING2 - NIGHT2 - 6.5 + 0.6, 0.25), 1400), NIGHT2 + 6.5, 0.45)
    melody(buf, NIGHT2 + 0.4, MORNING2, ["F4", "A4", "C5", "E5", "G5"], 0.7, 0.1, seed=2)
    # Morning 2: bright, building
    prog = [["C3", "G3", "E4"], ["G2", "D3", "B3"], ["A2", "E3", "C4"], ["F2", "C3", "A3"]]
    at = MORNING2
    for chord in prog:
        place(buf, lowpass(pad(chord, 1.7, 0.6), 2600), at, 0.5)
        at += 1.5
    melody(buf, MORNING2 + 0.3, LATER, ["C5", "D5", "E5", "G5", "A5"], 0.25, 0.09, fn=pluck, seed=3)
    # Three months later + end: full and warm, resolving
    place(buf, lowpass(pad(["F2", "C3", "A3", "C5"], END - LATER + 0.6, 0.9), 3200), LATER, 0.6)
    resolve = pad(["C3", "G3", "C4", "E4", "G4"], DURATION - END, 0.8)
    resolve *= np.linspace(1, 0, len(resolve)) ** 1.3
    place(buf, lowpass(resolve, 3000), END, 0.6)
    melody(buf, LATER + 0.2, DURATION - 1.5, ["C5", "E5", "G5", "A5", "C6"], 0.25, 0.1, fn=pluck, seed=4)
    return buf


def crickets(seconds=8.0):
    buf = np.zeros(int(seconds * SR))
    for k in range(int(seconds * 2.2)):
        at = rng.uniform(0, seconds - 0.3)
        t = t_axis(0.18)
        chirp = np.sin(2 * np.pi * rng.uniform(4200, 5200) * t) * (np.sin(2 * np.pi * 30 * t) > 0) * np.exp(-t * 8)
        place(buf, chirp, at, rng.uniform(0.2, 0.5))
    return buf


def birds(seconds=6.0):
    buf = np.zeros(int(seconds * SR))
    for k in range(9):
        at = rng.uniform(0, seconds - 0.4)
        t = t_axis(0.22)
        f = rng.uniform(2600, 3600) + 900 * np.sin(2 * np.pi * rng.uniform(6, 11) * t)
        chirp = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.sin(np.pi * t / t[-1])
        place(buf, chirp, at, rng.uniform(0.3, 0.6))
    return buf


def alarm():
    buf = np.zeros(int(1.1 * SR))
    for i in range(4):
        t = t_axis(0.12)
        beep = np.sign(np.sin(2 * np.pi * 1900 * t)) * 0.5 * np.clip(t / 0.005, 0, 1) * np.clip((0.12 - t) / 0.005, 0, 1)
        place(buf, beep, i * 0.26)
    return buf


def rewind():
    t = t_axis(2.4)
    f = 300 + 2600 * (t / t[-1]) ** 1.5
    whir = np.sin(2 * np.pi * np.cumsum(f) / SR) * 0.4 + rng.standard_normal(len(t)) * 0.25
    return lowpass(whir, 5000) * np.clip(t / 0.1, 0, 1) * np.clip((t[-1] - t) / 0.2, 0, 1)


def ding():
    out = np.zeros(int(1.8 * SR))
    for i, n in enumerate(["E6", "G#6", "B6"]):
        place(out, music_box(note(n), 1.6), i * 0.06, 0.6)
    return out


def write(path, data, peak=0.9):
    data = data / max(1e-9, np.max(np.abs(data))) * peak
    path.parent.mkdir(parents=True, exist_ok=True)
    sf.write(path, data.astype(np.float32), SR)
    print(path.relative_to(OUT.parent), f"{len(data) / SR:.2f}s")


write(OUT / "music.wav", music(), 0.5)
write(OUT / "crickets.wav", crickets(), 0.35)
write(OUT / "birds.wav", birds(), 0.4)
write(OUT / "alarm.wav", alarm(), 0.5)
write(OUT / "rewind.wav", rewind(), 0.5)
write(OUT / "ding.wav", ding(), 0.6)
