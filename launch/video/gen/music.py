# Warm ambient-electronic underscore (fallback: ElevenLabs Music needs a paid plan).
# Sections follow the scene starts; music sits under narration, SFX come from ElevenLabs.
import numpy as np, wave, os, sys, json
SR = 44100; DUR = float(sys.argv[1]); N = int(SR * DUR)
S = json.loads(sys.argv[2])  # scene starts
rng = np.random.default_rng(5)
t = np.arange(N) / SR
L = np.zeros(N); R = np.zeros(N)

def tt(d): return np.arange(int(d * SR)) / SR
def hz(m): return 440 * 2 ** ((m - 69) / 12)
def lp(x, hi, lo=0):
    X = np.fft.rfft(x); f = np.fft.rfftfreq(len(x), 1 / SR); X[(f > hi) | (f < lo)] = 0
    return np.fft.irfft(X, len(x))
def add(sig, at, g=1.0, pan=0.0):
    i = int(at * SR)
    if i >= N: return
    s = sig[:N - i] * g
    L[i:i + len(s)] += s * np.sqrt(.5 * (1 - pan)); R[i:i + len(s)] += s * np.sqrt(.5 * (1 + pan))

BPM = 100; B = 60 / BPM
CH = [(57, 60, 64, 67), (53, 57, 60, 64), (48, 52, 55, 59), (55, 59, 62, 66)]  # Am7 Fmaj7 Cmaj7 G(add)
ROOT = [45, 41, 36, 43]
bar = 4 * B
def chord(x): return int(x // bar) % 4

# lush pad: detuned soft saws, heavily lowpassed, slow crossfades
pad = np.zeros((2, N))
for b in range(int(DUR / bar) + 2):
    a = b * bar; i0 = int(max(0, a - .4) * SR); i1 = min(N, int((a + bar + .4) * SR))
    if i0 >= N: break
    seg = t[i0:i1]; env = np.clip((seg - (a - .4)) / .8, 0, 1) * np.clip(((a + bar + .4) - seg) / .8, 0, 1)
    for m in CH[b % 4]:
        for ch, det in ((0, -.004), (1, .004)):
            pad[ch, i0:i1] += sum(np.sin(2 * np.pi * hz(m) * (1 + det) * k * seg) / k ** 1.4 for k in range(1, 5)) * env * .08
pad[0] = lp(pad[0], 1400, 50); pad[1] = lp(pad[1], 1400, 50)

def soft_kick():
    x = tt(.4); f = 48 + 70 * np.exp(-x * 30)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-x * 8)
def shaker():
    x = tt(.08); return lp(rng.standard_normal(len(x)), 14000, 6000) * np.exp(-x * 55)
def pluck(m, d=.6):
    x = tt(d); f = hz(m)
    return (np.sin(2 * np.pi * f * x) + .25 * np.sin(4 * np.pi * f * x)) * np.exp(-x * 7) * np.minimum(1, x * 400)
def bass(m, d):
    x = tt(d); f = hz(m)
    return (np.sin(2 * np.pi * f * x) + .3 * np.sin(4 * np.pi * f * x)) * np.minimum(1, x * 60) * np.exp(-x * 1.5)

dock, newin, cloud, trust, end = S['dock'], S['newin'], S['cloud'], S['trust'], S['end']
def beats(a, b, step):
    x = a
    while x < b - 1e-6: yield x; x += step

# arpeggio across most of the piece, brighter after "New in 2.5"
ARP = [0, 2, 1, 3, 2, 1, 3, 2]
for k, x in enumerate(beats(S['title'], DUR - 2.5, B / 2)):
    if newin - .2 <= x < cloud: continue
    c = CH[chord(x)]; m = c[ARP[k % 8]] + 12 + (12 if x > cloud and k % 16 >= 8 else 0)
    g = .05 if x < dock else .07 if x < trust else .055
    add(pluck(m), x, g, pan=.4 if k % 2 else -.4)
# pulse: soft kick + shaker + sustained bass in the feature sections
for lo, hi in ((dock, newin - .2), (cloud, trust)):
    for k, x in enumerate(beats(lo, hi, B)):
        add(soft_kick(), x, .32)
        add(shaker(), x + B / 2, .06, pan=.3)
    for x in beats(lo, hi, bar / 2):
        add(bass(ROOT[chord(x)], bar / 2), x, .16)
# swell into "New in 2.5" and the end card
for at in (newin, end):
    x = tt(1.2); add(lp(rng.standard_normal(len(x)), 6000, 300) * (x / 1.2) ** 2.5, at - 1.2, .05)

vol = np.interp(t, [0, 3, S['title'], newin - .3, newin, cloud, trust, end, DUR - 2.5, DUR], [.45, .7, .85, .9, 1.0, .95, .75, .9, .7, 0])
outL = (L + pad[0]) * vol; outR = (R + pad[1]) * vol
# simple stereo room
ir = rng.standard_normal(int(1.6 * SR)) * np.exp(-np.arange(int(1.6 * SR)) / SR * 3.2); ir = lp(ir, 6000, 200); ir /= np.abs(ir).sum() ** .5
def conv(x):
    n = 1 << int(np.ceil(np.log2(len(x) + len(ir))))
    return np.fft.irfft(np.fft.rfft(x, n) * np.fft.rfft(ir, n), n)[:len(x)]
outL += conv(outL) * .25; outR += conv(outR) * .25
pk = max(np.abs(outL).max(), np.abs(outR).max())
data = (np.stack([outL, outR], 1) / pk * .8 * 32767).astype(np.int16)
out = os.path.join(os.path.dirname(__file__), '..', 'public', 'music.wav')
with wave.open(out, 'wb') as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(data.tobytes())
print('music ok', DUR)
