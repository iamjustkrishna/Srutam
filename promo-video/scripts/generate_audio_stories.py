"""Score, sound effects and lip-sync data for "Knock Knock" and "Nani's Laddoos".

Everything is synthesized here (no downloads). Times mirror the frame numbers
in src/knock/timeline.ts and src/nani/timeline.ts (seconds = frame / 30).
Lip-sync: the loudness of each voice line per video frame, written to
src/<video>/lipsync.ts so mouths open with the real voice.

Usage: python scripts/generate_audio_stories.py
"""
from pathlib import Path

import numpy as np
import soundfile as sf

SR = 44100
ROOT = Path(__file__).resolve().parent.parent
PUB = ROOT / "public"
rng = np.random.default_rng(23)
F = 1 / 30


def t_axis(sec):
    return np.arange(int(sec * SR)) / SR


def hz(name):
    names = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}
    semis = names[name[0]] + name.count("#") - name[1:].count("b")
    return 440.0 * 2 ** ((semis - 9) / 12 + (int(name[-1]) - 4))


def shape(x, fc, kind="lp", order=2):
    """Gentle spectral low/high-pass (offline, so an FFT mask is fine)."""
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR) + 1e-9
    g = 1 / (1 + (f / fc) ** (2 * order)) if kind == "lp" else 1 / (1 + (fc / f) ** (2 * order))
    return np.fft.irfft(spec * g, len(x))


def place(buf, clip, at, gain=1.0):
    i = int(at * SR)
    j = min(len(buf), i + len(clip))
    if j > i >= 0:
        buf[i:j] += clip[: j - i] * gain


def env(t, attack, decay):
    return np.clip(t / attack, 0, 1) * np.exp(-t * decay)


# ---------- instruments ----------


def pizz(f, sec=0.5):
    t = t_axis(sec)
    tone = sum(np.sin(2 * np.pi * f * k * t) * np.exp(-t * (9 + 6 * k)) / k for k in range(1, 6))
    return tone * np.clip(t / 0.003, 0, 1)


def marimba(f, sec=0.8):
    t = t_axis(sec)
    tone = np.sin(2 * np.pi * f * t) * np.exp(-t * 7) + 0.3 * np.sin(2 * np.pi * f * 3.93 * t) * np.exp(-t * 24)
    return tone * np.clip(t / 0.002, 0, 1)


def piano(f, sec=1.6, vel=1.0):
    t = t_axis(sec)
    out = np.zeros_like(t)
    for k in range(1, 8):
        fk = f * k * np.sqrt(1 + 0.0003 * k * k)
        out += np.sin(2 * np.pi * fk * t) * (0.65 * np.exp(-t * (6 + 2 * k)) + 0.35 * np.exp(-t * (1.6 + 0.4 * k))) / k ** (2.2 - vel)
    return out * np.clip(t / 0.004, 0, 1)


def pad(notes, sec, bright=0.4, attack=0.6):
    t = t_axis(sec)
    out = np.zeros_like(t)
    for n in notes:
        f = hz(n)
        for det in (-0.07, 0.0, 0.07):
            ff = f * 2 ** (det / 12)
            out += np.sin(2 * np.pi * ff * t + rng.uniform(0, 6.28)) + bright * 0.3 * np.sin(4 * np.pi * ff * t)
    out /= len(notes) * 3
    return out * np.clip(t / attack, 0, 1) * np.clip((sec - t) / 0.6, 0, 1)


def strings(notes, sec, attack=1.2):
    """Bowed strings: slightly brighter pad with slow vibrato."""
    t = t_axis(sec)
    out = np.zeros_like(t)
    for n in notes:
        f = hz(n)
        vib = 1 + 0.004 * np.sin(2 * np.pi * 5.2 * t + rng.uniform(0, 6))
        ph = 2 * np.pi * np.cumsum(f * vib) / SR
        out += sum(np.sin(k * ph) / k ** 1.3 for k in range(1, 7))
    out /= len(notes) * 2
    return shape(out, 3200) * np.clip(t / attack, 0, 1) ** 1.5 * np.clip((sec - t) / 0.8, 0, 1)


def sitar(f, sec=1.4, bend=0.0):
    """Sitar-ish pluck: bright decaying harmonics, a buzzy bridge and an optional meend (bend)."""
    t = t_axis(sec)
    freq = f * (1 + bend * np.clip(t / 0.18, 0, 1))
    ph = 2 * np.pi * np.cumsum(freq) / SR
    tone = sum(np.sin(k * ph) * np.exp(-t * (2.5 + 1.6 * k)) / k ** 0.7 for k in range(1, 14))
    buzz = np.tanh(tone * 2.2) * 0.35
    return (tone * 0.6 + buzz) * np.clip(t / 0.002, 0, 1)


def tanpura(root, sec):
    """A four-string tanpura cycle (Pa Sa Sa Sa-low) with a jawari shimmer, looped."""
    out = np.zeros(int(sec * SR))
    cycle = [hz(root) * 1.5 / 2, hz(root), hz(root), hz(root) / 2]
    at = 0.0
    i = 0
    while at < sec:
        f = cycle[i % 4]
        n = t_axis(3.2)
        ph = 2 * np.pi * f * n
        tone = sum(np.sin(k * ph) * (0.6 + 0.4 * np.sin(2 * np.pi * (0.4 + 0.25 * k) * n)) / k ** 0.9 for k in range(1, 16))
        place(out, tone * np.clip(n / 0.05, 0, 1) * np.exp(-n * 0.7), at, 0.25)
        at += 0.8
        i += 1
    return shape(out, 4000)


def bell(f, sec=2.5):
    t = t_axis(sec)
    out = sum(a * np.sin(2 * np.pi * f * r * t) * np.exp(-t * d) for r, a, d in ((1, 1, 1.4), (2.76, 0.5, 2.6), (5.4, 0.25, 4.5), (8.9, 0.12, 7)))
    return out * np.clip(t / 0.002, 0, 1)


def tabla(kind):
    t = t_axis(0.5)
    if kind == "na":
        f = 520 * (1 + 0.04 * np.exp(-t * 40))
        return (np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 14) + 0.3 * np.sin(2 * np.pi * 1300 * t) * np.exp(-t * 40))
    f = 90 + 70 * np.exp(-t * 9)  # "ge": the bayan's swooping bass
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 6)


def noise(sec):
    return rng.standard_normal(int(sec * SR))


# ---------- Knock Knock ----------

K_TOTAL = 945 * F


def knock_music():
    buf = np.zeros(int((K_TOTAL + 1) * SR))
    beat = 60 / 104
    # Hook: two curious plucks after "Who's there?", then a hush under the title
    place(buf, pizz(hz("C5")), 2.62, 0.5)
    place(buf, pizz(hz("A4")), 2.85, 0.5)
    place(buf, shape(pad(["D3", "A3", "F4"], 1.2, 0.2), 1500), 2.7, 0.35)

    def bouncy(start, end, chords, gain=1.0, bass=True):
        at, i = start, 0
        while at < end - 0.05:
            root, third, fifth = chords[(i // 4) % len(chords)]
            if bass and i % 2 == 0:
                place(buf, pizz(hz(root), 0.4), at, 0.55 * gain)
            place(buf, marimba(hz([third, fifth, third, fifth][i % 4]), 0.5), at + beat / 2, 0.22 * gain)
            sh = shape(noise(0.06), 6000, "hp") * np.exp(-t_axis(0.06) * 60)
            place(buf, sh, at, 0.05 * gain)
            place(buf, sh, at + beat / 2, 0.03 * gain)
            at += beat
            i += 1

    # The promise: bouncy F - Bb - F - C
    bouncy(3.5, 8.5, [("F2", "A5", "C6"), ("A#1", "D6", "F6"), ("F2", "A5", "C6"), ("C2", "G5", "E6")])
    # Life happens: a lonely descending piano in D minor over a dull pad
    place(buf, shape(pad(["D3", "F3", "A3"], 5.0, 0.1), 900), 8.5, 0.45)
    for i, n in enumerate(["A4", "G4", "F4", "E4", "D4"]):
        place(buf, piano(hz(n), 1.8, 0.8), 8.9 + i * 0.9, 0.35)
    # Say it once: light, building again (no bass at first)
    bouncy(13.5, 15.2, [("F2", "A5", "C6")], 0.6, bass=False)
    bouncy(15.2, 19.8, [("A#1", "D6", "F6"), ("C2", "E6", "G6")], 0.8)
    for i, n in enumerate(["C6", "F6", "A6"]):
        place(buf, bell(hz(n), 1.4), 18.0 + i * 0.05, 0.12)
    # (silence from 19.8 for the knock at 20.0)
    # Payoff build: brighter, fuller
    bouncy(20.6, 25.0, [("F2", "A5", "C6"), ("E2", "G5", "C6"), ("D2", "F5", "A5"), ("A#1", "D6", "F6")], 1.0)
    place(buf, shape(pad(["F3", "A3", "C4", "G4"], 4.6, 0.6), 2600), 20.6, 0.35)
    # Video call: warm pad and a few marimba notes
    place(buf, shape(pad(["F3", "C4", "A4", "G5"], 2.6, 0.7), 3000), 25.0, 0.45)
    for i, n in enumerate(["C6", "A5", "F5", "G5"]):
        place(buf, marimba(hz(n)), 25.2 + i * 0.5, 0.2)
    # End: resolve after the logo knock
    end = K_TOTAL - 27.85
    res = pad(["F2", "C3", "A3", "F4", "G4", "C5"], end, 0.8, 0.3)
    res *= np.linspace(1, 0, len(res)) ** 1.4
    place(buf, shape(res, 3000), 27.85, 0.55)
    for i, n in enumerate(["F5", "A5", "C6", "F6"]):
        place(buf, bell(hz(n), 2.2), 27.9 + i * 0.07, 0.1)
    return buf


def street():
    sec = 5.2
    hum = shape(noise(sec), 400) * 0.6 + shape(noise(sec), 2500, "hp") * 0.03
    out = hum * (0.8 + 0.2 * np.sin(2 * np.pi * 0.3 * t_axis(sec)))
    # an auto-rickshaw putt-putt passing by
    t = t_axis(sec)
    putt = np.sin(2 * np.pi * 55 * t) * (np.sin(2 * np.pi * 9 * t) > 0.3) * np.exp(-((t - 2.2) ** 2) / 1.2)
    return out + shape(putt, 600) * 1.4


def horn():
    t = t_axis(0.5)
    tone = np.sign(np.sin(2 * np.pi * 420 * t)) + np.sign(np.sin(2 * np.pi * 530 * t))
    return shape(tone, 2500) * np.clip(t / 0.01, 0, 1) * np.clip((0.5 - t) / 0.05, 0, 1) * (t < 0.18) + 0


def tv_laugh(sec=1.6):
    """A muffled studio-audience laugh coming from the TV."""
    t = t_axis(sec)
    out = np.zeros_like(t)
    for _ in range(14):
        f0 = rng.uniform(180, 320)
        rate = rng.uniform(5, 8)
        ph = 2 * np.pi * np.cumsum(f0 * (1 + 0.05 * np.sin(2 * np.pi * 3 * t))) / SR
        voice = np.tanh(2 * np.sin(ph)) * (0.5 + 0.5 * np.sin(2 * np.pi * rate * t + rng.uniform(0, 6))) ** 2
        out += voice * np.exp(-t * rng.uniform(0.6, 1.4))
    return shape(shape(out, 2200), 250, "hp") * np.clip(t / 0.05, 0, 1)


def clock(sec=5.2):
    out = np.zeros(int(sec * SR))
    for i in range(int(sec)):
        click = shape(noise(0.03), 3000, "hp") * np.exp(-t_axis(0.03) * 300)
        place(out, click, i + 0.1, 0.5 if i % 2 else 0.35)
    return out


# ---------- Nani's Laddoos ----------

N_TOTAL = 1245 * F


def nani_music():
    buf = np.zeros(int((N_TOTAL + 1) * SR))
    place(buf, tanpura("D3", N_TOTAL), 0.0, 1.0)

    def phrase(start, notes, step=0.42, gain=0.3, bends=None):
        for i, n in enumerate(notes):
            if n:
                place(buf, sitar(hz(n), 1.4, (bends or {}).get(i, 0.0)), start + i * step, gain)

    # Kitchen: a warm, playful sitar line in D (Yaman colour with G#)
    phrase(0.4, ["A4", None, "B4", "C#5", "D5", None, "C#5", "B4", "A4"], 0.42, 0.28, {4: 0.03})
    phrase(4.6, ["F#4", "A4", None, "B4", "A4", "G#4", "A4"], 0.4, 0.22)
    # Let it listen: the motif, softer
    phrase(8.2, ["A4", "B4", "D5", None, "C#5", "A4"], 0.45, 0.18)
    # Diwali night: drone, a soft B minor pad and distant bells
    place(buf, shape(pad(["B2", "D3", "F#3"], 7.4, 0.2, 1.0), 1200), 12.7, 0.5)
    for at, n in ((13.2, "F#5"), (15.6, "D5"), (18.0, "B4")):
        place(buf, bell(hz(n), 3.0), at, 0.12)
    # Ask: a curious little figure
    phrase(20.2, ["F#4", "A4", "B4", None, "A4", "B4", "D5"], 0.36, 0.16)
    # Her voice: strings swell D - Bm - G - A, opening up after "pyaar"
    for i, ch in enumerate([["D3", "A3", "F#4"], ["B2", "F#3", "D4"], ["G2", "D3", "B3"], ["A2", "E3", "C#4"]]):
        place(buf, strings(ch, 2.3), 26.0 + i * 2.0, 0.32 + 0.1 * i)
    place(buf, strings(["D3", "A3", "D4", "F#4"], 3.2, 0.6), 33.8, 0.55)
    # Video call: tabla pulse and a happy sitar line, resolving on D
    beat = 60 / 92
    at = 34.0
    i = 0
    while at < 39.2:
        place(buf, tabla("ge" if i % 4 in (0, 3) else "na"), at, 0.35 if i % 4 == 0 else 0.22)
        place(buf, tabla("na"), at + beat / 2, 0.12)
        at += beat
        i += 1
    phrase(34.4, ["D5", "E5", "F#5", "A5", None, "F#5", "E5", "D5"], 0.33, 0.2)
    end = N_TOTAL - 38.4
    res = pad(["D3", "A3", "D4", "F#4", "A4"], end, 0.6, 0.4)
    res *= np.linspace(1, 0, len(res)) ** 1.3
    place(buf, shape(res, 3000), 38.4, 0.5)
    for i, n in enumerate(["D5", "F#5", "A5", "D6"]):
        place(buf, bell(hz(n), 2.6), 37.4 + i * 0.08, 0.09)
    return buf


def sizzle(sec=8.5):
    t = t_axis(sec)
    crackle = np.zeros_like(t)
    for _ in range(int(sec * 60)):
        at = rng.uniform(0, sec - 0.02)
        pop = shape(noise(0.012), 3500, "hp") * np.exp(-t_axis(0.012) * 400)
        place(crackle, pop, at, rng.uniform(0.1, 0.6))
    hiss = shape(shape(noise(sec), 2500, "hp"), 9000) * 0.12
    return (crackle + hiss) * np.clip(t / 0.4, 0, 1) * np.clip((sec - t) / 0.6, 0, 1)


def crackers(sec=7.4):
    out = np.zeros(int(sec * SR))
    for _ in range(9):
        at = rng.uniform(0.2, sec - 1.2)
        boom = shape(noise(1.0), 180) * np.exp(-t_axis(1.0) * 4)
        place(out, boom, at, rng.uniform(0.6, 1.2))
        for k in range(rng.integers(3, 9)):  # a string of distant pops
            pop = shape(shape(noise(0.03), 1500, "hp"), 5000) * np.exp(-t_axis(0.03) * 150)
            place(out, pop, at + 0.15 + k * rng.uniform(0.05, 0.12), rng.uniform(0.2, 0.5))
    return shape(out, 3000)


def typing(sec, rate=13):
    out = np.zeros(int(sec * SR))
    at = 0.0
    while at < sec - 0.05:
        tick = shape(noise(0.02), 2500, "hp") * np.exp(-t_axis(0.02) * 350)
        place(out, tick, at, rng.uniform(0.3, 0.6))
        at += rng.uniform(0.6, 1.4) / rate
    return out


# ---------- Who Said That? (animation test) ----------

M_TEST = 405 * F


def bam():
    t = t_axis(0.8)
    thump = np.sin(2 * np.pi * np.cumsum(140 * np.exp(-t * 18) + 45) / SR) * np.exp(-t * 7)
    slap = shape(noise(0.8), 1800, "hp") * np.exp(-t * 60)
    cups = sum(np.sin(2 * np.pi * f * t) * np.exp(-t * 12) for f in (1870, 2410, 3120)) * 0.15  # cups rattle
    return thump + slap * 0.6 + cups * (t > 0.03)


def boing(f0=330):
    t = t_axis(0.6)
    f = f0 * (1 + 0.9 * (1 - np.exp(-t * 8))) * (1 + 0.06 * np.sin(2 * np.pi * 14 * t))
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 5) * np.clip(t / 0.01, 0, 1)


def scratch():
    t = t_axis(0.55)
    speed = np.sin(2 * np.pi * 3.2 * t) ** 2 * 2.2
    n = noise(0.55)
    src = np.interp(np.cumsum(speed) * 40 % len(n), np.arange(len(n)), n)
    return shape(shape(src, 3500), 400, "hp") * np.clip(t / 0.02, 0, 1) * np.exp(-t * 2)


def sting():
    t = t_axis(2.2)
    out = np.zeros_like(t)
    for n in ("C2", "G2", "C3", "D#3", "G3"):
        ph = 2 * np.pi * hz(n) * t
        out += sum(np.sin(k * ph) / k for k in range(1, 9))
    timp = np.sin(2 * np.pi * np.cumsum(90 + 40 * np.exp(-t * 10)) / SR) * np.exp(-t * 3)
    return shape(out / 5, 2400) * np.exp(-t * 1.6) * np.clip(t / 0.01, 0, 1) + timp * 0.8


def meeting_test_music():
    buf = np.zeros(int((M_TEST + 1) * SR))
    # Rising tension before the slam
    t = t_axis(0.6)
    place(buf, shape(noise(0.6), 300) * np.clip(t / 0.6, 0, 1) ** 2, 0.0, 0.5)
    # After the shout: a comic pizzicato walk for "Her!" / "Him!"
    beat = 60 / 132
    at, i = 5.0, 0
    for n in ["C3", "D#3", "G3", "F#3", "F3", "D#3", "C3", "G2"]:
        place(buf, pizz(hz(n), 0.35), at + i * beat / 2, 0.5)
        i += 1
    # Meera takes over: a light, clever marimba groove in F
    at = 7.6
    notes = ["F5", "A5", "C6", "A5", "G5", "C6", "E6", "C6"]
    i = 0
    while at < 12.6:
        place(buf, marimba(hz(notes[i % len(notes)]), 0.5), at, 0.22)
        if i % 2 == 0:
            place(buf, pizz(hz(["F2", "C3", "D3", "A#2"][(i // 4) % 4]), 0.45), at, 0.4)
        at += beat / 2
        i += 1
    end = M_TEST - 12.0
    res = pad(["F3", "C4", "A4", "G5"], end, 0.6, 0.2)
    res *= np.linspace(1, 0, len(res)) ** 1.2
    place(buf, shape(res, 3000), 12.0, 0.4)
    return buf


# ---------- Who Said That? (full) and 2 a.m. Before the Exam ----------

W_TOTAL = 1310 * F
E_TOTAL = 1235 * F


def office_hum(sec):
    t = t_axis(sec)
    return shape(noise(sec), 220) * 0.5 + np.sin(2 * np.pi * 120 * t) * 0.02 + shape(noise(sec), 3000, "hp") * 0.01


def cricket(sec=1.6):
    out = np.zeros(int(sec * SR))
    for k in range(3):
        t = t_axis(0.22)
        chirp = np.sin(2 * np.pi * 4600 * t) * (np.sin(2 * np.pi * 32 * t) > 0) * np.exp(-t * 6)
        place(out, chirp, 0.1 + k * 0.45, 0.6)
    return out


def paper_flip():
    t = t_axis(0.35)
    return shape(shape(noise(0.35), 2500, "hp"), 9000) * np.exp(-t * 9) * (1 + 0.6 * np.sin(2 * np.pi * 26 * t))


def chalk(sec=1.6):
    out = np.zeros(int(sec * SR))
    at = 0.0
    while at < sec - 0.2:
        t = t_axis(0.16)
        tone = shape(noise(0.16), 2800, "hp") * (0.6 + 0.4 * np.sin(2 * np.pi * 80 * t)) * np.exp(-t * 8)
        place(out, tone, at, rng.uniform(0.4, 0.8))
        at += rng.uniform(0.18, 0.3)
    return out


def bell_ring(sec=2.2):
    t = t_axis(sec)
    ring = sum(np.sin(2 * np.pi * f * t) for f in (1480, 1520, 2960)) * (np.sin(2 * np.pi * 22 * t) > -0.2)
    return ring * np.clip(t / 0.01, 0, 1) * np.clip((1.4 - t) / 0.3, 0, 1) * 0.3


def door_bang():
    t = t_axis(0.7)
    return np.sin(2 * np.pi * np.cumsum(110 * np.exp(-t * 10) + 60) / SR) * np.exp(-t * 9) + shape(noise(0.7), 1200) * np.exp(-t * 30) * 0.6


def clink():
    t = t_axis(0.8)
    return sum(np.sin(2 * np.pi * f * t) * np.exp(-t * d) for f, d in ((2300, 7), (3550, 10), (5100, 14))) * 0.4


def door_knock():
    """Two knuckle raps on a wooden door: a low body thump plus a short click."""
    out = np.zeros(int(0.9 * SR))
    for k, at in enumerate((0.0, 0.2, 0.48)):
        t = t_axis(0.25)
        body = np.sin(2 * np.pi * np.cumsum(170 * np.exp(-t * 25) + 95) / SR) * np.exp(-t * 32)
        click = shape(noise(0.25), 2400) * np.exp(-t * 180) * 0.5
        place(out, body + click, at, 1.0 if k != 1 else 0.85)
    return out


def rewind(sec=0.9):
    """Tape-rewind whir: a filtered noise band sweeping up."""
    t = t_axis(sec)
    sweep = np.sin(2 * np.pi * np.cumsum(300 + 2600 * (t / sec) ** 2) / SR)
    return (sweep * 0.3 + shape(noise(sec), 3000) * 0.5) * np.clip(t / 0.05, 0, 1) * np.clip((sec - t) / 0.15, 0, 1)


def snore(sec=2.6):
    """A slow cartoon snore: a rough low in-breath and a whistling out-breath."""
    out = np.zeros(int(sec * SR))
    t = t_axis(1.0)
    rough = shape(noise(1.0), 400) * (0.6 + 0.4 * np.sin(2 * np.pi * 34 * t)) * np.sin(np.pi * t) ** 2
    whistle = np.sin(2 * np.pi * np.cumsum(900 - 300 * t) / SR) * np.sin(np.pi * t) ** 3 * 0.12
    place(out, rough, 0.0, 0.8)
    place(out, whistle, 1.2, 1.0)
    return out


def scribble(sec=1.2):
    """Pen on paper: bursts of bright friction noise."""
    out = np.zeros(int(sec * SR))
    at = 0.0
    while at < sec - 0.15:
        d = rng.uniform(0.06, 0.14)
        t = t_axis(d)
        place(out, shape(shape(noise(d), 1800, "hp"), 7000) * np.sin(np.pi * t / d), at, rng.uniform(0.5, 1.0))
        at += d + rng.uniform(0.01, 0.05)
    return out


# ---------- v2: a quieter sound kit that supports the cut instead of taking focus ----------


def soft_air(sec=0.7, lo=180, hi=1600):
    """A soft whoosh for a cut: a band of air that swells and passes."""
    t = t_axis(sec)
    env = np.sin(np.pi * np.clip(t / sec, 0, 1)) ** 2.2
    x = shape(shape(noise(sec), hi), lo, "hp") * env
    return x * (0.7 + 0.3 * np.sin(2 * np.pi * 1.3 * t))


def soft_swell(sec=1.1):
    """A gentle rise into a transition (filter opens, then fades fast)."""
    t = t_axis(sec)
    env = (t / sec) ** 2 * np.clip((sec - t) / 0.12, 0, 1)
    sweep = np.sin(2 * np.pi * np.cumsum(220 + 440 * (t / sec) ** 2) / SR) * 0.25
    n = len(t)
    lows, highs = shape(noise(sec), 900), shape(noise(sec), 3600)
    k = t / sec
    return (lows * (1 - k) + highs * k * 0.7 + sweep[:n]) * env


def soft_thump():
    """A fist on a meeting table: a felt low thud, no cartoon crack."""
    t = t_axis(0.5)
    body = np.sin(2 * np.pi * np.cumsum(95 * np.exp(-t * 14) + 55) / SR) * np.exp(-t * 11)
    knock = shape(noise(0.5), 700) * np.exp(-t * 60) * 0.35
    return body + knock


def soft_blip():
    """A chat message arriving: two quiet sine notes."""
    out = np.zeros(int(0.3 * SR))
    for i, f in enumerate((988, 1319)):
        t = t_axis(0.16)
        place(out, np.sin(2 * np.pi * f * t) * np.sin(np.pi * t / 0.16) ** 2, i * 0.07, 0.5)
    return out


def soft_tap():
    t = t_axis(0.08)
    return shape(noise(0.08), 3500) * np.exp(-t * 90) + np.sin(2 * np.pi * 1600 * t) * np.exp(-t * 70) * 0.3


def soft_tick():
    t = t_axis(0.05)
    return shape(shape(noise(0.05), 2500, "hp"), 8000) * np.exp(-t * 160)


def soft_page():
    t = t_axis(0.45)
    return shape(shape(noise(0.45), 1500, "hp"), 6000) * np.sin(np.pi * t / 0.45) ** 1.5 * (1 + 0.5 * np.sin(2 * np.pi * 18 * t))


def soft_rewind(sec=0.8):
    t = t_axis(sec)
    env = np.sin(np.pi * t / sec) ** 2
    return (np.sin(2 * np.pi * np.cumsum(500 + 1800 * (1 - t / sec)) / SR) * 0.2 + shape(noise(sec), 2200) * 0.6) * env


def night_room(sec=12.0):
    """2 a.m. in a hostel room: a slow ceiling fan and far crickets."""
    t = t_axis(sec)
    fan = shape(noise(sec), 300) * (0.8 + 0.2 * np.sin(2 * np.pi * 2.2 * t))
    out = fan * 0.6
    at = 0.4
    while at < sec - 0.3:
        place(out, cricket(0.9) * 0.15, at, 0.35)
        at += rng.uniform(1.4, 2.6)
    return out


def class_room(sec=6.0):
    """A lecture hall: low murmur of air and the odd chair creak."""
    return shape(noise(sec), 500) * 0.6 + shape(noise(sec), 2500, "hp") * 0.02


W2_TOTAL = 1340 * F
E2_TOTAL = 1240 * F


def groove(buf, start, end, notes, roots, beat, level=1.0, bass=True):
    """A soft marimba line over a pizzicato bass, on 8ths."""
    at, i = start, 0
    while at < end:
        place(buf, marimba(hz(notes[i % len(notes)]), 0.5), at, 0.15 * level)
        if bass and i % 2 == 0:
            place(buf, pizz(hz(roots[(i // 4) % len(roots)]), 0.45), at, 0.3 * level)
        at += beat / 2
        i += 1


def who_music_v2():
    """Sits under the dialogue: tension after the slam, a light office groove on
    Tuesday, a confident groove for Srutam, nothing under the playback, a warm resolve."""
    buf = np.zeros(int((W2_TOTAL + 1) * SR))
    beat = 60 / 108
    # cold open: a low uneasy drone after the slam
    place(buf, shape(pad(["C2", "G2", "C#3"], 3.0, 0.1, 0.4), 500), 0.6, 0.35)
    # Tuesday: warm plucks (stop at the side-eye)
    at, i = 5.0, 0
    prog = [("C3", ["E5", "G5", "C6"]), ("A2", ["E5", "A5", "C6"]), ("F2", ["F5", "A5", "C6"]), ("G2", ["D5", "G5", "B5"])]
    while at < 16.8:
        root, tri = prog[(i // 4) % 4]
        if i % 2 == 0:
            place(buf, pizz(hz(root), 0.4), at, 0.3)
        place(buf, pizz(hz(tri[i % 3]), 0.35), at + beat / 2, 0.14)
        at += beat
        i += 1
    place(buf, bell(hz("E6"), 2.0), 17.2, 0.05)
    # Srutam: dock and cards
    groove(buf, 19.4, 27.0, ["G5", "B5", "D6", "B5", "A5", "D6", "F#6", "D6"], ["G2", "D3", "E3", "C3"], beat, 0.9)
    # blame: a quiet pizzicato walk
    for j, n in enumerate(["C3", "D#3", "G3", "F#3", "F3", "D#3", "C3", "G2"]):
        place(buf, pizz(hz(n), 0.35), 29.2 + j * beat / 2, 0.32)
    # ask: a hopeful pad; it fades out before the playback
    ask = pad(["G3", "D4", "B4"], 3.9, 0.3, 1.2)
    ask *= np.clip(np.linspace(1.6, 0, len(ask)), 0, 1)
    place(buf, shape(ask, 2400), 32.3, 0.28)
    # payoff and end: the groove returns softly with the laugh, then resolves
    groove(buf, 40.4, 42.2, ["G5", "B5", "D6", "B5"], ["G2", "D3"], beat, 0.8)
    res = pad(["G2", "D3", "B3", "G4", "A4"], W2_TOTAL - 41.6, 0.7, 0.4)
    res *= np.linspace(1, 0, len(res)) ** 1.3
    place(buf, shape(res, 3000), 41.6, 0.35)
    for j, n in enumerate(["G5", "B5", "D6", "G6"]):
        place(buf, bell(hz(n), 2.2), 41.8 + j * 0.08, 0.06)
    return buf


def exam_music_v2():
    """2 a.m. hush, a sepia music box for the memory, near-silence under the lecture
    playback, then a warm groove for chai and the exam."""
    buf = np.zeros(int((E2_TOTAL + 1) * SR))
    # 2 a.m.: low pad, a slow heartbeat pulse
    place(buf, shape(pad(["D2", "A2", "F3"], 8.4, 0.15, 1.2), 700), 0.0, 0.32)
    for k in range(9):
        place(buf, shape(noise(0.12), 120) * np.exp(-t_axis(0.12) * 30), 0.5 + k * 0.95, 0.35)
    place(buf, bell(hz("A5"), 2.4), 7.9, 0.06)
    # memory: music box over a drone
    for k, n in enumerate(["A5", "C6", "E6", "D6", "C6", "A5", "G5", "A5"]):
        place(buf, bell(hz(n), 1.6), 8.6 + k * 0.6, 0.05)
    place(buf, shape(pad(["A2", "E3", "C4"], 4.8, 0.15, 1.0), 800), 8.5, 0.25)
    # ask: a hopeful lift that thins out under the playback
    lift = pad(["F3", "C4", "A4", "E5"], 9.0, 0.4, 1.0)
    lift *= np.concatenate([np.ones(int(3.6 * SR)), np.linspace(1, 0.25, len(lift) - int(3.6 * SR))])
    place(buf, shape(lift, 2200), 13.4, 0.28)
    for k, n in enumerate(["C5", "F5", "A5", "C6"]):
        place(buf, bell(hz(n), 1.8), 21.4 + k * 0.12, 0.06)
    # share, door, chai, time-lapse: an easy groove in F
    beat = 60 / 112
    groove(buf, 23.8, 26.2, ["F5", "A5", "C6", "A5"], ["F2", "C3"], beat, 0.6, bass=False)
    groove(buf, 29.6, 33.6, ["F5", "A5", "C6", "A5", "G5", "C6", "E6", "C6"], ["F2", "C3", "D3", "A#2"], beat, 0.9)
    # exam hall: steady and sure
    groove(buf, 34.8, 37.6, ["C6", "A5", "F5", "A5"], ["F2", "C3"], beat, 0.7)
    res = pad(["F2", "C3", "A3", "F4", "G4"], E2_TOTAL - 37.6, 0.7, 0.3)
    res *= np.linspace(1, 0, len(res)) ** 1.3
    place(buf, shape(res, 3000), 37.6, 0.35)
    return buf


W3_TOTAL = 1456 * F


def who_music_v3():
    """v3 timing: drone after the slam, warm plucks through Tuesday, a groove for Srutam,
    a quiet walk for the blame, a hopeful pad while she asks, nothing under the playback."""
    buf = np.zeros(int((W3_TOTAL + 1) * SR))
    beat = 60 / 108
    place(buf, shape(pad(["C2", "G2", "C#3"], 3.0, 0.1, 0.4), 500), 0.6, 0.35)
    at, i = 6.8, 0
    prog = [("C3", ["E5", "G5", "C6"]), ("A2", ["E5", "A5", "C6"]), ("F2", ["F5", "A5", "C6"]), ("G2", ["D5", "G5", "B5"])]
    while at < 18.5:
        root, tri = prog[(i // 4) % 4]
        if i % 2 == 0:
            place(buf, pizz(hz(root), 0.4), at, 0.3)
        place(buf, pizz(hz(tri[i % 3]), 0.35), at + beat / 2, 0.14)
        at += beat
        i += 1
    place(buf, bell(hz("E6"), 2.0), 19.0, 0.05)
    groove(buf, 21.0, 28.6, ["G5", "B5", "D6", "B5", "A5", "D6", "F#6", "D6"], ["G2", "D3", "E3", "C3"], beat, 0.9)
    for j, n in enumerate(["C3", "D#3", "G3", "F#3", "F3", "D#3", "C3", "G2"]):
        place(buf, pizz(hz(n), 0.35), 31.0 + j * beat / 2, 0.32)
    ask = pad(["G3", "D4", "B4"], 4.8, 0.3, 1.2)
    ask *= np.clip(np.linspace(1.8, 0, len(ask)), 0, 1)
    place(buf, shape(ask, 2400), 33.8, 0.28)
    groove(buf, 44.3, 45.4, ["G5", "B5", "D6", "B5"], ["G2", "D3"], beat, 0.8)
    res = pad(["G2", "D3", "B3", "G4", "A4"], W3_TOTAL - 45.4, 0.7, 0.4)
    res *= np.linspace(1, 0, len(res)) ** 1.3
    place(buf, shape(res, 3000), 45.4, 0.35)
    for j, n in enumerate(["G5", "B5", "D6", "G6"]):
        place(buf, bell(hz(n), 2.2), 45.6 + j * 0.08, 0.06)
    return buf


E3_TOTAL = 1296 * F


def who_music_wide(total=1606 / 30):
    """who_music_v3 retimed for the 16:9 cut (src/wide/who/timeline.ts): plucks from
    the board to the side-eye, the Srutam groove over the table and cards, the blame
    walk, the hopeful pad as she asks, the laugh and the resolve under the band."""
    buf = np.zeros(int((total + 1) * SR))
    beat = 60 / 108
    place(buf, shape(pad(["C2", "G2", "C#3"], 3.0, 0.1, 0.4), 500), 0.6, 0.35)
    at, i = 7.1, 0
    prog = [("C3", ["E5", "G5", "C6"]), ("A2", ["E5", "A5", "C6"]), ("F2", ["F5", "A5", "C6"]), ("G2", ["D5", "G5", "B5"])]
    while at < 22.4:
        root, tri = prog[(i // 4) % 4]
        if i % 2 == 0:
            place(buf, pizz(hz(root), 0.4), at, 0.3)
        place(buf, pizz(hz(tri[i % 3]), 0.35), at + beat / 2, 0.14)
        at += beat
        i += 1
    place(buf, bell(hz("E6"), 2.0), 22.7, 0.05)
    groove(buf, 24.8, 32.6, ["G5", "B5", "D6", "B5", "A5", "D6", "F#6", "D6"], ["G2", "D3", "E3", "C3"], beat, 0.9)
    for j, n in enumerate(["C3", "D#3", "G3", "F#3", "F3", "D#3", "C3", "G2"]):
        place(buf, pizz(hz(n), 0.35), 34.9 + j * beat / 2, 0.32)
    ask = pad(["G3", "D4", "B4"], 4.8, 0.3, 1.2)
    ask *= np.clip(np.linspace(1.8, 0, len(ask)), 0, 1)
    place(buf, shape(ask, 2400), 37.9, 0.28)
    groove(buf, 49.6, 50.5, ["G5", "B5", "D6", "B5"], ["G2", "D3"], beat, 0.8)
    res = pad(["G2", "D3", "B3", "G4", "A4"], total - 50.5, 0.7, 0.4)
    res *= np.linspace(1, 0, len(res)) ** 1.3
    place(buf, shape(res, 3000), 50.5, 0.35)
    for j, n in enumerate(["G5", "B5", "D6", "G6"]):
        place(buf, bell(hz(n), 2.2), 50.7 + j * 0.08, 0.06)
    return buf


def exam_music_wide(total=1364 / 30):
    """exam_music_v3 retimed for the 16:9 cut (src/wide/exam/timeline.ts): the night pad
    and clock ticks, the flashback bells, the lift as she asks, bells on the relief, the
    groove for the share, the time-lapse and the hall, and the resolve under the band."""
    buf = np.zeros(int((total + 1) * SR))
    place(buf, shape(pad(["D2", "A2", "F3"], 8.4, 0.15, 1.2), 700), 0.0, 0.32)
    for k in range(9):
        place(buf, shape(noise(0.12), 120) * np.exp(-t_axis(0.12) * 30), 0.5 + k * 0.95, 0.35)
    place(buf, bell(hz("A5"), 2.4), 7.9, 0.06)
    for k, n in enumerate(["A5", "C6", "E6", "D6", "C6", "A5", "G5", "A5"]):
        place(buf, bell(hz(n), 1.6), 8.7 + k * 0.72, 0.05)
    place(buf, shape(pad(["A2", "E3", "C4"], 6.2, 0.15, 1.0), 800), 8.6, 0.25)
    lift = pad(["F3", "C4", "A4", "E5"], 10.0, 0.4, 1.0)
    lift *= np.concatenate([np.ones(int(4.6 * SR)), np.linspace(1, 0.22, len(lift) - int(4.6 * SR))])
    place(buf, shape(lift, 2200), 14.67, 0.28)
    for k, n in enumerate(["C5", "F5", "A5", "C6"]):
        place(buf, bell(hz(n), 1.8), 23.8 + k * 0.12, 0.06)
    beat = 60 / 112
    groove(buf, 26.7, 29.2, ["F5", "A5", "C6", "A5"], ["F2", "C3"], beat, 0.6, bass=False)
    groove(buf, 34.2, 37.7, ["F5", "A5", "C6", "A5", "G5", "C6", "E6", "C6"], ["F2", "C3", "D3", "A#2"], beat, 0.9)
    groove(buf, 38.87, 41.67, ["C6", "A5", "F5", "A5"], ["F2", "C3"], beat, 0.7)
    res = pad(["F2", "C3", "A3", "F4", "G4"], total - 41.8, 0.7, 0.3)
    res *= np.linspace(1, 0, len(res)) ** 1.3
    place(buf, shape(res, 3000), 41.8, 0.35)
    return buf


def exam_music_v3():
    """v3 timing for 2 a.m. Before the Exam (see src/exam/timeline.ts)."""
    buf = np.zeros(int((E3_TOTAL + 1) * SR))
    place(buf, shape(pad(["D2", "A2", "F3"], 8.4, 0.15, 1.2), 700), 0.0, 0.32)
    for k in range(9):
        place(buf, shape(noise(0.12), 120) * np.exp(-t_axis(0.12) * 30), 0.5 + k * 0.95, 0.35)
    place(buf, bell(hz("A5"), 2.4), 7.9, 0.06)
    for k, n in enumerate(["A5", "C6", "E6", "D6", "C6", "A5", "G5", "A5"]):
        place(buf, bell(hz(n), 1.6), 8.7 + k * 0.62, 0.05)
    place(buf, shape(pad(["A2", "E3", "C4"], 5.4, 0.15, 1.0), 800), 8.6, 0.25)
    lift = pad(["F3", "C4", "A4", "E5"], 10.0, 0.4, 1.0)
    lift *= np.concatenate([np.ones(int(4.6 * SR)), np.linspace(1, 0.22, len(lift) - int(4.6 * SR))])
    place(buf, shape(lift, 2200), 14.0, 0.28)
    for k, n in enumerate(["C5", "F5", "A5", "C6"]):
        place(buf, bell(hz(n), 1.8), 23.0 + k * 0.12, 0.06)
    beat = 60 / 112
    groove(buf, 25.7, 28.2, ["F5", "A5", "C6", "A5"], ["F2", "C3"], beat, 0.6, bass=False)
    groove(buf, 31.9, 35.4, ["F5", "A5", "C6", "A5", "G5", "C6", "E6", "C6"], ["F2", "C3", "D3", "A#2"], beat, 0.9)
    groove(buf, 36.6, 39.4, ["C6", "A5", "F5", "A5"], ["F2", "C3"], beat, 0.7)
    res = pad(["F2", "C3", "A3", "F4", "G4"], E3_TOTAL - 39.5, 0.7, 0.3)
    res *= np.linspace(1, 0, len(res)) ** 1.3
    place(buf, shape(res, 3000), 39.5, 0.35)
    return buf


def who_music():
    buf = np.zeros(int((W_TOTAL + 1) * SR))
    beat = 60 / 112
    # 1-2 Tuesday sync: light office ukulele-ish plucks over a soft pad (stops dead on "note it down")
    at, i = 0.3, 0
    prog = [("C3", ["E5", "G5", "C6"]), ("A2", ["E5", "A5", "C6"]), ("F2", ["F5", "A5", "C6"]), ("G2", ["D5", "G5", "B5"])]
    while at < 10.6:
        root, tri = prog[(i // 4) % 4]
        if i % 2 == 0:
            place(buf, pizz(hz(root), 0.4), at, 0.45)
        place(buf, pizz(hz(tri[i % 3]), 0.35), at + beat / 2, 0.22)
        at += beat
        i += 1
    # 4-5 Srutam: soft confident marimba groove, cards pop on the beat
    at, i = 15.0, 0
    notes = ["G5", "B5", "D6", "B5", "A5", "D6", "F#6", "D6"]
    while at < 23.0:
        place(buf, marimba(hz(notes[i % len(notes)]), 0.5), at, 0.2)
        if i % 2 == 0:
            place(buf, pizz(hz(["G2", "D3", "E3", "C3"][(i // 4) % 4]), 0.45), at, 0.38)
        at += beat / 2
        i += 1
    # 6 one week later: a ticking hush; 7 the slam gets the sting (SFX); 8 comic pizz walk
    at, i = 30.2, 0
    for n in ["C3", "D#3", "G3", "F#3", "F3", "D#3", "C3", "G2"]:
        place(buf, pizz(hz(n), 0.35), at + i * beat / 2, 0.5)
        i += 1
    # 9-10 Meera's reveal and the laugh: the groove returns, then a warm resolve
    at, i = 33.0, 0
    while at < 41.0:
        place(buf, marimba(hz(notes[i % len(notes)]), 0.5), at, 0.2)
        if i % 2 == 0:
            place(buf, pizz(hz(["G2", "D3", "E3", "C3"][(i // 4) % 4]), 0.45), at, 0.4)
        at += beat / 2
        i += 1
    end = W_TOTAL - 40.6
    res = pad(["G2", "D3", "B3", "G4", "A4"], end, 0.7, 0.3)
    res *= np.linspace(1, 0, len(res)) ** 1.3
    place(buf, shape(res, 3000), 40.6, 0.45)
    for i, n in enumerate(["G5", "B5", "D6", "G6"]):
        place(buf, bell(hz(n), 2.2), 40.7 + i * 0.07, 0.09)
    return buf


def exam_music():
    buf = np.zeros(int((E_TOTAL + 1) * SR))
    # 1-2 2 a.m.: ticking tension, low pulse, rising on the panic
    for i in range(16):
        place(buf, shape(noise(0.03), 3000, "hp") * np.exp(-t_axis(0.03) * 300), 0.2 + i * 0.5, 0.25)
    place(buf, shape(pad(["D2", "A2", "F3"], 7.6, 0.2, 1.0), 900), 0.0, 0.45)
    for i, n in enumerate(["D4", "F4", "A4", "C5", "D5", "F5"]):
        place(buf, pizz(hz(n), 0.3), 5.1 + i * 0.25, 0.3)
    # 3-5 flashback: sepia music-box over a drone
    for i, n in enumerate(["A5", "C6", "E6", "D6", "C6", "A5", "G5", "A5", "C6", "E6", "G6", "E6"]):
        place(buf, bell(hz(n), 1.6), 7.8 + i * 0.75, 0.08)
    place(buf, shape(pad(["A2", "E3", "C4"], 10.6, 0.15, 1.0), 800), 7.6, 0.35)
    # 6 the answer: a hopeful lift
    place(buf, shape(pad(["F3", "C4", "A4", "E5"], 8.0, 0.5, 0.8), 2600), 18.4, 0.4)
    for i, n in enumerate(["C5", "F5", "A5", "C6"]):
        place(buf, bell(hz(n), 1.8), 22.6 + i * 0.12, 0.08)
    # 7-10 friends, time-lapse, exam, end: an upbeat groove in F, resolving
    beat = 60 / 118
    at, i = 26.4, 0
    notes = ["F5", "A5", "C6", "A5", "G5", "C6", "E6", "C6"]
    while at < 37.0:
        place(buf, marimba(hz(notes[i % len(notes)]), 0.5), at, 0.2)
        if i % 2 == 0:
            place(buf, pizz(hz(["F2", "C3", "D3", "A#2"][(i // 4) % 4]), 0.45), at, 0.42)
        if i % 4 == 2:
            place(buf, shape(noise(0.08), 4000, "hp") * np.exp(-t_axis(0.08) * 40), at, 0.12)
        at += beat / 2
        i += 1
    end = E_TOTAL - 36.8
    res = pad(["F2", "C3", "A3", "F4", "G4"], end, 0.7, 0.3)
    res *= np.linspace(1, 0, len(res)) ** 1.3
    place(buf, shape(res, 3000), 36.8, 0.45)
    return buf


# ---------- lip-sync ----------


def envelope(path):
    x, sr = sf.read(path)
    if x.ndim == 2:
        x = x.mean(axis=1)
    hop = sr / 30
    n = int(len(x) / hop) + 1
    rms = np.array([np.sqrt(np.mean(x[int(i * hop): int((i + 1) * hop)] ** 2) + 1e-12) for i in range(n)])
    ref = np.percentile(rms[rms > 1e-4], 90) if np.any(rms > 1e-4) else 1
    v = np.clip(rms / ref, 0, 1)
    v = np.convolve(v, [0.25, 0.5, 0.25], mode="same")
    v[v < 0.12] = 0
    return [round(float(a), 2) for a in v]


def write_lipsync(name, files):
    lines = [f"// Generated by scripts/generate_audio_stories.py: voice loudness per frame (0..1).", "export const LIPSYNC: Record<string, number[]> = {"]
    for key, path in files.items():
        lines.append(f'  "{key}": {envelope(path)},')
    lines.append("};")
    out = ROOT / "src" / name / "lipsync.ts"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("\n".join(lines) + "\n")
    print(out.relative_to(ROOT))


def write(path, data, peak=0.9):
    data = data / max(1e-9, np.max(np.abs(data))) * peak
    path.parent.mkdir(parents=True, exist_ok=True)
    sf.write(path, data.astype(np.float32), SR)
    print(path.relative_to(ROOT), f"{len(data) / SR:.2f}s")


if __name__ == "__main__":
    write(PUB / "knock" / "music.wav", knock_music(), 0.6)
    write(PUB / "knock" / "street.wav", street(), 0.5)
    write(PUB / "knock" / "horn.wav", horn(), 0.5)
    write(PUB / "knock" / "tv-laugh.wav", tv_laugh(), 0.5)
    write(PUB / "knock" / "clock.wav", clock(), 0.5)
    write(PUB / "nani" / "music.wav", nani_music(), 0.6)
    write(PUB / "nani" / "sizzle.wav", sizzle(), 0.5)
    write(PUB / "nani" / "crackers.wav", crackers(), 0.6)
    write(PUB / "nani" / "typing.wav", typing(2.4), 0.5)
    write(PUB / "meeting" / "bam.wav", bam(), 0.9)
    write(PUB / "meeting" / "boing.wav", boing(), 0.6)
    write(PUB / "meeting" / "boing-hi.wav", boing(440), 0.6)
    write(PUB / "meeting" / "scratch.wav", scratch(), 0.6)
    write(PUB / "meeting" / "sting.wav", sting(), 0.8)
    write(PUB / "meeting" / "test-music.wav", meeting_test_music(), 0.6)
    write(PUB / "meeting" / "music.wav", who_music(), 0.6)
    write(PUB / "meeting" / "office.wav", office_hum(9.5), 0.3)
    write(PUB / "meeting" / "cricket.wav", cricket(), 0.4)
    write(PUB / "meeting" / "paper.wav", paper_flip(), 0.5)
    write(PUB / "exam" / "music.wav", exam_music(), 0.6)
    write(PUB / "exam" / "chalk.wav", chalk(), 0.5)
    write(PUB / "exam" / "bell.wav", bell_ring(), 0.5)
    write(PUB / "exam" / "door.wav", door_bang(), 0.8)
    write(PUB / "exam" / "clink.wav", clink(), 0.5)
    write(PUB / "exam" / "paper.wav", paper_flip(), 0.5)
    write(PUB / "exam" / "knock-door.wav", door_knock(), 0.8)
    write(PUB / "exam" / "rewind.wav", rewind(), 0.4)
    write(PUB / "exam" / "snore.wav", snore(), 0.5)
    write(PUB / "exam" / "scribble.wav", scribble(), 0.4)
    write(PUB / "meeting" / "music2.wav", who_music_v2(), 0.5)
    write(PUB / "meeting" / "music3.wav", who_music_v3(), 0.5)
    write(PUB / "exam" / "music2.wav", exam_music_v2(), 0.5)
    write(PUB / "exam" / "music3.wav", exam_music_v3(), 0.5)
    # v2 sound kit
    write(PUB / "sfx2" / "air.wav", soft_air(), 0.5)
    write(PUB / "sfx2" / "air-long.wav", soft_air(1.1, 140, 1200), 0.5)
    write(PUB / "sfx2" / "swell.wav", soft_swell(), 0.5)
    write(PUB / "sfx2" / "thump.wav", soft_thump(), 0.7)
    write(PUB / "sfx2" / "blip.wav", soft_blip(), 0.4)
    write(PUB / "sfx2" / "tap.wav", soft_tap(), 0.4)
    write(PUB / "sfx2" / "tick.wav", soft_tick(), 0.35)
    write(PUB / "sfx2" / "page.wav", soft_page(), 0.4)
    write(PUB / "sfx2" / "rewind.wav", soft_rewind(), 0.4)
    write(PUB / "sfx2" / "night.wav", night_room(), 0.3)
    write(PUB / "sfx2" / "class.wav", class_room(), 0.3)
    vo = PUB / "voiceover"
    write_lipsync("knock", {
        **{p.stem: p for p in sorted((vo / "knock" / "meera").glob("k*.mp3"))},
        "ma-remembered": vo / "knock" / "ma" / "ma-remembered.wav",
    })
    write_lipsync("nani", {
        **{p.stem: p for p in sorted((vo / "nani" / "meera").glob("n*.mp3"))},
        **{p.stem: p for p in sorted((vo / "nani" / "nani").glob("*.wav"))},
    })
