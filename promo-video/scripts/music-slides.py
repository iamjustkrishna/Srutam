"""Music bed for "Who was doing slides 8-12?" (src/slides/timeline.ts, 38 s).

    python scripts/music-slides.py

1:43 a.m. group-chat tension (clock ticks over a low pad), a light comic pluck under the
canteen flashback, a lift as Meera asks, bells on the reveal, a bouncy groove for the
chat pile-on, a sheepish walk-down for Kabir at 4 a.m., and a resolve under the band.
"""
import numpy as np

from generate_audio_stories import PUB, SR, bell, groove, noise, pad, pizz, place, shape, t_axis, hz, write

TOTAL = 1140 / 30


def slides_music(total=TOTAL):
    buf = np.zeros(int((total + 1) * SR))
    # 0-6.5 s: the chat at 1:43 a.m.
    place(buf, shape(pad(["D2", "A2", "F3"], 6.8, 0.2, 1.0), 700), 0.0, 0.3)
    for k in range(7):
        place(buf, shape(noise(0.1), 120) * np.exp(-t_axis(0.1) * 30), 0.4 + k * 0.92, 0.3)
    # 6.5-18 s: flashback, light comic plucks in C
    beat = 60 / 104
    at, i = 6.7, 0
    prog = [("C3", ["E5", "G5", "C6"]), ("F2", ["F5", "A5", "C6"]), ("G2", ["D5", "G5", "B5"]), ("C3", ["E5", "G5", "C6"])]
    while at < 17.9:
        root, tri = prog[(i // 4) % 4]
        if i % 2 == 0:
            place(buf, pizz(hz(root), 0.4), at, 0.26)
        place(buf, pizz(hz(tri[i % 3]), 0.35), at + beat / 2, 0.12)
        at += beat
        i += 1
    # 18.3-22.5 s: she asks, a lift
    lift = pad(["F3", "C4", "A4", "E5"], 5.0, 0.4, 1.0)
    place(buf, shape(lift, 2200), 18.3, 0.24)
    # 22.6 s: the answer lands
    for k, n in enumerate(["C5", "F5", "A5", "C6"]):
        place(buf, bell(hz(n), 1.8), 21.4 + k * 0.1, 0.06)
    # 27.7-31 s: the group chat piles on
    groove(buf, 27.7, 31.0, ["F5", "A5", "C6", "A5", "G5", "C6", "E6", "C6"], ["F2", "C3"], 60 / 112, 0.7)
    # 31.2-35 s: Kabir at 4:10 a.m., a sheepish walk down
    for j, n in enumerate(["C4", "B3", "A#3", "A3", "G#3", "G3"]):
        place(buf, pizz(hz(n), 0.4), 31.4 + j * 0.42, 0.3)
    # 35.3 s: resolve under the band
    res = pad(["F2", "C3", "A3", "F4", "G4"], total - 35.3, 0.6, 0.3)
    res *= np.linspace(1, 0, len(res)) ** 1.3
    place(buf, shape(res, 3000), 35.3, 0.35)
    for k, n in enumerate(["F5", "A5", "C6", "F6"]):
        place(buf, bell(hz(n), 2.2), 35.4 + k * 0.07, 0.07)
    return buf


if __name__ == "__main__":
    (PUB / "slides").mkdir(exist_ok=True)
    write(PUB / "slides" / "music.wav", slides_music(), 0.5)
