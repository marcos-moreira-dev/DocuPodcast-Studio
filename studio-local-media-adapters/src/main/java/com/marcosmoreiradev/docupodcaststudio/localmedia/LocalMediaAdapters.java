package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/** Explicit built-in adapter catalog used only by the composition root. */
public final class LocalMediaAdapters {
    private LocalMediaAdapters() { }

    public static MediaEnginePlatform create(LocalMediaLayout layout) {
        return create(layout, requested ->
                ComfyUiLaunchProfile.automatic(ComfyUiMemoryProfile.from(requested)), () -> "auto");
    }

    public static MediaEnginePlatform create(LocalMediaLayout layout,
                                             Function<String, ComfyUiLaunchProfile> launchProfiles) {
        return create(layout, launchProfiles, () -> "auto");
    }

    public static MediaEnginePlatform create(LocalMediaLayout layout,
                                             Function<String, ComfyUiLaunchProfile> launchProfiles,
                                             Supplier<String> voiceDevice) {
        RuntimeAssetCatalog assets = RuntimeAssetCatalog.currentLayout(layout);
        EngineRegistry<VoiceSynthesisEngine> voices = new EngineRegistry<>(CapabilityId.VOICE_SYNTHESIS);
        voices.register(new PiperVoiceEngine(new EngineConfiguration(PiperVoiceEngine.ID,
                Map.of("commandTemplate", piperCommand(assets),
                        "script", assets.require(PiperVoiceEngine.ID, "script").toString(),
                        "executable", assets.require(PiperVoiceEngine.ID, "executable").toString(),
                        "model", assets.require(PiperVoiceEngine.ID, "model").toString(),
                        "metadata", assets.require(PiperVoiceEngine.ID, "metadata").toString()))));
        voices.register(new XttsVoiceEngine(new EngineConfiguration(XttsVoiceEngine.ID,
                Map.ofEntries(
                        Map.entry("commandTemplate", xttsCommand(assets)),
                        Map.entry("batchCommandTemplate", xttsBatchCommand(assets)),
                        Map.entry("runtimeRoot", assets.root().toString()),
                        Map.entry("script", assets.require(XttsVoiceEngine.ID, "script").toString()),
                        Map.entry("batchScript", assets.require(XttsVoiceEngine.ID, "batchScript").toString()),
                        Map.entry("python", assets.require(XttsVoiceEngine.ID, "python").toString()),
                        Map.entry("wrapper", assets.require(XttsVoiceEngine.ID, "wrapper").toString()),
                        Map.entry("batchWrapper", assets.require(XttsVoiceEngine.ID, "batchWrapper").toString()),
                        Map.entry("workerWrapper", assets.require(XttsVoiceEngine.ID, "workerWrapper").toString()),
                        Map.entry("modelDirectory", assets.require(XttsVoiceEngine.ID, "modelDirectory").toString()),
                        Map.entry("modelConfig", assets.require(XttsVoiceEngine.ID, "modelConfig").toString()),
                        Map.entry("modelCheckpoint", assets.require(XttsVoiceEngine.ID, "modelCheckpoint").toString()),
                        Map.entry("vocabulary", assets.require(XttsVoiceEngine.ID, "vocabulary").toString()),
                        Map.entry("speakers", assets.require(XttsVoiceEngine.ID, "speakers").toString()),
                        Map.entry("dvae", assets.require(XttsVoiceEngine.ID, "dvae").toString()),
                        Map.entry("melStats", assets.require(XttsVoiceEngine.ID, "melStats").toString()),
                        Map.entry("defaultSpeaker", assets.require(XttsVoiceEngine.ID, "speaker").toString()))),
                voiceDevice));
        voices.register(new Qwen3TtsVoiceEngine(new EngineConfiguration(Qwen3TtsVoiceEngine.ID,
                Map.of(
                        "executable", assets.require(Qwen3TtsVoiceEngine.ID, "executable").toString(),
                        "model", assets.require(Qwen3TtsVoiceEngine.ID, "model").toString(),
                        "codec", assets.require(Qwen3TtsVoiceEngine.ID, "codec").toString(),
                        "defaultSpeaker", assets.require(Qwen3TtsVoiceEngine.ID, "speaker").toString())),
                voiceDevice));

        ComfyUiManagedProcess comfyProcess = new ComfyUiManagedProcess();
        ComfyUiImageEngine.ManagedRuntime imageRuntime = new ComfyUiImageEngine.ManagedRuntime() {
            private ComfyUiLaunchProfile profile() {
                return launchProfiles == null
                        ? ComfyUiLaunchProfile.automatic(ComfyUiMemoryProfile.SAFE_LOW_VRAM)
                        : launchProfiles.apply(null);
            }
            public void ensureReady(ExecutionContext context) throws java.io.IOException, InterruptedException {
                comfyProcess.start(assets, profile(), context);
            }
            public boolean recover(ExecutionContext context) throws java.io.IOException, InterruptedException {
                return comfyProcess.recoverStopped(assets, profile(), context);
            }
        };
        EngineRegistry<ImageGenerationEngine> images = new EngineRegistry<>(CapabilityId.IMAGE_GENERATION);
        images.register(new ComfyUiImageEngine(new EngineConfiguration(ComfyUiImageEngine.ID,
                Map.ofEntries(
                        Map.entry("baseUrl", "http://127.0.0.1:8188"),
                        Map.entry("draftWorkflow", assets.require(ComfyUiImageEngine.ID, "draftWorkflow").toString()),
                        Map.entry("fluxWorkflow", assets.require(ComfyUiImageEngine.ID, "fluxWorkflow").toString()),
                        Map.entry("sd15Model", assets.require(ComfyUiImageEngine.ID, "sd15Model").toString()),
                        Map.entry("dreamshaperModel", assets.require(ComfyUiImageEngine.ID, "dreamshaperModel").toString()),
                        Map.entry("sdxlModel", assets.require(ComfyUiImageEngine.ID, "sdxlModel").toString()),
                        Map.entry("fluxModel", assets.require(ComfyUiImageEngine.ID, "fluxModel").toString()),
                        Map.entry("fluxClipL", assets.require(ComfyUiImageEngine.ID, "fluxClipL").toString()),
                        Map.entry("fluxT5", assets.require(ComfyUiImageEngine.ID, "fluxT5").toString()),
                        Map.entry("fluxVae", assets.require(ComfyUiImageEngine.ID, "fluxVae").toString()),
                        Map.entry("ipAdapterModel", assets.require(ComfyUiImageEngine.ID, "ipAdapterModel").toString()),
                        Map.entry("clipVisionModel", assets.require(ComfyUiImageEngine.ID, "clipVisionModel").toString()),
                        Map.entry("scribbleControlNet", assets.require(ComfyUiImageEngine.ID, "scribbleControlNet").toString()),
                        Map.entry("ipAdapterNode", assets.require(ComfyUiImageEngine.ID, "ipAdapterNode").toString()))), imageRuntime));

        EngineRegistry<ImageSuperResolutionEngine> superResolution =
                new EngineRegistry<>(CapabilityId.IMAGE_SUPER_RESOLUTION);
        ComfyUiSuperResolutionEngine realEsrgan = new ComfyUiSuperResolutionEngine(
                new EngineConfiguration(ComfyUiSuperResolutionEngine.ID, Map.of(
                        "baseUrl", "http://127.0.0.1:8188",
                        "modelPath", assets.require(ComfyUiSuperResolutionEngine.ID, "model").toString())));
        superResolution.register(realEsrgan);

        EngineRegistry<ImageRefinementEngine> refinement =
                new EngineRegistry<>(CapabilityId.IMAGE_REFINEMENT);
        ComfyUiTileRefinementEngine controlNetTile = new ComfyUiTileRefinementEngine(
                new EngineConfiguration(ComfyUiTileRefinementEngine.ID, Map.of(
                        "baseUrl", "http://127.0.0.1:8188",
                        "controlNetPath", assets.require(
                                ComfyUiTileRefinementEngine.ID, "controlNet").toString(),
                        "checkpointPath", assets.require(
                                ComfyUiTileRefinementEngine.ID, "checkpoint").toString())));
        refinement.register(controlNetTile);

        EngineRegistry<VideoGenerationEngine> videoGeneration = new EngineRegistry<>(CapabilityId.VIDEO_GENERATION);
        videoGeneration.register(new ComfyUiVideoGenerationEngine(new EngineConfiguration(ComfyUiVideoGenerationEngine.ID,
                Map.of("baseUrl", "http://127.0.0.1:8188",
                        "wanBalancedWorkflow", assets.require(ComfyUiVideoGenerationEngine.ID, "wanBalancedWorkflow").toString(),
                        "wanQualityWorkflow", assets.require(ComfyUiVideoGenerationEngine.ID, "wanQualityWorkflow").toString(),
                        "ltxPortraitWorkflow", assets.require(ComfyUiVideoGenerationEngine.ID, "ltxPortraitWorkflow").toString()))));

        Path embedded = assets.require(FfmpegVideoRenderEngine.ID, "executable");
        EngineRegistry<VideoRenderEngine> videos = new EngineRegistry<>(CapabilityId.VIDEO_RENDERING);
        FfmpegVideoRenderEngine ffmpeg = new FfmpegVideoRenderEngine(new EngineConfiguration(FfmpegVideoRenderEngine.ID,
                Map.of("executable", Files.isRegularFile(embedded) ? embedded.toString() : "ffmpeg")));
        videos.register(ffmpeg);
        ContentAnalysisEngineRegistry contentAnalysis = new ContentAnalysisEngineRegistry();
        PdfVlmRuntimeProfile pdfVlmProfile = PdfVlmRuntimeProfile.fromSystem();
        ManagedOllamaProcess ollamaProcess = new ManagedOllamaProcess(
                assets.require(QwenVisualAnalysisEngine.ID, "executable"),
                assets.require(QwenVisualAnalysisEngine.ID, "models"),
                assets.require(QwenVisualAnalysisEngine.ID, "logs"),
                pdfVlmProfile.kvCacheType(), pdfVlmProfile.flashAttention());
        EngineCertificationStore certifications = new FileEngineCertificationStore(
                assets.root().resolve("state/document-ai-certifications"));
        QwenVisualAnalysisEngine qwenVisual = new QwenVisualAnalysisEngine(
                new EngineConfiguration(QwenVisualAnalysisEngine.ID, Map.of(
                        "model", pdfVlmProfile.model(),
                        "contextTokens", Integer.toString(pdfVlmProfile.contextTokens()),
                        "maxOutputTokens", Integer.toString(pdfVlmProfile.maxOutputTokens()),
                        "batchSize", Integer.toString(pdfVlmProfile.batchSize()),
                        "kvCacheType", pdfVlmProfile.kvCacheType(),
                        "flashAttention", Boolean.toString(pdfVlmProfile.flashAttention()))),
                ollamaProcess, certifications);
        contentAnalysis.register(qwenVisual);
        contentAnalysis.register(new QwenContextCorrectionEngine(qwenVisual));
        contentAnalysis.register(new QwenNarratabilityAnalysisEngine(qwenVisual));
        contentAnalysis.register(new QwenNarrationTranslationEngine(qwenVisual));
        contentAnalysis.register(new QwenTableAnalysisEngine(qwenVisual));
        PpStructureV3Engine ppStructure = new PpStructureV3Engine(
                new EngineConfiguration(PpStructureV3Engine.ID, Map.of(
                        "runtime", assets.require(PpStructureV3Engine.ID, "runtime").toString(),
                        "python", assets.require(PpStructureV3Engine.ID, "python").toString(),
                        "models", assets.require(PpStructureV3Engine.ID, "models").toString(),
                        "script", assets.require(PpStructureV3Engine.ID, "script").toString())),
                certifications);
        contentAnalysis.register(ppStructure);
        contentAnalysis.register(new PpFormulaRecognitionEngine(ppStructure));
        MathCatSpeechEngine mathCat = new MathCatSpeechEngine(assets.root(), certifications);
        contentAnalysis.register(mathCat);
        EngineAdministrationRegistry administration = new EngineAdministrationRegistry();
        administration.register(new PiperEngineAdministration(voices.require(PiperVoiceEngine.ID), assets));
        administration.register(new XttsEngineAdministration(voices.require(XttsVoiceEngine.ID), assets));
        administration.register(new Qwen3TtsEngineAdministration(
                voices.require(Qwen3TtsVoiceEngine.ID), assets));
        administration.register(new ComfyUiImageEngineAdministration(
                images.require(ComfyUiImageEngine.ID), assets, comfyProcess, launchProfiles));
        administration.register(new ComfyUiSuperResolutionAdministration(realEsrgan, assets, comfyProcess));
        administration.register(new ComfyUiTileRefinementAdministration(controlNetTile, assets, comfyProcess));
        administration.register(new ComfyUiVideoEngineAdministration(
                videoGeneration.require(ComfyUiVideoGenerationEngine.ID), assets, comfyProcess, launchProfiles));
        administration.register(new FfmpegEngineAdministration(ffmpeg, assets));
        administration.register(new QwenVisualAnalysisAdministration(
                qwenVisual, assets, ollamaProcess, DiskSpaceProbe.system(), certifications));
        administration.register(new PpStructureV3Administration(
                ppStructure, assets, certifications));
        administration.register(new MathCatEngineAdministration(
                mathCat, assets, certifications));
        return new MediaEnginePlatform(voices, images, superResolution, refinement,
                videoGeneration, videos, contentAnalysis, administration);
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
                + " -Language " + quote("{language}") + " -Device " + quote("{device}");
    }

    private static String xttsBatchCommand(RuntimeAssetCatalog assets) {
        return quote("powershell.exe") + " -NoProfile -ExecutionPolicy Bypass -File "
                + quote(assets.require(XttsVoiceEngine.ID, "batchScript"))
                + " -Python " + quote(assets.require(XttsVoiceEngine.ID, "python"))
                + " -Wrapper " + quote(assets.require(XttsVoiceEngine.ID, "batchWrapper"))
                + " -ModelDir " + quote(assets.require(XttsVoiceEngine.ID, "modelDirectory"))
                + " -Manifest " + quote("{manifestFile}")
                + " -Language " + quote("{language}") + " -Device " + quote("{device}");
    }

    private static String quote(Object value) { return "\"" + value + "\""; }
}
