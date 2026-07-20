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

    public static RuntimeAssetCatalog currentLayout(Path applicationRoot) {
        Path root = applicationRoot == null ? Path.of(".") : applicationRoot;
        root = root.toAbsolutePath().normalize();
        return new RuntimeAssetCatalog(root, Map.of(
                PiperVoiceEngine.ID, Map.of(
                        "script", root.resolve("scripts/tts/piper-file-to-wav.ps1"),
                        "executable", root.resolve("tools/piper/piper.exe"),
                        "model", root.resolve("models/tts/piper/voices/es_ES-default-medium.onnx")),
                XttsVoiceEngine.ID, Map.of(
                        "script", root.resolve("scripts/tts/xtts-file-to-wav.ps1"),
                        "python", root.resolve("tools/xtts-wrapper/.venv/Scripts/python.exe"),
                        "wrapper", root.resolve("tools/xtts-wrapper/synthesize_xtts.py"),
                        "modelDirectory", root.resolve("models/tts/xtts"),
                        "speaker", root.resolve("models/tts/xtts/speakers/voz-por-defecto.wav")),
                ComfyUiImageEngine.ID, Map.of(
                        "models", root.resolve("models/image"),
                        "runtime", root.resolve("runtime/comfyui"),
                        "rife", root.resolve("models/video/rife")),
                FfmpegVideoRenderEngine.ID, Map.of(
                        "executable", root.resolve("tools/ffmpeg/bin/ffmpeg.exe"))));
    }
}
