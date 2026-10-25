#!/bin/bash
# Check a finished video: specs, loudness, long silences, and a 12-frame contact sheet.
#
#   scripts/render/verify.sh <video.mp4> <sheet.png>
#
# Needs ffmpeg and Python with Pillow (set PYTHON to pick the interpreter).
here=$(cd "$(dirname "$0")" && pwd)
f=$1; sheet=$2
echo "== $(basename "$f")"
ffprobe -v error -show_entries stream=codec_name,width,height,r_frame_rate,nb_frames:format=duration -of compact=p=0 "$f"
ffmpeg -hide_banner -i "$f" -vn -af ebur128=peak=true -f null - 2>&1 | grep -A14 Summary | grep -E "I:|Peak:" | tr -s ' '
echo "silences over 1.2 s: $(ffmpeg -hide_banner -i "$f" -vn -af silencedetect=n=-45dB:d=1.2 -f null - 2>&1 | grep -c silence_start)"
d=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$f")
tmp=$(mktemp -d)
for i in $(seq 0 11); do
  t=$(python3 -c "print(round(($i+0.5)*$d/12,2))")
  ffmpeg -y -loglevel error -ss "$t" -i "$f" -frames:v 1 -vf "scale=iw/2:ih/2" "$tmp/f-$(printf %02d $i)-$t.png"
done
${PYTHON:-python3} "$here/sheet.py" "$sheet" "$tmp"/*.png
rm -rf "$tmp"
