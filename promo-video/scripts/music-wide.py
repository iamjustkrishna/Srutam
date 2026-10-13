"""Music beds retimed for the 16:9 cuts (their scenes run longer for the English lines).

    python scripts/music-wide.py
"""
from generate_audio_stories import PUB, who_music_wide, write

write(PUB / "meeting" / "music-wide.wav", who_music_wide(), 0.5)
