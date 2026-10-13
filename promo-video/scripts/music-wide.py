"""Music beds retimed for the 16:9 cuts (their scenes run longer for the English lines).

    python scripts/music-wide.py
"""
from generate_audio_stories import PUB, exam_music_wide, who_music_wide, write

write(PUB / "meeting" / "music-wide.wav", who_music_wide(), 0.5)
write(PUB / "exam" / "music-wide.wav", exam_music_wide(), 0.5)
