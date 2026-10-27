"""Score for "The Brain Court" (src/court/timeline.ts, 58 s), played by "people".

    python scripts/court-music.py

One theme carries the story: a lonely piano for Aarav at his desk, a playful
pizzicato-and-clarinet courtroom version under the trial, a frantic minor version in
the spiral, silence on "Objection.", a warm string swell and piano when Srutam
arrives, then the full theme (guitar, piano, clarinet, brushes) when the case
closes, resolving under the call to action. Instruments are physically modelled
(plucked strings via Karplus-Strong, a felt piano, a breathy reed), timing and
dynamics are humanised, and everything sits in a synthetic room reverb.
"""
import numpy as np

from generate_audio_stories import PUB, SR, hz, place, shape, t_axis, write

rng = np.random.default_rng(7)
TOTAL = 1740 / 30
s = lambda frame: frame / 30
BPM = 104
B = 60 / BPM


def human(at, amt=0.012):
    """A player is never exactly on the grid."""
    return at + rng.normal(0, amt)


def vel(base, amt=0.12):
    return base * (1 + rng.normal(0, amt))


def pluck(f, sec=1.6, bright=0.5, damp=0.996):
    """Karplus-Strong plucked string: guitar when bright, pizzicato cello when dark."""
    n = int(SR / f)
    buf = rng.uniform(-1, 1, n)
    buf = shape(np.concatenate([buf, np.zeros(1)]), 2000 + 6000 * bright)[:n]
    periods = []
    for _ in range(int(sec * SR / n) + 1):  # one string period at a time
        periods.append(buf)
        buf = damp * 0.5 * (buf + np.roll(buf, -1))
    out = np.concatenate(periods)[: int(sec * SR)]
    t = t_axis(sec)
    return out * np.clip(t / 0.002, 0, 1)


def felt_piano(f, sec=2.2, v=1.0):
    t = t_axis(sec)
    partials = sum(np.sin(2 * np.pi * f * k * (1 + 0.0004 * k * k) * t) * np.exp(-t * (1.6 + 1.1 * k)) / k ** 1.3 for k in range(1, 8))
    hammer = shape(rng.normal(0, 1, len(t)), 1800) * np.exp(-t * 90) * 0.15
    return (partials + hammer) * np.clip(t / 0.004, 0, 1) * v


def reed(f, sec, v=1.0):
    """A soft clarinet-ish lead: odd harmonics, breath, delayed vibrato."""
    t = t_axis(sec)
    vib = 1 + 0.006 * np.sin(2 * np.pi * 5.2 * t) * np.clip((t - 0.25) / 0.3, 0, 1)
    ph = 2 * np.pi * f * np.cumsum(vib) / SR
    tone = np.sin(ph) + 0.35 * np.sin(3 * ph) + 0.12 * np.sin(5 * ph)
    breath = shape(rng.normal(0, 1, len(t)), 3000) * 0.04
    envl = np.clip(t / 0.06, 0, 1) * np.clip((sec - t) / 0.12, 0, 1)
    return (tone + breath) * envl * v


def strings(notes, sec, swell=1.5, v=1.0):
    t = t_axis(sec)
    out = np.zeros_like(t)
    for n in notes:
        for d in (-0.004, 0.0, 0.005):  # a section, slightly out of tune with itself
            f = hz(n) * (1 + d)
            ph = 2 * np.pi * f * t + rng.uniform(0, 6)
            out += sum(np.sin(k * ph) / k for k in range(1, 7)) * (1 + 0.1 * np.sin(2 * np.pi * rng.uniform(4, 6) * t))
    envl = np.clip(t / swell, 0, 1) ** 1.5 * np.clip((sec - t) / 0.8, 0, 1)
    return shape(out / (3 * len(notes)), 2600) * envl * v


def brush(sec=0.25, v=1.0):
    t = t_axis(sec)
    return shape(rng.normal(0, 1, len(t)), 2500, "hp") * np.exp(-t * 18) * v


def kick(v=1.0):
    t = t_axis(0.4)
    return np.sin(2 * np.pi * np.cumsum(55 + 90 * np.exp(-t * 30)) / SR) * np.exp(-t * 9) * v


def reverb(x, sec=2.2, mix=0.28):
    """Convolve with a decaying noise tail: a warm room."""
    t = t_axis(sec)
    ir = rng.normal(0, 1, len(t)) * np.exp(-t * 3.2)
    ir = shape(ir, 4500)
    ir[0] = 0
    n = len(x) + len(ir)
    wet = np.fft.irfft(np.fft.rfft(x, n) * np.fft.rfft(ir, n), n)[: len(x)]
    wet *= np.max(np.abs(x)) / (np.max(np.abs(wet)) + 1e-9)
    return x * (1 - mix) + wet * mix


# The theme, as (beat offset, note, beats). Two bars of question, two bars of answer.
THEME = [(0, "E5", 1), (1, "G5", 1), (2, "A5", 1.5), (3.5, "G5", 0.5), (4, "E5", 1), (5, "D5", 1), (6, "C5", 2),
         (8, "E5", 1), (9, "G5", 1), (10, "C6", 1.5), (11.5, "B5", 0.5), (12, "A5", 1), (13, "G5", 1), (14, "E5", 2)]
CHORDS = [("C3", ["C4", "E4", "G4"]), ("A2", ["A3", "C4", "E4"]), ("F2", ["F3", "A3", "C4"]), ("G2", ["G3", "B3", "D4"])]


def score(total=TOTAL):
    buf = np.zeros(int((total + 3) * SR))

    # 2.8-6.5 s: Aarav alone at 9 p.m., a wondering piano (A minor - F, unresolved)
    for i, (root, tri) in enumerate([("A2", ["A3", "C4", "E4"]), ("F2", ["F3", "A3", "C4"])]):
        at = s(86) + i * 1.8
        place(buf, felt_piano(hz(root), 2.6), human(at), vel(0.35))
        for j, n in enumerate(tri):
            place(buf, felt_piano(hz(n), 2.2), human(at + 0.12 * j, 0.02), vel(0.22))

    # 6.5-32 s: the trial, a playful courtroom version (pizzicato bass, guitar off-beats,
    # brushes, the theme on a reed, kept light under the dialogue)
    start, end = s(156), s(958)
    bar = 4 * B
    k = 0
    at = start
    while at < end - 0.1:
        root, tri = CHORDS[k % 4]
        for beat in range(4):
            bt = at + beat * B
            if bt >= end:
                break
            note = root if beat % 2 == 0 else tri[0][:-1] + str(int(tri[0][-1]) - 1)
            place(buf, pluck(hz(note), 0.9, 0.15, 0.992), human(bt), vel(0.5))
            place(buf, brush(0.2), human(bt + B / 2, 0.01), vel(0.10))
            for n in tri:  # guitar chops on the off-beat
                place(buf, pluck(hz(n) * 2, 0.5, 0.7, 0.990), human(bt + B / 2 + 0.008 * tri.index(n), 0.006), vel(0.07))
        k += 1
        at += bar
    for rep, at0 in enumerate([start + 2 * bar, start + 10 * bar]):
        for off, n, ln in THEME:
            place(buf, reed(hz(n), ln * B * 0.92, 0.12), human(at0 + off * B, 0.015), vel(1.0, 0.08))

    # 32-36.3 s: the spiral, the theme in minor, faster, over trembling strings and a heartbeat
    sp0 = s(960)
    place(buf, strings(["C3", "G3", "D#4", "G4"], s(130), 1.2, 0.5), sp0, 1.0)
    fast = B * 0.6
    for rep in range(2):
        for off, n, ln in THEME[:7]:
            m = n.replace("E", "D#").replace("A", "G#").replace("B", "A#")
            place(buf, reed(hz(m), ln * fast * 0.9, 0.11), human(sp0 + 0.3 + (off + rep * 8) * fast, 0.01), vel(1.0))
    for i in range(int(s(130) / 0.55)):
        place(buf, kick(0.5), sp0 + i * 0.55 * (1 - i * 0.012), 1.0)
        place(buf, kick(0.3), sp0 + i * 0.55 * (1 - i * 0.012) + 0.18, 1.0)

    # 36.3-38 s: "Objection." -> silence (the record scratch is a sound effect)

    # 38-45 s: Srutam and the proof, a warm string swell and an open piano arpeggio
    sw0 = s(1140)
    place(buf, strings(["C3", "G3", "E4", "G4"], s(220), 2.5, 0.65), sw0, 1.0)
    arp = ["C4", "G4", "C5", "E5", "G5", "E5", "C5", "G4"]
    for i in range(int(s(220) / (B / 2))):
        place(buf, felt_piano(hz(arp[i % 8]), 1.6), human(sw0 + 1.0 + i * B / 2, 0.012), vel(0.12 + 0.05 * (i % 4 == 0)))

    # 45.3-54 s: case closed, the full theme (guitar strums, piano melody, reed, brushes)
    f0 = s(1364)
    at, k = f0, 0
    while at < s(1618):
        root, tri = CHORDS[k % 4]
        place(buf, pluck(hz(root), 1.4, 0.2, 0.994), human(at), vel(0.5))
        for beat in range(4):
            bt = at + beat * B
            for j, n in enumerate(tri + [tri[0][:-1] + str(int(tri[0][-1]) + 1)]):
                place(buf, pluck(hz(n), 0.8, 0.6, 0.993), human(bt + 0.012 * j * (1 if beat % 2 == 0 else -1) + (0 if beat % 2 == 0 else 0.03), 0.004), vel(0.09))
            place(buf, brush(0.2), human(bt + B / 2), vel(0.12))
        k += 1
        at += 4 * B
    for off, n, ln in THEME:
        place(buf, felt_piano(hz(n), 1.8), human(f0 + 0.2 + off * B, 0.012), vel(0.4))
        place(buf, reed(hz(n), ln * B * 0.92, 0.08), human(f0 + 0.2 + off * B, 0.012), vel(1.0))

    # 54-58 s: call to action, the theme resolves on a held C chord
    c0 = s(1620)
    place(buf, strings(["C3", "G3", "C4", "E4", "G4"], total - s(1620) + 1, 0.8, 0.5), c0, 1.0)
    for off, n, ln in [(0, "G5", 1), (1, "E5", 1), (2, "D5", 1), (3, "C5", 3)]:
        place(buf, felt_piano(hz(n), 2.6), human(c0 + 0.4 + off * B), vel(0.4))
    for j, n in enumerate(["C3", "G3", "C4", "E4", "G4"]):
        place(buf, pluck(hz(n), 3.0, 0.45, 0.998), human(c0 + 0.4 + 3 * B + 0.04 * j, 0.004), vel(0.25))

    buf = reverb(buf)
    fade = np.ones_like(buf)
    e = int(total * SR)
    fade[e - int(0.8 * SR): e] = np.linspace(1, 0, int(0.8 * SR))
    fade[e:] = 0
    # "Objection." lands in silence: only the record scratch and the shimmer are heard
    o0, o1 = int(s(1091) * SR), int(s(1138) * SR)
    fade[o0:o1] = 0.05
    fade[o0 - int(0.05 * SR): o0] = np.linspace(1, 0.05, int(0.05 * SR))
    fade[o1: o1 + int(0.3 * SR)] = np.linspace(0.05, 1, int(0.3 * SR))
    return buf * fade


if __name__ == "__main__":
    write(PUB / "court" / "music.wav", score(), 0.5)
