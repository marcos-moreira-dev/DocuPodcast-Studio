package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Associates built-in adapters with the existing repository layout without moving heavy assets. */
public final class RuntimeAssetCatalog {
    private final Path root;
    private final Map<EngineId, Map<String, Path>> assets;

    public RuntimeAssetCatalog(Path root, Map<EngineId, Map<String, Path>> assets) {
        this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        LinkedHashMap<EngineId, Map<String, Path>> normalized = new LinkedHashMap<>();
        if (assets != null) assets.forEach((engine, entries) -> normalized.put(engine, Map.copyOf(entries)));
        this.assets = Map.copyOf(normalized);
    }

    public Path root() { return root; }

    public Path require(EngineId engineId, String asset) {
        Map<String, Path> engineAssets = assets.get(engineId);
        if (engineAssets == null || !engineAssets.containsKey(asset)) {
            throw new IllegalArgumentException("runtime asset not registered: " + engineId + "." + asset);
        }
        return engineAssets.get(asset);
    }

    public Map<String, Path> assetsFor(EngineId engineId) {
        return assets.getOrDefault(engineId, Map.of());
    }

    public static RuntimeAssetCatalog currentLayout(LocalMediaLayout layout) {
        Objects.requireNonNull(layout, "local media layout");
        Path installation = layout.installationRoot();
        Path runtime = layout.runtimeRoot();
        return new RuntimeAssetCatalog(runtime, Map.of(
                PiperVoiceEngine.ID, Map.of(
                        "script", installation.resolve("scripts/tts/piper-file-to-wav.ps1"),
                        "executable", runtime.resolve("tools/piper/piper.exe"),
                        "model", runtime.resolve("models/tts/piper/voices/es_ES-default-medium.onnx"),
                        "metadata", runtime.resolve("models/tts/piper/voices/es_ES-default-medium.onnx.json")),
                XttsVoiceEngine.ID, Map.ofEntries(
                        Map.entry("script", installation.resolve("scripts/tts/xtts-file-to-wav.ps1")),
                        Map.entry("batchScript", installation.resolve("scripts/tts/xtts-batch-to-wav.ps1")),
                        Map.entry("python", runtime.resolve("tools/xtts-wrapper/.venv/Scripts/python.exe")),
                        Map.entry("wrapper", runtime.resolve("tools/xtts-wrapper/synthesize_xtts.py")),
                        Map.entry("batchWrapper", runtime.resolve("tools/xtts-wrapper/synthesize_xtts_batch.py")),
                        Map.entry("workerWrapper", runtime.resolve("tools/xtts-wrapper/synthesize_xtts_worker.py")),
                        Map.entry("modelDirectory", runtime.resolve("models/tts/xtts")),
                        Map.entry("modelConfig", runtime.resolve("models/tts/xtts/config.json")),
                        Map.entry("modelCheckpoint", runtime.resolve("models/tts/xtts/model.pth")),
                        Map.entry("vocabulary", runtime.resolve("models/tts/xtts/vocab.json")),
                        Map.entry("speakers", runtime.resolve("models/tts/xtts/speakers_xtts.pth")),
                        Map.entry("dvae", runtime.resolve("models/tts/xtts/dvae.pth")),
                        Map.entry("melStats", runtime.resolve("models/tts/xtts/mel_stats.pth")),
                        Map.entry("speaker", runtime.resolve("models/tts/xtts/speakers/voz-por-defecto.wav"))),
                Qwen3TtsVoiceEngine.ID, Map.of(
                        "executable", runtime.resolve("tools/qwen3-tts/llama.cpp/llama-tts.exe"),
                        "model", runtime.resolve("models/tts/qwen3-tts/Qwen3-TTS-12Hz-1.7B-Base-Q8_0.gguf"),
                        "codec", runtime.resolve("models/tts/qwen3-tts/mmproj-Qwen3-TTS-12Hz-1.7B-Base-Q8_0.gguf"),
                        "speaker", installation.resolve("samples/voices/advanced-presets/"
                                + "hombre_adulto_personaje_narrativo/neutral.wav")),
                ComfyUiImageEngine.ID, Map.ofEntries(
                        Map.entry("models", runtime.resolve("models/image")),
                        Map.entry("runtime", runtime.resolve("tools/image/ComfyUI")),
                        Map.entry("python", runtime.resolve("tools/image/venv/Scripts/python.exe")),
                        Map.entry("main", runtime.resolve("tools/image/ComfyUI/main.py")),
                        Map.entry("extraModelPaths", runtime.resolve("tools/image/extra_model_paths.yaml")),
                        Map.entry("draftWorkflow", runtime.resolve("models/image/workflows/workflow-sd15-reference.json")),
                        Map.entry("fluxWorkflow", runtime.resolve("models/image/workflows/workflow-flux-reference.json")),
                        Map.entry("sd15Model", runtime.resolve("models/image/v1-5-pruned-emaonly-fp16.safetensors")),
                        Map.entry("dreamshaperModel", runtime.resolve("models/image/DreamShaper_8_pruned.safetensors")),
                        Map.entry("sdxlModel", runtime.resolve("models/image/sd_xl_base_1.0.safetensors")),
                        Map.entry("fluxModel", runtime.resolve("models/image/flux1-dev.safetensors")),
                        Map.entry("fluxClipL", runtime.resolve("models/image/text_encoders/clip_l.safetensors")),
                        Map.entry("fluxT5", runtime.resolve("models/image/text_encoders/t5xxl_bf16.safetensors")),
                        Map.entry("fluxVae", runtime.resolve("models/image/vae/ae.safetensors")),
                        Map.entry("ipAdapterModel", runtime.resolve(
                                "tools/image/ComfyUI/models/ipadapter/ip-adapter-plus_sd15.safetensors")),
                        Map.entry("clipVisionModel", runtime.resolve(
                                "tools/image/ComfyUI/models/clip_vision/CLIP-ViT-H-14-laion2B-s32B-b79K.safetensors")),
                        Map.entry("scribbleControlNet", runtime.resolve(
                                "tools/image/ComfyUI/models/controlnet/control_v11p_sd15_scribble_fp16.safetensors")),
                        Map.entry("ipAdapterNode", runtime.resolve(
                                "tools/image/ComfyUI/custom_nodes/comfyui-ipadapter")),
                        Map.entry("ipAdapterArchive", runtime.resolve(
                                "tools/image/downloads/comfyui-ipadapter-b188a6cb.zip")),
                        Map.entry("rife", runtime.resolve("tools/image/ComfyUI/models/frame_interpolation/rife_v4.25_lite.safetensors"))),
                ComfyUiSuperResolutionEngine.ID, Map.of(
                        "runtime", runtime.resolve("tools/image/ComfyUI"),
                        "model", runtime.resolve("tools/image/ComfyUI/models/upscale_models/RealESRGAN_x4plus.pth")),
                ComfyUiTileRefinementEngine.ID, Map.of(
                        "runtime", runtime.resolve("tools/image/ComfyUI"),
                        "controlNet", runtime.resolve(
                                "tools/image/ComfyUI/models/controlnet/control_v11f1e_sd15_tile.pth"),
                        "checkpoint", runtime.resolve(
                                "models/image/v1-5-pruned-emaonly-fp16.safetensors")),
                ComfyUiVideoGenerationEngine.ID, Map.of(
                        "runtime", runtime.resolve("tools/image/ComfyUI"),
                        "python", runtime.resolve("tools/image/venv/Scripts/python.exe"),
                        "main", runtime.resolve("tools/image/ComfyUI/main.py"),
                        "wanBalancedWorkflow", runtime.resolve("models/video/workflows/workflow-wan22-ti2v-5b-api.json"),
                        "wanQualityWorkflow", runtime.resolve("models/video/workflows/workflow-wan22-i2v-14b-api.json"),
                        "ltxPortraitWorkflow", runtime.resolve("models/video/workflows/workflow-ltx23-i2v-portrait-api.json")),
                QwenVisualAnalysisEngine.ID, Map.of(
                        "executable", runtime.resolve("tools/document-ai/ollama/ollama.exe"),
                        "models", runtime.resolve("tools/document-ai/ollama-models"),
                        "logs", runtime.resolve("logs/document-ai")),
                PpStructureV3Engine.ID, Map.of(
                        "runtime", runtime.resolve("tools/document-ai/pp-structure"),
                        "python", runtime.resolve("tools/document-ai/pp-structure/python/python.exe"),
                        "models", runtime.resolve("tools/document-ai/pp-structure/models"),
                        "script", runtime.resolve(
                                "tools/document-ai/pp-structure/bridge/pp_structure_v3.py")),
                FfmpegVideoRenderEngine.ID, Map.of(
                        "executable", runtime.resolve("tools/ffmpeg/bin/ffmpeg.exe"))));
    }
}
