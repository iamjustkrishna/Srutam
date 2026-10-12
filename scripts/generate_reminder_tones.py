"""Synthesize candidate reminder tones for Srutam (no samples, no downloads).

Each tone is short (about 1-2 s), melodic, starts immediately, rings out
into a soft synthetic room and fades to true silence (no click), and is
normalized to the same loudness so the options compare fairly.

Usage: python scripts/generate_reminder_tones.py <out_dir>
"""
import sys
from pathlib import Path

import numpy as np
import soundfile as sf

SR = 44100


def t_axis(sec):
    return np.arange(int(sec * SR)) / SR


def hz(name):
    names = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}
    semis = names[name[0]] + (1 if "#" in name else 0)
    return 440.0 * 2 ** ((semis - 9) / 12 + (int(name[-1]) - 4))


def place(buf, clip, at, gain=1.0):
    i = int(at * SR)
    j = min(len(buf), i + len(clip))
    buf[i:j] += clip[: j - i] * gain


def lowpass(x, cutoff):
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc = (1 - a) * v + a * acc
        y[i] = acc
    return y


def room(x, mix=0.18, decay=0.72):
    """A small, soft synthetic room: four damped combs + two allpasses."""
    out = np.zeros(len(x) + int(0.8 * SR))
    src = np.concatenate([x, np.zeros(int(0.8 * SR))])
    wet = np.zeros_like(out)
    for ms in (29.7, 37.1, 41.1, 43.7):
        d = int(ms / 1000 * SR)
        y = np.zeros_like(out)
        lp = 0.0
        for i in range(len(out)):
            fb = y[i - d] if i >= d else 0.0
            lp = 0.6 * fb + 0.4 * lp
            y[i] = src[i] + decay * lp
        wet += y
    for ms, g in ((5.0, 0.7), (1.7, 0.7)):
        d = int(ms / 1000 * SR)
        y = np.zeros_like(wet)
        for i in range(len(wet)):
            xd = wet[i - d] if i >= d else 0.0
            yd = y[i - d] if i >= d else 0.0
            y[i] = -g * wet[i] + xd + g * yd
        wet = y
    wet /= max(1e-9, np.max(np.abs(wet)))
    return (1 - mix) * src / max(1e-9, np.max(np.abs(src))) + mix * wet


def finish(x, length):
    x = x[: int(length * SR)]
    fade = int(0.25 * SR)
    x[-fade:] *= np.linspace(1, 0, fade) ** 2
    x[-int(0.01 * SR):] = 0.0
    # Same perceived level: RMS of the loud part, then a -1 dBFS peak ceiling
    head = x[: int(0.4 * SR)]
    x *= 10 ** (-12 / 20) / max(1e-9, np.sqrt(np.mean(head**2)))
    peak = np.max(np.abs(x))
    if peak > 10 ** (-1 / 20):
        x *= 10 ** (-1 / 20) / peak
    return x


# ---------- instruments ----------


def marimba(f, sec=0.9):
    t = t_axis(sec)
    tone = np.sin(2 * np.pi * f * t) * np.exp(-t * 7) + 0.35 * np.sin(2 * np.pi * f * 3.93 * t) * np.exp(-t * 22)
    tone += 0.12 * np.sin(2 * np.pi * f * 9.2 * t) * np.exp(-t * 60)
    click = lowpass(np.random.default_rng(1).standard_normal(len(t)), 2500) * np.exp(-t * 900) * 0.4
    return (tone + click) * np.clip(t / 0.002, 0, 1)


def glass_bell(f, sec=1.4):
    t = t_axis(sec)
    out = np.zeros_like(t)
    for ratio, amp, dec in ((1, 1.0, 3.2), (2.0, 0.45, 4.5), (2.76, 0.35, 6), (5.4, 0.18, 9), (8.9, 0.08, 14)):
        out += amp * np.sin(2 * np.pi * f * ratio * t) * np.exp(-t * dec)
    return out * np.clip(t / 0.003, 0, 1)


def kalimba(f, sec=1.2):
    t = t_axis(sec)
    tone = np.sin(2 * np.pi * f * t) * np.exp(-t * 4.2) + 0.25 * np.sin(2 * np.pi * f * 5.95 * t) * np.exp(-t * 18)
    tone += 0.08 * np.sin(2 * np.pi * f * 2 * t) * np.exp(-t * 6)
    tine = np.sin(2 * np.pi * f * 1.004 * t) * np.exp(-t * 4.2) * 0.3  # slight shimmer
    return (tone + tine) * np.clip(t / 0.0015, 0, 1)


def small_bell(f, sec=2.0):
    """A small brass bell (ghanti): inharmonic partials with slow beating."""
    t = t_axis(sec)
    out = np.zeros_like(t)
    for ratio, amp, dec in ((1, 1.0, 1.8), (2.01, 0.6, 2.4), (2.76, 0.5, 3.0), (3.9, 0.3, 4.0), (5.4, 0.22, 5.5), (6.8, 0.12, 7.5)):
        for det in (-0.6, 0.6):
            out += amp * 0.5 * np.sin(2 * np.pi * (f * ratio + det) * t) * np.exp(-t * dec)
    strike = lowpass(np.random.default_rng(2).standard_normal(len(t)), 6000) * np.exp(-t * 600) * 0.25
    return (out + strike) * np.clip(t / 0.001, 0, 1)


def soft_pad(freqs, sec=1.2, attack=0.05):
    t = t_axis(sec)
    out = sum(np.sin(2 * np.pi * f * t) + 0.2 * np.sin(4 * np.pi * f * t) for f in freqs) / len(freqs)
    return out * np.clip(t / attack, 0, 1) * np.exp(-t * 2.4)


def pluck(f, sec=0.8, bright=1.0):
    t = t_axis(sec)
    tone = np.sin(2 * np.pi * f * t) + 0.4 * bright * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t * 8)
    return tone * np.exp(-t * 6) * np.clip(t / 0.002, 0, 1)


# ---------- the options ----------


def nudge():
    """A friendly tap on the shoulder: two rising marimba notes."""
    buf = np.zeros(int(1.0 * SR))
    place(buf, marimba(hz("G5")), 0.0, 0.9)
    place(buf, marimba(hz("C6")), 0.15, 1.0)
    return finish(room(buf, 0.15), 1.3)


def aha():
    """The moment you remember: a bright three-note glass-bell arpeggio."""
    buf = np.zeros(int(1.4 * SR))
    for i, n in enumerate(["C6", "E6", "G6"]):
        place(buf, glass_bell(hz(n)), i * 0.09, 0.8 + i * 0.1)
    place(buf, glass_bell(hz("C7"), 0.8), 0.3, 0.25)
    return finish(room(buf, 0.2), 1.7)


def kalimba_tone():
    """Warm and organic: a thumb-piano phrase that resolves upward."""
    buf = np.zeros(int(1.3 * SR))
    for i, n in enumerate(["E5", "G5", "B5", "E6"]):
        place(buf, kalimba(hz(n)), i * 0.11, 0.75 + 0.08 * i)
    return finish(room(buf, 0.18), 1.6)


def ghanti():
    """A small brass temple bell, struck twice: calm, unmistakably Indian."""
    buf = np.zeros(int(2.2 * SR))
    place(buf, small_bell(hz("A5")), 0.0, 1.0)
    place(buf, small_bell(hz("A5")), 0.34, 0.55)
    return finish(room(buf, 0.22, 0.75), 2.2)


def spark():
    """The firefly from the story: a quick sparkle landing on a warm chord."""
    buf = np.zeros(int(1.4 * SR))
    for i, n in enumerate(["E7", "C7", "G6", "E6"]):
        place(buf, glass_bell(hz(n), 0.5), i * 0.035, 0.35)
    place(buf, soft_pad([hz("C5"), hz("E5"), hz("G5")]), 0.16, 1.0)
    return finish(room(buf, 0.22), 1.6)


def echo():
    """Srutam means "that which is heard": one note and its fading echoes."""
    buf = np.zeros(int(1.4 * SR))
    note = pluck(hz("D6"), 0.7)
    for i in range(4):
        place(buf, lowpass(note, 9000 / (1 + i * 1.6)), i * 0.17, 0.95 * 0.55**i)
    place(buf, pluck(hz("A6"), 0.6, 0.6), 0.085, 0.35)
    return finish(room(buf, 0.12), 1.4)


OPTIONS = {
    "1-nudge": nudge,
    "2-aha": aha,
    "3-kalimba": kalimba_tone,
    "4-ghanti": ghanti,
    "5-spark": spark,
    "6-echo": echo,
}

if __name__ == "__main__":
    out = Path(sys.argv[1] if len(sys.argv) > 1 else "reminder-tones")
    out.mkdir(parents=True, exist_ok=True)
    for name, fn in OPTIONS.items():
        x = fn()
        sf.write(out / f"{name}.wav", x.astype(np.float32), SR, subtype="PCM_16")
        print(f"{name}: {len(x) / SR:.2f}s peak {20 * np.log10(np.max(np.abs(x))):.1f} dBFS")
