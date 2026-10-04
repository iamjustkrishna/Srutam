# Third-party notices

Srutam's on-device speech recognition is built from the following components.

## Speech recognition model: NVIDIA Parakeet-TDT-CTC-110M

- Creator: NVIDIA. Source: <https://huggingface.co/nvidia/parakeet-tdt_ctc-110m>
- License: [Creative Commons Attribution 4.0 International (CC BY 4.0)](https://creativecommons.org/licenses/by/4.0/)
- Changes: the model was converted to ONNX and quantized to int8 by the sherpa-onnx project
  (<https://github.com/k2-fsa/sherpa-onnx/releases/tag/asr-models>, file
  `sherpa-onnx-nemo-parakeet_tdt_ctc_110m-en-36000-int8`). Srutam uses it unmodified from that export.
- NVIDIA does not endorse Srutam.

The model file is not stored in this repository; run `scripts/fetch-asr-model.ps1` (Windows) or
`scripts/fetch-asr-model.sh` to download it into `app/src/main/assets/` before building.

## Voice activity detection model: Silero VAD

- Source: <https://github.com/snakers4/silero-vad>
- License: MIT

## Inference runtime: sherpa-onnx and ONNX Runtime

- sherpa-onnx (<https://github.com/k2-fsa/sherpa-onnx>): Apache License 2.0. Prebuilt `libsherpa-onnx-jni.so`
  and the Kotlin wrappers under `app/src/main/java/com/k2fsa/sherpa/onnx/` come from release v1.12.39.
- ONNX Runtime (<https://github.com/microsoft/onnxruntime>): MIT License
