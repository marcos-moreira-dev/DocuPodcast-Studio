package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Explicit built-in adapter catalog used only by the composition root. */
public final class LocalMediaAdapters {
    private LocalMediaAdapters() { }

    public static MediaEnginePlatform create(Path applicationRoot) {
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize()
                : applicationRoot.toAbsolutePath().normalize();
        RuntimeAssetCatalog assets = RuntimeAssetCatalog.currentLayout(root);
        EngineRegistry<VoiceSynthesisEngine> voices = new EngineRegistry<>(CapabilityId.VOICE_SYNTHESIS);
        voices.register(new PiperVoiceEngine(new EngineConfiguration(PiperVoiceEngine.ID,
                Map.of("commandTemplate", piperCommand(assets)))));
        voices.register(new XttsVoiceEngine(new EngineConfiguration(XttsVoiceEngine.ID,
                Map.of("commandTemplate", xttsCommand(assets)))));

        EngineRegistry<ImageGenerationEngine> images = new EngineRegistry<>(CapabilityId.IMAGE_GENERATION);
        images.register(new ComfyUiImageEngine(new EngineConfiguration(ComfyUiImageEngine.ID,
                Map.of("baseUrl", "http://127.0.0.1:8188"))));

        Path embedded = assets.require(FfmpegVideoRenderEngine.ID, "executable");
        EngineRegistry<VideoRenderEngine> videos = new EngineRegistry<>(CapabilityId.VIDEO_RENDERING);
        videos.register(new FfmpegVideoRenderEngine(new EngineConfiguration(FfmpegVideoRenderEngine.ID,
                Map.of("executable", Files.isRegularFile(embedded) ? embedded.toString() : "ffmpeg"))));
        return new MediaEnginePlatform(voices, images, videos);
    }

    private static String piperCommand(RuntimeAssetCatalog assets) {
        return quote("powershell.exe") + " -NoProfile -ExecutionPolicy Bypass -File "
                + quote(assets.require(PiperVoiceEngine.ID, "script"))
                + " -Piper " + quote(assets.require(PiperVoiceEngine.ID, "executable"))
                + " -Model " + quote(assets.require(PiperVoiceEngine.ID, "model"))
                + " -Text " + quote("{textFile}") + " -Output " + quote("{outputFile}");
    }

    private static String xttsCommand(RuntimeAssetCatalog assets) {
        return quote("powershell.exe") + " -NoProfile -ExecutionPolicy Bypass -File "
                + quote(assets.require(XttsVoiceEngine.ID, "script"))
                + " -Python " + quote(assets.require(XttsVoiceEngine.ID, "python"))
                + " -Wrapper " + quote(assets.require(XttsVoiceEngine.ID, "wrapper"))
                + " -ModelDir " + quote(assets.require(XttsVoiceEngine.ID, "modelDirectory"))
                + " -SpeakerWav " + quote(assets.require(XttsVoiceEngine.ID, "speaker"))
                + " -Text " + quote("{textFile}") + " -Output " + quote("{outputFile}")
                + " -Language " + quote("{language}");
    }

    private static String quote(Object value) { return "\"" + value + "\""; }
}
