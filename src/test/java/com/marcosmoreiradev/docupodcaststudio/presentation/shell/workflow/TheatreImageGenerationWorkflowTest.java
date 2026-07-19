package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.ApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;
import com.marcosmoreiradev.docupodcaststudio.application.services.StoryboardApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.UpsertTheatreGeneratedFrameVariantUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.UpsertTheatreStoryboardFrameVariantUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageAspectRatio;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreGeneratedImageCandidate;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationPreset;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationUnit;
import com.marcosmoreiradev.docupodcaststudio.application.visual.ComfyUiVisualEngineClient;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class TheatreImageGenerationWorkflowTest {
    @Test
    void test4GbPresetUsesSd15LowVramSettings() {
        TheatreImageGenerationPreset preset = TheatreImageGenerationPreset.TEST_4GB_SD15;
        assertEquals("v1-5-pruned-emaonly-fp16.safetensors", preset.checkpointName());
        assertEquals(512, preset.width());
        assertEquals(512, preset.height());
        assertEquals(1, preset.batchSize());
        assertTrue(preset.lowVramRecommended());
    }

    @Test
    void unavailableComfyUiReturnsClearMessage() {
        ComfyUiVisualEngineClient.ConnectionResult result = new ComfyUiVisualEngineClient()
                .test("http://127.0.0.1:1", Duration.ofMillis(80));
        assertFalse(result.available());
        assertTrue(result.message().contains("motor de generacion visual local no responde"));
    }

    @Test
    void largeModelPresetBuildsAVisualRequestForIntegratedWorkflow() throws Exception {
        TheatreImageGenerationUnit unit = new TheatreImageGenerationUnit(
                "ACT-1", "Acto", "SC-1", "Escena", "INT-1", "SEG-1",
                "NARRADOR", "texto de prueba", null);

        var request = TheatreImageGenerationWorkflow.visualEngineRequest(
                unit,
                TheatreImageGenerationPreset.HIGH_QUALITY_FLUX,
                ImageEnhancementOutputProfile.FHD_1080,
                TheatreImageAspectRatio.WIDE_16_9,
                java.nio.file.Path.of("target/test-generated/unsupported-flux"),
                "integrated-flux");

        assertEquals("flux1-dev.safetensors", request.checkpointName());
        assertEquals(1920, request.targetWidth());
        assertEquals(1080, request.targetHeight());
    }

    @Test
    void visualRequestKeepsSpatialContextSeparateFromScriptTextAndIncludesItInPrompt() throws Exception {
        TheatreImageGenerationUnit unit = new TheatreImageGenerationUnit(
                "ACT-1", "Acto", "SC-1", "Escena", "INT-1", "SEG-1",
                "NARRADOR", "NARRADOR: Texto original del guion.",
                "Disposicion espacial configurada:\n- NARRADOR se encuentra en fondo centro.",
                null);

        var request = TheatreImageGenerationWorkflow.visualEngineRequest(
                unit,
                TheatreImageGenerationPreset.TEST_4GB_SD15,
                ImageEnhancementOutputProfile.FHD_1080,
                TheatreImageAspectRatio.WIDE_16_9,
                java.nio.file.Path.of("target/test-generated/spatial-context"),
                "spatial-context");

        assertTrue(request.prompt().contains("Texto original del guion."));
        assertTrue(request.prompt().contains("spatial blocking context"));
        assertTrue(request.prompt().contains("NARRADOR se encuentra en fondo centro."));
    }

    @Test
    void generatedCandidateDoesNotReplaceVisualUntilApproval() {
        ProjectSession session = ProjectSession.opened(projectWithOldVisual(), java.nio.file.Path.of("obra.docupodcast.json"));
        session.hydrateNarrationScript(NarrationScriptDocument.create("Obra", "es", "source.docx", List.of(
                NarrationSegment.of("SEG-1", NarrationSegmentType.PARAGRAPH, "Uno", "Texto", List.of("B0001")))));
        TheatreGeneratedImageCandidate candidate = new TheatreGeneratedImageCandidate("INTERVENCION-1", "SC-1", "INTERVENCION-1", "SEG-1", "IMG-NEW", java.nio.file.Path.of("new.png"), false);
        assertEquals("IMG-OLD", session.project().theatre().intervencionesVisuales().getFirst().assetId());

        UpsertTheatreStoryboardFrameVariantUseCase variants = new UpsertTheatreStoryboardFrameVariantUseCase();
        StoryboardApplicationServices storyboard = new StoryboardApplicationServices(
                null, null, null, null, variants, new UpsertTheatreGeneratedFrameVariantUseCase(variants), null, null);
        TheatreImageGenerationWorkflow workflow = new TheatreImageGenerationWorkflow(new ApplicationServices(
                null, null, null, null, null, null, null, null, null, null, storyboard,
                null, null, null, null, null, null, null, null, null, null, null));
        TheatreGeneratedImageCandidate approved = workflow.approve(session, candidate);

        assertTrue(approved.approved());
        assertEquals("IMG-NEW", session.project().theatre().intervencionesVisuales().getFirst().assetId());
    }

    private static DocuPodcastProject projectWithOldVisual() {
        DocuPodcastProject project = DocuPodcastProject.createNew("Obra")
                .withAsset(new ProjectAssetReference("IMG-OLD", ProjectAssetKind.IMAGE, "old", "media/old.png", "image/png", "", "", ""))
                .withAsset(new ProjectAssetReference("IMG-NEW", ProjectAssetKind.IMAGE, "new", "media/new.png", "image/png", "", "", ""));
        return project.withTheatre(new TheatreProjectLayer(
                List.of(new TheatreProjectLayer.Intervencion("INTERVENCION-1", "B0001", 1)),
                List.of(), List.of(), List.of(),
                List.of(new TheatreProjectLayer.IntervencionVisual("INTERVENCION-1", "IMG-OLD", "")),
                List.of(), List.of(new TheatreProjectLayer.Scene("SC-1", "Escena", "")),
                List.of(), List.of(), List.of(), List.of(), List.of()));
    }
}
