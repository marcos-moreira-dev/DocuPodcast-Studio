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
                Map.of("commandTemplate", xttsCommand(assets),
                        "batchCommandTemplate", xttsBatchCommand(assets),
                        "defaultSpeaker", assets.require(XttsVoiceEngine.ID, "speaker").toString()))));

        EngineRegistry<ImageGenerationEngine> images = new EngineRegistry<>(CapabilityId.IMAGE_GENERATION);
        images.register(new ComfyUiImageEngine(new EngineConfiguration(ComfyUiImageEngine.ID,
                Map.of("baseUrl", "http://127.0.0.1:8188",
                        "draftWorkflow", assets.require(ComfyUiImageEngine.ID, "draftWorkflow").toString()))));

        EngineRegistry<VideoGenerationEngine> videoGeneration = new EngineRegistry<>(CapabilityId.VIDEO_GENERATION);
        videoGeneration.register(new ComfyUiVideoGenerationEngine(new EngineConfiguration(ComfyUiVideoGenerationEngine.ID,
                Map.of("baseUrl", "http://127.0.0.1:8188",
                        "wanBalancedWorkflow", assets.require(ComfyUiVideoGenerationEngine.ID, "wanBalancedWorkflow").toString(),
                        "wanQualityWorkflow", assets.require(ComfyUiVideoGenerationEngine.ID, "wanQualityWorkflow").toString(),
                        "ltxPortraitWorkflow", assets.require(ComfyUiVideoGenerationEngine.ID, "ltxPortraitWorkflow").toString()))));

        Path embedded = assets.require(FfmpegVideoRenderEngine.ID, "executable");
        EngineRegistry<VideoRenderEngine> videos = new EngineRegistry<>(CapabilityId.VIDEO_RENDERING);
        videos.register(new FfmpegVideoRenderEngine(new EngineConfiguration(FfmpegVideoRenderEngine.ID,
                Map.of("executable", Files.isRegularFile(embedded) ? embedded.toString() : "ffmpeg"))));
        EngineAdministrationRegistry administration = new EngineAdministrationRegistry();
        voices.engines().forEach(engine -> administration.register(ReadinessEngineAdministration.forEngine(engine)));
        images.engines().forEach(engine -> administration.register(ReadinessEngineAdministration.forEngine(engine)));
        videoGeneration.engines().forEach(engine -> administration.register(ReadinessEngineAdministration.forEngine(engine)));
        videos.engines().forEach(engine -> administration.register(ReadinessEngineAdministration.forEngine(engine)));
        return new MediaEnginePlatform(voices, images, videoGeneration, videos, administration);
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

    private static String xttsBatchCommand(RuntimeAssetCatalog assets) {
        return quote("powershell.exe") + " -NoProfile -ExecutionPolicy Bypass -File "
                + quote(assets.require(XttsVoiceEngine.ID, "batchScript"))
                + " -Python " + quote(assets.require(XttsVoiceEngine.ID, "python"))
                + " -Wrapper " + quote(assets.require(XttsVoiceEngine.ID, "batchWrapper"))
                + " -ModelDir " + quote(assets.require(XttsVoiceEngine.ID, "modelDirectory"))
                + " -Manifest " + quote("{manifestFile}")
                + " -Language " + quote("{language}");
    }

    private static String quote(Object value) { return "\"" + value + "\""; }
}
