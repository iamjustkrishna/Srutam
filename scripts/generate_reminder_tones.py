"""Synthesize candidate reminder tones for Srutam (no samples, no downloads).

Each tone is short (about 1-2 s), melodic, starts immediately, rings out
into a soft synthetic room and fades to true silence (no click), and is
normalized to the same loudness so the options compare fairly.

The "premium" set is stereo and FM-synthesized (bells, electric piano, a
voice-like hum), with a richer feedback-delay-network reverb.

Usage: python scripts/generate_reminder_tones.py <out_dir> [classic|premium|pop|iphone]
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

# ---------- premium set: stereo, FM, airy ----------


def fm(fc, ratio, index, idx_decay, amp_decay, sec, attack=0.002):
    """Two-operator FM voice: bells (non-integer ratio) or electric piano (1:1)."""
    t = t_axis(sec)
    mod = index * np.exp(-t * idx_decay) * np.sin(2 * np.pi * fc * ratio * t)
    return np.sin(2 * np.pi * fc * t + mod) * np.exp(-t * amp_decay) * np.clip(t / attack, 0, 1)


def epiano(f, sec=1.6, vel=1.0):
    """Warm FM electric piano with a glassy tine at the attack."""
    t = t_axis(sec)
    body = fm(f, 1.0, 1.6 * vel, 5.5, 2.4, sec)
    tine = 0.16 * vel * np.sin(2 * np.pi * f * 14 * t) * np.exp(-t * 70)
    return body + tine


def fm_bell(f, sec=1.6, bright=1.0):
    return fm(f, 3.5, 2.2 * bright, 3.0, 2.6, sec) + 0.35 * fm(f * 2.0, 1.41, 1.0 * bright, 6, 4.0, sec)


def tactile(sec=0.012, cutoff=7000):
    """A tiny high transient, the "click" that makes a tone feel physical."""
    t = t_axis(sec)
    noise = np.random.default_rng(5).standard_normal(len(t))
    return (noise - lowpass(noise, cutoff)) * np.exp(-t * 700)


def fdn(x, seconds=1.4, mix=0.25, damp=0.45):
    """Stereo 8-line feedback delay network reverb (Hadamard mixing).

    x may be mono or stereo; the dry signal keeps its own panning.
    """
    lines = [1031, 1327, 1523, 1801, 2053, 2311, 2591, 2833]
    n = len(x) + int(seconds * SR)
    pad = np.zeros(int(seconds * SR))
    stereo_in = x if x.ndim == 2 else np.stack([x, x], 1)
    dry = np.concatenate([stereo_in, np.stack([pad, pad], 1)])
    src = dry.mean(axis=1)
    g = 10 ** (-3 * np.mean(lines) / (seconds * SR))  # ~60 dB decay over `seconds`
    h = np.array([[1]])
    while h.shape[0] < 8:
        h = np.block([[h, h], [h, -h]])
    h = h / np.sqrt(8)
    bufs = [np.zeros(d) for d in lines]
    idx = [0] * 8
    lp = np.zeros(8)
    left = np.zeros(n)
    right = np.zeros(n)
    for i in range(n):
        outs = np.array([bufs[k][idx[k]] for k in range(8)])
        lp = (1 - damp) * outs + damp * lp
        fb = h @ lp * g
        for k in range(8):
            bufs[k][idx[k]] = src[i] * 0.35 + fb[k]
            idx[k] = (idx[k] + 1) % lines[k]
        left[i] = outs[0] + outs[2] + outs[4] + outs[6]
        right[i] = outs[1] + outs[3] + outs[5] + outs[7]
    wet = np.stack([left, right], 1)
    wet /= max(1e-9, np.max(np.abs(wet)))
    dry = dry / max(1e-9, np.max(np.abs(dry)))
    return (1 - mix) * dry + mix * wet


def pan(x, p):
    """Equal-power pan of a mono clip, p in [-1, 1]."""
    a = (p + 1) * np.pi / 4
    return np.stack([x * np.cos(a), x * np.sin(a)], 1)


def place2(buf, clip, at, gain=1.0):
    i = int(at * SR)
    j = min(len(buf), i + len(clip))
    buf[i:j] += clip[: j - i] * gain


def master(x, length, loud_window=None):
    x = x[: int(length * SR)].copy()
    x = np.tanh(1.15 * x / max(1e-9, np.max(np.abs(x)))) / np.tanh(1.15)
    fade = int(0.3 * SR)
    x[-fade:] *= (np.linspace(1, 0, fade) ** 2)[:, None]
    x[-int(0.01 * SR):] = 0.0
    a, b = loud_window or (0.0, 0.4)
    win = x[int(a * SR): int(b * SR)]
    x *= 10 ** (-13 / 20) / max(1e-9, np.sqrt(np.mean(win**2)))
    peak = np.max(np.abs(x))
    if peak > 10 ** (-1 / 20):
        x *= 10 ** (-1 / 20) / peak
    return x


def surface():
    """A thought surfacing: a reversed swell that blooms into a Cmaj9 bell chord."""
    chord = ["C5", "E5", "G5", "B5", "D6"]
    hit = np.zeros(int(1.8 * SR))
    for i, n in enumerate(chord):
        place(hit, fm_bell(hz(n), 1.8, 0.8), i * 0.012, 0.8)
    tail = fdn(hit, 1.2, mix=1.0)[:, 0]
    swell = tail[: int(0.42 * SR)][::-1] * np.linspace(0, 1, int(0.42 * SR)) ** 2
    buf = np.zeros((int(2.4 * SR), 2))
    place2(buf, pan(swell, 0.0), 0.0, 0.5)
    for i, n in enumerate(chord):
        place2(buf, pan(fm_bell(hz(n), 1.8, 0.8), -0.6 + i * 0.3), 0.42 + i * 0.012, 0.8)
    place2(buf, pan(tactile(), 0.0), 0.42, 0.25)
    return master(fdn(buf, 1.4, 0.3), 2.2, (0.4, 0.8))


def droplet():
    """A memory resurfacing: a water-drop glide that ripples left and right."""
    t = t_axis(0.5)
    f = 650 + 900 * np.exp(-t * 55)
    drop = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 9) * np.clip(t / 0.001, 0, 1)
    buf = np.zeros((int(1.4 * SR), 2))
    for i, (p, g) in enumerate(((0.0, 1.0), (-0.7, 0.45), (0.7, 0.25), (-0.4, 0.12))):
        place2(buf, pan(lowpass(drop, 12000 / (1 + i)), p), i * 0.16, g)
    place2(buf, pan(fm_bell(hz("E6"), 0.9, 0.4), 0.0), 0.02, 0.18)
    return master(fdn(buf, 1.0, 0.18), 1.3)


def hum():
    """A voice app should answer like a voice: a soft, warm "mm-hm".

    Uses a recorded hum when REMINDER_HUM_VOICE points to a WAV (the shipped
    tone uses one from ElevenLabs); otherwise falls back to a synthesized hum.
    """
    import os

    path = os.environ.get("REMINDER_HUM_VOICE")
    if path and Path(path).exists():
        voice, vsr = sf.read(path)
        if voice.ndim == 2:
            voice = voice.mean(axis=1)
        if vsr != SR:
            voice = np.interp(np.arange(int(len(voice) * SR / vsr)) * vsr / SR, np.arange(len(voice)), voice)
        voice = voice / max(1e-9, np.max(np.abs(voice)))
        lead = 0.0
    else:
        sec = 0.8
        t = t_axis(sec)
        pitch = np.where(t < 0.2, 205 + 10 * t, np.where(t < 0.26, 0, 185 + 140 * np.clip((t - 0.26) / 0.34, 0, 1)))
        phase = np.cumsum(pitch) / SR
        src = sum(np.sin(2 * np.pi * k * phase) / k**1.4 for k in range(1, 30))
        gate = np.where(t < 0.2, np.clip(t / 0.03, 0, 1) * np.clip((0.2 - t) / 0.03, 0, 1), 0)
        gate += np.where((t > 0.26) & (t < 0.62), np.clip((t - 0.26) / 0.03, 0, 1) * np.clip((0.62 - t) / 0.08, 0, 1), 0)
        v = src * gate
        voice = lowpass(v, 420) + (lowpass(v, 2600) - lowpass(v, 1900)) * 0.6
        voice /= max(1e-9, np.max(np.abs(voice)))
        lead = 0.0
    buf = np.zeros((int(1.4 * SR), 2))
    place2(buf, pan(voice, -0.12), lead, 1.0)
    place2(buf, pan(voice, 0.12), lead + 0.008, 0.85)  # Haas widening
    place2(buf, pan(epiano(hz("E5"), 1.0, 0.5), -0.35), 0.02, 0.22)
    place2(buf, pan(epiano(hz("B5"), 1.1, 0.6), 0.35), 0.3, 0.3)
    return master(fdn(buf, 0.9, 0.15), 1.3)


def signature():
    """Srutam's sonic logo: "shru-TAM", short then long, rising a fifth, with the logo's waveform bars as a tiny arpeggio."""
    buf = np.zeros((int(2.0 * SR), 2))
    for i, n in enumerate(["G6", "B6", "D7", "G7"]):
        place2(buf, pan(fm_bell(hz(n), 0.4, 0.5), -0.6 + i * 0.4), i * 0.022, 0.12)
    place2(buf, pan(epiano(hz("G5"), 0.6, 0.8), -0.25), 0.1, 0.8)
    place2(buf, pan(epiano(hz("D6"), 1.7, 1.0), 0.25), 0.28, 1.0)
    place2(buf, pan(epiano(hz("D5"), 1.7, 0.6), 0.0), 0.28, 0.4)
    place2(buf, pan(fm_bell(hz("D7"), 1.5, 0.4), 0.5), 0.3, 0.12)
    return master(fdn(buf, 1.3, 0.25), 1.9)


def glass():
    """Two crisp FM glass taps, wide and airy."""
    buf = np.zeros((int(1.4 * SR), 2))
    place2(buf, pan(fm_bell(hz("C7"), 1.0, 1.0), -0.5), 0.0, 0.8)
    place2(buf, pan(tactile(), -0.5), 0.0, 0.3)
    place2(buf, pan(fm_bell(hz("G6"), 1.2, 1.0), 0.5), 0.12, 1.0)
    place2(buf, pan(tactile(), 0.5), 0.12, 0.3)
    place2(buf, pan(fm(hz("G4"), 1.0, 0.4, 4, 5, 0.6), 0.0), 0.12, 0.2)
    return master(fdn(buf, 1.1, 0.3), 1.3)


def halo():
    """A fast, shimmering major-seventh strum: bright, open, modern."""
    buf = np.zeros((int(1.9 * SR), 2))
    notes = ["C5", "E5", "G5", "B5", "E6"]
    for i, n in enumerate(notes):
        place2(buf, pan(epiano(hz(n), 1.6, 0.7) + 0.5 * fm_bell(hz(n) * 2, 1.6, 0.5), -0.8 + i * 0.4), i * 0.028, 0.7)
    t = t_axis(1.4)  # shimmer: octave-up air that blooms after the strum
    shimmer = sum(np.sin(2 * np.pi * hz(n) * 4 * t) for n in notes) * np.clip(t / 0.35, 0, 1) * np.exp(-t * 2.2)
    place2(buf, pan(shimmer, 0.0), 0.1, 0.05)
    return master(fdn(buf, 1.5, 0.3), 1.8)


PREMIUM = {
    "p1-surface": surface,
    "p2-droplet": droplet,
    "p3-hum": hum,
    "p4-signature": signature,
    "p5-glass": glass,
    "p6-halo": halo,
}

# ---------- pop set: short, bouncy, punchy ----------


def bubble(f, sec=0.35, rise=0.45):
    """A bubbly "bloop": a sine that swoops up into its note, the classic pop-app sound."""
    t = t_axis(sec)
    freq = f * (1 - rise * np.exp(-t * 80)) * (1 + 0.05 * t)
    phase = 2 * np.pi * np.cumsum(freq) / SR
    tone = np.sin(phase) + 0.18 * np.sin(2 * phase)
    return tone * np.exp(-t * 13) * np.clip(t / 0.002, 0, 1)


def synth_pluck(f, sec=0.6, bright=1.0, odd_only=False, detune=0.0):
    """Pop-synth pluck: a saw (or square) whose upper harmonics close fast, like a filter envelope."""
    t = t_axis(sec)
    out = np.zeros_like(t)
    k = 1
    while f * k < 12000 and k <= 24:
        if not odd_only or k % 2:
            out += np.sin(2 * np.pi * f * (1 + detune) * k * t) / k * np.exp(-t * (6 + 9 * k / bright))
        k += 1
    return out * np.clip(t / 0.0015, 0, 1)


def thump(sec=0.25, f0=190, f1=85):
    """A short pitched punch under the main note: felt on headphones, harmless on a phone speaker."""
    t = t_axis(sec)
    freq = f1 + (f0 - f1) * np.exp(-t * 35)
    return np.sin(2 * np.pi * np.cumsum(freq) / SR) * np.exp(-t * 16)


def mouth_pop():
    """A cheek pop: a fast upward swoop with a tiny click."""
    t = t_axis(0.09)
    freq = 260 + 900 * (1 - np.exp(-t * 120))
    tone = np.sin(2 * np.pi * np.cumsum(freq) / SR) * np.exp(-t * 45) * np.clip(t / 0.001, 0, 1)
    tone[: len(tactile())] += tactile() * 0.4
    return tone


def stereo_pluck(f, sec=0.6, bright=1.0, odd_only=False, width=0.0012):
    """A centred pluck plus a slightly detuned one per side: wide, and still solid in mono."""
    mid = synth_pluck(f, sec, bright, odd_only)
    left = synth_pluck(f, sec, bright, odd_only, -width)
    right = synth_pluck(f, sec, bright, odd_only, width)
    return np.stack([mid + 0.5 * left, mid + 0.5 * right], 1) / 1.5


def bloop():
    """Two bubbles rising a fifth: bloop-BLOOP."""
    buf = np.zeros((int(1.0 * SR), 2))
    place2(buf, pan(bubble(hz("A5")), -0.3), 0.0, 0.8)
    place2(buf, pan(bubble(hz("E6"), 0.5), 0.3), 0.13, 1.0)
    place2(buf, pan(bubble(hz("E7"), 0.25, 0.3), 0.5), 0.15, 0.12)
    return master(fdn(buf, 0.6, 0.12), 0.9)


def bounce():
    """A ball bouncing upward: marimba hits that rise and speed up."""
    buf = np.zeros((int(1.2 * SR), 2))
    hits = [(0.0, "C6", -0.4), (0.17, "E6", -0.15), (0.29, "G6", 0.15), (0.37, "C7", 0.4)]
    for at, n, p in hits:
        place2(buf, pan(marimba(hz(n), 0.8), p), at, 0.9)
        place2(buf, pan(tactile(), p), at, 0.15)
    place2(buf, pan(bubble(hz("C7"), 0.4, 0.25), 0.4), 0.37, 0.25)
    return master(fdn(buf, 0.7, 0.14), 1.1)


def tada():
    """Ta-da-da-DAH: a bright pop-synth arpeggio landing on an accented octave."""
    buf = np.zeros((int(1.3 * SR), 2))
    for i, n in enumerate(["C5", "E5", "G5"]):
        place2(buf, stereo_pluck(hz(n), 0.35, 0.8), i * 0.06, 0.55)
    place2(buf, stereo_pluck(hz("C6"), 0.9, 1.4), 0.2, 0.9)
    place2(buf, stereo_pluck(hz("G6"), 0.7, 1.0), 0.2, 0.25)
    place2(buf, pan(thump(), 0.0), 0.2, 0.3)
    place2(buf, pan(fm_bell(hz("C7"), 0.8, 0.5), 0.3), 0.21, 0.12)
    return master(fdn(buf, 0.8, 0.15), 1.2, (0.15, 0.55))


def pop_ding():
    """Pop! then a bright ding: a cheek pop that opens into a bell."""
    buf = np.zeros((int(1.4 * SR), 2))
    place2(buf, pan(mouth_pop(), -0.2), 0.0, 0.9)
    place2(buf, pan(fm_bell(hz("E6"), 1.2, 0.9), 0.2), 0.1, 0.8)
    place2(buf, pan(epiano(hz("B5"), 1.1, 0.7), -0.2), 0.1, 0.5)
    place2(buf, pan(tactile(), 0.2), 0.1, 0.2)
    return master(fdn(buf, 0.9, 0.18), 1.2)


def popcorn():
    """Popcorn: tiny pentatonic pops skipping across the stereo field, then one big pop."""
    rng = np.random.default_rng(23)
    buf = np.zeros((int(1.2 * SR), 2))
    notes = ["E6", "G6", "A6", "C7", "D7", "G6"]
    at = 0.0
    for i, n in enumerate(notes):
        place2(buf, pan(bubble(hz(n), 0.18, 0.5), rng.uniform(-0.8, 0.8)), at, 0.45)
        at += rng.uniform(0.045, 0.075)
    place2(buf, pan(bubble(hz("C6"), 0.55, 0.5), 0.0), at + 0.04, 1.0)
    place2(buf, stereo_pluck(hz("C5"), 0.6, 0.8), at + 0.04, 0.35)
    return master(fdn(buf, 0.6, 0.12), 1.1, (0.2, 0.6))


def bip_bop():
    """Bip-BOP on the "Shru-tam" rhythm: a soft square pluck with a punch, rising a fifth."""
    buf = np.zeros((int(1.2 * SR), 2))
    place2(buf, stereo_pluck(hz("G5"), 0.18, 0.7, odd_only=True), 0.0, 0.7)
    place2(buf, stereo_pluck(hz("D6"), 0.75, 1.0, odd_only=True), 0.15, 1.0)
    place2(buf, pan(thump(0.2, 220, 110), 0.0), 0.15, 0.3)
    place2(buf, pan(bubble(hz("D7"), 0.3, 0.3), 0.35), 0.17, 0.15)
    return master(fdn(buf, 0.6, 0.12), 1.0)


POP = {
    "q1-bloop": bloop,
    "q2-bounce": bounce,
    "q3-tada": tada,
    "q4-pop-ding": pop_ding,
    "q5-popcorn": popcorn,
    "q6-bip-bop": bip_bop,
}

# ---------- iPhone-inspired set: pure, tight, close (original melodies) ----------


def mallet(f, sec=0.7, hardness=1.0, decay=6.0):
    """A pure sine mallet: the clean, close-miked tone of a phone alert."""
    t = t_axis(sec)
    tone = (np.sin(2 * np.pi * f * t) * np.exp(-t * decay)
            + 0.22 * hardness * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t * decay * 2.5)
            + 0.06 * hardness * np.sin(2 * np.pi * 4.07 * f * t) * np.exp(-t * decay * 6))
    hit = lowpass(np.random.default_rng(7).standard_normal(len(t)), 3500) * np.exp(-t * 500) * 0.12 * hardness
    return (tone + hit) * np.clip(t / 0.0025, 0, 1)


def felt_piano(f, sec=1.0, vel=1.0):
    """A soft felt piano: slightly stretched partials, fast-then-slow decay, a muted hammer."""
    t = t_axis(sec)
    out = np.zeros_like(t)
    for k in range(1, 9):
        fk = f * k * np.sqrt(1 + 0.0004 * k * k)
        if fk > 9000:
            break
        env = 0.7 * np.exp(-t * (9 + 3 * k)) + 0.3 * np.exp(-t * (2.2 + 0.6 * k))
        out += np.sin(2 * np.pi * fk * t) * env / k ** (1.9 - 0.5 * vel)
    hammer = lowpass(np.random.default_rng(8).standard_normal(len(t)), 1400) * np.exp(-t * 260) * 0.25 * vel
    return lowpass(out, 5000) + hammer


def soft_synth(f, sec=0.5, glide_from=None, glide_ms=60):
    """A rounded triangle-ish synth note, optionally gliding up into pitch."""
    t = t_axis(sec)
    freq = np.full_like(t, f)
    if glide_from:
        freq = f + (glide_from - f) * np.exp(-t / (glide_ms / 1000 / 3))
    phase = 2 * np.pi * np.cumsum(freq) / SR
    tone = sum(np.sin(k * phase) * (-1) ** ((k - 1) // 2) / k**2 for k in (1, 3, 5, 7))
    return tone * np.exp(-t * 5) * np.clip(t / 0.004, 0, 1)


def wood(f=1250, sec=0.12):
    """A small wooden knock."""
    t = t_axis(sec)
    tone = np.sin(2 * np.pi * f * t) * np.exp(-t * 60) + 0.4 * np.sin(2 * np.pi * f * 2.7 * t) * np.exp(-t * 110)
    return tone + lowpass(np.random.default_rng(9).standard_normal(len(t)), 2500) * np.exp(-t * 400) * 0.3


def tight(buf, length, room_sec=0.55, mix=0.1):
    return master(fdn(buf, room_sec, mix), length)


def trio():
    """Three pure mallet notes rising through a triad, the last one left to ring."""
    buf = np.zeros((int(1.0 * SR), 2))
    for i, (n, p) in enumerate((("A5", -0.12), ("D6", 0.0), ("F#6", 0.12))):
        place2(buf, pan(mallet(hz(n), 0.8 if i == 2 else 0.35, 1.0, 5 if i == 2 else 9), p), i * 0.085, 0.9)
    return tight(buf, 0.95)


def felt():
    """Felt piano, a fifth apart, with a soft octave underneath."""
    buf = np.zeros((int(1.2 * SR), 2))
    place2(buf, pan(felt_piano(hz("F#5"), 1.1, 0.6), 0.0), 0.0, 0.45)
    place2(buf, pan(felt_piano(hz("F#6"), 0.5, 0.9), -0.15), 0.0, 0.8)
    place2(buf, pan(felt_piano(hz("C#7"), 1.0, 0.9), 0.15), 0.11, 1.0)
    return tight(buf, 1.1, 0.6, 0.12)


def ricochet():
    """One glassy note that rebounds, faster and softer each time."""
    buf = np.zeros((int(1.0 * SR), 2))
    at, gap, gain = 0.0, 0.15, 1.0
    for i in range(6):
        place2(buf, pan(mallet(hz("E6") * (1 + 0.004 * i), 0.4, 1.2, 10), 0.1 * (-1) ** i), at, gain)
        at += gap
        gap *= 0.68
        gain *= 0.72
    return tight(buf, 0.85, 0.5, 0.08)


def glow():
    """A soft sine chord that swells in, topped by a single bright glint."""
    t = t_axis(1.2)
    chord = sum(np.sin(2 * np.pi * hz(n) * t + k) for k, n in enumerate(["E5", "B5", "D#6", "F#6"])) / 4
    chord *= np.clip(t / 0.12, 0, 1) ** 2 * np.exp(-np.clip(t - 0.12, 0, None) * 3.2)
    buf = np.zeros((int(1.3 * SR), 2))
    place2(buf, np.stack([chord, np.roll(chord, int(0.006 * SR))], 1), 0.0, 0.7)
    place2(buf, pan(mallet(hz("B6"), 0.9, 0.8, 4.5), 0.1), 0.12, 0.6)
    return master(fdn(buf, 0.8, 0.15), 1.2, (0.05, 0.45))


def flick():
    """A quick grace note flicked up into a ringing one."""
    buf = np.zeros((int(1.0 * SR), 2))
    place2(buf, pan(mallet(hz("B5"), 0.12, 0.8, 30), -0.1), 0.0, 0.6)
    place2(buf, pan(mallet(hz("E6"), 0.85, 1.0, 4.8), 0.05), 0.045, 1.0)
    place2(buf, pan(mallet(hz("E7"), 0.4, 0.5, 9), 0.15), 0.047, 0.12)
    return tight(buf, 0.9)


def knock():
    """A polite knock-knock, then a clear note: someone at the door of your day."""
    buf = np.zeros((int(1.1 * SR), 2))
    place2(buf, pan(wood(1250), -0.1), 0.0, 0.8)
    place2(buf, pan(wood(1180), 0.1), 0.11, 0.7)
    place2(buf, pan(mallet(hz("G6"), 0.8, 1.0, 5), 0.0), 0.26, 0.9)
    place2(buf, pan(mallet(hz("D6"), 0.8, 0.6, 5), 0.0), 0.26, 0.35)
    return tight(buf, 1.05)


def lift():
    """A rounded synth note that glides up a seventh and lands with a soft echo."""
    buf = np.zeros((int(1.0 * SR), 2))
    place2(buf, pan(soft_synth(hz("C6"), 0.18), -0.1), 0.0, 0.7)
    place2(buf, pan(soft_synth(hz("A#6"), 0.6, glide_from=hz("C6"), glide_ms=70), 0.1), 0.12, 1.0)
    place2(buf, pan(soft_synth(hz("A#6"), 0.4), -0.3), 0.32, 0.18)
    return tight(buf, 0.9)


def hello():
    """Up, down, and up again, like a friendly "hel-lo!"."""
    buf = np.zeros((int(1.0 * SR), 2))
    for i, (n, at, sec) in enumerate((("G6", 0.0, 0.25), ("E6", 0.1, 0.25), ("C7", 0.2, 0.75))):
        place2(buf, pan(mallet(hz(n), sec, 0.9, 8 if i < 2 else 5) + 0.35 * soft_synth(hz(n), sec), (i - 1) * 0.12), at, 0.85)
    return tight(buf, 0.95)


IPHONE = {
    "a1-trio": trio,
    "a2-felt": felt,
    "a3-ricochet": ricochet,
    "a4-glow": glow,
    "a5-flick": flick,
    "a6-knock": knock,
    "a7-lift": lift,
    "a8-hello": hello,
}

if __name__ == "__main__":
    out = Path(sys.argv[1] if len(sys.argv) > 1 else "reminder-tones")
    which = sys.argv[2] if len(sys.argv) > 2 else "classic"
    out.mkdir(parents=True, exist_ok=True)
    sets = {"classic": OPTIONS, "premium": PREMIUM, "pop": POP, "iphone": IPHONE}
    for name, fn in sets[which].items():
        x = fn()
        sf.write(out / f"{name}.wav", x.astype(np.float32), SR, subtype="PCM_16")
        print(f"{name}: {len(x) / SR:.2f}s peak {20 * np.log10(np.max(np.abs(x))):.1f} dBFS")
