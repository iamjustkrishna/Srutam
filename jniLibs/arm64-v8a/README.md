# Introduction

Note that if you use Android Studio, then you only need to
copy libonnxruntime.so and libsherpa-onnx-jni.so
to your jniLibs, and you don't need libsherpa-onnx-c-api.so or
libsherpa-onnx-cxx-api.so.

libsherpa-onnx-c-api.so and libsherpa-onnx-cxx-api.so are for users
who don't use JNI. In that case, libsherpa-onnx-jni.so is not needed.

In any case, libonnxruntime.so is always needed.

## This folder differs from the other ABIs

libsherpa-onnx-jni.so here is sherpa-onnx v1.12.39's `android-static-link-onnxruntime` build:
ONNX Runtime is linked into it, so there is intentionally no libonnxruntime.so (22.2 MB instead of
30.4 MB total). Only arm64-v8a is packaged (see abiFilters in app/build.gradle.kts).
