package com.marcosmoreiradev.docupodcaststudio.integration;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.BuildDocumentStudyVideoAudioOverlayPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.BuildDocumentStudyVideoPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentTextVideoOptions;
import com.marcosmoreiradev.docupodcaststudio.application.video.EmbeddedFfmpegLocator;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegRuntimeProbeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.FinalVideoRenderRequest;
import com.marcosmoreiradev.docupodcaststudio.application.video.RenderFinalVideoPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoExportSettings;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoResolutionPreset;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoAudioOverlayPlan;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.audio.AudioJobFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.ReadableDocumentWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.DocuPodcastProjectFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.process.DefaultExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.script.NarrationScriptWorkspaceFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentStudyVideoRealSmokeTest {
    @Test
    @EnabledIfSystemProperty(named = "docupodcast.documentarySmokeProject", matches = ".+")
    void exportsSavedDocumentaryProjectWithLocalFfmpeg() throws Exception {
        Path projectFile = Path.of(System.getProperty("docupodcast.documentarySmokeProject"))
                .toAbsolutePath().normalize();
        Path projectDirectory = projectFile.getParent();
        Path target = Path.of(System.getProperty("docupodcast.documentarySmokeTarget",
                projectDirectory.resolve("exports/documentary-smoke.mp4").toString())).toAbsolutePath().normalize();
        Path applicationRoot = Path.of(System.getProperty("docupodcast.applicationRoot", "."))
                .toAbsolutePath().normalize();

        var project = new DocuPodcastProjectFileRepository().open(projectFile);
        var document = new ReadableDocumentWorkspaceRepository().load(projectFile).orElseThrow();
        var script = new NarrationScriptWorkspaceFileRepository().load(projectFile).orElseThrow();
        var jobs = new AudioJobFileRepository().list(projectDirectory);
        DocumentTextVideoOptions options = DocumentTextVideoOptions.defaults()
                .withResolution(SimpleVideoResolutionPreset.HD_720);
        SimpleVideoPlan plan = new BuildDocumentStudyVideoPlanUseCase()
                .build(project, document, script, jobs, projectDirectory, options);
        assertTrue(plan.exportableAsRenderedVideo(), "El proyecto documental debe tener imagen y audio resolubles.");
        VideoAudioOverlayPlan overlays = new BuildDocumentStudyVideoAudioOverlayPlanUseCase()
                .build(project, projectDirectory, plan);

        DefaultExternalProcessRunner runner = new DefaultExternalProcessRunner();
        RenderFinalVideoPlanUseCase renderer = new RenderFinalVideoPlanUseCase(
                new FfmpegRuntimeProbeUseCase(runner), new EmbeddedFfmpegLocator(), runner);
        SimpleVideoExportSettings settings = new SimpleVideoExportSettings(
                SimpleVideoResolutionPreset.HD_720, 24, 0.35, true, true,
                ComputeDevicePolicy.CPU_ONLY, VideoEncoderPolicy.CPU_X264);
        renderer.render(new FinalVideoRenderRequest(plan, projectDirectory, target, settings,
                applicationRoot, null, overlays));

        assertTrue(Files.isRegularFile(target));
        assertTrue(Files.size(target) > 0L);
    }
}
