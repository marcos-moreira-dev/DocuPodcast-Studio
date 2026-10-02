package com.marcosmoreiradev.docupodcaststudio.bootstrap;

import com.marcosmoreiradev.docupodcaststudio.infrastructure.audio.AudioJobFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.audio.AudioProcessDiagnosticsFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.audio.InMemoryAudioJobQueue;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.audio.VoiceEngineAudioGenerationGateway;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.audio.VoiceCapabilityTestSynthesisGateway;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.settings.LoadOperationalSettingsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.ApplicationRuntimeLayout;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimePathResolver;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.settings.PropertiesOperationalSettingsRepository;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationGateway;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.DocxDocumentImporter;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfOcrTextLayerUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.TesseractRuntimeLocator;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.MarkdownDocumentImporter;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PdfBoxRenderEngine;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PlainTextDocumentImporter;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.ReadableDocumentWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.TesseractPdfOcrEngine;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.DocuPodcastProjectFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.script.NarrationScriptWorkspaceFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.voice.VoiceLibraryWorkspaceFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.voice.LocalVoiceSampleFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.recording.JavaSoundAudioRecordingGateway;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.storyboard.LocalImageAssetFileRepository;
import com.marcosmoreiradev.docupodcaststudio.application.video.EmbeddedFfmpegLocator;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegToolDiscovery;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.media.FfmpegVideoAudioExtractionGateway;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.media.FfmpegAudioNormalizationGateway;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.media.LocalUserMediaAssetFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.process.DefaultExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.video.VideoRenderJobFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.storyboard.StoryboardWorkspaceFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.playback.JavaSoundSegmentAudioPlayer;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.export.FileSystemProjectBundleExporter;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.guide.ClasspathGuideCatalog;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.resources.OfficialAiResourceCatalog;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.resources.ClasspathAiResourceExporter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/** Wires concrete infrastructure adapters. */
public final class InfrastructureServicesFactory {
    public InfrastructureServices create(MediaCapabilityService mediaCapabilities, Path applicationRoot) {
        java.util.Objects.requireNonNull(mediaCapabilities, "media capabilities");
        AudioJobFileRepository audioJobRepository = new AudioJobFileRepository();
        AudioProcessDiagnosticsFileRepository processDiagnosticsRepository = new AudioProcessDiagnosticsFileRepository();
        PropertiesOperationalSettingsRepository settingsRepository = PropertiesOperationalSettingsRepository.defaultRepository();
        OperationalSettings operationalSettings = loadOperationalSettings(settingsRepository);
        ApplicationRuntimeLayout runtimeLayout = new ApplicationRuntimeLayout(applicationRoot);
        Path resolvedApplicationRoot = runtimeLayout.applicationRoot();
        DefaultExternalProcessRunner processRunner = new DefaultExternalProcessRunner();
        InMemoryAudioJobQueue audioQueue = new InMemoryAudioJobQueue();
        LoadOperationalSettingsUseCase loadSettings = new LoadOperationalSettingsUseCase(settingsRepository);
        AudioGenerationGateway audioGateway = new VoiceEngineAudioGenerationGateway(
                mediaCapabilities, loadSettings, audioQueue, audioJobRepository);
        OfficialAiResourceCatalog aiResourceCatalog = new OfficialAiResourceCatalog();
        Path configuredFfmpeg = runtimeLayout.resolveConfiguredPath(operationalSettings.video().ffmpegExecutable());
        FfmpegToolDiscovery ffmpegDiscovery = new EmbeddedFfmpegLocator().locate(resolvedApplicationRoot, configuredFfmpeg);
        FfmpegAudioNormalizationGateway audioNormalizationGateway = new FfmpegAudioNormalizationGateway(ffmpegDiscovery, processRunner);
        PdfBoxRenderEngine pdfRenderEngine = new PdfBoxRenderEngine();
        TesseractRuntimeLocator tesseractLocator = new TesseractRuntimeLocator();
        Supplier<String> tesseractCommand = () -> tesseractLocator
                .locate(loadOperationalSettings(settingsRepository), resolvedApplicationRoot)
                .command();
        BuildPdfOcrTextLayerUseCase pdfOcrTextLayer =
                new BuildPdfOcrTextLayerUseCase(new TesseractPdfOcrEngine(pdfRenderEngine, processRunner, tesseractCommand));
        return new InfrastructureServices(
                new DocuPodcastProjectFileRepository(),
                List.of(new DocxDocumentImporter(), new MarkdownDocumentImporter(), new PlainTextDocumentImporter()),
                new ReadableDocumentWorkspaceRepository(),
                new NarrationScriptWorkspaceFileRepository(),
                audioGateway,
                audioJobRepository,
                processDiagnosticsRepository,
                new VoiceLibraryWorkspaceFileRepository(),
                new LocalVoiceSampleFileRepository(resolvedApplicationRoot),
                new VoiceCapabilityTestSynthesisGateway(mediaCapabilities, loadSettings),
                new JavaSoundAudioRecordingGateway(),
                new LocalImageAssetFileRepository(),
                new StoryboardWorkspaceFileRepository(),
                new JavaSoundSegmentAudioPlayer(),
                new FileSystemProjectBundleExporter(),
                new ClasspathGuideCatalog(),
                aiResourceCatalog,
                new ClasspathAiResourceExporter(aiResourceCatalog),
                settingsRepository,
                new LocalUserMediaAssetFileRepository(),
                new FfmpegVideoAudioExtractionGateway(ffmpegDiscovery, processRunner),
                audioNormalizationGateway,
                new VideoRenderJobFileRepository()
        );
    }


    private static OperationalSettings loadOperationalSettings(PropertiesOperationalSettingsRepository repository) {
        try {
            return repository.load();
        } catch (IOException ex) {
            return OperationalSettings.defaults();
        }
    }
}
