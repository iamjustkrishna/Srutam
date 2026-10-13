"""Music beds retimed for the 16:9 cuts (their scenes run longer for the English lines).

    python scripts/music-wide.py
"""
from generate_audio_stories import PUB, exam_music_wide, knock_music, nani_music, who_music_wide, write

write(PUB / "meeting" / "music-wide.wav", who_music_wide(), 0.5)
write(PUB / "exam" / "music-wide.wav", exam_music_wide(), 0.5)
write(PUB / "nani" / "music-wide.wav", nani_music(total=1269 / 30, d=24 / 30), 0.6)
write(PUB / "knock" / "music-wide.wav", knock_music(total=960 / 30, end_at=28.65), 0.6)
