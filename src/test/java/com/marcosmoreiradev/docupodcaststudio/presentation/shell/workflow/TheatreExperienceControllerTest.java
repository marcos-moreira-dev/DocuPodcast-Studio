package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.services.StoryboardApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.UpsertTheatreGeneratedFrameVariantUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.UpsertTheatreStoryboardFrameVariantUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageAspectRatio;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreGeneratedImageCandidate;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationPreset;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreExperienceController;
import com.marcosmoreiradev.docupodcaststudio.media.api.LocalResourceScheduler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class TheatreExperienceControllerTest {
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
    void unavailableImageCapabilityReturnsClearReadiness() {
        TheatreExperienceController controller = new TheatreExperienceController(
                WorkspaceTestServices.empty(), mediaCapabilities());

        var result = controller.readiness();

        assertFalse(result.ready());
        assertTrue(result.summary().contains("motor de imagen"));
    }

    @Test
    void legacyLargeModelPresetMapsToANeutralEnginePreset() throws Exception {
        TheatreImageGenerationUnit unit = new TheatreImageGenerationUnit(
                "ACT-1", "Acto", "SC-1", "Escena", "INT-1", "SEG-1",
                "NARRADOR", "texto de prueba", null);

        var request = TheatreExperienceController.imageRequest(
                unit,
                TheatreImageGenerationPreset.HIGH_QUALITY_FLUX,
                ImageEnhancementOutputProfile.FHD_1080,
                TheatreImageAspectRatio.WIDE_16_9,
                Path.of("target/test-generated/neutral-image"),
                "neutral-image", "prompt", List.of());

        assertEquals("flux-high-quality", request.presetId().value());
        assertEquals(1920, request.width());
        assertEquals(1080, request.height());
    }

    @Test
    void visualRequestKeepsSpatialContextSeparateFromScriptTextAndIncludesItInPrompt() throws Exception {
        TheatreImageGenerationUnit unit = new TheatreImageGenerationUnit(
                "ACT-1", "Acto", "SC-1", "Escena", "INT-1", "SEG-1",
                "NARRADOR", "NARRADOR: Texto original del guion.",
                "Disposicion espacial configurada:\n- NARRADOR se encuentra en fondo centro.",
                null);

        String prompt = "NARRADOR: Texto original del guion., spatial blocking context: "
                + unit.spatialContextText();
        var request = TheatreExperienceController.imageRequest(
                unit,
                TheatreImageGenerationPreset.TEST_4GB_SD15,
                ImageEnhancementOutputProfile.FHD_1080,
                TheatreImageAspectRatio.WIDE_16_9,
                Path.of("target/test-generated/spatial-context"),
                "spatial-context", prompt, List.of());

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
        TheatreExperienceController workflow = new TheatreExperienceController(
                WorkspaceTestServices.withStoryboard(storyboard), mediaCapabilities());
        TheatreGeneratedImageCandidate approved = workflow.approve(session, candidate);

        assertTrue(approved.approved());
        assertEquals("IMG-NEW", session.project().theatre().intervencionesVisuales().getFirst().assetId());
    }

    private static MediaCapabilityService mediaCapabilities() {
        return new MediaCapabilityService(
                com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform.empty(),
                LocalResourceScheduler.safeDefaults());
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
