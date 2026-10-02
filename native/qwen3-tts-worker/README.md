# Qwen3-TTS batch worker

This small executable is built against llama.cpp commit `d775b8967` with its own
matching libraries. Unlike the upstream demonstration CLI, it loads the backbone
and codec once, then accepts one manifest per audio unit over standard input.

Install the executable and its matching DLLs in `resident-worker/` beside
`llama-tts.exe`. Do not mix its DLLs with the existing runtime. The Java adapter
prefers this isolated directory, also accepts a sibling worker, and otherwise
retains the official one-shot fallback. A batch owns one process and releases it
on completion, cancellation, failure or a scheduler yield.

Build with MSVC and CUDA (architecture 75 for GTX 1650):

```text
cmake -S native/qwen3-tts-worker -B target/qwen3-tts-worker-msvc -G Ninja -DLLAMA_CPP_ROOT=<source> -DGGML_CUDA=ON -DCMAKE_CUDA_ARCHITECTURES=75-real -DCMAKE_BUILD_TYPE=Release
cmake --build target/qwen3-tts-worker-msvc --target llama-tts-worker
```

The optional `smoke.ps1` runs two short local speech requests, checks that the
same process replies to both, and verifies clean shutdown. Outputs stay under
`target/qwen-worker-smoke`. It requires the existing Qwen models and sample voice.
