"""Mouth openness (voice loudness per frame) for the English 16:9 cuts of Knock Knock
and Nani's Laddoos: Meera's lines plus the English takes under voiceover/en.

    python scripts/lipsync-loudness-en.py
"""
from generate_audio_stories import PUB, write_lipsync

vo = PUB / "voiceover"
en = vo / "en"
write_lipsync("wide/knock", {
    **{p.stem: p for p in sorted((vo / "knock" / "meera").glob("k*.mp3"))},
    "k2-promise": en / "knock" / "meera" / "k2-promise.mp3",
    "ma-remembered": en / "knock" / "ma" / "ma-remembered.wav",
})
write_lipsync("wide/nani", {
    **{p.stem: p for p in sorted((vo / "nani" / "meera").glob("n*.mp3"))},
    **{p.stem: p for p in sorted((en / "nani" / "nani").glob("*.wav"))},
})
