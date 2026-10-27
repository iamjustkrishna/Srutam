#!/bin/bash
# Resumable render of any composition, with its own audio loudness-normalised.
#
#   scripts/render/render.sh <composition> <render frames> <out.mp4> [audio source]
#
# Run from promo-video/. Video renders in 720-frame chunks under <out dir>/chunks-<comp>;
# chunks that are already complete are skipped, so a killed render resumes where it
# stopped. Without an audio source, the composition's audio is rendered to wav and
# two-pass loudnorm'd to -14 LUFS / -1.5 dBTP; with one (an mp4 or wav), that is muxed.
# Set REMOTION_BROWSER_EXECUTABLE if Chromium isn't found on its own.
set -e
export REMOTION_BROWSER_EXECUTABLE=${REMOTION_BROWSER_EXECUTABLE:-$(ls /opt/pw-browsers/chromium_headless_shell-*/chrome-linux/headless_shell 2>/dev/null | head -1)}
comp=$1; total=$2; out=$3; audio=$4; step=720
dir=$(dirname "$out")/chunks-$comp; mkdir -p "$dir"
if [ -z "$audio" ]; then
  audio=$dir/audio-norm.wav
  if [ ! -s "$audio" ]; then
    [ -s "$dir/audio.wav" ] || npx remotion render "$comp" "$dir/audio.wav" --codec=wav --log=error --concurrency=3 > "$dir/log-audio.txt" 2>&1
    m=$(ffmpeg -hide_banner -i "$dir/audio.wav" -af loudnorm=I=-14:TP=-1.5:LRA=11:print_format=json -f null - 2>&1 | python3 -c '
import sys,json; t=sys.stdin.read(); j=json.loads(t[t.rindex("{"):t.rindex("}")+1])
print("measured_I=%s:measured_TP=%s:measured_LRA=%s:measured_thresh=%s:offset=%s"%(j["input_i"],j["input_tp"],j["input_lra"],j["input_thresh"],j["target_offset"]))')
    ffmpeg -y -loglevel error -i "$dir/audio.wav" -af "loudnorm=I=-14:TP=-1.5:LRA=11:$m:linear=true" -ar 48000 "$audio"
  fi
fi
: > "$dir/list.txt"
for ((a=0; a<total; a+=step)); do
  b=$((a+step-1)); [ $b -ge $total ] && b=$((total-1))
  f=$dir/$(printf %05d $a).mp4
  want=$((b-a+1))
  have=$(ffprobe -v error -count_frames -select_streams v:0 -show_entries stream=nb_read_frames -of csv=p=0 "$f" 2>/dev/null | tr -dc 0-9)
  if [ "$have" != "$want" ]; then
    rm -f "$f"
    npx remotion render "$comp" "$f" --frames=$a-$b --muted --log=error --concurrency=3 > "$dir/log-$a.txt" 2>&1
  fi
  echo "file '$(basename "$f")'" >> "$dir/list.txt"
done
ffmpeg -y -loglevel error -f concat -safe 0 -i "$dir/list.txt" -c copy "$dir/joined.mp4"
case $audio in
  *.wav) ffmpeg -y -loglevel error -i "$dir/joined.mp4" -i "$audio" -map 0:v -map 1:a -c:v copy -c:a aac -b:a 192k -movflags +faststart "$out" ;;
  *) ffmpeg -y -loglevel error -i "$dir/joined.mp4" -i "$audio" -map 0:v -map 1:a -c:v copy -c:a copy -movflags +faststart "$out" ;;
esac
echo "DONE $out"
