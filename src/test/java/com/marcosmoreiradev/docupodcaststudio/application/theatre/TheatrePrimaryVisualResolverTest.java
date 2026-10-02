package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.storyboard.UpsertTheatreStoryboardFrameVariantUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDisplayMode;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatrePrimaryVisualResolverTest {
    @TempDir Path projectDirectory;

    @Test
    void resolvesUserAssignedImageBeforeGeneratedAndStoryboardVariants() throws Exception {
        createImage("media/images/oficial.png");
        createImage("media/images/generated.png");
        createImage("storyboard/drawn/frame.png");
        NarrationScriptDocument script = script();
        DocuPodcastProject project = baseProject()
                .withAsset(asset("IMG-OFFICIAL", "media/images/oficial.png"))
                .withAsset(asset("IMG-GENERATED", "media/images/generated.png"))
                .withAsset(asset("IMG-DRAWN", "storyboard/drawn/frame.png"));
        StoryboardDocument storyboard = StoryboardDocument.createForScript(script).withBinding(new StoryboardBinding(
                "STB-001",
                "SEG-001",
                "IMG-GENERATED",
                StoryboardDisplayMode.FIT_CONTAIN,
                "Generada",
                Map.of(
                        UpsertTheatreStoryboardFrameVariantUseCase.OFFICIAL_IMAGE_ASSET_ID, "IMG-OFFICIAL",
                        UpsertTheatreStoryboardFrameVariantUseCase.GENERATED_IMAGE_ASSET_ID, "IMG-GENERATED",
                        UpsertTheatreStoryboardFrameVariantUseCase.DRAWN_FRAME_ASSET_ID, "IMG-DRAWN",
                        UpsertTheatreStoryboardFrameVariantUseCase.ACTIVE_VISUAL_VARIANT,
                        UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_GENERATED)));

        TheatrePrimaryVisualReference resolved = new TheatrePrimaryVisualResolver()
                .resolve(project, storyboard, script, "INTERVENCION-1", projectDirectory)
                .orElseThrow();

        assertEquals("IMG-OFFICIAL", resolved.assetId());
        assertEquals(TheatrePrimaryVisualReference.Source.OFFICIAL_IMAGE, resolved.source());
    }

    @Test
    void fallsBackToGeneratedThenStoryboardThenLegacyOnlyWhenEarlierVariantsAreUnavailable() throws Exception {
        createImage("media/images/generated.png");
        createImage("media/images/legacy.png");
        NarrationScriptDocument script = script();
        DocuPodcastProject project = baseProject()
                .withAsset(asset("IMG-GENERATED", "media/images/generated.png"))
                .withAsset(asset("IMG-LEGACY", "media/images/legacy.png"))
                .withTheatre(theatre(List.of(new TheatreProjectLayer.IntervencionVisual("INTERVENCION-1", "IMG-LEGACY", ""))));
        StoryboardDocument storyboard = StoryboardDocument.createForScript(script).withBinding(new StoryboardBinding(
                "STB-001",
                "SEG-001",
                "IMG-GENERATED",
                StoryboardDisplayMode.FIT_CONTAIN,
                "Generada",
                Map.of(
                        UpsertTheatreStoryboardFrameVariantUseCase.GENERATED_IMAGE_ASSET_ID, "IMG-GENERATED",
                        UpsertTheatreStoryboardFrameVariantUseCase.ACTIVE_VISUAL_VARIANT,
                        UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_GENERATED)));

        TheatrePrimaryVisualReference resolved = new TheatrePrimaryVisualResolver()
                .resolveForSegment(project, storyboard, script.segments().getFirst(), projectDirectory)
                .orElseThrow();

        assertEquals("IMG-GENERATED", resolved.assetId());
        assertEquals(TheatrePrimaryVisualReference.Source.GENERATED_IMAGE, resolved.source());
    }

    @Test
    void activeSceneryCompositionIsTheExportedVisualEvenWhenAnOfficialImageExists() throws Exception {
        createImage("media/images/oficial.png");
        createImage("storyboard/scenery/frame.png");
        NarrationScriptDocument script = script();
        DocuPodcastProject project = baseProject()
                .withAsset(asset("IMG-OFFICIAL", "media/images/oficial.png"))
                .withAsset(asset("IMG-SCENERY", "storyboard/scenery/frame.png"));
        StoryboardDocument storyboard = StoryboardDocument.createForScript(script).withBinding(new StoryboardBinding(
                "STB-001", "SEG-001", "IMG-SCENERY", StoryboardDisplayMode.FIT_CONTAIN,
                "Personajes y escenografía", Map.of(
                        UpsertTheatreStoryboardFrameVariantUseCase.OFFICIAL_IMAGE_ASSET_ID, "IMG-OFFICIAL",
                        UpsertTheatreStoryboardFrameVariantUseCase.SCENERY_IMAGE_ASSET_ID, "IMG-SCENERY",
                        UpsertTheatreStoryboardFrameVariantUseCase.ACTIVE_VISUAL_VARIANT,
                        UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_SCENERY)));

        TheatrePrimaryVisualReference resolved = new TheatrePrimaryVisualResolver()
                .resolveForSegment(project, storyboard, script.segments().getFirst(), projectDirectory)
                .orElseThrow();

        assertEquals("IMG-SCENERY", resolved.assetId());
        assertEquals(TheatrePrimaryVisualReference.Source.SCENERY_COMPOSITION, resolved.source());
    }

    @Test
    void batchPlannerCountsExistingGenerableAndBlockedPairsAcrossTheWholeWork() throws Exception {
        createImage("media/images/uno.png");
        createImage("media/images/dos.png");
        createImage("media/images/tres.png");
        createImage("media/images/uno-dos.png");
        NarrationScriptDocument script = NarrationScriptDocument.create("Demo", "es", "source.docx", List.of(
                segment("SEG-001", "B0001"),
                segment("SEG-002", "B0002"),
                segment("SEG-003", "B0003"),
                segment("SEG-004", "B0004")));
        DocuPodcastProject project = DocuPodcastProject.empty("Demo")
                .withAsset(asset("IMG-1", "media/images/uno.png"))
                .withAsset(asset("IMG-2", "media/images/dos.png"))
                .withAsset(asset("IMG-3", "media/images/tres.png"))
                .withAsset(asset("IMG-12", "media/images/uno-dos.png"))
                .withTheatre(theatre(
                        List.of(
                                new TheatreProjectLayer.IntervencionVisual("INTERVENCION-1", "IMG-1", ""),
                                new TheatreProjectLayer.IntervencionVisual("INTERVENCION-2", "IMG-2", ""),
                                new TheatreProjectLayer.IntervencionVisual("INTERVENCION-3", "IMG-3", "")),
                        List.of(new TheatreProjectLayer.IntermediateFrame(
                                "INTERVENCION-1", "INTERVENCION-2", "IMG-12", ""))));
        List<TheatreImageGenerationUnit> units = List.of(
                unit("INTERVENCION-1", "SEG-001"),
                unit("INTERVENCION-2", "SEG-002"),
                unit("INTERVENCION-3", "SEG-003"),
                unit("INTERVENCION-4", "SEG-004"));

        TheatreIntermediateFrameBatchPlanner.Plan plan = new TheatreIntermediateFrameBatchPlanner()
                .plan(project, StoryboardDocument.createForScript(script), script, units, projectDirectory);

        assertEquals(3, plan.totalPairs());
        assertEquals(1, plan.existingPairs());
        assertEquals(1, plan.generablePairs());
        assertEquals(2, plan.allGenerablePairs());
        assertEquals(1, plan.overwritePairs());
        assertEquals(1, plan.blockedPairs());
        assertEquals(List.of("INTERVENCION-4"), plan.missingPrincipalInterventionIds());
        assertTrue(plan.hasGenerablePairs());
        assertTrue(plan.hasAllGenerablePairs());
        assertEquals(1, plan.missingItems().size());
        assertEquals(2, plan.allGenerableItems().size());
        assertEquals(1, plan.overwriteItems().size());
        assertEquals("INTERVENCION-2", plan.missingItems().getFirst().current().interventionId());
        assertTrue(plan.overwriteItems().getFirst().existingIntermediate());
        assertEquals("IMG-12", plan.overwriteItems().getFirst().existingIntermediateAssetId());
    }

    @Test
    void batchPlannerBlocksPairsWhenEffectiveCameraChanges() throws Exception {
        createImage("media/images/uno.png");
        createImage("media/images/dos.png");
        createImage("media/images/tres.png");
        NarrationScriptDocument script = NarrationScriptDocument.create("Demo", "es", "source.docx", List.of(
                segment("SEG-001", "B0001"),
                segment("SEG-002", "B0002"),
                segment("SEG-003", "B0003")));
        TheatreProjectLayer theatre = theatre(List.of(
                new TheatreProjectLayer.IntervencionVisual("INTERVENCION-1", "IMG-1", ""),
                new TheatreProjectLayer.IntervencionVisual("INTERVENCION-2", "IMG-2", ""),
                new TheatreProjectLayer.IntervencionVisual("INTERVENCION-3", "IMG-3", "")))
                .withCameraCues(List.of(new TheatreProjectLayer.CameraCue(
                        "INTERVENCION-2", "CERCA_DERECHA_NIVEL", "Cambio de camara")));
        DocuPodcastProject project = DocuPodcastProject.empty("Demo")
                .withAsset(asset("IMG-1", "media/images/uno.png"))
                .withAsset(asset("IMG-2", "media/images/dos.png"))
                .withAsset(asset("IMG-3", "media/images/tres.png"))
                .withTheatre(theatre);
        List<TheatreImageGenerationUnit> units = List.of(
                unit("INTERVENCION-1", "SEG-001"),
                unit("INTERVENCION-2", "SEG-002"),
                unit("INTERVENCION-3", "SEG-003"));

        TheatreIntermediateFrameBatchPlanner.Plan plan = new TheatreIntermediateFrameBatchPlanner()
                .plan(project, StoryboardDocument.createForScript(script), script, units, projectDirectory);

        assertEquals(2, plan.totalPairs());
        assertEquals(1, plan.blockedPairs());
        assertEquals(1, plan.generablePairs());
        assertEquals("INTERVENCION-2", plan.missingItems().getFirst().current().interventionId());
    }

    private DocuPodcastProject baseProject() {
        return DocuPodcastProject.empty("Demo").withTheatre(theatre(List.of()));
    }

    private TheatreProjectLayer theatre(List<TheatreProjectLayer.IntervencionVisual> visuals) {
        return theatre(visuals, List.of());
    }

    private TheatreProjectLayer theatre(List<TheatreProjectLayer.IntervencionVisual> visuals,
                                        List<TheatreProjectLayer.IntermediateFrame> intermediateFrames) {
        return new TheatreProjectLayer(
                List.of(
                        TheatreProjectLayer.Intervencion.ofSequence(1, "B0001"),
                        TheatreProjectLayer.Intervencion.ofSequence(2, "B0002"),
                        TheatreProjectLayer.Intervencion.ofSequence(3, "B0003"),
                        TheatreProjectLayer.Intervencion.ofSequence(4, "B0004")),
                List.of(), List.of(), List.of(), visuals, intermediateFrames,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private NarrationScriptDocument script() {
        return NarrationScriptDocument.create("Demo", "es", "source.docx",
                List.of(segment("SEG-001", "B0001")));
    }

    private static NarrationSegment segment(String segmentId, String blockId) {
        return NarrationSegment.of(segmentId, NarrationSegmentType.PARAGRAPH,
                "Texto", "NARRADOR: Texto.", List.of(blockId));
    }

    private static TheatreImageGenerationUnit unit(String interventionId, String segmentId) {
        return new TheatreImageGenerationUnit("", "", "SCN-1", "Escena", interventionId, segmentId,
                "NARRADOR", "Texto", "", null);
    }

    private static ProjectAssetReference asset(String id, String relativePath) {
        return new ProjectAssetReference(id, ProjectAssetKind.IMAGE, id, relativePath, "image/png", "", "", "");
    }

    private void createImage(String relativePath) throws Exception {
        Path file = projectDirectory.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "png");
    }
}
