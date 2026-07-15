package com.marcosmoreiradev.docupodcaststudio.bootstrap;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationGateway;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobRepository;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioProcessDiagnosticsRepository;
import com.marcosmoreiradev.docupodcaststudio.application.playback.SegmentAudioPlayer;
import com.marcosmoreiradev.docupodcaststudio.application.recording.AudioRecordingGateway;
import com.marcosmoreiradev.docupodcaststudio.application.export.ProjectBundleExporter;
import com.marcosmoreiradev.docupodcaststudio.application.guide.GuideCatalog;
import com.marcosmoreiradev.docupodcaststudio.application.resources.AiResourceCatalog;
import com.marcosmoreiradev.docupodcaststudio.application.resources.AiResourceExporter;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentImporter;
import com.marcosmoreiradev.docupodcaststudio.application.document.ImportedDocumentWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectRepository;
import com.marcosmoreiradev.docupodcaststudio.application.script.NarrationScriptWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.ImageAssetRepository;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.StoryboardWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceLibraryWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceSampleRepository;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisGateway;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsRepository;
import com.marcosmoreiradev.docupodcaststudio.application.media.UserMediaAssetRepository;
import com.marcosmoreiradev.docupodcaststudio.application.media.VideoAudioExtractionGateway;
import com.marcosmoreiradev.docupodcaststudio.application.media.AudioNormalizationGateway;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderJobRepository;

import java.util.List;

/** Concrete infrastructure adapters available to the application layer. */
public record InfrastructureServices(
        ProjectRepository projectRepository,
        List<DocumentImporter> documentImporters,
        ImportedDocumentWorkspaceRepository importedDocumentWorkspaceRepository,
        NarrationScriptWorkspaceRepository narrationScriptWorkspaceRepository,
        AudioGenerationGateway audioGenerationGateway,
        AudioJobRepository audioJobRepository,
        AudioProcessDiagnosticsRepository audioProcessDiagnosticsRepository,
        VoiceLibraryWorkspaceRepository voiceLibraryWorkspaceRepository,
        VoiceSampleRepository voiceSampleRepository,
        VoiceTestSynthesisGateway voiceTestSynthesisGateway,
        AudioRecordingGateway audioRecordingGateway,
        ImageAssetRepository imageAssetRepository,
        StoryboardWorkspaceRepository storyboardWorkspaceRepository,
        SegmentAudioPlayer segmentAudioPlayer,
        ProjectBundleExporter projectBundleExporter,
        GuideCatalog guideCatalog,
        AiResourceCatalog aiResourceCatalog,
        AiResourceExporter aiResourceExporter,
        OperationalSettingsRepository operationalSettingsRepository,
        UserMediaAssetRepository userMediaAssetRepository,
        VideoAudioExtractionGateway videoAudioExtractionGateway,
        AudioNormalizationGateway audioNormalizationGateway,
        VideoRenderJobRepository videoRenderJobRepository
) {
    public InfrastructureServices {
        documentImporters = documentImporters == null ? List.of() : List.copyOf(documentImporters);
    }
}
