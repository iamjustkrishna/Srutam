"""Shared voice finishing for the story videos: a little room air for dialogue, a
phone-speaker treatment for recordings played back from a phone, a muffled memory for
flashbacks, plus trim and level. Used by generate-voices-indic.py and finish-voices.py."""

import numpy as np
from scipy.signal import butter, sosfilt


def band(x, sr, lo=None, hi=None, order=4):
    if lo and hi:
        sos = butter(order, [lo, hi], btype="band", fs=sr, output="sos")
    elif lo:
        sos = butter(order, lo, btype="high", fs=sr, output="sos")
    else:
        sos = butter(order, hi, btype="low", fs=sr, output="sos")
    return sosfilt(sos, x)


def room(x, sr, size=0.22, mix=0.09, seed=3):
    """A little air: a short synthetic room tail mixed under the dry voice."""
    rng = np.random.default_rng(seed)
    n = int(size * sr)
    ir = rng.standard_normal(n) * np.exp(-np.linspace(0, 7, n))
    ir = band(ir, sr, 300, 6000, 2)
    wet = np.zeros(len(x) + n)
    conv = np.convolve(x, ir)
    wet[: len(conv)] = conv[: len(x) + n]
    wet /= np.max(np.abs(wet)) + 1e-9
    out = np.concatenate([x, np.zeros(n)])
    return out + wet * mix * np.max(np.abs(x))


def phone(x, sr):
    """Played back from a phone speaker: band-limited, a little compressed and driven."""
    y = band(x, sr, 380, 3600, 4)
    y = np.tanh(y / (np.max(np.abs(y)) + 1e-9) * 1.8)
    return room(y, sr, 0.18, 0.12, seed=5)


def muffled(x, sr):
    """A half-heard memory: dark, roomy and distant."""
    y = band(x, sr, hi=1300, order=2)
    return room(y, sr, 0.6, 0.35, seed=7)


def trim(x, sr, pad=0.06, thresh=0.01):
    idx = np.where(np.abs(x) > thresh * np.max(np.abs(x)))[0]
    if len(idx) == 0:
        return x
    a = max(0, idx[0] - int(pad * sr))
    b = min(len(x), idx[-1] + int(pad * 2 * sr))
    return x[a:b]


def finish(x, sr, gain_db):
    x = x / (np.max(np.abs(x)) + 1e-9) * 0.7 * 10 ** (gain_db / 20)
    fade = int(0.01 * sr)
    x[:fade] *= np.linspace(0, 1, fade)
    x[-fade:] *= np.linspace(1, 0, fade)
    return np.clip(x, -0.98, 0.98).astype(np.float32)


TREAT = {"room": lambda x, sr: room(x, sr), "phone": phone, "muffled": muffled}
