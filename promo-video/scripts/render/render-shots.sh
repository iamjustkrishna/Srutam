#!/bin/bash
# Render a composition shot by shot, so a change to one shot re-renders only that shot.
#
#   scripts/render/render-shots.sh <composition> <story-name> [shot ...]
#   e.g. scripts/render/render-shots.sh BrainCourt court            # render anything missing
#        scripts/render/render-shots.sh BrainCourt court judge      # re-render just "judge"
#
# Clips go to out/<story>-shots/NN-<shot>.mp4 (muted, render fps). Existing clips are kept
# unless named. Sound changes never need this: run scripts/render/render.sh, which reuses
# the video chunks and only re-renders + re-normalises the audio (about 10 s).
set -e
comp=$1; story=$2; shift 2
export REMOTION_BROWSER_EXECUTABLE=${REMOTION_BROWSER_EXECUTABLE:-$(ls /opt/pw-browsers/chromium_headless_shell-*/chrome-linux/headless_shell 2>/dev/null | head -1)}
dir=out/$story-shots; mkdir -p "$dir"
npx esbuild "src/$story/timeline.ts" --bundle --format=esm --log-level=error --outfile="$dir/.t.mjs"
K=${K:-2}; : > "$dir/list.new"
n=0
node -e 'import("./'"$dir"'/.t.mjs").then(t=>{for(const [k,s] of Object.entries(t.SHOTS))console.log(k,s.from,s.duration)})' | while read -r shot from dur; do
  n=$((n+1)); f=$dir/$(printf %02d $n)-$shot.mp4
  want=$((dur*K))
  if [[ " $* " == *" $shot "* ]] || [ ! -s "$f" ]; then
    rm -f "$f"
    npx remotion render "$comp" "$f" --frames=$((from*K))-$((from*K+want-1)) --muted --log=error --concurrency=3
    echo "rendered $f"
  fi
  echo "file '$(basename "$f")'" >> "$dir/list.new"
done
mv "$dir/list.new" "$dir/list.txt"
echo "shots in $dir (list.txt for concat; -c copy joins them)"
