"""Mouth shapes per video frame from Rhubarb Lip Sync (https://github.com/DanielSWolf/rhubarb-lip-sync).

Rhubarb listens to each voice line and picks one of nine mouth shapes (A-H, X)
over time. This writes them as one character per frame (30 fps) to
src/<video>/visemes.ts, which the rigged cast reads through `visemeAt()`.
English lines are recognized with the dialog text as a hint; other languages use
Rhubarb's language-independent phonetic recognizer.

Usage: python scripts/lipsync-rhubarb.py <path/to/rhubarb> [video ...]
"""
import json
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
VO = ROOT / "public" / "voiceover"
FPS = 30

# video -> [(key, audio, dialog text or None for phonetic)]
LINES = {
    "meeting": [
        ("rao-who", VO / "meeting/rao/rao-who.wav", None),
        ("rao-launch", VO / "meeting/rao/rao-launch.wav", None),
        ("arjun-21", VO / "meeting/arjun/arjun-21.wav", None),
        ("rao-final", VO / "meeting/rao/rao-final.wav", None),
        ("rao-deck", VO / "meeting/rao/rao-deck.wav", None),
        ("rao-note", VO / "meeting/rao/rao-note.wav", None),
        ("arjun-her", VO / "meeting/arjun/arjun-her.wav", None),
        ("neha-him", VO / "meeting/neha/neha-him.wav", None),
        ("rao-recording", VO / "meeting/rao/rao-recording.wav", None),
        ("rao-sheepish", VO / "meeting/rao/rao-sheepish.wav", None),
        ("m-ask", VO / "meeting/meera/m-ask.mp3", "Relax. Let's ask Srutam."),
        ("w-nobody", VO / "meeting/meera/w-nobody.mp3", "Nobody wrote it down. Except Srutam."),
        ("w-decision", VO / "meeting/meera/w-decision.mp3", "Decision. Next step. Reminder. Before lunch."),
        ("w-minuted", VO / "meeting/meera/w-minuted.mp3", "Every meeting, minuted."),
    ],
    "exam": [
        ("e-forgot", VO / "exam/meera/e-forgot.mp3", "Exam at nine. And I forgot what Prof said."),
        ("e-asleep", VO / "exam/meera/e-asleep.mp3", "Half the class was asleep. Srutam wasn't."),
        ("e-ask", VO / "exam/meera/e-ask.mp3", "What did Prof say will be on the exam?"),
        ("e-tagline", VO / "exam/meera/e-tagline.mp3", "Listen once. Ask anytime."),
        ("prof-lecture", VO / "exam/prof/prof-lecture.wav", None),
        ("prof-recording", VO / "exam/prof/prof-recording.wav", None),
        ("kabir-chai", VO / "exam/kabir/kabir-chai.wav", None),
        ("riya-study", VO / "exam/riya/riya-study.wav", None),
    ],
    # The 16:9 cuts speak English throughout (voiceover/en, scripts/vo/en/*.json).
    "wide/who": [
        ("rao-who", VO / "en/meeting/rao/rao-who.wav", "The fourteenth? Who said yes to the fourteenth?"),
        ("rao-launch", VO / "en/meeting/rao/rao-launch.wav", "So... do we launch on the fourteenth?"),
        ("arjun-21", VO / "en/meeting/arjun/arjun-21.wav", "Sir, the twenty-first would be safer..."),
        ("rao-final", VO / "en/meeting/rao/rao-final.wav", "No, no. The fourteenth. Final."),
        ("rao-deck", VO / "en/meeting/rao/rao-deck.wav", "Arjun. Pricing deck, by Thursday."),
        ("rao-note", VO / "en/meeting/rao/rao-note.wav", "Someone note it down!"),
        ("arjun-her", VO / "en/meeting/arjun/arjun-her.wav", "Neha did!"),
        ("neha-him", VO / "en/meeting/neha/neha-him.wav", "Arjun did!"),
        ("rao-recording", VO / "en/meeting/rao/rao-recording.wav", "No, no. The fourteenth. Final."),
        ("rao-sheepish", VO / "en/meeting/rao/rao-sheepish.wav", "Okay... so the fourteenth is final, then."),
        ("m-ask", VO / "meeting/meera/m-ask.mp3", "Relax. Let's ask Srutam."),
        ("w-nobody", VO / "meeting/meera/w-nobody.mp3", "Nobody wrote it down. Except Srutam."),
        ("w-decision", VO / "meeting/meera/w-decision.mp3", "Decision. Next step. Reminder. Before lunch."),
        ("w-minuted", VO / "meeting/meera/w-minuted.mp3", "Every meeting, minuted."),
    ],
    "wide/exam": [
        ("e-forgot", VO / "exam/meera/e-forgot.mp3", "Exam at nine. And I forgot what Prof said."),
        ("e-asleep", VO / "exam/meera/e-asleep.mp3", "Half the class was asleep. Srutam wasn't."),
        ("e-ask", VO / "exam/meera/e-ask.mp3", "What did Prof say will be on the exam?"),
        ("e-tagline", VO / "exam/meera/e-tagline.mp3", "Listen once. Ask anytime."),
        ("prof-lecture", VO / "en/exam/prof/prof-lecture.wav", "So, in Thursday's exam..."),
        ("prof-recording", VO / "en/exam/prof/prof-recording.wav", "In Thursday's exam: chapters four and seven, and the metro case study. Twenty marks."),
        ("kabir-chai", VO / "en/exam/kabir/kabir-chai.wav", "Chai's here!"),
        ("riya-study", VO / "en/exam/riya/riya-study.wav", "Come on, let's start!"),
    ],
    "slides": [
        ("s-record", VO / "en/slides/meera/s-record.mp3", "Recording this. Nobody remembers anything anyway."),
        ("s-assign", VO / "en/slides/riya/s-assign.mp3", "Okay. Meera, intro. I'll do one to seven. And Kabir, you'll do eight to twelve."),
        ("s-easy", VO / "en/slides/kabir/s-easy.mp3", "Done. Easy."),
        ("s-canteen", VO / "en/slides/kabir/s-canteen.mp3", "Who records a canteen?!"),
    ],
}


def cues(rhubarb, audio, text):
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / "in.wav"
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(audio), "-ac", "1", "-ar", "16000", str(wav)], check=True)
        cmd = [rhubarb, "-q", "-f", "json", "--extendedShapes", "GHX"]
        if text:
            dialog = Path(tmp) / "dialog.txt"
            dialog.write_text(text)
            cmd += ["-d", str(dialog)]
        else:
            cmd += ["-r", "phonetic"]
        out = subprocess.run(cmd + [str(wav)], check=True, capture_output=True, text=True).stdout
        return json.loads(out)["mouthCues"]


def per_frame(mouth_cues):
    end = mouth_cues[-1]["end"]
    frames = []
    for i in range(int(end * FPS) + 1):
        t = (i + 0.5) / FPS
        shape = next((c["value"] for c in mouth_cues if c["start"] <= t < c["end"]), "X")
        frames.append(shape)
    return "".join(frames)


def main(rhubarb, only=None):
    for video, lines in LINES.items():
        if only and video not in only:
            continue
        rows = ["// Generated by scripts/lipsync-rhubarb.py: Rhubarb mouth shape per frame (30 fps).", "export const VISEMES: Record<string, string> = {"]
        for key, audio, text in lines:
            track = per_frame(cues(rhubarb, audio, text))
            rows.append(f'  "{key}": "{track}",')
            print(f"{video}/{key}: {track}")
        rows.append("};")
        out = ROOT / "src" / video / "visemes.ts"
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text("\n".join(rows) + "\n")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2:])
