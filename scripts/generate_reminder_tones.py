"""Synthesize candidate reminder tones for Srutam (no samples, no downloads).

Each tone is short (about 1-2 s), melodic, starts immediately, rings out
into a soft synthetic room and fades to true silence (no click), and is
normalized to the same loudness so the options compare fairly.

The "premium" set is stereo and FM-synthesized (bells, electric piano, a
voice-like hum), with a richer feedback-delay-network reverb.

Usage: python scripts/generate_reminder_tones.py <out_dir> [classic|premium]
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

if __name__ == "__main__":
    out = Path(sys.argv[1] if len(sys.argv) > 1 else "reminder-tones")
    which = sys.argv[2] if len(sys.argv) > 2 else "classic"
    out.mkdir(parents=True, exist_ok=True)
    for name, fn in (PREMIUM if which == "premium" else OPTIONS).items():
        x = fn()
        sf.write(out / f"{name}.wav", x.astype(np.float32), SR, subtype="PCM_16")
        print(f"{name}: {len(x) / SR:.2f}s peak {20 * np.log10(np.max(np.abs(x))):.1f} dBFS")
