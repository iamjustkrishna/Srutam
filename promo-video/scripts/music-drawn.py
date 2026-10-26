"""Music bed for the AI-illustrated "Who was doing slides 8-12?" (src/drawn/timeline.ts, 44 s).

    python scripts/music-drawn.py

A cosy pluck at 11 p.m., a hit and a ticking pad when the 1:43 a.m. message lands, a low
drop under the professor nightmare, a lonely pad at 2:30, bells when Meera remembers, a
lift as she asks Srutam, light plucks under the canteen flashback, a bouncy groove for the
group chat, a sheepish walk-down for Kabir at 4 a.m. and a resolve under the band.
"""
import numpy as np

from generate_audio_stories import PUB, SR, bell, groove, noise, pad, pizz, place, shape, t_axis, hz, write

TOTAL = (1320 + 90) / 30  # story + call-to-action card
s = lambda frame: frame / 30


def drawn_music(total=TOTAL):
    buf = np.zeros(int((total + 1) * SR))
    # 11 p.m.: cosy plucks
    for j, n in enumerate(["C5", "E5", "G5", "E5", "C5", "G4"]):
        place(buf, pizz(hz(n), 0.4), 0.2 + j * 0.5, 0.16)
    # 1:43 a.m.: low hit, then a tense pad with clock ticks until the nightmare
    place(buf, shape(pad(["D2", "A2"], 1.4, 0.0, 0.6), 400), s(96), 0.45)
    place(buf, shape(pad(["D2", "A2", "F3"], s(396 - 100), 0.6, 1.0), 700), s(100), 0.26)
    for k in range(int((s(396) - s(110)) / 0.92)):
        place(buf, shape(noise(0.1), 120) * np.exp(-t_axis(0.1) * 30), s(110) + k * 0.92, 0.25)
    # Nightmare: a low drop
    place(buf, shape(pad(["C#2", "G#2", "C#3"], s(75), 0.1, 0.8), 500), s(396), 0.42)
    # 2:30 a.m.: lonely pad
    place(buf, shape(pad(["A2", "E3", "C4"], s(120), 1.0, 1.2), 900), s(471), 0.2)
    # She remembers: bells
    for k, n in enumerate(["E5", "A5", "C6", "E6"]):
        place(buf, bell(hz(n), 1.8), s(596) + k * 0.1, 0.07)
    # She asks Srutam: a lift, bells on the answer
    place(buf, shape(pad(["F3", "C4", "A4", "E5"], s(150), 0.4, 1.0), 2200), s(666), 0.2)
    for k, n in enumerate(["C5", "F5", "A5", "C6"]):
        place(buf, bell(hz(n), 1.8), s(740) + k * 0.1, 0.06)
    # Canteen flashback: light plucks, quiet under the recording
    beat = 60 / 104
    at, i = s(816), 0
    prog = [("C3", ["E5", "G5", "C6"]), ("F2", ["F5", "A5", "C6"]), ("G2", ["D5", "G5", "B5"]), ("C3", ["E5", "G5", "C6"])]
    while at < s(958):
        root, tri = prog[(i // 4) % 4]
        if i % 2 == 0:
            place(buf, pizz(hz(root), 0.4), at, 0.16)
        place(buf, pizz(hz(tri[i % 3]), 0.35), at + beat / 2, 0.07)
        at += beat
        i += 1
    # Group chat piles on
    groove(buf, s(962), s(1048), ["F5", "A5", "C6", "A5", "G5", "C6", "E6", "C6"], ["F2", "C3"], 60 / 112, 0.7)
    # Kabir at 4 a.m.: a sheepish walk down
    for j, n in enumerate(["C4", "B3", "A#3", "A3", "G#3", "G3"]):
        place(buf, pizz(hz(n), 0.4), s(1054) + j * 0.42, 0.3)
    # Presentation and band: resolve
    res = pad(["F2", "C3", "A3", "F4", "G4"], total - s(1140), 0.6, 0.3)
    res *= np.linspace(1, 0, len(res)) ** 1.3
    place(buf, shape(res, 3000), s(1140), 0.3)
    for k, n in enumerate(["F5", "A5", "C6", "F6"]):
        place(buf, bell(hz(n), 2.2), s(1252) + k * 0.07, 0.07)
    # Call to action: a warm lift and bells under the end card
    place(buf, shape(pad(["F3", "C4", "A4", "C5"], total - s(1320), 0.3, 0.8), 1500), s(1320), 0.22)
    for k, n in enumerate(["C5", "F5", "A5"]):
        place(buf, bell(hz(n), 1.6), s(1348) + k * 0.12, 0.06)
    return buf


if __name__ == "__main__":
    (PUB / "drawn").mkdir(exist_ok=True)
    write(PUB / "drawn" / "music.wav", drawn_music(), 0.5)
