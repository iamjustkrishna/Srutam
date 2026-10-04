#!/usr/bin/env bash
# Downloads the on-device speech model into app/src/main/assets.
#
# Model: NVIDIA Parakeet-TDT-CTC-110M (English), int8 ONNX export published by the sherpa-onnx project.
# It is not committed to git because the model file (131 MB) is over GitHub's 100 MB file limit.
# Safe to re-run: files that already match the expected SHA-256 are left alone.
set -euo pipefail

ARCHIVE_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-nemo-parakeet_tdt_ctc_110m-en-36000-int8.tar.bz2"
ARCHIVE_DIR="sherpa-onnx-nemo-parakeet_tdt_ctc_110m-en-36000-int8"
ASSETS="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/app/src/main/assets"

# asset name | file inside the archive | expected SHA-256
FILES=(
  "parakeet-110m.int8.onnx|model.int8.onnx|9177a9146cf32ee0cc8152276ef95116f312018d316be37ccf57f7efea81fc1a"
  "parakeet-110m-tokens.txt|tokens.txt|450e56bd2f036fe5b6aa821865838cc5aa9d8b0106134ce9a9ba0664abe6cd10"
)

sha256() {
  if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | cut -d' ' -f1; else shasum -a 256 "$1" | cut -d' ' -f1; fi
}

up_to_date=true
for entry in "${FILES[@]}"; do
  IFS='|' read -r name _ expected <<<"$entry"
  if [[ ! -f "$ASSETS/$name" || "$(sha256 "$ASSETS/$name")" != "$expected" ]]; then up_to_date=false; fi
done
if $up_to_date; then
  echo "Speech model is already up to date."
  exit 0
fi

TEMP="$(mktemp -d)"
trap 'rm -rf "$TEMP"' EXIT

echo "Downloading $ARCHIVE_URL (about 100 MB)..."
curl -fL --retry 3 -o "$TEMP/model.tar.bz2" "$ARCHIVE_URL"
tar -xjf "$TEMP/model.tar.bz2" -C "$TEMP"

mkdir -p "$ASSETS"
for entry in "${FILES[@]}"; do
  IFS='|' read -r name source expected <<<"$entry"
  actual="$(sha256 "$TEMP/$ARCHIVE_DIR/$source")"
  if [[ "$actual" != "$expected" ]]; then
    echo "Checksum mismatch for $source: expected $expected but got $actual" >&2
    exit 1
  fi
  cp "$TEMP/$ARCHIVE_DIR/$source" "$ASSETS/$name"
  echo "Installed $name"
done
